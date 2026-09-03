# Modul Stok Native — Fase 0 (Fondasi) + Fase 1 (Monitoring)

Tanggal: 3 September 2026
Status: disetujui, siap implementasi
Modul: `:feature:stok`

## 1. Konteks dan batasan keras

Modul Stok native berbagi satu project Supabase (`khpkoreaaucvyqfhynfq`) dengan
aplikasi Stok versi web, POS, dan Absensi. Karena itu berlaku batasan mutlak:

- **Nol migration.** Tidak membuat/mengubah tabel, view, RPC, RLS, atau trigger.
- **Nol tulis di Fase 0-1.** Modul ini murni membaca.
- **Tidak menyentuh service-role.** Native tunduk penuh pada RLS, sama seperti
  browser client web. Service key tidak boleh ada di dalam APK.
- Mutasi stok (fase berikutnya) hanya boleh lewat `ledger_stok` + trigger,
  tidak pernah menulis `stok_balance` langsung.

## 2. Kontrak schema — terverifikasi, bukan dari dokumen

`ANALISIS_LOGIC_DAN_ALUR_BISNIS_STOK_WEB.md` terbukti tidak akurat sebagai
kontrak teknis: 11 dari 28 nama tabel salah, beberapa nama kolom salah, dan
seluruh model budget wallet (bab 16) tidak ada di database ini. Kontrak di bawah
diverifikasi langsung ke PostgREST (read-only, 3 Sep 2026).

Koreksi nama tabel yang relevan lintas fase:

| Dokumen menulis | Nyatanya |
|---|---|
| `stok_opname` / `stok_opname_items` | `opname` / `opname_item` |
| `waste_reports` | `stok_waste_reports` |
| `resep_menu`, `resep_menu_detail` | `resep`, `resep_item` |
| `resep_substitusi` | `bahan_baku_substitusi` |
| `mutasi_stok` / `mutasi_stok_items` | `mutasi_antar_outlet` / `mutasi_antar_outlet_item` |
| `purchase_orders` / `purchase_order_items` | `purchase_order` / `purchase_order_item` |
| `permintaan_bahan_items` | `permintaan_bahan_item` |
| `surat_jalan_items` | `surat_jalan_item` |

Objek yang dipakai Fase 1:

```
monitoring_view_scoped : outlet_id, outlet_name, bahan_baku_id, item_name,
                         current_qty, threshold, status, is_flagged,
                         saldo_is_gram, last_opname_date, kategori, satuan
monitoring_view_spv    : outlet_id, outlet_name, bahan_baku_id, current_qty,
                         threshold, status, is_flagged
bahan_baku             : id, nama, kategori, satuan, satuan_tengah, satuan_kecil,
                         faktor_tengah, faktor_tampilan, faktor_konversi,
                         satuan_distribusi, is_active
ledger_stok            : id, outlet_id, bahan_baku_id, tipe, qty, saldo_sebelum,
                         saldo_sesudah, catatan, created_at, created_by,
                         ref_order_id, ref_opname_id, ref_shipment_id,
                         ref_transfer_id, ref_waste_id, ref_po_id
resep                  : id, nama, outlet_id, is_active, created_at, updated_at
resep_item             : id, resep_id, bahan_baku_id, satuan, qty_per_porsi
outlets                : id, name, is_active, marquee_warning_threshold
accessible_outlet_ids()-> SETOF uuid (RPC, tanpa parameter)
```

Catatan penting: `outlets` memakai kolom `name`, bukan `nama`. `ledger_stok`
memakai `saldo_sebelum`/`saldo_sesudah`/`catatan`, bukan
`saldo_before`/`saldo_after`/`keterangan` seperti tertulis di dokumen.

Yang tidak bisa diverifikasi lewat probing anon, sehingga fase tulis (2-6)
belum boleh dispec: signature RPC, definisi policy RLS, dan definisi trigger.

## 3. Otorisasi dan scope outlet

Pengguna modul Stok: **`crew`, `leader`, `area_manager`, dan `regional_manager`**.
Daftar ini mencerminkan `isLeaderOrSPV` pada `BottomNav.tsx` di aplikasi web
(`spv`, `regional_manager`, `leader`, `area_manager`) ditambah `crew`, dikurangi
`spv` yang sudah tidak dipakai lagi karena digantikan `regional_manager`. Baris
`outlet_staff` lama ber-role `spv` tidak dihapus dari database.

