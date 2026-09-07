package com.sukashawarma.superapp.feature.manager.domain

import java.time.LocalDate

/** Staf yang muncul di papan monitoring. */
data class StafMonitoring(
    val id: String,
    val nama: String,
    val role: String,
    /** Outlet induk. Bisa null untuk role pusat yang tidak terikat satu cabang. */
    val outletId: String?,
)

/** Satu ketukan absen, sudah dinormalkan ke tanggal dan jam Jakarta saat dibaca. */
data class AbsenBaris(
    val outletId: String,
    val stafId: String,
    /** `in` atau `out`, apa adanya seperti kolom `attendance.type`. */
    val tipe: String,
    val tanggal: LocalDate?,
    val jam: String,
    /** Untuk mengurutkan dua absen pada hari yang sama. */
    val tsServer: String,
) {
    val masuk: Boolean get() = tipe == "in"
}

/** Opname harian sebuah outlet. */
data class OpnameBaris(val outletId: String, val tanggal: LocalDate?, val jam: String)

/** Outlet pada papan monitoring, termasuk wilayahnya. */
data class OutletMonitoring(
    val id: String,
    val nama: String,
    val region: String?,
) {
    /**
     * Nama pendek untuk lencana "Hadir di ...". Awalan yang sama pada semua cabang
     * hanya memakan tempat di layar sempit.
     */
    val namaPendek: String
        get() = nama
            .replace(Regex("^SUKA SHAWARMA\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^MITRA\\s+", RegexOption.IGNORE_CASE), "")

    val wilayah: String get() = region?.takeIf { it.isNotBlank() } ?: "Lain-lain / Unassigned"
}

/**
 * Keadaan kuncian POS sebuah outlet pada satu hari.
 *
 * Urutan pemeriksaannya menentukan hasil, dan urutan itu cermin `renderOutletCardForDate`
 * di web: "semua sudah pulang" diperiksa SEBELUM "ada yang masuk", supaya outlet yang
 * sudah tutup tidak dilaporkan terbuka hanya karena tadi pagi ada yang absen masuk.
 */
sealed interface StatusPos {
    val terbuka: Boolean get() = this is Terbuka

    /** Belum ada seorang pun absen — POS masih terkunci menunggu kru. */
    data object MenungguAbsen : StatusPos

    /** Seluruh kru yang tercatat hari itu sudah absen pulang. */
    data class Tutup(val jam: String?) : StatusPos

    /** Ada yang masuk, tapi checklist buka belum tuntas. */
    data class ChecklistBelumSelesai(val selesai: Int, val total: Int) : StatusPos

    data object Terbuka : StatusPos

    val label: String
        get() = when (this) {
            MenungguAbsen -> "Terkunci - Menunggu Absen"
            is Tutup -> if (jam != null) "Terkunci - Tutup Shift ($jam)" else "Terkunci - Tutup"
            is ChecklistBelumSelesai -> "Terkunci - Checklist Belum Selesai ($selesai/$total)"
            Terbuka -> "Terbuka - Siap Transaksi"
        }
}

/** Keadaan absen seorang kru pada kartu outlet tertentu. */
sealed interface StatusAbsen {
    data object BelumAbsen : StatusAbsen
    data object Hadir : StatusAbsen
    data object Pulang : StatusAbsen

    /** Absen tercatat di cabang lain — penting bagi leader yang berpindah outlet. */
    data class HadirDiOutletLain(val namaOutlet: String) : StatusAbsen
    data class PulangDiOutletLain(val namaOutlet: String) : StatusAbsen

    val label: String
        get() = when (this) {
            BelumAbsen -> "Belum Absen"
            Hadir -> "Hadir"
            Pulang -> "Pulang"
            is HadirDiOutletLain -> "Hadir di $namaOutlet"
            is PulangDiOutletLain -> "Pulang di $namaOutlet"
        }

    val diCabangLain: Boolean
        get() = this is HadirDiOutletLain || this is PulangDiOutletLain
}

data class KruMonitoring(
    val id: String,
    val nama: String,
    val role: String,
    val status: StatusAbsen,
    val jam: String,
)

/** Satu kartu papan monitoring: sebuah outlet pada sebuah tanggal. */
data class KartuMonitoring(
    val outlet: OutletMonitoring,
    val tanggal: LocalDate,
    val statusPos: StatusPos,
    val jamBuka: String?,
    val jamTutup: String?,
    val jamOpname: String?,
    val kru: List<KruMonitoring>,
)

enum class FilterStatusPos(val label: String) {
    SEMUA("Semua Status"),
    TERBUKA("POS Terbuka"),
    TERKUNCI("POS Terkunci"),
}

enum class FilterKru(val label: String) {
    SEMUA("Semua Kru"),
    HADIR("Sudah Hadir"),
    BELUM_HADIR("Belum Hadir"),
}

/** Urutan tampil kru: SPV, leader, crew, sisanya; lalu alfabetis. */
private fun peringkatRole(role: String): Int = when (role.lowercase()) {
    "spv" -> 1
    "leader" -> 2
    "crew" -> 3
    else -> 99
}

private val URUTAN_KRU = compareBy<KruMonitoring> { peringkatRole(it.role) }
    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.nama }

/**
 * Menyusun satu kartu outlet untuk satu tanggal.
 *
 * @param wajibChecklist id item checklist buka yang wajib di outlet ini.
 * @param sudahDicentang id item yang sudah dicentang. Sama seperti web, keduanya
 *   TIDAK dipisah per tanggal — pada rentang banyak hari, kemajuan checklist yang
 *   ditampilkan adalah gabungan seluruh rentang.
 */
