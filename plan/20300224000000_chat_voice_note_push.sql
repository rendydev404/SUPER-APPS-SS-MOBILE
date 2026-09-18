-- ====================================================================
-- MIGRATION: Teks push notifikasi untuk voice note, meniru WhatsApp
--
-- Sebelum ini, pesan tanpa teks selalu dianggap foto, jadi voice note muncul
-- di notifikasi sebagai "📷 Foto". WhatsApp menuliskan jenis lampirannya:
--   teks            -> isi pesannya
--   foto + caption  -> "📷 <caption>"
--   foto saja       -> "📷 Foto"
--   voice note      -> "🎤 Pesan suara (0:04)"
--
-- Nama pengirim TIDAK disisipkan ke dalam teks ini: aplikasi menggambar
-- notifikasi chat dengan MessagingStyle, yang sudah menaruh nama pengirim di
-- depan pesannya — menambahkannya di sini membuat namanya tertulis dua kali.
-- ====================================================================

-- 1. Satu tempat penentuan label, dipakai push grup maupun push chat pribadi.
CREATE OR REPLACE FUNCTION public.chat_teks_push(
    p_body TEXT,
    p_image_path TEXT DEFAULT NULL,
    p_audio_path TEXT DEFAULT NULL,
    p_audio_ms INTEGER DEFAULT NULL
)
RETURNS text
LANGUAGE sql
IMMUTABLE
SET search_path TO 'public'
AS $$
  SELECT CASE
    WHEN coalesce(btrim(p_audio_path), '') <> '' THEN
      '🎤 Pesan suara'
      || CASE
           WHEN coalesce(p_audio_ms, 0) > 0 THEN
             ' (' || (p_audio_ms / 60000)::text || ':'
                  || lpad((((p_audio_ms / 1000) % 60))::text, 2, '0') || ')'
           ELSE ''
         END
    WHEN btrim(coalesce(p_body, '')) <> '' AND coalesce(btrim(p_image_path), '') <> ''
      THEN '📷 ' || left(btrim(p_body), 120)
    WHEN btrim(coalesce(p_body, '')) <> ''
      THEN left(btrim(p_body), 140)
    WHEN coalesce(btrim(p_image_path), '') <> '' THEN '📷 Foto'
    ELSE 'Pesan baru'
  END;
$$;

GRANT EXECUTE ON FUNCTION public.chat_teks_push(TEXT, TEXT, TEXT, INTEGER) TO authenticated, service_role;

-- 2. Push chat grup (menggantikan versi di migrasi 20300212000000; hanya baris
--    penyusun v_body yang berubah).
CREATE OR REPLACE FUNCTION public.trigger_chat_message_push()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_url   text;
  v_key   text;
  v_body  text;
  v_grup  record;
BEGIN
  BEGIN
    SELECT decrypted_secret INTO v_url
      FROM vault.decrypted_secrets WHERE name = 'push_webhook_url' LIMIT 1;
    SELECT decrypted_secret INTO v_key
      FROM vault.decrypted_secrets WHERE name = 'fcm_webhook_secret' LIMIT 1;

    IF COALESCE(v_url, '') = '' OR COALESCE(v_key, '') = '' THEN
      RETURN NEW;
    END IF;
    v_url := replace(v_url, '/send-push', '/send-chat-push');

    SELECT nama_grup, foto_grup INTO v_grup
      FROM public.chat_settings WHERE id = 1;

    v_body := public.chat_teks_push(NEW.body, NEW.image_path, NEW.audio_path, NEW.audio_ms);

    PERFORM net.http_post(
      url := v_url,
      headers := jsonb_build_object(
        'Content-Type', 'application/json',
        'Authorization', 'Bearer ' || v_key
      ),
      body := jsonb_build_object(
        'message_id',  NEW.id,
        'sender_id',   NEW.sender_id,
        'sender_name', NEW.sender_name,
        'body',        v_body,
        'group_name',  COALESCE(v_grup.nama_grup, 'Chat Tim'),
        'group_photo', COALESCE(v_grup.foto_grup, '')
      )
    );
  EXCEPTION WHEN OTHERS THEN
    -- Pesan tidak boleh gagal terkirim hanya karena notifikasinya gagal.
    RAISE WARNING 'Gagal mengirim push chat: %', SQLERRM;
  END;

  RETURN NEW;
END;
$$;

-- ROLLBACK:
--   Kembalikan trigger_chat_message_push dari migrasi 20300212000000
--   dan: DROP FUNCTION IF EXISTS public.chat_teks_push(TEXT, TEXT, TEXT, INTEGER);
