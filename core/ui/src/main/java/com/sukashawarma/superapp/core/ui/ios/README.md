# Design system "iOS"

Bahasa visual kartu & kontrol untuk semua layar Super App — pasangan design system
[`kaca`](../kaca/README.md) (tab bar & lembar menu). Tata letak tiap modul boleh berbeda;
yang harus sama adalah token dan komponen di package ini.

## Token (`TokenIos.kt`)

| Token | Isi |
|---|---|
| `WarnaIos` | Warna sistem iOS: `Latar` #F2F2F7, `Kartu` putih, `Label`, `LabelKedua` (60%), `LabelKetiga`, `Isian`, `Pemisah`, `Aksen` (= `TokenKaca.Aksen` #EA580C), `Merah`, `Oranye`, `Hijau`, `Biru`, … |
| `NadaIos` | Nada semantik (`SUKSES`, `PERINGATAN`, `BAHAYA`, `INFO`, `AKSEN`, `NETRAL`, `UNGU`) — `.warna` untuk isian, `.teks` untuk teks yang terbaca di atasnya. |
| `TipeIos` | Skala Dynamic Type: `JudulBesar`, `Judul1-3`, `Utama` (17 semibold), `Isi`, `Keterangan`, `SubJudul`, `Catatan` (13 abu), `Kecil`, `Angka`, `AngkaBesar`. |
| `UkuranIos` | Sudut (kartu 20, grup/petak 16, kontrol 12, blok 14), tepi layar 16, jarak kartu 12, tinggi kontrol 40, tombol 50. |

## Komponen (`KomponenIos.kt`)

| Komponen | Untuk |
|---|---|
| `KartuIos` | Kartu putih dasar; `onKlik` memberi efek tekan iOS. |
| `GrupIos` + `BarisIos` + `PemisahIos` | Daftar "inset grouped" ala Pengaturan: rincian, menu, formulir baca. |
| `JudulSeksiIos`, `LabelSeksiIos` | Judul seksi besar / label kapital kecil di atas grup. |
| `LencanaIos` | Status kapsul bernada dengan titik. |
| `PetakStatIos` | Petak KPI ala smart list Pengingat. |
| `BlokAngkaIos` + `AngkaIos` | Beberapa angka bersekat dalam blok abu. |
| `KolomCariIos` | Kolom cari abu tanpa garis tepi. |
| `TombolBundarIos`, `TombolKapsulIos`, `TombolUtamaIos`, `TombolKeduaIos` | Tombol toolbar, pemicu menu/filter, aksi utama & sekunder. |
| `BilahJudulIos` | Bilah judul dengan tombol kembali bulat. |
| `KeadaanIos` | Keadaan kosong / gagal / tanpa akses. |
| `Modifier.tekanIos`, `bayanganIos`, `permukaanIos` | Efek tekan (skala, tanpa riak), bayangan lembut, permukaan kartu. |

## Kontrol (`KontrolIos.kt`)

| Komponen | Untuk |
|---|---|
| `WadahSegmenIos` + `SegmenIos` | Segmented control (tab, periode); `gulir = true` untuk deret panjang. |
| `KapsulPilihanIos` | Kapsul filter/pilihan pengganti chip Material, dengan hitungan opsional. |
| `IkonBulatIos` | Ikon dalam lingkaran — `padat` (isi penuh, ikon putih) atau tipis bernada. |
| `PanelGalatIos` | Galat di dalam daftar dengan tombol "Coba Lagi". |
| `warnaKolomIos()`, `warnaSaklarIos()` | Warna `OutlinedTextField` & `Switch` Material agar serasi dengan kartu iOS. |

Sebelum membuat helper lokal di modul, cek dulu tabel di atas; kalau memang belum ada,
tambahkan ke core supaya modul lain ikut memakai versi yang sama.

Dropdown: `SukaDropdownMenu` / `SukaDropdownMenuItem` / `SukaDropdownHeader` /
`SukaFilterDropdown` (package `core.ui`) sudah bergaya pull-down menu iOS — layar di
belakang diburamkan oleh compositor sistem (ringan), menu putih tembus, pemisah hairline,
centang untuk pilihan aktif. Jangan memakai Material `DropdownMenu` langsung.

Contoh lengkap: `feature/stok/.../monitoring/MonitoringScreen.kt` (Dashboard Stok).

## Aturan

- Latar halaman `WarnaIos.Latar`; kartu putih tanpa garis tepi, bayangan lembut.
- Teks isi tidak kapital semua; kapital kecil hanya untuk `LabelSeksiIos`.
- Warna status selalu lewat `NadaIos`, bukan hex lepas.
- Efek tekan `tekanIos`, bukan riak Material.
- Performa: jangan pasang blur/RenderEffect atau animasi tak berujung di item daftar.
  `tekanIos` hanya mengubah `graphicsLayer`, `bayanganIos` dikerjakan RenderThread.
