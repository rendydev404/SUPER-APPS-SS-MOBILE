package com.sukashawarma.superapp.feature.stok.domain

import kotlin.math.roundToLong

/** Satu bahan pada susunan HPP sebuah menu. */
data class BahanHpp(
    val bahanBakuId: String,
    val nama: String,
    val satuanResep: String,
    val qtyPerPorsi: Double,
    val hargaBeliMaster: Double,
    val faktorKonversi: Double,
    val biayaPerSatuanResep: Double,
    val subtotal: Double,
    val kontribusiPersen: Double,
)

/** Sehat-tidaknya food cost sebuah menu — ambangnya sama dengan web. */
enum class StatusFoodCost(val label: String) {
    OPTIMAL("Optimal"),
    WASPADA("Waspada"),
    KRITIS("Kritis"),
}

data class MenuHpp(
    val resepId: String,
    val menuNama: String,
    val kategori: String,
    val hargaJual: Double,
    val totalHpp: Double,
    val bahan: List<BahanHpp>,
) {
    val marginRupiah: Double get() = hargaJual - totalHpp
    val marginPersen: Double get() = if (hargaJual > 0) marginRupiah / hargaJual * 100.0 else 0.0
    val foodCostPersen: Double get() = if (hargaJual > 0) totalHpp / hargaJual * 100.0 else 0.0

    /**
     * Ambang 35% dan 45% disalin apa adanya dari `hppMenu.ts`.
     *
     * Menu tanpa harga jual TIDAK otomatis optimal: food cost-nya nol karena
     * pembaginya nol, bukan karena murah. Web menandainya optimal — di sini
     * dibedakan supaya menu yang belum berharga tidak menyamar sebagai sehat.
     */
    val status: StatusFoodCost
        get() = when {
            hargaJual <= 0.0 -> StatusFoodCost.WASPADA
            foodCostPersen > 45.0 -> StatusFoodCost.KRITIS
            foodCostPersen >= 35.0 -> StatusFoodCost.WASPADA
            else -> StatusFoodCost.OPTIMAL
        }
}

/**
 * Menyusun HPP satu resep — port perhitungan di `app/actions/hppMenu.ts`.
 *
 * Biaya per satuan resep = harga beli master dibagi isi kemasan. Faktor
 * pembaginya memakai `kemasan_qty` bila ada, kalau tidak jatuh ke
 * `faktor_tampilan` lalu `faktor_konversi` — urutan yang sama dengan web,
 * dan urutannya penting: kemasan pembelian bisa berbeda dari faktor tampilan.
 */
fun susunHpp(
    resepId: String,
    menuNama: String,
    kategori: String,
    hargaJual: Double,
    bahanMentah: List<BahanMentahHpp>,
): MenuHpp {
    val terhitung = bahanMentah.map { mentah ->
        val faktor = mentah.kemasanQty?.takeIf { it > 0.0 }
            ?: mentah.faktorTampilan?.takeIf { it > 0.0 }
            ?: mentah.faktorKonversi?.takeIf { it > 0.0 }
            ?: 1.0
        val biayaSatuan = mentah.hargaBeli / faktor
        val subtotal = mentah.qtyPerPorsi * biayaSatuan
        BahanHpp(
            bahanBakuId = mentah.bahanBakuId,
            nama = mentah.nama,
            satuanResep = mentah.satuanResep,
            qtyPerPorsi = mentah.qtyPerPorsi,
            hargaBeliMaster = mentah.hargaBeli,
            faktorKonversi = faktor,
            biayaPerSatuanResep = (biayaSatuan * 100).roundToLong() / 100.0,
            subtotal = subtotal.roundToLong().toDouble(),
            kontribusiPersen = 0.0,
        )
    }

    val total = terhitung.sumOf { it.subtotal }
    val denganKontribusi = if (total > 0) {
        terhitung
            .map { it.copy(kontribusiPersen = (it.subtotal / total * 1000).roundToLong() / 10.0) }
            // Bahan termahal di atas: itu yang harus dilihat lebih dulu saat
            // menekan HPP.
            .sortedByDescending { it.subtotal }
    } else {
        terhitung
    }

    return MenuHpp(resepId, menuNama, kategori, hargaJual, total, denganKontribusi)
}

/** Bentuk mentah satu bahan resep sebelum biayanya dihitung. */
data class BahanMentahHpp(
    val bahanBakuId: String,
    val nama: String,
    val satuanResep: String,
    val qtyPerPorsi: Double,
    val hargaBeli: Double,
    val kemasanQty: Double?,
    val faktorTampilan: Double?,
    val faktorKonversi: Double?,
)
