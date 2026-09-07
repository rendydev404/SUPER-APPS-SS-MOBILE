package com.sukashawarma.superapp.feature.manager.domain

import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Rentang waktu layar Laporan. Cermin `RANGE_LABELS` di `ReportsClient.tsx`,
 * termasuk "Semua Waktu" yang tidak ada di penyaring layar Overview.
 */
enum class PresetLaporan(val kunci: String, val label: String) {
    HARI_INI("today", "Hari Ini"),
    KEMARIN("yesterday", "Kemarin"),
    TUJUH_HARI("7days", "7 Hari Terakhir"),
    TIGA_PULUH_HARI("30days", "30 Hari Terakhir"),
    SEMUA_WAKTU("all", "Semua Waktu"),
    KUSTOM("custom", "Kustom Tanggal");

    /**
     * Rentang tanggal Jakarta untuk preset ini. KUSTOM tidak punya rentang bawaan —
     * pemanggil memakai tanggal pilihan pengguna, dan HARI_INI di sini hanya penjaga
     * supaya fungsi tetap total.
     */
    fun rentang(hariIni: LocalDate = LocalDate.now(ZONA_JAKARTA)): RentangTanggal = when (this) {
        HARI_INI, KUSTOM -> RentangTanggal(hariIni, hariIni)
        KEMARIN -> hariIni.minusDays(1).let { RentangTanggal(it, it) }
        TUJUH_HARI -> RentangTanggal(hariIni.minusDays(6), hariIni)
        TIGA_PULUH_HARI -> RentangTanggal(hariIni.minusDays(29), hariIni)
        // Web memakai epoch 0 sebagai batas bawah "semua waktu"; tanggal ini lebih
        // tua dari baris mana pun di database dan tidak melibatkan zona waktu negatif.
        SEMUA_WAKTU -> RentangTanggal(LocalDate.of(1970, 1, 1), hariIni)
    }
}

/**
 * Penyaring channel penjualan.
 *
 * `nilaiDb` sengaja berupa daftar, bukan satu string: satu pilihan di layar bisa
 * memetakan ke beberapa nilai kolom. "TikTok Go" adalah contohnya — datanya pernah
 * ditulis sebagai `tiktokgo`, `tiktok`, dan `tiktok_go` oleh versi POS yang berbeda,
 * dan menyaring satu ejaan saja diam-diam menyembunyikan pesanan.
 */
enum class FilterChannel(val kunci: String, val label: String, val nilaiDb: List<String>) {
    SEMUA("all", "Semua Channel", emptyList()),
    FOOD_APPS("food_apps", "Semua Food Apps", listOf("gofood", "grabfood", "shopeefood", "tiktokgo", "tiktok", "tiktok_go")),
    OFFLINE("offline", "POS Kasir (Walk-in)", emptyList()),
    GOFOOD("gofood", "GoFood", listOf("gofood")),
    GRABFOOD("grabfood", "GrabFood", listOf("grabfood")),
    SHOPEEFOOD("shopeefood", "ShopeeFood", listOf("shopeefood")),
    TIKTOKGO("tiktokgo", "TikTok Go", listOf("tiktokgo", "tiktok", "tiktok_go"));

    /** Pesanan walk-in tidak punya channel sama sekali, jadi disaring lewat NULL. */
    val kolomKosong: Boolean get() = this == OFFLINE
}

enum class FilterPembayaran(val kunci: String, val label: String) {
    SEMUA("all", "Semua Metode"),
    TUNAI("cash", "Tunai"),
    QRIS("qris", "QRIS"),
    KARTU("card", "Kartu");
}

/** Satu baris pesanan beserta itemnya, sebatas kolom yang dipakai laporan. */
data class PesananLaporan(
    val status: String,
    val metodeBayar: String?,
    val totalAmount: Long,
    val discountAmount: Long,
    val promoSubsidy: Long,
    /** Jam Jakarta 0..23 saat pesanan dibuat, sudah dihitung ketika baris dibaca. */
    val jamJakarta: Int?,
    val items: List<ItemPesanan>,
)

data class ItemPesanan(val nama: String, val qty: Int, val subtotal: Long)

/** Rincian satu metode bayar. */
data class RincianPembayaran(
    val metode: String,
    val label: String,
    val jumlah: Int,
    val omzet: Long,
)

/** Satu menu pada daftar item terjual. */
data class ItemTerjual(val nama: String, val qty: Int, val omzet: Long)

data class AnalitikLaporan(
    val omzetKotor: Long,
    val potonganMerchant: Long,
    val subsidiPlatform: Long,
    val omzetBersih: Long,
    val pesananSukses: Int,
    val pesananBatal: Int,
    val itemTerjual: Int,
    val rataRataPerOrder: Long,
    val rincianPembayaran: List<RincianPembayaran>,
    /** 24 angka, indeks = jam Jakarta. */
    val perJam: List<Int>,
    val jamTersibuk: Int?,
    val daftarItem: List<ItemTerjual>,
) {
    private val diproses: Int get() = pesananSukses + pesananBatal
    val persenSukses: Int
        get() = if (diproses > 0) (pesananSukses.toDouble() / diproses * 100).roundToInt() else 0
    val persenBatal: Int get() = if (diproses > 0) 100 - persenSukses else 0
    val qtyTertinggi: Int get() = daftarItem.firstOrNull()?.qty ?: 1

    companion object {
        val KOSONG = AnalitikLaporan(
            omzetKotor = 0, potonganMerchant = 0, subsidiPlatform = 0, omzetBersih = 0,
            pesananSukses = 0, pesananBatal = 0, itemTerjual = 0, rataRataPerOrder = 0,
            rincianPembayaran = emptyList(), perJam = List(24) { 0 }, jamTersibuk = null,
            daftarItem = emptyList(),
        )
    }
}

