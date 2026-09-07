package com.sukashawarma.superapp.feature.manager.domain

import kotlin.math.roundToLong

/**
 * Kelompok menu pada layar Resep & HPP. Cermin `CATEGORY_GROUPS` di
 * `app/resep/categoryHelper.ts` web, termasuk urutannya.
 */
enum class KelompokMenu(val id: String, val nama: String, val singkat: String, val urutan: Int, val ikon: String) {
    SAPI("sapi", "Original Shawarma Sapi", "Shawarma Sapi", 10, "🥩"),
    AYAM("ayam", "Original Shawarma Ayam", "Shawarma Ayam", 20, "🍗"),
    MIX("mix", "Shawarma Mix (Sapi + Ayam)", "Shawarma Mix", 30, "🌯"),
    SUKA_SUKA("suka_suka", "Suka Suka Series", "Suka Suka", 40, "🌶️"),
    SHAWARMIE("shawarmie", "Shawarmie (Shawarma Mie)", "Shawarmie", 50, "🍜"),
    COMBO("combo_reguler", "Combo Reguler (Kasir / Dine-In)", "Combo Reguler", 60, "📦"),
    TOPPING("topping", "Topping & Add-ons", "Topping", 70, "🍟"),
    MINUMAN("drink", "Minuman / Suka Drink", "Minuman", 80, "🥤"),
    LAINNYA("other", "Lainnya", "Lainnya", 85, "🍴"),
    TIKTOK("tiktok", "TikTok Series (TikTok Go)", "TikTok Series", 90, "📱"),
}

/**
 * Menentukan kelompok sebuah menu.
 *
 * Urutan pemeriksaannya SAMA PERSIS dengan `getMenuCategoryGroup` web dan tidak
 * boleh diringkas: aturan belakangan sengaja lebih longgar, jadi menukar urutan
 * akan memindahkan menu ke kelompok lain. Contohnya menu ber-channel `tiktokgo`
 * diperiksa paling dulu, sebelum kategori namanya sempat dilihat.
 */
fun kelompokMenu(
    nama: String,
    kategori: String?,
    paket: Boolean,
    channelOnline: List<String>,
): KelompokMenu {
    val kat = kategori.orEmpty().trim().lowercase()
    val n = nama.lowercase()

    val tiktokEksklusif = channelOnline.size == 1 &&
        (channelOnline[0] == "tiktokgo" || channelOnline[0] == "tiktok")
    val tiktokBestSeller = n.startsWith("best seller") && "tiktokgo" in channelOnline
    val tiktokCombo = (
        paket || "paket" in n || "triple" in n || "duo" in n || "megabite" in n
        ) && "tiktokgo" in channelOnline &&
        "gofood" !in channelOnline && "grabfood" !in channelOnline
    val tiktokSaja = "tiktokgo" in channelOnline &&
        "gofood" !in channelOnline && "grabfood" !in channelOnline && "pos_kasir" !in channelOnline

    return when {
        tiktokEksklusif || tiktokBestSeller || tiktokCombo || tiktokSaja -> KelompokMenu.TIKTOK
        "sapi" in kat && "shawarmie" !in kat && "mix" !in kat && "suka suka" !in kat -> KelompokMenu.SAPI
        "ayam" in kat && "shawarmie" !in kat && "mix" !in kat && "suka suka" !in kat -> KelompokMenu.AYAM
        "mix" in kat || "mix" in n -> KelompokMenu.MIX
        "suka suka" in kat || (n.startsWith("suka ") && !paket && "merdeka" !in n) -> KelompokMenu.SUKA_SUKA
        "shawarmie" in kat || n.startsWith("shawarmie") -> KelompokMenu.SHAWARMIE
        "combo" in kat || paket -> KelompokMenu.COMBO
        "topping" in kat || n.startsWith("extra ") -> KelompokMenu.TOPPING
        "drink" in kat || "minuman" in kat || "tea" in n || "juice" in n -> KelompokMenu.MINUMAN
        else -> KelompokMenu.LAINNYA
    }
}

/**
 * Harga acuan sebuah bahan: berapa rupiah per kemasan, dan berapa isi kemasannya.
 *
 * Dua sumber yang mungkin, dan urutannya penting — SKU aktif lebih dulu (yang
 * ditandai default, kalau tidak ada ambil yang pertama), baru `bahan_baku_harga`.
 * Ini menyalin `calcHppFromRecipe` web; membalik urutannya membuat bahan yang
 * punya SKU baru tetap dihitung dengan harga lama.
 */
data class HargaBahan(val hargaBeli: Double, val qtyIsi: Double) {
    val adaHarga: Boolean get() = hargaBeli > 0 && qtyIsi > 0

    /** Rupiah per satu satuan pemakaian, mis. per gram. */
    val perSatuan: Double get() = if (adaHarga) hargaBeli / qtyIsi else 0.0
}

