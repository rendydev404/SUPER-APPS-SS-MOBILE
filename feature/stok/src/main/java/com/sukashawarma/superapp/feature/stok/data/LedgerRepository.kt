package com.sukashawarma.superapp.feature.stok.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optBoolean
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optInt
import com.sukashawarma.superapp.data.remote.optJsonArray
import com.sukashawarma.superapp.data.remote.optJsonObject
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.feature.stok.data.model.LedgerDetailRow
import com.sukashawarma.superapp.feature.stok.data.model.LedgerTransaksi
import com.sukashawarma.superapp.feature.stok.domain.UnitMeta

/**
 * Buku ledger stok — cermin `hooks/useLedger.ts` di web.
 *
 * Daftar dibaca dari view `ledger_transaksi_ringkas`, yang sudah menggabungkan
 * beberapa baris ledger dari satu dokumen menjadi satu kejadian.
 */
object LedgerRepository {

    const val PAGE_SIZE = 50

    private const val RINGKAS_SELECT =
        "transaksi_key,outlet_id,created_at,jumlah_bahan,ref_order_id,ref_opname_id," +
            "ref_shipment_id,ref_transfer_id,single_bahan_baku_id,single_tipe,single_qty," +
            "single_catatan,single_saldo_sesudah"

    /**
     * Satu halaman kejadian ledger.
     *
     * Pada halaman pertama, waste yang masih `PENDING` ikut disisipkan walau belum
     * menyentuh ledger. Ini disengaja dan sama dengan web: tanpa itu, kru yang baru
     * melaporkan waste tidak melihat jejak apa pun dan cenderung melapor dua kali.
     */
    suspend fun daftar(outletId: String, page: Int = 0): List<LedgerTransaksi> {
        val ringkas = Postgrest.select(
            "ledger_transaksi_ringkas",
            listOf(
                "select" to RINGKAS_SELECT,
                "outlet_id" to "eq.$outletId",
                "order" to "created_at.desc",
                "limit" to PAGE_SIZE.toString(),
                "offset" to (page * PAGE_SIZE).toString(),
            ),
        ).mapNotNull { it.asJsonObject.toTransaksi() }

        val gabungan = if (page == 0) {
            (wastePending(outletId) + ringkas).sortedByDescending { it.createdAt.orEmpty() }
        } else {
            ringkas
        }

        return perkaya(gabungan)
    }

    private suspend fun wastePending(outletId: String): List<LedgerTransaksi> = try {
        Postgrest.select(
            "stok_waste_reports",
            listOf(
                "select" to "id,created_at,bahan_baku_id,qty,reason",
                "outlet_id" to "eq.$outletId",
                "status" to "eq.PENDING",
            ),
        ).mapNotNull { el ->
            val o = el.asJsonObject
            val id = o.optString("id") ?: return@mapNotNull null
            LedgerTransaksi(
                transaksiKey = "waste_pending_$id",
                outletId = outletId,
                createdAt = o.optString("created_at"),
                jumlahBahan = 1,
                refOrderId = null,
                refOpnameId = null,
                refShipmentId = null,
                refTransferId = null,
                singleBahanBakuId = o.optString("bahan_baku_id"),
                singleTipe = "waste_pending",
                // Waste selalu mengurangi stok, jadi ditampilkan bertanda negatif
                // walau tabel menyimpannya sebagai angka positif.
                singleQty = -(o.optDouble("qty") ?: 0.0),
                singleCatatan = o.optString("reason"),
                singleSaldoSesudah = null,
            )
        }
    } catch (_: Exception) {
        // Waste pending hanyalah pelengkap; kegagalannya tidak boleh mengosongkan ledger.
        emptyList()
    }

