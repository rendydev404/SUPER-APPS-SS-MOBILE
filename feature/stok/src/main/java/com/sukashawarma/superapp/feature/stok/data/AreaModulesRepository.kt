package com.sukashawarma.superapp.feature.stok.data

import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.*
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.stok.domain.*
import java.time.Instant

object AreaModuleAccess {
    fun allowed(role: Role?) = role in setOf(Role.AREA_MANAGER, Role.LEADER, Role.REGIONAL_MANAGER, Role.SPV)
    fun requireAccess() { check(AppSession.staff.value?.isActive == true && allowed(AppSession.staff.value?.role)) { "Akses modul Area Manager ditolak." } }
}

/**
 * Gerbang persetujuan waste — lebih luas daripada [AreaModuleAccess] karena web
 * mengizinkan kitchen/admin/owner/purchasing juga. Lihat [Approver.bolehApproveWaste].
 */
object WasteApprovalAccess {
    fun allowed(role: Role?) = Approver.bolehApproveWaste(role)
    fun requireAccess() { check(AppSession.staff.value?.isActive == true && allowed(AppSession.staff.value?.role)) { "Akses persetujuan waste ditolak." } }
}

/** Pagination avoids silently truncating catalogs and purchase history at the REST row limit. */
internal suspend fun allRows(table: String, params: List<Pair<String, String>>): List<JsonObject> {
    val result = mutableListOf<JsonObject>()
    while (true) {
        val page = Postgrest.select(table, params + listOf("offset" to result.size.toString(), "limit" to "500"))
        result.addAll(page.map { it.asJsonObject })
        if (page.size() < 500) return result
    }
}

data class PricePurchase(val id: String, val bahanId: String, val number: String, val date: String,
    val vendor: String, val price: Double, val qty: Double, val subtotal: Double)

data class MaterialPrice(val id: String, val name: String, val category: String, val unit: String,
    val master: Double?, val purchases: List<PricePurchase>) {
    val latest get() = purchases.firstOrNull()
    val previous get() = purchases.getOrNull(1)
    val effective get() = latest?.price ?: master
    val delta get() = latest?.let { a -> previous?.let { a.price - it.price } }
    val percent get() = previous?.price?.takeIf { it > 0 }?.let { delta!! / it }
    // Web's stable filter includes a first purchase, but excludes materials without PO.
    fun matchesStatus(status: String) = when (status) {
        "naik" -> (percent ?: 0.0) > 0
        "turun" -> (percent ?: 0.0) < 0
        "stabil" -> latest != null && (percent ?: 0.0) == 0.0
        else -> true
    }
}

object HargaBahanRepository {
    private suspend fun fetch(days: Int?, bahanId: String? = null): JsonObject {
        AreaModuleAccess.requireAccess()
        return Postgrest.rpc("native_stok_price_data", JsonObject().apply {
            if (days == null) add("p_days", com.google.gson.JsonNull.INSTANCE) else addProperty("p_days", days)
            bahanId?.let { addProperty("p_bahan_id", it) }
        }).asJsonObject
    }
    suspend fun purchases(bahanId: String): List<PricePurchase> = parsePurchases(fetch(null, bahanId))

    private fun parsePurchases(data: JsonObject): List<PricePurchase> =
        data.getAsJsonArray("purchases").map { element ->
            val row = element.asJsonObject
            val po = row.optJsonObject("purchase_order")!!
            val price = row.optDouble("harga_terima") ?: 0.0
            val qty = row.optDouble("qty_terima") ?: 0.0
            PricePurchase(row.optString("id").orEmpty(), row.optString("bahan_baku_id").orEmpty(),
                po.optString("nomor_po").orEmpty(), po.optString("tanggal_po").orEmpty(), po.optString("supplier_nama").orEmpty(),
                price, qty, row.optDouble("subtotal")?.takeIf { it != 0.0 } ?: (price * qty))
        }.sortedByDescending { it.date }
    suspend fun load(days: Int?): List<MaterialPrice> {
        val data = fetch(days)
        val materials = data.getAsJsonArray("materials").map { it.asJsonObject }
        val prices = data.getAsJsonArray("prices").map { it.asJsonObject }
            .associate { it.optString("bahan_baku_id") to (it.optDouble("harga_beli_display") ?: it.optDouble("harga_beli")) }
        val history = parsePurchases(data).groupBy { it.bahanId }
        return materials.map { row ->
            val id = row.optString("id").orEmpty()
            MaterialPrice(id, row.optString("nama").orEmpty(), row.optString("kategori")?.takeIf { it.isNotBlank() }
                ?: row.optString("kategori_core")?.takeIf { it.isNotBlank() } ?: "Lainnya",
                row.optString("satuan").orEmpty(), prices[id], history[id].orEmpty())
        }
    }
}

