package com.sukashawarma.superapp.feature.leader.domain

/** Satu cabang binaan beserta angka hariannya — sebaris pada panel "Omzet per Cabang". */
data class BarisOutlet(
    val id: String,
    val nama: String,
    val omzet: Long,
    val transaksi: Int,
)

/** Satu pesanan selesai yang ikut membentuk angka omzet hari ini. */
data class PesananSelesai(
    val outletId: String,
    val total: Long,
    val dibuatPada: String,
)

/** Shift kasir yang sedang berjalan di outlet utama, bila ada. */
data class ShiftAktif(
    val catatanAdmin: String?,
    val disesuaikanPada: String?,
)

/**
 * Isi layar Ringkasan Leader — cermin `app/dashboard/leader/page.tsx` web.
 *
 * Seluruh angkanya sudah jadi di sini, bukan dihitung ulang di Composable: layar
 * yang menghitung sendiri akan menghitung ulang tiap recomposition, dan yang lebih
 * penting, aturannya jadi tidak bisa diuji tanpa merender apa pun.
 */
data class RingkasanLeader(
    val namaOutletUtama: String,
    val jumlahCabang: Int,
    val omzetHariIni: Long,
    val jumlahTransaksi: Int,
    val jamTransaksiTerakhir: String?,
    val sisaPettyCash: Long,
    val adaShiftAktif: Boolean,
    val shift: ShiftAktif?,
    val jumlahItemStok: Int,
    val hadir: Int,
    val totalKru: Int,
    val perOutlet: List<BarisOutlet>,
    /** Kosong berarti akun belum ditugaskan ke cabang mana pun — layar memberi peringatan. */
    val punyaOutlet: Boolean,
) {
    /**
     * Judul kepala halaman. Leader satu cabang melihat nama cabangnya; leader yang
     * membina beberapa melihat cacahnya — cermin `headerTitle` web.
     */
    val judul: String
        get() = if (jumlahCabang > 1) "$jumlahCabang Cabang Binaan" else namaOutletUtama

    /** Omzet dibagi transaksi, dibulatkan ke rupiah penuh. Nol transaksi berarti nol. */
    val rataRataTransaksi: Long
        get() = if (jumlahTransaksi > 0) omzetHariIni / jumlahTransaksi else 0L

    /**
     * Ambang "Kritis" petty cash: Rp 150.000, angka yang sama dengan
     * `isPettyCashHampirHabis` di web. Saldo minus ikut dianggap kritis.
     */
    val pettyCashKritis: Boolean
        get() = sisaPettyCash < AMBANG_PETTY_CASH_KRITIS

    companion object {
        const val AMBANG_PETTY_CASH_KRITIS = 150_000L

        val KOSONG = RingkasanLeader(
            namaOutletUtama = "Cabang",
            jumlahCabang = 0,
            omzetHariIni = 0L,
            jumlahTransaksi = 0,
            jamTransaksiTerakhir = null,
            sisaPettyCash = 0L,
            adaShiftAktif = false,
            shift = null,
            jumlahItemStok = 0,
            hadir = 0,
            totalKru = 0,
            perOutlet = emptyList(),
            punyaOutlet = false,
        )
    }
}

/**
 * Menyusun rincian per cabang: seluruh outlet binaan muncul, termasuk yang belum
 * menjual apa pun hari ini, diurutkan omzet terbesar lebih dulu.
 *
 * Cabang tanpa transaksi sengaja TIDAK disembunyikan — nol omzet pada jam sepuluh
 * pagi adalah kabar yang justru perlu dilihat, bukan baris yang hilang.
 */
fun susunPerOutlet(
    outletIds: List<String>,
    namaOutlet: Map<String, String>,
    pesanan: List<PesananSelesai>,
): List<BarisOutlet> {
    val omzet = HashMap<String, Long>()
    val transaksi = HashMap<String, Int>()
    for (p in pesanan) {
        omzet[p.outletId] = (omzet[p.outletId] ?: 0L) + p.total
        transaksi[p.outletId] = (transaksi[p.outletId] ?: 0) + 1
    }
    return outletIds
        .map { id ->
            BarisOutlet(
                id = id,
                nama = namaOutlet[id] ?: "Cabang",
                omzet = omzet[id] ?: 0L,
                transaksi = transaksi[id] ?: 0,
            )
        }
        .sortedByDescending { it.omzet }
}

/**
 * Jam transaksi terakhir hari ini, atau null bila belum ada satu pun.
 *
 * Dipilih dari cap waktu TERBESAR, bukan dari baris terakhir daftar: urutan yang
 * datang dari server tidak dijamin, dan mengandalkannya pernah membuat "terakhir
 * 08.14" tampil pada pukul sebelas.
 */
fun jamTransaksiTerakhir(pesanan: List<PesananSelesai>): String? =
    pesanan.maxByOrNull { it.dibuatPada }?.let { jamJakarta(it.dibuatPada) }
