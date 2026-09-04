# Modul Distribusi Native — Fase 1 (Penerimaan & Pemantauan)

Tanggal: 4 September 2026
Status: disetujui, siap implementasi
Modul: `:feature:distribusi`
Sumber yang direview: `DIGITALISASI-SS-PROJECT/apps/distribusi` (~10.100 baris) dan
`DIGITALISASI-SS-PROJECT/supabase/migrations`

## 1. Konteks dan batasan keras

Aplikasi Distribusi versi web sedang dipakai pengguna produksi. Modul native
berbagi satu project Supabase (`khpkoreaaucvyqfhynfq`) dengannya. Karena itu
berlaku batasan mutlak, dan pelanggaran mana pun membatalkan implementasi:

- **Nol migration.** Tidak membuat atau mengubah tabel, kolom, view, RPC, RLS,
  trigger, bucket, maupun policy storage. Semua yang dipakai sudah ada hari ini.
- **Nol perubahan pada repo web.** Modul ini tidak menyentuh satu file pun di
  `DIGITALISASI-SS-PROJECT`.
- **Tidak menyentuh service-role.** Native tunduk penuh pada RLS, persis seperti
  browser client web. Service key tidak boleh ada di dalam APK.
- **Tidak menulis kolom yang tidak ditulis web.** Setiap penulisan native memakai
  kolom dan RPC yang sama persis dengan yang dipakai web hari ini, sehingga tidak
  ada baris berbentuk baru yang bisa mengagetkan laporan atau trigger web.
- **Tidak menulis kolom turunan.** `surat_jalan_item.selisih` adalah kolom
  `GENERATED ALWAYS ... STORED`; menulisnya akan ditolak Postgres.
- Mutasi stok hanya lewat RPC `finalize_surat_jalan_and_ledger`. Native tidak
  pernah menulis `ledger_stok` atau `stok_balance` langsung.

Konsekuensi paling penting dari batasan ini ada di §3: penerbitan surat jalan
tidak diport, karena database memang tidak mengizinkan role native melakukannya.

## 2. Kontrak schema — terverifikasi dari migration, bukan dari dokumen

Dibaca langsung dari `supabase/migrations`. Nama tabel adalah **tunggal**
(`surat_jalan_item`, bukan `surat_jalan_items`).

```
surat_jalan
  id                  uuid PK, default gen_random_uuid()
  outlet_id           uuid NOT NULL -> outlets(id)
  status              text NOT NULL default 'draft'
                      CHECK IN ('draft','dikirim','dikirim_lengkap',
                                'diterima_sebagian','diterima_lengkap','selesai')
  created_by          uuid -> outlet_staff(id)
  created_at          timestamptz default now()
  updated_at          timestamptz default now()
  notes               text
  signatures          jsonb default '[]'   -- TTD pengirim
  receipt_signatures  jsonb NOT NULL default '[]'  -- TTD penerima
  document_number     text
  verification_code   text UNIQUE, default 6 karakter heksadesimal huruf besar

surat_jalan_item
  id              uuid PK
  surat_jalan_id  uuid NOT NULL -> surat_jalan(id) ON DELETE CASCADE
  bahan_baku_id   uuid NOT NULL -> bahan_baku(id)
  qty_dikirim     numeric NOT NULL CHECK (> 0)      -- SATUAN DASAR
  qty_terima      numeric                            -- SATUAN DASAR
  kondisi         text CHECK IN ('baik','rusak','hilang_qty')
  selisih         numeric GENERATED ALWAYS AS (COALESCE(qty_terima,0) - qty_dikirim) STORED
  flagged         boolean NOT NULL default false
  foto_path       text
  catatan         text
  verified_by     uuid -> outlet_staff(id)
  verified_at     timestamptz
  harga_snapshot  numeric NOT NULL default 0        -- diisi trigger, jangan disentuh
  UNIQUE (surat_jalan_id, bahan_baku_id)

bahan_baku   id, nama, kategori, satuan, satuan_tengah, satuan_kecil,
             faktor_tengah, faktor_tampilan, faktor_konversi, satuan_distribusi, is_active
outlets      id, name, address, is_active            -- kolom `name`, bukan `nama`
```

