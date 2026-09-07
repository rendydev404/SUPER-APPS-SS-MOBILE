package com.sukashawarma.superapp.feature.stok.data

import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optJsonArray
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.feature.stok.domain.ItemPesanan
import com.sukashawarma.superapp.feature.stok.domain.PesananRingkas
import com.sukashawarma.superapp.feature.stok.domain.outletDiabaikan
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Rentang tanggal laporan, dalam WIB. */
data class RentangLaporan(val dari: LocalDate, val sampai: LocalDate) {
    companion object {
        val ZONA: ZoneId = ZoneId.of("Asia/Jakarta")

        fun hariIni(): RentangLaporan {
            val kini = LocalDate.now(ZONA)
            return RentangLaporan(kini, kini)
        }

        fun tujuhHari(): RentangLaporan {
            val kini = LocalDate.now(ZONA)
            return RentangLaporan(kini.minusDays(6), kini)
        }

        fun bulanIni(): RentangLaporan {
            val kini = LocalDate.now(ZONA)
            return RentangLaporan(kini.withDayOfMonth(1), kini)
        }
    }
}

/** Data laporan penjualan sebelum diringkas. */
data class DataPenjualan(
    val pesanan: List<PesananRingkas>,
    val namaOutlet: Map<String, String>,
)

/**
 * Laporan penjualan — cermin `app/stok/laporan-penjualan/page.tsx`.
 *
 * Web membaca `orders` dengan service-role. Native memakai JWT pengguna, jadi
 * yang terbaca hanya outlet dalam cakupan orang itu — untuk role pusat cakupannya
 * seluruh outlet, dan merekalah yang diberi menu ini di web. Kalau RLS menolak,
 * kegagalannya muncul sebagai pesan, bukan angka nol yang menyesatkan.
 */
object LaporanPenjualanRepository {

    private const val KOLOM =
        "id,status,channel,total_amount,discount_amount,promo_subsidy,created_at,outlet_id," +
            "order_items(id,menu_item_name,quantity,subtotal)"

    suspend fun muat(rentang: RentangLaporan, outletId: String? = null): DataPenjualan {
        val outlets = Postgrest.select(
            "outlets",
            listOf("select" to "id,name", "is_active" to "eq.true", "order" to "name"),
        ).mapNotNull { elemen ->
            val baris = elemen.asJsonObject
            val id = baris.optString("id") ?: return@mapNotNull null
            id to baris.optString("name").orEmpty()
        }.toMap()

        // Batas hari memakai WIB, bukan UTC: satu hari laporan di Jakarta bergeser
        // tujuh jam dari batas UTC, dan pergeseran itu memindahkan omzet malam
        // ke tanggal berikutnya.
        val mulai = rentang.dari.atStartOfDay(RentangLaporan.ZONA).toInstant().toString()
        val selesai = rentang.sampai.plusDays(1).atStartOfDay(RentangLaporan.ZONA).toInstant().toString()

        val filter = buildList {
            add("select" to KOLOM)
            add("created_at" to "gte.$mulai")
            add("created_at" to "lt.$selesai")
            add("order" to "created_at.desc")
            if (outletId != null) add("outlet_id" to "eq.$outletId")
        }

        val outletDipakai = outlets.filterValues { !outletDiabaikan(it) }.keys

        val pesanan = allRows("orders", filter).mapNotNull { baris ->
            val id = baris.optString("id") ?: return@mapNotNull null
            val outlet = baris.optString("outlet_id")
            // Outlet uji coba dan gudang pusat dibuang hanya saat melihat semua
            // outlet; kalau satu outlet dipilih secara sadar, tampilkan apa adanya.
            if (outletId == null && outlet != null && outlet !in outletDipakai) return@mapNotNull null

            val waktu = baris.optString("created_at")
            val jam = waktu?.let {
                runCatching { Instant.parse(it).atZone(RentangLaporan.ZONA).hour }.getOrNull()
            } ?: 0

            PesananRingkas(
                id = id,
                status = baris.optString("status").orEmpty(),
                channel = baris.optString("channel"),
                outletId = outlet,
                total = baris.optDouble("total_amount") ?: 0.0,
                diskon = baris.optDouble("discount_amount") ?: 0.0,
                subsidiPromo = baris.optDouble("promo_subsidy") ?: 0.0,
                jamWib = jam,
                items = baris.optJsonArray("order_items").orEmpty().mapNotNull { elemen ->
                    val item = elemen.asJsonObject
                    ItemPesanan(
                        nama = item.optString("menu_item_name") ?: return@mapNotNull null,
                        qty = item.optDouble("quantity") ?: 0.0,
                        subtotal = item.optDouble("subtotal") ?: 0.0,
                    )
                },
            )
        }

        return DataPenjualan(pesanan, outlets)
    }

    private fun com.google.gson.JsonArray?.orEmpty(): List<com.google.gson.JsonElement> =
        this?.toList() ?: emptyList()
}
