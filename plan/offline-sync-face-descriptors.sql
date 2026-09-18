-- Mode offline: menyalin descriptor wajah staf SATU outlet ke perangkat, supaya absensi
-- tetap bisa mengenali orang ketika internet mati.
--
-- Hari ini pencocokan seluruhnya di server (match_face_mobile), jadi tanpa RPC ini mode
-- offline absensi tidak ada artinya: crew bahkan tidak sampai ke layar liveness.
--
-- Kenapa RPC, bukan membuka kolomnya lewat RLS: `outlet_staff.face_descriptor_mobile`
-- adalah data biometrik. Lewat RPC, siapa yang boleh menarik descriptor siapa ditentukan
-- satu tempat dan bisa diaudit; lewat RLS, setiap query klien ikut menentukannya.
--
-- PERBEDAAN YANG DISENGAJA dari match_face_mobile: fungsi itu, pada mode 1:N, juga
-- mencocokkan staf ber-role pengawas (spv/admin/owner/admin_hr/leader/korlap/
-- regional_manager/area_manager) dari OUTLET MANA PUN. Di sini mereka TIDAK ikut disalin
-- kecuali memang terdaftar di outlet tersebut. Menyalinnya berarti menaruh data biometrik
-- seluruh lapisan manajemen di setiap HP outlet, dan itu tidak sebanding dengan kasus yang
-- ditutupinya (pengawas yang kebetulan berkunjung persis saat internet mati). Pengawas
-- dalam keadaan itu memakai tombol manual, sama seperti sebelum ada mode offline.

SET lock_timeout = '5s';

CREATE OR REPLACE FUNCTION public.sync_face_descriptors(p_outlet_id uuid)
RETURNS TABLE (
  staff_id   uuid,
  name       text,
  descriptor real[],
  updated_at timestamptz
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_caller_id    uuid := auth.uid();
  v_caller       outlet_staff%ROWTYPE;
  v_boleh        boolean := false;
BEGIN
  IF v_caller_id IS NULL THEN
    RAISE EXCEPTION 'unauthenticated' USING ERRCODE = '28000';
  END IF;

  SELECT * INTO v_caller FROM outlet_staff WHERE id = v_caller_id;
  IF NOT FOUND OR v_caller.status <> 'active' THEN
    RAISE EXCEPTION 'staff_tidak_aktif' USING ERRCODE = '42501';
  END IF;

  -- Perangkat hanya boleh menarik descriptor outlet tempat pemakainya benar-benar bekerja.
  -- Tanpa batas ini, satu akun crew cukup untuk mengunduh wajah seluruh perusahaan.
  v_boleh := v_caller.outlet_id = p_outlet_id
    OR EXISTS (
      SELECT 1 FROM staff_outlets so
      WHERE so.staff_id = v_caller_id AND so.outlet_id = p_outlet_id
    )
    OR v_caller.role IN (
      'spv', 'admin', 'owner', 'admin_hr', 'korlap', 'regional_manager', 'area_manager'
    );

  IF NOT v_boleh THEN
    RAISE EXCEPTION 'bukan_outlet_anda' USING ERRCODE = '42501';
  END IF;

  RETURN QUERY
  SELECT os.id,
         os.name,
         os.face_descriptor_mobile,
         coalesce(os.mobile_enrolled_at, os.updated_at, now())
    FROM outlet_staff os
   WHERE os.status = 'active'
     AND os.face_descriptor_mobile IS NOT NULL
     AND (
       os.outlet_id = p_outlet_id
       OR EXISTS (
         SELECT 1 FROM staff_outlets so
         WHERE so.staff_id = os.id AND so.outlet_id = p_outlet_id
       )
     );
END;
$$;

GRANT EXECUTE ON FUNCTION public.sync_face_descriptors(uuid) TO authenticated;

NOTIFY pgrst, 'reload schema';

-- DOWN:
-- DROP FUNCTION IF EXISTS public.sync_face_descriptors(uuid);