RPC dan RLS yang dipakai:

| Objek | Kontrak |
|---|---|
| `accessible_outlet_ids()` | `SETOF uuid`, tanpa parameter. Sumber tunggal cakupan outlet. |
| `sign_receipt_surat_jalan(p_surat_jalan_id uuid, p_signed_by_name text, p_role text, p_signature_image text)` | `SECURITY DEFINER`. Menolak status di luar `dikirim`/`dikirim_lengkap`/`diterima_sebagian`, menolak role di luar `'Crew Penerima'`/`'Supir'`, menolak TTD ganda per peran. Mengembalikan `{success, receipt_signatures, total}`. |
| `finalize_surat_jalan_and_ledger(p_surat_jalan_id uuid)` | `SECURITY DEFINER`. Menulis `ledger_stok` tipe `terima_kiriman` (qty lewat `to_ledger_scale`) dan `rejected_kiriman` (qty 0, murni catatan), lalu menetapkan status akhir `diterima_lengkap`/`diterima_sebagian`. Idempoten: mengembalikan `{success:false, message:'Surat jalan sudah diverifikasi sebelumnya'}` bila sudah pernah dijalankan. |
| RLS `surat_jalan_select` / `surat_jalan_item_select` | `outlet_id IN (SELECT accessible_outlet_ids())`. |
| RLS `surat_jalan_update_scoped` / `surat_jalan_item_update_scoped` | Sama, untuk `UPDATE`. Inilah yang mengizinkan verifikasi dan penutupan dokumen dari native. |
| Bucket `verif-foto-bahan` | Privat, batas 200 KB (204800 byte), MIME `image/jpeg`, `image/jpg`, `image/webp`. Policy `INSERT`/`SELECT`/`UPDATE` terbuka untuk seluruh `authenticated`. |

## 3. Peran, hak akses, dan apa yang tidak diport

Modul native melayani `crew`, `leader`, `area_manager`, `regional_manager`.
`kitchen` dan `admin` tetap sepenuhnya di web.

**Cakupan outlet tidak pernah ditentukan di aplikasi.** Repository memanggil
`accessible_outlet_ids()` satu kali per sesi, lalu memfilter dengan
`outlet_id=in.(…)`. Perbedaan antar-role — crew satu outlet, leader beberapa,
AM/RM lebih luas — muncul dari RPC itu, bukan dari daftar role di dalam APK.
Kebijakan yang berubah di database tidak menuntut rilis APK baru. Ini pola yang
sama dengan modul Stok native.

Hanya dua kemampuan yang benar-benar dibedakan per role, dan keduanya tinggal di
`domain/DistribusiAkses.kt`:

| Kemampuan | crew | leader | area_manager | regional_manager |
|---|---|---|---|---|
| Lihat inbox, riwayat, detail, dashboard | ya | ya | ya | ya |
| Verifikasi penerimaan (scan → isi → TTD → finalisasi) | ya | ya | tidak | tidak |
| Tutup dokumen jadi `selesai` | tidak | tidak | ya | ya |
| Lihat kode/QR verifikasi di layar detail | tidak | tidak | ya | ya |

QR disembunyikan dari crew dan leader dengan sengaja. Kalau kode verifikasi bisa
dibaca dari layar, gerbang scan kehilangan maknanya: crew dapat membuka verifikasi
tanpa memegang dokumen fisik yang dibawa supir. Web melakukan hal yang sama lewat
opsi `hideQR: !isPusat`.

**Penerbitan surat jalan tidak diport, dan itu bukan kehilangan fitur.** Web
menampilkan tombol "Buat SJ" untuk `regional_manager` karena daftar `isPusat` di
UI memasukkannya, tetapi database menolaknya di dua lapis: gerbang role di dalam
`create_surat_jalan_with_number` (`role IN ('kitchen','admin','owner')`) dan
policy `surat_jalan_item_insert_scoped` dengan daftar role yang sama. RM yang
menekan tombol itu di web menerima `Forbidden`, bukan surat jalan. Mengaktifkannya
di native menuntut migration ke database produksi, dan itu dilarang oleh §1.

