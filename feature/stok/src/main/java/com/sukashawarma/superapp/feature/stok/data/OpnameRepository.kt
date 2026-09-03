package com.sukashawarma.superapp.feature.stok.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optBoolean
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optJsonArray
import com.sukashawarma.superapp.data.remote.optJsonObject
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.feature.stok.data.model.OpnameHeader
import com.sukashawarma.superapp.feature.stok.data.model.StatusOpname
import java.time.LocalDate
import java.time.ZoneId

/**
 * Stock opname — cermin `hooks/useOpname.ts` dan `app/actions/opname.ts` di web.
 *
 * Web menyimpan item lewat service-role dengan alasan "RLS opname_item tidak
 * mengizinkan INSERT dari crew". Komentar itu sudah usang: policy
 * `opname_item_write` (FOR ALL TO authenticated) mengizinkannya selama
 * `opname.status = 'draft'` dan outletnya ada di `accessible_outlet_ids()`.
 * Native menulis langsung di bawah RLS, tanpa service-role.
 *
 * Konsekuensinya satu: native hanya bisa menyimpan item selama opname masih
 * `draft`. Web bisa juga saat `pending_approval` karena mem-bypass RLS. Itu
 * perbedaan yang disengaja — mengejarnya berarti menaruh service key di dalam APK.
 */
object OpnameRepository {

    private val WIB: ZoneId = ZoneId.of("Asia/Jakarta")

    /** Tanggal hari ini menurut WIB, format `YYYY-MM-DD`. */
    fun hariIniWIB(): String = LocalDate.now(WIB).toString()

    /**
     * Web punya `getEffectiveTodayWIB` dengan sederet pengecualian outlet/tanggal
     * (Cileungsi 21 Agu, Empang 24 Agu, Paledang 26 Agu, Jatiwaringin 30 Agu 2026,
     * dan jatah opname ganda 13 Agu 2026). Seluruh tanggal itu sudah lewat, sehingga
     * cabangnya tidak akan pernah aktif lagi. Sengaja tidak diport: menyalin cabang
     * mati hanya menambah jalur yang tak teruji, dan perilakunya untuk semua tanggal
     * ke depan identik dengan jalur normal ini.
     */
    private fun tanggalEfektif(): String = hariIniWIB()

    // -------------------------------------------------------------- pembacaan

    suspend fun daftar(outletId: String): List<OpnameHeader> = Postgrest.select(
        "opname",
        listOf(
            "select" to "id,outlet_id,tanggal,tipe,status,created_by,created_at," +
                "outlet_staff!opname_created_by_fkey(name)," +
                "opname_item(qty_fisik,qty_system,selisih,flagged)",
            "outlet_id" to "eq.$outletId",
            "order" to "tanggal.desc",
            "limit" to "60",
        ),
    ).mapNotNull { it.asJsonObject.toHeader() }

    /** Opname yang menunggu persetujuan, dibatasi outlet yang boleh diakses. */
    suspend fun menungguPersetujuan(outletIds: List<String>): List<OpnameHeader> {
        if (outletIds.isEmpty()) return emptyList()
        return Postgrest.select(
            "opname",
            listOf(
                "select" to "id,outlet_id,tanggal,tipe,status,created_by,created_at," +
                    "outlets(name),outlet_staff!opname_created_by_fkey(name)," +
                    "opname_item(qty_fisik,qty_system,selisih,flagged)",
                "status" to "eq.pending_approval",
                "outlet_id" to "in.(${outletIds.joinToString(",")})",
                "order" to "created_at.desc",
            ),
        ).mapNotNull { it.asJsonObject.toHeader() }
    }

    private fun JsonObject.toHeader(): OpnameHeader? {
        val id = optString("id") ?: return null
        val outletId = optString("outlet_id") ?: return null
        val items = optJsonArray("opname_item") ?: JsonArray()
        return OpnameHeader(
            id = id,
            outletId = outletId,
            tanggal = optString("tanggal"),
            tipe = optString("tipe"),
            status = StatusOpname.dari(optString("status")),
            createdBy = optString("created_by"),
            createdAt = optString("created_at"),
            outletName = optJsonObject("outlets")?.optString("name"),
            creatorName = optJsonObject("outlet_staff")?.optString("name"),
            jumlahItem = items.size(),
            jumlahFlagged = items.count { it.asJsonObject.optBoolean("flagged") },
        )
    }

    /** Draft yang masih berjalan hari ini, kalau ada. */
    suspend fun draftHariIni(outletId: String): JsonObject? = Postgrest.selectOne(
        "opname",
        listOf(
            "select" to "*,opname_item(*)",
            "outlet_id" to "eq.$outletId",
            "tanggal" to "eq.${tanggalEfektif()}",
            "status" to "eq.draft",
            "order" to "created_at.desc",
        ),
    )

