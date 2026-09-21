-- ====================================================================
-- MIGRATION: Fitur Chat Khusus Area Manager & Crew (Grup Area)
-- Idempoten, aman diulang. Jalankan di Supabase SQL Editor (project khpkoreaaucvyqfhynfq).
-- ====================================================================

SET lock_timeout = '5s';

-- 1. Tabel Pesan Chat Area
CREATE TABLE IF NOT EXISTS public.area_chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    area_id TEXT NOT NULL, -- identifier grup area, misal 'abu_bakar', 'muchtar', 'chairul_rizky', 'tri_rizky', 'mulyadi'
    sender_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    sender_name TEXT NOT NULL DEFAULT '',
    sender_avatar TEXT,
    sender_role TEXT,
    outlet_name TEXT,
    body TEXT NOT NULL DEFAULT '',
    image_path TEXT,
    audio_path TEXT,
    audio_ms INTEGER,
    audio_wave TEXT,
    reply_to_id UUID REFERENCES public.area_chat_messages(id) ON DELETE SET NULL,
    reply_to_snippet TEXT,
    reply_to_name TEXT,
    reply_to_image TEXT,
    mentions JSONB DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    edited_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    deleted_by_name TEXT,
    CONSTRAINT check_area_chat_isi_ada CHECK (
        deleted_at IS NOT NULL OR btrim(body) <> '' OR image_path IS NOT NULL OR audio_path IS NOT NULL
    )
);

COMMENT ON TABLE public.area_chat_messages IS
  'Ruang obrolan khusus Area Manager dan seluruh crew & leader cabang binaannya. Siklus harian reset 03:00 WIB.';

