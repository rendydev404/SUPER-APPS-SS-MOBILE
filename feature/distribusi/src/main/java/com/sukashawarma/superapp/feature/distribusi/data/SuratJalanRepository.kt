package com.sukashawarma.superapp.feature.distribusi.data

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optBoolean
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optJsonArray
import com.sukashawarma.superapp.data.remote.optJsonObject
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.distribusi.data.model.RentangTanggal
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanDetail
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanItem
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanRingkas
import com.sukashawarma.superapp.feature.distribusi.data.model.TandaTangan
import com.sukashawarma.superapp.feature.distribusi.domain.BahanBakuMeta
import com.sukashawarma.superapp.feature.distribusi.domain.KondisiItem
import com.sukashawarma.superapp.feature.distribusi.domain.StatusSuratJalan
import com.sukashawarma.superapp.feature.distribusi.domain.adaSelisih
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
        // Kunci cache diikat pada staf yang sedang masuk. Tablet outlet dipakai
        // bergantian: tanpa ini, staf berikutnya memakai cakupan outlet staf
        // sebelumnya selama sisa TTL, dan melihat daftar yang salah secara diam-diam.
        val kunci = "${AppSession.staff.value?.id ?: "anonim"}:$key"
        val hit = cache[kunci]
        if (hit != null && System.currentTimeMillis() - hit.first < TTL_MS) {
            @Suppress("UNCHECKED_CAST")
            return hit.second as T
        }
        val value = load()
        cache[kunci] = System.currentTimeMillis() to value
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

    // ------------------------------------------------------------ menulis

    /** Peran TTD penerimaan yang diterima RPC `sign_receipt_surat_jalan`.
     *  Nilai di luar keduanya ditolak server dengan exception. */
    const val PERAN_CREW = "Crew Penerima"
    const val PERAN_SUPIR = "Supir"

    /**
     * Membentuk badan PATCH untuk satu `surat_jalan_item`.
     *
     * Dipisah dari pemanggilan jaringan supaya bisa diuji: inilah satu-satunya
     * tempat yang menentukan kolom apa saja yang tersentuh, dan daftar itu harus
     * persis sama dengan yang ditulis `handleSubmit` di `VerifikasiForm.tsx`.
     *
     * `qtyTerimaDasar` dan `qtyDikirimDasar` keduanya dalam SATUAN DASAR — bandingan
     * `flagged` harus pada satuan yang sama, bukan mencampur satuan distribusi.
     */
    internal fun patchVerifikasi(
        qtyTerimaDasar: Double,
        qtyDikirimDasar: Double,
        kondisi: KondisiItem,
        catatan: String,
        fotoPath: String?,
        waktuIso: String,
    ): JsonObject {
        val patch = JsonObject()
        patch.addProperty("qty_terima", qtyTerimaDasar)
        patch.addProperty("kondisi", kondisi.nilaiDb)
        if (catatan.isBlank()) patch.add("catatan", JsonNull.INSTANCE)
        else patch.addProperty("catatan", catatan)
        patch.addProperty(
            "flagged",
            qtyTerimaDasar != qtyDikirimDasar || kondisi == KondisiItem.TIDAK_SESUAI,
        )
        if (fotoPath.isNullOrBlank()) patch.add("foto_path", JsonNull.INSTANCE)
        else patch.addProperty("foto_path", fotoPath)
        patch.addProperty("verified_at", waktuIso)
        return patch
    }

    suspend fun simpanVerifikasiItem(
        itemId: String,
        qtyTerimaDasar: Double,
        qtyDikirimDasar: Double,
        kondisi: KondisiItem,
        catatan: String,
        fotoPath: String?,
    ) {
        val waktu = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        Postgrest.update(
            "surat_jalan_item",
            listOf("id" to "eq.$itemId"),
            patchVerifikasi(qtyTerimaDasar, qtyDikirimDasar, kondisi, catatan, fotoPath, waktu),
        )
    }

    /**
     * Menyimpan satu tanda tangan penerimaan dan mengembalikan daftar terbaru
     * dari server. Daftar itulah yang harus jadi sumber kebenaran layar — bukan
     * salinan lokal — supaya TTD yang sudah tersimpan tetap terlihat setelah
     * aplikasi ditutup di tengah proses.
     *
     * RPC menolak status di luar penerimaan, peran di luar dua nilai yang sah,
     * dan tanda tangan ganda untuk peran yang sama. Pesan penolakannya sudah
     * berbahasa Indonesia dan diteruskan apa adanya oleh `distribusiErrorMessage`.
     */
    suspend fun tandaTanganPenerimaan(
        suratJalanId: String,
        nama: String,
        peran: String,
        gambar: String,
    ): List<TandaTangan> {
        val body = JsonObject()
        body.addProperty("p_surat_jalan_id", suratJalanId)
        body.addProperty("p_signed_by_name", nama)
        body.addProperty("p_role", peran)
        body.addProperty("p_signature_image", gambar)
        val hasil = Postgrest.rpc("sign_receipt_surat_jalan", body)
        invalidate()
        val obj = hasil.takeIf { it.isJsonObject }?.asJsonObject ?: return emptyList()
        return keTandaTangan(obj.optJsonArray("receipt_signatures"))
    }

    data class HasilFinalisasi(val sukses: Boolean, val pesan: String, val statusAkhir: String?)

    /**
     * Menutup verifikasi: RPC menulis `ledger_stok` dan menetapkan status akhir.
     * Aplikasi tidak pernah menghitung status akhir sendiri.
     *
     * Idempoten di sisi server: pemanggilan kedua untuk surat jalan yang sudah
     * diverifikasi mengembalikan `success:false` dengan pesan "sudah diverifikasi
     * sebelumnya". Pemanggil harus memperlakukan kasus itu sebagai berhasil —
     * artinya percobaan sebelumnya sampai ke server walau jaringannya putus.
     */
    suspend fun finalisasi(suratJalanId: String): HasilFinalisasi {
        val body = JsonObject()
        body.addProperty("p_surat_jalan_id", suratJalanId)
        val hasil = Postgrest.rpc("finalize_surat_jalan_and_ledger", body)
        invalidate()
        val obj = hasil.takeIf { it.isJsonObject }?.asJsonObject
            ?: return HasilFinalisasi(false, "Balasan server tidak dikenali.", null)
        return HasilFinalisasi(
            sukses = obj.optBoolean("success"),
            pesan = obj.optString("message").orEmpty(),
            statusAkhir = obj.optString("status"),
        )
    }

    /**
     * Menutup dokumen jadi `selesai`. Hanya area/regional manager yang boleh,
     * dan hanya untuk dokumen yang sudah diverifikasi outlet — kedua syarat itu
     * ditegakkan pemanggil lewat `DistribusiAkses` dan `bolehDitutup`.
     *
     * RLS `surat_jalan_update_scoped` adalah jaring pengaman terakhirnya di server.
     */
    suspend fun tutupDokumen(suratJalanId: String) {
        val patch = JsonObject()
        patch.addProperty("status", StatusSuratJalan.SELESAI.nilai)
        patch.addProperty(
            "updated_at",
            OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
        )
        Postgrest.update("surat_jalan", listOf("id" to "eq.$suratJalanId"), patch)
        invalidate()
    }
}
