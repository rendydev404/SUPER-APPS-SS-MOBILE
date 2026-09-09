package com.sukashawarma.superapp.feature.stok.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optBoolean
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optJsonArray
import com.sukashawarma.superapp.data.remote.optJsonObject
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.feature.stok.data.model.OpnameHeader
import com.sukashawarma.superapp.feature.stok.data.model.OpnameItemDetail
import com.sukashawarma.superapp.feature.stok.data.model.StatusOpname
import com.sukashawarma.superapp.feature.stok.domain.KeputusanDraftOpname
import com.sukashawarma.superapp.feature.stok.domain.MasukanBerjenjang
import com.sukashawarma.superapp.feature.stok.domain.parseCatatanOpname
import com.sukashawarma.superapp.feature.stok.domain.OpnameTanggal
import com.sukashawarma.superapp.feature.stok.domain.putuskanDraftOpname
import com.sukashawarma.superapp.feature.stok.domain.UnitMeta
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

/**
 * Stock opname — cermin `hooks/useOpname.ts` dan `app/actions/opname.ts` di web.
 *
 * Web menyimpan item lewat service-role dengan alasan "RLS opname_item tidak
 * mengizinkan INSERT dari crew". Komentar itu sudah usang: policy
 * `opname_item_write` (FOR ALL TO authenticated) mengizinkannya selama
 * `opname.status = 'draft'` dan outletnya ada di `accessible_outlet_ids()`.
 * Native menulis langsung di bawah RLS, tanpa service-role.
 *
 * Konsekuensinya satu: native hanya bisa menyimpan item selama opname masih
 * `draft`. Web bisa juga saat `pending_approval` karena mem-bypass RLS. Itu
 * perbedaan yang disengaja — mengejarnya berarti menaruh service key di dalam APK.
 */
object OpnameRepository {

    private val WIB: ZoneId = ZoneId.of("Asia/Jakarta")

    /** Tanggal hari ini menurut WIB, format `YYYY-MM-DD`. */
    fun hariIniWIB(): String = LocalDate.now(WIB).toString()

    /**
     * Tanggal yang dipakai untuk opname hari ini — cermin `getEffectiveTodayWIB` web.
     *
     * Umumnya sama dengan [hariIniWIB]. Berbeda hanya bila outlet ini punya aturan
     * pengalihan yang sedang berlaku DAN kuota opname pada tanggal tujuannya belum
     * terpenuhi; lihat [OpnameTanggal] untuk alasan aturan itu ada.
     *
     * Kegagalan membaca jumlah finalized sengaja jatuh kembali ke tanggal hari ini,
     * bukan melempar: opname pekerjaan harian yang tidak boleh terhenti gara-gara satu
     * query tambahan gagal, dan tanggal hari ini jawaban yang benar untuk hampir semua
     * outlet pada hampir semua hari.
     */
    private suspend fun tanggalEfektif(outletId: String): String {
        val hariIni = hariIniWIB()
        val aturan = OpnameTanggal.pengalihanUntuk(outletId, hariIni) ?: return hariIni
        val sudahFinal = runCatching {
            jumlahOpname(outletId, aturan.tanggalTujuan, hanyaFinalized = true)
        }.getOrElse { return hariIni }
        return if (sudahFinal < aturan.minimalFinalized) aturan.tanggalTujuan else hariIni
    }

    /**
     * Menghitung opname satu outlet pada satu tanggal.
     *
     * Barisnya ditarik lalu dihitung, bukan lewat `count` PostgREST: satu outlet pada
     * satu tanggal hanya punya segelintir opname, dan `select id` sudah sekecil itu —
     * menambah pembacaan header `Content-Range` ke lapisan jaringan tidak menghemat apa
     * pun yang terasa.
     */
    private suspend fun jumlahOpname(
        outletId: String,
        tanggal: String,
        hanyaFinalized: Boolean,
    ): Int = Postgrest.select(
        "opname",
        listOf(
            "select" to "id",
            "outlet_id" to "eq.$outletId",
            "tanggal" to "eq.$tanggal",
            if (hanyaFinalized) "status" to "eq.finalized" else "status" to "neq.rejected",
        ),
    ).size()

