package com.sukashawarma.superapp.feature.distribusi.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optJsonArray
import com.sukashawarma.superapp.data.remote.optJsonObject
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.feature.distribusi.data.model.RentangTanggal
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanDetail
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanItem
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanRingkas
import com.sukashawarma.superapp.feature.distribusi.data.model.TandaTangan
import com.sukashawarma.superapp.feature.distribusi.domain.BahanBakuMeta
import com.sukashawarma.superapp.feature.distribusi.domain.StatusSuratJalan
import com.sukashawarma.superapp.feature.distribusi.domain.adaSelisih
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Akses ke domain surat jalan.
 *
 * Cakupan outlet SELALU berasal dari RPC `accessible_outlet_ids()`, tidak pernah
 * dari daftar role di dalam APK: kebijakan yang berubah di database tidak boleh
 * menuntut rilis aplikasi baru. RLS `surat_jalan_select` menegakkan hal yang
 * sama di sisi server, jadi filter di sini adalah efisiensi, bukan keamanan.
 */
object SuratJalanRepository {

    /** Kolom yang cukup untuk daftar dan penghitungan selisih. */
    private const val SELECT_RINGKAS =
        "id,outlet_id,status,created_at,document_number,outlets(name)," +
            "surat_jalan_item(qty_dikirim,qty_terima,kondisi)"

    private const val SELECT_DETAIL =
        "id,outlet_id,status,created_at,document_number,verification_code," +
            "signatures,receipt_signatures,outlets(name)," +
            "surat_jalan_item(id,bahan_baku_id,qty_dikirim,qty_terima,kondisi,catatan," +
            "foto_path,verified_at,bahan_baku(id,nama,kategori,satuan,satuan_distribusi," +
            "satuan_tengah,satuan_kecil,faktor_tengah,faktor_tampilan))"

    private const val TTL_MS = 60_000L
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

    /** Dipanggil saat pengguna menarik untuk menyegarkan, dan setelah setiap tulis. */
    fun invalidate() = cache.clear()

    // ------------------------------------------------------------ cakupan

    suspend fun outletTerjangkau(): List<String> = cached("outlet_terjangkau") {
        val hasil = Postgrest.rpc("accessible_outlet_ids")
        if (!hasil.isJsonArray) return@cached emptyList()
        hasil.asJsonArray.mapNotNull { elemen ->
            elemen.takeIf { !it.isJsonNull }?.asString
        }
    }

    /** Format nilai untuk operator `in.` PostgREST: `in.(id1,id2)`. */
    private fun filterOutlet(ids: List<String>): Pair<String, String> =
        "outlet_id" to ids.joinToString(prefix = "in.(", postfix = ")", separator = ",")

    // ------------------------------------------------------------ daftar

    suspend fun daftar(rentang: RentangTanggal): List<SuratJalanRingkas> {
        val ids = outletTerjangkau()
        if (ids.isEmpty()) return emptyList()

        val params = mutableListOf(
            "select" to SELECT_RINGKAS,
            filterOutlet(ids),
            "order" to "created_at.desc",
        )
        batasWaktu(rentang)?.let { params += "created_at" to "gte.$it" }
        return Postgrest.select("surat_jalan", params).map { it.asJsonObject.keRingkas() }
    }

    /** Kiriman yang masih menunggu diterima outlet. */
    suspend fun inbox(): List<SuratJalanRingkas> {
        val ids = outletTerjangkau()
        if (ids.isEmpty()) return emptyList()
        return Postgrest.select(
            "surat_jalan",
            listOf(
                "select" to SELECT_RINGKAS,
                filterOutlet(ids),
                "status" to "in.(dikirim,dikirim_lengkap,diterima_sebagian)",
                "order" to "created_at.desc",
            ),
        ).map { it.asJsonObject.keRingkas() }
    }

    /** Penerimaan yang sudah diverifikasi, termasuk yang sudah ditutup pusat. */
    suspend fun riwayat(): List<SuratJalanRingkas> {
        val ids = outletTerjangkau()
        if (ids.isEmpty()) return emptyList()
        return Postgrest.select(
            "surat_jalan",
            listOf(
                "select" to SELECT_RINGKAS,
                filterOutlet(ids),
                "status" to "in.(diterima_lengkap,diterima_sebagian,selesai)",
                "order" to "created_at.desc",
            ),
        ).map { it.asJsonObject.keRingkas() }
    }

    suspend fun detail(id: String): SuratJalanDetail? {
        val baris = Postgrest.selectOne(
            "surat_jalan",
            listOf("select" to SELECT_DETAIL, "id" to "eq.$id"),
        ) ?: return null
        return baris.keDetail()
    }

