package com.sukashawarma.superapp.feature.manager.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sukashawarma.superapp.core.storage.StorageUtil
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.SupabaseClient
import com.sukashawarma.superapp.data.remote.optInt
import com.sukashawarma.superapp.data.remote.optJsonArray
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.manager.domain.FotoCeklist
import com.sukashawarma.superapp.feature.manager.domain.IsianCeklist
import com.sukashawarma.superapp.feature.manager.domain.LaporanCeklist
import com.sukashawarma.superapp.feature.manager.domain.NilaiCeklist
import com.sukashawarma.superapp.feature.manager.domain.SEMUA_BUTIR
import com.sukashawarma.superapp.feature.manager.domain.ZONA_JAKARTA
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
import java.util.UUID

/** Outlet yang dicek beserta laporan hari yang diminta, bila sudah ada. */
data class DataCeklistHari(
    val outlets: List<OutletPilihan>,
    /** outlet_id -> laporan pada tanggal itu. */
    val laporan: Map<String, LaporanCeklist>,
)

/**
 * Pembacaan dan pengiriman ceklist harian area manager.
 *
 * Seluruh jalurnya memakai JWT pengguna. Yang menjaga kewenangan adalah database:
 * RLS `ceklist_harian_read` (role manajemen + `accessible_outlet_ids()`), RPC
 * `submit_ceklist_harian` (hanya area_manager, tanggal ditentukan server), dan
 * `tinjau_ceklist_harian` (hanya regional manager/admin/owner). Lihat migrasi web
 * `20300238000000_ceklist_harian_area_manager`.
 */
object CeklistHarianRepository {

    private const val BUCKET = "ceklist-harian-foto"
    private val jsonMedia = "application/json".toMediaType()

    private const val KOLOM_LAPORAN =
        "id,outlet_id,submitted_by,nama_am,tanggal,temuan,perbaikan,online_review,catatan," +
            "nama_peninjau,ditinjau_pada,tanggapan_rm,created_at,updated_at," +
            "ceklist_harian_item(kategori,sub_item,nilai,keterangan)," +
            "ceklist_harian_foto(kategori,path,urutan)"

    fun hariIni(): LocalDate = LocalDate.now(ZONA_JAKARTA)

    /**
     * Outlet toko yang boleh dilihat pengguna.
     *
     * Hanya tipe `outlet` dan `mitra`: kantor pusat, gudang, marketplace, dan
     * outlet uji tidak punya rasa, crew, atau stok jualan untuk dicek.
     */
    private suspend fun outlets(): List<OutletPilihan> {
        val filter = CakupanOutletRepository.filterOutlet(CakupanOutletRepository.cakupan())
            ?: return emptyList()
        return Postgrest.select(
            "outlets",
            listOf(
                "select" to "id,name",
                "is_active" to "eq.true",
                "type" to "in.(outlet,mitra)",
                "order" to "name",
            ) + filter,
        ).mapNotNull { elemen ->
            val baris = elemen.asJsonObject
            val id = baris.optString("id") ?: return@mapNotNull null
            OutletPilihan(id, baris.optString("name").orEmpty())
        }
    }

    /** Outlet dan laporan pada [tanggal]. */
    suspend fun muatHari(tanggal: LocalDate): DataCeklistHari = coroutineScope {
        val outlets = async { outlets() }
        val laporan = async {
            Postgrest.select(
                "ceklist_harian",
                listOf("select" to KOLOM_LAPORAN, "tanggal" to "eq.$tanggal"),
            ).mapNotNull { petakan(it.asJsonObject) }
        }
        val daftarOutlet = outlets.await()
        val ids = daftarOutlet.map { it.id }.toSet()
        DataCeklistHari(
            outlets = daftarOutlet,
            // Laporan untuk outlet di luar daftar (mis. outlet yang baru
            // dinonaktifkan) tidak punya kartu; dibuang supaya hitungan cocok.
            laporan = laporan.await().filter { it.outletId in ids }.associateBy { it.outletId },
        )
    }

    /** Jumlah laporan hari ini yang belum ditinjau — lencana menu RM. */
    suspend fun jumlahBelumDitinjau(): Int =
        Postgrest.select(
            "ceklist_harian",
            listOf(
                "select" to "id",
                "tanggal" to "eq.${hariIni()}",
                "ditinjau_pada" to "is.null",
            ),
        ).size()

    private fun petakan(baris: JsonObject): LaporanCeklist? {
        val id = baris.optString("id") ?: return null
        val isian = baris.optJsonArray("ceklist_harian_item").orEmpty().mapNotNull { e ->
            val item = e.asJsonObject
            val kategori = item.optString("kategori") ?: return@mapNotNull null
            val sub = item.optString("sub_item").orEmpty()
            val kunci = if (sub.isEmpty()) kategori else "$kategori.$sub"
            kunci to IsianCeklist(NilaiCeklist.dari(item.optString("nilai")), item.optString("keterangan").orEmpty())
        }.toMap()
        val foto = baris.optJsonArray("ceklist_harian_foto").orEmpty()
            .map { it.asJsonObject }
            .sortedBy { it.optInt("urutan") ?: 0 }
            .mapNotNull { f ->
                val kategori = f.optString("kategori") ?: return@mapNotNull null
                val path = f.optString("path") ?: return@mapNotNull null
                kategori to FotoCeklist(path)
            }
            .groupBy({ it.first }, { it.second })

        return LaporanCeklist(
            id = id,
            outletId = baris.optString("outlet_id").orEmpty(),
            submittedBy = baris.optString("submitted_by").orEmpty(),
            namaAm = baris.optString("nama_am").orEmpty(),
            tanggal = baris.optString("tanggal").orEmpty(),
            isian = isian,
            foto = foto,
            onlineReview = teksArray(baris, "online_review"),
            temuan = teksArray(baris, "temuan"),
            perbaikan = teksArray(baris, "perbaikan"),
            catatan = baris.optString("catatan"),
            namaPeninjau = baris.optString("nama_peninjau"),
            ditinjauPada = baris.optString("ditinjau_pada"),
            tanggapanRm = baris.optString("tanggapan_rm"),
            dibuatPada = baris.optString("created_at").orEmpty(),
            diperbaruiPada = baris.optString("updated_at").orEmpty(),
        )
    }

