package com.sukashawarma.superapp.feature.stok.data

import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optJsonObject
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.stok.domain.BarisNilai
import com.sukashawarma.superapp.feature.stok.domain.StatusNilai
import com.sukashawarma.superapp.feature.stok.domain.UnitMeta

/**
 * Nilai persediaan — cermin `hooks/useNilaiPersediaan.ts`.
 *
 * Seluruh perhitungannya sudah dilakukan view `nilai_persediaan_spv`, termasuk
 * pemisahan tiga tingkat keyakinan. Native hanya membaca dan meringkas, sama
 * seperti web — menghitung ulang di sini justru berisiko berbeda dari angka yang
 * dilihat orang lain.
 */
object NilaiPersediaanRepository {

    private const val KOLOM =
        "outlet_id,outlet,outlet_type,bahan_baku_id,bahan,kategori,satuan,satuan_kecil," +
            "kemasan_qty,harga_beli,saldo,status,skala_pasti,jumlah_satuan_besar," +
            "nilai,nilai_min,nilai_max,updated_at"

    suspend fun muat(): List<BarisNilai> =
        allRows("nilai_persediaan_spv", listOf("select" to KOLOM)).mapNotNull { baris ->
            val outletId = baris.optString("outlet_id") ?: return@mapNotNull null
            val bahanId = baris.optString("bahan_baku_id") ?: return@mapNotNull null
            BarisNilai(
                outletId = outletId,
                outlet = baris.optString("outlet").orEmpty(),
                outletType = baris.optString("outlet_type"),
                bahanBakuId = bahanId,
                bahan = baris.optString("bahan").orEmpty(),
                kategori = baris.optString("kategori"),
                satuan = baris.optString("satuan"),
                saldo = baris.optDouble("saldo") ?: 0.0,
                hargaBeli = baris.optDouble("harga_beli"),
                status = StatusNilai.dari(baris.optString("status")),
                jumlahSatuanBesar = baris.optDouble("jumlah_satuan_besar") ?: 0.0,
                nilai = baris.optDouble("nilai") ?: 0.0,
                nilaiMin = baris.optDouble("nilai_min") ?: 0.0,
                nilaiMax = baris.optDouble("nilai_max") ?: 0.0,
            )
        }
}

/** Satu bahan pada layar pengaturan threshold. */
data class BarisThreshold(
    val bahanBakuId: String,
    val nama: String,
    val satuan: String,
    val kategori: String,
    val defaultReorderPoint: Double,
    /** null = outlet ini belum menimpa nilai bawaan. */
    val overrideOutlet: Double?,
) {
    val berlaku: Double get() = overrideOutlet ?: defaultReorderPoint
    val ditimpa: Boolean get() = overrideOutlet != null
}

/**
 * Pengaturan titik pesan ulang per outlet — cermin `lib/queries/threshold.ts`.
 *
 * Angka inilah yang dipakai seluruh status stok di layar monitoring, jadi
 * mengubahnya di sini langsung mengubah bahan mana yang dianggap kritis.
 */
object ThresholdRepository {

    suspend fun daftar(outletId: String): List<BarisThreshold> = Postgrest.select(
        "bahan_baku",
        listOf(
            "select" to "id,nama,satuan,kategori,default_reorder_point,outlet_reorder_point!left(reorder_point)",
            "is_active" to "eq.true",
            // Penyaring pada tabel embed, bukan pada `bahan_baku` — tanpa ini setiap
            // bahan akan membawa override milik SEMUA outlet.
            "outlet_reorder_point.outlet_id" to "eq.$outletId",
            "order" to "kategori,nama",
        ),
    ).mapNotNull { elemen ->
        val baris = elemen.asJsonObject
        val id = baris.optString("id") ?: return@mapNotNull null
        val override = baris.get("outlet_reorder_point")
            ?.takeIf { it.isJsonArray }
            ?.asJsonArray
            ?.firstOrNull()
            ?.asJsonObject
            ?.optDouble("reorder_point")
        BarisThreshold(
            bahanBakuId = id,
            nama = baris.optString("nama").orEmpty(),
            satuan = baris.optString("satuan").orEmpty(),
            kategori = baris.optString("kategori")?.takeIf { it.isNotBlank() } ?: "Lainnya",
            defaultReorderPoint = baris.optDouble("default_reorder_point") ?: 0.0,
            overrideOutlet = override,
        )
    }

    suspend fun simpan(outletId: String, bahanBakuId: String, nilai: Double) {
        val pengguna = AppSession.staff.value?.id
        Postgrest.upsert(
            "outlet_reorder_point",
            com.google.gson.JsonArray().apply {
                add(
                    JsonObject().apply {
                        addProperty("outlet_id", outletId)
                        addProperty("bahan_baku_id", bahanBakuId)
                        addProperty("reorder_point", nilai)
                        addProperty("updated_by", pengguna)
                    }
                )
            },
            onConflict = "outlet_id,bahan_baku_id",
        )
    }

    /** Menghapus override supaya outlet kembali memakai `default_reorder_point`. */
    suspend fun kembalikanKeBawaan(outletId: String, bahanBakuId: String) {
        Postgrest.delete(
            "outlet_reorder_point",
            listOf("outlet_id" to "eq.$outletId", "bahan_baku_id" to "eq.$bahanBakuId"),
        )
    }
}

