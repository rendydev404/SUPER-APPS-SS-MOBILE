-- ====================================================================
-- MIGRATION: Status "sudah diputar" untuk voice note
--
-- GRUP  : tabel `chat_voice_plays` mencatat siapa saja yang sudah mendengarkan
--         sebuah pesan suara; ditampilkan di lembar Info Pesan.
-- PRIBADI: satu kolom `audio_played_at` pada pesannya sendiri sudah cukup —
--         obrolan berdua hanya punya satu pendengar, dan tabel terpisah untuk
--         itu hanya menambah baris yang isinya selalu satu.
--
-- Mode pantau developer TIDAK pernah menulis ke keduanya: RPC siluman tidak
-- disentuh migrasi ini, dan aplikasi sengaja tidak memanggil penanda ini dari
-- layar pantau. Memutar rekaman di sana tetap tidak meninggalkan jejak.
-- ====================================================================

-- 1. Siapa yang sudah mendengarkan pesan suara grup
CREATE TABLE IF NOT EXISTS public.chat_voice_plays (
    message_id UUID NOT NULL REFERENCES public.chat_messages(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    played_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (message_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_cvp_message ON public.chat_voice_plays (message_id);

ALTER TABLE public.chat_voice_plays ENABLE ROW LEVEL SECURITY;

-- Catatan pendengar boleh dibaca semua anggota — itu memang gunanya: pengirim
-- ingin tahu suaranya sudah didengar siapa. Menulisnya hanya boleh atas nama
-- diri sendiri.
DROP POLICY IF EXISTS chat_voice_plays_select ON public.chat_voice_plays;
CREATE POLICY chat_voice_plays_select
    ON public.chat_voice_plays FOR SELECT
    TO authenticated
    USING (true);

DROP POLICY IF EXISTS chat_voice_plays_insert_self ON public.chat_voice_plays;
CREATE POLICY chat_voice_plays_insert_self
    ON public.chat_voice_plays FOR INSERT
    TO authenticated
    WITH CHECK (user_id = (SELECT auth.uid()));

-- Ikut disiarkan realtime supaya pengirim melihat daftarnya bertambah tanpa
-- menutup dan membuka lembar Info Pesan.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables
         WHERE pubname = 'supabase_realtime'
           AND schemaname = 'public'
           AND tablename = 'chat_voice_plays'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.chat_voice_plays;
    END IF;
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'Lewati publikasi realtime chat_voice_plays: %', SQLERRM;
END;
$$;

-- 2. Tandai pesan suara grup sebagai sudah diputar oleh pemanggil.
--    Pengirimnya sendiri tidak dicatat: mendengar ulang rekaman sendiri bukan
--    kabar yang berguna bagi siapa pun.
CREATE OR REPLACE FUNCTION public.chat_tandai_suara_diputar(p_message_id UUID)
RETURNS boolean
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_sender UUID;
    v_audio TEXT;
BEGIN
    IF v_uid IS NULL THEN
        RETURN false;
    END IF;

    SELECT sender_id, audio_path INTO v_sender, v_audio
      FROM public.chat_messages WHERE id = p_message_id;

    IF v_audio IS NULL OR v_sender = v_uid THEN
        RETURN false;
    END IF;

    INSERT INTO public.chat_voice_plays (message_id, user_id)
    VALUES (p_message_id, v_uid)
    ON CONFLICT (message_id, user_id) DO NOTHING;

    RETURN true;
END;
$$;

GRANT EXECUTE ON FUNCTION public.chat_tandai_suara_diputar(UUID) TO authenticated, service_role;

-- 3. Daftar pendengar sebuah pesan suara grup.
--    Bentuk barisnya sengaja sama dengan daftar pembaca di `chat_info_pesan`
--    supaya lembar Info Pesan memakai baris tampilan yang sama persis.
CREATE OR REPLACE FUNCTION public.chat_suara_pendengar(p_message_id UUID)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
DECLARE
    v_res jsonb;
BEGIN
    IF auth.uid() IS NULL THEN
        RETURN '[]'::jsonb;
    END IF;

    SELECT coalesce(json_agg(
        json_build_object(
            'user_id', p.user_id,
            'nama', s.name,
            'display_username', s.display_name,
            'avatar_url', s.avatar_url,
            'role', s.role,
            'outlet_nama', o.name,
            'read_at', p.played_at
        ) ORDER BY p.played_at ASC
    ), '[]'::json) INTO v_res
    FROM public.chat_voice_plays p
    JOIN public.outlet_staff s ON s.id = p.user_id
    LEFT JOIN public.outlets o ON o.id = s.outlet_id
    WHERE p.message_id = p_message_id;

    RETURN v_res;
END;
$$;

GRANT EXECUTE ON FUNCTION public.chat_suara_pendengar(UUID) TO authenticated, service_role;

-- 4. Chat pribadi: satu kolom penanda sudah didengar.
ALTER TABLE public.private_chat_messages
    ADD COLUMN IF NOT EXISTS audio_played_at TIMESTAMPTZ;

CREATE OR REPLACE FUNCTION public.private_chat_tandai_suara_diputar(p_id UUID)
RETURNS boolean
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_ubah int;
BEGIN
    IF v_uid IS NULL THEN
        RETURN false;
    END IF;

    -- HANYA penerima yang boleh menandai. Pengirim yang mendengar ulang
    -- rekamannya sendiri tidak boleh membuat penanda "sudah didengar" menyala
    -- di layarnya sendiri.
    UPDATE public.private_chat_messages
       SET audio_played_at = coalesce(audio_played_at, now())
     WHERE id = p_id
       AND recipient_id = v_uid
       AND audio_path IS NOT NULL;

    GET DIAGNOSTICS v_ubah = ROW_COUNT;
    RETURN v_ubah > 0;
END;
$$;

GRANT EXECUTE ON FUNCTION public.private_chat_tandai_suara_diputar(UUID) TO authenticated, service_role;

-- 5. RPC pengambil pesan pribadi ikut membawa penanda itu.
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
            'audio_played_at', m.audio_played_at,
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

-- 6. Pantau developer ikut melihat penandanya (baca saja, tetap siluman).
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
            'audio_played_at', m.audio_played_at,
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

-- ROLLBACK:
--   DROP FUNCTION IF EXISTS public.chat_suara_pendengar(UUID);
--   DROP FUNCTION IF EXISTS public.chat_tandai_suara_diputar(UUID);
--   DROP FUNCTION IF EXISTS public.private_chat_tandai_suara_diputar(UUID);
--   DROP TABLE IF EXISTS public.chat_voice_plays;
--   ALTER TABLE public.private_chat_messages DROP COLUMN IF EXISTS audio_played_at;
