package com.sukashawarma.superapp.feature.manager.data

import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optInt
import com.sukashawarma.superapp.data.remote.optJsonArray
import com.sukashawarma.superapp.data.remote.optJsonObject
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.feature.manager.domain.ItemPesananVoid
import com.sukashawarma.superapp.feature.manager.domain.PengajuanBypass
import com.sukashawarma.superapp.feature.manager.domain.PengajuanVoid
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class DataPersetujuan(
    val void: List<PengajuanVoid>,
    val bypass: List<PengajuanBypass>,
)

/**
 * Antrean persetujuan: pembatalan transaksi dan bypass kuncian POS.
 *
 * Hanya bypass yang bisa diproses dari sini. `bypass_requests` punya policy UPDATE
 * yang discope `accessible_outlet_ids()`, sedangkan jalur void tidak punya policy
 * tulis sama sekali — lihat `VOID_BISA_DIPROSES_NATIVE` untuk rinciannya.
 */
object PersetujuanRepository {

    /** Web membatasi 100 baris terbaru pada kedua antrean; disamakan. */
    private const val BATAS = 100

    suspend fun muat(): DataPersetujuan = coroutineScope {
        val void = async { void() }
        val bypass = async { bypass() }
        DataPersetujuan(void.await(), bypass.await())
    }

    private suspend fun void(): List<PengajuanVoid> =
        Postgrest.select(
            "cancellation_requests",
            listOf(
                "select" to (
                    // `order_id` ikut dibaca sebagai kolom biasa: ia tetap terbaca
                    // meski pesanannya sendiri disembunyikan RLS, dan jadi satu-satunya
                    // penanda yang bisa ditunjukkan ke pengguna dalam keadaan itu.
                    "id,order_id,reason,created_at," +
                        "orders(order_number,customer_name,total_amount,outlets(name)," +
                        "order_items(menu_item_name,quantity,subtotal))," +
                        "outlet_staff!requested_by(name)"
                    ),
                "status" to "eq.pending",
                "order" to "created_at.desc",
                "limit" to BATAS.toString(),
            ),
        ).mapNotNull { petakanVoid(it.asJsonObject) }

    /**
     * Memetakan satu baris antrean void, atau null bila pengajuan itu BUKAN urusan
     * pengguna ini.
     *
     * `orders` datang null persis ketika outlet pesanannya di luar
     * `accessible_outlet_ids()` — untuk area manager itu sama dengan `staff_outlets`.
     * Jadi embed yang null bukan sekadar "data tidak lengkap", melainkan tanda bahwa
     * pengajuannya di luar cakupan, dan barisnya dibuang.
     *
     * Ini menyamakan hasilnya dengan web, yang menyaring dengan cara berbeda tapi
     * bermuara pada aturan yang sama: `getVoidOrders` memakai service-role sehingga
     * `orders` selalu terbaca, lalu membuang baris yang `orders.outlet_id`-nya tidak
     * ada di `staff_outlets` pengguna.
     *
     * Sempat ditampilkan dengan nilai cadangan ("Outlet tidak dikenal", "Rp 0") dan
     * itu dua kesalahan sekaligus: angkanya karangan, dan pengajuannya memang bukan
     * milik pengguna ini.
     */
    private fun petakanVoid(baris: JsonObject): PengajuanVoid? {
        val id = baris.optString("id") ?: return null
        val pesanan = baris.optJsonObject("orders") ?: return null

        return PengajuanVoid(
            id = id,
            alasan = baris.optString("reason").orEmpty(),
            dibuatPada = baris.optString("created_at").orEmpty(),
            // `order_number` bertipe angka di database; dibaca lewat double supaya
            // nilai besar tidak terpotong, lalu ditulis tanpa desimal.
            nomorOrder = pesanan.optDouble("order_number")?.toLong()?.toString() ?: "-",
            namaPelanggan = pesanan.optString("customer_name")?.takeIf { it.isNotBlank() }
                ?: "Tanpa nama",
            total = pesanan.optDouble("total_amount")?.toLong() ?: 0L,
            outletNama = pesanan.optJsonObject("outlets")?.optString("name")
                ?: "Outlet tidak dikenal",
            // Kasir bisa saja sudah pindah outlet sehingga barisnya tak terbaca;
            // itu hanya soal nama, bukan soal kewenangan, jadi cukup dicadangkan.
            pemohon = baris.optJsonObject("outlet_staff")?.optString("name") ?: "Kasir",
            items = pesanan.optJsonArray("order_items")?.map { elemen ->
                val item = elemen.asJsonObject
                ItemPesananVoid(
                    nama = item.optString("menu_item_name") ?: "Item",
                    qty = item.optInt("quantity") ?: 0,
                    subtotal = item.optDouble("subtotal")?.toLong() ?: 0L,
                )
            }.orEmpty(),
        )
    }

    private suspend fun bypass(): List<PengajuanBypass> =
        Postgrest.select(
            "bypass_requests",
            listOf(
                "select" to "id,outlet_id,requested_by_name,reason,created_at,outlets(name)",
                "status" to "eq.pending",
                "order" to "created_at.desc",
                "limit" to BATAS.toString(),
            ),
        ).mapNotNull { elemen ->
            val baris = elemen.asJsonObject
            val id = baris.optString("id") ?: return@mapNotNull null
            PengajuanBypass(
                id = id,
                outletId = baris.optString("outlet_id").orEmpty(),
                outletNama = baris.optJsonObject("outlets")?.optString("name")
                    ?: "Outlet tidak dikenal",
                pemohon = baris.optString("requested_by_name")?.takeIf { it.isNotBlank() } ?: "Kasir",
                alasan = baris.optString("reason")?.takeIf { it.isNotBlank() } ?: "-",
                dibuatPada = baris.optString("created_at").orEmpty(),
            )
        }

    /**
     * Menyetujui atau menolak satu pengajuan bypass.
     *
     * Filter `status=eq.pending` ikut dikirim supaya dua manajer yang menekan tombol
     * bersamaan tidak sama-sama berhasil — penjaga yang sama dipakai layar Waste.
     *
     * @return null kalau berhasil; pesan siap-tampil kalau kalah balapan.
     */
    suspend fun prosesBypass(id: String, setujui: Boolean): String? {
        val patch = JsonObject().apply {
            addProperty("status", if (setujui) "approved" else "rejected")
            addProperty("resolved_at", java.time.Instant.now().toString())
        }
        val hasil = Postgrest.update(
            "bypass_requests",
            listOf("id" to "eq.$id", "status" to "eq.pending"),
            patch,
        )
        return if (hasil.size() == 0) {
            "Gagal memproses: pengajuan mungkin sudah diproses orang lain."
        } else {
            null
        }
    }
}