    /**
     * Lengkapi judul kejadian: nomor order beserta menunya, tanggal/tipe opname, dan
     * nama outlet tujuan surat jalan.
     *
     * Outlet tujuan perlu dicari terpisah karena `ledger_stok.outlet_id` pada baris
     * `transfer_keluar` berisi outlet GUDANG (pengirim), bukan outlet peminta.
     */
    private suspend fun perkaya(baris: List<LedgerTransaksi>): List<LedgerTransaksi> {
        val orderIds = baris.mapNotNull { it.refOrderId }.distinct()
        val opnameIds = baris.mapNotNull { it.refOpnameId }.distinct()
        val shipmentIds = baris.mapNotNull { it.refShipmentId }.distinct()

        val orders = if (orderIds.isEmpty()) emptyMap() else runCatching {
            Postgrest.select(
                "orders",
                listOf(
                    "select" to "id,order_number,order_items(menu_item_name)",
                    "id" to "in.(${orderIds.joinToString(",")})",
                ),
            ).mapNotNull { el ->
                val o = el.asJsonObject
                val id = o.optString("id") ?: return@mapNotNull null
                val menu = (o.optJsonArray("order_items") ?: JsonArray())
                    .mapNotNull { it.asJsonObject.optString("menu_item_name") }
                    .joinToString(", ")
                    .ifBlank { null }
                id to (o.optDouble("order_number")?.toLong() to menu)
            }.toMap()
        }.getOrDefault(emptyMap())

        val opnames = if (opnameIds.isEmpty()) emptyMap() else runCatching {
            Postgrest.select(
                "opname",
                listOf(
                    "select" to "id,tanggal,tipe",
                    "id" to "in.(${opnameIds.joinToString(",")})",
                ),
            ).mapNotNull { el ->
                val o = el.asJsonObject
                val id = o.optString("id") ?: return@mapNotNull null
                id to (o.optString("tanggal") to o.optString("tipe"))
            }.toMap()
        }.getOrDefault(emptyMap())

        val shipments = if (shipmentIds.isEmpty()) emptyMap() else runCatching {
            Postgrest.select(
                "surat_jalan",
                listOf(
                    "select" to "id,outlets(name)",
                    "id" to "in.(${shipmentIds.joinToString(",")})",
                ),
            ).mapNotNull { el ->
                val o = el.asJsonObject
                val id = o.optString("id") ?: return@mapNotNull null
                id to o.optJsonObject("outlets")?.optString("name")
            }.toMap()
        }.getOrDefault(emptyMap())

        return baris.map { t ->
            t.copy(
                orderNumber = t.refOrderId?.let { orders[it]?.first },
                orderItemsNames = t.refOrderId?.let { orders[it]?.second },
                opnameTanggal = t.refOpnameId?.let { opnames[it]?.first },
                opnameTipe = t.refOpnameId?.let { opnames[it]?.second },
                shipmentDestOutletName = t.refShipmentId?.let { shipments[it] },
            )
        }
    }

    private fun JsonObject.toTransaksi(): LedgerTransaksi? {
        val key = optString("transaksi_key") ?: return null
        val outletId = optString("outlet_id") ?: return null
        return LedgerTransaksi(
            transaksiKey = key,
            outletId = outletId,
            createdAt = optString("created_at"),
            jumlahBahan = optInt("jumlah_bahan") ?: 1,
            refOrderId = optString("ref_order_id"),
            refOpnameId = optString("ref_opname_id"),
            refShipmentId = optString("ref_shipment_id"),
            refTransferId = optString("ref_transfer_id"),
            singleBahanBakuId = optString("single_bahan_baku_id"),
            singleTipe = optString("single_tipe"),
            singleQty = optDouble("single_qty"),
            singleCatatan = optString("single_catatan"),
            singleSaldoSesudah = optDouble("single_saldo_sesudah"),
        )
    }

