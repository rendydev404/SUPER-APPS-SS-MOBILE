package com.sukashawarma.superapp.feature.stok.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.sukashawarma.superapp.core.storage.StorageUtil
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.SupabaseClient
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optJsonArray
import com.sukashawarma.superapp.data.remote.optJsonObject
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.feature.stok.data.model.Retur
import com.sukashawarma.superapp.feature.stok.data.model.ReturItem
import com.sukashawarma.superapp.feature.stok.domain.JenisLogistik
import com.sukashawarma.superapp.feature.stok.domain.StatusRetur
import java.util.UUID

/**
 * Retur & Refund bahan core — cermin `app/actions/retur.ts` dan `hooks/useRetur.ts`.
 *
 * Seluruh perubahan status lewat RPC, tidak ada satu pun UPDATE langsung ke
 * `retur_stok` dari sini. Itu bukan selera: tiap RPC mengerjakan lebih dari sekadar
 * menaikkan status — pengajuan sekaligus memotong `ledger_stok`, penolakan manajer
 * mengalihkan baris ledger yang sama menjadi `waste`, dan verifikasi kitchen
 * menerbitkan `surat_jalan` beserta itemnya. Menulis status langsung akan
 * meninggalkan saldo stok yang tidak pernah dikoreksi.
 *
 * Pembacaan tetap lewat PostgREST biasa: RLS `retur_stok_read` membatasi baris ke
 * `accessible_outlet_ids()`, jadi satu kueri yang sama otomatis menyempit sendiri
 * untuk kru outlet dan melebar untuk manajer wilayah.
 */
object ReturRepository {

    /**
     * Bucket khusus bukti retur. Publik (lihat migration `20260914170000`), jadi URL
     * penuhnya boleh disimpan apa adanya di kolom foto — sama seperti waste.
     */
    private const val BUCKET_RETUR = "retur_evidence"
    private const val BUCKET_CADANGAN = "waste_evidence"

    private const val SELECT_FULL =
        "*,outlets(name)," +
            "created_by_staff:created_by(name)," +
            "surat_jalan_pengganti:ref_surat_jalan_pengganti_id(id,document_number,status)," +
            "items:retur_stok_item(" +
            "id,bahan_baku_id,qty_klaim,qty_diterima_kitchen,foto_fisik_url," +
            "foto_timbangan_url,alasan,catatan,bahan_baku(nama,satuan))"

    // -------------------------------------------------------------- pembacaan

    /**
     * Daftar tiket yang boleh dilihat akun ini.
     *
     * [outletId] null berarti seluruh outlet yang terjangkau — dipakai manajer dan
     * gudang pusat, yang antreannya memang lintas outlet. Kru mengirim outletnya
     * sendiri supaya tiket outlet tetangga (yang ikut terjangkau lewat
     * `accessible_outlet_ids()` pada akun multi-outlet) tidak ikut terbaca.
     */
    suspend fun daftar(outletId: String?): List<Retur> = Postgrest.select(
        "retur_stok",
        buildList {
            add("select" to SELECT_FULL)
            add("order" to "created_at.desc")
            if (outletId != null) add("outlet_id" to "eq.$outletId")
        },
    ).mapNotNull { it.asJsonObject.toRetur() }

    /**
     * Status + outlet tiap tiket, untuk lencana angka di bilah bawah.
     *
     * Sengaja tidak memakai [daftar]: lencana ikut termuat di layar mana pun dalam
     * modul Stok, sedangkan [daftar] menarik item, foto, dan embed surat jalan yang
     * sama sekali tidak dipakai untuk menghitung satu angka.
     *
     * Batas 200 baris cukup: yang dihitung hanya tiket yang belum tuntas, dan
     * antrean sebanyak itu berarti ada yang salah jauh sebelum angkanya meleset.
     */
    suspend fun statusUntukBadge(): List<Pair<StatusRetur, String>> = Postgrest.select(
        "retur_stok",
        listOf(
            "select" to "status,outlet_id",
            "order" to "created_at.desc",
            "limit" to "200",
        ),
    ).mapNotNull { el ->
        val o = el.asJsonObject
        val status = StatusRetur.dari(o.optString("status")) ?: return@mapNotNull null
        val outlet = o.optString("outlet_id") ?: return@mapNotNull null
        status to outlet
    }

    /**
     * Tiket yang fisiknya sudah ditimbang di gudang pusat tetapi penggantinya belum
     * dikirim — cermin `fetchPendingReturForOutlet`.
     *
     * Inilah yang membuat penggantian bisa menumpang pengiriman reguler: layar
     * persetujuan Permintaan membacanya untuk outlet yang sedang disetujui, lalu
     * menerbitkan SJ Pengganti sekalian. Kegagalannya tidak boleh menggagalkan
     * persetujuan, jadi pemanggil membungkusnya dengan runCatching.
     */
    suspend fun menungguPengganti(outletId: String): List<Retur> = Postgrest.select(
        "retur_stok",
        listOf(
            "select" to SELECT_FULL,
            "outlet_id" to "eq.$outletId",
            "status" to "eq.${StatusRetur.DITERIMA_KITCHEN.nilai}",
            "order" to "created_at.asc",
        ),
    ).mapNotNull { it.asJsonObject.toRetur() }

