package com.sukashawarma.superapp.feature.stok.domain

/**
 * Tingkat keyakinan sebuah nilai persediaan — cermin `StatusNilai` di
 * `hooks/useNilaiPersediaan.ts`.
 *
 * Ketiganya sengaja dipisah supaya angka yang belum bisa dipegang tidak menyusup
 * ke total seolah pasti. `SKALA_BELUM_PASTI` hilang sendiri setelah outlet
 * meng-opname bahan tersebut; `DATA_BELUM_LENGKAP` berarti harga beli atau isi
 * kemasan belum diisi — bukan berarti stoknya tidak bernilai.
 */
enum class StatusNilai(val nilai: String, val label: String) {
    PASTI("pasti", "Pasti"),
    SKALA_BELUM_PASTI("skala_belum_pasti", "Skala belum pasti"),
    DATA_BELUM_LENGKAP("data_belum_lengkap", "Data belum lengkap");

    companion object {
        fun dari(nilai: String?): StatusNilai =
            entries.firstOrNull { it.nilai == nilai } ?: DATA_BELUM_LENGKAP
    }
}

/** Satu bahan di satu outlet, apa adanya dari view `nilai_persediaan_spv`. */
data class BarisNilai(
    val outletId: String,
    val outlet: String,
    val outletType: String?,
    val bahanBakuId: String,
    val bahan: String,
    val kategori: String?,
    val satuan: String?,
    val saldo: Double,
    val hargaBeli: Double?,
    val status: StatusNilai,
    val jumlahSatuanBesar: Double,
    val nilai: Double,
    val nilaiMin: Double,
    val nilaiMax: Double,
)

/** Ringkasan satu outlet, hasil agregasi baris-barisnya. */
data class RingkasNilaiOutlet(
    val outletId: String,
    val outlet: String,
    /** Nilai dari baris berstatus pasti. Angka ini bisa dipegang. */
    val nilaiPasti: Double,
    /** Tafsir terbaik untuk baris yang skalanya belum pasti. */
    val nilaiBelumPasti: Double,
    val batasBawah: Double,
    val batasAtas: Double,
    val jumlahBahan: Int,
    val jumlahBelumPasti: Int,
    val jumlahDataKurang: Int,
    val items: List<BarisNilai>,
) {
    val total: Double get() = nilaiPasti + nilaiBelumPasti
}

/** Total lintas outlet. */
data class TotalNilai(
    val nilaiPasti: Double = 0.0,
    val nilaiBelumPasti: Double = 0.0,
    val batasBawah: Double = 0.0,
    val batasAtas: Double = 0.0,
    val jumlahBelumPasti: Int = 0,
    val jumlahDataKurang: Int = 0,
) {
    val total: Double get() = nilaiPasti + nilaiBelumPasti
}

/**
 * Agregasi per outlet — port `ringkas()` di web.
 *
 * Batas bawah dan atas dijumlahkan dari SELURUH baris, termasuk yang datanya
 * belum lengkap, sedangkan `nilaiPasti`/`nilaiBelumPasti` hanya dari baris yang
 * memang punya angka. Itu yang membuat rentangnya jujur: total bisa lebih besar
 * daripada yang tercatat, dan halaman harus bisa menunjukkan itu.
 */
fun ringkasNilaiPerOutlet(baris: List<BarisNilai>): List<RingkasNilaiOutlet> {
    val per = LinkedHashMap<String, MutableList<BarisNilai>>()
    baris.forEach { per.getOrPut(it.outletId) { mutableListOf() }.add(it) }

    return per.map { (outletId, isi) ->
        RingkasNilaiOutlet(
            outletId = outletId,
            outlet = isi.first().outlet,
            nilaiPasti = isi.filter { it.status == StatusNilai.PASTI }.sumOf { it.nilai },
            nilaiBelumPasti = isi.filter { it.status == StatusNilai.SKALA_BELUM_PASTI }.sumOf { it.nilai },
            batasBawah = isi.sumOf { it.nilaiMin },
            batasAtas = isi.sumOf { it.nilaiMax },
            jumlahBahan = isi.size,
            jumlahBelumPasti = isi.count { it.status == StatusNilai.SKALA_BELUM_PASTI },
            jumlahDataKurang = isi.count { it.status == StatusNilai.DATA_BELUM_LENGKAP },
            // Bahan termahal di atas: itulah yang menentukan angka totalnya.
            items = isi.sortedByDescending { it.nilai },
        )
    }.sortedByDescending { it.total }
}

fun totalNilai(outlets: List<RingkasNilaiOutlet>): TotalNilai = TotalNilai(
    nilaiPasti = outlets.sumOf { it.nilaiPasti },
    nilaiBelumPasti = outlets.sumOf { it.nilaiBelumPasti },
    batasBawah = outlets.sumOf { it.batasBawah },
    batasAtas = outlets.sumOf { it.batasAtas },
    jumlahBelumPasti = outlets.sumOf { it.jumlahBelumPasti },
    jumlahDataKurang = outlets.sumOf { it.jumlahDataKurang },
)
