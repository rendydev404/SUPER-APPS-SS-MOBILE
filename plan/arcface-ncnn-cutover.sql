-- Jalankan SEKALI di Supabase SQL editor SEBELUM APK ArcFace dirilis.
-- Keputusan migrasi: tetap memakai satu kolom face_descriptor_mobile; descriptor
-- MobileFaceNet/TFLite lama (192 dimensi) dibuang, lalu enrollment ArcFace mengisinya
-- ulang dengan embedding 512 dimensi yang L2-normalized.

update public.outlet_staff
set
  face_descriptor_mobile = null,
  mobile_enrolled_at = null,
  mobile_enrolled_by = null
where face_descriptor_mobile is not null;

-- Di definisi RPC match_face_mobile yang sudah ada, pastikan query kandidat memfilter:
--   and cardinality(face_descriptor_mobile) = 512
-- dan gunakan threshold ArcFace yang sudah dikalibrasi pada perangkat produksi.
-- Jangan membandingkan atau mempertahankan descriptor 192d lama.
