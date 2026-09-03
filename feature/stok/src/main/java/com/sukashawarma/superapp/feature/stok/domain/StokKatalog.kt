package com.sukashawarma.superapp.feature.stok.domain

/**
 * Aturan katalog bahan yang disalin dari aplikasi web supaya isi daftar di HP sama
 * dengan yang dilihat orang di browser.
 */

/** Kategori yang ditampilkan beserta urutannya — cermin `KATEGORI_ORDER` di `CrewList.tsx`. */
enum class KategoriStok(val kunci: String, val label: String, val emoji: String) {
    FOOD_BEVERAGE("FOOD & BEVERAGE", "Food & Beverage", "🥩"),
    BUMBU("BUMBU", "Bumbu", "🌶️"),
    PACKAGING("PACKAGING", "Packaging", "📦"),
    OPERASIONAL("OPERASIONAL", "Operasional", "📋");

    companion object {
        /** Kategori di luar keempat ini jatuh ke Operasional, sama seperti web. */
        fun dari(kategori: String?): KategoriStok {
            val upper = kategori?.uppercase().orEmpty()
            return entries.firstOrNull { it.kunci == upper } ?: OPERASIONAL
        }
    }
}

/** Asal bahan — cermin `getBahanBakuSource` di `packages/design-system`. */
enum class SumberBahan { KITCHEN, OUTLET, GUDANG_PUSAT, UNKNOWN }

private val BAHAN_OUTLET = setOf("MIE", "LETTUCE", "ES BATU")

private val BAHAN_GUDANG_PUSAT = setOf(
    "GARAM", "JINTEN", "KAYU MANIS", "KETUMBAR", "KUNYIT", "SASA", "CENGKEH",
)

private val BAHAN_KITCHEN = setOf(
    "SAOS CABE", "SAOS TOMAT", "SAOS SAMYANG", "MAYONES",
    "KULIT 25", "KULIT 28", "KULIT 32",
    "AYAM", "SAPI", "KENTANG", "KEJU", "TUM", "BAWANG", "TEPUNG",
    "MINYAK SAYUR", "FOIL", "SARUNG TANGAN BENING", "KERTAS STRUK",
    "PLASTIK BESAR", "PLASTIK KECIL", "PLASTIK VACUM", "PLASTIK MERAH",
    "POLYBAG", "PAPER WRAP", "POWDER TEH", "POWDER JERUK", "POWDER MIX",
    "CUP + TUTUP", "SEDOTAN", "STIKER",
)

fun sumberBahan(nama: String): SumberBahan {
    val upper = nama.uppercase()
    return when {
        upper in BAHAN_OUTLET -> SumberBahan.OUTLET
        upper in BAHAN_GUDANG_PUSAT -> SumberBahan.GUDANG_PUSAT
        upper in BAHAN_KITCHEN -> SumberBahan.KITCHEN
        else -> SumberBahan.UNKNOWN
    }
}

/**
 * Bahan milik gudang pusat disembunyikan dari outlet biasa — cermin penyaringan di
 * `CrewList.tsx`. Outlet yang namanya mengandung GUDANG tetap melihatnya.
 */
fun bolehTampilDiOutlet(namaBahan: String, namaOutlet: String): Boolean {
    if (sumberBahan(namaBahan) != SumberBahan.GUDANG_PUSAT) return true
    return namaOutlet.uppercase().contains("GUDANG")
}

/** Lokasi penyimpanan tebakan berdasarkan nama/kategori — cermin `getStorageLocation` web. */
fun lokasiPenyimpanan(kategori: String?, nama: String): String {
    val n = nama.lowercase()
    val k = kategori?.lowercase().orEmpty()
    return when {
        k == "item core" || n.contains("daging") || n.contains("ayam") -> "Frozen Storage"
        k == "minuman" || n.contains("garlic") -> "Chilled Storage"
        n.contains("lpg") || n.contains("gas") -> "Utility Area"
        else -> "Dry Storage"
    }
}
