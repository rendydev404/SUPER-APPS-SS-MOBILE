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
import com.sukashawarma.superapp.feature.stok.data.model.LedgerEntry
import com.sukashawarma.superapp.feature.stok.data.model.MonitoringRow
import com.sukashawarma.superapp.feature.stok.data.model.OutletRingkas
import com.sukashawarma.superapp.feature.stok.data.model.ResepItemRingkas
import com.sukashawarma.superapp.feature.stok.data.model.ResepRingkas
import com.sukashawarma.superapp.feature.stok.domain.UnitMeta
import com.sukashawarma.superapp.feature.stok.domain.UnitScale

/**
 * Akses baca ke domain stok. Lapisan ini hanya tahu PostgREST dan JSON —
 * tidak ada aturan bisnis di sini; normalisasi satuan dan penentuan status
 * tinggal di `domain/`.
 *
 * Modul Stok Fase 1 murni membaca. Tidak ada satu pun operasi tulis di kelas ini,
 * dan itu disengaja: database ini dipakai bersama aplikasi Stok versi web.
 *
 * Cache disimpan di memori dengan TTL pendek dan hilang saat proses berakhir.
 * Sengaja tidak ada cache disk: saldo berubah tiap transaksi POS, dan angka
 * kemarin yang tersimpan di disk lebih berbahaya daripada berguna.
 */
object StokRepository {

    private const val TTL_MS = 60_000L
    const val PAGE_SIZE = 200
    private const val MAX_PAGE = 20
    private const val LEDGER_LIMIT = 30

    private const val MONITORING_SELECT =
        "outlet_id,outlet_name,bahan_baku_id,item_name,current_qty," +
            "threshold,status,is_flagged,saldo_is_gram,last_opname_date,kategori,satuan," +
            "bahan_baku(satuan,satuan_tengah,satuan_kecil,faktor_tengah,faktor_tampilan)"

    private val cache = HashMap<String, Pair<Long, Any>>()

    private suspend fun <T : Any> cached(key: String, load: suspend () -> T): T {
        val hit = cache[key]
        if (hit != null && System.currentTimeMillis() - hit.first < TTL_MS) {
            @Suppress("UNCHECKED_CAST")
            return hit.second as T
        }
        val value = load()
        cache[key] = System.currentTimeMillis() to value
        return value
    }

    /** Buang seluruh cache — dipanggil saat pengguna menarik untuk menyegarkan. */
    fun invalidate() = cache.clear()

    // ------------------------------------------------------------------ outlet

    /**
     * Outlet yang boleh dilihat pengguna, menurut `accessible_outlet_ids()`.
     *
     * Otoritasnya sengaja diserahkan ke fungsi database, bukan matriks role di
     * dalam aplikasi: matriksnya terbukti berubah (spv menjadi regional_manager)
     * dan dokumen analisis sudah tidak sinkron dengannya. Dengan cara ini native
     * ikut berubah tanpa perlu rilis APK baru.
     *
     * Daftar kosong berarti benar-benar kosong. Jangan pernah ditafsirkan sebagai
     * "berarti semua outlet".
     */
    suspend fun accessibleOutlets(): List<OutletRingkas> = cached("outlets") {
        val ids = Postgrest.rpc("accessible_outlet_ids").let { el ->
            when {
                el.isJsonArray -> el.asJsonArray.mapNotNull { item ->
                    when {
                        item.isJsonPrimitive -> item.asString
                        item.isJsonObject -> item.asJsonObject.optString("accessible_outlet_ids")
                        else -> null
                    }
                }
                else -> emptyList()
            }
        }.distinct()

        if (ids.isEmpty()) return@cached emptyList()

        Postgrest.select(
            "outlets",
            listOf(
                "select" to "id,name,marquee_warning_threshold",
                "id" to "in.(${ids.joinToString(",")})",
                "is_active" to "eq.true",
                "order" to "name.asc",
            ),
        ).mapNotNull { el ->
            val o = el.asJsonObject
            val id = o.optString("id") ?: return@mapNotNull null
            OutletRingkas(
                id = id,
                name = o.optString("name") ?: "Outlet",
                marqueeWarningThreshold = o.optInt("marquee_warning_threshold")
                    ?: UnitScale.DEFAULT_MARQUEE_WARNING,
            )
        }
    }

    // -------------------------------------------------------------- monitoring

    /**
     * Seluruh bahan satu outlet, diambil bertahap sampai habis.
     *
     * Sengaja memuat penuh, bukan per halaman: hitungan Kritis/Selisih/Aman dan
     * pengelompokan per kategori harus dihitung atas seluruh isi outlet. Kalau hanya
     * sehalaman, angka ringkasan akan berbohong. Satu outlet berisi puluhan bahan,
     * jadi beban sekali muat masih ringan, dan setelahnya pencarian serta pengurutan
     * berjalan di memori tanpa menyentuh jaringan lagi.
     *
     * Metadata satuan ikut ditarik lewat embed dalam query yang sama.
     */
    suspend fun monitoringOutlet(outletId: String): List<MonitoringRow> =
        cached("mon-outlet|$outletId") {
            buildList {
                var page = 0
                while (page < MAX_PAGE) {
                    val batch = Postgrest.select(
                        "monitoring_view_scoped",
                        listOf(
                            "select" to MONITORING_SELECT,
                            "outlet_id" to "eq.$outletId",
                            "order" to "item_name.asc",
                            "limit" to PAGE_SIZE.toString(),
                            "offset" to (page * PAGE_SIZE).toString(),
                        ),
                    ).mapNotNull { it.asJsonObject.toMonitoringRow() }
                    addAll(batch)
                    if (batch.size < PAGE_SIZE) break
                    page++
                }
            }
        }

