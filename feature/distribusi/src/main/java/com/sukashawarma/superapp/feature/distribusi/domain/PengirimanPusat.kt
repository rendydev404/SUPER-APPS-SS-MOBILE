package com.sukashawarma.superapp.feature.distribusi.domain

/**
 * Aturan sisi pengirim (Gudang Pusat / kitchen): kategori bahan di form,
 * peran tanda tangan pengirim, dan gerbang kirim. Cermin `SuratJalanForm.tsx`
 * dan `SignatureFlow.tsx` web.
 */
object PengirimanPusat {

    /** Tab kategori form buat surat jalan, urutan sama dengan `KATEGORI_TABS` web. */
    enum class Kategori(val kunci: String, val label: String) {
        SEMUA("all", "Semua"),
        ITEM_CORE("item core", "Item Core"),
        BUMBU("bumbu", "Bumbu"),
        MINUMAN("minuman", "Minuman"),
        KEMASAN("kemasan", "Kemasan"),
        LAINNYA("lainnya", "Lainnya"),
    }

    /** `normalizeKategori` web. */
    fun kategori(mentah: String?): Kategori = when (mentah.orEmpty().lowercase()) {
        "protein", "sayur", "item core" -> Kategori.ITEM_CORE
        "saus", "bumbu" -> Kategori.BUMBU
        "minuman" -> Kategori.MINUMAN
        "kemasan" -> Kategori.KEMASAN
        else -> Kategori.LAINNYA
    }

    /** Nilai `p_role` yang dikirim ke `sign_surat_jalan`. Web menampilkannya
     *  sebagai "Admin Gudang" tapi menyimpan "Admin Kitchen". */
    const val PERAN_ADMIN = "Admin Kitchen"
    const val PERAN_SUPIR = "Supir"

    /** Data lama menyimpan varian nama peran admin; ketiganya sah. */
    private val PERAN_ADMIN_SAH = setOf("Admin Kitchen", "Kitchen SPV", "Admin Gudang")

    fun sudahTtdAdmin(peran: List<String>): Boolean = peran.any { it in PERAN_ADMIN_SAH }
    fun sudahTtdSupir(peran: List<String>): Boolean = peran.any { it == PERAN_SUPIR }

    /** Label peran yang belum tanda tangan, dengan kata-kata pesan web. */
    fun ttdKurang(peran: List<String>): List<String> = buildList {
        if (!sudahTtdAdmin(peran)) add("Admin Gudang")
        if (!sudahTtdSupir(peran)) add("Supir (Kurir)")
    }

    /**
     * Alasan tombol Kirim ditahan, atau null bila boleh dikirim. Urutannya sama
     * dengan `handleSend`: vendor dulu, baru tanda tangan.
     */
    fun alasanTidakBisaKirim(vendorBelumDipilih: Int, peran: List<String>): String? {
        if (vendorBelumDipilih > 0) {
            return "Terdapat $vendorBelumDipilih bahan yang belum dipilih vendornya. " +
                "Tentukan vendornya terlebih dahulu."
        }
        val kurang = ttdKurang(peran)
        if (kurang.isNotEmpty()) return "Tanda tangan yang masih diperlukan: ${kurang.joinToString(", ")}"
        return null
    }

    /** Kode verifikasi hanya bermakna setelah dikirim; draft masih "terkunci". */
    fun kodeTerbuka(status: StatusSuratJalan?): Boolean =
        status != null && status != StatusSuratJalan.DRAFT && status != StatusSuratJalan.DIBATALKAN

    /** Pesan penjaga saldo dari DB memuat numeric seperti `12.000`; web merapikannya jadi `12`. */
    fun rapikanPesanKirim(pesan: String): String =
        Regex("""\d+\.\d+""").replace(pesan) { m ->
            m.value.toDoubleOrNull()?.let { AlokasiVendor.angka(it) } ?: m.value
        }
}
