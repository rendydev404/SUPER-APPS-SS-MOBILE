package com.sukashawarma.superapp.feature.stok.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optBoolean
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optJsonArray
import com.sukashawarma.superapp.data.remote.optJsonObject
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.feature.stok.data.model.Permintaan
import com.sukashawarma.superapp.feature.stok.data.model.PermintaanItem
import com.sukashawarma.superapp.feature.stok.data.model.SaranPermintaan
import com.sukashawarma.superapp.feature.stok.data.model.StatusPermintaan

/**
 * Permintaan bahan — cermin `app/actions/permintaan.ts` dan `hooks/usePermintaan.ts`.
 *
 * Web membaca lewat service-role, tetapi RLS `select_permintaan_bahan_accessible_outlets`
 * sudah mengizinkan pembacaan yang sama untuk user terautentikasi, jadi native memakai
 * JWT pengguna dan hasilnya identik tanpa perlu melewati RLS.
 *
 * Seluruh penulisan lewat RPC SECURITY DEFINER yang sama persis dengan web:
 * `buat_permintaan_svc`, `approve_permintaan_svc`, dan `tolak_permintaan_svc`.
 */
object PermintaanRepository {

    private const val SELECT_FULL =
        "*,permintaan_bahan_item(*,bahan_baku(nama,satuan)),outlets(name)"

    // -------------------------------------------------------------- pembacaan

    suspend fun daftarOutlet(outletId: String): List<Permintaan> = Postgrest.select(
        "permintaan_bahan",
        listOf(
            "select" to SELECT_FULL,
            "outlet_id" to "eq.$outletId",
            "order" to "created_at.desc",
        ),
    ).mapNotNull { it.asJsonObject.toPermintaan() }.lengkapiNamaPembuat()

    /** Permintaan berstatus `menunggu` pada outlet yang boleh direview. */
    suspend fun menunggu(outletIds: List<String>): List<Permintaan> {
        if (outletIds.isEmpty()) return emptyList()
        return Postgrest.select(
            "permintaan_bahan",
            listOf(
                "select" to SELECT_FULL,
                "status" to "eq.menunggu",
                "outlet_id" to "in.(${outletIds.joinToString(",")})",
                "order" to "created_at.desc",
            ),
        ).mapNotNull { it.asJsonObject.toPermintaan() }.lengkapiNamaPembuat()
    }

    /** Nama pembuat tidak tersedia lewat embed, jadi diambil sekali untuk semua baris. */
    private suspend fun List<Permintaan>.lengkapiNamaPembuat(): List<Permintaan> {
        val ids = mapNotNull { it.dibuatOleh }.distinct()
        if (ids.isEmpty()) return this
        val nama = runCatching {
            Postgrest.select(
                "outlet_staff",
                listOf(
                    "select" to "id,name",
                    "id" to "in.(${ids.joinToString(",")})",
                ),
            ).mapNotNull { el ->
                val o = el.asJsonObject
                val id = o.optString("id") ?: return@mapNotNull null
                id to (o.optString("name") ?: "")
            }.toMap()
        }.getOrDefault(emptyMap())
        return map { it.copy(pembuatNama = it.dibuatOleh?.let { id -> nama[id] } ?: "Sistem") }
    }

    private fun JsonObject.toPermintaan(): Permintaan? {
        val id = optString("id") ?: return null
        val outletId = optString("outlet_id") ?: return null
        val items = (optJsonArray("permintaan_bahan_item") ?: JsonArray()).mapNotNull { el ->
            val o = el.asJsonObject
            val bahanId = o.optString("bahan_baku_id") ?: return@mapNotNull null
            val bb = o.optJsonObject("bahan_baku")
            PermintaanItem(
                id = o.optString("id"),
                bahanBakuId = bahanId,
                namaBahan = bb?.optString("nama"),
                satuan = bb?.optString("satuan"),
                qtyDiminta = o.optDouble("qty_diminta") ?: 0.0,
                qtyDisetujui = o.optDouble("qty_disetujui"),
                hargaSnapshot = o.optDouble("harga_snapshot"),
            )
        }
        return Permintaan(
            id = id,
            outletId = outletId,
            outletName = optJsonObject("outlets")?.optString("name"),
            status = StatusPermintaan.dari(optString("status")),
            createdAt = optString("created_at"),
            dibuatOleh = optString("dibuat_oleh"),
            pembuatNama = null,
            alasanPenolakan = optString("alasan_penolakan"),
            suratJalanId = optString("surat_jalan_id"),
            items = items,
        )
    }

