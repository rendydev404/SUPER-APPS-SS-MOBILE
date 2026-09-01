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
        // Kolom PII (bank_name/bank_account_number/bank_account_holder, no_pks, dst) SENGAJA
        // tidak diminta di sini — belum ada layar di app ini yang membacanya, dan
        // HttpLoggingInterceptor jalan di Level.BODY untuk build debug (core/network
        // SupabaseClient.kt), jadi kolom rekening bank bisa tertulis penuh ke logcat kalau
        // ikut diminta. Tambahkan kembali di sub-proyek yang benar-benar merender kolom itu
        // (data minimisation).
        val row = Postgrest.selectOne(
            "mitra_profiles",
            listOf(
                "user_id" to "eq.$userId",
                "select" to "id,user_id,nama_mitra,outlet_ids,status"
            )
        ) ?: return null
        return MitraProfile.fromRow(row)
    }
}