    /**
     * Ambil draft hari ini bila ada, atau buat baru.
     *
     * Pencarian mengabaikan opname berstatus `rejected` — opname yang ditolak berarti
     * kru harus menghitung ulang, jadi tidak boleh dipakai ulang sebagai draft.
     */
    suspend fun buatAtauPakaiDraft(
        outletId: String,
        tipe: String,
        createdBy: String,
        catatan: String? = null,
    ): OpnameHeader {
        val tanggal = tanggalEfektif()

        val adaSebelumnya = Postgrest.selectOne(
            "opname",
            listOf(
                "select" to "id,outlet_id,tanggal,tipe,status,created_by,created_at",
                "outlet_id" to "eq.$outletId",
                "tipe" to "eq.$tipe",
                "tanggal" to "eq.$tanggal",
                "status" to "neq.rejected",
            ),
        )
        if (adaSebelumnya != null) {
            return adaSebelumnya.toHeader() ?: error("Opname tidak terbaca")
        }

        val body = JsonObject().apply {
            addProperty("outlet_id", outletId)
            addProperty("tipe", tipe)
            addProperty("status", "draft")
            addProperty("created_by", createdBy)
            addProperty("tanggal", tanggal)
            if (catatan.isNullOrBlank()) add("notes", com.google.gson.JsonNull.INSTANCE)
            else addProperty("notes", catatan)
        }
        val hasil = Postgrest.insert("opname", body)
        val row = hasil.firstOrNull()?.asJsonObject ?: error("Gagal membuat draft opname")
        return row.toHeader() ?: error("Draft opname tidak terbaca")
    }

    // -------------------------------------------------------------- penulisan

    data class ItemSimpan(
        val opnameId: String,
        val bahanBakuId: String,
        val qtyFisik: Double,
        val qtySystem: Double,
        val flagged: Boolean,
        val catatan: String? = null,
    )

    /**
     * Simpan item hitung fisik.
     *
     * `selisih` sengaja TIDAK dikirim: kolom itu dihitung database dari
     * `qty_fisik - qty_system`. Mengirimnya sendiri berisiko berbeda dari
     * perhitungan database.
     */
    suspend fun simpanItem(items: List<ItemSimpan>) {
        if (items.isEmpty()) return
        val body = JsonArray()
        items.forEach { item ->
            body.add(
                JsonObject().apply {
                    addProperty("opname_id", item.opnameId)
                    addProperty("bahan_baku_id", item.bahanBakuId)
                    addProperty("qty_fisik", item.qtyFisik)
                    addProperty("qty_system", item.qtySystem)
                    addProperty("flagged", item.flagged)
                    if (item.catatan.isNullOrBlank()) add("catatan", com.google.gson.JsonNull.INSTANCE)
                    else addProperty("catatan", item.catatan)
                }
            )
        }
        Postgrest.upsert("opname_item", body, onConflict = "opname_id,bahan_baku_id")
    }

    /**
     * Ajukan opname untuk disetujui leader. Dipakai bila ada item yang ditandai.
     *
     * Definisi RPC ini tidak ada di repository migration web (hanya di database
     * live), jadi kegagalannya ditangani sebagai pesan yang bisa dibaca pengguna,
     * bukan crash.
     */
    suspend fun ajukanPersetujuan(opnameId: String) {
        Postgrest.rpc("set_opname_pending", JsonObject().apply {
            addProperty("p_opname_id", opnameId)
        })
    }

    /** Finalisasi langsung — menulis `opname_selisih` ke ledger lewat trigger. */
    suspend fun finalisasi(opnameId: String) {
        Postgrest.rpc("finalize_opname", JsonObject().apply {
            addProperty("p_opname_id", opnameId)
        })
    }

    suspend fun setujui(opnameId: String, approvedBy: String) {
        Postgrest.rpc("approve_opname", JsonObject().apply {
            addProperty("p_opname_id", opnameId)
            addProperty("p_approved_by", approvedBy)
        })
    }

    suspend fun tolak(opnameId: String, rejectedBy: String, alasan: String) {
        Postgrest.rpc("reject_opname", JsonObject().apply {
            addProperty("p_opname_id", opnameId)
            addProperty("p_rejected_by", rejectedBy)
            addProperty("p_reason", alasan)
        })
    }

    /** Item yang sudah tersimpan pada satu opname, untuk melanjutkan draft. */
    suspend fun itemTersimpan(opnameId: String): Map<String, Double> = Postgrest.select(
        "opname_item",
        listOf(
            "select" to "bahan_baku_id,qty_fisik",
            "opname_id" to "eq.$opnameId",
        ),
    ).mapNotNull { el ->
        val o = el.asJsonObject
        val id = o.optString("bahan_baku_id") ?: return@mapNotNull null
        val qty = o.optDouble("qty_fisik") ?: return@mapNotNull null
        id to qty
    }.toMap()
}
