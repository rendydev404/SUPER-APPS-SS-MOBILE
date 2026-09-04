-- Menyambungkan top-up yang disetujui ke angka budget, TANPA mengganti model budget.
--
-- Masalahnya: produksi memakai model "jatah per periode" (20260819150000) —
--   sisa = nominal - SUM(qty_disetujui * harga_snapshot) dalam periode berjalan,
-- sedangkan fitur top-up dirancang untuk model "dompet berjalan" (outlet_balance).
-- Mengganti fungsi budget ke model dompet akan merusak web: semua outlet tampak
-- sisa Rp 0, label periode berubah jadi harian, dan kolom period_type/custom_days
-- yang ditulis updateOutletBudgetConfigAction ikut dihapus sehingga admin tidak
-- bisa lagi mengubah plafon.
--
-- Solusinya: JANGAN ganti modelnya. Cukup akui top-up sebagai penambah plafon
-- untuk periode berjalan saja:
--
--   plafon_efektif = nominal + SUM(top-up disetujui dalam periode ini)
--   sisa           = plafon_efektif - terpakai
--
-- Kenapa ini aman:
--   * Tanpa top-up yang disetujui, SUM = 0 sehingga hasilnya IDENTIK dengan
--     sekarang. Saat ini tabel top-up masih 0 baris, jadi menjalankan ini TIDAK
--     mengubah satu angka pun di web.
--   * period_type, custom_days, effective_from, dan perhitungan terpakai tidak
--     disentuh — label "3 Hari Ini" dan halaman pengaturan plafon tetap utuh.
--   * Top-up hanya berlaku untuk periode saat disetujui. Periode berikutnya
--     kembali ke plafon normal, sesuai sifat top-up sebagai tambahan darurat —
--     berbeda dengan menaikkan `nominal` yang akan menaikkan plafon SELAMANYA.
--   * Berlaku serentak untuk web dan native, karena keduanya membaca fungsi ini.
--
-- Catatan: tanggal berlakunya top-up diambil dari `updated_at` baris yang
-- berstatus 'approved', yaitu saat Finance menyetujui.
--
-- ===========================================================================
-- LANGKAH 0 — WAJIB, JANGAN DILEWATI.
--
-- Skrip ini MENIMPA fungsi yang sedang dipakai web. Badan fungsi di bawah
-- disalin dari migration 20260819150000, tetapi database ini sudah dua kali
-- terbukti menyimpang dari isi repo (policy RLS permintaan dan tabel top-up ada
-- di file, tidak ada di database). Kalau ternyata fungsi terpasang pernah diubah
-- manual, menjalankan skrip ini akan MENGHAPUS perubahan itu.
--
-- Jalankan ini dulu, simpan hasilnya sebagai cadangan sekaligus pembanding:
--
--   SELECT pg_get_functiondef('public.get_outlet_budget_status(uuid)'::regprocedure);
--
-- Bandingkan isinya dengan badan fungsi di bawah. Yang boleh berbeda hanya
-- bagian v_topup yang memang baru. Kalau ada perbedaan lain, BERHENTI dan
-- tunjukkan hasilnya dulu.
--
-- Untuk mengembalikan seperti semula, jalankan kembali teks hasil query di atas.
--
-- Sengaja TIDAK ada REVOKE/GRANT di bawah: CREATE OR REPLACE mempertahankan hak
-- akses yang sudah ada, jadi menulis ulang izin justru menambah risiko.
-- ===========================================================================