## 4. Arsitektur modul

Mengikuti `:feature:stok`: `data/` hanya tahu PostgREST dan JSON, `domain/`
berisi aturan bisnis murni tanpa dependensi Android sehingga bisa diuji dengan
JUnit biasa, `ui/` satu paket per layar.

```
feature/distribusi/src/main/java/…/feature/distribusi/
  DistribusiRoutes.kt          rute internal + helper encode argumen
  DistribusiNavGraph.kt        NavHost modul, dipasang di Routes.DISTRIBUSI
  data/
    model/DistribusiModels.kt  SuratJalan, SuratJalanItem, TandaTangan, OutletRingkas
    SuratJalanRepository.kt    baca daftar & detail, tulis verifikasi, panggil RPC
    FotoBuktiStore.kt          kompres, unggah, dan ambil kembali foto bukti
    VerifikasiDraftStore.kt    Simpanan lokal: draft verifikasi + status unlock QR
  domain/
    DistribusiAkses.kt         role -> kemampuan (tabel di §3)
    SatuanDistribusi.kt        konversi satuan dasar <-> satuan distribusi
    StatusSuratJalan.kt        enum status, label Indonesia, aturan "ada selisih"
    RingkasanDistribusi.kt     statistik dashboard dari daftar SJ
    ValidasiVerifikasi.kt      aturan boleh-lanjut per item
  ui/
    inbox/         InboxScreen + InboxViewModel
    scan/          ScanQrScreen + ScanQrViewModel
    verifikasi/    VerifikasiScreen + VerifikasiViewModel
    ttd/           TandaTanganCanvas.kt
    riwayat/       RiwayatScreen + RiwayatViewModel
    detail/        DetailSuratJalanScreen + DetailViewModel
    dashboard/     DashboardScreen + DashboardViewModel
    DistribusiComponents.kt    kartu SJ, lencana status, baris item — dipakai bersama
```

Dependensi baru pada `feature/distribusi/build.gradle.kts`:

```
com.google.mlkit:barcode-scanning:17.2.0     gerbang QR
androidx.camera:camera-*                     pratinjau kamera (versi sama dengan :core:camera)
SharedPreferences (android.content)             draft verifikasi & penanda unlock — pola sama dengan AuthPrefs
project(":core:storage")                     unggah foto bukti
project(":core:network")                     Postgrest, SupabaseClient
project(":core:roles")                       AppSession, Role
project(":core:ui")                          tema
```

Modul didaftarkan di `HomeScreen.kt` sebagai kartu ketiga dengan penjaga role
`DISTRIBUSI_ROLES = setOf(CREW, LEADER, AREA_MANAGER, REGIONAL_MANAGER)`, pola
yang sama dengan `STOK_ROLES` yang sudah ada.

## 5. Konversi satuan

Kolom `qty_dikirim` dan `qty_terima` selalu dalam **satuan dasar**
(`bahan_baku.satuan`), sedangkan layar selalu menampilkan **satuan distribusi**
(`bahan_baku.satuan_distribusi`, misalnya "dus" atau "kg"). `SatuanDistribusi.kt`
memuat satu-satunya tempat konversi ini terjadi, meniru persis
`getDistribusiFactor` di web:

```kotlin
fun faktor(b: BahanBakuMeta): Double = when {
    b.satuanDistribusi == null || b.satuanDistribusi.equals(b.satuan, true) -> 1.0
    b.satuanDistribusi.equals(b.satuanTengah, true) && b.faktorTengah != null -> b.faktorTengah
    b.satuanDistribusi.equals(b.satuanKecil, true) && b.faktorTampilan != null -> b.faktorTampilan
    // Pemetaan implisit web: satuan distribusi "kg" dengan satuan kecil "gram".
    b.satuanDistribusi.equals("kg", true) && b.satuanKecil.equals("gram", true)
        && b.faktorTampilan != null -> b.faktorTampilan / 1000.0
    else -> 1.0
}

fun keTampilan(qtyDasar: Double, b: BahanBakuMeta): Long = Math.round(qtyDasar * faktor(b))
fun keDasar(qtyTampilan: Double, b: BahanBakuMeta): Double = qtyTampilan / faktor(b)
```

