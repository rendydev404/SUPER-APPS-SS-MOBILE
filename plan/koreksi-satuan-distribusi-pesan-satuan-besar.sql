-- Koreksi satuan pesan bahan baku: outlet memesan dalam SATUAN BESAR.
--
-- Latar (2026-09-04): estimasi harga di layar permintaan menampilkan BAWANG
-- "1 kg = Rp 650.000". Setelah ditelusuri, Rp 650.000 itu memang harga 1 BAL
-- (harga_beli selalu per satuan besar), dan outlet memang memesan per Bal —
-- bukan per kg. Yang keliru adalah kolom `bahan_baku.satuan_distribusi` yang
-- disetel ke satuan tengah/kecil untuk 19 bahan, sehingga:
--   - kartu bahan menulis "Satuan Pesan: kg" padahal pesanannya per Bal,
--   - qty tersimpan terbagi faktor (KEJU 1 pack -> 0,0416 Dus),
--   - estimasi web mengalikan harga per Bal dengan qty kg (membengkak 20x).
--
-- Perbaikan: samakan satuan_distribusi dengan satuan besar. Faktor distribusi
-- otomatis menjadi 1, sehingga satu langkah stepper = 1 satuan besar dan
-- harga_beli terpakai apa adanya — di aplikasi native MAUPUN web, tanpa
-- perubahan kode di keduanya.
--
-- CATATAN DAMPAK (semua di web, karena kolom ini dipakai bersama):
--   1. `InboundOutboundList` dan `MutasiForm` akan menulis satuan besar
--      (mis. "Bal", "Dus") menggantikan "kg"/"pack" pada label qty.
--   2. Riwayat belanja (`getOutletSpendingHistory`) ikut tampil pada satuan besar.
--   3. Baris permintaan LAMA tersimpan pecahan (KEJU 0,0416 Dus). Setelah ini
--      baris itu tampil "1 Dus" (pembulatan ke atas), bukan "1 pack" seperti
--      sebelumnya. Angka tersimpannya tidak diubah — hanya tampilannya.
--   4. Bug estimasi di web ikut hilang untuk 19 bahan ini, karena faktornya
--      menjadi 1. Bahan yang satuan pesannya memang sudah = satuan besar
--      (mis. AYAM per Kg) tidak tersentuh sama sekali.

-- ---------------------------------------------------------------------------
-- 1. PRATINJAU — jalankan ini dulu, pastikan daftarnya sesuai harapan (19 baris)
-- ---------------------------------------------------------------------------
SELECT b.nama,
       b.kategori,
       b.satuan            AS satuan_besar,
       b.satuan_distribusi AS satuan_pesan_sekarang,
       b.satuan            AS satuan_pesan_setelah,
       h.harga_beli        AS harga_per_satuan_besar
FROM public.bahan_baku b
LEFT JOIN public.bahan_baku_harga h ON h.bahan_baku_id = b.id
WHERE b.is_active = true
  AND b.satuan_distribusi IS NOT NULL
  AND lower(b.satuan_distribusi) <> lower(b.satuan)
ORDER BY b.nama;

-- ---------------------------------------------------------------------------
-- 2. KOREKSI — jalankan setelah pratinjau di atas cocok
-- ---------------------------------------------------------------------------
UPDATE public.bahan_baku
SET satuan_distribusi = satuan
WHERE is_active = true
  AND satuan_distribusi IS NOT NULL
  AND lower(satuan_distribusi) <> lower(satuan);

-- ---------------------------------------------------------------------------
-- 3. VERIFIKASI — harus mengembalikan 0 baris
-- ---------------------------------------------------------------------------
SELECT count(*) AS sisa_yang_belum_sama
FROM public.bahan_baku
WHERE is_active = true
  AND satuan_distribusi IS NOT NULL
  AND lower(satuan_distribusi) <> lower(satuan);