/** Label metode bayar seperti `PAYMENT_META` di web; nilai tak dikenal jadi "Lainnya". */
fun labelMetodeBayar(metode: String): String = when (metode) {
    "cash" -> "Tunai"
    "qris" -> "QRIS"
    "card" -> "Kartu"
    else -> "Lainnya"
}

/**
 * Nama menu untuk pengelompokan. POS menyimpan varian sebagai `Nama | Varian`;
 * laporan menggabungkannya ke menu induk, sama seperti web.
 */
fun namaMenuPokok(mentah: String?): String {
    val nama = mentah?.takeIf { it.isNotBlank() } ?: "Item"
    return if ('|' in nama) nama.substringBefore('|').trim() else nama
}

/**
 * Potongan yang benar-benar ditanggung outlet untuk satu pesanan.
 *
 * ACUAN TUNGGAL (migrasi web 20300128000000): Potongan = MAX(0, nilai item −
 * total_amount). Bertumpu pada nilai menu, bukan `discount_amount`/`promo_subsidy`,
 * karena arti `total_amount` sempat berubah (19 Agustus 2026) sehingga menjumlahkan
 * promo ke total_amount menghitungnya dua kali.
 *
 * Pesanan tanpa baris item tidak punya "nilai menu" untuk dibandingkan, jadi hanya
 * di sanalah kolom diskon dipakai sebagai gantinya.
 */
fun potonganPesanan(pesanan: PesananLaporan): Long =
    if (pesanan.items.isEmpty()) {
        pesanan.discountAmount + pesanan.promoSubsidy
    } else {
        maxOf(0L, pesanan.items.sumOf { it.subtotal } - pesanan.totalAmount)
    }

/**
 * Menyusun seluruh angka layar Laporan dari daftar pesanan satu periode.
 *
 * Hanya pesanan berstatus `completed` yang masuk hitungan uang; yang batal hanya
 * dihitung jumlahnya untuk kartu Status Transaksi.
 */
fun susunAnalitikLaporan(pesanan: List<PesananLaporan>): AnalitikLaporan {
    val selesai = pesanan.filter { it.status == "completed" }
    val batal = pesanan.count { it.status == "cancelled" }

    // `total_amount` tersimpan SUDAH net — checkout, walk-in, dan input manual
    // sama-sama mengurangi diskon sebelum menyimpan. Jangan dikurangi lagi di sini.
    val omzetBersih = selesai.sumOf { it.totalAmount }
    val potongan = selesai.sumOf { potonganPesanan(it) }
    val subsidi = selesai.sumOf { it.promoSubsidy }

    val perMetode = LinkedHashMap<String, RincianPembayaran>()
    val perJam = IntArray(24)
    val perMenu = LinkedHashMap<String, ItemTerjual>()

    selesai.forEach { p ->
        val metode = p.metodeBayar?.takeIf { it.isNotBlank() } ?: "unknown"
        val ada = perMetode[metode]
        perMetode[metode] = if (ada == null) {
            RincianPembayaran(metode, labelMetodeBayar(metode), 1, p.totalAmount)
        } else {
            ada.copy(jumlah = ada.jumlah + 1, omzet = ada.omzet + p.totalAmount)
        }

        p.jamJakarta?.let { if (it in 0..23) perJam[it]++ }

        p.items.forEach { item ->
            val nama = item.nama
            val sebelumnya = perMenu[nama]
            perMenu[nama] = if (sebelumnya == null) {
                ItemTerjual(nama, item.qty, item.subtotal)
            } else {
                sebelumnya.copy(qty = sebelumnya.qty + item.qty, omzet = sebelumnya.omzet + item.subtotal)
            }
        }
    }

    // Jam tersibuk: yang PERTAMA mencapai puncak, bukan yang terakhir — supaya dua
    // jam berjumlah sama tidak berganti-ganti tampil di antara dua pemuatan.
    var puncak = 0
    var jamTersibuk: Int? = null
    for (jam in 0..23) {
        if (perJam[jam] > puncak) {
            puncak = perJam[jam]
            jamTersibuk = jam
        }
    }

    return AnalitikLaporan(
        omzetKotor = omzetBersih + potongan,
        potonganMerchant = potongan,
        subsidiPlatform = subsidi,
        omzetBersih = omzetBersih,
        pesananSukses = selesai.size,
        pesananBatal = batal,
        itemTerjual = perMenu.values.sumOf { it.qty },
        rataRataPerOrder = if (selesai.isEmpty()) 0L else (omzetBersih.toDouble() / selesai.size).roundToLong(),
        rincianPembayaran = perMetode.values.sortedByDescending { it.omzet },
        perJam = perJam.toList(),
        jamTersibuk = jamTersibuk,
        daftarItem = perMenu.values.sortedByDescending { it.qty },
    )
}
