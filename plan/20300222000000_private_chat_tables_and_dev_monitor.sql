-- ====================================================================
-- MIGRATION: Fitur Chat Pribadi (1-on-1), Reset 03:00 WIB, Centang Biru,
-- & Mode Pantau Siluman Developer
-- ====================================================================

-- 1. Helper function: apakah user ini developer?
CREATE OR REPLACE FUNCTION public.is_developer(p_uid uuid DEFAULT auth.uid())
RETURNS boolean
LANGUAGE sql
STABLE SECURITY DEFINER
SET search_path TO 'public'
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.outlet_staff
     WHERE id = coalesce(p_uid, auth.uid())
       AND role = 'developer'
  );
$$;

GRANT EXECUTE ON FUNCTION public.is_developer(uuid) TO postgres, anon, authenticated, service_role;

-- 2. Tabel Pesan Chat Pribadi
CREATE TABLE IF NOT EXISTS public.private_chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    recipient_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    body TEXT NOT NULL DEFAULT '',
    image_path TEXT,
    reply_to_id UUID REFERENCES public.private_chat_messages(id) ON DELETE SET NULL,
    reply_to_snippet TEXT,
    reply_to_name TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    delivered_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,
    edited_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT check_not_self_chat CHECK (sender_id <> recipient_id)
);

-- Indeks performa
CREATE INDEX IF NOT EXISTS idx_pcm_pair ON public.private_chat_messages (
    LEAST(sender_id, recipient_id),
    GREATEST(sender_id, recipient_id),
    created_at DESC
);

CREATE INDEX IF NOT EXISTS idx_pcm_recipient_status ON public.private_chat_messages (
    recipient_id,
    delivered_at,
    read_at
);

CREATE INDEX IF NOT EXISTS idx_pcm_created_at ON public.private_chat_messages (
    created_at
);

-- Row Level Security (RLS)
ALTER TABLE public.private_chat_messages ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS private_chat_select ON public.private_chat_messages;
CREATE POLICY private_chat_select ON public.private_chat_messages
FOR SELECT USING (
    sender_id = auth.uid()
    OR recipient_id = auth.uid()
    OR public.is_developer(auth.uid())
);

DROP POLICY IF EXISTS private_chat_insert ON public.private_chat_messages;
CREATE POLICY private_chat_insert ON public.private_chat_messages
FOR INSERT WITH CHECK (
    sender_id = auth.uid()
    AND sender_id <> recipient_id
);

-- Tambahkan ke Supabase Realtime
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_publication_tables
    WHERE pubname = 'supabase_realtime' AND schemaname = 'public' AND tablename = 'private_chat_messages'
  ) THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.private_chat_messages;
  END IF;
END $$;

ALTER TABLE public.private_chat_messages REPLICA IDENTITY FULL;

