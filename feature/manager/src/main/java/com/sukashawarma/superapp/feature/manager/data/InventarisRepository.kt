package com.sukashawarma.superapp.feature.manager.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.sukashawarma.superapp.core.storage.StorageUtil
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optBoolean
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optInt
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.manager.domain.IsianItem
import com.sukashawarma.superapp.feature.manager.domain.ItemMaster
import com.sukashawarma.superapp.feature.manager.domain.KondisiAset
import com.sukashawarma.superapp.feature.manager.domain.ModeItem
import com.sukashawarma.superapp.feature.manager.domain.ZONA_JAKARTA
import com.sukashawarma.superapp.feature.manager.domain.nilaiPenilaian
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.util.UUID

/** Data acuan yang sama dipakai daftar outlet dan form pengisian. */
data class ReferensiInventaris(
    val outlets: List<OutletPilihan>,
    val master: List<ItemMaster>,
    /** Outlet yang sudah punya laporan tersimpan. */
    val sudahTersimpan: Set<String>,
)

/** Laporan inventaris terkini sebuah outlet, dalam bentuk siap diisi ulang. */
data class LaporanTerkini(
    val id: String,
    val tanggal: String,
    val catatan: String?,
    val diperbaruiPada: String,
    /** master_item_id -> isian tersimpan. */
    val isian: Map<String, IsianItem>,
)

/**
 * Pembacaan dan pengiriman inventaris aset — cermin `apps/inventori` web.
 *
 * Seluruh jalurnya memakai JWT pengguna, sama seperti web: route submit di sana
 * pun memakai klien sesi (`createSupabaseServerClient`), bukan service-role.
 * Yang menjaga kewenangan adalah RPC `accessible_outlet_ids()` dan
 * `submit_inventaris`, keduanya sudah `GRANT EXECUTE ... TO authenticated`.
 */
object InventarisRepository {

    private const val BUCKET = "inventaris-foto"

    /** Outlet yang boleh diisi, menurut database — bukan menurut aplikasi. */
    private suspend fun outletTerakses(): List<String> {
        val hasil = Postgrest.rpc("accessible_outlet_ids")
        if (!hasil.isJsonArray) return emptyList()
        return hasil.asJsonArray.mapNotNull { elemen ->
            when {
                elemen.isJsonPrimitive -> elemen.asString
                // PostgREST membungkus fungsi set-returning sebagai objek berkolom
                // satu; web menangani kedua bentuk itu dan native ikut.
                elemen.isJsonObject -> elemen.asJsonObject.optString("accessible_outlet_ids")
                else -> null
            }
        }
    }

    suspend fun referensi(): ReferensiInventaris = coroutineScope {
        val ids = outletTerakses()
        if (ids.isEmpty()) return@coroutineScope ReferensiInventaris(emptyList(), emptyList(), emptySet())
        val daftarId = ids.joinToString(",")

        val outlets = async {
            Postgrest.select(
                "outlets",
                listOf("select" to "id,name", "id" to "in.($daftarId)", "order" to "name"),
            ).mapNotNull { elemen ->
                val baris = elemen.asJsonObject
                val id = baris.optString("id") ?: return@mapNotNull null
                OutletPilihan(id, baris.optString("name").orEmpty())
            }
        }
        val master = async {
            Postgrest.select(
                "inventaris_master_items",
                listOf(
                    "select" to "id,section,subsection,name,mode,target_qty,target_min,target_max,unit,sort_order",
                    "is_active" to "eq.true",
                    "order" to "sort_order",
                ),
            ).mapNotNull { elemen -> petakanMaster(elemen.asJsonObject) }
        }
        val tersimpan = async {
            Postgrest.select(
                "inventaris_submissions",
                listOf("select" to "outlet_id", "outlet_id" to "in.($daftarId)"),
            ).mapNotNull { it.asJsonObject.optString("outlet_id") }.toSet()
        }

        ReferensiInventaris(outlets.await(), master.await(), tersimpan.await())
    }

    private fun petakanMaster(baris: JsonObject): ItemMaster? {
        val id = baris.optString("id") ?: return null
        return ItemMaster(
            id = id,
            section = baris.optString("section").orEmpty(),
            subsection = baris.optString("subsection").orEmpty(),
            nama = baris.optString("name").orEmpty(),
            mode = ModeItem.dari(baris.optString("mode")),
            targetQty = baris.optDouble("target_qty"),
            targetMin = baris.optDouble("target_min"),
            targetMax = baris.optDouble("target_max"),
            satuan = baris.optString("unit"),
            urutan = baris.optInt("sort_order") ?: 0,
        )
    }

