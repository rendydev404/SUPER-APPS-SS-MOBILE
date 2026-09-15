-- Pilihan dua shift per outlet untuk app native — cermin fitur web
-- `docs/FITUR-ABSENSI-DUA-SHIFT.md` (repo DIGITALISASI-SS-PROJECT, commit 5b50145e → eca66b97).
--
-- Prasyarat: migration web `20260915120000_absensi_pilih_shift.sql` sudah diterapkan
-- (kolom outlet_attendance_config.pilih_shift_aktif/shift2_* dan attendance.shift_jam_*).
--
-- Kenapa native butuh file ini: web memakai route Next.js ber-service-role, native tidak.
-- Native menulis lewat RPC SECURITY DEFINER, jadi aturan shift harus ada juga di RPC:
--   1. list_outlet_attendance_config  → ikut mengembalikan kolom shift
--   2. save_outlet_attendance_config  → bisa menyimpan toggle + jam Shift 2
--   3. attendance_shift_config        → config shift outlet aktif untuk layar Absen
--                                        (RLS oac_read_own_outlet hanya membuka outlet
--                                        utama, padahal leader absen di banyak outlet)
--   4. submit_attendance              → penegakan §7 dokumen: shift_required, jejak shift,
--                                        jam efektif, gerbang penutupan hanya shift penutup
--
-- Idempoten, aman diulang. Jalankan di Supabase SQL Editor (project khpkoreaaucvyqfhynfq).

SET lock_timeout = '5s';

-- ─────────────────────────────────────────────────────────────── 1. list
-- Tipe kembalian berubah → wajib DROP dulu (CREATE OR REPLACE menolak ubah RETURNS TABLE).
DROP FUNCTION IF EXISTS public.list_outlet_attendance_config();