    // ----------------------------------------------------------------- bukti

    /**
     * Mengunggah foto bukti dan mengembalikan URL publiknya.
     *
     * Jatuh ke `waste_evidence` bila bucket retur belum ada, persis seperti web:
     * migration bucket dan migration tabel terpisah, dan yang pertama bisa saja
     * belum diterapkan di lingkungan tertentu. Tanpa cadangan ini, seluruh
     * pengajuan retur akan mati total hanya karena satu bucket belum dibuat.
     */
    suspend fun unggahBukti(outletId: String, jpeg: ByteArray): String {
        val nama = "${outletId}_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
        return try {
            StorageUtil.uploadJpeg(BUCKET_RETUR, nama, jpeg)
            urlPublik(BUCKET_RETUR, nama)
        } catch (_: Exception) {
            StorageUtil.uploadJpeg(BUCKET_CADANGAN, "retur/$nama", jpeg)
            urlPublik(BUCKET_CADANGAN, "retur/$nama")
        }
    }

    private fun urlPublik(bucket: String, path: String) =
        "${SupabaseClient.BASE_URL}storage/v1/object/public/$bucket/$path"

    // ------------------------------------------------------------------- RPC

    /**
     * Mengajukan klaim retur. [qtyBesarPerBahan] WAJIB pada satuan besar: RPC
     * meneruskannya ke `to_ledger_scale` lalu memotong saldo outlet sebesar itu.
     *
     * `tipe_retur` dikunci `chiller_outlet` karena hanya itu yang dipakai formulir
     * web. Jalur `inbound_sj` (menolak di depan kurir saat SJ datang) ada di
     * constraint dan di dokumen desain, tetapi belum punya antarmuka di mana pun —
     * dan bedanya bukan kosmetik: `inbound_sj` TIDAK memotong saldo outlet, sebab
     * barangnya memang tidak pernah masuk.
     */
    suspend fun ajukan(
        outletId: String,
        bahanBakuId: String,
        qtyBesar: Double,
        fotoUrl: String,
        alasan: String,
        catatan: String?,
    ) {
        val item = JsonObject().apply {
            addProperty("bahan_baku_id", bahanBakuId)
            addProperty("qty_klaim", qtyBesar)
            addProperty("foto_fisik_url", fotoUrl)
            // Satu foto, dua kolom — schema menuntut foto fisik, web mengisi keduanya
            // dengan berkas yang sama supaya bukti timbangan tidak pernah kosong.
            addProperty("foto_timbangan_url", fotoUrl)
            addProperty("alasan", alasan)
            if (!catatan.isNullOrBlank()) addProperty("catatan", catatan)
        }
        Postgrest.rpc(
            "ajukan_retur_stok",
            JsonObject().apply {
                addProperty("p_outlet_id", outletId)
                addProperty("p_tipe_retur", "chiller_outlet")
                add("p_items", JsonArray().apply { add(item) })
                if (catatan.isNullOrBlank()) add("p_catatan", null) else addProperty("p_catatan", catatan)
            },
        )
    }

    /**
     * Keputusan AM/RM. Penolakan tidak sekadar menutup tiket: RPC mengalihkan baris
     * ledger `retur_ke_pusat` milik tiket ini menjadi `waste`, sehingga stok yang
     * sudah terpotong resmi menjadi beban waste outlet, bukan kembali ajaib.
     *
     * [managerId] WAJIB dikirim, dan bukan karena RPC membutuhkannya — badan
     * fungsinya sendiri sudah ber-`COALESCE(p_manager_id, auth.uid())`.
     *
     * Database menyimpan DUA fungsi bernama sama: versi lama 3 parameter
     * (`p_retur_id, p_approve, p_catatan`) dan versi migration `20260914161000`
     * yang menambah `p_manager_id`. `CREATE OR REPLACE FUNCTION` hanya menimpa
     * signature yang persis sama, jadi yang lama tidak pernah tergantikan.
     * Memanggil dengan 3 parameter membuat PostgREST tidak bisa memilih di antara
     * keduanya dan menjawab `PGRST203` / HTTP 300 — persis yang dialami halaman
     * `/stok/refund` web, yang memang masih memanggil versi 3 parameter dan karena
     * itu tombol setujunya tidak pernah berfungsi. Yang berhasil selama ini hanya
     * `apps/manager`, yang kebetulan mengirim keempatnya.
     *
     * Mengirim parameter keempat membuat panggilannya tidak ambigu. Perbaikan yang
     * sebenarnya adalah `DROP FUNCTION` versi 3 parameter di database; sampai itu
     * dikerjakan, jangan menghapus argumen ini.
     */
    suspend fun putuskanManager(
        returId: String,
        setuju: Boolean,
        catatan: String?,
        managerId: String?,
    ) {
        Postgrest.rpc(
            "approve_retur_by_manager",
            JsonObject().apply {
                addProperty("p_retur_id", returId)
                addProperty("p_approve", setuju)
                if (catatan.isNullOrBlank()) add("p_catatan", null) else addProperty("p_catatan", catatan)
                isiAtauNull("p_manager_id", managerId)
            },
        )
    }

