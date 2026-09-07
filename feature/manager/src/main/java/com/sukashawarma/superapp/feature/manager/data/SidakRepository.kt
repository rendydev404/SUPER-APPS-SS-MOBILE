package com.sukashawarma.superapp.feature.manager.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.SupabaseClient
import com.sukashawarma.superapp.data.remote.optBoolean
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optJsonArray
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.feature.manager.domain.HasilSidak
import com.sukashawarma.superapp.feature.manager.domain.ItemSidak
import com.sukashawarma.superapp.feature.manager.domain.LaporanSidak
import com.sukashawarma.superapp.feature.manager.domain.KeputusanSidak
import com.sukashawarma.superapp.feature.manager.domain.OutletSidak
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class DataSidak(
    val outlets: List<OutletSidak>,
    /** outlet_id -> laporan terbaru outlet itu. */
    val laporan: Map<String, LaporanSidak>,
    /** submission_id -> hasil sidak yang sudah tersimpan. */
    val hasil: Map<String, HasilSidak>,
)

/**
 * Pembacaan sidak inventaris — padanan `lib/inventaris-sidak-server.ts` web.
 *
 * Seluruh tabelnya sudah discope RLS untuk area & regional manager:
 * `inventaris_submission_read` memakai `accessible_outlet_ids()`, dan
 * `inventaris_sidak_reviews_manager_read` bahkan memeriksa role secara eksplisit.
 * Penyimpanan hasil sidak TIDAK tersedia dari sini — lihat
 * `SIDAK_BISA_DISIMPAN_NATIVE`.
 */
object SidakRepository {

    private const val BUCKET_FOTO = "inventaris-foto"
    private val jsonMedia = "application/json".toMediaType()

    suspend fun muat(): DataSidak = coroutineScope {
        val outlets = async { outlets() }
        val master = async { masterItems() }
        val submissions = async { submissions() }

        val laporanMentah = submissions.await()
        // Hanya laporan TERBARU tiap outlet yang disidak. Baris dibaca urut
        // `updated_at` menurun, jadi kemunculan pertama sebuah outlet adalah yang terbaru.
        val terbaru = LinkedHashMap<String, JsonObject>()
        laporanMentah.forEach { baris ->
            val outletId = baris.optString("outlet_id") ?: return@forEach
            if (!terbaru.containsKey(outletId)) terbaru[outletId] = baris
        }
        val idLaporan = terbaru.values.mapNotNull { it.optString("id") }

        val items = async { itemsLaporan(idLaporan) }
        val hasil = async { hasilSidak(idLaporan) }

        val katalog = master.await()
        val perLaporan = items.await().groupBy { it.first }

        DataSidak(
            outlets = outlets.await(),
            laporan = terbaru.mapNotNull { (outletId, baris) ->
                val id = baris.optString("id") ?: return@mapNotNull null
                outletId to LaporanSidak(
                    id = id,
                    outletId = outletId,
                    tanggal = baris.optString("tanggal").orEmpty(),
                    diperbaruiPada = baris.optString("updated_at").orEmpty(),
                    catatan = baris.optString("notes"),
                    items = perLaporan[id].orEmpty().mapNotNull { (_, isi) -> gabungMaster(isi, katalog) },
                )
            }.toMap(),
            hasil = hasil.await(),
        )
    }

    /**
     * Outlet dibatasi cakupan pengguna, BUKAN diserahkan ke RLS.
     * `outlets_select_authenticated` berisi `USING (true)` — tanpa penyaring ini
     * area manager melihat seluruh cabang, termasuk yang bukan binaannya.
     */
    private suspend fun outlets(): List<OutletSidak> {
        val filter = CakupanOutletRepository.filterOutlet(CakupanOutletRepository.cakupan())
            ?: return emptyList()
        return Postgrest.select(
            "outlets",
            listOf("select" to "id,name,region", "is_active" to "eq.true", "order" to "name") + filter,
        ).mapNotNull { elemen ->
            val baris = elemen.asJsonObject
            val id = baris.optString("id") ?: return@mapNotNull null
            OutletSidak(id, baris.optString("name").orEmpty(), baris.optString("region"))
        }
    }

    private suspend fun submissions(): List<JsonObject> =
        Postgrest.select(
            "inventaris_submissions",
            listOf(
                "select" to "id,outlet_id,tanggal,notes,updated_at",
                "order" to "updated_at.desc",
            ),
        ).map { it.asJsonObject }

    /** Katalog aset: id -> definisi target. Berlaku lintas outlet. */
    private suspend fun masterItems(): Map<String, JsonObject> =
        Postgrest.select(
            "inventaris_master_items",
            listOf(
                "select" to "id,name,section,subsection,mode,target_qty,target_min,target_max,unit",
                "is_active" to "eq.true",
                "order" to "sort_order",
            ),
        ).mapNotNull { elemen ->
            val baris = elemen.asJsonObject
            val id = baris.optString("id") ?: return@mapNotNull null
            id to baris
        }.toMap()

    private suspend fun itemsLaporan(idLaporan: List<String>): List<Pair<String, JsonObject>> {
        if (idLaporan.isEmpty()) return emptyList()
        return idLaporan.chunked(100).flatMap { potongan ->
            Postgrest.select(
                "inventaris_submission_items",
                listOf(
                    "select" to (
                        "id,submission_id,master_item_id,observed_qty,is_present," +
                            "kondisi,status_penilaian,catatan,photo_path"
                        ),
                    "submission_id" to "in.(${potongan.joinToString(",")})",
                ),
            ).mapNotNull { elemen ->
                val baris = elemen.asJsonObject
                val submissionId = baris.optString("submission_id") ?: return@mapNotNull null
                submissionId to baris
            }
        }
    }

