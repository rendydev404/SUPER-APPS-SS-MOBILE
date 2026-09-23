# Design system "kaca"

Bahasa visual bersama untuk navigasi modul di Super App: tab bar **liquid glass**
yang mengambang dan lembar menu dengan **latar blur tebal**, plus ikon bergaya
SF Symbols. Tata letak tiap modul boleh berbeda; yang harus sama adalah komponen
dan token di package ini.

| Komponen | Untuk |
|---|---|
| `ShellKaca` | Kerangka layar: isi sampai dasar layar di balik kapsul, bilah di bawah, blur otomatis saat lembar menu terbuka. |
| `BilahTabKaca` + `ItemTabKaca` | Tab bar kapsul kaca: isi di belakang dibiaskan seperti lensa, tetes kaca aktif meluncur dengan pegas. |
| `LembarMenuKaca`, `JudulKelompokMenuKaca`, `BarisMenuKaca` | Lembar "Lainnya/Menu/More": kartu putih, ikon oranye, tanpa kotak warna. |
| `denganRuangNav()`, `navigationBarsPaddingKaca()`, `LocalRuangNavKaca` | Ruang bawah untuk daftar dan bilah tombol yang tampil di dalam shell. |
| `IkonIos` | Ikon bergaya SF Symbols (digambar ulang; bukan aset Apple). `IkonIos.aktif(ikon)` memberi varian terisi/tebal. |
| `TokenKaca` | Warna aksen, label, ukuran kapsul, radius blur. |

## Pola pakai

```kotlin
var menu by rememberSaveable { mutableStateOf(false) }
val lembar = rememberModalBottomSheetState(skipPartiallyExpanded = true)
val scope = rememberCoroutineScope()

ShellKaca(
    bilah = { latar ->
        BilahTabKaca(
            item = listOf(ItemTabKaca("Beranda", IkonIos.Home), ItemTabKaca("Menu", IkonIos.Menu)),
            indeksAktif = if (menu) 1 else 0,
            latar = latar,
            onPilih = { i -> if (i == 1) menu = true },
        )
    },
) {
    LayarBeranda()   // daftarnya: contentPadding = PaddingValues(16.dp).denganRuangNav()

    // Lembar dikomposisikan DI DALAM isi shell supaya latarnya ikut diburamkan.
    if (menu) {
        LembarMenuKaca(onTutup = { menu = false }, sheetState = lembar, judul = "Menu") {
            JudulKelompokMenuKaca("Operasional")
            BarisMenuKaca(IkonIos.Receipt, "Laporan", onKlik = {
                scope.launch { lembar.hide() }.invokeOnCompletion { menu = false /* lalu navigasi */ }
            })
        }
    }
}
```

## Aturan

- Isi di dalam shell berada di belakang kapsul: setiap daftar wajib `denganRuangNav()`,
  setiap bilah tombol yang dipatok di dasar layar wajib `navigationBarsPaddingKaca()`.
  Keduanya jangan dipakai di dalam `ModalBottomSheet`/Dialog.
- Warna aktif selalu `TokenKaca.Aksen` (#EA580C) — jangan diganti per modul.
- Ikon di bilah dan lembar menu memakai `IkonIos`. Ikon baru ditambahkan di `IkonIos.kt`
  dengan idiom yang sama (kisi 24, garis bulat 1.7, varian terisi bila perlu).
- Blur & pembiasan butuh Android 12+. Di bawah itu komponen otomatis jatuh ke kaca
  susu/lembar pekat — tidak perlu penanganan di pemanggil.
