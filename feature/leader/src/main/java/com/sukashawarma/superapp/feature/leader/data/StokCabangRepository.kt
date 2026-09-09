package com.sukashawarma.superapp.feature.leader.data

import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.feature.leader.domain.BahanCabang
import com.sukashawarma.superapp.feature.leader.domain.StatusStok
import com.sukashawarma.superapp.feature.leader.domain.urutkanStok

/**
 * Sisa bahan baku satu cabang.
 *
 * Sumbernya `monitoring_view_scoped` — view yang sama dipakai modul Stok native,
 * dan namanya berakhiran `_scoped` karena ia sudah menyaring diri ke
 * `accessible_outlet_ids()`. Penyaring `outlet_id` di bawah hanyalah pilihan
 * pengguna pada pemilih cabang, bukan kendali akses.
 *
 * Halaman web membaca `inventory_batches`/`inventory_items`/`inventory_units`.
 * Ketiganya TIDAK ADA di database ini — migrasi `20260709000001_merge_fifo_po`
 * membatalkan rencana FIFO dan malah menghapus `inventory_items`, dengan catatan
 * eksplisit bahwa sisanya tidak pernah dibuat. Karena itu layar stok leader di web
 * selalu berbunyi "Tidak Ada Data". Native membacanya dari tempat stok yang
 * sungguhan tersimpan; itu divergensi yang disengaja, bukan kelalaian menyalin.
 */
object StokCabangRepository {

    /** Satu outlet berisi puluhan bahan; satu halaman selebar ini sudah memuat semuanya. */
    private const val BATAS = 500

    suspend fun bahan(outletId: String): List<BahanCabang> {
        val baris = Postgrest.select(
            "monitoring_view_scoped",
            listOf(
                "select" to "bahan_baku_id,item_name,current_qty,threshold,status,satuan",
                "outlet_id" to "eq.$outletId",
                "order" to "item_name.asc",
                "limit" to BATAS.toString(),
            ),
        ).mapNotNull { elemen ->
            val b = elemen.asJsonObject
            val id = b.optString("bahan_baku_id") ?: return@mapNotNull null
            BahanCabang(
                id = id,
                nama = b.optString("item_name")?.takeIf { it.isNotBlank() } ?: "(tanpa nama)",
                saldo = b.optDouble("current_qty") ?: 0.0,
                satuan = b.optString("satuan").orEmpty(),
                batasMinimal = b.optDouble("threshold"),
                status = StatusStok.dariView(b.optString("status")),
            )
        }
        return urutkanStok(baris)
    }
}