Pembulatan pada `keTampilan` disengaja dan meniru `Math.round` di web, supaya
angka yang dibaca crew di HP identik dengan angka di dokumen cetak dan di web.
Nilai yang ditulis kembali ke database memakai `keDasar` tanpa pembulatan.

Berbeda dengan modul Stok — yang sengaja menghitung status di atas satuan
ternormalisasi karena view web membandingkan skala campuran — di sini **tidak ada
divergensi perhitungan sama sekali**. Angka yang ditulis native harus identik
dengan angka yang ditulis web untuk masukan yang sama, karena keduanya mengalir ke
`ledger_stok` yang sama.

## 6. Alur layar

### 6.1 Dashboard (semua role)

Satu query: `surat_jalan` dengan `outlet_id=in.(accessible)`, embed
`outlets(name)` dan `surat_jalan_item(qty_dikirim,qty_terima,kondisi)`, urut
`created_at` menurun, difilter rentang tanggal.

`RingkasanDistribusi.kt` menghitung dari daftar itu, meniru web:

- Hitungan per status: draft, dikirim, diterima (lengkap + sebagian), selesai.
- `adaSelisih(sj)` = ada item dengan `kondisi == "rusak"` **atau**
  (`qty_terima != null` dan `qty_terima < qty_dikirim`).
- Tingkat akurasi = `round(akurat / terverifikasi * 100)`, dengan terverifikasi =
  SJ berstatus `selesai`/`diterima_lengkap`/`diterima_sebagian` dan akurat =
  terverifikasi dikurangi yang ada selisih. Bernilai 100 bila belum ada yang
  terverifikasi.
- Rincian per outlet: nama, jumlah total, jumlah aktif, jumlah bermasalah, urut
  menurun, enam teratas.

Kontrol: filter tanggal (semua / hari ini / 7 hari / 30 hari), tab status
(semua / draft / dikirim / belum diverifikasi / ada selisih / selesai), pencarian
nomor dokumen atau nama outlet, dan daftar bergulir dengan pemuatan bertahap.
Web memakai paginasi 8 baris; native memakai `LazyColumn` yang memuat bertahap,
karena paginasi bertombol adalah pola web, bukan pola HP.

Untuk AM dan RM, tab "Belum Diverifikasi" adalah antrean kerja mereka: tiap kartu
punya aksi **Tutup Dokumen** yang mengirim
`PATCH surat_jalan?id=eq.<id>` dengan `{status:'selesai', updated_at:<now>}`.
Aksi ini hanya muncul bila status saat ini `diterima_lengkap` atau
`diterima_sebagian`, dan dikonfirmasi lewat dialog sebelum dikirim.

### 6.2 Inbox penerimaan (crew, leader)

`surat_jalan` dengan `status=in.(dikirim,dikirim_lengkap,diterima_sebagian)` dan
`outlet_id=in.(accessible)`. Tiap kartu memuat nomor dokumen, lencana status,
nama outlet tujuan, dan tanggal. Menekan kartu membuka gerbang QR.

### 6.3 Gerbang QR (crew, leader)

Layar pemindai memakai CameraX dengan penganalisis ML Kit `barcode-scanning`,
ditambah kolom masukan manual untuk kode enam karakter — jalur cadangan yang
wajib ada, karena kamera bisa ditolak izinnya atau rusak.

Pencarian mengikuti web persis: buang bagian sebelum garis miring terakhir bila
hasil pindai berupa URL, lalu bila panjangnya 36 karakter dan mengandung tanda
hubung perlakukan sebagai `id` (huruf kecil), selain itu sebagai
`verification_code` (huruf besar). Query `selectOne` mengambil
`id,status,document_number`.

Penolakan yang harus ditangani:

| Keadaan | Perlakuan |
|---|---|
| Kode tidak ditemukan | Pesan galat, pemindai tetap hidup, kode manual tetap bisa dicoba |
| Status sudah `diterima_lengkap`/`diterima_sebagian`/`selesai` | Pesan "sudah pernah diverifikasi", arahkan ke Riwayat |
| Izin kamera ditolak | Layar tetap berguna: sembunyikan pratinjau, tonjolkan kode manual |
| Perangkat tanpa kamera belakang | Sama dengan di atas |

