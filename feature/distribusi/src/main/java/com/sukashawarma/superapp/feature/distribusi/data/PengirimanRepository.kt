package com.sukashawarma.superapp.feature.distribusi.data

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optBoolean
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optJsonArray
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.feature.distribusi.data.model.TandaTangan
import com.sukashawarma.superapp.feature.distribusi.domain.Alokasi
import com.sukashawarma.superapp.feature.distribusi.domain.BahanBakuMeta
import com.sukashawarma.superapp.feature.distribusi.domain.SaldoVendor
import com.sukashawarma.superapp.feature.distribusi.domain.SatuanDistribusi
import com.sukashawarma.superapp.feature.distribusi.domain.distribusiErrorMessage

/** Outlet tujuan di form buat surat jalan. */
data class OutletTujuan(val id: String, val nama: String, val alamat: String?)

/** Satu baris muatan di form: bahan, qty dalam SATUAN DISTRIBUSI, dan alokasi
 *  vendornya (juga satuan distribusi) bila bahannya multi-vendor. */
data class MuatanKirim(val bahan: BahanBakuMeta, val qty: Double, val alokasi: List<Alokasi>)

/**
 * Sisi pengirim (Gudang Pusat / kitchen): membuat, menandatangani, mengirim,
 * dan membatalkan surat jalan. Cermin `SuratJalanForm.tsx`, `SignatureFlow.tsx`,
 * dan `SuratJalanDetail.tsx` web — nama RPC, parameter, dan kolom sama persis.
 *
 * Gerbang role dijaga pemanggil lewat `DistribusiAkses`; `create_surat_jalan_with_number`
 * dan `batalkan_surat_jalan_draft` juga memeriksa role di server.
 */
object PengirimanRepository {

    /** Outlet uji coba yang disembunyikan dari pilihan tujuan — `lib/outletFilters.ts`. */
    private const val OUTLET_UJI = "eb174b2b-ff69-47eb-97af-b6c824d3ce4a"

    /**
     * @param sertakanOutletUji true hanya di build debug, supaya alur kirim bisa
     *   diuji ujung-ke-ujung ke outlet tes tanpa menyentuh outlet sungguhan.
     *   Build rilis tetap menyembunyikannya seperti web.
     */
    suspend fun outletTujuan(sertakanOutletUji: Boolean = false): List<OutletTujuan> =
        Postgrest.select(
            "outlets",
            buildList {
                add("select" to "id,name,address")
                add("is_active" to "eq.true")
                if (!sertakanOutletUji) add("id" to "neq.$OUTLET_UJI")
                add("order" to "name.asc")
            },
        ).map {
            val o = it.asJsonObject
            OutletTujuan(o.optString("id").orEmpty(), o.optString("name").orEmpty(), o.optString("address"))
        }

    suspend fun bahanAktif(): List<BahanBakuMeta> =
        Postgrest.select(
            "bahan_baku",
            listOf(
                "select" to "id,nama,kategori,satuan,satuan_distribusi,satuan_tengah," +
                    "satuan_kecil,faktor_tengah,faktor_tampilan",
                "is_active" to "eq.true",
                "order" to "nama.asc",
            ),
        ).map { with(SuratJalanRepository) { it.asJsonObject.keBahanMeta() } }

    /** Saldo vendor Gudang Pusat per bahan, dalam SATUAN BESAR seperti dari RPC. */
    suspend fun saldoVendor(bahanIds: Collection<String>): Map<String, List<SaldoVendor>> {
        if (bahanIds.isEmpty()) return emptyMap()
        val body = JsonObject()
        body.add("p_bahan_ids", JsonArray().apply { bahanIds.forEach { add(it) } })
        val hasil = Postgrest.rpc("saldo_vendor_gudang", body)
        if (!hasil.isJsonArray) return emptyMap()
        return hasil.asJsonArray.mapNotNull { e ->
            val o = e.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            val bahanId = o.optString("bahan_baku_id") ?: return@mapNotNull null
            bahanId to SaldoVendor(
                vendorId = o.optString("vendor_id").orEmpty(),
                vendorNama = o.optString("vendor_nama").orEmpty(),
                sisa = o.optDouble("sisa") ?: 0.0,
                aktif = o.optBoolean("aktif"),
                multi = o.optBoolean("multi"),
            )
        }.groupBy({ it.first }, { it.second })
    }

    /** Nama staf aktif untuk pilihan penanda tangan: (semua, khusus kitchen). */
    suspend fun namaStaf(): Pair<List<String>, List<String>> {
        val baris = Postgrest.select(
            "outlet_staff",
            listOf("select" to "name,role", "status" to "eq.active", "order" to "name.asc"),
        ).map { it.asJsonObject }
        val semua = baris.mapNotNull { it.optString("name") }.distinct()
        val kitchen = baris.filter { it.optString("role") == "kitchen" }
            .mapNotNull { it.optString("name") }.distinct()
        return semua to kitchen
    }