    /**
     * Menggabungkan baris laporan dengan definisi asetnya.
     *
     * Item yang master-nya sudah dinonaktifkan dibuang, sama seperti web — tanpa
     * definisi target, barisnya tidak punya arti yang bisa ditampilkan.
     */
    private fun gabungMaster(baris: JsonObject, katalog: Map<String, JsonObject>): ItemSidak? {
        val id = baris.optString("id") ?: return null
        val masterId = baris.optString("master_item_id") ?: return null
        val master = katalog[masterId] ?: return null
        return ItemSidak(
            id = id,
            nama = master.optString("name").orEmpty(),
            section = master.optString("section").orEmpty(),
            subsection = master.optString("subsection").orEmpty(),
            mode = master.optString("mode").orEmpty(),
            targetQty = master.optDouble("target_qty"),
            targetMin = master.optDouble("target_min"),
            targetMax = master.optDouble("target_max"),
            satuan = master.optString("unit"),
            observedQty = baris.optDouble("observed_qty"),
            ada = baris.get("is_present")?.takeIf { !it.isJsonNull }?.let { baris.optBoolean("is_present") },
            kondisi = baris.optString("kondisi").orEmpty(),
            catatan = baris.optString("catatan")?.takeIf { it.isNotBlank() },
            fotoPath = baris.optString("photo_path").orEmpty(),
        )
    }

    private suspend fun hasilSidak(idLaporan: List<String>): Map<String, HasilSidak> {
        if (idLaporan.isEmpty()) return emptyMap()
        return idLaporan.chunked(100).flatMap { potongan ->
            Postgrest.select(
                "inventaris_sidak_reviews",
                listOf(
                    "select" to (
                        "id,submission_id,status,note," +
                            "inventaris_sidak_review_items(submission_item_id,status)"
                        ),
                    "submission_id" to "in.(${potongan.joinToString(",")})",
                ),
            ).mapNotNull { elemen ->
                val baris = elemen.asJsonObject
                val id = baris.optString("id") ?: return@mapNotNull null
                val laporanId = baris.optString("submission_id") ?: return@mapNotNull null
                laporanId to HasilSidak(
                    id = id,
                    laporanId = laporanId,
                    status = baris.optString("status").orEmpty(),
                    catatan = baris.optString("note")?.takeIf { it.isNotBlank() },
                    penilaian = baris.optJsonArray("inventaris_sidak_review_items")
                        ?.mapNotNull { item ->
                            val isi = item.asJsonObject
                            val itemId = isi.optString("submission_item_id") ?: return@mapNotNull null
                            itemId to isi.optString("status").orEmpty()
                        }?.toMap().orEmpty(),
                )
            }
        }.toMap()
    }

    /**
     * Menyimpan hasil sidak lewat RPC `submit_sidak_inventaris`.
     *
     * Bukan INSERT/UPDATE langsung: `inventaris_sidak_reviews` hanya punya policy
     * SELECT, jadi tulisan dari JWT pengguna pasti ditolak. RPC-nya SECURITY
     * DEFINER dan memeriksa sendiri role, scope outlet, serta kelengkapan item —
     * pola yang sama dipakai `submit_inventaris` dan
     * `area_manager_process_petty_cash`.
     *
     * RPC ini datang dari migrasi `20300131000000_submit_sidak_inventaris_rpc`.
     * Selama migrasi itu belum dijalankan, panggilan ini gagal dengan 404 dan
     * layar menjelaskannya lewat [PesanSidak.RPC_BELUM_ADA].
     */
    suspend fun simpanSidak(
        laporanId: String,
        catatan: String?,
        keputusan: Map<String, KeputusanSidak>,
    ) {
        val items = JsonArray().apply {
            keputusan.forEach { (itemId, nilai) ->
                add(
                    JsonObject().apply {
                        addProperty("submission_item_id", itemId)
                        addProperty("status", nilai.nilai)
                    }
                )
            }
        }
        Postgrest.rpc(
            "submit_sidak_inventaris",
            JsonObject().apply {
                addProperty("p_submission_id", laporanId)
                addProperty("p_note", catatan?.trim()?.takeIf { it.isNotEmpty() })
                add("p_items", items)
            },
        )
    }

    /**
     * URL bertanda tangan untuk satu foto bukti, berlaku satu jam.
     *
     * Bucket `inventaris-foto` bersifat privat, tapi policy `storage.objects`-nya
     * mengizinkan SELECT bagi `authenticated`, jadi tanda tangannya bisa diminta
     * dengan JWT pengguna. Web menempuh jalan memutar lewat route API hanya karena
     * browsernya tidak memegang service-role key — di sini tidak perlu.
     */
    suspend fun urlFoto(path: String): String? {
        if (path.isBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${SupabaseClient.BASE_URL}storage/v1/object/sign/$BUCKET_FOTO/$path")
                    .post(JsonObject().apply { addProperty("expiresIn", 3600) }.toString().toRequestBody(jsonMedia))
                    .build()
                SupabaseClient.okHttpClient.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    val body = resp.body?.string().orEmpty()
                    val signed = JsonParser.parseString(body).asJsonObject.optString("signedURL")
                        ?: return@use null
                    // Balasannya berupa path relatif terhadap /storage/v1.
                    "${SupabaseClient.BASE_URL}storage/v1${signed.removePrefix("/storage/v1")}"
                }
            } catch (e: Exception) {
                android.util.Log.e("SidakRepository", "urlFoto() gagal", e)
                null
            }
        }
    }
}