Sukses menyimpan penanda unlock ke simpanan lokal terenkripsi
(`unlock_verifikasi_<surat_jalan_id> = true`) lalu membuka layar verifikasi.
Penanda ini bertahan setelah app ditutup, sama seperti `localStorage` di web, dan
dihapus setelah finalisasi berhasil.

Layar verifikasi menolak dibuka tanpa penanda ini, meskipun dinavigasi langsung.

### 6.4 Verifikasi per item (crew, leader)

Satu kartu per item, satu layar penuh, dengan indikator kemajuan — meniru web.
Detail dibaca dengan satu query bersarang:

```
surat_jalan?id=eq.<id>&select=id,outlet_id,status,created_at,signatures,
  receipt_signatures,document_number,verification_code,outlets(name),
  surat_jalan_item(*,bahan_baku(id,nama,satuan,kategori,satuan_distribusi,
  satuan_tengah,satuan_kecil,faktor_tengah,faktor_tampilan))
```

Per item, crew mengisi:

1. **Qty terima** dalam satuan distribusi, dengan tombol "Sesuai Kirim" yang
   mengisinya dengan `qty_dikirim` yang sudah dikonversi.
2. **Kondisi**: Baik atau Tidak Sesuai. Tidak Sesuai mewajibkan catatan alasan.
3. **Foto bukti**, wajib, tidak bisa dilewati.

`ValidasiVerifikasi.kt` memuat aturannya sebagai fungsi murni, sehingga bisa
diuji tanpa Compose:

- Qty terima wajib diisi dan tidak boleh nol untuk kondisi Baik.
- Qty terima tidak boleh negatif.
- Qty terima tidak boleh melebihi qty dikirim.
- Kondisi Tidak Sesuai wajib disertai catatan tidak kosong.
- Lanjut ke item berikutnya wajib punya `foto_path`.

Foto: diambil lewat CameraX, dikompres di `FotoBuktiStore` — sisi terpanjang
dibatasi 1280 piksel, mutu JPEG mulai 0,85 dan turun 0,1 sampai berkas di bawah
200 KB atau mutu menyentuh 0,2 — lalu diunggah ke `verif-foto-bahan` pada path
`<surat_jalan_id>/<item_id>.jpg` dengan `x-upsert: true`.

Penting: `StorageUtil.uploadJpeg` mengembalikan `"<bucket>/<path>"`, sedangkan
kolom `foto_path` di web berisi **path saja** tanpa nama bucket. `FotoBuktiStore`
wajib menyimpan bentuk path saja, kalau tidak foto tidak akan bisa dibuka dari web.

Karena bucket privat, menampilkan foto memakai
`GET /storage/v1/object/authenticated/verif-foto-bahan/<path>` lewat
`SupabaseClient.okHttpClient`, yang interseptornya sudah menyisipkan token
pengguna. Tidak perlu signed URL dan tidak perlu API baru.

**Draft lokal.** Setiap perubahan menulis ke simpanan lokal: peta `item_id` ke
(qty terima, kondisi, catatan, foto_path), indeks item aktif, langkah aktif, dan
penanda kondisi terkonfirmasi. Saat layar dibuka ulang, draft dipulihkan dan
pratinjau foto diambil ulang dari `foto_path` yang tersimpan. Foto tidak
diantrekan: pengunggahan tetap terjadi saat itu juga, sama seperti web. Kunci
draft memuat `surat_jalan_id`, jadi dua SJ berbeda tidak saling menimpa.

Setelah item terakhir, layar berpindah ke **ringkasan**: seluruh item dengan
lencana Sesuai atau Selisih, foto kecil, dan catatan. Dari sini ada jalan mundur
ke item terakhir untuk memperbaiki.

### 6.5 Tanda tangan penerimaan (crew, leader)

Dua peran wajib, sama dengan web: `Crew Penerima` dan `Supir`. Nama untuk
`Crew Penerima` terisi otomatis dari `AppSession.staff.name` dan tidak dapat
diubah; nama `Supir` diketik manual.