Perbedaan antar role ini hanya pada **cakupan outlet, bukan pada fitur**. Di web,
`MonitoringPage.tsx` memberi leader dan area manager `SPVDashboard` yang dibatasi
`allowedOutletIds` dari `staff_outlets`, memberi regional manager `SPVDashboard`
penuh, dan memberi crew `CrewDashboard` satu outlet. Native memperoleh pembedaan
yang sama tanpa cabang role apa pun, karena cakupannya berasal dari
`accessible_outlet_ids()` yang sudah memetakan leader/area_manager lewat
`staff_outlets`. Pemilih outlet muncul sendiri ketika hasilnya lebih dari satu.

Scope outlet **tidak** ditentukan matriks role di dalam aplikasi. Aplikasi
memanggil `accessible_outlet_ids()`, yang merupakan otoritas sebenarnya di
database. Alasannya: matriks role terbukti berubah (spv menjadi
regional_manager) dan dokumen sudah tidak sinkron dengannya. Dengan menyerahkan
keputusan ke fungsi DB, native ikut berubah tanpa perlu rilis APK baru.

Role di aplikasi hanya dipakai untuk keputusan kosmetik: menampilkan tile Stok
di Home, dan menampilkan pemilih outlet (regional manager) atau tidak (crew).

Aturan keras: bila `accessible_outlet_ids()` mengembalikan kosong, tampilkan
keadaan kosong yang eksplisit. Jangan pernah menafsirkannya sebagai
"berarti semua outlet".

## 4. Arsitektur modul

```
feature/stok/src/main/java/com/sukashawarma/superapp/feature/stok/
  StokRoutes.kt, StokNavGraph.kt
  data/    StokRepository.kt, model/*.kt
  domain/  UnitScale.kt, ProduksiEstimator.kt, TransferSuggester.kt, StokError.kt
  ui/      monitoring/, detail/, produksi/, transfer/, OutletPicker.kt
```

Pembagian tanggung jawab:

- `data/` hanya tahu PostgREST dan JSON. Tidak ada aturan bisnis.
- `domain/` murni Kotlin tanpa dependensi Android, sehingga bisa diuji unit
  tanpa emulator. Di sinilah logika yang rawan salah berada.
- `ui/` tidak pernah memanggil `Postgrest` langsung.

Folder dan deklarasi `package` dibuat konsisten (`feature.stok`). Modul lain
yang tidak konsisten tidak disentuh — itu di luar lingkup.

Fase 0-1 tidak menambah dependensi runtime apa pun. Satu-satunya penambahan
adalah `testImplementation("junit:junit:4.13.2")`.

## 5. Layar

Tile "Stok" ditambahkan di `HomeScreen` memakai `ModuleCard` yang sudah ada,
tampil hanya untuk crew dan regional_manager. Rute `Routes.STOK` di
`MainActivity` memuat `StokNavGraph` — pola yang sama dengan Absensi.

1. **Dashboard monitoring** — susunan dan fungsinya mengikuti `CrewList.tsx` di web,
   tampilannya memakai tema Suka sendiri (bukan salinan gaya web):
   - tiga kartu ringkasan **Kritis / Selisih / Aman**, dihitung atas seluruh isi
     outlet; Kritis dan Selisih berfungsi sebagai filter yang bisa ditekan, Aman
     hanya penunjuk angka;
   - kolom pencarian dan dropdown urutan (**Sort: Nama** / **Sort: Status**);
   - daftar dikelompokkan per kategori dengan urutan tetap Food & Beverage,
     Bumbu, Packaging, Operasional, masing-masing dengan jumlah item;
   - kartu bahan berisi nama, lokasi penyimpanan, `Min: <threshold>`, badge status,
     dan rincian tiga jenjang **Sat. Besar / Sat. Tengah / Sat. Kecil**;
   - bahan bersumber gudang pusat disembunyikan dari outlet non-gudang;
   - pemilih outlet muncul sendiri bila outlet yang dapat diakses lebih dari satu.

   Bilah navigasi bawah memuat Dashboard, Permintaan, Opname, Ledger, dan Mutasi
   seperti web. Baru Dashboard yang berisi; empat lainnya tetap ditampilkan agar
   peta menunya sama, tetapi menyatakan terus terang bahwa fiturnya belum ada.
2. **Detail bahan** — saldo, satuan lengkap dan faktornya, tanggal opname
   terakhir, riwayat mutasi dari `ledger_stok` dengan saldo sebelum/sesudah.
3. **Estimasi produksi** — porsi yang masih bisa dibuat per resep, dengan bahan
   penghambat disebut namanya.
4. **Saran transfer** — hanya regional manager; outlet surplus vs outlet kurang.
   Murni saran, tidak mengeksekusi apa pun.

Mobile-first: satu kolom, target sentuh minimal 48dp, angka besar dan tebal
(dibaca sambil berdiri di gudang), tanpa tabel yang perlu digulir menyamping.

