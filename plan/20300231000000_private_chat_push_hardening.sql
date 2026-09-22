-- ====================================================================
-- MIGRATION: Pengerasan jalur push chat PRIBADI (anti-bocor)
--
-- GEJALA
-- Pesan pribadi tampil di HP SEMUA staf sebagai notifikasi "Chat Tim", jadi
-- orang lain bisa membaca percakapan berdua dari bilah notifikasi.
--
-- SEBAB
-- Migrasi 20300225000000 memindahkan push chat pribadi ke `send-chat-push`
-- (per penerima), tetapi:
--   a) trigger push LAMA pada private_chat_messages — dibuat langsung di DB,
--      menyiram seluruh `fcm_tokens` via `send-push` broadcast — bisa masih
--      hidup bila migrasi itu belum pernah dijalankan; dan
--   b) fungsi trigger baru menurunkan URL `send-chat-push` dengan
--      replace('/send-push', ...). Bila rahasia `push_webhook_url` tidak
--      berakhiran `/send-push` persis, replace tidak mengubah apa pun dan
--      pesan pribadi tetap disiram lewat fungsi lama.
--
-- PERBAIKAN
--   1. Lepas SEMUA trigger http_post pada private_chat_messages selain milik
--      kita, sekaligus fungsinya bila tak dipakai tabel lain.
--   2. Tulis ulang trigger_private_chat_push: URL edge function dibentuk
--      dari akar `/functions/v1/` (bukan replace rapuh), dan bila penerimanya
--      kosong pesan TIDAK dikirim sama sekali — lebih baik tanpa notifikasi
--      daripada notifikasi yang salah alamat.
--   3. Kueri verifikasi di bagian bawah untuk memastikan hasilnya.
--
-- Prasyarat: edge function `send-chat-push` versi terbaru (menyaring
-- `recipient_id` dan menyertakannya di payload) sudah di-deploy:
--   supabase functions deploy send-chat-push
-- ====================================================================

-- 1. Lepas trigger push lama (apa pun namanya) pada private_chat_messages.
DO $$
DECLARE
    r record;
BEGIN
    FOR r IN
        SELECT t.tgname, p.proname, p.oid AS fn_oid
          FROM pg_trigger t
          JOIN pg_class c ON c.oid = t.tgrelid
          JOIN pg_namespace n ON n.oid = c.relnamespace
          JOIN pg_proc  p ON p.oid = t.tgfoid
         WHERE n.nspname = 'public'
           AND c.relname = 'private_chat_messages'
           AND NOT t.tgisinternal
           AND (p.prosrc ILIKE '%net.http_post%' OR p.prosrc ILIKE '%send-push%' OR p.prosrc ILIKE '%send_push%')
           AND p.proname <> 'trigger_private_chat_push'
    LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS %I ON public.private_chat_messages', r.tgname);
        RAISE NOTICE 'Trigger push lama dilepas: % (fungsi %)', r.tgname, r.proname;

        -- Fungsinya ikut dibuang bila tidak lagi dipakai trigger tabel mana pun,
        -- supaya tidak ada yang memasangnya kembali tanpa sengaja.
        IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgfoid = r.fn_oid) THEN
            EXECUTE format('DROP FUNCTION IF EXISTS %s', r.fn_oid::regprocedure);
            RAISE NOTICE 'Fungsi push lama dibuang: %', r.proname;
        END IF;
    END LOOP;
END;
$$;

-- 2. Trigger push chat pribadi versi keras.
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
    -- Tanpa penerima, tidak ada yang boleh dikirimi. Jangan pernah jatuh ke
    -- siaran.
    IF NEW.recipient_id IS NULL THEN
      RETURN NEW;
    END IF;

    SELECT decrypted_secret INTO v_url
      FROM vault.decrypted_secrets WHERE name = 'push_webhook_url' LIMIT 1;
    SELECT decrypted_secret INTO v_key
      FROM vault.decrypted_secrets WHERE name = 'fcm_webhook_secret' LIMIT 1;

    IF COALESCE(v_url, '') = '' OR COALESCE(v_key, '') = '' THEN
      RETURN NEW;
    END IF;

    -- URL edge function dibentuk dari akarnya, bukan replace nama fungsi:
    -- ".../functions/v1/apa-pun" -> ".../functions/v1/send-chat-push".
    v_url := regexp_replace(rtrim(v_url, '/'), '/functions/v1/.*$', '/functions/v1/send-chat-push');
    IF v_url NOT LIKE '%/functions/v1/send-chat-push' THEN
      RAISE WARNING 'push_webhook_url tidak dikenali (%): push chat pribadi dibatalkan agar tidak bocor.', v_url;
      RETURN NEW;
    END IF;

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

-- 3. VERIFIKASI (jalankan setelah migrasi; hasil yang benar = tepat SATU baris
--    trigger http, yaitu trg_private_chat_push -> trigger_private_chat_push):
--
--   SELECT t.tgname, p.proname
--     FROM pg_trigger t
--     JOIN pg_class c ON c.oid = t.tgrelid
--     JOIN pg_proc  p ON p.oid = t.tgfoid
--    WHERE c.relname = 'private_chat_messages'
--      AND NOT t.tgisinternal
--      AND p.prosrc ILIKE '%http_post%';
--
--   -- dan jawaban edge function atas kiriman terakhir (status 200 = terkirim):
--   SELECT id, status_code, error_msg, created
--     FROM net._http_response
--    ORDER BY created DESC LIMIT 5;
--
-- ROLLBACK:
--   DROP TRIGGER IF EXISTS trg_private_chat_push ON public.private_chat_messages;
--   DROP FUNCTION IF EXISTS public.trigger_private_chat_push();