CREATE OR REPLACE FUNCTION public.get_outlet_budget_status(p_outlet_id UUID)
RETURNS TABLE (
  nominal      NUMERIC,
  period_type  TEXT,
  period_start DATE,
  period_end   DATE,
  terpakai     NUMERIC,
  sisa         NUMERIC,
  has_config   BOOLEAN,
  custom_days  INT
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_cfg          outlet_budget_config;
  v_today        DATE := (NOW() AT TIME ZONE 'Asia/Jakarta')::date;
  v_start        DATE;
  v_end          DATE;
  v_days_since   INT;
  v_period_index INT;
  v_terpakai     NUMERIC;
  v_topup        NUMERIC;
  v_plafon       NUMERIC;
  v_cdays        INT;
BEGIN
  SELECT * INTO v_cfg FROM outlet_budget_config WHERE outlet_budget_config.outlet_id = p_outlet_id;

  IF v_cfg.outlet_id IS NULL THEN
    RETURN QUERY SELECT 0::NUMERIC, NULL::TEXT, NULL::DATE, NULL::DATE, 0::NUMERIC, 0::NUMERIC, false, NULL::INT;
    RETURN;
  END IF;

  IF v_cfg.period_type = 'harian' THEN
    v_start := v_today;
    v_end   := v_today;
  ELSIF v_cfg.period_type = 'mingguan' THEN
    v_days_since   := v_today - v_cfg.effective_from;
    v_period_index := FLOOR(v_days_since / 7.0);
    v_start := v_cfg.effective_from + (v_period_index * 7);
    v_end   := v_start + 6;
  ELSIF v_cfg.period_type = 'custom' THEN
    v_cdays        := GREATEST(COALESCE(v_cfg.custom_days, 1), 1);
    v_days_since   := v_today - v_cfg.effective_from;
    v_period_index := FLOOR(v_days_since::NUMERIC / v_cdays::NUMERIC);
    v_start := v_cfg.effective_from + (v_period_index * v_cdays);
    v_end   := v_start + v_cdays - 1;
  ELSE -- bulanan
    v_start := DATE_TRUNC('month', v_today)::date;
    v_end   := (DATE_TRUNC('month', v_today) + INTERVAL '1 month' - INTERVAL '1 day')::date;
  END IF;

  SELECT COALESCE(SUM(pbi.qty_disetujui * COALESCE(pbi.harga_snapshot, 0)), 0)
  INTO v_terpakai
  FROM permintaan_bahan pb
  JOIN permintaan_bahan_item pbi ON pbi.permintaan_id = pb.id
  WHERE pb.outlet_id = p_outlet_id
    AND pb.status = 'disetujui'
    AND (pb.updated_at AT TIME ZONE 'Asia/Jakarta')::date BETWEEN v_start AND v_end;

  -- BARU: top-up yang sudah disetujui Finance dalam periode berjalan menambah plafon.
  -- Tabelnya mungkin belum ada pada database yang belum menjalankan
  -- plan/topup-saldo-outlet.sql — di situ v_topup tetap 0 dan hasilnya sama persis
  -- dengan versi sebelumnya.
  v_topup := 0;
  IF to_regclass('public.outlet_budget_topup_requests') IS NOT NULL THEN
    SELECT COALESCE(SUM(t.requested_amount), 0)
    INTO v_topup
    FROM outlet_budget_topup_requests t
    WHERE t.outlet_id = p_outlet_id
      AND t.status = 'approved'
      AND (t.updated_at AT TIME ZONE 'Asia/Jakarta')::date BETWEEN v_start AND v_end;
  END IF;

  v_plafon := v_cfg.nominal + v_topup;

  RETURN QUERY SELECT
    v_plafon,
    v_cfg.period_type,
    v_start,
    v_end,
    v_terpakai,
    (v_plafon - v_terpakai),
    true,
    v_cfg.custom_days;
END;
$$;

-- ===========================================================================
-- LANGKAH 2 — VERIFIKASI. Harus mengembalikan angka yang SAMA PERSIS dengan
-- sebelum skrip dijalankan, karena belum ada top-up yang disetujui.
--
--   SELECT * FROM get_outlet_budget_status('eb174b2b-ff69-47eb-97af-b6c824d3ce4a');
--
-- Harapan: nominal 5000000 | period_type custom | custom_days 3 |
--          terpakai 8791.2 | sisa 4991208.8
--
-- Lalu buka web: badge harus tetap berbunyi
--   "Sisa Budget 3 Hari Ini: Rp 4.991.209 dari Rp 5.000.000".
-- Kalau berbeda walau seangka, kembalikan memakai cadangan dari LANGKAH 0.
-- ===========================================================================
