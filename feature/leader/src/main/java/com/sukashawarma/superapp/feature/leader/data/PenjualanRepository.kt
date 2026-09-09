package com.sukashawarma.superapp.feature.leader.data

import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optInt
import com.sukashawarma.superapp.data.remote.optJsonArray
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.feature.leader.domain.ItemPesanan
import com.sukashawarma.superapp.feature.leader.domain.PenjualanHariIni
import com.sukashawarma.superapp.feature.leader.domain.PesananTerbaru
import com.sukashawarma.superapp.feature.leader.domain.StatusPesanan
import com.sukashawarma.superapp.feature.leader.domain.akhirHariIso
import com.sukashawarma.superapp.feature.leader.domain.awalHariIso
import com.sukashawarma.superapp.feature.leader.domain.hariIniJakarta
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Pembacaan layar Penjualan & Target satu cabang.
 */
object PenjualanRepository {

    /** Web menampilkan sepuluh transaksi terakhir; disamakan supaya isinya sama. */
    private const val BATAS_TERBARU = 10

    suspend fun muat(outletId: String): PenjualanHariIni = coroutineScope {
        val hariIni = hariIniJakarta()
        val dari = awalHariIso(hariIni)
        val sampai = akhirHariIso(hariIni)

        val omzet = async { omzetHariIni(outletId, dari, sampai) }
        val terbaru = async { pesananTerbaru(outletId, dari, sampai) }
        val target = async { target(outletId, hariIni.toString()) }

        PenjualanHariIni(
            omzet = omzet.await(),
            target = target.await(),
            pesanan = terbaru.await(),
        )
    }

    private suspend fun omzetHariIni(outletId: String, dari: String, sampai: String): Long =
        Postgrest.select(
            "orders",
            listOf(
                "select" to "total_amount",
                "outlet_id" to "eq.$outletId",
                "status" to "eq.completed",
                "created_at" to "gte.$dari",
                "created_at" to "lte.$sampai",
            ),
        ).sumOf { it.asJsonObject.optDouble("total_amount")?.toLong() ?: 0L }

    /**
     * Sepuluh transaksi terakhir, SEMUA status.
     *
     * Pesanan yang masih diproses dan yang dibatalkan sengaja ikut: layar ini dipakai
     * leader untuk memantau antrean berjalan, bukan hanya menghitung uang masuk.
     *
     * Isi pesanan diambil dalam satu embed `order_items`, bukan query kedua seperti
     * web. Hasilnya sama, dengan satu perjalanan jaringan lebih sedikit.
     */
    private suspend fun pesananTerbaru(
        outletId: String,
        dari: String,
        sampai: String,
    ): List<PesananTerbaru> =
        Postgrest.select(
            "orders",
            listOf(
                "select" to "id,order_number,created_at,status,total_amount," +
                    "scheduled_promo_names,order_items(menu_item_name,quantity)",
                "outlet_id" to "eq.$outletId",
                "created_at" to "gte.$dari",
                "created_at" to "lte.$sampai",
                "order" to "created_at.desc",
                "limit" to BATAS_TERBARU.toString(),
            ),
        ).mapNotNull { elemen ->
            val baris = elemen.asJsonObject
            val id = baris.optString("id") ?: return@mapNotNull null
            PesananTerbaru(
                id = id,
                nomor = baris.optInt("order_number"),
                dibuatPada = baris.optString("created_at").orEmpty(),
                status = StatusPesanan.dari(baris.optString("status")),
                total = baris.optDouble("total_amount")?.toLong() ?: 0L,
                promo = baris.optJsonArray("scheduled_promo_names")
                    ?.mapNotNull { p -> p.takeIf { !it.isJsonNull }?.asString?.takeIf { it.isNotBlank() } }
                    .orEmpty(),
                item = baris.optJsonArray("order_items")?.mapNotNull { i ->
                    val item = i.asJsonObject
                    val nama = item.optString("menu_item_name")?.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null
                    ItemPesanan(nama, item.optInt("quantity") ?: 0)
                }.orEmpty(),
            )
        }

    /**
     * Target omzet hari ini dari snapshot `historical_daily_targets`.
     *
     * Web memaksudkan query yang sama tetapi menyaringnya dengan variabel `today`
     * yang tak pernah didefinisikan di komponen itu — jadi barisnya melempar
     * ReferenceError, tertangkap `catch`, dan targetnya diam-diam selalu nol. Di sini
     * tanggalnya diisi benar, sehingga bilah progres akhirnya menampilkan sesuatu.
     *
     * Nol tetap berarti "belum ditentukan", bukan "target nol": snapshot-nya ditulis
     * cron menjelang tengah malam, jadi hari berjalan memang sering belum punya baris.
     */
    private suspend fun target(outletId: String, tanggal: String): Long =
        Postgrest.selectOne(
            "historical_daily_targets",
            listOf(
                "select" to "target_amount",
                "outlet_id" to "eq.$outletId",
                "record_date" to "eq.$tanggal",
            ),
        )?.optDouble("target_amount")?.toLong() ?: 0L
}
