package com.sukashawarma.superapp.feature.manager.domain

import com.sukashawarma.superapp.domain.model.Role

/**
 * Menentukan zona (nama area manager) sebuah outlet. Cermin `getAMName` dan
 * `isIgnoredAM` di `app/page.tsx` web.
 *
 * Zona inilah yang mengelompokkan daftar "Status Outlet" dan mengisi tabel
 * "Performa Zona AM", jadi hasilnya harus sama persis dengan web — outlet yang
 * di web masuk zona "Muchtar" tidak boleh muncul di zona lain di HP.
 *
 * Daftar pencocokan nama di bawah adalah CADANGAN, sama seperti di web: yang
 * dipakai lebih dulu adalah pemetaan nyata dari `staff_outlets`. Bedanya, web
 * membacanya dengan service-role key sehingga selalu lengkap, sedangkan native
 * membacanya dengan JWT pengguna — dan RLS `staff_outlets_select_self` hanya
 * memberi baris milik pengguna sendiri. Untuk area manager itu cukup (ia hanya
 * perlu namanya sendiri, lihat cabang pertama fungsi). Untuk regional manager
 * pemetaannya datang kosong dan seluruh outlet jatuh ke daftar cadangan ini.
 */
object AreaManagerNama {

    /**
     * Nama yang bukan zona sungguhan: akun uji, akun sistem, dan label penampung.
     * Outlet dengan zona seperti ini dibuang dari ranking maupun tabel zona, persis
     * seperti web — kalau tidak, "Lainnya" akan muncul sebagai zona berperingkat.
     */
    fun diabaikan(nama: String?): Boolean {
        if (nama.isNullOrBlank()) return true
        val n = nama.trim().uppercase()
        return n.startsWith("AREA MANAGER") ||
            n.contains("TEST") ||
            n == "ADMIN" ||
            n == "DEVELOPER" ||
            n == "LAINNYA" ||
            n == "LAIN-LAIN" ||
            n == "OTHER" ||
            n == "OTHERS"
    }

    /** Pencocokan cadangan berdasarkan penggalan nama outlet — urutannya mengikuti web. */
    private val CADANGAN: List<Pair<String, List<String>>> = listOf(
        "Abu Bakar" to listOf("EMPANG", "BCC", "DRAMAGA", "PALEDANG", "CICURUG", "CIMANGGU"),
        "Muchtar" to listOf("CIBINONG", "CISEENG", "SENTUL", "PAJAJARAN"),
        "Chairul Rizky" to listOf("SUKMAJAYA", "BEJI", "SAWANGAN", "CIRENDEU", "JAGAKARSA"),
        "Tri Rizky" to listOf("KALISARI", "CIBUBUR", "CILENGSI", "CILEUNGSI"),
        "Mulyadi" to listOf("PEKAYON", "JATIASIH", "JATIWARINGIN"),
    )

    /**
     * @param pemetaan outlet_id -> nama AM dari `staff_outlets`; boleh kosong.
     * @param rolePengguna role yang sedang login.
     * @param namaPengguna nama yang sedang login, dipakai saat ia sendiri area manager.
     */
    fun untuk(
        outletId: String,
        namaOutlet: String,
        pemetaan: Map<String, String>,
        rolePengguna: Role?,
        namaPengguna: String?,
    ): String {
        // Area manager hanya melihat outlet binaannya sendiri, jadi semuanya zona dia.
        if (rolePengguna == Role.AREA_MANAGER) {
            return namaPengguna?.takeIf { it.isNotBlank() } ?: "Area Anda"
        }

        pemetaan[outletId]?.let { if (!diabaikan(it)) return it }

        val nama = namaOutlet.uppercase()
        CADANGAN.forEach { (am, penggalan) ->
            if (penggalan.any { nama.contains(it) }) return am
        }
        return "Lainnya"
    }
}
