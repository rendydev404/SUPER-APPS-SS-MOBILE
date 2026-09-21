-- ====================================================================
-- MIGRATION: Auto-Cleanup Harian 03:00 WIB untuk Area Chat Messages
-- File: 20300229000000_area_chat_auto_cleanup_03wib.sql
--
-- Menambahkan public.area_chat_messages dan berkas media terkait ke dalam
-- fungsi pembersih harian public.chat_bersihkan_pesan_kadaluarsa().
--
-- Karakteristik:
-- - Idempoten, aman dieksekusi berulang kali.
-- - Menghapus pesan grup, pesan pribadi, dan pesan area yang melewati batas 03:00 WIB.
-- - Otomatis menghapus reaksi & status baca (karena FOREIGN KEY ... ON DELETE CASCADE).
-- - Menghapus file fisik di storage bucket 'chat-media' via Supabase Storage API & storage.objects.
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
  -- 1. Hitung batas cutoff 03:00 AM WIB (Asia/Jakarta)
  v_now_wib := timezone('Asia/Jakarta', now());
  IF extract(hour from v_now_wib) < 3 THEN
    v_cutoff_wib := (v_now_wib::date - 1) + time '03:00:00';
  ELSE
    v_cutoff_wib := v_now_wib::date + time '03:00:00';
  END IF;
  v_cutoff := timezone('Asia/Jakarta', v_cutoff_wib);

  -- 2. Kumpulkan berkas gambar & audio kadaluarsa dari seluruh tabel chat
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

  -- 3. Hapus berkas fisik di Supabase Storage via HTTP DELETE jika kredensial vault tersedia
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
        RAISE NOTICE 'Peringatan: Gagal memanggil net.http_delete storage cleanup: %', SQLERRM;
      END;
    END IF;
  END IF;

  -- 4. Hapus record objek di storage.objects
  PERFORM set_config('storage.allow_delete_query', 'true', true);
  DELETE FROM storage.objects
   WHERE bucket_id = 'chat-media'
     AND created_at < v_cutoff;
  GET DIAGNOSTICS v_deleted_storage = ROW_COUNT;

  -- 5. Hapus pesan kadaluarsa di public.chat_messages (CASCADE ke reactions & reads)
  DELETE FROM public.chat_messages
   WHERE created_at < v_cutoff;
  GET DIAGNOSTICS v_deleted_messages = ROW_COUNT;

  -- 6. Hapus pesan kadaluarsa di public.private_chat_messages (CASCADE ke reactions & reads)
  DELETE FROM public.private_chat_messages
   WHERE created_at < v_cutoff;
  GET DIAGNOSTICS v_deleted_private_messages = ROW_COUNT;

  -- 7. Hapus pesan kadaluarsa di public.area_chat_messages (CASCADE ke reactions & reads)
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

-- Pastikan jadwal pg_cron tetap aktif (idempoten)
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_cron') THEN
    IF EXISTS (SELECT 1 FROM cron.job WHERE jobname = 'chat-auto-cleanup-03wib') THEN
      PERFORM cron.unschedule('chat-auto-cleanup-03wib');
    END IF;
    PERFORM cron.schedule(
      'chat-auto-cleanup-03wib',
      '5 20 * * *', -- 20:05 UTC = 03:05 WIB
      'SELECT public.chat_bersihkan_pesan_kadaluarsa();'
    );

    IF EXISTS (SELECT 1 FROM cron.job WHERE jobname = 'chat-auto-cleanup-hourly') THEN
      PERFORM cron.unschedule('chat-auto-cleanup-hourly');
    END IF;
    PERFORM cron.schedule(
      'chat-auto-cleanup-hourly',
      '15 * * * *', -- Tiap jam menit 15
      'SELECT public.chat_bersihkan_pesan_kadaluarsa();'
    );
  END IF;
EXCEPTION WHEN OTHERS THEN
  RAISE NOTICE 'pg_cron tidak aktif atau cron.schedule dilewati: %', SQLERRM;
END $$;
