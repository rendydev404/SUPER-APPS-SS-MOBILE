package com.sukashawarma.superapp.feature.stok.domain

import com.sukashawarma.superapp.feature.stok.data.model.MonitoringRow

/**
 * Papan pantau stok lintas outlet untuk Gudang Pusat — cermin `SPVDashboard.tsx` dan
 * `LiveMonitoringPage.tsx` web yang dibuka role `kitchen`.
 *
 * Semua perhitungan di sini murni di memori atas satu kali muat seluruh saldo. Status
 * tiap bahan memakai [MonitoringRow.status] (skala ternormalisasi), BUKAN kolom
 * `status` view: layar detail outlet juga menghitung ulang, dan papan ini tidak boleh
 * menyebut outlet "Kritis" lalu isinya tampil "Aman" begitu dibuka.
 */
enum class WilayahOutlet(val label: String) {
    PUSAT("Pusat"),
    BOGOR("Bogor"),
    DEPOK("Depok"),
    JAKARTA("Jakarta"),
    BEKASI("Bekasi"),
    TANGERANG("Tangerang"),
    LAINNYA("Lainnya"),
}

/** Filter status outlet — pil "Semua / Kritis / Menipis / Aman" papan web. */
enum class FilterPantau { SEMUA, KRITIS, MENIPIS, AMAN }

data class RingkasOutlet(
    val outletId: String,
    val nama: String,
    val wilayah: WilayahOutlet,
    val status: StokStatus,
    val jumlahKritis: Int,
    val jumlahMenipis: Int,
    val jumlahBahan: Int,
    /** Bahan kritis lalu menipis, masing-masing dari yang paling parah. */
    val bahanRendah: List<MonitoringRow>,
) {
    val namaPendek: String get() = PantauOutlet.namaPendek(nama)

    /** Persentase bahan aman — "Health" di dashboard web. */
    val persenSehat: Int
        get() = if (jumlahBahan == 0) 100
        else ((jumlahBahan - jumlahKritis - jumlahMenipis) * 100 / jumlahBahan)
}

data class PapanPantau(
    /** Gudang Pusat ditampilkan terpisah di atas, seperti panel "Kitchen Pusat" web. */
    val hub: RingkasOutlet?,
    val outlets: List<RingkasOutlet>,
    /** Tiga bahan kritis paling parah lintas outlet (tanpa hub). */
    val prioritas: List<MonitoringRow>,
) {
    val jumlahKritis: Int get() = outlets.count { it.status == StokStatus.BELOW }
    val jumlahMenipis: Int get() = outlets.count { it.status == StokStatus.WARNING }
    val jumlahAman: Int get() = outlets.size - jumlahKritis - jumlahMenipis

    companion object {
        val KOSONG = PapanPantau(null, emptyList(), emptyList())
    }
}

object PantauOutlet {

    /**
     * Outlet yang tidak ikut papan. Kantor Pusat tidak memegang stok bahan (web juga
     * menyaringnya), dan outlet uji/sistem/marketplace hanya mengacaukan hitungan —
     * papan live web mengecualikannya dengan alasan yang sama.
     */
    private val KATA_DIKECUALIKAN = listOf(
        "KANTOR PUSAT", "GLOBAL OUTLET", "GLOBAL SYSTEM", "OUTLET TES", "OUTLET TEST",
        "SHOOPE", "SHOPEE", "TITKOSHOP", "TIKTOK",
    )

    private const val HUB = "GUDANG PUSAT"

    fun dikecualikan(nama: String): Boolean {
        val n = nama.uppercase()
        return KATA_DIKECUALIKAN.any { it in n }
    }

    fun adalahHub(nama: String): Boolean = HUB in nama.uppercase()

    /** Cermin `getOutletRegion` di `SPVDashboard.tsx` (berbasis nama outlet). */
    fun wilayah(nama: String): WilayahOutlet {
        val n = nama.uppercase()
        return when {
            "GUDANG" in n -> WilayahOutlet.PUSAT
            "BNR" in n -> WilayahOutlet.BOGOR
            listOf("PEKAYON", "JATIASIH", "JATIWARINGIN", "JATIWANGIN").any { it in n } -> WilayahOutlet.BEKASI
            // PAMULANG tidak ada di daftar web (jatuh ke Jakarta); secara geografis Tangerang Selatan.
            listOf("CIRENDEU", "PAMULANG").any { it in n } -> WilayahOutlet.TANGERANG
            listOf(
                "CIBINONG", "CISEENG", "CITAYAM", "DRAMAGA", "EMPANG", "CIMANGGU", "CIBUBUR",
                "PAJAJARAN", "PAJA JARAN", "PALEDANG", "CICURUG", "SENTUL", "CILEUNGSI", "BOGOR",
            ).any { it in n } -> WilayahOutlet.BOGOR
            listOf("DEPOK", "SUKMAJAYA", "BEJI", "SAWANGAN").any { it in n } -> WilayahOutlet.DEPOK
            listOf("TEBET", "KALISARI", "JAGAKARSA", "JAKARTA").any { it in n } -> WilayahOutlet.JAKARTA
            else -> WilayahOutlet.LAINNYA
        }
    }