## 6. Satuan campuran — divergensi yang disengaja

`stok_balance.saldo` bisa berada di satuan besar (legacy) atau satuan terkecil
(setelah opname modern); `saldo_is_gram` menandainya. Audit dokumen bab 18
mencatat bahwa view monitoring membandingkan `current_qty` mentah dengan
`threshold` tanpa menyamakan skala, sehingga status bisa meleset berkali-kali
lipat untuk bahan ber-`faktor_tampilan` besar.

**Keputusan: native menghitung status sendiri dengan skala ternormalisasi.**
Semua nilai dinormalisasi ke satuan terkecil sebelum dibandingkan.

Konsekuensi yang diterima secara sadar: untuk sebagian bahan, status di native
bisa berbeda dengan status di web. Mitigasinya, layar detail menampilkan saldo
lengkap beserta satuan dan faktornya, sehingga selisih dengan web bisa
dijelaskan dan bukan menjadi misteri.

Aturan status (setelah normalisasi):

```
below   : saldo < threshold / 2
          ATAU estimasi porsi < outlets.marquee_warning_threshold (default 7)
warning : saldo < threshold
ok      : selain itu
```

Estimasi produksi wajib dinormalisasi karena dihitung sendiri dari
`resep_item.qty_per_porsi` dan tidak punya padanan di view.

Skala kebutuhan resep mengikuti aturan `estimasi_produksi.ts` di web, yang sudah
diverifikasi terhadap source: bila `resep_item.satuan` berbeda dari
`bahan_baku.satuan`, angka resep dianggap berada pada satuan terkecil; bila sama
atau salah satunya kosong, angka itu berada pada satuan besar dan dikalikan
`faktor_konversi`. Perhatikan bahwa resep memakai `faktor_konversi`, sedangkan
saldo dan opname memakai `faktor_tampilan` — keduanya sering bernilai sama tetapi
tidak boleh disamakan begitu saja.

Perbedaan native terhadap web pada perhitungan ini: `productionEstimate.ts` di web
membandingkan kebutuhan satuan besar terhadap `current_qty` mentah yang skalanya
campuran, sehingga porsi meleset sebesar faktor konversi pada outlet yang saldonya
sudah small-scale. Native menormalisasi kedua sisi ke satuan terkecil lebih dulu.

## 7. Strategi data dan performa

- **Seluruh bahan satu outlet dimuat sekali**, diambil bertahap sampai habis.
  Ini keharusan, bukan pilihan: hitungan Kritis/Selisih/Aman dan pengelompokan
  kategori harus dihitung atas seluruh isi outlet — kalau hanya sehalaman, angka
  ringkasannya berbohong. Satu outlet berisi puluhan bahan, jadi sekali muat
  masih ringan.
- **Cari, urutkan, dan filter berjalan di memori** setelah data ada, tanpa
  menyentuh jaringan lagi, sehingga mengetik terasa seketika dan tidak perlu
  debounce. Karena seluruh isi outlet sudah ada di memori, pencarian menjangkau
  semuanya — bukan hanya halaman aktif seperti bug bab 18 di web.
- **Cache memori per outlet**: `StateFlow` di repository, TTL 60 detik, mati
  saat modul ditutup. Tidak ada cache disk — saldo berubah tiap transaksi POS,
  dan angka basi di disk lebih berbahaya daripada berguna.
- **Estimasi produksi dihitung di memori** dari cache, di `Dispatchers.Default`.
  Resep khusus outlet menang atas resep global (`outlet_id=is.null`).
- **Nol polling.** Hanya tarik-untuk-refresh.
- `LazyColumn` dengan `key = bahan_baku_id`, data class immutable.

## 8. Penanganan error

Pesan dipetakan per jenis exception mengikuti pola `AppSession`
(`UnknownHostException`, `SocketTimeoutException`, `SSLException`, `IOException`,
`JsonParseException`), bukan satu pesan generik yang menyesatkan.

Tiga keadaan dibedakan tegas: **kosong** ("belum ada bahan di outlet ini"),
**gagal** (pesan disertai tombol coba lagi), dan **tidak berhak** (RLS menolak).

## 9. Pengujian

Unit test pada `domain/` (tanpa emulator):

- Konversi satuan bolak-balik tidak kehilangan nilai; faktor `null`/`0`/satuan
  tak dikenal gagal eksplisit, tidak diam-diam dianggap 1.
- Dua outlet dengan stok fisik identik tapi `saldo_is_gram` berbeda menghasilkan
  status yang sama.
- Estimasi produksi: bahan penghambat terpilih benar; kebutuhan nol tidak
  menyebabkan pembagian nol; resep outlet menang atas resep global.