    // -------------------------------------------------------------- pembacaan

    suspend fun daftar(outletId: String): List<OpnameHeader> = Postgrest.select(
        "opname",
        listOf(
            "select" to "id,outlet_id,tanggal,tipe,status,created_by,created_at," +
                "outlet_staff!opname_created_by_fkey(name)," +
                "opname_item(qty_fisik,qty_system,selisih,flagged)",
            "outlet_id" to "eq.$outletId",
            "order" to "tanggal.desc",
            "limit" to "60",
        ),
    ).mapNotNull { it.asJsonObject.toHeader() }

    /** Opname yang menunggu persetujuan, dibatasi outlet yang boleh diakses. */
    suspend fun menungguPersetujuan(outletIds: List<String>): List<OpnameHeader> {
        if (outletIds.isEmpty()) return emptyList()
        return Postgrest.select(
            "opname",
            listOf(
                "select" to "id,outlet_id,tanggal,tipe,status,created_by,created_at," +
                    "outlets(name),outlet_staff!opname_created_by_fkey(name)," +
                    "opname_item(qty_fisik,qty_system,selisih,flagged)",
                "status" to "eq.pending_approval",
                "outlet_id" to "in.(${outletIds.joinToString(",")})",
                "order" to "created_at.desc",
            ),
        ).mapNotNull { it.asJsonObject.toHeader() }
    }

    private fun JsonObject.toHeader(): OpnameHeader? {
        val id = optString("id") ?: return null
        val outletId = optString("outlet_id") ?: return null
        val items = optJsonArray("opname_item") ?: JsonArray()
        return OpnameHeader(
            id = id,
            outletId = outletId,
            tanggal = optString("tanggal"),
            tipe = optString("tipe"),
            status = StatusOpname.dari(optString("status")),
            createdBy = optString("created_by"),
            createdAt = optString("created_at"),
            updatedAt = optString("updated_at"),
            outletName = optJsonObject("outlets")?.optString("name"),
            creatorName = optJsonObject("outlet_staff")?.optString("name"),
            jumlahItem = items.size(),
            jumlahFlagged = items.count { it.asJsonObject.optBoolean("flagged") },
        )
    }

    /**
     * Draft yang masih berjalan hari ini, APA PUN tipenya.
     *
     * Tanpa filter tipe, dan itu inti perbaikannya: draft `ad_hoc` yang lahir dari
     * opname kedua tidak akan pernah terlihat oleh pencarian bertipe `harian`. Cermin
     * `fetchTodayOpnameDraftAction` web, yang juga hanya menyaring `status='draft'`.
     */
    private suspend fun draftBerjalan(outletId: String, tanggal: String): OpnameHeader? =
        Postgrest.selectOne(
            "opname",
            listOf(
                "select" to "id,outlet_id,tanggal,tipe,status,created_by,created_at,updated_at",
                "outlet_id" to "eq.$outletId",
                "tanggal" to "eq.$tanggal",
                "status" to "eq.draft",
                "order" to "created_at.desc",
            ),
        )?.toHeader()