fun susunKartuMonitoring(
    outlet: OutletMonitoring,
    tanggal: LocalDate,
    staf: List<StafMonitoring>,
    absen: List<AbsenBaris>,
    opname: List<OpnameBaris>,
    outlets: List<OutletMonitoring>,
    wajibChecklist: List<String>,
    sudahDicentang: Set<String>,
): KartuMonitoring {
    val absenHariIni = absen.filter { it.tanggal == tanggal }
    val absenOutlet = absenHariIni.filter { it.outletId == outlet.id }
    val absenPerStaf = absenHariIni.groupBy { it.stafId }

    val jamBuka = absenOutlet.filter { it.masuk }.minByOrNull { it.tsServer }?.jam
    val jamTutup = absenOutlet.filterNot { it.masuk }.maxByOrNull { it.tsServer }?.jam
    val jamOpname = opname.firstOrNull { it.outletId == outlet.id && it.tanggal == tanggal }?.jam

    // Keadaan terakhir tiap staf DI OUTLET INI, bukan lintas cabang: kuncian POS
    // sebuah outlet tidak boleh ditentukan oleh absen yang terjadi di cabang lain.
    val terakhirDiOutlet = absenOutlet
        .sortedBy { it.tsServer }
        .associateBy { it.stafId }

    val adaYangMasuk = terakhirDiOutlet.values.any { it.masuk }
    val semuaSudahPulang = terakhirDiOutlet.isNotEmpty() && terakhirDiOutlet.values.none { it.masuk }
    val selesaiChecklist = wajibChecklist.count { it in sudahDicentang }

    val statusPos = when {
        semuaSudahPulang -> StatusPos.Tutup(jamTutup)
        !adaYangMasuk -> StatusPos.MenungguAbsen
        wajibChecklist.isNotEmpty() && selesaiChecklist < wajibChecklist.size ->
            StatusPos.ChecklistBelumSelesai(selesaiChecklist, wajibChecklist.size)
        else -> StatusPos.Terbuka
    }

    val kru = staf.map { s ->
        val diOutletIni = terakhirDiOutlet[s.id]
        val terakhirDiMana = absenPerStaf[s.id]?.maxByOrNull { it.tsServer }
        when {
            diOutletIni != null -> KruMonitoring(
                s.id, s.nama, s.role,
                if (diOutletIni.masuk) StatusAbsen.Hadir else StatusAbsen.Pulang,
                diOutletIni.jam,
            )
            terakhirDiMana != null -> {
                val lain = outlets.find { it.id == terakhirDiMana.outletId }?.namaPendek ?: "Outlet Lain"
                KruMonitoring(
                    s.id, s.nama, s.role,
                    if (terakhirDiMana.masuk) StatusAbsen.HadirDiOutletLain(lain)
                    else StatusAbsen.PulangDiOutletLain(lain),
                    terakhirDiMana.jam,
                )
            }
            else -> KruMonitoring(s.id, s.nama, s.role, StatusAbsen.BelumAbsen, "")
        }
    }.sortedWith(URUTAN_KRU)

    return KartuMonitoring(outlet, tanggal, statusPos, jamBuka, jamTutup, jamOpname, kru)
}

/**
 * Staf yang ditampilkan pada kartu sebuah outlet.
 *
 * SPV ikut di semua outlet karena wewenangnya memang lintas cabang. Pemetaan
 * `staff_outlets` — yang di web menambahkan staf binaan ke outlet kedua — tidak
 * dipakai di sini: RLS `staff_outlets_select_self` hanya memberi baris milik
 * pengguna sendiri, jadi datanya tidak pernah lengkap di klien. Akibatnya staf
 * multi-outlet hanya muncul di outlet induknya. Lihat catatan yang sama pada
 * penamaan zona di [AreaManagerNama].
 */
fun stafUntukOutlet(semua: List<StafMonitoring>, outletId: String): List<StafMonitoring> =
    semua.filter { it.role.equals("spv", ignoreCase = true) || it.outletId == outletId }

/** Apakah kartu lolos penyaring status POS. */
fun lolosFilterPos(status: StatusPos, filter: FilterStatusPos): Boolean = when (filter) {
    FilterStatusPos.SEMUA -> true
    FilterStatusPos.TERBUKA -> status.terbuka
    FilterStatusPos.TERKUNCI -> !status.terbuka
}

/** Apakah seorang kru lolos penyaring kehadiran. */
fun lolosFilterKru(kru: KruMonitoring, filter: FilterKru): Boolean {
    val hadir = kru.status == StatusAbsen.Hadir || kru.status is StatusAbsen.HadirDiOutletLain
    return when (filter) {
        FilterKru.SEMUA -> true
        FilterKru.HADIR -> hadir
        FilterKru.BELUM_HADIR -> !hadir
    }
}

/** Tanggal dalam rentang, terbaru lebih dulu — urutan kartu di web. */
fun tanggalDalamRentang(rentang: RentangTanggal): List<LocalDate> {
    val hasil = mutableListOf<LocalDate>()
    var kursor = rentang.dari
    while (!kursor.isAfter(rentang.sampai)) {
        hasil += kursor
        kursor = kursor.plusDays(1)
    }
    return hasil.reversed()
}