/** Satu kejadian arus barang gudang pusat. */
data class BarisArusBarang(
    val id: String,
    val tipe: String,
    val sumber: String?,
    val kategori: String,
    val qty: Double,
    val hargaSatuan: Double?,
    val saldoSesudah: Double?,
    val catatan: String?,
    val createdAt: String?,
    val pencatat: String?,
    val bahanNama: String,
    val meta: UnitMeta,
    val tujuanOutlet: String?,
    val nomorSj: String?,
    val nomorPo: String?,
    val supplier: String?,
) {
    /** `masuk` menambah saldo gudang, sisanya mengurangi. */
    val masuk: Boolean get() = qty > 0
}

/**
 * Inbound / Outbound gudang pusat — cermin `hooks/useInboundOutbound.ts`.
 *
 * Sumbernya view `inbound_outbound_feed` yang sudah meratakan ledger, surat
 * jalan, dan PO menjadi satu arus kejadian. Web membacanya dengan sesi pengguna
 * biasa, jadi native tidak butuh perlakuan khusus.
 */
object ArusBarangRepository {

    private const val KOLOM =
        "id,outlet_id,bahan_baku_id,tipe,sumber,kategori,qty,harga_satuan,saldo_sesudah," +
            "catatan,created_by,created_at,pencatat_nama,bahan_nama,bahan_satuan," +
            "satuan_tengah,faktor_tengah,satuan_kecil,faktor_tampilan,satuan_distribusi," +
            "ref_shipment_id,tujuan_outlet_nama,nomor_sj,nomor_po,supplier_nama"

    const val PAGE_SIZE = 100

    suspend fun daftar(outletId: String, halaman: Int = 0): List<BarisArusBarang> =
        Postgrest.select(
            "inbound_outbound_feed",
            listOf(
                "select" to KOLOM,
                "outlet_id" to "eq.$outletId",
                "order" to "created_at.desc",
                "limit" to PAGE_SIZE.toString(),
                "offset" to (halaman * PAGE_SIZE).toString(),
            ),
        ).mapNotNull { elemen ->
            val baris = elemen.asJsonObject
            val id = baris.optString("id") ?: return@mapNotNull null
            BarisArusBarang(
                id = id,
                tipe = baris.optString("tipe").orEmpty(),
                sumber = baris.optString("sumber"),
                kategori = baris.optString("kategori").orEmpty(),
                qty = baris.optDouble("qty") ?: 0.0,
                hargaSatuan = baris.optDouble("harga_satuan"),
                saldoSesudah = baris.optDouble("saldo_sesudah"),
                catatan = baris.optString("catatan"),
                createdAt = baris.optString("created_at"),
                pencatat = baris.optString("pencatat_nama"),
                bahanNama = baris.optString("bahan_nama").orEmpty(),
                meta = UnitMeta(
                    satuan = baris.optString("bahan_satuan"),
                    satuanTengah = baris.optString("satuan_tengah"),
                    satuanKecil = baris.optString("satuan_kecil"),
                    faktorTengah = baris.optDouble("faktor_tengah"),
                    faktorTampilan = baris.optDouble("faktor_tampilan"),
                ),
                tujuanOutlet = baris.optString("tujuan_outlet_nama"),
                nomorSj = baris.optString("nomor_sj"),
                nomorPo = baris.optString("nomor_po"),
                supplier = baris.optString("supplier_nama"),
            )
        }
}

/** Ringkasan plafon belanja satu outlet. */
data class BudgetOutlet(
    val outletId: String,
    val outletName: String,
    val region: String,
    val nominal: Double,
    val terpakai: Double,
    val sisa: Double,
    val periodType: String?,
    val customDays: Int?,
    val hasConfig: Boolean,
) {
    val persen: Double get() = if (nominal > 0) terpakai / nominal * 100.0 else 0.0
}

/**
 * Plafon & belanja seluruh outlet — cermin `getAllOutletsBudgetStatus` web.
 *
 * Web memanggil `get_outlet_budget_status` dengan service-role; native memakai
 * varian `_scoped` yang sudah dipakai layar Permintaan dan memeriksa cakupan
 * outlet pemanggil sendiri.
 */
object BudgetOutletRepository {

    /** Outlet non-operasional yang disembunyikan web dari halaman ini. */
    private val DISEMBUNYIKAN = setOf(
        "GUDANG PUSAT (HQ)", "KANTOR PUSAT", "Shopee", "TikTok Shop",
    )

    suspend fun daftar(): List<BudgetOutlet> {
        val outlets = StokRepository.accessibleOutlets()
            .filter { it.name !in DISEMBUNYIKAN }
        return outlets.map { outlet ->
            val status = try {
                PermintaanRepository.budgetStatus(outlet.id)
            } catch (e: Exception) {
                android.util.Log.e("BudgetOutletRepository", "budgetStatus(${outlet.id}) gagal", e)
                null
            }
            BudgetOutlet(
                outletId = outlet.id,
                outletName = outlet.name,
                region = "-",
                nominal = status?.nominal ?: 0.0,
                terpakai = status?.terpakai ?: 0.0,
                sisa = status?.sisa ?: 0.0,
                periodType = status?.periodType,
                customDays = status?.customDays,
                // Outlet yang RPC-nya gagal dibedakan dari yang memang belum
                // dikonfigurasi: keduanya tampil, tetapi hanya yang kedua yang
                // boleh terbaca sebagai "plafon nol".
                hasConfig = status?.hasConfig ?: false,
            )
        }.sortedBy { it.outletName }
    }
}
