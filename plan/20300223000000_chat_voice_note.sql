-- ====================================================================
-- MIGRATION: Voice Note (Pesan Suara) untuk Chat Grup & Chat Pribadi
--
-- Menambahkan tiga kolom pada kedua tabel pesan:
--   audio_path  : path objek di bucket `chat-media` ("chat-media/<uid>/<uuid>.m4a")
--   audio_ms    : durasi rekaman dalam milidetik (dipakai UI tanpa membuka file)
--   audio_wave  : sampel amplitudo saat merekam, satu digit '0'-'9' per bilah
--                 (maks 56 bilah). Disimpan supaya waveform yang tampil adalah
--                 bentuk suara yang sebenarnya, bukan hiasan acak — dan supaya
--                 penerima tidak perlu mengunduh audio hanya untuk menggambar.
--
-- Mode pantau developer TIDAK mendapat RPC baru: ia memakai
-- `developer_private_chat_ambil_pesan` yang sama, yang di sini hanya ditambahi
-- kolom audio. Sifat silumannya tetap — fungsi ini tidak pernah menyentuh
-- delivered_at/read_at, jadi memutar suara di layar pantau tidak terlihat
-- oleh kedua pihak, persis seperti membaca teksnya.
-- ====================================================================

-- 1. Kolom baru
ALTER TABLE public.chat_messages
    ADD COLUMN IF NOT EXISTS audio_path TEXT,
    ADD COLUMN IF NOT EXISTS audio_ms   INTEGER,
    ADD COLUMN IF NOT EXISTS audio_wave TEXT;

ALTER TABLE public.private_chat_messages
    ADD COLUMN IF NOT EXISTS audio_path TEXT,
    ADD COLUMN IF NOT EXISTS audio_ms   INTEGER,
    ADD COLUMN IF NOT EXISTS audio_wave TEXT;

-- 1b. `chat_messages_isi_ada` (migrasi 20300209000000) menuntut body terisi ATAU
--     ada foto. Voice note tidak punya keduanya, jadi syaratnya diperluas —
--     bukan dibuang: pesan benar-benar kosong tetap ditolak database.
ALTER TABLE public.chat_messages DROP CONSTRAINT IF EXISTS chat_messages_isi_ada;
ALTER TABLE public.chat_messages
    ADD CONSTRAINT chat_messages_isi_ada
    CHECK (deleted_at IS NOT NULL OR btrim(body) <> '' OR image_path IS NOT NULL OR audio_path IS NOT NULL);

-- 2. Bucket `chat-media` harus menerima audio.
--    Hanya disentuh bila bucket memang membatasi mime type; bila NULL (bebas),
--    dibiarkan apa adanya supaya migrasi ini tidak diam-diam melonggarkan aturan.
UPDATE storage.buckets
   SET allowed_mime_types = (
        SELECT array_agg(DISTINCT x)
          FROM unnest(allowed_mime_types || ARRAY['audio/mp4', 'audio/aac', 'audio/mpeg']) AS x
       )
 WHERE id = 'chat-media'
   AND allowed_mime_types IS NOT NULL;

-- 3. Hapus pesan (nisan) harus ikut membuang suaranya.
--    Dipasang sebagai trigger, bukan dengan menulis ulang `chat_hapus_pesan` /
--    `private_chat_hapus_pesan`: keduanya hidup di migrasi lain, dan trigger
--    menutup lubang yang sama tanpa menduplikasi badan fungsi yang bisa berubah.
CREATE OR REPLACE FUNCTION public.chat_nisan_buang_audio()
RETURNS trigger
LANGUAGE plpgsql
SET search_path TO 'public'
AS $$
BEGIN
    IF NEW.deleted_at IS NOT NULL AND OLD.deleted_at IS NULL THEN
        NEW.audio_path := NULL;
        NEW.audio_ms   := NULL;
        NEW.audio_wave := NULL;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_chat_nisan_buang_audio ON public.chat_messages;
CREATE TRIGGER trg_chat_nisan_buang_audio
    BEFORE UPDATE ON public.chat_messages
    FOR EACH ROW EXECUTE FUNCTION public.chat_nisan_buang_audio();