    /**
     * Laporan terakhir sebuah outlet, kalau ada.
     *
     * Diurut `updated_at` lalu `created_at` menurun, sama dengan
     * `getCurrentSubmission` web — dua laporan pada hari yang sama bukan hal yang
     * mustahil, dan yang berlaku adalah yang terakhir disunting.
     */
    suspend fun laporanTerkini(outletId: String): LaporanTerkini? {
        val laporan = Postgrest.select(
            "inventaris_submissions",
            listOf(
                "select" to "id,tanggal,notes,updated_at",
                "outlet_id" to "eq.$outletId",
                "order" to "updated_at.desc",
                "limit" to "1",
            ),
        ).firstOrNull()?.asJsonObject ?: return null

        val id = laporan.optString("id") ?: return null
        val baris = Postgrest.select(
            "inventaris_submission_items",
            listOf(
                "select" to (
                    "master_item_id,observed_qty,is_present,kondisi,catatan," +
                        "purchase_date,purchase_price,depreciation_rate,brand,photo_path"
                    ),
                "submission_id" to "eq.$id",
            ),
        ).map { it.asJsonObject }

        return LaporanTerkini(
            id = id,
            tanggal = laporan.optString("tanggal").orEmpty(),
            catatan = laporan.optString("notes"),
            diperbaruiPada = laporan.optString("updated_at").orEmpty(),
            isian = baris.mapNotNull { item ->
                val masterId = item.optString("master_item_id") ?: return@mapNotNull null
                masterId to IsianItem(
                    jumlah = item.optDouble("observed_qty")?.let { angkaRapi(it) } ?: "",
                    ada = item.optBoolean("is_present", default = true),
                    kondisi = KondisiAset.dari(item.optString("kondisi")),
                    catatan = item.optString("catatan").orEmpty(),
                    tanggalBeli = item.optString("purchase_date").orEmpty(),
                    harga = item.optDouble("purchase_price")?.let { angkaRapi(it) } ?: "",
                    depresiasi = item.optDouble("depreciation_rate")?.let { angkaRapi(it) } ?: "",
                    merek = item.optString("brand").orEmpty(),
                    fotoPath = item.optString("photo_path"),
                )
            }.toMap(),
        )
    }

    private fun angkaRapi(nilai: Double): String =
        if (nilai % 1.0 == 0.0) nilai.toLong().toString() else nilai.toString()

    /**
     * Mengunggah satu foto bukti dan mengembalikan path-nya.
     *
     * Path WAJIB diawali id pengguna: policy `inventaris_photo_upload` menuntut
     * `(storage.foldername(name))[1] = auth.uid()`, dan `submit_inventaris` menolak
     * `photo_path` yang tidak diawali id itu kecuali path tersebut memang sudah
     * terpakai laporan sebelumnya.
     */
    suspend fun unggahFoto(outletId: String, itemId: String, jpeg: ByteArray): String {
        val userId = AppSession.staff.value?.id
            ?: throw IllegalStateException("Sesi tidak valid, silakan login ulang.")
        val path = "$userId/drafts/$outletId/$itemId-${UUID.randomUUID()}.jpg"
        StorageUtil.uploadJpeg(BUCKET, path, jpeg)
        return path
    }

    /** URL bertanda tangan untuk menampilkan foto yang sudah tersimpan. */
    suspend fun urlFoto(path: String): String? = SidakRepository.urlFoto(path)

    /**
     * Mengirim seluruh laporan lewat RPC `submit_inventaris`.
     *
     * `status_penilaian` dihitung di sini dengan [nilaiPenilaian] — rumus yang sama
     * dipakai layar untuk menampilkan hasilnya, jadi yang terlihat dan yang
     * tersimpan tidak bisa berselisih.
     */
    suspend fun kirim(
        outletId: String,
        catatan: String?,
        master: List<ItemMaster>,
        isian: Map<String, IsianItem>,
        laporanLamaId: String?,
    ) {
        val items = JsonArray()
        master.forEach { item ->
            val isi = isian[item.id] ?: IsianItem()
            items.add(
                JsonObject().apply {
                    addProperty("master_item_id", item.id)
                    // Item keberadaan TIDAK boleh membawa angka, dan sebaliknya —
                    // itu yang dituntut kolom `observed_qty`/`is_present` di RPC.
                    if (item.mode == ModeItem.KEBERADAAN) {
                        add("observed_qty", com.google.gson.JsonNull.INSTANCE)
                        addProperty("is_present", isi.ada)
                    } else {
                        addProperty("observed_qty", isi.jumlahAngka)
                        add("is_present", com.google.gson.JsonNull.INSTANCE)
                    }
                    addProperty("kondisi", isi.kondisi.nilai)
                    addProperty("status_penilaian", nilaiPenilaian(item, isi))
                    addProperty("catatan", isi.catatan.trim().takeIf { it.isNotEmpty() })
                    addProperty("purchase_date", isi.tanggalBeli.trim().takeIf { it.isNotEmpty() })
                    addProperty("purchase_price", isi.harga.trim().toDoubleOrNull())
                    addProperty("depreciation_rate", isi.depresiasi.trim().toDoubleOrNull())
                    addProperty("brand", isi.merek.trim().takeIf { it.isNotEmpty() })
                    addProperty("photo_path", isi.fotoPath)
                }
            )
        }

        Postgrest.rpc(
            "submit_inventaris",
            JsonObject().apply {
                addProperty("p_submission_id", laporanLamaId ?: UUID.randomUUID().toString())
                addProperty("p_outlet_id", outletId)
                addProperty("p_tanggal", LocalDate.now(ZONA_JAKARTA).toString())
                add("p_area_scores", JsonObject())
                addProperty("p_notes", catatan?.trim()?.takeIf { it.isNotEmpty() })
                add("p_items", items)
            },
        )
    }
}
