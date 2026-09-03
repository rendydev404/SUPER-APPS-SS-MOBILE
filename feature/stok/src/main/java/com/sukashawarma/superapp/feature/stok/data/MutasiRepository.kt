package com.sukashawarma.superapp.feature.stok.data

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optJsonArray
import com.sukashawarma.superapp.data.remote.optJsonObject
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.feature.stok.data.model.Mutasi
import com.sukashawarma.superapp.feature.stok.data.model.MutasiItem
import com.sukashawarma.superapp.feature.stok.data.model.OutletRingkas
import com.sukashawarma.superapp.feature.stok.data.model.StatusMutasi

/**
 * Mutasi antar outlet — cermin `app/actions/mutasi.ts` dan `hooks/useMutasi.ts`.
 *
 * Web pun sudah memakai authenticated client di sini, jadi native identik: RLS dan
 * `auth.uid()` berlaku sama, dan seluruh transisi status lewat RPC yang sama.
 *
 * Alur statusnya:
 * `menunggu_persetujuan` -> `menunggu_pengiriman` -> `dikirim` -> `selesai`,
 * dengan `ditolak` sebagai cabang dari langkah persetujuan.
 */
object MutasiRepository {

    private const val SELECT_FULL =
        "*,mutasi_antar_outlet_item(*,bahan_baku(nama,satuan))," +
            "outlet_asal:outlets!outlet_asal_id(name),outlet_tujuan:outlets!outlet_tujuan_id(name)"

    // -------------------------------------------------------------- pembacaan

    /**
     * Mutasi yang menyangkut satu outlet, baik sebagai pengirim maupun penerima.
     * Tanpa `outletId`, seluruh mutasi yang terlihat oleh RLS dikembalikan.
     */
    suspend fun daftar(outletId: String?): List<Mutasi> {
        val params = buildList {
            add("select" to SELECT_FULL)
            if (outletId != null) {
                add("or" to "(outlet_asal_id.eq.$outletId,outlet_tujuan_id.eq.$outletId)")
            }
            add("order" to "created_at.desc")
        }
        return Postgrest.select("mutasi_antar_outlet", params)
            .mapNotNull { it.asJsonObject.toMutasi() }
            .lengkapiNamaStaff()
    }

    suspend fun detail(mutasiId: String): Mutasi? {
        val row = Postgrest.selectOne(
            "mutasi_antar_outlet",
            listOf("select" to SELECT_FULL, "id" to "eq.$mutasiId"),
        ) ?: return null
        return row.toMutasi()?.let { listOf(it).lengkapiNamaStaff().first() }
    }

    private suspend fun List<Mutasi>.lengkapiNamaStaff(): List<Mutasi> {
        val ids = flatMap { listOfNotNull(it.pembuatNama, it.approverNama, it.penerimaNama) }.distinct()
        if (ids.isEmpty()) return this
        val nama = runCatching {
            Postgrest.select(
                "outlet_staff",
                listOf("select" to "id,name", "id" to "in.(${ids.joinToString(",")})"),
            ).mapNotNull { el ->
                val o = el.asJsonObject
                val id = o.optString("id") ?: return@mapNotNull null
                id to (o.optString("name") ?: "")
            }.toMap()
        }.getOrDefault(emptyMap())
        return map {
            it.copy(
                pembuatNama = it.pembuatNama?.let { id -> nama[id] } ?: "Sistem",
                approverNama = it.approverNama?.let { id -> nama[id] },
                penerimaNama = it.penerimaNama?.let { id -> nama[id] },
            )
        }
    }

    /**
     * Sementara ini kolom pembuat/approver/penerima diisi ID mentah, lalu ditukar
     * menjadi nama oleh [lengkapiNamaStaff]. Dipisah supaya satu query nama cukup
     * untuk seluruh daftar, bukan satu query per baris.
     */
    private fun JsonObject.toMutasi(): Mutasi? {
        val id = optString("id") ?: return null
        val asal = optString("outlet_asal_id") ?: return null
        val tujuan = optString("outlet_tujuan_id") ?: return null
        val items = (optJsonArray("mutasi_antar_outlet_item") ?: JsonArray()).mapNotNull { el ->
            val o = el.asJsonObject
            val itemId = o.optString("id") ?: return@mapNotNull null
            val bahanId = o.optString("bahan_baku_id") ?: return@mapNotNull null
            val bb = o.optJsonObject("bahan_baku")
            MutasiItem(
                id = itemId,
                bahanBakuId = bahanId,
                namaBahan = bb?.optString("nama"),
                satuan = bb?.optString("satuan"),
                qtyDiajukan = o.optDouble("qty_diajukan") ?: 0.0,
                qtyDikirim = o.optDouble("qty_dikirim"),
                qtyDiterima = o.optDouble("qty_diterima"),
                kondisiDiterima = o.optString("kondisi_diterima"),
            )
        }
        return Mutasi(
            id = id,
            outletAsalId = asal,
            outletTujuanId = tujuan,
            outletAsalNama = optJsonObject("outlet_asal")?.optString("name"),
            outletTujuanNama = optJsonObject("outlet_tujuan")?.optString("name"),
            status = StatusMutasi.dari(optString("status")),
            catatan = optString("catatan"),
            catatanPenolakan = optString("catatan_penolakan"),
            createdAt = optString("created_at"),
            pembuatNama = optString("created_by"),
            approverNama = optString("approved_by"),
            penerimaNama = optString("received_by"),
            items = items,
        )
    }

