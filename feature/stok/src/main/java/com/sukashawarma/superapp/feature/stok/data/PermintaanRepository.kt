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
import com.sukashawarma.superapp.feature.stok.data.model.BahanBaku
import com.sukashawarma.superapp.feature.stok.data.model.BudgetStatus
import com.sukashawarma.superapp.feature.stok.data.model.CrosscheckSaldo
import com.sukashawarma.superapp.feature.stok.data.model.EstimasiKeranjang
import com.sukashawarma.superapp.feature.stok.data.model.Permintaan
import com.sukashawarma.superapp.feature.stok.data.model.PermintaanItem
import com.sukashawarma.superapp.feature.stok.data.model.SaranPermintaan
import com.sukashawarma.superapp.feature.stok.data.model.StatusPermintaan
import com.sukashawarma.superapp.feature.stok.data.model.TargetJual

/**
 * Permintaan bahan — cermin `app/actions/permintaan.ts` dan `hooks/usePermintaan.ts`.
 *
 * Web membaca lewat service-role; native memakai JWT pengguna dan bergantung pada RLS
 * `select_permintaan_bahan_accessible_outlets`.
 *
 * Catatan lapangan (2026-09-04): policy SELECT itu ternyata TIDAK ADA di remote walau
 * migration 20260615000200 mendefinisikannya — akibatnya daftar selalu kosong padahal
 * penulisan sukses. Tak pernah ketahuan dari web karena web tak pernah membaca tabel ini
 * dengan sesi user. Dipulihkan oleh migration 20300126000000 di repo web. Kalau layar
 * riwayat kembali kosong sementara pengiriman sukses, curigai policy ini lebih dulu.
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
        // `target_metadata` diisi array {nama, qty, harga_jual} bila permintaan lahir
        // dari target penjualan; bentuk lain diabaikan diam-diam seperti web.
        val target = (optJsonArray("target_metadata") ?: JsonArray()).mapNotNull { el ->
            if (!el.isJsonObject) return@mapNotNull null
            val o = el.asJsonObject
            TargetJual(
                resepId = o.optString("id"),
                nama = o.optString("nama") ?: return@mapNotNull null,
                qty = o.optDouble("qty") ?: 0.0,
                hargaJual = o.optDouble("harga_jual") ?: 0.0,
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
            catatanKitchen = optString("catatan_kitchen"),
            suratJalanId = optString("surat_jalan_id"),
            targetJual = target,
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

    // ---------------------------------------------------------------- katalog

    private var katalogCache: Pair<Long, List<BahanBaku>>? = null
    private const val KATALOG_TTL_MS = 5 * 60_000L

    /**
     * Master bahan baku aktif untuk katalog form — cermin `useBahanBaku` web
     * (staleTime 5 menit). RLS `bahan_baku` mengizinkan baca untuk authenticated.
     */
    suspend fun bahanBaku(): List<BahanBaku> {
        katalogCache?.let { (saat, data) ->
            if (System.currentTimeMillis() - saat < KATALOG_TTL_MS) return data
        }
        val hasil = Postgrest.select(
            "bahan_baku",
            listOf(
                "select" to "id,nama,kategori,satuan,satuan_tengah,satuan_kecil," +
                    "faktor_tengah,faktor_tampilan,satuan_distribusi",
                "is_active" to "eq.true",
                "order" to "nama.asc",
            ),
        ).mapNotNull { el ->
            val o = el.asJsonObject
            BahanBaku(
                id = o.optString("id") ?: return@mapNotNull null,
                nama = o.optString("nama") ?: "(tanpa nama)",
                kategori = o.optString("kategori"),
                satuan = o.optString("satuan"),
                satuanTengah = o.optString("satuan_tengah"),
                satuanKecil = o.optString("satuan_kecil"),
                faktorTengah = o.optDouble("faktor_tengah"),
                faktorTampilan = o.optDouble("faktor_tampilan"),
                satuanDistribusi = o.optString("satuan_distribusi"),
            )
        }
        katalogCache = System.currentTimeMillis() to hasil
        return hasil
    }

    /**
     * Saldo bahan-bahan tertentu pada beberapa outlet sekaligus, untuk crosscheck
     * "Stok Outlet | Stok Gudang" di layar persetujuan — padanan `fetchCrosscheckStok`
     * web. Web membaca `stok_balance` lewat service-role; native memakai
     * `monitoring_view_crew` (SECURITY DEFINER) yang menyajikan saldo yang sama
     * tanpa perlu melewati RLS `stok_balance`.
     *
     * Hasil: outletId -> (bahanBakuId -> saldo).
     */
    suspend fun crosscheck(
        outletIds: List<String>,
        bahanBakuIds: List<String>,
    ): Map<String, Map<String, CrosscheckSaldo>> {
        if (outletIds.isEmpty() || bahanBakuIds.isEmpty()) return emptyMap()
        val hasil = HashMap<String, MutableMap<String, CrosscheckSaldo>>()
        Postgrest.select(
            "monitoring_view_crew",
            listOf(
                "select" to "outlet_id,bahan_baku_id,current_qty,saldo_is_gram",
                "outlet_id" to "in.(${outletIds.joinToString(",")})",
                "bahan_baku_id" to "in.(${bahanBakuIds.joinToString(",")})",
            ),
        ).forEach { el ->
            val o = el.asJsonObject
            val outletId = o.optString("outlet_id") ?: return@forEach
            val bahanId = o.optString("bahan_baku_id") ?: return@forEach
            hasil.getOrPut(outletId) { HashMap() }[bahanId] = CrosscheckSaldo(
                currentQty = o.optDouble("current_qty") ?: 0.0,
                saldoIsGram = o.optBoolean("saldo_is_gram"),
            )
        }
        return hasil
    }

    // ------------------------------------------------------------ nilai & budget
    //
    // Web menghitung keduanya di Server Action ber-service-role (`estimateCartValue`,
    // `getOutletBudgetStatus` di app/actions/budget.ts). Native memakai RPC pembungkus
    // ber-scope dari plan/permintaan-estimasi-budget-scoped-rpcs.sql yang memasang pagar
    // yang sama (staff aktif / outlet accessible) lalu mendelegasikan ke fungsi aslinya.
    // Harga beli per bahan tetap tidak pernah sampai ke klien — hanya agregatnya.
    //
    // Top-up plafon (`requestBudgetTopupAction` web) sengaja TIDAK diport: tabel
    // `outlet_budget_topup_requests` dan RPC `request_budget_topup_svc` tidak ada di
    // database produksi (migration 20260820110001 tak pernah ter-apply), sehingga
    // tombol top-up di web pun error di sana. Lihat catatan di file SQL tersebut.

    /**
     * Estimasi Rupiah untuk item pada satuan distribusi — cermin `estimateCartValue`.
     * `items`: bahanBakuId -> qty distribusi.
     */
    suspend fun estimasiNilai(items: List<Pair<String, Double>>): EstimasiKeranjang {
        if (items.isEmpty()) return EstimasiKeranjang()
        val arr = JsonArray()
        items.forEach { (id, qty) ->
            arr.add(JsonObject().apply {
                addProperty("bahan_baku_id", id)
                addProperty("qty", qty)
            })
        }
        val hasil = Postgrest.rpc("estimasi_nilai_keranjang", JsonObject().apply { add("p_items", arr) })
        if (!hasil.isJsonObject) return EstimasiKeranjang()
        val o = hasil.asJsonObject
        val kategori = o.optJsonObject("kategori_nilai")?.entrySet()
            ?.mapNotNull { (k, v) -> if (v.isJsonPrimitive) k to v.asDouble else null }
            ?.toMap().orEmpty()
        return EstimasiKeranjang(
            totalNilai = o.optDouble("total_nilai") ?: 0.0,
            itemTanpaHarga = (o.optJsonArray("item_tanpa_harga") ?: JsonArray())
                .mapNotNull { if (it.isJsonPrimitive) it.asString else null },
            kategoriNilai = kategori,
        )
    }

    /** Status plafon satu outlet — cermin `getOutletBudgetStatus`. */
    suspend fun budgetStatus(outletId: String): BudgetStatus {
        val hasil = Postgrest.rpc("get_outlet_budget_status_scoped", JsonObject().apply {
            addProperty("p_outlet_id", outletId)
        })
        val row = when {
            hasil.isJsonArray && hasil.asJsonArray.size() > 0 -> hasil.asJsonArray[0].asJsonObject
            hasil.isJsonObject -> hasil.asJsonObject
            else -> JsonObject()
        }
        return BudgetStatus(
            outletId = outletId,
            nominal = row.optDouble("nominal") ?: 0.0,
            periodType = row.optString("period_type"),
            periodStart = row.optString("period_start"),
            periodEnd = row.optString("period_end"),
            terpakai = row.optDouble("terpakai") ?: 0.0,
            sisa = row.optDouble("sisa") ?: 0.0,
            hasConfig = row.optBoolean("has_config"),
            customDays = row.optInt("custom_days"),
        )
    }

    /**
     * Kebutuhan bahan (HPP penggunaan) untuk permintaan bertarget penjualan —
     * RPC `calculate_bahan_baku_request`, sama dengan `calculateBahanBakuRequest` web.
     * Hasil: bahanBakuId -> kebutuhan.
     */
    suspend fun kebutuhanTarget(
        outletId: String,
        targets: List<Pair<String, Double>>,
    ): Map<String, Double> {
        if (targets.isEmpty()) return emptyMap()
        val arr = JsonArray()
        targets.forEach { (resepId, qty) ->
            arr.add(JsonObject().apply {
                addProperty("resep_id", resepId)
                addProperty("qty_target", qty)
            })
        }
        val hasil = Postgrest.rpc("calculate_bahan_baku_request", JsonObject().apply {
            addProperty("p_outlet_id", outletId)
            add("p_targets", arr)
        })
        if (!hasil.isJsonArray) return emptyMap()
        return hasil.asJsonArray.mapNotNull { el ->
            val o = el.asJsonObject
            val id = o.optString("bahan_baku_id") ?: return@mapNotNull null
            id to (o.optDouble("kebutuhan") ?: return@mapNotNull null)
        }.toMap()
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
