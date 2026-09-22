package com.sukashawarma.superapp.feature.leader.domain

import com.sukashawarma.superapp.feature.stok.domain.StokStatus
import com.sukashawarma.superapp.feature.stok.domain.UnitMeta
import com.sukashawarma.superapp.feature.stok.domain.UnitScale

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
    val saldoTeks: String get() = saldoBerjenjang ?: "${kuantitas(saldo)} $satuan".trim()

    /** Null saat bahan belum punya titik pesan ulang — layar menyebutnya "belum diatur". */
    val batasTeks: String?
        get() = batasMinimal?.let { "${kuantitas(it)} $satuan".trim() }
}

/**
 * Bangun [BahanCabang] dari satu baris `monitoring_view_scoped`.
 *
 * `current_qty` berskala campuran: satuan terkecil bila [saldoIsGram], satuan besar
 * bila tidak. Membacanya mentah membuat 2.032 gram tepung tampil "2032 Kg" dan
 * statusnya ikut salah. Normalisasinya memakai [UnitScale] milik modul Stok, jadi
 * angka dan status di sini identik dengan layar Stok.
 *
 * Bila faktor satuan tidak dapat dipakai, saldo ditampilkan mentah dengan satuan
 * yang sesuai skala barisnya — bukan ditebak.
 */
fun bahanCabang(
    id: String,
    nama: String,
    currentQty: Double,
    saldoIsGram: Boolean,
    threshold: Double?,
    meta: UnitMeta,
): BahanCabang {
    val saldoNorm = UnitScale.normalizeSaldo(currentQty, saldoIsGram, meta)
    val thresholdNorm = UnitScale.normalizeThreshold(threshold, meta)
    val berjenjang = saldoNorm?.let { UnitScale.formatBerjenjang(it, meta) }
        ?: if (saldoIsGram) "${kuantitas(currentQty)} ${meta.satuanKecil.orEmpty()}".trim() else null
    return BahanCabang(
        id = id,
        nama = nama,
        saldo = currentQty,
        // Satuan besar: dipakai batasTeks (threshold selalu di satuan besar) dan
        // fallback saldo baris non-gram.
        satuan = meta.satuan.orEmpty(),
        batasMinimal = threshold,
        status = StatusStok.dariSkala(UnitScale.status(saldoNorm, thresholdNorm)),
        saldoBerjenjang = berjenjang,
    )
}

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
