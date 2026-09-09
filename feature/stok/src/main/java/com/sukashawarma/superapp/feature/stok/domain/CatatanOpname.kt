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
    /** Masukan tiga jenjang apa adanya, untuk melanjutkan draft. */
    val masukan: MasukanBerjenjang? = null,
)

/**
 * Angka yang benar-benar DIKETIK kru pada tiga kolom satuan, disimpan sebagai teks.
 *
 * Teks, bukan angka, dan itu penting: "0" yang diketik berbeda dari kolom yang
 * dibiarkan kosong, dan mengubahnya jadi Double lalu kembali akan menghapus
 * perbedaan itu sekaligus menggeser nilai karena pembulatan.
 */
data class MasukanBerjenjang(
    val besar: String = "",
    val tengah: String = "",
    val kecil: String = "",
) {
    val kosong: Boolean get() = besar.isBlank() && tengah.isBlank() && kecil.isBlank()
}

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
    val obj = runCatching { JsonParser.parseString(muatan).asJsonObject }.getOrNull()
        ?: return CatatanOpname()

    val target = runCatching { obj.get("t")?.asString }.getOrNull()
    val masukan = runCatching {
        obj.getAsJsonObject("raw")?.let { raw ->
            fun teks(nama: String) = runCatching { raw.get(nama)?.asString }.getOrNull().orEmpty()
            MasukanBerjenjang(teks("besar"), teks("tengah"), teks("kecil"))
        }?.takeUnless { it.kosong }
    }.getOrNull()

    return CatatanOpname(
        targetKitchen = target?.takeIf { it.isNotBlank() },
        masukan = masukan,
    )
}

/**
 * Menyusun isi `opname_item.catatan` — format yang sama dengan `buildItemsToSave`
 * di `components/stok/OpnameForm.tsx` web.
 *
 * Empat field ditulis, dan tiga di antaranya BUKAN untuk native:
 * - `raw` dipakai native maupun web untuk melanjutkan draft persis seperti yang
 *   diketik. Inilah yang dulu tidak pernah disimpan native, sehingga draft yang
 *   dilanjutkan hanya memunculkan satu angka besar di kolom satuan terkecil.
 * - `f`, `s`, `d` adalah teks siap tampil yang dibaca `OpnameDetailModal` di
 *   admin-dashboard. Native tidak membacanya, tetapi tetap menulisnya supaya opname
 *   yang dibuat dari HP tidak tampil kosong di dashboard kantor.
 *
 * Field `t`/`traw` milik target Kitchen sengaja tidak ditulis: native tidak punya
 * layar target produksi, dan menulis kunci kosong hanya membuat pembacanya menebak.
 */
fun bungkusCatatanOpname(
    fisikTeks: String,
    sistemTeks: String,
    selisihTeks: String,
    masukan: MasukanBerjenjang,
): String {
    val raw = com.google.gson.JsonObject().apply {
        addProperty("besar", masukan.besar)
        addProperty("tengah", masukan.tengah)
        addProperty("kecil", masukan.kecil)
    }
    val muatan = com.google.gson.JsonObject().apply {
        addProperty("f", fisikTeks)
        addProperty("s", sistemTeks)
        addProperty("d", selisihTeks)
        add("raw", raw)
    }
    return "$PREFIKS_RAW $muatan"
}