    /**
     * Rincian satu kejadian. Kunci transaksi bisa berupa id dokumen mana pun, jadi
     * dicocokkan ke seluruh kolom referensi sekaligus — sama seperti web.
     */
    suspend fun detail(outletId: String, transaksiKey: String): List<LedgerDetailRow> {
        val rows = Postgrest.select(
            "ledger_stok",
            listOf(
                "select" to "id,tipe,qty,catatan,saldo_sebelum,saldo_sesudah,created_at," +
                    "bahan_baku_id,bahan_baku(nama,satuan,satuan_tengah,satuan_kecil," +
                    "faktor_tengah,faktor_tampilan)",
                "outlet_id" to "eq.$outletId",
                "or" to "(ref_order_id.eq.$transaksiKey,ref_opname_id.eq.$transaksiKey," +
                    "ref_shipment_id.eq.$transaksiKey,ref_transfer_id.eq.$transaksiKey," +
                    "id.eq.$transaksiKey)",
                "order" to "created_at.asc",
            ),
        ).mapNotNull { el ->
            val o = el.asJsonObject
            val id = o.optString("id") ?: return@mapNotNull null
            val bahanId = o.optString("bahan_baku_id") ?: return@mapNotNull null
            val bb = o.optJsonObject("bahan_baku")
            LedgerDetailRow(
                id = id,
                tipe = o.optString("tipe") ?: "adjustment",
                qty = o.optDouble("qty") ?: 0.0,
                catatan = o.optString("catatan"),
                saldoSebelum = o.optDouble("saldo_sebelum"),
                saldoSesudah = o.optDouble("saldo_sesudah"),
                createdAt = o.optString("created_at"),
                bahanBakuId = bahanId,
                namaBahan = bb?.optString("nama"),
                meta = UnitMeta(
                    satuan = bb?.optString("satuan"),
                    satuanTengah = bb?.optString("satuan_tengah"),
                    satuanKecil = bb?.optString("satuan_kecil"),
                    faktorTengah = bb?.optDouble("faktor_tengah"),
                    faktorTampilan = bb?.optDouble("faktor_tampilan"),
                ),
            )
        }
        if (rows.isEmpty()) return rows

        // `saldo_is_gram` adalah kolom terhitung di `stok_balance` (kondisi SAAT INI
        // per outlet+bahan) dan tidak tersedia di `ledger_stok`, jadi harus dicari
        // terpisah supaya angka pada layar ini tidak salah dikonversi.
        val bahanIds = rows.map { it.bahanBakuId }.distinct()
        val gram = runCatching {
            Postgrest.select(
                "stok_balance",
                listOf(
                    "select" to "bahan_baku_id,saldo_is_gram",
                    "outlet_id" to "eq.$outletId",
                    "bahan_baku_id" to "in.(${bahanIds.joinToString(",")})",
                ),
            ).mapNotNull { el ->
                val o = el.asJsonObject
                val id = o.optString("bahan_baku_id") ?: return@mapNotNull null
                id to o.optBoolean("saldo_is_gram")
            }.toMap()
        }.getOrDefault(emptyMap())

        return rows.map { it.copy(saldoIsGram = gram[it.bahanBakuId] ?: false) }
    }

    /**
     * Catatan manual ke ledger: penyesuaian masuk/keluar dan transfer keluar.
     *
     * Tanda mengikuti aturan web: `adjustment` memakai nilai apa adanya (arah
     * ditentukan pemanggil), selain itu selalu negatif. Waste TIDAK ditulis di sini —
     * waste harus lewat `stok_waste_reports` dan approval, lalu trigger yang
     * merekonsiliasi ke ledger.
     */
    suspend fun tambahManual(
        outletId: String,
        createdBy: String,
        catatanGlobal: String,
        items: List<ManualItem>,
    ) {
        if (items.isEmpty()) return
        val body = JsonArray()
        items.forEach { item ->
            val qty = item.signedOverride
                ?: if (item.tipe == "adjustment") item.qtyAbs else -kotlin.math.abs(item.qtyAbs)
            body.add(
                JsonObject().apply {
                    addProperty("outlet_id", outletId)
                    addProperty("bahan_baku_id", item.bahanBakuId)
                    addProperty("tipe", item.tipe)
                    addProperty("qty", qty)
                    addProperty("catatan", item.catatan?.ifBlank { null } ?: catatanGlobal)
                    addProperty("created_by", createdBy)
                }
            )
        }
        Postgrest.insert("ledger_stok", body, returning = false)
    }

    data class ManualItem(
        val bahanBakuId: String,
        val tipe: String,
        val qtyAbs: Double,
        val catatan: String? = null,
        val signedOverride: Double? = null,
    )
}
