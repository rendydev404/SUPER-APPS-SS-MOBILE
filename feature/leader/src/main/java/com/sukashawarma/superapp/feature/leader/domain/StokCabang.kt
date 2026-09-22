package com.sukashawarma.superapp.feature.leader.domain

import com.sukashawarma.superapp.feature.stok.data.model.MonitoringRow
import com.sukashawarma.superapp.feature.stok.domain.StokStatus
import com.sukashawarma.superapp.feature.stok.domain.UnitScale
import com.sukashawarma.superapp.feature.stok.domain.decomposeTriUnit
import com.sukashawarma.superapp.feature.stok.domain.formatAngkaStok
import com.sukashawarma.superapp.feature.stok.domain.formatSatuan

/**
 * Tingkat kecukupan sebuah bahan di cabang.
 *
 * TIDAK diambil dari kolom `status` view `monitoring_view_scoped`. View itu
 * membandingkan `current_qty` mentah dengan `threshold`, padahal sebagian besar
 * saldo (`saldo_is_gram`) tersimpan di satuan terkecil sementara threshold selalu
 * di satuan besar — status view meleset sebesar `faktor_tampilan`. Status di sini
 * dihitung ulang oleh [UnitScale.status] di atas skala yang sudah disamakan, aturan
 * yang sama persis dengan layar Stok.
 */
enum class StatusStok(val label: String, val urutan: Int) {
    KRITIS("Kritis", 0),
    MENIPIS("Menipis", 1),
    AMAN("Aman", 2);

    companion object {
        /**
         * UNKNOWN (faktor satuan bahan belum diisi) dibaca sebagai AMAN. Menandainya
         * kritis akan membanjiri lencana merah dengan hal yang belum tentu bermasalah.
         */
        fun dariSkala(status: StokStatus): StatusStok = when (status) {
            StokStatus.BELOW -> KRITIS
            StokStatus.WARNING -> MENIPIS
            StokStatus.OK, StokStatus.UNKNOWN -> AMAN
        }
    }
}

/** Satu bahan baku di cabang terpilih. */
data class BahanCabang(
    val id: String,
    val nama: String,
    val saldo: Double,
    val satuan: String,
    val batasMinimal: Double?,
    val status: StatusStok,
    /** Saldo siap tampil ("2 Kg 32 Gram"); null = tulis [saldo] mentah dengan [satuan]. */
    val saldoBerjenjang: String? = null,
) {
    val saldoTeks: String get() = saldoBerjenjang ?: "${formatAngkaStok(saldo)} $satuan".trim()

    /** Null saat bahan belum punya titik pesan ulang — layar menyebutnya "belum diatur". */
    val batasTeks: String?
        get() = batasMinimal?.let { "${formatAngkaStok(it)} $satuan".trim() }
}

/**
 * Bangun [BahanCabang] dari baris yang SAMA dengan kartu di layar Monitoring modul
 * Stok: status lewat [MonitoringRow.status] dengan ambang porsi outlet, saldo dipecah
 * [decomposeTriUnit] (port `compositeUnit.ts` web) lalu ditulis dengan
 * [formatAngkaStok]/[formatSatuan] — angka yang tampil di sini dan di sana identik.
 *
 * Jenjang bernilai nol dilewati supaya muat satu baris; bila semuanya nol,
 * ditulis "0" pada satuan besarnya.
 */
fun bahanCabang(
    row: MonitoringRow,
    marqueeWarning: Int = UnitScale.DEFAULT_MARQUEE_WARNING,
): BahanCabang {
    val tri = decomposeTriUnit(
        qty = row.currentQty,
        saldoIsGram = row.saldoIsGram,
        satuanTengah = row.meta.satuanTengah,
        faktorTengah = row.meta.faktorTengah,
        satuanKecil = row.meta.satuanKecil,
        faktorTampilan = row.meta.faktorTampilan,
    )
    val jenjang = buildList {
        if (tri.besar != 0.0) add("${formatAngkaStok(tri.besar)} ${formatSatuan(row.meta.satuan)}")
        if (row.meta.satuanTengah != null && tri.tengah != 0.0) {
            add("${formatAngkaStok(tri.tengah)} ${formatSatuan(row.meta.satuanTengah)}")
        }
        if (row.meta.satuanKecil != null && tri.kecil != 0.0) {
            add("${formatAngkaStok(tri.kecil)} ${formatSatuan(row.meta.satuanKecil)}")
        }
    }.ifEmpty { listOf("0 ${formatSatuan(row.meta.satuan)}") }
    return BahanCabang(
        id = row.bahanBakuId,
        nama = row.itemName,
        saldo = row.currentQty,
        satuan = formatSatuan(row.satuan),
        batasMinimal = row.threshold,
        status = StatusStok.dariSkala(row.status(marqueeWarning = marqueeWarning)),
        saldoBerjenjang = jenjang.joinToString(" ").trim(),
    )
}

/** Cacah status seluruh bahan satu cabang — dipakai lencana Stok Cabang dan kartu Ringkasan. */
data class RingkasanStok(val kritis: Int, val menipis: Int, val total: Int)

fun ringkasStok(daftar: List<BahanCabang>): RingkasanStok = RingkasanStok(
    kritis = daftar.count { it.status == StatusStok.KRITIS },
    menipis = daftar.count { it.status == StatusStok.MENIPIS },
    total = daftar.size,
)

/**
 * Urutan tampil: yang paling genting lebih dulu, lalu abjad.
 *
 * Cermin pengurutan halaman web (`rank` kritis/menipis/aman, lalu
 * `localeCompare`). Perbandingan namanya memakai `compareTo` peka-huruf-besar
 * yang sama dengan sisa modul ini supaya urutannya tidak berubah antar perangkat
 * yang locale-nya berbeda.
 */
fun urutkanStok(daftar: List<BahanCabang>): List<BahanCabang> =
    daftar.sortedWith(compareBy({ it.status.urutan }, { it.nama.lowercase() }))

/** Penyaring kotak pencarian: cocok bila nama bahan mengandung [kunci], tanpa peduli huruf besar. */
fun saringStok(daftar: List<BahanCabang>, kunci: String): List<BahanCabang> {
    val q = kunci.trim()
    if (q.isEmpty()) return daftar
    return daftar.filter { it.nama.contains(q, ignoreCase = true) }
}