DROP TRIGGER IF EXISTS trg_pcm_nisan_buang_audio ON public.private_chat_messages;
CREATE TRIGGER trg_pcm_nisan_buang_audio
    BEFORE UPDATE ON public.private_chat_messages
    FOR EACH ROW EXECUTE FUNCTION public.chat_nisan_buang_audio();

-- 4. RPC kirim chat pribadi — versi lama (4 argumen) DIBUANG lebih dulu.
--    Menambah parameter ber-default tanpa menghapusnya akan menghasilkan dua
--    fungsi bernama sama, dan panggilan lama dari klien menjadi ambigu.
DROP FUNCTION IF EXISTS public.private_chat_kirim(UUID, TEXT, TEXT, UUID);

CREATE OR REPLACE FUNCTION public.private_chat_kirim(
    p_recipient_id UUID,
    p_body TEXT,
    p_image_path TEXT DEFAULT NULL,
    p_reply_to_id UUID DEFAULT NULL,
    p_audio_path TEXT DEFAULT NULL,
    p_audio_ms INTEGER DEFAULT NULL,
    p_audio_wave TEXT DEFAULT NULL
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
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
       AND coalesce(btrim(p_audio_path), '') = '' THEN
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
        p_reply_to_id,
        v_reply_name,
        v_reply_snippet,
        now()
    )
    RETURNING * INTO v_new_msg;

    RETURN to_jsonb(v_new_msg);
END;
$$;

GRANT EXECUTE ON FUNCTION public.private_chat_kirim(UUID, TEXT, TEXT, UUID, TEXT, INTEGER, TEXT) TO authenticated, service_role;

-- 5. RPC ambil pesan pribadi — tambah kolom audio
CREATE OR REPLACE FUNCTION public.private_chat_ambil_pesan(p_partner_id UUID)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
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
$$;

GRANT EXECUTE ON FUNCTION public.private_chat_ambil_pesan(UUID) TO authenticated, service_role;

-- 6. RPC daftar percakapan pribadi — tandai percakapan yang pesan terakhirnya suara
CREATE OR REPLACE FUNCTION public.private_chat_daftar_percakapan()
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
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

    WITH pasangan AS (
        SELECT
            CASE WHEN sender_id = v_my_id THEN recipient_id ELSE sender_id END AS partner_id,
            id AS message_id,
            sender_id,
            body,
            image_path,
            audio_path,
            audio_ms,
            created_at,
            delivered_at,
            read_at,
            ROW_NUMBER() OVER (
                PARTITION BY CASE WHEN sender_id = v_my_id THEN recipient_id ELSE sender_id END
                ORDER BY created_at DESC
            ) as rn
        FROM public.private_chat_messages
        WHERE (sender_id = v_my_id OR recipient_id = v_my_id)
          AND created_at >= v_cutoff
    ),
    unread AS (
        SELECT
            sender_id AS partner_id,
            COUNT(*) AS unread_count
        FROM public.private_chat_messages
        WHERE recipient_id = v_my_id
          AND read_at IS NULL
          AND created_at >= v_cutoff
        GROUP BY sender_id
    )
    SELECT coalesce(json_agg(
        json_build_object(
            'partner_id', p.partner_id,
            'partner_name', s.name,
            'partner_display_name', s.display_name,
            'partner_role', s.role,
            'partner_avatar', s.avatar_url,
            'partner_outlet', o.name,
            'last_message_id', p.message_id,
            'last_message_body', p.body,
            'last_message_has_image', (p.image_path IS NOT NULL),
            'last_message_has_audio', (p.audio_path IS NOT NULL),
            'last_message_audio_ms', p.audio_ms,
            'last_message_at', p.created_at,
            'last_sender_id', p.sender_id,
            'is_self_last_sender', (p.sender_id = v_my_id),
            'delivered_at', p.delivered_at,
            'read_at', p.read_at,
            'unread_count', coalesce(u.unread_count, 0)
        ) ORDER BY p.created_at DESC
    ), '[]'::json) INTO v_res
    FROM pasangan p
    JOIN public.outlet_staff s ON s.id = p.partner_id
    LEFT JOIN public.outlets o ON o.id = s.outlet_id
    LEFT JOIN unread u ON u.partner_id = p.partner_id
    WHERE p.rn = 1;

    RETURN v_res;
