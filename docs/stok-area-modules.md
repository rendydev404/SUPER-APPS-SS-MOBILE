# Modul stok pengelola area

Menu **Stok → Lainnya** menyediakan **Persetujuan Waste** dan **Master Harga Bahan Baku**. Keduanya memakai gerbang role yang berbeda:

- **Master Harga Bahan Baku** — `area_manager`, `leader`, `regional_manager`, `spv`. Dibatasi RPC baca yang memeriksa keempat role ini di database.
- **Persetujuan Waste** — cermin `WASTE_APPROVER_ROLES` web (`leader`, `regional_manager`, `spv`, `kitchen`, `admin`, `owner`, `purchasing`, `developer`) ditambah `area_manager` yang menyetujui tabel yang sama lewat `apps/manager`. RLS `stok_waste_reports` hanya menuntut outlet masuk `accessible_outlet_ids()`, jadi menyamakan daftar ini dengan gerbang modul area akan mencabut kewenangan yang dipunyai kitchen/admin/owner/purchasing di web.

## Harga bahan

Native hanya membaca. Web menambah dan mengubah bahan baku lewat service-role (`createBahanBakuAction` menulis `bahan_baku`, `bahan_baku_sku`, dan `bahan_baku_harga` sekaligus), sedangkan dengan JWT pengguna RLS menuntut role `leader` untuk `bahan_baku` tetapi `admin` untuk `bahan_baku_sku` dan `bahan_baku_harga` — tidak ada satu role pun yang bisa menyelesaikan ketiganya. Menambah bahan dari native karena itu akan menghasilkan master tanpa SKU default, dan ditutup sampai ada RPC khusus.

Mengikuti `apps/stok/src/app/actions/hargaBahan.ts` dan komponen `harga-bahan` web: katalog aktif, harga master display sebagai fallback, hanya PO diterima lengkap/sebagian, periode 7/30/90 hari atau semua riwayat, pencarian nama/vendor/nomor PO (SKU web kosong), filter kategori/status, ringkasan, urutan naik/turun, kelompok kategori buka/tutup, ekspor CSV melalui pemilih file Android, riwayat PO lintas periode dan grafik. Kolom vendor pada web sebenarnya mengurutkan tanggal PO; native menamainya secara eksplisit. Pembelian pertama dihitung stabil seperti web, tanpa persentase perubahan. Area Manager tidak mendapat editor harga atau tambah bahan.

`plan/native-stok-area-price-data.sql` diterapkan ke Supabase dengan nama migrasi `native_stok_area_price_data`. RPC baca ini diperlukan karena RLS tabel harga master tidak memberi akses kepada Area Manager, sementara web membaca melalui server. RPC memeriksa sesi, status aktif, role, dan periode; tidak memberi izin menulis atau mengubah policy web. Install database lain harus menerapkan SQL ini sebelum APK dipakai.

## Waste

Daftar PENDING dibatasi `accessible_outlet_ids()`, foto dibuka melalui penampil perangkat, qty memakai tiga jenjang satuan. Saldo diperiksa per outlet laporan, termasuk konversi gram ke satuan besar. Persetujuan memeriksa ulang saldo dan meminta konfirmasi jika negatif atau faktor konversi tidak diketahui. Penolakan wajib beralasan. UPDATE selalu menyertakan status PENDING; respons kosong dianggap konflik/akses berubah. Trigger database menangani pengurangan stok dan ledger tepat sekali. Realtime postgres_changes tersambung hanya saat layar aktif, disertai rekoneksi dan rekonsiliasi berkala.

## Validasi

- 81 unit test modul stok lulus, termasuk 6 kasus baru untuk harga, CSV, role, dan satuan waste.
- APK debug berhasil dibangun.
- Android lint lulus: 0 error, 4 peringatan versi dependensi yang sudah ada.
- RPC diuji di database melalui transaksi rollback: keempat role pengelola membaca 52 bahan aktif dan seluruh riwayat; crew ditolak. Tidak ada laporan waste produksi yang disetujui/ditolak selama pengujian.
- Uji interaksi perangkat dengan akun Area Manager masih diperlukan: menu Lainnya, filter/urut/kelompok, simpan CSV, detail riwayat, foto, konfirmasi defisit, penolakan, serta pembaruan lintas perangkat. APK belum dipasang ke perangkat oleh pekerjaan ini.
