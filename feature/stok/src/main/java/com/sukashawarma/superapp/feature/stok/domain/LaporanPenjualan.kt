package com.sukashawarma.superapp.feature.stok.domain

/** Satu pesanan yang sudah diringkas dari `orders`. */
data class PesananRingkas(
    val id: String,
    val status: String,
    val channel: String?,
    val outletId: String?,
    val total: Double,
    val diskon: Double,
    val subsidiPromo: Double,
    val jamWib: Int,
    val items: List<ItemPesanan>,
)

data class ItemPesanan(val nama: String, val qty: Double, val subtotal: Double)

/** Kanal penjualan setelah dinormalkan — cermin `channelStats` web. */
enum class KanalJual(val kunci: String, val label: String) {
    OFFLINE("offline", "Offline / Kasir"),
    GOFOOD("gofood", "GoFood"),
    GRABFOOD("grabfood", "GrabFood"),
    SHOPEEFOOD("shopeefood", "ShopeeFood"),
    TIKTOK("tiktok", "TikTok Shop"),
    LAINNYA("lainnya", "Lainnya");

    companion object {
        /**
         * Nilai `channel` di database tidak seragam (`tiktokgo`, `tiktok_go`,
         * `TIKTOK`), jadi pencocokannya memakai "mengandung", bukan sama dengan —
         * persis seperti web.
         */
        fun dari(mentah: String?): KanalJual {
            val ch = mentah?.lowercase()?.trim().orEmpty()
            return when {
                ch.isEmpty() || ch == "offline" || ch == "pos" -> OFFLINE
                ch.contains("gofood") -> GOFOOD
                ch.contains("grab") -> GRABFOOD
                ch.contains("shopee") -> SHOPEEFOOD
                ch.contains("tiktok") -> TIKTOK
                else -> LAINNYA
            }
        }
    }
}

data class BarisKanal(val kanal: KanalJual, val jumlahOrder: Int, val omzet: Double)
data class BarisOutletJual(val outletId: String, val nama: String, val omzet: Double, val jumlahOrder: Int, val porsi: Double)
data class BarisMenuLaris(val nama: String, val qty: Double, val omzet: Double)

data class RingkasPenjualan(
    val omzetBersih: Double = 0.0,
    val potongan: Double = 0.0,
    val jumlahOrder: Int = 0,
    val jumlahBatal: Int = 0,
    val perKanal: List<BarisKanal> = emptyList(),
    val perOutlet: List<BarisOutletJual> = emptyList(),
    val menuTerlaris: List<BarisMenuLaris> = emptyList(),
    /** Omzet per jam WIB, indeks 0..23. */
    val perJam: List<Double> = List(24) { 0.0 },
) {
    val omzetKotor: Double get() = omzetBersih + potongan
    val rataRataOrder: Double get() = if (jumlahOrder > 0) omzetBersih / jumlahOrder else 0.0
    val jamTersibuk: Int? get() = perJam.withIndex().maxByOrNull { it.value }?.takeIf { it.value > 0 }?.index
}

/**
 * Outlet yang bukan gerai penjualan sungguhan — daftar kata kunci ini disalin
 * dari `isIgnoredOutletName` di web.
 *
 * Tanpa penyaring ini, gudang pusat dan outlet uji coba ikut menaikkan omzet dan
 * membuat angka laporannya tidak bisa dipakai.
 */
private val KATA_OUTLET_DIABAIKAN = listOf(
    "GUDANG PUSAT", "GEDUNG PUSAT", "KANTOR PUSAT", "OUTLET TES", "OUTLET TEST",
    "SHOOPE", "SHOPEE", "TIKTOK", "GLOBAL OUTLET", "GLOBAL SYSTEM",
)

fun outletDiabaikan(nama: String?): Boolean {
    if (nama.isNullOrBlank()) return true
    val n = nama.uppercase()
    return KATA_OUTLET_DIABAIKAN.any { n.contains(it) }
}

/** Nama outlet tanpa awalan merek, supaya muat di layar sempit. */
fun namaOutletRingkas(nama: String): String =
    nama.replace("SUKA SHAWARMA ", "", ignoreCase = true).trim()

/**
 * Meringkas pesanan menjadi angka laporan — port agregasi di
 * `app/stok/laporan-penjualan/page.tsx`.
 *
 * Hanya pesanan `completed` yang dihitung sebagai omzet; yang `cancelled`
 * dihitung terpisah sebagai jumlah, bukan nilai.
 */
fun ringkasPenjualan(
    pesanan: List<PesananRingkas>,
    namaOutlet: Map<String, String>,
): RingkasPenjualan {
    val selesai = pesanan.filter { it.status == "completed" }
    val batal = pesanan.count { it.status == "cancelled" }
    if (selesai.isEmpty()) return RingkasPenjualan(jumlahBatal = batal)

    val perKanal = LinkedHashMap<KanalJual, Pair<Int, Double>>()
    val perOutlet = LinkedHashMap<String, Triple<Double, Int, Double>>()
    val perMenu = LinkedHashMap<String, Pair<Double, Double>>()
    val perJam = DoubleArray(24)

    selesai.forEach { order ->
        val kanal = KanalJual.dari(order.channel)
        val (n, omzet) = perKanal[kanal] ?: (0 to 0.0)
        perKanal[kanal] = (n + 1) to (omzet + order.total)

        val outletId = order.outletId ?: "tanpa-outlet"
        val porsi = order.items.sumOf { it.qty }
        val (o, jml, p) = perOutlet[outletId] ?: Triple(0.0, 0, 0.0)
        perOutlet[outletId] = Triple(o + order.total, jml + 1, p + porsi)

        order.items.forEach { item ->
            val (q, r) = perMenu[item.nama] ?: (0.0 to 0.0)
            perMenu[item.nama] = (q + item.qty) to (r + item.subtotal)
        }

        perJam[order.jamWib.coerceIn(0, 23)] += order.total
    }

    return RingkasPenjualan(
        omzetBersih = selesai.sumOf { it.total },
        potongan = selesai.sumOf { it.diskon + it.subsidiPromo },
        jumlahOrder = selesai.size,
        jumlahBatal = batal,
        perKanal = perKanal.map { (kanal, isi) -> BarisKanal(kanal, isi.first, isi.second) }
            .sortedByDescending { it.omzet },
        perOutlet = perOutlet.map { (id, isi) ->
            BarisOutletJual(
                outletId = id,
                nama = namaOutlet[id]?.let(::namaOutletRingkas) ?: "Tanpa outlet",
                omzet = isi.first,
                jumlahOrder = isi.second,
                porsi = isi.third,
            )
        }.sortedByDescending { it.omzet },
        menuTerlaris = perMenu.map { (nama, isi) -> BarisMenuLaris(nama, isi.first, isi.second) }
            .sortedByDescending { it.qty }
            .take(15),
        perJam = perJam.toList(),
    )
}