    private fun JsonArray?.orEmpty(): List<com.google.gson.JsonElement> = this?.toList() ?: emptyList()

    private fun teksArray(baris: JsonObject, kolom: String): List<String> =
        baris.optJsonArray(kolom).orEmpty()
            .mapNotNull { if (it.isJsonNull) null else it.asString.trim() }
            .filter { it.isNotEmpty() }

    /**
     * Mengunggah satu foto WebP dan mengembalikan path-nya.
     *
     * Path WAJIB diawali id pengguna: policy `ceklist_harian_photo_upload` menuntut
     * folder pertama = `auth.uid()`, dan `submit_ceklist_harian` menolak path lain.
     */
    suspend fun unggahFoto(outletId: String, kategori: String, webp: ByteArray): String {
        val userId = AppSession.staff.value?.id
            ?: throw IllegalStateException("Sesi tidak valid, silakan login ulang.")
        val path = "$userId/${hariIni()}/$outletId/$kategori-${UUID.randomUUID()}.webp"
        // Tanpa upsert: nama berkas UUID baru tiap potret, jadi tidak ada yang
        // ditimpa — dan bucket ini memang tidak punya policy UPDATE.
        StorageUtil.uploadWebp(BUCKET, path, webp, upsert = false)
        return path
    }

    /** URL bertanda tangan satu jam untuk menampilkan foto di bucket privat. */
    suspend fun urlFoto(path: String): String? {
        if (path.isBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${SupabaseClient.BASE_URL}storage/v1/object/sign/$BUCKET/$path")
                    .post(JsonObject().apply { addProperty("expiresIn", 3600) }.toString().toRequestBody(jsonMedia))
                    .build()
                SupabaseClient.okHttpClient.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    val body = resp.body?.string().orEmpty()
                    val signed = JsonParser.parseString(body).asJsonObject.optString("signedURL")
                        ?: return@use null
                    "${SupabaseClient.BASE_URL}storage/v1${signed.removePrefix("/storage/v1")}"
                }
            } catch (e: Exception) {
                android.util.Log.e("CeklistHarianRepository", "urlFoto() gagal", e)
                null
            }
        }
    }

    /** Mengirim (atau menyunting) ceklist hari ini untuk satu outlet. */
    suspend fun kirim(
        outletId: String,
        isian: Map<String, IsianCeklist>,
        foto: Map<String, List<FotoCeklist>>,
        onlineReview: List<String>,
        temuan: List<String>,
        perbaikan: List<String>,
        catatan: String,
    ) {
        val items = JsonArray()
        SEMUA_BUTIR.forEach { butir ->
            val isi = isian[butir.kunci] ?: IsianCeklist()
            items.add(
                JsonObject().apply {
                    addProperty("kategori", butir.kategori)
                    addProperty("sub_item", butir.subItem)
                    addProperty("nilai", isi.nilai?.nilai)
                    addProperty("keterangan", isi.keterangan.trim().takeIf { it.isNotEmpty() })
                }
            )
        }
        val daftarFoto = JsonArray()
        foto.forEach { (kategori, isi) ->
            isi.forEach { f ->
                daftarFoto.add(
                    JsonObject().apply {
                        addProperty("kategori", kategori)
                        addProperty("path", f.path)
                    }
                )
            }
        }
        Postgrest.rpc(
            "submit_ceklist_harian",
            JsonObject().apply {
                addProperty("p_outlet_id", outletId)
                add("p_items", items)
                add("p_foto", daftarFoto)
                add("p_temuan", teksKeJson(temuan))
                add("p_perbaikan", teksKeJson(perbaikan))
                add("p_online_review", teksKeJson(onlineReview))
                addProperty("p_catatan", catatan.trim().takeIf { it.isNotEmpty() })
            },
        )
    }

    private fun teksKeJson(daftar: List<String>): JsonArray = JsonArray().apply {
        daftar.map { it.trim() }.filter { it.isNotEmpty() }.forEach { add(it) }
    }

    /** Galat RPC saat laporan berubah sejak dilihat peninjau (migrasi 20300242). */
    const val PESAN_VERSI_BERUBAH = "baru saja diperbarui"

    /**
     * Regional manager menandai laporan sudah ditinjau, dengan tanggapan opsional.
     *
     * [diperbaruiPada] dan [ditinjauPada] adalah versi laporan yang SEDANG TAMPIL
     * di layar peninjau, diteruskan persis seperti diterima dari PostgREST. Server
     * menolak bila AM sudah mengirim ulang atau peninjau lain sudah menanggapi
     * sejak itu — persetujuan tidak boleh jatuh ke isi yang belum pernah dilihat.
     */
    suspend fun tinjau(ceklistId: String, tanggapan: String, diperbaruiPada: String, ditinjauPada: String?) {
        Postgrest.rpc(
            "tinjau_ceklist_harian",
            JsonObject().apply {
                addProperty("p_ceklist_id", ceklistId)
                addProperty("p_tanggapan", tanggapan.trim().takeIf { it.isNotEmpty() })
                addProperty("p_diperbarui_pada", diperbaruiPada.takeIf { it.isNotBlank() })
                addProperty("p_ditinjau_pada", ditinjauPada?.takeIf { it.isNotBlank() })
            },
        )
    }
}