    /** Seluruh baris lintas outlet untuk satu bahan — dasar saran transfer. */
    suspend fun monitoringLintasOutlet(bahanBakuId: String): List<MonitoringRow> =
        cached("mon-lintas|$bahanBakuId") {
            Postgrest.select(
                "monitoring_view_scoped",
                listOf(
                    "select" to MONITORING_SELECT,
                    "bahan_baku_id" to "eq.$bahanBakuId",
                    "order" to "outlet_name.asc",
                ),
            ).mapNotNull { it.asJsonObject.toMonitoringRow() }
        }

    /** Bahan berstatus tidak aman lintas outlet — kandidat penerima transfer. */
    suspend fun bahanTidakAmanLintasOutlet(limit: Int = 100): List<MonitoringRow> =
        cached("mon-kritis|$limit") {
            Postgrest.select(
                "monitoring_view_scoped",
                listOf(
                    "select" to MONITORING_SELECT,
                    "status" to "in.(below,warning)",
                    "order" to "item_name.asc",
                    "limit" to limit.toString(),
                ),
            ).mapNotNull { it.asJsonObject.toMonitoringRow() }
        }

    private fun JsonObject.toMonitoringRow(): MonitoringRow? {
        val outletId = optString("outlet_id") ?: return null
        val bahanId = optString("bahan_baku_id") ?: return null
        val bb = optJsonObject("bahan_baku")
        return MonitoringRow(
            outletId = outletId,
            outletName = optString("outlet_name") ?: "Outlet",
            bahanBakuId = bahanId,
            itemName = optString("item_name") ?: "(tanpa nama)",
            currentQty = optDouble("current_qty") ?: 0.0,
            threshold = optDouble("threshold"),
            statusView = optString("status"),
            isFlagged = optBoolean("is_flagged"),
            saldoIsGram = optBoolean("saldo_is_gram"),
            lastOpnameDate = optString("last_opname_date"),
            kategori = optString("kategori"),
            satuan = optString("satuan"),
            meta = UnitMeta(
                satuan = bb?.optString("satuan") ?: optString("satuan"),
                satuanTengah = bb?.optString("satuan_tengah"),
                satuanKecil = bb?.optString("satuan_kecil"),
                faktorTengah = bb?.optDouble("faktor_tengah"),
                faktorTampilan = bb?.optDouble("faktor_tampilan"),
            ),
        )
    }

    // ------------------------------------------------------------------ ledger

    /** Riwayat mutasi terakhir satu bahan di satu outlet. */
    suspend fun ledger(outletId: String, bahanBakuId: String): List<LedgerEntry> =
        cached("ledger|$outletId|$bahanBakuId") {
            Postgrest.select(
                "ledger_stok",
                listOf(
                    "select" to "id,tipe,qty,saldo_sebelum,saldo_sesudah,catatan,created_at",
                    "outlet_id" to "eq.$outletId",
                    "bahan_baku_id" to "eq.$bahanBakuId",
                    "order" to "created_at.desc",
                    "limit" to LEDGER_LIMIT.toString(),
                ),
            ).mapNotNull { el ->
                val o = el.asJsonObject
                LedgerEntry(
                    id = o.optString("id") ?: return@mapNotNull null,
                    tipe = o.optString("tipe") ?: "adjustment",
                    qty = o.optDouble("qty") ?: 0.0,
                    saldoSebelum = o.optDouble("saldo_sebelum"),
                    saldoSesudah = o.optDouble("saldo_sesudah"),
                    catatan = o.optString("catatan"),
                    createdAt = o.optString("created_at"),
                )
            }
        }

    // ------------------------------------------------------------------- resep

    /**
     * Resep yang berlaku di satu outlet: resep khusus outlet digabung dengan resep
     * global (`outlet_id IS NULL`). Pemilihan pemenang antara keduanya dilakukan di
     * `ProduksiEstimator`, bukan di sini — itu aturan bisnis.
     */
    suspend fun resep(outletId: String): List<ResepRingkas> = cached("resep|$outletId") {
        Postgrest.select(
            "resep",
            listOf(
                "select" to "id,nama,outlet_id," +
                    "resep_item(bahan_baku_id,qty_per_porsi,satuan," +
                    "bahan_baku(satuan,faktor_konversi))",
                "is_active" to "eq.true",
                "or" to "(outlet_id.eq.$outletId,outlet_id.is.null)",
                "order" to "nama.asc",
            ),
        ).mapNotNull { el ->
            val o = el.asJsonObject
            val id = o.optString("id") ?: return@mapNotNull null
            ResepRingkas(
                id = id,
                nama = o.optString("nama") ?: "(tanpa nama)",
                outletId = o.optString("outlet_id"),
                items = (o.optJsonArray("resep_item") ?: JsonArray())
                    .mapNotNull { item -> item.asJsonObject.toResepItem() },
            )
        }
    }

    private fun JsonObject.toResepItem(): ResepItemRingkas? {
        val bahanId = optString("bahan_baku_id") ?: return null
        val qty = optDouble("qty_per_porsi") ?: return null
        val bb = optJsonObject("bahan_baku")
        return ResepItemRingkas(
            bahanBakuId = bahanId,
            qtyPerPorsi = qty,
            satuanResep = optString("satuan"),
            satuanBesarBahan = bb?.optString("satuan"),
            faktorKonversi = bb?.optDouble("faktor_konversi"),
        )
    }
}
