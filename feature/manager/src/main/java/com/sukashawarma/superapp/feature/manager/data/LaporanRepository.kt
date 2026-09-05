package com.sukashawarma.superapp.feature.manager.data

import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optInt
import com.sukashawarma.superapp.data.remote.optJsonArray
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.feature.manager.domain.FilterChannel
import com.sukashawarma.superapp.feature.manager.domain.FilterPembayaran
import com.sukashawarma.superapp.feature.manager.domain.ItemPesanan
import com.sukashawarma.superapp.feature.manager.domain.PesananLaporan
import com.sukashawarma.superapp.feature.manager.domain.RentangTanggal
import com.sukashawarma.superapp.feature.manager.domain.ZONA_JAKARTA
import com.sukashawarma.superapp.feature.manager.domain.akhirIso
import com.sukashawarma.superapp.feature.manager.domain.awalIso
import com.sukashawarma.superapp.feature.manager.domain.namaMenuPokok
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * Pembacaan pesanan untuk layar Laporan.
 *
 * Sama seperti repository lain di modul ini, cakupan outlet ditentukan RLS
 * (`orders_select_scoped` memakai `accessible_outlet_ids()`), bukan penyaringan di
 * klien. Penyaring outlet di layar adalah pilihan pengguna untuk mempersempit,
 * bukan pengaman.
 */
object LaporanRepository {

    private const val UKURAN_HALAMAN = 1000

    private const val KOLOM =
        "id,status,payment_method,channel,total_amount,discount_amount,promo_subsidy," +
            "created_at,outlet_id,order_items(menu_item_name,quantity,subtotal)"

    /**
     * Seluruh pesanan pada [rentang] yang cocok dengan penyaring.
     *
     * Pesanan batal dan tertunda ikut dibaca — kartu "Status Transaksi" perlu
     * menghitungnya, dan menyaring `status=completed` di server akan membuat
     * persentase suksesnya selalu 100%.
     */
    suspend fun pesanan(
        rentang: RentangTanggal,
        channel: FilterChannel,
        pembayaran: FilterPembayaran,
        outletId: String?,
    ): List<PesananLaporan> {
        val filter = buildList {
            add("select" to KOLOM)
            add("created_at" to "gte.${rentang.awalIso()}")
            add("created_at" to "lte.${rentang.akhirIso()}")
            if (pembayaran != FilterPembayaran.SEMUA) {
                add("payment_method" to "eq.${pembayaran.kunci}")
            }
            when {
                channel.kolomKosong -> add("channel" to "is.null")
                channel.nilaiDb.isNotEmpty() -> add("channel" to "in.(${channel.nilaiDb.joinToString(",")})")
            }
            if (!outletId.isNullOrBlank()) add("outlet_id" to "eq.$outletId")
        }

        val hasil = mutableListOf<PesananLaporan>()
        var offset = 0
        while (true) {
            val halaman = Postgrest.select(
                "orders",
                filter + listOf(
                    "limit" to UKURAN_HALAMAN.toString(),
                    "offset" to offset.toString(),
                ),
            )
            halaman.forEach { baris -> petakan(baris.asJsonObject)?.let(hasil::add) }
            if (halaman.size() < UKURAN_HALAMAN) break
            offset += UKURAN_HALAMAN
        }
        return hasil
    }

    private fun petakan(baris: com.google.gson.JsonObject): PesananLaporan? {
        val status = baris.optString("status") ?: return null
        val items = baris.optJsonArray("order_items")?.mapNotNull { elemen ->
            val item = elemen.asJsonObject
            ItemPesanan(
                nama = namaMenuPokok(item.optString("menu_item_name")),
                qty = item.optInt("quantity") ?: 0,
                subtotal = item.optDouble("subtotal")?.toLong() ?: 0L,
            )
        }.orEmpty()

        return PesananLaporan(
            status = status,
            metodeBayar = baris.optString("payment_method"),
            totalAmount = baris.optDouble("total_amount")?.toLong() ?: 0L,
            discountAmount = baris.optDouble("discount_amount")?.toLong() ?: 0L,
            promoSubsidy = baris.optDouble("promo_subsidy")?.toLong() ?: 0L,
            jamJakarta = baris.optString("created_at")?.let(::jamJakarta),
            items = items,
        )
    }

    /**
     * Jam Jakarta sebuah cap waktu server.
     *
     * Dihitung di sini, sekali per baris, supaya lapisan domain tidak perlu tahu
     * bentuk string tanggal maupun zona waktu — dan supaya sebaran per jam tidak
     * berubah mengikuti zona waktu perangkat yang sedang dipakai.
     */
    private fun jamJakarta(iso: String): Int? = try {
        OffsetDateTime.parse(iso).atZoneSameInstant(ZONA_JAKARTA).hour
    } catch (e: DateTimeParseException) {
        null
    }
}