Goresan diambil di `TandaTanganCanvas.kt` — Compose `Canvas` yang merekam jalur
sentuh, lalu dirender ke `Bitmap` dan disandikan menjadi data URL PNG. Batas
50.000 karakter ditegakkan sebelum pengiriman, meniru `MAX_SIGNATURE_SIZE` web;
bila terlampaui, tawarkan mengulang goresan.

Tiap TTD dikirim seketika lewat
`sign_receipt_surat_jalan(p_surat_jalan_id, p_signed_by_name, p_role,
p_signature_image)`. RPC mengembalikan `receipt_signatures` terbaru yang langsung
menjadi sumber kebenaran layar — bukan salinan lokal — sehingga TTD yang sudah
tersimpan tetap terlihat walau app ditutup di tengah jalan.

Tombol finalisasi tetap nonaktif sampai kedua peran menandatangani.

### 6.6 Finalisasi

Dua langkah, urutannya tidak boleh dibalik:

1. Untuk tiap item, `PATCH surat_jalan_item?id=eq.<item_id>`:

   ```json
   {
     "qty_terima":  <qty tampilan / faktor>,
     "kondisi":     "baik" | "rusak",
     "catatan":     "<teks>" | null,
     "flagged":     <qty_terima_dasar != qty_dikirim || kondisi tidak sesuai>,
     "foto_path":   "<path>",
     "verified_at": "<ISO-8601 sekarang>"
   }
   ```

   Kolom `selisih` dan `harga_snapshot` tidak pernah disebut. `verified_by` juga
   tidak, karena web tidak menulisnya — bentuk baris yang dihasilkan native harus
   identik dengan yang dihasilkan web.

2. `POST /rest/v1/rpc/finalize_surat_jalan_and_ledger` dengan
   `{"p_surat_jalan_id": "<id>"}`.

RPC-lah yang menulis `ledger_stok` dan menetapkan status akhir. Aplikasi tidak
pernah menghitung sendiri status akhir dan tidak pernah menulis `ledger_stok`.

Bila langkah 1 sebagian gagal, langkah 2 tidak dijalankan dan draft dipertahankan
sehingga crew bisa mencoba lagi tanpa mengulang dari awal. Bila langkah 2
mengembalikan `success:false` dengan pesan "sudah diverifikasi sebelumnya",
perlakukan sebagai sukses — itu berarti dokumen sudah tertutup, mungkin oleh
percobaan sebelumnya yang jaringannya putus setelah server menerima permintaan.

Sukses menghapus draft dan penanda unlock, lalu membuka Riwayat.

### 6.7 Riwayat dan detail dokumen

Riwayat: `status=in.(diterima_lengkap,diterima_sebagian,selesai)` dalam cakupan
outlet, urut terbaru, dengan penanda "ada selisih" per kartu.

Detail dokumen menampilkan nomor dokumen, outlet, tanggal, status, lalu:

- Daftar item: qty dikirim dan qty terima dalam satuan distribusi, lencana
  Sesuai / Kurang Kirim / Rusak, catatan, dan foto bukti yang bisa diperbesar.
- Dua blok tanda tangan — pengirim (`signatures`) dan penerimaan
  (`receipt_signatures`) — lengkap dengan gambar goresan, nama, peran, dan waktu.
- Kode verifikasi hanya untuk AM dan RM (§3).

## 7. Penanganan galat

| Keadaan | Perlakuan |
|---|---|
| Jaringan mati saat memuat daftar | Layar galat dengan tombol coba lagi; data lama yang sudah tampil tidak dibuang |
| Jaringan mati saat mengunggah foto | Galat per-foto, item tidak dianggap selesai, crew bisa mengulang tanpa kehilangan isian |
| Jaringan mati di tengah finalisasi | Draft dipertahankan; percobaan ulang aman karena RPC idempoten |
| RPC menolak (status berubah, TTD ganda) | Tampilkan pesan dari server apa adanya — pesannya sudah berbahasa Indonesia dan sudah user-facing |
| RLS menolak (403) | "Anda tidak punya akses ke outlet ini" dan kembali ke daftar |
| Foto melebihi 200 KB setelah kompres | Kompres ulang pada mutu terendah; bila masih besar, tolak dengan pesan yang menyarankan pencahayaan lebih baik atau bidikan lebih dekat |
| Sesi kedaluwarsa | `AppSession` sudah menangani penyegaran token; kegagalan menyegarkan mengembalikan ke layar login |