    /**
     * Ambil draft hari ini bila ada, atau buat baru.
     *
     * Pencarian mengabaikan opname berstatus `rejected` — opname yang ditolak berarti
     * kru harus menghitung ulang, jadi tidak boleh dipakai ulang sebagai draft.
     */
    suspend fun buatAtauPakaiDraft(
        outletId: String,
        tipe: String,
        createdBy: String,
        catatan: String? = null,
    ): OpnameHeader {
        val tanggal = tanggalEfektif(outletId)

        // Draft berjalan dicari LEBIH DULU dan tanpa memandang tipe. Urutan ini yang
        // membuat "Simpan Draft lalu lanjutkan" bekerja; lihat [putuskanDraftOpname].
        val berjalan = draftBerjalan(outletId, tanggal)

        val sudahAda = if (berjalan != null) null else Postgrest.selectOne(
            "opname",
            listOf(
                "select" to "id,outlet_id,tanggal,tipe,status,created_by,created_at,updated_at",
                "outlet_id" to "eq.$outletId",
                "tipe" to "eq.$tipe",
                "tanggal" to "eq.$tanggal",
                "status" to "neq.rejected",
            ),
        )?.toHeader()

        val keputusan = putuskanDraftOpname(
            tipe = tipe,
            draftBerjalanId = berjalan?.id,
            sudahAdaId = sudahAda?.id,
            sudahAdaFinal = sudahAda?.status == StatusOpname.FINALIZED,
            // Dihitung hanya bila memang menentukan, supaya tidak menambah satu
            // permintaan jaringan pada jalur yang paling sering ditempuh.
            jumlahHariIni = if (sudahAda?.status == StatusOpname.FINALIZED) {
                jumlahOpname(outletId, tanggal, hanyaFinalized = false)
            } else {
                0
            },
            maksimalHariIni = OpnameTanggal.maksimalOpname(outletId, tanggal),
        )

        return when (keputusan) {
            is KeputusanDraftOpname.Lanjutkan -> berjalan!!
            is KeputusanDraftOpname.Tampilkan -> sudahAda!!
            is KeputusanDraftOpname.BuatBaru ->
                buatBaru(outletId, keputusan.tipe, createdBy, catatan, tanggal)
        }
    }

    private suspend fun buatBaru(
        outletId: String,
        tipe: String,
        createdBy: String,
        catatan: String?,
        tanggal: String,
    ): OpnameHeader {
        val body = JsonObject().apply {
            addProperty("outlet_id", outletId)
            addProperty("tipe", tipe)
            addProperty("status", "draft")
            addProperty("created_by", createdBy)
            addProperty("tanggal", tanggal)
            if (catatan.isNullOrBlank()) add("notes", com.google.gson.JsonNull.INSTANCE)
            else addProperty("notes", catatan)
        }
        val hasil = Postgrest.insert("opname", body)
        val row = hasil.firstOrNull()?.asJsonObject ?: error("Gagal membuat draft opname")
        return row.toHeader() ?: error("Draft opname tidak terbaca")
    }

    // -------------------------------------------------------------- penulisan

    data class ItemSimpan(
        val opnameId: String,
        val bahanBakuId: String,
        val qtyFisik: Double,
        val qtySystem: Double,
        val flagged: Boolean,
        val catatan: String? = null,
    )

    /**
     * Simpan item hitung fisik.
     *
     * `selisih` sengaja TIDAK dikirim: kolom itu dihitung database dari
     * `qty_fisik - qty_system`. Mengirimnya sendiri berisiko berbeda dari
     * perhitungan database.
     */
    suspend fun simpanItem(items: List<ItemSimpan>) {
        if (items.isEmpty()) return
        val body = JsonArray()
        items.forEach { item ->
            body.add(
                JsonObject().apply {
                    addProperty("opname_id", item.opnameId)
                    addProperty("bahan_baku_id", item.bahanBakuId)
                    addProperty("qty_fisik", item.qtyFisik)
                    addProperty("qty_system", item.qtySystem)
                    addProperty("flagged", item.flagged)
                    if (item.catatan.isNullOrBlank()) add("catatan", com.google.gson.JsonNull.INSTANCE)
                    else addProperty("catatan", item.catatan)
                }
            )
        }
        Postgrest.upsert("opname_item", body, onConflict = "opname_id,bahan_baku_id")
    }

    /**
     * Ajukan opname untuk disetujui leader. Dipakai bila ada item yang ditandai.
     *
     * Definisi RPC ini tidak ada di repository migration web (hanya di database
     * live), jadi kegagalannya ditangani sebagai pesan yang bisa dibaca pengguna,
     * bukan crash.
     */
    suspend fun ajukanPersetujuan(opnameId: String) {
        Postgrest.rpc("set_opname_pending", JsonObject().apply {
            addProperty("p_opname_id", opnameId)
        })
    }

