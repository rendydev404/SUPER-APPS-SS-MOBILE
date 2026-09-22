package com.sukashawarma.superapp.notif

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Tujuan yang diminta sebuah notifikasi saat diketuk.
 *
 * Notifikasi tiba lewat Intent, sedangkan navigasi hidup di dalam Compose. Titik
 * temunya di sini: [MainActivity] menaruh tujuannya, `RootNav` mengambilnya
 * sekali lalu berpindah.
 *
 * Nilainya dikosongkan begitu dibaca ([ambil]). Tanpa itu, tiap kali layar
 * disusun ulang — rotasi, kembali dari latar — aplikasi akan melompat lagi ke
 * halaman yang sama dan pengguna tidak bisa keluar dari sana.
 */
object NotifikasiTujuan {

    /** Nilai `route` pada payload FCM, mis. "manager_persetujuan". */
    private val _rute = MutableStateFlow<String?>(null)
    val rute: StateFlow<String?> = _rute

    /** ID area target bila notifikasi berasal dari percakapan grup area tertentu. */
    private val _areaId = MutableStateFlow<String?>(null)
    val areaId: StateFlow<String?> = _areaId

    fun set(nilai: String?, targetArea: String? = null) {
        if (!nilai.isNullOrBlank()) _rute.value = nilai
        if (!targetArea.isNullOrBlank()) _areaId.value = targetArea
    }

    fun setArea(targetArea: String?) {
        _areaId.value = targetArea
    }

    /** Lawan bicara pribadi yang diminta notifikasi: (id, nama, avatar). */
    private val _partner = MutableStateFlow<Triple<String, String, String?>?>(null)
    val partner: StateFlow<Triple<String, String, String?>?> = _partner

    fun setPartner(id: String?, nama: String?, avatar: String?) {
        if (id.isNullOrBlank()) return
        _partner.value = Triple(id, nama?.takeIf { it.isNotBlank() } ?: "Rekan kerja", avatar?.takeIf { it.isNotBlank() })
    }

    fun ambilPartner(): Triple<String, String, String?>? {
        val nilai = _partner.value
        _partner.value = null
        return nilai
    }

    fun ambil(): String? {
        val nilai = _rute.value
        _rute.value = null
        return nilai
    }

    fun ambilArea(): String? {
        val nilai = _areaId.value
        _areaId.value = null
        return nilai
    }

    /** Nama extra pada Intent notifikasi. */
    const val EXTRA_RUTE = "notif_rute"
    const val EXTRA_AREA_ID = "notif_area_id"
    const val EXTRA_PARTNER_ID = "notif_partner_id"
    const val EXTRA_PARTNER_NAMA = "notif_partner_nama"
    const val EXTRA_PARTNER_AVATAR = "notif_partner_avatar"

    /** Rute yang dikenali. Payload di luar daftar ini diabaikan, bukan dipercaya. */
    const val MANAGER_PERSETUJUAN = "manager_persetujuan"
    const val MANAGER_WASTE = "manager_waste"
    const val MANAGER_PETTY_CASH = "manager_petty_cash"
    const val LEADER_PETTY_CASH = "leader_petty_cash"
    const val CHAT = "chat"
    const val CHAT_AREA = "chat_area"
    const val CHAT_PRIBADI = "chat_pribadi"
}
