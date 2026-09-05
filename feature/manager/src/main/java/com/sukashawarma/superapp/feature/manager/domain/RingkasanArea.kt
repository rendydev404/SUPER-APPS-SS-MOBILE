package com.sukashawarma.superapp.feature.manager.domain

import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.feature.manager.data.model.AbsenMasuk
import com.sukashawarma.superapp.feature.manager.data.model.OutletRingkas
import com.sukashawarma.superapp.feature.manager.data.model.PesananRingkas
import com.sukashawarma.superapp.feature.manager.data.model.WasteDisetujui
import kotlin.math.abs
import kotlin.math.roundToLong

/** Satu outlet pada papan peringkat omzet. */
data class PeringkatOutlet(
    val id: String,
    val nama: String,
    val zona: String,
    val omzet: Long,
)

/** Satu zona area manager beserta cabang di bawahnya. */
data class PerformaZona(
    val zona: String,
    val outlets: List<PeringkatOutlet>,
    val totalOmzet: Long,
)

/** Baris pada daftar "Status Outlet": buka sejak jam berapa, atau tutup. */
data class StatusOutlet(
    val id: String,
    val nama: String,
    val zona: String,
    /** null = belum ada absen masuk hari ini, yaitu outlet masih tutup. */
    val jamBuka: String?,
)

/** Perubahan relatif terhadap periode pembanding, dalam persen. */
data class Perubahan(val persen: Double) {
    val naik: Boolean get() = persen >= 0
    /** Angka tanpa tanda, satu desimal — bentuk yang ditampilkan badge KPI. */
    val besaranTeks: String get() = String.format(java.util.Locale.US, "%.1f", abs(persen))
}

/**
 * Seluruh isi layar Ringkasan Area dalam satu nilai.
 *
 * Dihitung sekali dari baris mentah, lalu dipakai apa adanya oleh UI. Layar tidak
 * boleh menghitung ulang apa pun dari daftar mentah — itu jalan tercepat menuju
 * dua angka yang tidak sepakat di layar yang sama.
 */
data class RingkasanArea(
    val omzet: Long,
    val jumlahTransaksi: Int,
    val jumlahItem: Int,
    val perubahanOmzet: Perubahan,
    val kerugianWaste: Long,
    val wasteMenungguPersetujuan: Int,
    val estimasiBonus: Long,
    val perubahanBonus: Perubahan,
    val peringkat: List<PeringkatOutlet>,
    val omzetTertinggi: Long,
    val zona: List<PerformaZona>,
    val statusOutlet: List<StatusOutlet>,
    val jumlahCabang: Int,
) {
    val totalOmzetSemuaZona: Long get() = zona.sumOf { it.totalOmzet }
    val jumlahOutletDalamZona: Int get() = zona.sumOf { it.outlets.size }
    val zonaTertinggi: PerformaZona? get() = zona.firstOrNull()
    val rataRataOmzetPerZona: Long
        get() = if (zona.isEmpty()) 0L else totalOmzetSemuaZona / zona.size

    companion object {
        val KOSONG = RingkasanArea(
            omzet = 0, jumlahTransaksi = 0, jumlahItem = 0, perubahanOmzet = Perubahan(0.0),
            kerugianWaste = 0, wasteMenungguPersetujuan = 0, estimasiBonus = 0,
            perubahanBonus = Perubahan(0.0), peringkat = emptyList(), omzetTertinggi = 0,
            zona = emptyList(), statusOutlet = emptyList(), jumlahCabang = 0,
        )
    }
}

/**
 * Tarif insentif per porsi terjual. Cermin `BonusKpiCard` web: manajer Rp 50,
 * leader dan crew Rp 100. Modul ini hanya terbuka untuk manajer, tapi tarifnya
 * tetap diturunkan dari role supaya angkanya tidak diam-diam berbeda dari web
 * bila daftar role modul berubah kelak.
 */
fun tarifBonus(role: Role?): Long = when (role) {
    Role.LEADER, Role.CREW -> 100L
    else -> 50L
}

/**
 * Persentase perubahan terhadap periode pembanding.
 *
 * Pembagi nol tidak menghasilkan tak-hingga: dari nol ke angka apa pun dilaporkan
 * sebagai +100%, dan nol ke nol sebagai 0% — persis seperti web, supaya outlet
 * yang baru buka tidak menampilkan "Infinity%".
 */
