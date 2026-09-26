-- Stiker chat dari KLIPY (grup global, grup area, chat pribadi).
--
-- Berkas stiker TIDAK disimpan di storage kita: kolom `sticker_url` menyimpan URL
-- media KLIPY apa adanya, dan app memuatnya langsung dari CDN mereka.
--
-- Pesan stiker tetap membawa `body = 'Stiker'` sebagai teks cadangan. Dengan itu
-- constraint "isi ada", teks push (`chat_teks_push`), dan snapshot kutipan balasan
-- berjalan tanpa diubah, dan app versi lama/web menampilkan tulisan "Stiker"
-- alih-alih bubble kosong. App baru mengabaikan body bila `sticker_url` terisi.
--
-- Definisi fungsi di bawah disalin dari produksi (pg_get_functiondef, 24 Sep 2026),
-- BUKAN dari berkas plan/ lama — keduanya sudah berbeda.

BEGIN;

-- 1. Kolom. Hanya HTTPS di domain klipy.com: tanpa batas ini siapa pun bisa
--    menyelipkan URL pelacak yang otomatis dimuat HP semua penerima.
ALTER TABLE public.chat_messages
    ADD COLUMN IF NOT EXISTS sticker_url TEXT;
ALTER TABLE public.private_chat_messages
    ADD COLUMN IF NOT EXISTS sticker_url TEXT;
ALTER TABLE public.area_chat_messages
    ADD COLUMN IF NOT EXISTS sticker_url TEXT;

ALTER TABLE public.chat_messages DROP CONSTRAINT IF EXISTS chat_messages_stiker_klipy;
ALTER TABLE public.chat_messages ADD CONSTRAINT chat_messages_stiker_klipy CHECK (
    sticker_url IS NULL
    OR (char_length(sticker_url) <= 500 AND sticker_url ~ '^https://([a-z0-9-]+\.)*klipy\.com/')
);
ALTER TABLE public.private_chat_messages DROP CONSTRAINT IF EXISTS private_chat_messages_stiker_klipy;
ALTER TABLE public.private_chat_messages ADD CONSTRAINT private_chat_messages_stiker_klipy CHECK (
    sticker_url IS NULL
    OR (char_length(sticker_url) <= 500 AND sticker_url ~ '^https://([a-z0-9-]+\.)*klipy\.com/')
);
ALTER TABLE public.area_chat_messages DROP CONSTRAINT IF EXISTS area_chat_messages_stiker_klipy;
ALTER TABLE public.area_chat_messages ADD CONSTRAINT area_chat_messages_stiker_klipy CHECK (
    sticker_url IS NULL
    OR (char_length(sticker_url) <= 500 AND sticker_url ~ '^https://([a-z0-9-]+\.)*klipy\.com/')
);

-- 2. Pesan yang dihapus (nisan) ikut kehilangan stikernya, di jalur hapus mana pun
--    (chat_hapus_pesan, private_chat_hapus_pesan, atau update area). Trigger
--    terpisah dari chat_nisan_buang_audio supaya fungsi lama tidak tersentuh.
CREATE OR REPLACE FUNCTION public.chat_nisan_buang_stiker()
RETURNS trigger
LANGUAGE plpgsql
SET search_path TO 'public'
AS $function$
BEGIN
    IF NEW.deleted_at IS NOT NULL AND OLD.deleted_at IS NULL THEN
        NEW.sticker_url := NULL;
    END IF;
    RETURN NEW;
END;
$function$;

DROP TRIGGER IF EXISTS trg_chat_nisan_buang_stiker ON public.chat_messages;
CREATE TRIGGER trg_chat_nisan_buang_stiker
    BEFORE UPDATE ON public.chat_messages
    FOR EACH ROW EXECUTE FUNCTION public.chat_nisan_buang_stiker();
DROP TRIGGER IF EXISTS trg_pcm_nisan_buang_stiker ON public.private_chat_messages;
CREATE TRIGGER trg_pcm_nisan_buang_stiker
    BEFORE UPDATE ON public.private_chat_messages
    FOR EACH ROW EXECUTE FUNCTION public.chat_nisan_buang_stiker();
DROP TRIGGER IF EXISTS trg_area_nisan_buang_stiker ON public.area_chat_messages;
CREATE TRIGGER trg_area_nisan_buang_stiker
    BEFORE UPDATE ON public.area_chat_messages
    FOR EACH ROW EXECUTE FUNCTION public.chat_nisan_buang_stiker();

