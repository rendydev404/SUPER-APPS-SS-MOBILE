package com.sukashawarma.superapp.feature.manager.domain

import com.sukashawarma.superapp.domain.model.Role

/**
 * Cara sebuah aset dinilai. Nilainya persis kolom `inventaris_master_items.mode`.
 *
 * `presence` tidak memakai angka sama sekali — asetnya cuma ada atau tidak ada.
 * Membedakan ini penting karena `submit_inventaris` menolak baris yang mengisi
 * `observed_qty` untuk item presence, dan sebaliknya.
 */
enum class ModeItem(val nilai: String) {
    JUMLAH("quantity"),
    RENTANG("range"),
    KEBERADAAN("presence");

    companion object {
        fun dari(nilai: String?): ModeItem = entries.find { it.nilai == nilai } ?: JUMLAH
    }
}

/** Kondisi fisik aset — cermin `CONDITIONS` di route submit web. */
enum class KondisiAset(val nilai: String, val label: String) {
    BAIK("baik", "Baik"),
    PERLU_PERBAIKAN("perlu_perbaikan", "Perlu perbaikan"),
    RUSAK("rusak", "Rusak"),
    TIDAK_ADA("tidak_ada", "Tidak ada");

    companion object {
        fun dari(nilai: String?): KondisiAset = entries.find { it.nilai == nilai } ?: BAIK
    }
}

/** Satu aset pada master checklist inventaris. */
data class ItemMaster(
    val id: String,
    val section: String,
    val subsection: String,
    val nama: String,
    val mode: ModeItem,
    val targetQty: Double?,
    val targetMin: Double?,
    val targetMax: Double?,
    val satuan: String?,
    val urutan: Int,
) {
    /** Cermin `targetLabel` web. */
    val targetTeks: String
        get() = when (mode) {
            ModeItem.KEBERADAAN -> "Wajib tersedia"
            ModeItem.RENTANG -> "Target ${angka(targetMin)}–${angka(targetMax)} ${satuan.orEmpty()}".trim()
            ModeItem.JUMLAH -> "Target min. ${angka(targetQty)} ${satuan.orEmpty()}".trim()
        }

    /**
     * Freezer wajib diberi catatan ukuran dan kondisinya.
     *
     * Aturan ini ada DI SERVER juga (`submit_inventaris` route web menolak freezer
     * tanpa catatan), jadi menyalinnya ke sini bukan pengaman ganda yang sia-sia —
     * tanpa itu pengguna baru tahu setelah mengisi 87 item dan menekan simpan.
     */
    val catatanWajib: Boolean get() = nama.lowercase().contains("freezer")
}

private fun angka(nilai: Double?): String {
    val n = nilai ?: return "0"
    return if (n % 1.0 == 0.0) n.toLong().toString() else n.toString()
}

/**
 * Isian satu item di layar, sebelum dikirim.
 *
 * Angka disimpan sebagai String, bukan Double, karena itulah yang sedang diketik
 * pengguna: "" berbeda dari 0, dan "1." adalah keadaan sah di tengah pengetikan.
 */
data class IsianItem(
    val jumlah: String = "",
    val ada: Boolean = true,
    val kondisi: KondisiAset = KondisiAset.BAIK,
    val catatan: String = "",
    val tanggalBeli: String = "",
    val harga: String = "",
    val depresiasi: String = "",
    val merek: String = "",
    /** Path foto di storage: baru diunggah sesi ini, atau bawaan dari laporan lama. */
    val fotoPath: String? = null,
    /** URL bertanda tangan untuk menampilkan foto yang sudah ada. */
    val fotoUrl: String? = null,
) {
    val adaFoto: Boolean get() = !fotoPath.isNullOrBlank()

    val jumlahAngka: Double? get() = jumlah.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
}

/**
 * Hasil penilaian otomatis sebuah item — cermin `evaluate` di route submit web
 * DAN `evaluation` di halaman formnya. Keduanya harus sepakat; kalau tidak, angka
 * yang tampil di layar berbeda dari yang tersimpan.
 */
fun nilaiPenilaian(item: ItemMaster, isian: IsianItem): String {
    if (item.mode == ModeItem.KEBERADAAN) return if (isian.ada) "sesuai" else "tidak_ada"
    val qty = isian.jumlahAngka ?: return "kurang"
    return when (item.mode) {
        ModeItem.RENTANG ->
            if (qty >= (item.targetMin ?: 0.0) && qty <= (item.targetMax ?: 0.0)) "sesuai" else "di_luar_target"
        else -> if (qty >= (item.targetQty ?: 0.0)) "sesuai" else "kurang"
    }
}

/** Apakah satu item sudah lengkap: angka sah (bila perlu) dan foto sudah ada. */
fun itemLengkap(item: ItemMaster, isian: IsianItem?): Boolean {
    if (isian == null) return false
    val angkaSah = item.mode == ModeItem.KEBERADAAN || isian.jumlahAngka != null
    return angkaSah && isian.adaFoto
}

/** Alasan satu item belum bisa dikirim, atau null bila sudah beres. */
fun halanganItem(item: ItemMaster, isian: IsianItem?): String? = when {
    isian == null -> "Belum diisi"
    item.mode != ModeItem.KEBERADAAN && isian.jumlahAngka == null -> "Jumlah belum diisi"
    item.catatanWajib && isian.catatan.isBlank() -> "Catatan freezer wajib diisi"
    !isian.adaFoto -> "Foto belum ada"
    else -> null
}

/** Item dikelompokkan per subsection, urut sesuai `sort_order` — bentuk langkah form. */
fun kelompokPerSubsection(items: List<ItemMaster>): List<Pair<String, List<ItemMaster>>> =
    items.sortedBy { it.urutan }
        .groupBy { it.subsection }
        .toList()

/** Kemajuan pengisian dalam persen, 0..100. */
fun kemajuanIsian(items: List<ItemMaster>, isian: Map<String, IsianItem>): Int {
    if (items.isEmpty()) return 0
    val lengkap = items.count { itemLengkap(it, isian[it.id]) }
    return lengkap * 100 / items.size
}

/**
 * Alasan seluruh laporan belum bisa dikirim, atau null bila sudah boleh.
 *
 * Menyebut item pertama yang bermasalah, bukan sekadar "belum lengkap": pada
 * daftar 87 baris, pesan tanpa nama item memaksa pengguna menyisir satu per satu.
 */
fun halanganKirim(items: List<ItemMaster>, isian: Map<String, IsianItem>): String? {
    if (items.isEmpty()) return "Master inventaris belum termuat."
    val bermasalah = items.firstNotNullOfOrNull { item ->
        halanganItem(item, isian[item.id])?.let { item to it }
    } ?: return null
    val (item, alasan) = bermasalah
    val sisa = items.count { halanganItem(it, isian[it.id]) != null }
    return if (sisa > 1) {
        "$alasan: ${item.nama} — dan ${sisa - 1} item lain."
    } else {
        "$alasan: ${item.nama}."
    }
}

/**
 * Role yang MENGISI inventaris, bukan sekadar melihat laporannya.
 *
 * Cermin `isReportViewer` di `apps/inventori/src/app/dashboard/page.tsx`: admin
 * dan regional manager dialihkan ke halaman laporan, sisanya mengisi form.
 */
fun mengisiInventaris(role: Role?): Boolean =
    role != null && role != Role.REGIONAL_MANAGER && role != Role.ADMIN && role != Role.OWNER

/** Kebalikannya: role yang membuka Inventori untuk membaca laporan. */
fun melihatLaporanInventaris(role: Role?): Boolean = !mengisiInventaris(role)