END;
$$;

GRANT EXECUTE ON FUNCTION public.private_chat_daftar_percakapan() TO authenticated, service_role;

-- 7. Pantau developer: daftar percakapan — tandai pesan suara pada cuplikan
CREATE OR REPLACE FUNCTION public.developer_private_chat_semua_percakapan()
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
DECLARE
    v_cutoff_wib timestamp;
    v_cutoff timestamptz;
    v_now_wib timestamp;
    v_res jsonb;
BEGIN
    IF NOT public.is_developer(auth.uid()) THEN
        RAISE EXCEPTION 'Akses ditolak: Hanya role developer yang diizinkan mengakses pantauan obrolan.';
    END IF;

    v_now_wib := timezone('Asia/Jakarta', now());
    IF extract(hour from v_now_wib) < 3 THEN
        v_cutoff_wib := (v_now_wib::date - 1) + time '03:00:00';
    ELSE
        v_cutoff_wib := v_now_wib::date + time '03:00:00';
    END IF;
    v_cutoff := timezone('Asia/Jakarta', v_cutoff_wib);

    WITH pairs AS (
        SELECT
            LEAST(sender_id, recipient_id) AS user_a,
            GREATEST(sender_id, recipient_id) AS user_b,
            id AS last_message_id,
            sender_id AS last_sender_id,
            body AS last_message_body,
            (image_path IS NOT NULL) AS last_has_image,
            (audio_path IS NOT NULL) AS last_has_audio,
            audio_ms AS last_audio_ms,
            created_at,
            delivered_at,
            read_at,
            COUNT(*) OVER (PARTITION BY LEAST(sender_id, recipient_id), GREATEST(sender_id, recipient_id)) as total_messages,
            ROW_NUMBER() OVER (
                PARTITION BY LEAST(sender_id, recipient_id), GREATEST(sender_id, recipient_id)
                ORDER BY created_at DESC
            ) as rn
        FROM public.private_chat_messages
        WHERE created_at >= v_cutoff
    )
    SELECT coalesce(json_agg(
        json_build_object(
            'user_a_id', p.user_a,
            'user_a_name', sa.name,
            'user_a_role', sa.role,
            'user_a_outlet', oa.name,
            'user_a_avatar', sa.avatar_url,
            'user_b_id', p.user_b,
            'user_b_name', sb.name,
            'user_b_role', sb.role,
            'user_b_outlet', ob.name,
            'user_b_avatar', sb.avatar_url,
            'last_message_id', p.last_message_id,
            'last_message_body', p.last_message_body,
            'last_has_image', p.last_has_image,
            'last_has_audio', p.last_has_audio,
            'last_audio_ms', p.last_audio_ms,
            'last_sender_id', p.last_sender_id,
            'last_sender_name', CASE WHEN p.last_sender_id = p.user_a THEN sa.name ELSE sb.name END,
            'last_message_at', p.created_at,
            'total_messages', p.total_messages,
            'delivered_at', p.delivered_at,
            'read_at', p.read_at
        ) ORDER BY p.created_at DESC
    ), '[]'::json) INTO v_res
    FROM pairs p
    JOIN public.outlet_staff sa ON sa.id = p.user_a
    LEFT JOIN public.outlets oa ON oa.id = sa.outlet_id
    JOIN public.outlet_staff sb ON sb.id = p.user_b
    LEFT JOIN public.outlets ob ON ob.id = sb.outlet_id
    WHERE p.rn = 1;

    RETURN v_res;
END;
$$;

GRANT EXECUTE ON FUNCTION public.developer_private_chat_semua_percakapan() TO authenticated, service_role;