    /**
     * Apakah opname ini sudah difinalisasi.
     *
     * Dipakai antrean offline untuk membedakan dua kegagalan yang tampak sama: kiriman
     * yang benar-benar belum sampai, dan kiriman yang sebenarnya SUDAH diterima server
     * tetapi balasannya hilang di jalan. Tanpa pembedaan ini baris kedua akan terus
     * dicoba selamanya.
     *
     * Gagal membaca dijawab `false` — menganggapnya belum selesai berarti antreannya
     * dipertahankan, dan itu arah salah yang lebih aman.
     */
    suspend fun sudahFinalized(opnameId: String): Boolean = runCatching {
        Postgrest.selectOne(
            "opname",
            listOf("select" to "status", "id" to "eq.$opnameId"),
        )?.optString("status") == StatusOpname.FINALIZED.nilai
    }.getOrDefault(false)

    /** Finalisasi langsung — menulis `opname_selisih` ke ledger lewat trigger. */
    suspend fun finalisasi(opnameId: String) {
        Postgrest.rpc("finalize_opname", JsonObject().apply {
            addProperty("p_opname_id", opnameId)
        })
    }

    suspend fun setujui(opnameId: String, approvedBy: String) {
        Postgrest.rpc("approve_opname", JsonObject().apply {
            addProperty("p_opname_id", opnameId)
            addProperty("p_approved_by", approvedBy)
        })
    }

    suspend fun tolak(opnameId: String, rejectedBy: String, alasan: String) {
        Postgrest.rpc("reject_opname", JsonObject().apply {
            addProperty("p_opname_id", opnameId)
            addProperty("p_rejected_by", rejectedBy)
            addProperty("p_reason", alasan)
        })
    }

    /**
     * Satu baris draft yang sudah tersimpan.
     *
     * [masukan] null berarti barisnya ditulis SEBELUM native menyimpan masukan mentah
     * (atau oleh klien lain yang tidak menulisnya). Draft seperti itu tetap bisa
     * dilanjutkan, hanya saja pemulihannya jatuh ke [qtyFisik] pada satuan terkecil —
     * lihat penanganannya di OpnameViewModel.
     */
    data class ItemDraft(val qtyFisik: Double, val masukan: MasukanBerjenjang?)

    /** Item yang sudah tersimpan pada satu opname, untuk melanjutkan draft. */
    suspend fun itemTersimpan(opnameId: String): Map<String, ItemDraft> = Postgrest.select(
        "opname_item",
        listOf(
            "select" to "bahan_baku_id,qty_fisik,catatan",
            "opname_id" to "eq.$opnameId",
        ),
    ).mapNotNull { el ->
        val o = el.asJsonObject
        val id = o.optString("bahan_baku_id") ?: return@mapNotNull null
        val qty = o.optDouble("qty_fisik") ?: return@mapNotNull null
        id to ItemDraft(qty, parseCatatanOpname(o.optString("catatan")).masukan)
    }.toMap()

    // ------------------------------------------------------------------ detail
    //
    // Tiga pembacaan di bawah menyusun layar Detail Opname — cermin
    // `components/stok/OpnameDetail.tsx`. Web membacanya dengan browser client
    // biasa, jadi tidak ada satu pun yang butuh service-role: `opname_item_read`
    // membuka baris lewat outlet induknya, `ledger_read` dan `bahan_baku_read`
    // sudah terbuka untuk `authenticated`.

    /**
     * Satu opname beserta pembuat dan catatan persetujuannya.
     *
     * Tidak ada filter `outlet_id` di sini, sama seperti web — RLS `opname_read`
     * adalah satu-satunya gerbangnya, dan id di luar cakupan hanya mengembalikan
     * baris kosong.
     */
    suspend fun kepala(opnameId: String): JsonObject? = Postgrest.selectOne(
        "opname",
        listOf(
            "select" to "id,outlet_id,tanggal,tipe,status,created_by,created_at,notes," +
                "approval_notes,outlet_staff!opname_created_by_fkey(name)",
            "id" to "eq.$opnameId",
        ),
    )