    /** Serah terima fisik ke kurir internal maupun 3PL. */
    suspend fun serahKurir(
        returId: String,
        jenis: JenisLogistik,
        nomorResi: String?,
        driverNama: String?,
        driverKontak: String?,
        driverPlat: String?,
        fotoUrl: String?,
    ) {
        Postgrest.rpc(
            "konfirmasi_serah_terima_logistik",
            JsonObject().apply {
                addProperty("p_retur_id", returId)
                addProperty("p_jenis_logistik", jenis.nilai)
                isiAtauNull("p_nomor_resi", nomorResi)
                isiAtauNull("p_driver_nama", driverNama)
                isiAtauNull("p_driver_kontak", driverKontak)
                isiAtauNull("p_driver_plat", driverPlat)
                isiAtauNull("p_foto_serah_terima", fotoUrl)
            },
        )
    }

    /**
     * Gudang pusat menimbang ulang, lalu memilih menerbitkan SJ Pengganti sekarang
     * atau menyimpan hasil timbangan saja.
     *
     * [terbitkanSjSekarang] false menaruh tiket di `diterima_kitchen`, yang lalu
     * terbaca [menungguPengganti] dan bisa menumpang pengiriman reguler berikutnya.
     */
    suspend fun verifikasiKitchen(
        returId: String,
        qtyPerItem: Map<String, Double>,
        catatan: String?,
        terbitkanSjSekarang: Boolean,
    ) {
        val items = JsonArray().apply {
            qtyPerItem.forEach { (itemId, qty) ->
                add(
                    JsonObject().apply {
                        addProperty("id", itemId)
                        addProperty("qty_diterima_kitchen", qty)
                    }
                )
            }
        }
        Postgrest.rpc(
            "verifikasi_kitchen_dan_buat_sj",
            JsonObject().apply {
                addProperty("p_retur_id", returId)
                add("p_items_verified", items)
                if (catatan.isNullOrBlank()) add("p_catatan", null) else addProperty("p_catatan", catatan)
                addProperty("p_terbitkan_sj_sekarang", terbitkanSjSekarang)
            },
        )
    }

    private fun JsonObject.isiAtauNull(kunci: String, nilai: String?) {
        if (nilai.isNullOrBlank()) add(kunci, null) else addProperty(kunci, nilai)
    }

    // -------------------------------------------------------------- pemetaan

    private fun JsonObject.toRetur(): Retur? {
        val id = optString("id") ?: return null
        val status = StatusRetur.dari(optString("status")) ?: return null
        val sj = optJsonObject("surat_jalan_pengganti")
        return Retur(
            id = id,
            nomorRetur = optString("nomor_retur") ?: "-",
            outletId = optString("outlet_id") ?: return null,
            outletName = optJsonObject("outlets")?.optString("name"),
            status = status,
            createdAt = optString("created_at"),
            pembuatNama = optJsonObject("created_by_staff")?.optString("name"),
            catatanManager = optString("catatan_manager"),
            jenisLogistik = JenisLogistik.dari(optString("jenis_logistik")),
            nomorResi = optString("nomor_resi_order"),
            driverNama = optString("driver_nama"),
            driverKontak = optString("driver_kontak"),
            driverPlat = optString("driver_plat_kendaraan"),
            fotoSerahTerimaUrl = optString("foto_serah_terima_url"),
            catatanKitchen = optString("catatan_kitchen"),
            suratJalanPenggantiId = sj?.optString("id"),
            nomorSuratJalanPengganti = sj?.optString("document_number"),
            items = optJsonArray("items")?.mapNotNull { it.asJsonObject.toReturItem() } ?: emptyList(),
        )
    }

    private fun JsonObject.toReturItem(): ReturItem? {
        val bb = optJsonObject("bahan_baku")
        return ReturItem(
            id = optString("id") ?: return null,
            bahanBakuId = optString("bahan_baku_id") ?: return null,
            namaBahan = bb?.optString("nama"),
            satuan = bb?.optString("satuan"),
            qtyKlaim = optDouble("qty_klaim") ?: 0.0,
            qtyDiterimaKitchen = optDouble("qty_diterima_kitchen"),
            fotoFisikUrl = optString("foto_fisik_url"),
            fotoTimbanganUrl = optString("foto_timbangan_url"),
            alasan = optString("alasan") ?: "-",
            catatan = optString("catatan"),
        )
    }
}