    /** Outlet tujuan yang bisa dipilih — semua outlet aktif selain outlet asal. */
    suspend fun outletTujuan(kecualiOutletId: String): List<OutletRingkas> = Postgrest.select(
        "outlets",
        listOf(
            "select" to "id,name",
            "is_active" to "eq.true",
            "id" to "neq.$kecualiOutletId",
            "order" to "name.asc",
        ),
    ).mapNotNull { el ->
        val o = el.asJsonObject
        val id = o.optString("id") ?: return@mapNotNull null
        OutletRingkas(id = id, name = o.optString("name") ?: "Outlet")
    }

    // -------------------------------------------------------------- penulisan

    data class ItemAjuan(val bahanBakuId: String, val qtyDiajukan: Double)

    suspend fun ajukan(
        outletAsalId: String,
        outletTujuanId: String,
        catatan: String,
        items: List<ItemAjuan>,
    ): String? {
        val arr = JsonArray()
        items.forEach {
            arr.add(
                JsonObject().apply {
                    addProperty("bahan_baku_id", it.bahanBakuId)
                    addProperty("qty_diajukan", it.qtyDiajukan)
                }
            )
        }
        val hasil = Postgrest.rpc("ajukan_mutasi", JsonObject().apply {
            addProperty("p_outlet_asal_id", outletAsalId)
            addProperty("p_outlet_tujuan_id", outletTujuanId)
            addProperty("p_catatan", catatan)
            add("p_items", arr)
        })
        return if (hasil.isJsonPrimitive) hasil.asString else null
    }

    suspend fun setujui(mutasiId: String, disetujui: Boolean, catatanPenolakan: String? = null) {
        Postgrest.rpc("approve_mutasi", JsonObject().apply {
            addProperty("p_mutasi_id", mutasiId)
            addProperty("p_is_approved", disetujui)
            if (catatanPenolakan.isNullOrBlank()) add("p_catatan_penolakan", JsonNull.INSTANCE)
            else addProperty("p_catatan_penolakan", catatanPenolakan)
        })
    }

    data class ItemKirim(val itemId: String, val qtyDikirim: Double)

    /** Menulis `transfer_keluar` negatif di outlet asal lewat RPC yang sadar skala satuan. */
    suspend fun kirim(mutasiId: String, kurir: String, items: List<ItemKirim>) {
        val arr = JsonArray()
        items.forEach {
            arr.add(
                JsonObject().apply {
                    addProperty("item_id", it.itemId)
                    addProperty("qty_dikirim", it.qtyDikirim)
                }
            )
        }
        Postgrest.rpc("kirim_mutasi", JsonObject().apply {
            addProperty("p_mutasi_id", mutasiId)
            add("p_kurir_info", JsonObject().apply { addProperty("nama", kurir) })
            add("p_items_dikirim", arr)
        })
    }

    data class ItemTerima(
        val itemId: String,
        val qtyDiterima: Double,
        val kondisi: String,
    )

    /** Menulis `transfer_masuk` positif di outlet tujuan. */
    suspend fun terima(mutasiId: String, items: List<ItemTerima>) {
        val arr = JsonArray()
        items.forEach {
            arr.add(
                JsonObject().apply {
                    addProperty("item_id", it.itemId)
                    addProperty("qty_diterima", it.qtyDiterima)
                    addProperty("kondisi_diterima", it.kondisi)
                }
            )
        }
        Postgrest.rpc("terima_mutasi", JsonObject().apply {
            addProperty("p_mutasi_id", mutasiId)
            add("p_items_diterima", arr)
        })
    }
}