-- 3. private_chat_kirim menerima p_sticker_url. Parameter baru di akhir dengan
--    DEFAULT, jadi app lama (yang tidak mengirimnya) tetap cocok. Fungsi lama
--    di-DROP dulu: dua overload membuat PostgREST gagal memilih ("could not
--    choose the best candidate function"). Isi di luar p_sticker_url identik
--    dengan produksi.
DROP FUNCTION IF EXISTS public.private_chat_kirim(uuid, text, text, uuid, text, integer, text);

CREATE FUNCTION public.private_chat_kirim(
    p_recipient_id uuid,
    p_body text,
    p_image_path text DEFAULT NULL::text,
    p_reply_to_id uuid DEFAULT NULL::uuid,
    p_audio_path text DEFAULT NULL::text,
    p_audio_ms integer DEFAULT NULL::integer,
    p_audio_wave text DEFAULT NULL::text,
    p_sticker_url text DEFAULT NULL::text
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $function$
DECLARE
    v_sender_id UUID := auth.uid();
    v_new_msg RECORD;
    v_reply_name TEXT := NULL;
    v_reply_snippet TEXT := NULL;
BEGIN
    IF v_sender_id IS NULL THEN
        RAISE EXCEPTION 'Tidak ada sesi aktif.';
    END IF;
    IF v_sender_id = p_recipient_id THEN
        RAISE EXCEPTION 'Tidak dapat mengirim chat pribadi ke diri sendiri.';
    END IF;
    IF coalesce(btrim(p_body), '') = ''
       AND coalesce(btrim(p_image_path), '') = ''
       AND coalesce(btrim(p_audio_path), '') = ''
       AND coalesce(btrim(p_sticker_url), '') = '' THEN
        RAISE EXCEPTION 'Isi pesan, foto, atau rekaman suara tidak boleh kosong.';
    END IF;

    -- Suara wajib berada di folder pengirim sendiri, sama seperti policy storage.
    IF p_audio_path IS NOT NULL
       AND p_audio_path NOT LIKE 'chat-media/' || v_sender_id::text || '/%' THEN
        RAISE EXCEPTION 'Path rekaman suara tidak sah.';
    END IF;

    -- Ambil kutipan reply jika ada
    IF p_reply_to_id IS NOT NULL THEN
        SELECT
            s.name,
            left(m.body, 100)
        INTO v_reply_name, v_reply_snippet
        FROM public.private_chat_messages m
        JOIN public.outlet_staff s ON s.id = m.sender_id
        WHERE m.id = p_reply_to_id
          AND (m.sender_id IN (v_sender_id, p_recipient_id) AND m.recipient_id IN (v_sender_id, p_recipient_id));

        -- Kutipan pesan suara tidak punya teks; beri label supaya balasannya
        -- tidak tampil sebagai kutipan kosong.
        IF coalesce(btrim(v_reply_snippet), '') = '' THEN
            SELECT CASE
                     WHEN m.audio_path IS NOT NULL THEN '🎤 Pesan suara'
                     WHEN m.image_path IS NOT NULL THEN '📷 Foto'
                     ELSE v_reply_snippet
                   END
              INTO v_reply_snippet
              FROM public.private_chat_messages m
             WHERE m.id = p_reply_to_id;
        END IF;
    END IF;

    INSERT INTO public.private_chat_messages (
        sender_id,
        recipient_id,
        body,
        image_path,
        audio_path,
        audio_ms,
        audio_wave,
        sticker_url,
        reply_to_id,
        reply_to_name,
        reply_to_snippet,
        created_at
    ) VALUES (
        v_sender_id,
        p_recipient_id,
        btrim(p_body),
        p_image_path,
        p_audio_path,
        p_audio_ms,
        left(coalesce(p_audio_wave, ''), 56),
        nullif(btrim(p_sticker_url), ''),
        p_reply_to_id,
        v_reply_name,
        v_reply_snippet,
        now()
    )
    RETURNING * INTO v_new_msg;

    RETURN to_jsonb(v_new_msg);
END;
$function$;

-- 4. Pembacaan pesan pribadi ikut mengembalikan sticker_url (signature tetap,
--    hanya satu kunci JSON bertambah).
CREATE OR REPLACE FUNCTION public.private_chat_ambil_pesan(p_partner_id uuid)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $function$
DECLARE
    v_my_id UUID := auth.uid();
    v_cutoff_wib timestamp;
    v_cutoff timestamptz;
    v_now_wib timestamp;
    v_res jsonb;
BEGIN
    IF v_my_id IS NULL THEN
        RETURN '[]'::jsonb;
    END IF;

    v_now_wib := timezone('Asia/Jakarta', now());
    IF extract(hour from v_now_wib) < 3 THEN
        v_cutoff_wib := (v_now_wib::date - 1) + time '03:00:00';
    ELSE
        v_cutoff_wib := v_now_wib::date + time '03:00:00';
    END IF;
    v_cutoff := timezone('Asia/Jakarta', v_cutoff_wib);

    SELECT coalesce(json_agg(
        json_build_object(
            'id', m.id,
            'sender_id', m.sender_id,
            'recipient_id', m.recipient_id,
            'sender_name', s.name,
            'sender_avatar', s.avatar_url,
            'body', m.body,
            'image_path', m.image_path,
            'audio_path', m.audio_path,
            'audio_ms', m.audio_ms,
            'audio_wave', m.audio_wave,
            'audio_played_at', m.audio_played_at,
            'sticker_url', m.sticker_url,
            'reply_to_id', m.reply_to_id,
            'reply_to_name', m.reply_to_name,
            'reply_to_snippet', m.reply_to_snippet,
            'created_at', m.created_at,
            'delivered_at', m.delivered_at,
            'read_at', m.read_at,
            'edited_at', m.edited_at,
            'deleted_at', m.deleted_at
        ) ORDER BY m.created_at ASC
    ), '[]'::json) INTO v_res
    FROM public.private_chat_messages m
    JOIN public.outlet_staff s ON s.id = m.sender_id
    WHERE ((m.sender_id = v_my_id AND m.recipient_id = p_partner_id)
       OR (m.sender_id = p_partner_id AND m.recipient_id = v_my_id))
      AND m.created_at >= v_cutoff;

    RETURN v_res;
END;
$function$;

CREATE OR REPLACE FUNCTION public.developer_private_chat_ambil_pesan(p_user_a uuid, p_user_b uuid)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $function$
DECLARE
    v_cutoff_wib timestamp;
    v_cutoff timestamptz;
    v_now_wib timestamp;
    v_res jsonb;
BEGIN
    IF NOT public.is_developer(auth.uid()) THEN
        RAISE EXCEPTION 'Akses ditolak: Hanya role developer yang diizinkan memantau percakapan.';
    END IF;

    v_now_wib := timezone('Asia/Jakarta', now());
    IF extract(hour from v_now_wib) < 3 THEN
        v_cutoff_wib := (v_now_wib::date - 1) + time '03:00:00';
    ELSE
        v_cutoff_wib := v_now_wib::date + time '03:00:00';
    END IF;
    v_cutoff := timezone('Asia/Jakarta', v_cutoff_wib);

    SELECT coalesce(json_agg(
        json_build_object(
            'id', m.id,
            'sender_id', m.sender_id,
            'recipient_id', m.recipient_id,
            'sender_name', s.name,
            'sender_avatar', s.avatar_url,
            'body', m.body,
            'image_path', m.image_path,
            'audio_path', m.audio_path,
            'audio_ms', m.audio_ms,
            'audio_wave', m.audio_wave,
            'audio_played_at', m.audio_played_at,
            'sticker_url', m.sticker_url,
            'reply_to_id', m.reply_to_id,
            'reply_to_name', m.reply_to_name,
            'reply_to_snippet', m.reply_to_snippet,
            'created_at', m.created_at,
            'delivered_at', m.delivered_at,
            'read_at', m.read_at,
            'edited_at', m.edited_at,
            'deleted_at', m.deleted_at
        ) ORDER BY m.created_at ASC
    ), '[]'::json) INTO v_res
    FROM public.private_chat_messages m
    JOIN public.outlet_staff s ON s.id = m.sender_id
    WHERE ((m.sender_id = p_user_a AND m.recipient_id = p_user_b)
       OR (m.sender_id = p_user_b AND m.recipient_id = p_user_a))
      AND m.created_at >= v_cutoff;

    RETURN v_res;
END;
$function$;

-- Supaya PostgREST langsung mengenali signature private_chat_kirim yang baru.
NOTIFY pgrst, 'reload schema';

COMMIT;

-- ROLLBACK (manual, bila perlu):
--   DROP FUNCTION public.private_chat_kirim(uuid, text, text, uuid, text, integer, text, text);
--   lalu jalankan ulang definisi 7-parameter lama (lihat pg_get_functiondef sebelum migrasi);
--   DROP TRIGGER trg_chat_nisan_buang_stiker ON public.chat_messages;
--   DROP TRIGGER trg_pcm_nisan_buang_stiker ON public.private_chat_messages;
--   DROP TRIGGER trg_area_nisan_buang_stiker ON public.area_chat_messages;
--   DROP FUNCTION public.chat_nisan_buang_stiker();
--   ALTER TABLE ... DROP CONSTRAINT ..._stiker_klipy; ALTER TABLE ... DROP COLUMN sticker_url;
