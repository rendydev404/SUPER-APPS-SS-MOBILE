package com.sukashawarma.superapp.feature.stok.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optJsonObject
import com.sukashawarma.superapp.data.remote.optString
import java.time.LocalDate

/** Satu PO yang barangnya sedang dalam perjalanan dari supplier. */
data class PoInbound(
    val id: String,
    val nomorPo: String,
    val supplierNama: String,
    val status: String,
    val tanggal: String?,
    val total: Double?,
) {
    /** `sebagian_diterima` berarti sudah pernah ada penerimaan parsial. */
    val sebagian: Boolean get() = status == "sebagian_diterima"
}

/** Satu baris barang pada PO, beserta apa yang sudah pernah diterima. */
data class ItemPo(
    val id: String,
    val bahanBakuId: String?,
    val nama: String,
    val satuan: String,
    val qtyPesan: Double,
    val qtyTerimaSebelumnya: Double,
    val hargaPesan: Double,
) {
    /** Sisa yang belum tiba; itulah angka yang paling mungkin diisi hari ini. */
    val sisa: Double get() = (qtyPesan - qtyTerimaSebelumnya).coerceAtLeast(0.0)
}

/**
 * Penerimaan PO supplier — cermin `app/stok/penerimaan-po/page.tsx` dan
 * `KitchenVerifikasiModal.tsx` di web.
 *
 * Daftar diambil lewat `get_purchase_orders` (SECURITY DEFINER) lalu disaring ke
 * dua status yang memang menunggu barang, sama seperti web. Verifikasinya lewat
 * `verifikasi_terima_po`, yang menulis stok masuk, harga beli, dan memperbarui
 * status PO dalam satu transaksi — jadi native tidak perlu menyusun ulang
 * ketiganya sendiri.
 */
object PenerimaanPoRepository {

    private val STATUS_MENUNGGU = setOf("dikirim_ke_supplier", "sebagian_diterima")

    /** PO 30 hari terakhir yang masih menunggu barang — rentang yang sama dengan web. */
    suspend fun daftar(): List<PoInbound> {
        val hasil = Postgrest.rpc(
            "get_purchase_orders",
            JsonObject().apply {
                addProperty("p_from", LocalDate.now().minusDays(30).toString())
                addProperty("p_to", LocalDate.now().toString())
                add("p_status", com.google.gson.JsonNull.INSTANCE)
            },
        )
        if (!hasil.isJsonArray) return emptyList()
        return hasil.asJsonArray.mapNotNull { elemen ->
            val baris = elemen.asJsonObject
            val status = baris.optString("status").orEmpty()
            if (status !in STATUS_MENUNGGU) return@mapNotNull null
            val id = baris.optString("id") ?: return@mapNotNull null
            PoInbound(
                id = id,
                nomorPo = baris.optString("nomor_po").orEmpty(),
                supplierNama = baris.optString("supplier_nama").orEmpty().ifBlank { "Supplier" },
                status = status,
                tanggal = baris.optString("tanggal") ?: baris.optString("created_at"),
                total = baris.optDouble("total"),
            )
        }
    }

    /**
     * Baris barang sebuah PO.
     *
     * `qty_terima` yang tersimpan adalah AKUMULASI, bukan penerimaan terakhir —
     * itu sebabnya nilainya dibawa terpisah sebagai [ItemPo.qtyTerimaSebelumnya]
     * dan dijumlahkan lagi saat mengirim.
     */
    suspend fun item(poId: String): List<ItemPo> = Postgrest.select(
        "purchase_order_item",
        listOf(
            "select" to "id,bahan_baku_id,item_description,satuan_ad_hoc,qty_pesan," +
                "qty_terima,harga_pesan,harga_terima,bahan_baku(nama,satuan)",
            "purchase_order_id" to "eq.$poId",
        ),
    ).mapNotNull { elemen ->
        val baris = elemen.asJsonObject
        val id = baris.optString("id") ?: return@mapNotNull null
        val bahan = baris.optJsonObject("bahan_baku")
        ItemPo(
            id = id,
            bahanBakuId = baris.optString("bahan_baku_id"),
            // Item ad-hoc (belum jadi master bahan) tetap harus terlihat namanya,
            // bukan kosong — web memakai urutan cadangan yang sama.
            nama = bahan?.optString("nama")
                ?: baris.optString("item_description")
                ?: "Item Ad-Hoc",
            satuan = bahan?.optString("satuan") ?: baris.optString("satuan_ad_hoc") ?: "pcs",
            qtyPesan = baris.optDouble("qty_pesan") ?: 0.0,
            qtyTerimaSebelumnya = baris.optDouble("qty_terima") ?: 0.0,
            hargaPesan = baris.optDouble("harga_pesan") ?: 0.0,
        )
    }

    /** Satu baris hasil pemeriksaan fisik yang akan dikirim. */
    data class Verifikasi(
        val itemId: String,
        val bahanBakuId: String?,
        val qtyDatang: Double,
        val qtyTerimaSebelumnya: Double,
        val hargaTerima: Double,
        val kondisi: String,
        val catatan: String?,
    )

    suspend fun verifikasi(poId: String, items: List<Verifikasi>) {
        val payload = JsonArray()
        items.forEach { item ->
            payload.add(
                JsonObject().apply {
                    addProperty("id", item.itemId)
                    addProperty("bahan_baku_id", item.bahanBakuId)
                    addProperty("qty_datang", item.qtyDatang)
                    // RPC menuntut angka KUMULATIF di `qty_terima`; mengirim hanya
                    // yang datang hari ini akan menghapus penerimaan sebelumnya.
                    addProperty("qty_terima", item.qtyTerimaSebelumnya + item.qtyDatang)
                    addProperty("harga_terima", item.hargaTerima)
                    addProperty("kondisi", item.kondisi)
                    addProperty("catatan", item.catatan?.takeIf { it.isNotBlank() })
                }
            )
        }
        val hasil = Postgrest.rpc(
            "verifikasi_terima_po",
            JsonObject().apply {
                addProperty("p_po_id", poId)
                add("p_items", payload)
            },
        )
        // RPC ini melaporkan kegagalan lewat badan jawaban, bukan kode HTTP —
        // tanpa pemeriksaan ini, penolakan server akan terlihat seperti sukses.
        if (hasil.isJsonObject) {
            val objek = hasil.asJsonObject
            val sukses = objek.get("success")?.takeIf { it.isJsonPrimitive }?.asBoolean
            if (sukses == false) {
                throw IllegalStateException(
                    objek.optString("message") ?: "Gagal memverifikasi PO."
                )
            }
        }
    }
}
