package com.sukashawarma.superapp.feature.stok.domain

import com.google.gson.JsonParser

/**
 * Isi kolom `opname_item.catatan` setelah dipisahkan menurut modenya.
 *
 * @property targetKitchen target produksi yang dititipkan Kitchen, bila ada.
 * @property catatanBebas catatan yang benar-benar diketik orang, bila ada.
 */
data class CatatanOpname(
    val targetKitchen: String? = null,
    val catatanBebas: String? = null,
)

private const val PREFIKS_RAW = "[RAW]"

/**
 * Membaca `opname_item.catatan` yang dipakai untuk dua hal sekaligus — port dari
 * `components/stok/OpnameDetail.tsx`.
 *
 * Kolom ini bermode ganda. Bila diawali `[RAW]`, sisanya adalah JSON titipan mesin
 * yang field `t`-nya berisi target Kitchen; bila tidak, seluruh isinya catatan yang
 * diketik orang.
 *
 * Perhatikan satu asimetri yang mudah "dirapikan" keliru: yang menentukan sebuah
 * catatan disembunyikan sebagai teks bebas adalah **prefiksnya**, bukan berhasil
 * atau tidaknya JSON diurai. Baris `[RAW]` dengan JSON rusak tetap bukan catatan
 * orang — ia titipan mesin yang gagal terbaca, dan menampilkannya mentah-mentah
 * hanya memunculkan tanda kurung kurawal di layar. Web berperilaku begitu, dan
 * perilakunya ditiru apa adanya.
 */
fun parseCatatanOpname(catatan: String?): CatatanOpname {
    val isi = catatan?.trim()
    if (isi.isNullOrBlank()) return CatatanOpname()
    if (!isi.startsWith(PREFIKS_RAW)) return CatatanOpname(catatanBebas = isi)

    val muatan = isi.removePrefix(PREFIKS_RAW).trimStart()
    val target = runCatching {
        JsonParser.parseString(muatan).asJsonObject.get("t")?.asString
    }.getOrNull()

    return CatatanOpname(targetKitchen = target?.takeIf { it.isNotBlank() })
}