-- Indeks performa
CREATE INDEX IF NOT EXISTS idx_acm_area_created_at
  ON public.area_chat_messages (area_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_acm_sender
  ON public.area_chat_messages (sender_id);

CREATE INDEX IF NOT EXISTS idx_acm_created_at
  ON public.area_chat_messages (created_at DESC);

-- Trigger Snapshot Identitas Pengirim & Kutipan
CREATE OR REPLACE FUNCTION public.area_chat_fill_sender()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_uid uuid := auth.uid();
  v_staff record;
  v_asal record;
BEGIN
  IF v_uid IS NULL THEN
    RAISE EXCEPTION 'Tidak ada sesi aktif.';
  END IF;

  SELECT s.id,
         coalesce(nullif(btrim(s.display_name), ''), s.name) AS nama,
         s.avatar_url,
         s.role,
         o.name AS outlet_nama
    INTO v_staff
    FROM public.outlet_staff s
    LEFT JOIN public.outlets o ON o.id = s.outlet_id
   WHERE s.id = v_uid;

  NEW.sender_id := v_uid;
  NEW.sender_name := coalesce(v_staff.nama, 'Staf');
  NEW.sender_avatar := v_staff.avatar_url;
  NEW.sender_role := v_staff.role;
  NEW.outlet_name := v_staff.outlet_nama;
  NEW.created_at := coalesce(NEW.created_at, now());

  -- Validasi image_path harus berawalan auth.uid()
  IF NEW.image_path IS NOT NULL AND NOT (NEW.image_path LIKE (v_uid::text || '/%')) THEN
    RAISE EXCEPTION 'Path foto tidak sah.';
  END IF;

  -- Validasi audio_path harus berawalan auth.uid()
  IF NEW.audio_path IS NOT NULL AND NOT (NEW.audio_path LIKE (v_uid::text || '/%')) THEN
    RAISE EXCEPTION 'Path audio tidak sah.';
  END IF;

  -- Snapshot kutipan reply jika ada
  IF NEW.reply_to_id IS NOT NULL THEN
    SELECT m.sender_name,
           coalesce(nullif(btrim(m.body), ''), CASE WHEN m.image_path IS NOT NULL THEN '[Foto]' WHEN m.audio_path IS NOT NULL THEN '[Pesan Suara]' ELSE '' END) AS snippet,
           m.image_path
      INTO v_asal
      FROM public.area_chat_messages m
     WHERE m.id = NEW.reply_to_id;

    IF FOUND THEN
      NEW.reply_to_name := v_asal.sender_name;
      NEW.reply_to_snippet := substring(v_asal.snippet from 1 for 140);
      NEW.reply_to_image := v_asal.image_path;
    ELSE
      NEW.reply_to_id := NULL;
    END IF;
  END IF;

  RETURN NEW;
END;
$$;

REVOKE ALL ON FUNCTION public.area_chat_fill_sender() FROM PUBLIC, anon;

DROP TRIGGER IF EXISTS trg_area_chat_fill_sender ON public.area_chat_messages;
CREATE TRIGGER trg_area_chat_fill_sender
  BEFORE INSERT ON public.area_chat_messages
  FOR EACH ROW EXECUTE FUNCTION public.area_chat_fill_sender();

-- 2. Tabel Reaksi Pesan Area
CREATE TABLE IF NOT EXISTS public.area_chat_message_reactions (
    message_id UUID NOT NULL REFERENCES public.area_chat_messages(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    user_name TEXT NOT NULL DEFAULT '',
    emoji TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (message_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_acmr_message
  ON public.area_chat_message_reactions (message_id);

CREATE OR REPLACE FUNCTION public.area_chat_reaction_fill_user()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_uid uuid := auth.uid();
  v_nama text;
BEGIN
  IF v_uid IS NULL THEN
    RAISE EXCEPTION 'Tidak ada sesi aktif.';
  END IF;

  SELECT coalesce(nullif(btrim(display_name), ''), name) INTO v_nama
    FROM public.outlet_staff
   WHERE id = v_uid;

  NEW.user_id := v_uid;
  NEW.user_name := coalesce(v_nama, 'Staf');
  NEW.created_at := now();
  RETURN NEW;
END;
$$;

REVOKE ALL ON FUNCTION public.area_chat_reaction_fill_user() FROM PUBLIC, anon;

DROP TRIGGER IF EXISTS trg_area_chat_reaction_fill_user ON public.area_chat_message_reactions;
CREATE TRIGGER trg_area_chat_reaction_fill_user
  BEFORE INSERT OR UPDATE ON public.area_chat_message_reactions
  FOR EACH ROW EXECUTE FUNCTION public.area_chat_reaction_fill_user();

-- 3. Tabel Pembacaan Pesan Area
CREATE TABLE IF NOT EXISTS public.area_chat_message_reads (
    message_id UUID NOT NULL REFERENCES public.area_chat_messages(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    user_name TEXT NOT NULL DEFAULT '',
    user_avatar TEXT,
    read_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (message_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_acm_reads_msg ON public.area_chat_message_reads (message_id);
CREATE INDEX IF NOT EXISTS idx_acm_reads_usr ON public.area_chat_message_reads (user_id);

CREATE OR REPLACE FUNCTION public.area_chat_read_fill_user()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_uid uuid := auth.uid();
  v_staff record;
BEGIN
  IF v_uid IS NULL THEN
    RAISE EXCEPTION 'Tidak ada sesi aktif.';
  END IF;

  SELECT coalesce(nullif(btrim(display_name), ''), name) AS nama,
         avatar_url
    INTO v_staff
    FROM public.outlet_staff
   WHERE id = v_uid;

  NEW.user_id := v_uid;
  NEW.user_name := coalesce(v_staff.nama, 'Staf');
  NEW.user_avatar := v_staff.avatar_url;
  NEW.read_at := now();
  RETURN NEW;
END;
$$;

REVOKE ALL ON FUNCTION public.area_chat_read_fill_user() FROM PUBLIC, anon;

DROP TRIGGER IF EXISTS trg_area_chat_read_fill_user ON public.area_chat_message_reads;
CREATE TRIGGER trg_area_chat_read_fill_user
  BEFORE INSERT ON public.area_chat_message_reads
  FOR EACH ROW EXECUTE FUNCTION public.area_chat_read_fill_user();

-- 4. Keamanan RLS
ALTER TABLE public.area_chat_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.area_chat_message_reactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.area_chat_message_reads ENABLE ROW LEVEL SECURITY;

-- SELECT
DROP POLICY IF EXISTS area_chat_select ON public.area_chat_messages;
CREATE POLICY area_chat_select ON public.area_chat_messages
  FOR SELECT TO authenticated USING (true);

DROP POLICY IF EXISTS area_chat_reactions_select ON public.area_chat_message_reactions;
CREATE POLICY area_chat_reactions_select ON public.area_chat_message_reactions
  FOR SELECT TO authenticated USING (true);

DROP POLICY IF EXISTS area_chat_reads_select ON public.area_chat_message_reads;
CREATE POLICY area_chat_reads_select ON public.area_chat_message_reads
  FOR SELECT TO authenticated USING (true);

-- INSERT
DROP POLICY IF EXISTS area_chat_insert ON public.area_chat_messages;
CREATE POLICY area_chat_insert ON public.area_chat_messages
  FOR INSERT TO authenticated WITH CHECK (sender_id = auth.uid());

DROP POLICY IF EXISTS area_chat_reactions_insert ON public.area_chat_message_reactions;
CREATE POLICY area_chat_reactions_insert ON public.area_chat_message_reactions
  FOR INSERT TO authenticated WITH CHECK (user_id = auth.uid());

DROP POLICY IF EXISTS area_chat_reads_insert ON public.area_chat_message_reads;
CREATE POLICY area_chat_reads_insert ON public.area_chat_message_reads
  FOR INSERT TO authenticated WITH CHECK (user_id = auth.uid());

-- UPDATE (Soft delete nisan)
DROP POLICY IF EXISTS area_chat_update ON public.area_chat_messages;
CREATE POLICY area_chat_update ON public.area_chat_messages
  FOR UPDATE TO authenticated USING (sender_id = auth.uid() OR public.is_developer(auth.uid()));

-- DELETE
DROP POLICY IF EXISTS area_chat_delete ON public.area_chat_messages;
CREATE POLICY area_chat_delete ON public.area_chat_messages
  FOR DELETE TO authenticated USING (sender_id = auth.uid() OR public.is_developer(auth.uid()));

DROP POLICY IF EXISTS area_chat_reactions_delete ON public.area_chat_message_reactions;
CREATE POLICY area_chat_reactions_delete ON public.area_chat_message_reactions
  FOR DELETE TO authenticated USING (user_id = auth.uid());

-- 5. Realtime Publication
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_publication_tables
    WHERE pubname = 'supabase_realtime' AND schemaname = 'public' AND tablename = 'area_chat_messages'
  ) THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.area_chat_messages;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM pg_publication_tables
    WHERE pubname = 'supabase_realtime' AND schemaname = 'public' AND tablename = 'area_chat_message_reactions'
  ) THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.area_chat_message_reactions;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM pg_publication_tables
    WHERE pubname = 'supabase_realtime' AND schemaname = 'public' AND tablename = 'area_chat_message_reads'
  ) THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.area_chat_message_reads;
  END IF;
END $$;

ALTER TABLE public.area_chat_messages REPLICA IDENTITY FULL;
ALTER TABLE public.area_chat_message_reactions REPLICA IDENTITY FULL;
ALTER TABLE public.area_chat_message_reads REPLICA IDENTITY FULL;

-- ====================================================================
-- 6. Integrasi Pembersih Otomatis Harian 03:00 WIB (chat_bersihkan_pesan_kadaluarsa)
--    Menghapus pesan dan file media yang lewat batas siklus operasional harian (03:00 WIB).
--    Mencakup: chat global (chat_messages), chat pribadi (private_chat_messages),
--    dan chat area (area_chat_messages).
-- ====================================================================
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
  v_deleted_area_messages int := 0;
  v_deleted_storage int := 0;
  v_proj_url text;
  v_srv_key text;
  v_result jsonb;
BEGIN
  -- Hitung batas cutoff 03:00 AM WIB (Asia/Jakarta)
  v_now_wib := timezone('Asia/Jakarta', now());
  IF extract(hour from v_now_wib) < 3 THEN
    v_cutoff_wib := (v_now_wib::date - 1) + time '03:00:00';
  ELSE
    v_cutoff_wib := v_now_wib::date + time '03:00:00';
  END IF;
  v_cutoff := timezone('Asia/Jakarta', v_cutoff_wib);

  -- Kumpulkan berkas gambar & suara kadaluarsa dari seluruh tabel chat (grup, privat, & area)
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
    UNION ALL
    SELECT image_path AS berkas FROM public.area_chat_messages WHERE image_path IS NOT NULL AND created_at < v_cutoff
    UNION ALL
    SELECT audio_path AS berkas FROM public.area_chat_messages WHERE audio_path IS NOT NULL AND created_at < v_cutoff
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

  -- Hapus record objek di storage.objects dengan mengizinkan query delete
  PERFORM set_config('storage.allow_delete_query', 'true', true);
  DELETE FROM storage.objects
   WHERE bucket_id = 'chat-media'
     AND created_at < v_cutoff;
  GET DIAGNOSTICS v_deleted_storage = ROW_COUNT;

  -- Hapus pesan kadaluarsa di public.chat_messages (CASCADE ke reads & reactions)
  DELETE FROM public.chat_messages
   WHERE created_at < v_cutoff;
  GET DIAGNOSTICS v_deleted_messages = ROW_COUNT;

  -- Hapus pesan kadaluarsa di public.private_chat_messages (CASCADE ke reads & reactions)
  DELETE FROM public.private_chat_messages
   WHERE created_at < v_cutoff;
  GET DIAGNOSTICS v_deleted_private_messages = ROW_COUNT;

  -- Hapus pesan kadaluarsa di public.area_chat_messages (CASCADE ke reads & reactions)
  DELETE FROM public.area_chat_messages
   WHERE created_at < v_cutoff;
  GET DIAGNOSTICS v_deleted_area_messages = ROW_COUNT;

  v_result := jsonb_build_object(
    'success', true,
    'cutoff_wib', v_cutoff_wib,
    'cutoff_utc', v_cutoff,
    'deleted_group_messages', v_deleted_messages,
    'deleted_private_messages', v_deleted_private_messages,
    'deleted_area_messages', v_deleted_area_messages,
    'deleted_storage_objects', v_deleted_storage
  );

  RETURN v_result;
END;
$$;

REVOKE ALL ON FUNCTION public.chat_bersihkan_pesan_kadaluarsa() FROM public, anon;
GRANT EXECUTE ON FUNCTION public.chat_bersihkan_pesan_kadaluarsa() TO postgres, service_role;

