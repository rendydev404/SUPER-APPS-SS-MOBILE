-- ====================================================================
-- MIGRATION: Push Notifikasi Terarah untuk Fitur Chat Area Manager & Crew
-- File: 20300230000000_area_chat_push_trigger.sql
--
-- Karakteristik:
-- - Idempoten, aman dieksekusi berulang kali di Supabase SQL Editor.
-- - Push notifikasi HANYA dikirimkan ke anggota area yang bersangkutan:
--   1. Kru dan Store Leader di cabang/outlet binaan area tersebut.
--   2. Area Manager pengampu area tersebut.
-- - Pengirim pesan (sender_id) dikecualikan otomatis.
-- - Menggunakan fungsi format label pesan public.chat_teks_push(...)
--   sehingga otomatis membedakan teks, foto ("📷 Foto"), dan suara ("🎤 Pesan suara").
-- - Mengirimkan payload 'area_chat' ke edge function send-chat-push via pg_net.
-- ====================================================================

-- 1. Helper Function: Nama Tampilan Grup Area
CREATE OR REPLACE FUNCTION public.area_chat_nama_grup(p_area_id TEXT)
RETURNS TEXT
LANGUAGE sql
IMMUTABLE
AS $$
  SELECT CASE p_area_id
    WHEN 'abu_bakar' THEN 'Area Abu Bakar'
    WHEN 'muchtar' THEN 'Area Muchtar'
    WHEN 'chairul_rizky' THEN 'Area Chairul Rizky'
    WHEN 'tri_rizky' THEN 'Area Tri Rizky'
    WHEN 'mulyadi' THEN 'Area Mulyadi'
    ELSE 'Area Chat'
  END;
$$;

GRANT EXECUTE ON FUNCTION public.area_chat_nama_grup(TEXT) TO authenticated, service_role;

-- 2. Helper Function: Mengambil seluruh user_id penerima yang sah di Area tersebut
CREATE OR REPLACE FUNCTION public.area_chat_penerima_ids(p_area_id TEXT, p_sender_id UUID)
RETURNS TABLE (user_id UUID)
LANGUAGE plpgsql
STABLE SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  RETURN QUERY
  WITH target_outlets AS (
    SELECT unnest(CASE p_area_id
      WHEN 'abu_bakar' THEN ARRAY['EMPANG', 'BCC', 'DRAMAGA', 'PALEDANG', 'CICURUG', 'CIMANGGU']
      WHEN 'muchtar' THEN ARRAY['CIBINONG', 'CISEENG', 'SENTUL', 'PAJAJARAN']
      WHEN 'chairul_rizky' THEN ARRAY['SUKMAJAYA', 'BEJI', 'SAWANGAN', 'CIRENDEU', 'JAGAKARSA']
      WHEN 'tri_rizky' THEN ARRAY['KALISARI', 'CIBUBUR', 'CILENGSI', 'CILEUNGSI']
      WHEN 'mulyadi' THEN ARRAY['PEKAYON', 'JATIASIH', 'JATIWARINGIN']
      ELSE ARRAY[]::TEXT[]
    END) AS outlet_nama
  ),
  target_am AS (
    SELECT CASE p_area_id
      WHEN 'abu_bakar' THEN 'abu bakar'
      WHEN 'muchtar' THEN 'muchtar'
      WHEN 'chairul_rizky' THEN 'chairul'
      WHEN 'tri_rizky' THEN 'tri'
      WHEN 'mulyadi' THEN 'mulyadi'
      ELSE ''
    END AS am_name
  )
  -- 1. Seluruh staf & leader di cabang binaan area ini
  SELECT DISTINCT s.id AS user_id
    FROM public.outlet_staff s
    JOIN public.outlets o ON o.id = s.outlet_id
   WHERE UPPER(o.name) IN (SELECT outlet_nama FROM target_outlets)
     AND s.id <> p_sender_id
  UNION
  -- 2. Area Manager pengampu area ini
  SELECT DISTINCT s.id AS user_id
    FROM public.outlet_staff s
    CROSS JOIN target_am am
   WHERE (s.role = 'area_manager' OR LOWER(s.role) = 'area_manager')
     AND (
       LOWER(s.name) LIKE '%' || am.am_name || '%'
       OR LOWER(COALESCE(s.display_name, '')) LIKE '%' || am.am_name || '%'
     )
     AND s.id <> p_sender_id;
END;
$$;

GRANT EXECUTE ON FUNCTION public.area_chat_penerima_ids(TEXT, UUID) TO authenticated, service_role;

-- 3. Trigger Function: Pengiriman Push Notifikasi ke Edge Function
CREATE OR REPLACE FUNCTION public.trigger_area_chat_message_push()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_url        text;
  v_key        text;
  v_body       text;
  v_nama_grup  text;
  v_recip      record;
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

    v_nama_grup := public.area_chat_nama_grup(NEW.area_id);
    v_body := public.chat_teks_push(NEW.body, NEW.image_path, NEW.audio_path, NEW.audio_ms);

    -- Kirim push notifikasi per recipient yang merupakan anggota area
    FOR v_recip IN
      SELECT user_id FROM public.area_chat_penerima_ids(NEW.area_id, NEW.sender_id)
    LOOP
      BEGIN
        PERFORM net.http_post(
          url := v_url,
          headers := jsonb_build_object(
            'Content-Type', 'application/json',
            'Authorization', 'Bearer ' || v_key
          ),
          body := jsonb_build_object(
            'message_id',    NEW.id,
            'sender_id',     NEW.sender_id,
            'sender_name',   COALESCE(NEW.sender_name, 'Rekan Staf'),
            'sender_avatar', COALESCE(NEW.sender_avatar, ''),
            'recipient_id',  v_recip.user_id,
            'body',          v_body,
            'type',          'area_chat',
            'area_id',       NEW.area_id,
            'group_name',    v_nama_grup,
            'title',         v_nama_grup,
            'route',         '/chat/area?id=' || NEW.area_id
          )
        );
      EXCEPTION WHEN OTHERS THEN
        NULL;
      END;
    END LOOP;

  EXCEPTION WHEN OTHERS THEN
    -- Pengiriman pesan di basis data tidak boleh gagal hanya karena push bermasalah
    RAISE WARNING 'Gagal memproses push area chat: %', SQLERRM;
  END;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_area_chat_message_push ON public.area_chat_messages;
CREATE TRIGGER trg_area_chat_message_push
    AFTER INSERT ON public.area_chat_messages
    FOR EACH ROW EXECUTE FUNCTION public.trigger_area_chat_message_push();