    /**
     * Baris `surat_jalan_item` yang akan disisipkan. Dipisah dari jaringan supaya
     * bisa diuji: bahan multi-vendor pecah satu baris per alokasi dengan
     * `vendor_id`; bahan satu-vendor satu baris tanpa `vendor_id` (diisi trigger
     * `fill_harga_snapshot`). `qty_dikirim` selalu dalam SATUAN DASAR.
     */
    internal fun barisItem(
        suratJalanId: String,
        muatan: List<MuatanKirim>,
        multiVendor: (String) -> Boolean,
    ): JsonArray {
        val arr = JsonArray()
        muatan.forEach { m ->
            if (multiVendor(m.bahan.id)) {
                m.alokasi.forEach { a ->
                    arr.add(JsonObject().apply {
                        addProperty("surat_jalan_id", suratJalanId)
                        addProperty("bahan_baku_id", m.bahan.id)
                        addProperty("qty_dikirim", SatuanDistribusi.keDasar(a.qty, m.bahan))
                        addProperty("vendor_id", a.vendorId)
                    })
                }
            } else {
                arr.add(JsonObject().apply {
                    addProperty("surat_jalan_id", suratJalanId)
                    addProperty("bahan_baku_id", m.bahan.id)
                    addProperty("qty_dikirim", SatuanDistribusi.keDasar(m.qty, m.bahan))
                })
            }
        }
        return arr
    }

    class GagalBuat(pesan: String) : Exception(pesan)

    /**
     * Dua langkah seperti web: header lewat RPC (nomor dokumen & kode verifikasi
     * dibuat server), lalu item. Tidak atomik — kalau langkah kedua gagal, draft
     * kosong tertinggal dan pesannya menyebut itu supaya kitchen bisa membatalkannya.
     */
    suspend fun buatSuratJalan(
        outletId: String,
        muatan: List<MuatanKirim>,
        multiVendor: (String) -> Boolean,
    ): String {
        val body = JsonObject().apply { addProperty("p_outlet_id", outletId) }
        val hasil = Postgrest.rpc("create_surat_jalan_with_number", body)
        val header = hasil.takeIf { it.isJsonObject }?.asJsonObject
        val id = header?.optString("id") ?: throw GagalBuat("ID Surat Jalan tidak valid dari server")
        try {
            Postgrest.insert("surat_jalan_item", barisItem(id, muatan, multiVendor), returning = false)
        } catch (e: Exception) {
            // Batas waktu habis tidak berarti gagal: sisipan bisa saja sudah masuk.
            // Kalau itemnya ada, surat jalan ini lengkap dan diperlakukan berhasil.
            val adaItem = runCatching {
                Postgrest.select(
                    "surat_jalan_item",
                    listOf("select" to "id", "surat_jalan_id" to "eq.$id", "limit" to "1"),
                ).size() > 0
            }.getOrDefault(false)
            if (adaItem) return id
            val nomor = header.optString("document_number") ?: id.take(8).uppercase()
            throw GagalBuat(
                "item gagal disimpan (${distribusiErrorMessage(e)}). Draft kosong SJ $nomor " +
                    "sudah terbuat — batalkan dari daftar surat jalan.",
            )
        } finally {
            SuratJalanRepository.invalidate()
        }
        return id
    }

    suspend fun ubahVendorItem(itemId: String, vendorId: String) {
        val body = JsonObject().apply {
            addProperty("p_item_id", itemId)
            addProperty("p_vendor_id", vendorId)
        }
        Postgrest.rpc("update_surat_jalan_item_vendor", body)
        SuratJalanRepository.invalidate()
    }

    /** Tanda tangan pengirim; daftar balasan server jadi sumber kebenaran layar. */
    suspend fun tandaTanganPengirim(
        suratJalanId: String,
        nama: String,
        peran: String,
        gambar: String,
    ): List<TandaTangan> {
        val body = JsonObject().apply {
            addProperty("p_surat_jalan_id", suratJalanId)
            addProperty("p_signed_by_name", nama)
            addProperty("p_role", peran)
            addProperty("p_signature_image", gambar)
        }
        val hasil = Postgrest.rpc("sign_surat_jalan", body)
        SuratJalanRepository.invalidate()
        val arr = hasil.takeIf { it.isJsonObject }?.asJsonObject?.optJsonArray("signatures")
            ?: throw GagalBuat("Tidak ada data tanda tangan kembali dari server")
        return SuratJalanRepository.keTandaTangan(arr)
    }

    /** draft -> dikirim. Trigger DB memotong stok Gudang Pusat dan saldo vendor. */
    suspend fun kirim(suratJalanId: String) {
        val body = JsonObject().apply { addProperty("p_surat_jalan_id", suratJalanId) }
        try {
            Postgrest.rpc("send_surat_jalan_signed", body)
        } finally {
            SuratJalanRepository.invalidate()
        }
    }

    data class HasilBatal(val sukses: Boolean, val pesan: String)

    /** RPC membalas `{success, message}` alih-alih melempar — `success` wajib diperiksa. */
    suspend fun batalkanDraft(suratJalanId: String, alasan: String): HasilBatal {
        val body = JsonObject().apply {
            addProperty("p_surat_jalan_id", suratJalanId)
            val bersih = alasan.trim()
            if (bersih.isEmpty()) add("p_alasan", JsonNull.INSTANCE) else addProperty("p_alasan", bersih)
        }
        val hasil = Postgrest.rpc("batalkan_surat_jalan_draft", body)
        SuratJalanRepository.invalidate()
        val obj = hasil.takeIf { it.isJsonObject }?.asJsonObject
            ?: return HasilBatal(false, "Balasan server tidak dikenali.")
        return HasilBatal(obj.optBoolean("success"), obj.optString("message").orEmpty())
    }
}
