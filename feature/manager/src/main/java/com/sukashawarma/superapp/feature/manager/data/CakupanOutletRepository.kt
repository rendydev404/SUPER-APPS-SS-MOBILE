package com.sukashawarma.superapp.feature.manager.data

import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.manager.domain.CakupanOutlet
import com.sukashawarma.superapp.feature.manager.domain.cakupanUntuk

/**
 * Menentukan cakupan outlet pengguna yang sedang masuk.
 *
 * Membaca `staff_outlets` MILIK SENDIRI — itu justru yang diizinkan RLS
 * (`staff_outlets_select_self` memakai `staff_id = auth.uid()`). Yang tidak bisa
 * dibaca adalah pemetaan milik orang lain, dan itu masalah terpisah yang hanya
 * menyentuh penamaan zona AM di layar Overview.
 */
object CakupanOutletRepository {

    suspend fun cakupan(): CakupanOutlet {
        val staff = AppSession.staff.value
            // Tanpa sesi, tidak ada yang boleh dilihat. Mengembalikan `Semua` di sini
            // akan membuat layar menampilkan seluruh cabang justru pada keadaan yang
            // paling tidak jelas.
            ?: return CakupanOutlet.Binaan(emptySet())

        val binaan = try {
            outletBinaan(staff.id)
        } catch (e: Exception) {
            android.util.Log.e("CakupanOutletRepository", "outletBinaan() gagal", e)
            // Gagal membaca pemetaan bukan alasan untuk membuka seluruh cabang.
            emptySet()
        }
        return cakupanUntuk(staff.role, binaan)
    }

    private suspend fun outletBinaan(stafId: String): Set<String> =
        Postgrest.select(
            "staff_outlets",
            listOf("select" to "outlet_id", "staff_id" to "eq.$stafId"),
        ).mapNotNull { it.asJsonObject.optString("outlet_id") }.toSet()

    /**
     * Penyaring `outlet_id` untuk query PostgREST, atau daftar kosong bila pengguna
     * memegang seluruh cabang.
     *
     * Mengembalikan null untuk cakupan binaan yang KOSONG supaya pemanggil bisa
     * berhenti lebih awal: `in.()` tanpa isi adalah sintaks tak sah, dan mengirimnya
     * akan menghasilkan galat server, bukan hasil kosong.
     */
    fun filterOutlet(cakupan: CakupanOutlet, kolom: String = "id"): List<Pair<String, String>>? =
        when (cakupan) {
            CakupanOutlet.Semua -> emptyList()
            is CakupanOutlet.Binaan ->
                if (cakupan.ids.isEmpty()) null
                else listOf(kolom to "in.(${cakupan.ids.joinToString(",")})")
        }
}
