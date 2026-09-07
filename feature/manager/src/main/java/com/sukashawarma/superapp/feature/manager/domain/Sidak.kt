package com.sukashawarma.superapp.feature.manager.domain

import com.sukashawarma.superapp.domain.model.Role

/** Outlet pada daftar scope sidak. */
data class OutletSidak(val id: String, val nama: String, val region: String?)

/** Satu aset pada laporan inventaris yang dikirim area manager. */
data class ItemSidak(
    val id: String,
    val nama: String,
    val section: String,
    val subsection: String,
    /** `quantity`, `range`, atau `presence` — menentukan bentuk target dan hasil. */
    val mode: String,
    val targetQty: Double?,
    val targetMin: Double?,
    val targetMax: Double?,
    val satuan: String?,
    val observedQty: Double?,
    val ada: Boolean?,
    val kondisi: String,
    val catatan: String?,
    val fotoPath: String,
) {
    /** Nilai yang seharusnya ada, dalam bentuk yang bisa langsung dibaca. */
    val targetTeks: String
        get() = when {
            mode == "range" -> "${angkaRapi(targetMin)}–${angkaRapi(targetMax)} ${satuan.orEmpty()}".trim()
            targetQty != null -> "${angkaRapi(targetQty)} ${satuan.orEmpty()}".trim()
            else -> "Ada / tidak ada"
        }

    /** Apa yang dilaporkan area manager di lapangan. */
    val hasilTeks: String
        get() = when {
            mode == "quantity" -> "${angkaRapi(observedQty ?: 0.0)} ${satuan.orEmpty()}".trim()
            ada == false -> "Tidak ada"
            ada == true -> "Ada"
            else -> LABEL_KONDISI[kondisi] ?: kondisi
        }
}

private fun angkaRapi(nilai: Double?): String {
    val n = nilai ?: return "0"
    return if (n % 1.0 == 0.0) n.toLong().toString() else n.toString()
}

/** Laporan inventaris satu outlet. */
data class LaporanSidak(
    val id: String,
    val outletId: String,
    val tanggal: String,
    val diperbaruiPada: String,
    val catatan: String?,
    val items: List<ItemSidak>,
)

/** Hasil sidak yang sudah tersimpan atas sebuah laporan. */
data class HasilSidak(
    val id: String,
    val laporanId: String,
    /** `draft` atau `final`. */
    val status: String,
    val catatan: String?,
    /** submission_item_id -> `ok` / `issue` / `not_checked`. */
    val penilaian: Map<String, String>,
) {
    val selesai: Boolean get() = status == "final"
}

val LABEL_SECTION = mapOf(
    "interior" to "Interior",
    "exterior" to "Exterior",
    "kamar_mandi" to "Kamar mandi",
    "utilitas" to "Utilitas",
)

val LABEL_KONDISI = mapOf(
    "baik" to "Baik",
    "perlu_perbaikan" to "Perlu perbaikan",
    "rusak" to "Rusak",
    "tidak_ada" to "Tidak ada",
)

fun labelSection(section: String): String = LABEL_SECTION[section] ?: section

/** Keadaan sebuah outlet di daftar scope. */
enum class KeadaanSidak(val label: String) {
    SUDAH_DISIDAK("Sudah disidak"),
    MENUNGGU_SIDAK("Menunggu sidak"),
    BELUM_ADA_LAPORAN("Belum ada laporan AM"),
}

fun keadaanOutlet(laporan: LaporanSidak?, hasil: HasilSidak?): KeadaanSidak = when {
    laporan == null -> KeadaanSidak.BELUM_ADA_LAPORAN
    hasil?.selesai == true -> KeadaanSidak.SUDAH_DISIDAK
    else -> KeadaanSidak.MENUNGGU_SIDAK
}

/**
 * Ringkasan hasil sidak sebuah laporan.
 *
 * `diperiksa` hanya menghitung item yang sudah diberi keputusan; `not_checked`
 * sengaja tidak dihitung karena artinya memang belum diputuskan, bukan "baik".
 */
data class RingkasanSidak(val diperiksa: Int, val bermasalah: Int, val total: Int) {
    val persen: Int get() = if (total == 0) 0 else (diperiksa * 100) / total
}

fun ringkasSidak(laporan: LaporanSidak, hasil: HasilSidak?): RingkasanSidak {
    val penilaian = hasil?.penilaian.orEmpty()
    val keputusan = laporan.items.mapNotNull { penilaian[it.id] }.filter { it != "not_checked" }
    return RingkasanSidak(
        diperiksa = keputusan.size,
        bermasalah = keputusan.count { it == "issue" },
        total = laporan.items.size,
    )
}

/** Keputusan sidak untuk satu item. Nilainya persis kolom `status` di database. */
enum class KeputusanSidak(val nilai: String, val label: String) {
    BAIK("ok", "Baik"),
    RUSAK("issue", "Rusak");

    companion object {
        fun dari(nilai: String?): KeputusanSidak? = entries.find { it.nilai == nilai }
    }
}

/**
 * Role yang boleh menyimpan hasil sidak.
 *
 * Cermin gerbang di route web (`['regional_manager','area_manager']`) DAN di RPC
 * `submit_sidak_inventaris`. Yang menentukan tetap database; ini hanya supaya
 * tombolnya tidak menjanjikan sesuatu yang pasti ditolak.
 */
fun bolehMenyidak(role: Role?): Boolean =
    role == Role.REGIONAL_MANAGER || role == Role.AREA_MANAGER

/**
 * Keputusan awal saat layar dibuka: hasil sidak yang sudah tersimpan, kalau ada.
 *
 * `not_checked` sengaja tidak dipetakan — artinya memang belum diputuskan, dan
 * memperlakukannya sebagai "baik" akan menyetujui item yang belum dilihat siapa pun.
 */
fun keputusanAwal(hasil: HasilSidak?): Map<String, KeputusanSidak> =
    hasil?.penilaian.orEmpty().mapNotNull { (itemId, nilai) ->
        KeputusanSidak.dari(nilai)?.let { itemId to it }
    }.toMap()

/**
 * Alasan hasil sidak belum bisa disimpan, atau null bila sudah boleh.
 *
 * Aturannya sama dengan web: SETIAP item laporan harus diberi hasil. Sidak
 * separuh jalan tidak disimpan sebagai draft — RPC-nya pun menolak.
 */
fun halanganSimpanSidak(
    laporan: LaporanSidak?,
    keputusan: Map<String, KeputusanSidak>,
): String? {
    if (laporan == null || laporan.items.isEmpty()) return "Belum ada laporan inventaris untuk disidak."
    val belum = laporan.items.count { keputusan[it.id] == null }
    return if (belum > 0) "Masih ada $belum item yang belum diperiksa." else null
}

/** Jumlah item yang sudah diberi keputusan pada sesi ini. */
fun ringkasSidakBerjalan(
    laporan: LaporanSidak?,
    keputusan: Map<String, KeputusanSidak>,
): RingkasanSidak {
    val items = laporan?.items.orEmpty()
    val dinilai = items.mapNotNull { keputusan[it.id] }
    return RingkasanSidak(
        diperiksa = dinilai.size,
        bermasalah = dinilai.count { it == KeputusanSidak.RUSAK },
        total = items.size,
    )
}
