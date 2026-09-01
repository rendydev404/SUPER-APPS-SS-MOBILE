package com.sukashawarma.superapp.data.repository

import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.domain.model.MitraProfile

/**
 * RLS `mitra_profiles_select_own` (`user_id = auth.uid()`) yang membatasi hasilnya,
 * jadi query ini aman dipanggil dengan JWT user biasa — tak perlu service role.
 */
object MitraRepository {

    /** null = user ini memang tidak punya profil mitra. Kegagalan jaringan dilempar
     *  sebagai exception, BUKAN diubah jadi null — pemanggil harus bisa membedakan
     *  "tidak terdaftar" dari "sinyal jelek". */
    suspend fun getProfile(userId: String): MitraProfile? {
        val row = Postgrest.selectOne(
            "mitra_profiles",
            listOf(
                "user_id" to "eq.$userId",
                "select" to "id,user_id,nama_mitra,outlet_ids,profit_sharing_pct," +
                    "bank_name,bank_account_number,bank_account_holder," +
                    "no_pks,tanggal_pks,tanggal_berakhir_pks,status"
            )
        ) ?: return null
        return MitraProfile.fromRow(row)
    }
}
