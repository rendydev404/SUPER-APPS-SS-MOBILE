package com.sukashawarma.superapp.feature.stok.domain

/**
 * Apa yang harus dilakukan saat kru menekan "Mulai Opname Hari Ini".
 *
 * Dipisahkan dari [com.sukashawarma.superapp.feature.stok.data.OpnameRepository] supaya
 * urutan prioritasnya bisa diuji tanpa jaringan — dan urutan itulah yang dulu salah.
 */
sealed interface KeputusanDraftOpname {
    /** Lanjutkan draft yang sudah ada. */
    data class Lanjutkan(val opnameId: String) : KeputusanDraftOpname

    /** Buat opname baru dengan tipe ini. */
    data class BuatBaru(val tipe: String) : KeputusanDraftOpname

    /** Tidak ada yang bisa dikerjakan; kembalikan baris ini apa adanya agar pemanggil menjelaskannya. */
    data class Tampilkan(val opnameId: String) : KeputusanDraftOpname
}

/**
 * Memutuskan draft mana yang dibuka hari ini.
 *
 * ## Kenapa draft yang berjalan DIDAHULUKAN
 *
 * Ini yang dulu terbalik dan menyebabkan bug nyata (9 Sep 2026, outlet tes): pencarian
 * hanya melihat opname bertipe sama (`harian`), sehingga draft `ad_hoc` yang sedang
 * berjalan tidak pernah terlihat. Yang ketemu justru opname `harian` yang sudah
 * `finalized`, dan karena outlet itu berjatah banyak, setiap penekanan tombol membuat
 * draft `ad_hoc` BARU. Hitungan yang sudah disimpan kru jadi yatim dan mustahil
 * dilanjutkan — persis keluhan "sudah simpan draft tapi tidak bisa lanjut".
 *
 * Web tidak kena karena `OpnameForm` memanggil `fetchTodayOpnameDraftAction` lebih dulu,
 * dan kueri itu menyaring `status='draft'` TANPA memandang tipe. Urutan itu yang ditiru
 * di sini.
 *
 * @param draftBerjalanId draft hari ini apa pun tipenya, bila ada.
 * @param sudahAdaId      opname hari ini bertipe [tipe] yang bukan `rejected`, bila ada.
 * @param sudahAdaFinal   apakah [sudahAdaId] sudah difinalisasi.
 * @param jumlahHariIni   banyaknya opname hari ini yang bukan `rejected`.
 * @param maksimalHariIni jatah opname untuk outlet & tanggal ini — lihat [OpnameTanggal].
 */
fun putuskanDraftOpname(
    tipe: String,
    draftBerjalanId: String?,
    sudahAdaId: String?,
    sudahAdaFinal: Boolean,
    jumlahHariIni: Int,
    maksimalHariIni: Int,
): KeputusanDraftOpname = when {
    // 1. Ada draft berjalan -> lanjutkan, apa pun tipenya.
    draftBerjalanId != null -> KeputusanDraftOpname.Lanjutkan(draftBerjalanId)

    // 2. Belum ada apa pun hari ini -> buat baru dengan tipe yang diminta.
    sudahAdaId == null -> KeputusanDraftOpname.BuatBaru(tipe)

    // 3. Yang ada sudah final DAN jatahnya belum habis -> opname tambahan sebagai ad_hoc.
    sudahAdaFinal && maksimalHariIni > 1 && jumlahHariIni < maksimalHariIni ->
        KeputusanDraftOpname.BuatBaru("ad_hoc")

    // 4. Sisanya tidak bisa disunting; pemanggil yang menjelaskan sebabnya.
    else -> KeputusanDraftOpname.Tampilkan(sudahAdaId)
}