    /**
     * Bahan yang disarankan untuk diminta: yang statusnya `below` atau `warning`.
     *
     * Dibaca dari `monitoring_view_crew` — view SECURITY DEFINER, sama seperti web,
     * supaya saldo tetap terbaca walau RLS `stok_balance` membatasi.
     */
    suspend fun saran(outletId: String): List<SaranPermintaan> = Postgrest.select(
        "monitoring_view_crew",
        listOf(
            "select" to "bahan_baku_id,item_name,satuan,current_qty,saldo_is_gram,threshold,status",
            "outlet_id" to "eq.$outletId",
        ),
    ).mapNotNull { el ->
        val o = el.asJsonObject
        val status = o.optString("status")
        if (status != "below" && status != "warning") return@mapNotNull null
        val bahanId = o.optString("bahan_baku_id") ?: return@mapNotNull null
        SaranPermintaan(
            bahanBakuId = bahanId,
            itemName = o.optString("item_name") ?: "(tanpa nama)",
            satuan = o.optString("satuan"),
            currentQty = o.optDouble("current_qty") ?: 0.0,
            saldoIsGram = o.optBoolean("saldo_is_gram"),
            threshold = o.optDouble("threshold") ?: 0.0,
            status = status,
        )
    }

    // -------------------------------------------------------------- penulisan

    data class ItemDiminta(val bahanBakuId: String, val qtyDiminta: Double)

    /**
     * Buat permintaan baru.
     *
     * RPC ini juga membatalkan permintaan `menunggu` yang lebih tua dari 12 jam di
     * outlet yang sama bila ada bahan yang tumpang tindih — perilaku itu ada di
     * dalam fungsi database, bukan di sini.
     */
    suspend fun buat(outletId: String, dibuatOleh: String, items: List<ItemDiminta>) {
        val arr = JsonArray()
        items.forEach { item ->
            arr.add(
                JsonObject().apply {
                    addProperty("bahan_baku_id", item.bahanBakuId)
                    addProperty("qty_diminta", item.qtyDiminta)
                }
            )
        }
        Postgrest.rpc("buat_permintaan_svc", JsonObject().apply {
            addProperty("p_outlet_id", outletId)
            add("p_items", arr)
            addProperty("p_dibuat_oleh", dibuatOleh)
            add("p_target_metadata", JsonArray())
        })
    }

    data class ItemDisetujui(val bahanBakuId: String, val qtyDisetujui: Double)

    /**
     * Setujui permintaan. RPC membuat surat jalan dan mendebit dompet outlet.
     *
     * Wajib ada minimal satu item dengan qty di atas nol; kalau semuanya nol, RPC
     * menolak dan menyuruh memakai jalur tolak. Kondisi itu dijaga di UI juga supaya
     * pengguna tidak menemui pesan error database.
     */
    suspend fun setujui(permintaanId: String, items: List<ItemDisetujui>) {
        val arr = JsonArray()
        items.forEach { item ->
            arr.add(
                JsonObject().apply {
                    addProperty("bahan_baku_id", item.bahanBakuId)
                    addProperty("qty_disetujui", item.qtyDisetujui)
                }
            )
        }
        Postgrest.rpc("approve_permintaan_svc", JsonObject().apply {
            addProperty("p_permintaan_id", permintaanId)
            add("p_items", arr)
        })
    }

    suspend fun tolak(permintaanId: String, alasan: String) {
        Postgrest.rpc("tolak_permintaan_svc", JsonObject().apply {
            addProperty("p_permintaan_id", permintaanId)
            addProperty("p_alasan", alasan)
        })
    }
}
