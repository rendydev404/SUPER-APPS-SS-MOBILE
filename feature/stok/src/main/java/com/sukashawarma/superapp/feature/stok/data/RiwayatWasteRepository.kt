package com.sukashawarma.superapp.feature.stok.data

import com.sukashawarma.superapp.data.remote.*
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.stok.data.model.StatusWaste
import com.sukashawarma.superapp.feature.stok.domain.UnitMeta
import com.sukashawarma.superapp.feature.stok.domain.decomposeTriUnit
import com.sukashawarma.superapp.feature.stok.domain.formatAngkaStok

/**
 * Satu laporan waste milik pengguna yang sedang masuk, beserta hasil keputusannya.
 */
data class RiwayatWaste(
    val id: String,
    val namaBahan: String,
    val qty: Double,
    val alasan: String,
    val status: StatusWaste,
    val alasanPenolakan: String?,
    val fotoUrl: String?,
    val createdAt: String?,
    val meta: UnitMeta,
) {
    /**
     * Qty berjenjang, mis. "1 Kg · 250 Gram".
     *
     * `stok_waste_reports.qty` SELALU pada satuan besar — `WasteModal` web membagi
     * masukan pengguna dengan `faktor_tengah`/`faktor_tampilan` sebelum menyimpan,
     * dan trigger persetujuan menulis `-NEW.qty` apa adanya ke ledger. Karena itu
     * `saldoIsGram` di bawah harus `false`; memakai varian gram akan salah sebesar
     * faktor_tampilan. Bandingkan dengan `OpnameItemDetail`, yang justru sebaliknya.
     *
     * Halaman web mencetak angka desimal mentah (`{r.qty} {satuan}`) sehingga 0,25 Kg
     * tampil "0.25 Kg" alih-alih "250 Gram". Di sini sengaja dipakai formatter
     * berjenjang yang sama dengan layar persetujuan waste — orang yang melapor dan
     * orang yang menyetujui jadi membaca angka yang bentuknya sama.
     */
    val qtyLabel: String
        get() {
            val parts = decomposeTriUnit(
                qty, false, meta.satuanTengah, meta.faktorTengah, meta.satuanKecil, meta.faktorTampilan,
            )
            return listOf(
                parts.besar to meta.satuan,
                parts.tengah to meta.satuanTengah,
                parts.kecil to meta.satuanKecil,
            ).filter { it.first != 0.0 && !it.second.isNullOrBlank() }
                .joinToString(" · ") { "${formatAngkaStok(it.first)} ${it.second}" }
                .ifBlank { "0 ${meta.satuan.orEmpty()}" }
        }
}

/**
 * Riwayat waste pelapor — cermin `fetchMyWasteReports` di `app/actions/waste.ts`.
 *
 * Web menariknya lewat Server Action ber-service-role, tetapi tidak ada yang perlu
 * ditiru dari situ: policy `waste_reports_read` sudah membuka baris yang outletnya
 * masuk `accessible_outlet_ids()`, dan setiap laporan milik pengguna pasti lolos —
 * kebijakan INSERT-nya menuntut hal yang sama saat laporan dibuat.
 *
 * Yang WAJIB ada justru filter `reported_by`. RLS sendirian akan mengembalikan
 * laporan seluruh outlet, yaitu persis yang diandalkan layar persetujuan; tanpa
 * filter ini layar riwayat akan membocorkan laporan rekan kerja.
 */
object RiwayatWasteRepository {

    private const val KOLOM =
        "id,qty,reason,status,rejection_reason,photo_url,created_at," +
            "bahan_baku(nama,satuan,satuan_tengah,faktor_tengah,satuan_kecil,faktor_tampilan)"

    suspend fun milikSaya(): List<RiwayatWaste> {
        val staffId = AppSession.staff.value?.id
        check(!staffId.isNullOrBlank()) { "Sesi tidak ditemukan. Masuk ulang untuk melihat riwayat." }

        return Postgrest.select(
            "stok_waste_reports",
            listOf(
                "select" to KOLOM,
                "reported_by" to "eq.$staffId",
                "order" to "created_at.desc",
                "limit" to "50",
            ),
        ).mapNotNull { el ->
            val row = el.asJsonObject
            val id = row.optString("id") ?: return@mapNotNull null
            val bahan = row.optJsonObject("bahan_baku")
            RiwayatWaste(
                id = id,
                namaBahan = bahan?.optString("nama")?.takeIf { it.isNotBlank() } ?: "(bahan dihapus)",
                qty = row.optDouble("qty") ?: 0.0,
                alasan = row.optString("reason").orEmpty(),
                status = StatusWaste.dari(row.optString("status")),
                alasanPenolakan = row.optString("rejection_reason")?.takeIf { it.isNotBlank() },
                fotoUrl = row.optString("photo_url")?.takeIf { it.isNotBlank() },
                createdAt = row.optString("created_at"),
                meta = UnitMeta(
                    satuan = bahan?.optString("satuan"),
                    satuanTengah = bahan?.optString("satuan_tengah"),
                    satuanKecil = bahan?.optString("satuan_kecil"),
                    faktorTengah = bahan?.optDouble("faktor_tengah"),
                    faktorTampilan = bahan?.optDouble("faktor_tampilan"),
                ),
            )
        }
    }
}