CREATE FUNCTION public.list_outlet_attendance_config()
RETURNS TABLE (
  outlet_id uuid,
  outlet_name text,
  jam_masuk text,
  jam_keluar text,
  toleransi_menit int,
  radius_m int,
  absen_window_mode text,
  pilih_shift_aktif boolean,
  shift2_jam_masuk text,
  shift2_jam_keluar text
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  PERFORM assert_attendance_settings_admin();
  RETURN QUERY
    SELECT
      c.outlet_id,
      o.name::text,
      left(c.jam_masuk::text, 5),
      left(c.jam_keluar::text, 5),
      c.toleransi_menit,
      c.radius_m,
      coalesce(c.absen_window_mode, 'auto'),
      coalesce(c.pilih_shift_aktif, false),
      left(c.shift2_jam_masuk::text, 5),
      left(c.shift2_jam_keluar::text, 5)
    FROM outlet_attendance_config c
    JOIN outlets o ON o.id = c.outlet_id
    ORDER BY o.name;
END;
$$;

-- ─────────────────────────────────────────────────────────────── 2. save
-- Parameter shift sengaja DEFAULT NULL = "jangan diubah". App native versi lama masih
-- memanggil dengan 6 argumen; tanpa aturan ini, admin yang menyunting jam BNR dari app
-- lama akan diam-diam mematikan pilihan shift-nya.
DROP FUNCTION IF EXISTS public.save_outlet_attendance_config(uuid, text, text, int, int, text);
DROP FUNCTION IF EXISTS public.save_outlet_attendance_config(uuid, text, text, int, int, text, boolean, text, text);

CREATE FUNCTION public.save_outlet_attendance_config(
  p_outlet_id uuid,
  p_jam_masuk text,
  p_jam_keluar text,
  p_toleransi_menit int,
  p_radius_m int,
  p_absen_window_mode text DEFAULT 'auto',
  p_pilih_shift_aktif boolean DEFAULT NULL,
  p_shift2_jam_masuk text DEFAULT NULL,
  p_shift2_jam_keluar text DEFAULT NULL
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_shift2_lengkap boolean := nullif(p_shift2_jam_masuk, '') IS NOT NULL
                          AND nullif(p_shift2_jam_keluar, '') IS NOT NULL;
BEGIN
  PERFORM assert_attendance_settings_admin();

  IF p_outlet_id IS NULL THEN
    RAISE EXCEPTION 'Outlet wajib dipilih' USING errcode = '22023';
  END IF;
  IF coalesce(p_absen_window_mode, 'auto') NOT IN ('auto', 'manual') THEN
    RAISE EXCEPTION 'Mode absensi tidak valid' USING errcode = '22023';
  END IF;

  -- Validasi sama dengan server action web `saveOutletException`.
  IF p_pilih_shift_aktif IS TRUE THEN
    IF NOT v_shift2_lengkap THEN
      RAISE EXCEPTION 'Isi jam masuk dan jam pulang Shift 2' USING errcode = '22023';
    END IF;
    IF p_shift2_jam_masuk::time = p_jam_masuk::time AND p_shift2_jam_keluar::time = p_jam_keluar::time THEN
      RAISE EXCEPTION 'Shift 2 sama persis dengan Shift 1 — ubah jamnya atau matikan pilihan shift'
        USING errcode = '22023';
    END IF;
  END IF;

  INSERT INTO outlet_attendance_config (
    outlet_id, jam_masuk, jam_keluar, toleransi_menit, radius_m, absen_window_mode,
    pilih_shift_aktif, shift2_jam_masuk, shift2_jam_keluar
  ) VALUES (
    p_outlet_id,
    p_jam_masuk::time,
    p_jam_keluar::time,
    p_toleransi_menit,
    p_radius_m,
    coalesce(p_absen_window_mode, 'auto'),
    coalesce(p_pilih_shift_aktif, false),
    CASE WHEN v_shift2_lengkap THEN p_shift2_jam_masuk::time END,
    CASE WHEN v_shift2_lengkap THEN p_shift2_jam_keluar::time END
  )
  ON CONFLICT (outlet_id) DO UPDATE SET
    jam_masuk = excluded.jam_masuk,
    jam_keluar = excluded.jam_keluar,
    toleransi_menit = excluded.toleransi_menit,
    radius_m = excluded.radius_m,
    absen_window_mode = excluded.absen_window_mode,
    pilih_shift_aktif = coalesce(p_pilih_shift_aktif, outlet_attendance_config.pilih_shift_aktif),
    -- Mematikan toggle tidak menghapus jam Shift 2, supaya tak perlu diketik ulang.
    shift2_jam_masuk = CASE WHEN v_shift2_lengkap THEN excluded.shift2_jam_masuk ELSE outlet_attendance_config.shift2_jam_masuk END,
    shift2_jam_keluar = CASE WHEN v_shift2_lengkap THEN excluded.shift2_jam_keluar ELSE outlet_attendance_config.shift2_jam_keluar END;
END;
$$;

-- ─────────────────────────────────────────────────── 3. config shift outlet aktif
-- Hanya jam kerja dan toggle — tidak ada data sensitif. NULL = outlet mengikuti aturan
-- pusat (selalu satu shift). Aturan "lengkap/tidak" diputuskan client lewat shiftOptions().
CREATE OR REPLACE FUNCTION public.attendance_shift_config(p_outlet_id uuid)
RETURNS jsonb
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF auth.uid() IS NULL THEN
    RETURN NULL;
  END IF;
  RETURN (
    SELECT jsonb_build_object(
      'jam_masuk', left(c.jam_masuk::text, 5),
      'jam_keluar', left(c.jam_keluar::text, 5),
      'pilih_shift_aktif', coalesce(c.pilih_shift_aktif, false),
      'shift2_jam_masuk', left(c.shift2_jam_masuk::text, 5),
      'shift2_jam_keluar', left(c.shift2_jam_keluar::text, 5)
    )
    FROM outlet_attendance_config c
    WHERE c.outlet_id = p_outlet_id
  );
END;
$$;

-- ─────────────────────────────────────────────────────────── 4. submit_attendance
-- Basis: 20300220000000_fix_attendance_night_shift_and_geofence.sql. Perubahan hanya pada
-- blok bertanda [SHIFT]; geofence, cegah dobel, dan perhitungan malam tetap sama.
CREATE OR REPLACE FUNCTION public.submit_attendance(payload jsonb)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_caller_id         uuid := auth.uid();
  v_caller_staff      outlet_staff%ROWTYPE;
  v_target_staff_id   uuid := coalesce(nullif(payload->>'outlet_staff_id', '')::uuid, v_caller_id);
  v_target_staff      outlet_staff%ROWTYPE;
  v_outlet_id         uuid := (payload->>'outlet_id')::uuid;
  v_type              text := payload->>'type';
  v_gps_lat           double precision := (payload->>'gps_lat')::double precision;
  v_gps_lng           double precision := (payload->>'gps_lng')::double precision;
  v_gps_accuracy      double precision := coalesce((payload->>'gps_accuracy')::double precision, 0);
  v_is_manual         boolean := coalesce((payload->>'is_manual_button')::boolean, false);
  v_id                uuid := coalesce(nullif(payload->>'id', '')::uuid, gen_random_uuid());
  -- [SHIFT] Hanya angka JSON 1/2 yang sah — string "1" ditolak, sama dengan isShiftKe() web.
  v_shift_ke          int := CASE
                               WHEN jsonb_typeof(payload->'shift_ke') = 'number'
                                AND (payload->>'shift_ke') IN ('1', '2')
                               THEN (payload->>'shift_ke')::int
                             END;
  v_outlet_lat        double precision;
  v_outlet_lng        double precision;
  v_distance_m        double precision;
  v_cfg_found         boolean := false;
  v_cfg_jam_masuk     time;
  v_cfg_jam_keluar    time;
  v_cfg_toleransi     int;
  v_cfg_window_mode   text;
  v_cfg_pilih_shift   boolean;
  v_cfg_s2_masuk      time;
  v_cfg_s2_keluar     time;
  v_opsi_shift        boolean := false;
  v_shift_masuk       time;
  v_shift_keluar      time;
  v_jam_masuk_ef      time;
  v_jam_keluar_ef     time;
  v_penutup           boolean := true;
  v_pulang_1          int;
  v_pulang_2          int;
  v_pulang_milik      int;
  v_global_cfg        jsonb;
  v_now_server        timestamptz := now();
  v_now_local         timestamp;
  v_now_minutes       int;
  v_in_minutes        int;
  v_out_minutes       int;
  v_deadline_minutes  int;
  v_diff_minutes      int;
  v_status            text := 'tepat';
  v_telat_menit       int;
  GEOFENCE_RADIUS_M   constant double precision := 150.0;
BEGIN
  IF v_caller_id IS NULL THEN
    RETURN jsonb_build_object('ok', false, 'reason', 'unauthenticated');
  END IF;
  IF v_outlet_id IS NULL OR v_type NOT IN ('in','out') THEN
    RETURN jsonb_build_object('ok', false, 'reason', 'invalid_payload');
  END IF;

  SELECT * INTO v_caller_staff FROM outlet_staff WHERE id = v_caller_id;
  IF NOT FOUND THEN
    RETURN jsonb_build_object('ok', false, 'reason', 'staff_not_found');
  END IF;
  IF v_caller_staff.status <> 'active' THEN
    RETURN jsonb_build_object('ok', false, 'reason', 'staff_inactive');
  END IF;

  IF v_target_staff_id <> v_caller_id THEN
    IF v_caller_staff.role NOT IN ('kiosk','spv','owner','admin','admin_hr','regional_manager','area_manager') THEN
      RETURN jsonb_build_object('ok', false, 'reason', 'forbidden_role');
    END IF;
    SELECT * INTO v_target_staff FROM outlet_staff WHERE id = v_target_staff_id;
    IF NOT FOUND THEN
      RETURN jsonb_build_object('ok', false, 'reason', 'staff_not_found');
    END IF;
    IF v_target_staff.status <> 'active' THEN
      RETURN jsonb_build_object('ok', false, 'reason', 'staff_inactive');
    END IF;
  ELSE
    v_target_staff := v_caller_staff;
  END IF;

  IF v_target_staff.role NOT IN ('spv','owner','admin','admin_hr','regional_manager','area_manager')
     AND v_target_staff.outlet_id <> v_outlet_id THEN
    IF NOT EXISTS (
      SELECT 1 FROM staff_outlets
      WHERE staff_id = v_target_staff_id AND outlet_id = v_outlet_id
    ) THEN
      RETURN jsonb_build_object('ok', false, 'reason', 'cross_outlet');
    END IF;
  END IF;

  SELECT lat, lng INTO v_outlet_lat, v_outlet_lng FROM outlets WHERE id = v_outlet_id;
  IF NOT FOUND THEN
    RETURN jsonb_build_object('ok', false, 'reason', 'outlet_not_found');
  END IF;

  IF v_outlet_lat IS NOT NULL AND v_outlet_lng IS NOT NULL THEN
    IF v_gps_lat IS NULL OR v_gps_lng IS NULL THEN
      RETURN jsonb_build_object('ok', false, 'reason', 'gps_required');
    END IF;
    v_distance_m := 6371000 * 2 * asin(sqrt(
      sin(radians(v_gps_lat - v_outlet_lat) / 2) ^ 2 +
      cos(radians(v_outlet_lat)) * cos(radians(v_gps_lat)) *
      sin(radians(v_gps_lng - v_outlet_lng) / 2) ^ 2
    ));
    IF greatest(0, v_distance_m - v_gps_accuracy) > GEOFENCE_RADIUS_M THEN
      RETURN jsonb_build_object('ok', false, 'reason', 'too_far_from_outlet');
    END IF;
  END IF;

  IF v_type = 'in' THEN
    IF EXISTS (
      SELECT 1 FROM attendance
      WHERE outlet_staff_id = v_target_staff_id
        AND type = 'in'
        AND status <> 'alpha'
        AND ts_server >= (v_now_server - interval '12 hours')
    ) THEN
      RETURN jsonb_build_object('ok', false, 'reason', 'already_clocked_in');
    END IF;
  END IF;

  -- Konfigurasi jam outlet. Dimuat SEBELUM gerbang penutupan karena gerbang itu kini
  -- bergantung pada shift crew (urutan §7.2 dokumen).
  SELECT true, jam_masuk, jam_keluar, toleransi_menit, absen_window_mode,
         pilih_shift_aktif, shift2_jam_masuk, shift2_jam_keluar
    INTO v_cfg_found, v_cfg_jam_masuk, v_cfg_jam_keluar, v_cfg_toleransi, v_cfg_window_mode,
         v_cfg_pilih_shift, v_cfg_s2_masuk, v_cfg_s2_keluar
    FROM outlet_attendance_config WHERE outlet_id = v_outlet_id;

  IF NOT coalesce(v_cfg_found, false) THEN
    -- Aturan pusat tidak punya pilihan shift → selalu satu shift.
    SELECT value INTO v_global_cfg FROM global_settings WHERE key = 'global_attendance_config';
    IF v_global_cfg IS NULL THEN
      RETURN jsonb_build_object('ok', false, 'reason', 'config_missing');
    END IF;
    v_cfg_jam_masuk   := coalesce((v_global_cfg->>'jam_masuk')::time, '09:00');
    v_cfg_jam_keluar  := coalesce((v_global_cfg->>'jam_keluar')::time, '17:00');
    v_cfg_toleransi   := coalesce((v_global_cfg->>'toleransi_menit')::int, 0);
    v_cfg_window_mode := coalesce(v_global_cfg->>'absen_window_mode', 'auto');
    v_cfg_pilih_shift := false;
  END IF;

  v_jam_masuk_ef  := v_cfg_jam_masuk;
  v_jam_keluar_ef := coalesce(v_cfg_jam_keluar, '17:00');

  -- [SHIFT] Tentukan shift. Sama dengan shiftOptions(): pilihan hanya ada bila toggle
  -- menyala dan keempat jam terisi.
  v_opsi_shift := coalesce(v_cfg_pilih_shift, false)
                  AND v_cfg_jam_masuk IS NOT NULL AND v_cfg_jam_keluar IS NOT NULL
                  AND v_cfg_s2_masuk IS NOT NULL AND v_cfg_s2_keluar IS NOT NULL;

  IF v_opsi_shift THEN
    -- Absen pulang: jejak shift dari absen masuk orang itu SELALU menang atas shift_ke
    -- yang dikirim HP, supaya shift tak bisa diganti di tengah hari.
    IF v_type = 'out' THEN
      SELECT shift_jam_masuk, shift_jam_keluar INTO v_shift_masuk, v_shift_keluar
        FROM attendance
       WHERE outlet_staff_id = v_target_staff_id
         AND type = 'in'
         AND status <> 'alpha'
         AND ts_server >= (v_now_server - interval '20 hours')
       ORDER BY ts_server DESC
       LIMIT 1;
      IF v_shift_masuk IS NULL OR v_shift_keluar IS NULL THEN
        v_shift_masuk := NULL;
        v_shift_keluar := NULL;
      END IF;
    END IF;

    IF v_shift_keluar IS NULL THEN
      IF v_shift_ke = 1 THEN
        v_shift_masuk := v_cfg_jam_masuk;
        v_shift_keluar := v_cfg_jam_keluar;
      ELSIF v_shift_ke = 2 THEN
        v_shift_masuk := v_cfg_s2_masuk;
        v_shift_keluar := v_cfg_s2_keluar;
      ELSIF v_type = 'in' THEN
        RETURN jsonb_build_object('ok', false, 'reason', 'shift_required');
      END IF;
      -- Absen pulang tanpa jejak & tanpa shift_ke (absen masuk sebelum toggle menyala):
      -- lanjut dengan jam outlet.
    END IF;

    IF v_shift_keluar IS NOT NULL THEN
      v_jam_masuk_ef := v_shift_masuk;
      v_jam_keluar_ef := v_shift_keluar;

      -- isShiftPenutup(): jam pulang lewat tengah malam dihitung besok (+1440).
      v_pulang_1 := extract(hour from v_cfg_jam_keluar)::int * 60 + extract(minute from v_cfg_jam_keluar)::int;
      IF v_cfg_jam_keluar < v_cfg_jam_masuk THEN v_pulang_1 := v_pulang_1 + 1440; END IF;
      v_pulang_2 := extract(hour from v_cfg_s2_keluar)::int * 60 + extract(minute from v_cfg_s2_keluar)::int;
      IF v_cfg_s2_keluar < v_cfg_s2_masuk THEN v_pulang_2 := v_pulang_2 + 1440; END IF;

      -- Dibandingkan sebagai HH:MM (date_trunc tidak menerima tipe time).
      IF left(v_shift_keluar::text, 5) = left(v_cfg_jam_keluar::text, 5) THEN
        v_pulang_milik := v_pulang_1;
      ELSIF left(v_shift_keluar::text, 5) = left(v_cfg_s2_keluar::text, 5) THEN
        v_pulang_milik := v_pulang_2;
      END IF;
      -- Jejak yang tak cocok dengan shift mana pun (jam diubah admin) → anggap penutup.
      v_penutup := v_pulang_milik IS NULL OR v_pulang_milik = greatest(v_pulang_1, v_pulang_2);
    END IF;
  END IF;

  -- Gerbang absen pulang (laci kasir & pesanan) — hanya crew shift penutup.
  IF v_type = 'out' AND v_penutup THEN
    IF EXISTS (SELECT 1 FROM shifts WHERE outlet_id = v_outlet_id AND status = 'open') THEN
      RETURN jsonb_build_object('ok', false, 'reason', 'shift_not_closed');
    END IF;
    IF EXISTS (
      SELECT 1 FROM orders
      WHERE outlet_id = v_outlet_id AND status IN ('pending','preparing','ready')
    ) THEN
      RETURN jsonb_build_object('ok', false, 'reason', 'unfinished_orders');
    END IF;
  END IF;

  v_now_local   := v_now_server AT TIME ZONE 'Asia/Jakarta';
  v_now_minutes := extract(hour from v_now_local)::int * 60 + extract(minute from v_now_local)::int;
  v_in_minutes  := extract(hour from v_jam_masuk_ef)::int * 60 + extract(minute from v_jam_masuk_ef)::int;
  v_out_minutes := extract(hour from v_jam_keluar_ef)::int * 60 + extract(minute from v_jam_keluar_ef)::int;

  IF v_out_minutes < v_in_minutes THEN
    v_out_minutes := v_out_minutes + 1440;
    IF v_now_minutes < v_in_minutes - 180 THEN
      v_now_minutes := v_now_minutes + 1440;
    END IF;
  END IF;

  IF coalesce(v_cfg_window_mode, 'auto') = 'auto' AND v_type = 'out' THEN
    v_deadline_minutes := v_out_minutes - 30;
    IF v_now_minutes < v_deadline_minutes THEN
      RETURN jsonb_build_object('ok', false, 'reason', 'too_early_out');
    END IF;
  END IF;

  IF v_type = 'out' THEN
    v_deadline_minutes := v_out_minutes;
    v_diff_minutes := v_now_minutes - v_deadline_minutes;
    IF v_diff_minutes < 0 THEN
      v_status := 'lebih_awal';
      v_telat_menit := abs(v_diff_minutes);
    ELSIF v_diff_minutes >= 1 THEN
      v_status := 'pulang_telat';
      v_telat_menit := v_diff_minutes;
    ELSE
      v_status := 'tepat';
    END IF;
  ELSE
    v_deadline_minutes := v_in_minutes;
    v_diff_minutes := v_now_minutes - v_deadline_minutes;
    IF v_diff_minutes <= 0 THEN
      v_status := 'tepat';
    ELSIF v_diff_minutes <= coalesce(v_cfg_toleransi, 0) THEN
      v_status := 'telat_toleransi';
      v_telat_menit := v_diff_minutes;
    ELSE
      v_status := 'telat';
      v_telat_menit := v_diff_minutes;
    END IF;
  END IF;

  INSERT INTO attendance (
    id, outlet_staff_id, outlet_id, type, ts_server, ts_client,
    gps_lat, gps_lng, distance_m, match_distance, selfie_url,
    status, telat_menit, is_manual_button, source,
    shift_jam_masuk, shift_jam_keluar
  ) VALUES (
    v_id, v_target_staff_id, v_outlet_id, v_type, v_now_server, (payload->>'ts_client')::timestamptz,
    v_gps_lat, v_gps_lng, v_distance_m, coalesce((payload->>'match_distance')::numeric, 0), payload->>'selfie_path',
    v_status, v_telat_menit, v_is_manual, 'native',
    -- [SHIFT] NULL untuk outlet satu shift, sama dengan web.
    CASE WHEN v_shift_keluar IS NOT NULL THEN v_shift_masuk END,
    v_shift_keluar
  )
  ON CONFLICT (id) DO NOTHING;

  IF v_type = 'in' AND v_target_staff.outlet_id <> v_outlet_id THEN
    UPDATE outlet_staff SET outlet_id = v_outlet_id WHERE id = v_target_staff_id;
  END IF;

  RETURN jsonb_build_object('ok', true, 'status', v_status, 'ts_server', v_now_server, 'attendance_id', v_id);
END;
$$;

-- ─────────────────────────────────────────────────────────────── grant
REVOKE ALL ON FUNCTION public.list_outlet_attendance_config() FROM public, anon;
REVOKE ALL ON FUNCTION public.save_outlet_attendance_config(uuid, text, text, int, int, text, boolean, text, text) FROM public, anon;
REVOKE ALL ON FUNCTION public.attendance_shift_config(uuid) FROM public, anon;

GRANT EXECUTE ON FUNCTION public.list_outlet_attendance_config() TO authenticated;
GRANT EXECUTE ON FUNCTION public.save_outlet_attendance_config(uuid, text, text, int, int, text, boolean, text, text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.attendance_shift_config(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.submit_attendance(jsonb) TO authenticated;

NOTIFY pgrst, 'reload schema';

-- DOWN: jalankan ulang plan/jadwal-khusus-outlet.sql (list & save versi lama), migration
-- 20300220000000_fix_attendance_night_shift_and_geofence.sql (submit_attendance lama), lalu
-- DROP FUNCTION IF EXISTS public.attendance_shift_config(uuid);