fun hitungPerubahan(sekarang: Long, sebelumnya: Long): Perubahan = when {
    sebelumnya != 0L -> Perubahan((sekarang - sebelumnya).toDouble() / sebelumnya * 100.0)
    sekarang > 0L -> Perubahan(100.0)
    else -> Perubahan(0.0)
}

/**
 * Rupiah kerugian dari baris waste yang sudah disetujui.
 *
 * Bahan tanpa harga tercatat dihitung nol, bukan dilewati: barisnya tetap ada di
 * laporan, hanya nilainya yang belum diketahui.
 */
fun hitungKerugianWaste(baris: List<WasteDisetujui>, harga: Map<String, Double>): Long =
    baris.sumOf { w ->
        val hargaBeli = harga[w.bahanBakuId] ?: 0.0
        (w.qty * hargaBeli).roundToLong()
    }

/**
 * Urutan daftar outlet: alfabetis, tapi outlet mitra selalu di bawah.
 * Cermin pengurutan `allOutlets` di `app/page.tsx` web.
 */
private val URUTAN_OUTLET = compareBy<OutletRingkas> { it.nama.uppercase().startsWith("MITRA") }
    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.nama }

/**
 * Menyusun seluruh layar dari baris mentah.
 *
 * @param pesanan pesanan selesai pada periode terpilih.
 * @param pesananSebelumnya pesanan selesai pada periode pembanding.
 * @param absenMasuk absen masuk pertama tiap outlet pada hari terakhir periode.
 */
fun susunRingkasanArea(
    outlets: List<OutletRingkas>,
    pesanan: List<PesananRingkas>,
    pesananSebelumnya: List<PesananRingkas>,
    absenMasuk: List<AbsenMasuk>,
    pemetaanAm: Map<String, String>,
    kerugianWaste: Long,
    wasteMenungguPersetujuan: Int,
    rolePengguna: Role?,
    namaPengguna: String?,
): RingkasanArea {
    val omzet = pesanan.sumOf { it.total }
    val omzetSebelumnya = pesananSebelumnya.sumOf { it.total }
    val item = pesanan.sumOf { it.jumlahItem }
    val itemSebelumnya = pesananSebelumnya.sumOf { it.jumlahItem }

    val tarif = tarifBonus(rolePengguna)
    val omzetPerOutlet = pesanan.groupingBy { it.outletId }.fold(0L) { acc, p -> acc + p.total }
    val jamBukaPerOutlet = absenMasuk.associate { it.outletId to it.jamBuka }

    fun zonaOutlet(o: OutletRingkas) =
        AreaManagerNama.untuk(o.id, o.nama, pemetaanAm, rolePengguna, namaPengguna)

    val terurut = outlets.sortedWith(URUTAN_OUTLET)

    // Peringkat dan tabel zona hanya memandang outlet aktif; daftar status memandang
    // semuanya, supaya cabang yang sedang dinonaktifkan tetap terlihat sebagai tutup.
    val peringkat = terurut
        .filter { it.aktif }
        .map { PeringkatOutlet(it.id, it.nama, zonaOutlet(it), omzetPerOutlet[it.id] ?: 0L) }
        .filterNot { AreaManagerNama.diabaikan(it.zona) }
        .sortedByDescending { it.omzet }

    val zona = peringkat
        .groupBy { it.zona }
        .map { (nama, isi) -> PerformaZona(nama, isi, isi.sumOf { it.omzet }) }
        .sortedByDescending { it.totalOmzet }

    val status = terurut
        .map { StatusOutlet(it.id, it.nama, zonaOutlet(it), jamBukaPerOutlet[it.id]) }
        .filterNot { AreaManagerNama.diabaikan(it.zona) }

    return RingkasanArea(
        omzet = omzet,
        jumlahTransaksi = pesanan.size,
        jumlahItem = item,
        perubahanOmzet = hitungPerubahan(omzet, omzetSebelumnya),
        kerugianWaste = kerugianWaste,
        wasteMenungguPersetujuan = wasteMenungguPersetujuan,
        estimasiBonus = item * tarif,
        perubahanBonus = hitungPerubahan(item * tarif, itemSebelumnya * tarif),
        peringkat = peringkat,
        omzetTertinggi = peringkat.firstOrNull()?.omzet ?: 0L,
        zona = zona,
        statusOutlet = status,
        jumlahCabang = terurut.size,
    )
}