-- 3. RPC: Kirim Pesan Pribadi
CREATE OR REPLACE FUNCTION public.private_chat_kirim(
    p_recipient_id UUID,
    p_body TEXT,
    p_image_path TEXT DEFAULT NULL,
    p_reply_to_id UUID DEFAULT NULL
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
    IF coalesce(btrim(p_body), '') = '' AND coalesce(btrim(p_image_path), '') = '' THEN
        RAISE EXCEPTION 'Isi pesan atau foto tidak boleh kosong.';
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
    END IF;

    INSERT INTO public.private_chat_messages (
        sender_id,
        recipient_id,
        body,
        image_path,
        reply_to_id,
        reply_to_name,
        reply_to_snippet,
        created_at
    ) VALUES (
        v_sender_id,
        p_recipient_id,
        btrim(p_body),
        p_image_path,
        p_reply_to_id,
        v_reply_name,
        v_reply_snippet,
        now()
    )
    RETURNING * INTO v_new_msg;

    RETURN to_jsonb(v_new_msg);
END;
$$;

GRANT EXECUTE ON FUNCTION public.private_chat_kirim(UUID, TEXT, TEXT, UUID) TO authenticated, service_role;

-- 4. RPC: Tandai Tersampaikan (Centang 2 Abu)
CREATE OR REPLACE FUNCTION public.private_chat_tandai_tersampaikan(p_sender_id UUID)
RETURNS int
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
DECLARE
    v_my_id UUID := auth.uid();
    v_updated int := 0;
BEGIN
    IF v_my_id IS NULL THEN
        RETURN 0;
    END IF;

    UPDATE public.private_chat_messages
       SET delivered_at = now()
     WHERE recipient_id = v_my_id
       AND sender_id = p_sender_id
       AND delivered_at IS NULL;

    GET DIAGNOSTICS v_updated = ROW_COUNT;
    RETURN v_updated;
END;
$$;

GRANT EXECUTE ON FUNCTION public.private_chat_tandai_tersampaikan(UUID) TO authenticated, service_role;

-- 5. RPC: Tandai Dibaca (Centang 2 Biru)
CREATE OR REPLACE FUNCTION public.private_chat_tandai_dibaca(p_sender_id UUID)
RETURNS int
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
DECLARE
    v_my_id UUID := auth.uid();
    v_updated int := 0;
BEGIN
    IF v_my_id IS NULL THEN
        RETURN 0;
    END IF;

    UPDATE public.private_chat_messages
       SET read_at = now(),
           delivered_at = coalesce(delivered_at, now())
     WHERE recipient_id = v_my_id
       AND sender_id = p_sender_id
       AND read_at IS NULL;

    GET DIAGNOSTICS v_updated = ROW_COUNT;
    RETURN v_updated;
END;
$$;

GRANT EXECUTE ON FUNCTION public.private_chat_tandai_dibaca(UUID) TO authenticated, service_role;

-- 6. RPC: Daftar Percakapan Aktif untuk Pengguna Login
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

    -- Cutoff 03:00 AM WIB
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

-- 7. RPC: Ambil Seluruh Pesan 1-on-1 dengan Partner (Siklus Aktif 03:00 WIB)
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

    -- Cutoff 03:00 AM WIB
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

-- 8. RPC: Developer God-Mode - Lihat SEMUA Percakapan Pribadi yang Aktif
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
    -- Verifikasi role developer
    IF NOT public.is_developer(auth.uid()) THEN
        RAISE EXCEPTION 'Akses ditolak: Hanya role developer yang diizinkan mengakses pantauan obrolan.';
    END IF;

    -- Cutoff 03:00 AM WIB
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

-- 9. RPC: Developer God-Mode - Intip Pesan Antara Dua Pengguna Secara SILUMAN
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
    -- Verifikasi role developer
    IF NOT public.is_developer(auth.uid()) THEN
        RAISE EXCEPTION 'Akses ditolak: Hanya role developer yang diizinkan memantau percakapan.';
    END IF;

    -- Cutoff 03:00 AM WIB
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

-- 10. Perbarui Fungsi Pembersih Harian 03:00 WIB agar ikut membersihkan private chat
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
  -- 1. Hitung batas cutoff 03:00 AM WIB (Asia/Jakarta)
  v_now_wib := timezone('Asia/Jakarta', now());
  IF extract(hour from v_now_wib) < 3 THEN
    v_cutoff_wib := (v_now_wib::date - 1) + time '03:00:00';
  ELSE
    v_cutoff_wib := v_now_wib::date + time '03:00:00';
  END IF;
  v_cutoff := timezone('Asia/Jakarta', v_cutoff_wib);

  -- 2. Kumpulkan path file gambar di bucket chat-media untuk dihapus via Storage API
  SELECT json_agg(replace(image_path, 'chat-media/', ''))
  INTO v_prefixes
  FROM (
    SELECT image_path FROM public.chat_messages WHERE image_path IS NOT NULL AND created_at < v_cutoff
    UNION ALL
    SELECT image_path FROM public.private_chat_messages WHERE image_path IS NOT NULL AND created_at < v_cutoff
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

  -- 3. Hapus record objek di storage.objects dengan mengizinkan query delete
  PERFORM set_config('storage.allow_delete_query', 'true', true);
  DELETE FROM storage.objects
   WHERE bucket_id = 'chat-media'
     AND created_at < v_cutoff;
  GET DIAGNOSTICS v_deleted_storage = ROW_COUNT;

  -- 4. Hapus pesan grup kadaluarsa di public.chat_messages
  DELETE FROM public.chat_messages
   WHERE created_at < v_cutoff;
  GET DIAGNOSTICS v_deleted_messages = ROW_COUNT;

  -- 5. Hapus pesan pribadi kadaluarsa di public.private_chat_messages
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