/** Satu bahan dalam resep sebuah menu. */
data class BarisResep(
    val bahanId: String,
    val bahanNama: String,
    val qtyPerPorsi: Double,
    val satuan: String,
    val harga: HargaBahan,
) {
    val subtotal: Long get() = if (harga.adaHarga) (harga.perSatuan * qtyPerPorsi).roundToLong() else 0L
    val adaHarga: Boolean get() = harga.adaHarga
    val qtyTeks: String
        get() = if (qtyPerPorsi % 1.0 == 0.0) qtyPerPorsi.toLong().toString() else qtyPerPorsi.toString()
}

/** Resep satu menu beserta buffer biayanya. */
data class ResepMenu(
    val menuId: String,
    val aktif: Boolean,
    val buffer: Long,
    val baris: List<BarisResep>,
) {
    val totalBahan: Long get() = baris.sumOf { it.subtotal }
    val totalHpp: Long get() = totalBahan + maxOf(0L, buffer)
    val adaBahanTanpaHarga: Boolean get() = baris.any { !it.adaHarga }
}

/** Satu menu pada tabel HPP. */
data class MenuHpp(
    val id: String,
    val nama: String,
    val kelompok: KelompokMenu,
    val kategoriAsli: String?,
    val hargaJual: Long,
    /** HPP dari resep atau rakitan paket; null kalau resepnya belum ada. */
    val hppResep: Long?,
    /** Nilai yang ditulis manual di `menu_items.hpp_override`. */
    val hppOverride: Long?,
    val paket: Boolean,
    /** true kalau sebagian komponen paket belum punya HPP. */
    val parsial: Boolean,
    val tersedia: Boolean,
    val urutan: Int,
) {
    /** Override menang atas hitungan resep — itu yang dipakai POS. */
    val hpp: Long? get() = hppOverride ?: hppResep

    val marginRp: Long? get() = hpp?.let { hargaJual - it }

    val marginPersen: Double?
        get() = hpp?.let { if (hargaJual > 0) (hargaJual - it).toDouble() / hargaJual * 100 else null }

    val foodcostPersen: Double?
        get() = hpp?.let { if (hargaJual > 0) it.toDouble() / hargaJual * 100 else null }

    /** Foodcost sehat di bisnis ini di bawah 40%; di atas itu perlu ditengok. */
    val foodcostTinggi: Boolean get() = (foodcostPersen ?: 0.0) > 40.0
}

/**
 * HPP satu menu paket: jumlah HPP tiap komponen dikali banyaknya.
 *
 * @param komponen pasangan (menuId komponen, jumlah).
 * @param hppKomponen HPP tiap menu, sudah memperhitungkan override-nya.
 * @return null kalau paket tidak punya komponen sama sekali; selain itu totalnya,
 *   disertai penanda `parsial` bila ada komponen yang HPP-nya belum diketahui.
 */
fun hitungHppPaket(
    komponen: List<Pair<String, Int>>,
    hppKomponen: Map<String, Long>,
): Pair<Long, Boolean>? {
    if (komponen.isEmpty()) return null
    var total = 0L
    var lengkap = true
    komponen.forEach { (menuId, jumlah) ->
        val hpp = hppKomponen[menuId]
        if (hpp == null) lengkap = false else total += hpp * jumlah
    }
    if (!lengkap && total == 0L) return null
    return total to !lengkap
}

/** Angka ringkas untuk tab Analisis HPP. */
data class RingkasanHpp(
    val jumlahMenu: Int,
    val jumlahBerResep: Int,
    val rataRataFoodcost: Double?,
    val foodcostTertinggi: MenuHpp?,
    val foodcostTerendah: MenuHpp?,
    val perluDitengok: List<MenuHpp>,
) {
    companion object {
        val KOSONG = RingkasanHpp(0, 0, null, null, null, emptyList())
    }
}

fun ringkasHpp(daftar: List<MenuHpp>): RingkasanHpp {
    // Menu tanpa harga jual tidak punya foodcost yang berarti; memasukkannya
    // sebagai 0% akan menarik rata-rata ke bawah dan menyembunyikan masalah.
    val berFoodcost = daftar.mapNotNull { menu -> menu.foodcostPersen?.let { menu to it } }
    return RingkasanHpp(
        jumlahMenu = daftar.size,
        jumlahBerResep = daftar.count { it.hpp != null },
        rataRataFoodcost = berFoodcost.map { it.second }.average().takeIf { berFoodcost.isNotEmpty() },
        foodcostTertinggi = berFoodcost.maxByOrNull { it.second }?.first,
        foodcostTerendah = berFoodcost.minByOrNull { it.second }?.first,
        perluDitengok = berFoodcost.filter { it.second > 40.0 }
            .sortedByDescending { it.second }
            .map { it.first },
    )
}

/** Ukuran porsi untuk pengurutan dalam satu kelompok — cermin `getSizeRank` web. */
fun peringkatUkuran(nama: String): Int {
    val n = nama.lowercase()
    return when {
        "sedang" in n -> 1
        "besar" in n -> 2
        "jumbo" in n -> 3
        "reguler" in n -> 4
        else -> 10
    }
}

/** Urutan tampil: kelompok, lalu ukuran porsi, lalu urutan menu, lalu nama. */
val URUTAN_MENU_HPP = compareBy<MenuHpp> { it.kelompok.urutan }
    .thenBy { peringkatUkuran(it.nama) }
    .thenBy { it.urutan }
    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.nama }
