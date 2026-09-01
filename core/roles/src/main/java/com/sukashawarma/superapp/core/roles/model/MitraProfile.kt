package com.sukashawarma.superapp.domain.model

import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optJsonArray
import com.sukashawarma.superapp.data.remote.optString

/**
 * Profil kemitraan — cermin tabel `mitra_profiles` yang dipakai portal mitra web
 * (apps/admin-dashboard/src/app/dashboard/mitra). Sumber kebenaran outlet milik
 * mitra adalah `outlet_ids` DI SINI, bukan `outlet_staff.outlet_id`.
 */
data class MitraProfile(
    val id: String,
    val userId: String,
    val namaMitra: String,
    /** Jamak walau semua mitra hari ini punya satu outlet — kolom DB-nya memang array. */
    val outletIds: List<String>,
    val profitSharingPct: Double?,
    val bankName: String?,
    val bankAccountNumber: String?,
    val bankAccountHolder: String?,
    val noPks: String?,
    val tanggalPks: String?,
    val tanggalBerakhirPks: String?,
    val status: String,
) {
    /** DB memakai bahasa Indonesia di kolom ini ('aktif'), bukan 'active' seperti outlet_staff. */
    val isAktif: Boolean get() = status == "aktif"

    companion object {
        /** Kembalikan null bila baris tak punya identitas — lebih baik "tanpa profil"
         *  daripada objek setengah jadi yang bocor ke layar sebagai data kosong. */
        fun fromRow(row: JsonObject): MitraProfile? {
            val id = row.optString("id") ?: return null
            val userId = row.optString("user_id") ?: return null
            return MitraProfile(
                id = id,
                userId = userId,
                namaMitra = row.optString("nama_mitra") ?: "Mitra",
                outletIds = row.optJsonArray("outlet_ids")
                    ?.mapNotNull { el -> el.takeIf { !it.isJsonNull }?.asString }
                    ?: emptyList(),
                profitSharingPct = row.optDouble("profit_sharing_pct"),
                bankName = row.optString("bank_name"),
                bankAccountNumber = row.optString("bank_account_number"),
                bankAccountHolder = row.optString("bank_account_holder"),
                noPks = row.optString("no_pks"),
                tanggalPks = row.optString("tanggal_pks"),
                tanggalBerakhirPks = row.optString("tanggal_berakhir_pks"),
                status = row.optString("status") ?: "",
            )
        }
    }
}