    /**
     * Baris-baris hitung satu opname.
     *
     * `selisih` ikut dibaca, bukan dihitung ulang: kolom itu generated stored di
     * database, jadi nilainya adalah kebenaran yang sama dengan yang dilihat web.
     */
    suspend fun detailItem(opnameId: String): List<OpnameItemDetail> = Postgrest.select(
        "opname_item",
        listOf(
            "select" to "id,bahan_baku_id,qty_fisik,qty_system,selisih,flagged,catatan," +
                "bahan_baku(nama,satuan,satuan_tengah,faktor_tengah,satuan_kecil,faktor_tampilan)",
            "opname_id" to "eq.$opnameId",
        ),
    ).mapNotNull { el ->
        val o = el.asJsonObject
        val id = o.optString("id") ?: return@mapNotNull null
        val bahanId = o.optString("bahan_baku_id") ?: return@mapNotNull null
        val bahan = o.optJsonObject("bahan_baku")
        OpnameItemDetail(
            id = id,
            bahanBakuId = bahanId,
            namaBahan = bahan?.optString("nama"),
            meta = UnitMeta(
                satuan = bahan?.optString("satuan"),
                satuanTengah = bahan?.optString("satuan_tengah"),
                satuanKecil = bahan?.optString("satuan_kecil"),
                faktorTengah = bahan?.optDouble("faktor_tengah"),
                faktorTampilan = bahan?.optDouble("faktor_tampilan"),
            ),
            qtySystem = o.optDouble("qty_system") ?: 0.0,
            // Dibiarkan null bila kolomnya null — "belum terhitung" bukan nol.
            qtyFisik = o.optDouble("qty_fisik"),
            selisih = o.optDouble("selisih") ?: 0.0,
            flagged = o.optBoolean("flagged") ?: false,
            catatan = o.optString("catatan")?.takeIf { it.isNotBlank() },
        )
    }

    /**
     * Pemakaian resep (BOM) pada tanggal opname, dijumlah per bahan.
     *
     * Rentang waktunya memakai offset `+07:00` apa adanya seperti web, bukan UTC:
     * `opname.tanggal` adalah tanggal WIB, jadi menggesernya ke UTC akan menarik
     * pemakaian dari hari yang salah selama tujuh jam pertama.
     *
     * `qty` pemakaian tersimpan negatif; diambil nilai mutlaknya supaya terbaca
     * sebagai "terpakai sekian", sama seperti `usageMap` di web.
     */
    suspend fun pemakaianHarian(outletId: String, tanggal: String): Map<String, Double> =
        Postgrest.select(
            "ledger_stok",
            listOf(
                "select" to "bahan_baku_id,qty",
                "outlet_id" to "eq.$outletId",
                "tipe" to "eq.pemakaian",
                "created_at" to "gte.${tanggal}T00:00:00+07:00",
                "created_at" to "lte.${tanggal}T23:59:59+07:00",
            ),
        ).mapNotNull { el ->
            val o = el.asJsonObject
            val id = o.optString("bahan_baku_id") ?: return@mapNotNull null
            id to abs(o.optDouble("qty") ?: 0.0)
        }.groupBy({ it.first }, { it.second })
            .mapValues { (_, nilai) -> nilai.sum() }

    /**
     * Master bahan aktif, untuk menentukan bahan mana yang terlewat dihitung.
     *
     * Daftar penuh memang diperlukan: bahan yang "belum dihitung" justru dikenali
     * dari ketiadaan barisnya di `opname_item`, jadi tidak bisa disimpulkan dari
     * hasil [detailItem] saja.
     */
    suspend fun bahanAktifRingkas(): List<BahanAktifRingkas> = Postgrest.select(
        "bahan_baku",
        listOf(
            "select" to "id,nama,kategori",
            "is_active" to "eq.true",
            "order" to "nama.asc",
        ),
    ).mapNotNull { el ->
        val o = el.asJsonObject
        val id = o.optString("id") ?: return@mapNotNull null
        BahanAktifRingkas(
            id = id,
            nama = o.optString("nama")?.takeIf { it.isNotBlank() } ?: "(tanpa nama)",
            kategori = o.optString("kategori")?.takeIf { it.isNotBlank() } ?: "Lainnya",
        )
    }
}

/** Baris master bahan seperlunya untuk tab "Belum Dihitung" di Detail Opname. */
data class BahanAktifRingkas(val id: String, val nama: String, val kategori: String)
