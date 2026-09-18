-- Menyalakan realtime untuk modul Inventaris & Sidak di aplikasi native.
--
-- Tiga layar (Sidak, Inventaris, Laporan Inventaris) sudah berlangganan kedua
-- tabel ini dari sisi aplikasi, tetapi langganan ke tabel yang TIDAK ada di
-- publication `supabase_realtime` gagal diam-diam: tidak ada error, sekadar
-- tidak pernah ada kabar. Jalankan berkas ini di project Supabase bersama
-- (khpkoreaaucvyqfhynfq) supaya langganan itu benar-benar hidup.
--
-- REPLICA IDENTITY FULL diperlukan agar payload UPDATE ikut membawa kolom lama —
-- pola yang sama dipakai migrasi web `20300108000022_mutasi_antar_outlet_realtime`.
-- Tanpa itu, perpindahan status tidak bisa dibaca dari event.
--
-- Aman dijalankan berulang: keanggotaan publication diperiksa dulu.

ALTER TABLE public.inventaris_submissions REPLICA IDENTITY FULL;
ALTER TABLE public.inventaris_master_items REPLICA IDENTITY FULL;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_publication_tables
    WHERE pubname = 'supabase_realtime'
      AND schemaname = 'public'
      AND tablename = 'inventaris_submissions'
  ) THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.inventaris_submissions;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_publication_tables
    WHERE pubname = 'supabase_realtime'
      AND schemaname = 'public'
      AND tablename = 'inventaris_master_items'
  ) THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.inventaris_master_items;
  END IF;
END $$;

-- Verifikasi:
--   SELECT tablename FROM pg_publication_tables
--   WHERE pubname = 'supabase_realtime' AND tablename LIKE 'inventaris%';
--
-- CATATAN RLS: event realtime tetap disaring policy SELECT tabelnya. Bila area
-- manager tidak menerima apa-apa setelah migrasi ini, periksa policy SELECT-nya
-- lebih dulu — persoalan yang sama pernah muncul pada `petty_cash_topups` dan
-- diperbaiki migrasi web `20300107000001_fix_manager_realtime_rls`.