Seluruh pesan galat berbahasa Indonesia dan menyebut tindakan yang bisa diambil
pengguna, mengikuti pola `networkErrorMessage` di `AppSession`.

## 8. Pengujian

Uji unit JUnit murni untuk seluruh `domain/`, tanpa dependensi Android:

- `SatuanDistribusiTest` — keempat cabang faktor, termasuk pemetaan implisit
  kg/gram, dan sifat bolak-balik `keDasar(keTampilan(x)) ≈ x` untuk faktor bulat.
- `StatusSuratJalanTest` — pemetaan seluruh enam status ke label, dan aturan
  "ada selisih" untuk item rusak, item kurang, item pas, serta item belum
  diverifikasi (`qty_terima` null tidak boleh dihitung sebagai selisih).
- `RingkasanDistribusiTest` — hitungan per status, tingkat akurasi termasuk kasus
  nol terverifikasi, dan rincian outlet termasuk pemotongan enam teratas.
- `ValidasiVerifikasiTest` — kelima aturan di §6.4, masing-masing pada batasnya.
- `DistribusiAksesTest` — tabel kemampuan di §3, seluruh empat role.

Verifikasi manual di perangkat, dengan surat jalan uji yang diterbitkan dari web:
pindai QR, isi item campuran (satu pas, satu kurang, satu rusak), tutup app di
tengah jalan lalu buka lagi untuk memastikan draft pulih, selesaikan TTD,
finalisasi, lalu **bandingkan baris `ledger_stok` dan `surat_jalan_item` yang
dihasilkan dengan baris dari verifikasi serupa yang dikerjakan lewat web**. Nilai
`qty_terima`, `kondisi`, `flagged`, dan `qty` di ledger harus identik. Ini adalah
pemeriksaan penerimaan yang menegakkan §1.

## 9. Divergensi yang disengaja dari web

Tiga saja, semuanya di lapisan tampilan dan tidak satu pun mengubah data:

1. **Tidak ada pembaruan langsung.** Web berlangganan Realtime Supabase;
   `:core:realtime` native masih kosong. Diganti tarik-untuk-menyegarkan dan
   penyegaran otomatis saat layar kembali aktif.
2. **Daftar bergulir, bukan paginasi bertombol.** Paginasi 8 baris milik web
   adalah pola desktop.
3. **Draft bertahan lebih baik.** Web menyimpan draft di `localStorage` yang bisa
   hilang bersama tab; native menyimpannya di simpanan lokal yang bertahan sampai
   finalisasi berhasil.

## 10. Di luar lingkup Fase 1

Ditunda ke spec sendiri, dengan alasan masing-masing:

- **Fase 2 — ekspor PDF.** `generatePDF.ts` adalah 1.053 baris tata letak jsPDF
  presisi milimeter untuk kertas rangkap tiga 14×12 cm. Porting ke `PdfDocument`
  Android harus cocok milimeter demi milimeter dengan cetakan web agar arsip
  seragam, dan itu pekerjaan tersendiri.
- **Fase 3 — `:core:printer`.** Modul native ini kosong sama sekali. Cetak QR via
  Bluetooth berarti membangun izin Bluetooth Android 12+, penemuan dan pemasangan
  perangkat, encoder ESC/POS, dan raster gambar dari nol. Butuh perangkat keras di
  tangan untuk diuji.
- **Penerbitan surat jalan.** Terhalang gerbang role di database (§3). Baru bisa
  dipertimbangkan bila pemilik sistem web memutuskan mengubah kebijakan itu di
  sisi mereka.
- **Verifikasi PO supplier** (`TerimaBahanList`, `usePOKitchen`). Ini murni alat
  `kitchen`, dan `kitchen` tetap di web.
