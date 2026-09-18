-- ====================================================================
-- MIGRATION: Push chat PRIBADI pindah ke jalur superapp sendiri
--
-- MASALAH
-- Notifikasi chat pribadi selama ini mendarat di aplikasi POS, bukan di
-- superapp. Sebabnya jalur pengirimannya: trigger lama memakai `send-push`,
-- yang menyiram seluruh isi `fcm_tokens` — tabel yang dipakai BERSAMA aplikasi
-- POS. Akibatnya isi percakapan berdua ikut sampai ke HP kasir, dan di sana
-- digambar sebagai notifikasi POS biasa tanpa nama maupun foto pengirim.
--
-- PERBAIKAN
-- Trigger baru memakai `send-chat-push`, yang membaca `chat_push_tokens`
-- (hanya diisi superapp) dan kini menerima `recipient_id` sehingga pesan
-- berhenti tepat di perangkat penerimanya — bukan disiarkan ke semua orang.
--
-- Prasyarat: edge function `send-chat-push` versi terbaru sudah di-deploy
-- (yang mengenal `recipient_id` dan `sender_avatar`), dan migrasi
-- 20300224000000 sudah masuk (fungsi `chat_teks_push`).
-- ====================================================================

-- 1. Buang trigger push LAMA pada private_chat_messages.
--    Namanya tidak diketahui di repo mana pun — fungsinya dibuat langsung di
--    database — jadi yang dicari adalah ciri khasnya: trigger pada tabel ini
--    yang badan fungsinya memanggil net.http_post. Trigger lain (mis. penanda
--    waktu atau pembersih audio) tidak ikut tersentuh.
DO $$
DECLARE
    r record;
BEGIN
    FOR r IN
        SELECT t.tgname, p.proname
          FROM pg_trigger t
          JOIN pg_class c ON c.oid = t.tgrelid
          JOIN pg_proc  p ON p.oid = t.tgfoid
         WHERE c.relname = 'private_chat_messages'
           AND NOT t.tgisinternal
           AND p.prosrc ILIKE '%net.http_post%'
           AND p.proname <> 'trigger_private_chat_push'
    LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS %I ON public.private_chat_messages', r.tgname);
        RAISE NOTICE 'Trigger push lama dilepas: % (fungsi %)', r.tgname, r.proname;
    END LOOP;
END;
$$;

-- 2. Trigger push chat pribadi versi superapp.
CREATE OR REPLACE FUNCTION public.trigger_private_chat_push()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_url    text;
  v_key    text;
  v_body   text;
  v_nama   text;
  v_avatar text;
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

    SELECT coalesce(nullif(btrim(s.display_name), ''), s.name), s.avatar_url
      INTO v_nama, v_avatar
      FROM public.outlet_staff s
     WHERE s.id = NEW.sender_id;

    v_body := public.chat_teks_push(NEW.body, NEW.image_path, NEW.audio_path, NEW.audio_ms);

    PERFORM net.http_post(
      url := v_url,
      headers := jsonb_build_object(
        'Content-Type', 'application/json',
        'Authorization', 'Bearer ' || v_key
      ),
      body := jsonb_build_object(
        'message_id',    NEW.id,
        'sender_id',     NEW.sender_id,
        'sender_name',   COALESCE(v_nama, 'Rekan kerja'),
        'sender_avatar', COALESCE(v_avatar, ''),
        'recipient_id',  NEW.recipient_id,
        'body',          v_body
      )
    );
  EXCEPTION WHEN OTHERS THEN
    -- Pesan tidak boleh gagal terkirim hanya karena notifikasinya gagal.
    RAISE WARNING 'Gagal mengirim push chat pribadi: %', SQLERRM;
  END;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_private_chat_push ON public.private_chat_messages;
CREATE TRIGGER trg_private_chat_push
    AFTER INSERT ON public.private_chat_messages
    FOR EACH ROW EXECUTE FUNCTION public.trigger_private_chat_push();

-- ROLLBACK:
--   DROP TRIGGER IF EXISTS trg_private_chat_push ON public.private_chat_messages;
--   DROP FUNCTION IF EXISTS public.trigger_private_chat_push();
--   (lalu pasang kembali trigger push lama bila memang masih diinginkan)