-- 8. Pantau developer: isi percakapan — tambah kolom audio, tetap tanpa menyentuh
--    delivered_at/read_at (siluman).
CREATE OR REPLACE FUNCTION public.developer_private_chat_ambil_pesan(
    p_user_a UUID,
    p_user_b UUID
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
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
$$;

GRANT EXECUTE ON FUNCTION public.developer_private_chat_ambil_pesan(UUID, UUID) TO authenticated, service_role;

-- 9. Pembersih 03:00 WIB ikut menghapus berkas suara di Storage.
--    Langkah DELETE storage.objects di bawah sebenarnya sudah menyapu apa pun
--    yang lewat cutoff, tapi daftar prefix inilah yang menghapus berkas fisik
--    lewat Storage API — tanpa audio_path, rekaman akan tertinggal di sana.
CREATE OR REPLACE FUNCTION public.chat_bersihkan_pesan_kadaluarsa()
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
  v_now_wib timestamp;
  v_cutoff_wib timestamp;
  v_cutoff timestamptz;
  v_prefixes jsonb;
  v_deleted_messages int := 0;
  v_deleted_private_messages int := 0;
  v_deleted_storage int := 0;
  v_proj_url text;
  v_srv_key text;
  v_result jsonb;
BEGIN
  v_now_wib := timezone('Asia/Jakarta', now());
  IF extract(hour from v_now_wib) < 3 THEN
    v_cutoff_wib := (v_now_wib::date - 1) + time '03:00:00';
  ELSE
    v_cutoff_wib := v_now_wib::date + time '03:00:00';
  END IF;
  v_cutoff := timezone('Asia/Jakarta', v_cutoff_wib);

  SELECT json_agg(replace(berkas, 'chat-media/', ''))
  INTO v_prefixes
  FROM (
    SELECT image_path AS berkas FROM public.chat_messages WHERE image_path IS NOT NULL AND created_at < v_cutoff
    UNION ALL
    SELECT audio_path AS berkas FROM public.chat_messages WHERE audio_path IS NOT NULL AND created_at < v_cutoff
    UNION ALL
    SELECT image_path AS berkas FROM public.private_chat_messages WHERE image_path IS NOT NULL AND created_at < v_cutoff
    UNION ALL
    SELECT audio_path AS berkas FROM public.private_chat_messages WHERE audio_path IS NOT NULL AND created_at < v_cutoff
  ) t;

  IF v_prefixes IS NOT NULL AND jsonb_array_length(v_prefixes) > 0 THEN
    SELECT decrypted_secret INTO v_proj_url FROM vault.decrypted_secrets WHERE name = 'project_url';
    SELECT decrypted_secret INTO v_srv_key FROM vault.decrypted_secrets WHERE name = 'service_role_key';

    IF v_proj_url IS NOT NULL AND v_srv_key IS NOT NULL THEN
      BEGIN
        PERFORM net.http_delete(
          url := v_proj_url || '/storage/v1/object/chat-media',
          headers := jsonb_build_object(
            'Authorization', 'Bearer ' || v_srv_key,
            'apikey', v_srv_key,
            'Content-Type', 'application/json'
          ),
          body := jsonb_build_object('prefixes', v_prefixes)
        );
      EXCEPTION WHEN OTHERS THEN
        RAISE NOTICE 'Peringatan: Gagal memanggil net.http_delete: %', SQLERRM;
      END;
    END IF;
  END IF;

  PERFORM set_config('storage.allow_delete_query', 'true', true);
  DELETE FROM storage.objects
   WHERE bucket_id = 'chat-media'
     AND created_at < v_cutoff;
  GET DIAGNOSTICS v_deleted_storage = ROW_COUNT;

  DELETE FROM public.chat_messages
   WHERE created_at < v_cutoff;
  GET DIAGNOSTICS v_deleted_messages = ROW_COUNT;

  DELETE FROM public.private_chat_messages
   WHERE created_at < v_cutoff;
  GET DIAGNOSTICS v_deleted_private_messages = ROW_COUNT;

  v_result := jsonb_build_object(
    'success', true,
    'cutoff_wib', v_cutoff_wib,
    'cutoff_utc', v_cutoff,
    'deleted_group_messages', v_deleted_messages,
    'deleted_private_messages', v_deleted_private_messages,
    'deleted_storage_objects', v_deleted_storage
  );

  RETURN v_result;
END;
$$;
