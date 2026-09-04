package com.sukashawarma.superapp.feature.distribusi.data.model

import com.sukashawarma.superapp.feature.distribusi.domain.BahanBakuMeta
import com.sukashawarma.superapp.feature.distribusi.domain.BarisRingkasan
import com.sukashawarma.superapp.feature.distribusi.domain.PenandaSelisih
import com.sukashawarma.superapp.feature.distribusi.domain.StatusSuratJalan

/** Satu tanda tangan di dalam `signatures` atau `receipt_signatures`.
 *  `gambar` adalah data URL PNG, bisa null pada baris lama. */
data class TandaTangan(
    val namaPenandaTangan: String,
    val peran: String,
    val waktu: String,
    val gambar: String?,
)

/**
 * Satu baris `surat_jalan_item`. `qtyDikirim` dan `qtyTerima` dalam SATUAN DASAR
 * — konversi ke satuan distribusi dilakukan lapisan UI lewat `SatuanDistribusi`.
 */
data class SuratJalanItem(
    val id: String,
    val bahanBakuId: String,
    override val qtyDikirim: Double,
    override val qtyTerima: Double?,
    override val kondisi: String?,
    val catatan: String?,
    val fotoPath: String?,
    val terverifikasiPada: String?,
    val bahan: BahanBakuMeta?,
) : PenandaSelisih

/** Proyeksi ringkas untuk daftar dan dashboard. */
data class SuratJalanRingkas(
    val id: String,
    val outletId: String,
    override val status: StatusSuratJalan?,
    override val namaOutlet: String?,
    val nomorDokumen: String?,
    val dibuatPada: String?,
    override val adaSelisih: Boolean,
) : BarisRingkasan

/** Dokumen lengkap beserta itemnya. */
data class SuratJalanDetail(
    val id: String,
    val outletId: String,
    val status: StatusSuratJalan?,
    val namaOutlet: String?,
    val nomorDokumen: String?,
    val kodeVerifikasi: String?,
    val dibuatPada: String?,
    val ttdPengirim: List<TandaTangan>,
    val ttdPenerimaan: List<TandaTangan>,
    val items: List<SuratJalanItem>,
)

/** Filter rentang tanggal dashboard — cermin `DateFilter` di `useSuratJalanList.ts`. */
enum class RentangTanggal(val label: String) {
    SEMUA("Semua"),
    HARI_INI("Hari Ini"),
    TUJUH_HARI("7 Hari"),
    TIGA_PULUH_HARI("30 Hari"),
}