    /**
     * Pencarian untuk gerbang QR. Kode 36 karakter bertanda hubung diperlakukan
     * sebagai `id` (huruf kecil), selain itu sebagai `verification_code` (huruf
     * besar) — persis seperti `navigateToVerifikasi` di `QRScanner.tsx`.
     */
    suspend fun cariUntukVerifikasi(kode: String): SuratJalanRingkas? {
        val bersih = kode.trim().substringAfterLast('/')
        if (bersih.isBlank()) return null
        val berupaUuid = bersih.length == 36 && bersih.contains('-')
        val kolom = if (berupaUuid) "id" else "verification_code"
        val nilai = if (berupaUuid) bersih.lowercase() else bersih.uppercase()
        val baris = Postgrest.selectOne(
            "surat_jalan",
            listOf("select" to SELECT_RINGKAS, kolom to "eq.$nilai"),
        ) ?: return null
        return baris.keRingkas()
    }

    // ------------------------------------------------------------ pemetaan

    private fun batasWaktu(rentang: RentangTanggal): String? {
        val zona = ZoneId.systemDefault()
        val sekarang = Instant.now()
        return when (rentang) {
            RentangTanggal.SEMUA -> null
            RentangTanggal.HARI_INI -> LocalDate.now(zona).atStartOfDay(zona).toInstant().toString()
            RentangTanggal.TUJUH_HARI -> sekarang.minus(7, ChronoUnit.DAYS).toString()
            RentangTanggal.TIGA_PULUH_HARI -> sekarang.minus(30, ChronoUnit.DAYS).toString()
        }
    }

    /** PostgREST mengembalikan relasi to-one sebagai objek, tapi beberapa versi
     *  membungkusnya dalam array satu elemen. Tangani keduanya. */
    private fun JsonObject.relasiTunggal(key: String): JsonObject? {
        optJsonObject(key)?.let { return it }
        val arr = optJsonArray(key) ?: return null
        return arr.firstOrNull()?.takeIf { it.isJsonObject }?.asJsonObject
    }

    private fun JsonObject.keRingkas(): SuratJalanRingkas {
        val items = (optJsonArray("surat_jalan_item") ?: JsonArray()).map { elemen ->
            val o = elemen.asJsonObject
            SuratJalanItem(
                id = o.optString("id").orEmpty(),
                bahanBakuId = o.optString("bahan_baku_id").orEmpty(),
                qtyDikirim = o.optDouble("qty_dikirim") ?: 0.0,
                qtyTerima = o.optDouble("qty_terima"),
                kondisi = o.optString("kondisi"),
                catatan = null,
                fotoPath = null,
                terverifikasiPada = null,
                bahan = null,
            )
        }
        return SuratJalanRingkas(
            id = optString("id").orEmpty(),
            outletId = optString("outlet_id").orEmpty(),
            status = StatusSuratJalan.dari(optString("status")),
            namaOutlet = relasiTunggal("outlets")?.optString("name"),
            nomorDokumen = optString("document_number"),
            dibuatPada = optString("created_at"),
            adaSelisih = adaSelisih(items),
        )
    }

    private fun JsonObject.keDetail(): SuratJalanDetail = SuratJalanDetail(
        id = optString("id").orEmpty(),
        outletId = optString("outlet_id").orEmpty(),
        status = StatusSuratJalan.dari(optString("status")),
        namaOutlet = relasiTunggal("outlets")?.optString("name"),
        nomorDokumen = optString("document_number"),
        kodeVerifikasi = optString("verification_code"),
        dibuatPada = optString("created_at"),
        ttdPengirim = keTandaTangan(optJsonArray("signatures")),
        ttdPenerimaan = keTandaTangan(optJsonArray("receipt_signatures")),
        items = (optJsonArray("surat_jalan_item") ?: JsonArray()).map { elemen ->
            val o = elemen.asJsonObject
            SuratJalanItem(
                id = o.optString("id").orEmpty(),
                bahanBakuId = o.optString("bahan_baku_id").orEmpty(),
                qtyDikirim = o.optDouble("qty_dikirim") ?: 0.0,
                qtyTerima = o.optDouble("qty_terima"),
                kondisi = o.optString("kondisi"),
                catatan = o.optString("catatan"),
                fotoPath = o.optString("foto_path"),
                terverifikasiPada = o.optString("verified_at"),
                bahan = o.relasiTunggal("bahan_baku")?.keBahanMeta(),
            )
        }.sortedBy { it.bahan?.nama ?: "" },
    )

    private fun JsonObject.keBahanMeta() = BahanBakuMeta(
        id = optString("id").orEmpty(),
        nama = optString("nama").orEmpty(),
        satuan = optString("satuan").orEmpty(),
        satuanDistribusi = optString("satuan_distribusi"),
        satuanTengah = optString("satuan_tengah"),
        satuanKecil = optString("satuan_kecil"),
        faktorTengah = optDouble("faktor_tengah"),
        faktorTampilan = optDouble("faktor_tampilan"),
        kategori = optString("kategori"),
    )

    private fun keTandaTangan(arr: JsonArray?): List<TandaTangan> =
        (arr ?: JsonArray()).mapNotNull { elemen ->
            val o = elemen.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            TandaTangan(
                namaPenandaTangan = o.optString("signed_by").orEmpty(),
                peran = o.optString("role").orEmpty(),
                waktu = o.optString("signed_at").orEmpty(),
                gambar = o.optString("signature_image"),
            )
        }
}
