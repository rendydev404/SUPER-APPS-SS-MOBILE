-- ====================================================================
-- MIGRATION: Auto Cleanup Chat Messages & Storage pada 03:00 AM WIB
-- Menghapus pesan dan file storage chat yang kadaluarsa (< 03:00 AM WIB siklus aktif)
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

  -- Kumpulkan path file gambar di bucket chat-media untuk dihapus via Storage API
  SELECT json_agg(replace(image_path, 'chat-media/', ''))
  INTO v_prefixes
  FROM public.chat_messages
  WHERE image_path IS NOT NULL AND created_at < v_cutoff;

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
        RAISE NOTICE 'Gagal memanggil net.http_delete: %', SQLERRM;
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

  v_result := jsonb_build_object(
    'success', true,
    'cutoff_wib', v_cutoff_wib,
    'cutoff_utc', v_cutoff,
    'deleted_messages', v_deleted_messages,
    'deleted_storage_objects', v_deleted_storage
  );

  RETURN v_result;
END;
$$;

REVOKE ALL ON FUNCTION public.chat_bersihkan_pesan_kadaluarsa() FROM public, anon;
GRANT EXECUTE ON FUNCTION public.chat_bersihkan_pesan_kadaluarsa() TO postgres, service_role;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM cron.job WHERE jobname = 'chat-cleanup-24h') THEN
    PERFORM cron.unschedule('chat-cleanup-24h');
  END IF;

  IF EXISTS (SELECT 1 FROM cron.job WHERE jobname = 'chat-auto-cleanup-03wib') THEN
    PERFORM cron.unschedule('chat-auto-cleanup-03wib');
  END IF;
  PERFORM cron.schedule(
    'chat-auto-cleanup-03wib',
    '5 20 * * *',
    'SELECT public.chat_bersihkan_pesan_kadaluarsa();'
  );

  IF EXISTS (SELECT 1 FROM cron.job WHERE jobname = 'chat-auto-cleanup-hourly') THEN
    PERFORM cron.unschedule('chat-auto-cleanup-hourly');
  END IF;
  PERFORM cron.schedule(
    'chat-auto-cleanup-hourly',
    '15 * * * *',
    'SELECT public.chat_bersihkan_pesan_kadaluarsa();'
  );
END $$;