data class WasteReview(val id: String, val outletId: String, val bahanId: String, val name: String,
    val outlet: String, val reporter: String, val date: String?, val qty: Double, val reason: String,
    val photo: String?, val meta: UnitMeta, val balance: Double?) {
    val deficit get() = balance == null || qty > balance
    val quantityLabel: String get() {
        val parts = decomposeTriUnit(qty, false, meta.satuanTengah, meta.faktorTengah, meta.satuanKecil, meta.faktorTampilan)
        return listOf(parts.besar to meta.satuan, parts.tengah to meta.satuanTengah, parts.kecil to meta.satuanKecil)
            .filter { it.first != 0.0 && !it.second.isNullOrBlank() }.joinToString(" · ") { "${formatAngkaStok(it.first)} ${it.second}" }.ifBlank { "0 ${meta.satuan.orEmpty()}" }
    }
}

object WasteApprovalRepository {
    private const val KOLOM = "*,bahan_baku(nama,satuan,satuan_tengah,faktor_tengah,satuan_kecil,faktor_tampilan)," +
        "outlets(name),reported_by_staff:outlet_staff!reported_by(name)"

    suspend fun load(): List<WasteReview> {
        WasteApprovalAccess.requireAccess()
        val ids = StokRepository.accessibleOutlets().map { it.id }
        if (ids.isEmpty()) return emptyList()
        return withBalances(allRows("stok_waste_reports", listOf("select" to KOLOM,
            "status" to "eq.PENDING", "outlet_id" to "in.(${ids.joinToString(",")})", "order" to "created_at.desc,id.asc")))
    }

    /**
     * Satu laporan saja. Approve wajib memeriksa saldo terkini, tetapi memuat ulang
     * seluruh daftar untuk itu membuat satu ketukan tombol menunggu tiga kali kerja
     * penuh — cukup baris yang sedang diputuskan yang perlu segar.
     */
    suspend fun reload(report: WasteReview): WasteReview? {
        WasteApprovalAccess.requireAccess()
        val rows = Postgrest.select("stok_waste_reports", listOf("select" to KOLOM,
            "id" to "eq.${report.id}", "status" to "eq.PENDING", "limit" to "1")).map { it.asJsonObject }
        return withBalances(rows).firstOrNull()
    }

    /**
     * Saldo diambil hanya untuk pasangan outlet×bahan yang benar-benar muncul di
     * daftar, dalam satu permintaan. Versi sebelumnya menarik seluruh `stok_balance`
     * per outlet secara halaman-per-halaman — ribuan baris untuk memakai belasan,
     * dan itulah yang memicu statement timeout 57014 di server.
     */
    private suspend fun withBalances(reports: List<JsonObject>): List<WasteReview> {
        if (reports.isEmpty()) return emptyList()
        val outlets = reports.mapNotNull { it.optString("outlet_id") }.distinct()
        val bahan = reports.mapNotNull { it.optString("bahan_baku_id") }.distinct()
        val balances = allRows("stok_balance", listOf("select" to "outlet_id,bahan_baku_id,saldo,saldo_is_gram",
            "outlet_id" to "in.(${outlets.joinToString(",")})", "bahan_baku_id" to "in.(${bahan.joinToString(",")})",
            "order" to "bahan_baku_id.asc")).associateBy { it.optString("outlet_id").orEmpty() to it.optString("bahan_baku_id").orEmpty() }
        return reports.map { row ->
            val bahanRow = row.optJsonObject("bahan_baku") ?: JsonObject()
            val meta = UnitMeta(bahanRow.optString("satuan"), bahanRow.optString("satuan_tengah"), bahanRow.optString("satuan_kecil"), bahanRow.optDouble("faktor_tengah"), bahanRow.optDouble("faktor_tampilan"))
            val outlet = row.optString("outlet_id").orEmpty()
            val bahanId = row.optString("bahan_baku_id").orEmpty()
            val bal = balances[outlet to bahanId]
            val raw = bal?.optDouble("saldo") ?: 0.0
            val balance = if (bal?.optBoolean("saldo_is_gram") == true) UnitScale.besarFromSmallest(raw, meta) else raw
            WasteReview(row.optString("id").orEmpty(), outlet, bahanId, bahanRow.optString("nama").orEmpty(),
                row.optJsonObject("outlets")?.optString("name").orEmpty(), row.optJsonObject("reported_by_staff")?.optString("name").orEmpty(),
                row.optString("created_at"), row.optDouble("qty") ?: 0.0, row.optString("reason").orEmpty(), row.optString("photo_url"), meta, balance)
        }
    }

    suspend fun decide(report: WasteReview, rejection: String? = null) {
        WasteApprovalAccess.requireAccess()
        require(rejection == null || rejection.isNotBlank()) { "Alasan penolakan wajib diisi." }
        check(StokRepository.accessibleOutlets().any { it.id == report.outletId }) { "Outlet di luar akses Anda." }
        val result = Postgrest.update("stok_waste_reports", listOf("id" to "eq.${report.id}", "outlet_id" to "eq.${report.outletId}", "status" to "eq.PENDING"), JsonObject().apply {
            addProperty("status", if (rejection == null) "APPROVED" else "REJECTED")
            addProperty("approved_by", AppSession.staff.value!!.id)
            addProperty("updated_at", Instant.now().toString())
            rejection?.let { addProperty("rejection_reason", it.trim()) }
        })
        check(result.size() == 1) { "Laporan sudah diproses atau akses berubah. Muat ulang daftar." }
        // Database trigger owns stock/ledger deduction; never deduct a second time here.
        StokRepository.invalidate()
    }
}