    /** "SUKA SHAWARMA BEJI" -> "BEJI", "MITRA CIBINONG" -> "CIBINONG" — cermin `cleanOutletName`. */
    fun namaPendek(nama: String): String {
        val bersih = nama
            .replace(Regex("MITRA SUKA SHAWARMA|SUKA SHAWARMA|MITRA", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        return bersih.ifEmpty { nama.trim() }
    }

    /** Seberapa jauh saldo dari batas minimum; makin kecil makin parah. */
    fun rasio(row: MonitoringRow): Double {
        val saldo = row.saldoNorm ?: return Double.MAX_VALUE
        val batas = row.thresholdNorm?.takeIf { it > 0.0 } ?: return Double.MAX_VALUE
        return saldo / batas
    }

    /**
     * Susun papan dari seluruh saldo. [outlets] adalah daftar master outlet aktif
     * (id -> nama): outlet tanpa baris saldo tetap tampil, dan baris saldo milik outlet di
     * luar daftar (nonaktif) diabaikan.
     * [marquee] per outlet mengikuti `outlets.marquee_warning_threshold`.
     */
    fun susun(
        baris: List<MonitoringRow>,
        outlets: List<Pair<String, String>>,
        marquee: (String) -> Int = { UnitScale.DEFAULT_MARQUEE_WARNING },
    ): PapanPantau {
        val perOutlet = baris.groupBy { it.outletId }

        // Hanya outlet di daftar master (aktif). Sengaja TANPA jalur cadangan dari baris
        // saldo: `stok_balance` outlet yang sudah ditutup (mis. JATIASIH, is_active=false)
        // masih ikut di view, dan dulu akan muncul sebagai kartu "Outlet" berstatus kritis.
        val semua = outlets
            .distinctBy { it.first }
            .filter { (_, nama) -> !dikecualikan(nama) }
            .map { (id, nama) -> ringkas(id, nama, perOutlet[id].orEmpty(), marquee(id)) }

        val hub = semua.firstOrNull { adalahHub(it.nama) }
        val grid = semua
            .filter { it !== hub }
            .sortedWith(
                compareBy<RingkasOutlet>(
                    { it.wilayah.ordinal },
                    { urutanStatus(it.status) },
                    { it.nama.uppercase() },
                ),
            )

        val prioritas = grid
            // bahanRendah diurutkan kritis lebih dulu, jadi potongan depannya = bahan kritis.
            .flatMap { o -> o.bahanRendah.take(o.jumlahKritis) }
            .sortedBy { rasio(it) }
            .take(3)

        return PapanPantau(hub, grid, prioritas)
    }

    private fun ringkas(id: String, nama: String, isi: List<MonitoringRow>, marquee: Int): RingkasOutlet {
        val berstatus = isi
            // Bahan milik gudang pusat disembunyikan dari outlet biasa, sama seperti layar outlet.
            .filter { bolehTampilDiOutlet(it.itemName, it.outletName) }
            .map { it to it.status(marqueeWarning = marquee) }
        val kritis = berstatus.filter { it.second == StokStatus.BELOW }.map { it.first }
        val menipis = berstatus.filter { it.second == StokStatus.WARNING }.map { it.first }
        val status = when {
            kritis.isNotEmpty() -> StokStatus.BELOW
            menipis.isNotEmpty() -> StokStatus.WARNING
            else -> StokStatus.OK
        }
        return RingkasOutlet(
            outletId = id,
            nama = nama,
            wilayah = wilayah(nama),
            status = status,
            jumlahKritis = kritis.size,
            jumlahMenipis = menipis.size,
            jumlahBahan = berstatus.size,
            bahanRendah = kritis.sortedBy { rasio(it) } + menipis.sortedBy { rasio(it) },
        )
    }

    fun saring(outlets: List<RingkasOutlet>, filter: FilterPantau, cari: String): List<RingkasOutlet> {
        val kata = cari.trim().uppercase()
        return outlets.filter { o ->
            val cocokStatus = when (filter) {
                FilterPantau.SEMUA -> true
                FilterPantau.KRITIS -> o.status == StokStatus.BELOW
                FilterPantau.MENIPIS -> o.status == StokStatus.WARNING
                FilterPantau.AMAN -> o.status == StokStatus.OK
            }
            cocokStatus && (kata.isEmpty() || kata in o.nama.uppercase())
        }
    }

    private fun urutanStatus(status: StokStatus): Int = when (status) {
        StokStatus.BELOW -> 0
        StokStatus.WARNING -> 1
        StokStatus.UNKNOWN -> 2
        StokStatus.OK -> 3
    }
}