- Saran transfer: donor tidak pernah didorong ke bawah threshold-nya sendiri.

## 10. Fase 2 — Permintaan, Opname, Ledger, Mutasi

Dibangun setelah repo web tersedia, sehingga seluruh kontraknya disalin dari
sumber aslinya, bukan ditebak.

### 10.1 Kontrak RPC yang dipakai

| Alur | RPC | Parameter |
|---|---|---|
| Opname finalisasi | `finalize_opname` | `p_opname_id` |
| Opname ajukan approval | `set_opname_pending` | `p_opname_id` |
| Opname setujui/tolak | `approve_opname` / `reject_opname` | `p_opname_id`, `p_approved_by` / `p_rejected_by`, `p_reason` |
| Permintaan buat | `buat_permintaan_svc` | `p_outlet_id`, `p_items`, `p_dibuat_oleh`, `p_target_metadata` |
| Permintaan setujui | `approve_permintaan_svc` | `p_permintaan_id`, `p_items` |
| Permintaan tolak | `tolak_permintaan_svc` | `p_permintaan_id`, `p_alasan` |
| Mutasi ajukan | `ajukan_mutasi` | `p_outlet_asal_id`, `p_outlet_tujuan_id`, `p_catatan`, `p_items` |
| Mutasi setujui | `approve_mutasi` | `p_mutasi_id`, `p_is_approved`, `p_catatan_penolakan` |
| Mutasi kirim | `kirim_mutasi` | `p_mutasi_id`, `p_kurir_info`, `p_items_dikirim` |
| Mutasi terima | `terima_mutasi` | `p_mutasi_id`, `p_items_diterima` |

`set_opname_pending`, `approve_opname`, dan `reject_opname` tidak ada di
repository migration web; ketiganya hanya hidup di database. Dipanggil dengan
signature yang dipakai `app/actions/opname.ts`.

### 10.2 Tanpa service-role

Web memakai service-role untuk menyimpan `opname_item` dan membaca permintaan.
Native tidak boleh, dan ternyata tidak perlu: policy `opname_item_write`
(FOR ALL TO authenticated, syarat `opname.status='draft'`) dan
`select_permintaan_bahan_accessible_outlets` sudah mengizinkan hal yang sama
lewat JWT pengguna. Komentar di web yang menyatakan RLS melarangnya sudah usang.

Satu perbedaan yang tersisa: native hanya bisa menyimpan item selama opname
berstatus `draft`, sedangkan web juga bisa saat `pending_approval` karena
mem-bypass RLS. Mengejar itu berarti menaruh service key di dalam APK.

### 10.3 Koreksi terhadap dokumen analisis

Dokumen menuliskan konversi satuan tengah sebagai `qty_tengah * faktor_tengah`.
Itu salah. `calculateTotalFisik` di web memakai
`faktor_tampilan / faktor_tengah`, karena `faktor_tengah` berarti banyak satuan
tengah dalam satu satuan besar. Untuk ES BATU (1 Bal = 10 Kg = 10.000 Gr),
rumus dokumen menghasilkan 10 gram per kilogram, bukan 1.000.

Koreksi lain: tabel `outlet_balance` dan `outlet_balance_ledger` sebenarnya ADA
di database — sebelumnya disimpulkan tidak ada karena tidak terlihat lewat
PostgREST untuk anon. `approve_permintaan_svc` menulis ke keduanya.

### 10.4 Perilaku web yang ditiru apa adanya

`OpnameForm.tsx` SELALU memanggil `finalize`, termasuk ketika ada item di luar
toleransi; `setPendingApproval` tersedia tetapi tidak pernah dipanggil dari
formulir. Native meniru ini persis. Alasannya: bila native menahan opname
bertanda untuk approval sementara web langsung memfinalisasi, satu tindakan
yang sama menghasilkan saldo berbeda tergantung perangkat. Kesenjangan ini
sebaiknya diperbaiki di kedua sisi sekaligus, bukan di salah satunya.

Pengecualian tanggal opname (Cileungsi, Empang, Paledang, Jatiwaringin,
Jatiasih, dan jatah opname ganda 13 Agustus 2026) tidak diport: seluruh
tanggalnya sudah lewat sehingga cabang itu tidak akan pernah aktif lagi, dan
perilaku untuk semua tanggal ke depan identik dengan jalur normal.

## 11. Di luar lingkup

Waste, penerimaan PO, harga bahan, HPP menu, budget, inbound/outbound, dan
laporan penjualan belum dibuat. Laporan penjualan berisiko tidak dapat dibuat
sama sekali dari native karena web membacanya dengan service-role.
