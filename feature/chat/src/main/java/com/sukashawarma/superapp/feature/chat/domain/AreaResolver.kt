package com.sukashawarma.superapp.feature.chat.domain

import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.feature.chat.data.AnggotaArea
import com.sukashawarma.superapp.feature.chat.data.AnggotaGrup
import com.sukashawarma.superapp.feature.chat.data.AreaInfo

/**
 * Logika resolusi area, pemetaan kru ke Area Manager, dan penyaringan anggota area.
 */
object AreaResolver {

    /**
     * Daftar 5 Area resmi Suka Shawarma beserta Area Manager dan cabang binaannya.
     */
    val DAFTAR_AREA: List<AreaInfo> = listOf(
        AreaInfo(
            areaId = "abu_bakar",
            namaArea = "Area Abu Bakar",
            amName = "Abu Bakar",
            outlets = listOf("EMPANG", "BCC", "DRAMAGA", "PALEDANG", "CICURUG", "CIMANGGU"),
            deskripsi = "Ruang obrolan Tim Area Abu Bakar (Bogor Barat & Selatan).",
        ),
        AreaInfo(
            areaId = "muchtar",
            namaArea = "Area Muchtar",
            amName = "Muchtar",
            outlets = listOf("CIBINONG", "CISEENG", "SENTUL", "PAJAJARAN"),
            deskripsi = "Ruang obrolan Tim Area Muchtar (Cibinong & Sentul).",
        ),
        AreaInfo(
            areaId = "chairul_rizky",
            namaArea = "Area Chairul Rizky",
            amName = "Chairul Rizky",
            outlets = listOf("SUKMAJAYA", "BEJI", "SAWANGAN", "CIRENDEU", "JAGAKARSA"),
            deskripsi = "Ruang obrolan Tim Area Chairul Rizky (Depok & Jaksel).",
        ),
        AreaInfo(
            areaId = "tri_rizky",
            namaArea = "Area Tri Rizky",
            amName = "Tri Rizky",
            outlets = listOf("KALISARI", "CIBUBUR", "CILENGSI", "CILEUNGSI"),
            deskripsi = "Ruang obrolan Tim Area Tri Rizky (Cibubur & Cileungsi).",
        ),
        AreaInfo(
            areaId = "mulyadi",
            namaArea = "Area Mulyadi",
            amName = "Mulyadi",
            outlets = listOf("PEKAYON", "JATIASIH", "JATIWARINGIN"),
            deskripsi = "Ruang obrolan Tim Area Mulyadi (Bekasi).",
        ),
    )

    val AREA_DEFAULT: AreaInfo = DAFTAR_AREA.first()

    /**
     * Menentukan apakah role ini boleh berpindah-pindah melihat seluruh area.
     */
    fun bolehGantiArea(role: Role?, roleRaw: String?): Boolean {
        if (role == Role.REGIONAL_MANAGER ||
            role == Role.DEVELOPER ||
            role == Role.OWNER ||
            role == Role.ADMIN ||
            role == Role.ADMIN_HR
        ) return true

        val raw = roleRaw?.lowercase() ?: ""
        return raw == "developer" || raw == "regional_manager" || raw == "owner" || raw == "admin"
    }

    /**
     * Menentukan AreaInfo awal berdasarkan profil pengguna saat ini.
     */
    fun tentukanAreaPengguna(
        role: Role?,
        roleRaw: String?,
        namaPengguna: String?,
        outletNama: String?,
    ): AreaInfo {
        val isAM = role == Role.AREA_MANAGER || roleRaw?.equals("area_manager", ignoreCase = true) == true
        val namaBersih = namaPengguna.orEmpty().trim().lowercase()

        // 1. Jika pengguna adalah Area Manager, cocokkan dengan nama AM
        if (isAM && namaBersih.isNotBlank()) {
            val cocok = DAFTAR_AREA.find { area ->
                namaBersih.contains(area.amName.lowercase()) || area.amName.lowercase().contains(namaBersih)
            }
            if (cocok != null) return cocok

            // Jika ada AM baru yang belum di-hardcode, buatkan Area dinamis
            return AreaInfo(
                areaId = "am_" + namaBersih.replace(" ", "_"),
                namaArea = "Area " + (namaPengguna ?: "Anda"),
                amName = namaPengguna ?: "Area Manager",
                outlets = emptyList(),
                deskripsi = "Ruang obrolan tim Area Manager.",
            )
        }

        // 2. Jika pengguna adalah kru/leader, cocokkan outlet pengguna ke area
        val outletBersih = outletNama.orEmpty().trim().uppercase()
        if (outletBersih.isNotBlank()) {
            val cocok = DAFTAR_AREA.find { area ->
                area.outlets.any { outletBersih.contains(it) }
            }
            if (cocok != null) return cocok
        }

        // 3. Fallback: area pertama
        return AREA_DEFAULT
    }

    /**
     * Mengambil daftar area berdasarkan ID
     */
    fun cariBerdasarkanId(areaId: String): AreaInfo =
        DAFTAR_AREA.find { it.areaId == areaId } ?: AREA_DEFAULT

    /**
     * Menyaring anggota global agar hanya anggota yang termasuk dalam area ini.
     */
    fun saringAnggotaArea(semuaAnggota: List<AnggotaGrup>, area: AreaInfo): List<AnggotaArea> {
        val amNameLower = area.amName.lowercase()
        val outletSet = area.outlets.map { it.uppercase() }.toSet()

        return semuaAnggota.filter { anggota ->
            val isAM = anggota.role.equals("area_manager", ignoreCase = true) &&
                (anggota.nama.lowercase().contains(amNameLower) || anggota.namaTampil.lowercase().contains(amNameLower))

            val outletPengguna = anggota.outlet.orEmpty().uppercase()
            val isOutletCocok = outletSet.any { outletPengguna.contains(it) }

            isAM || isOutletCocok
        }.map { a ->
            AnggotaArea(
                id = a.id,
                nama = a.nama,
                username = a.username,
                avatar = a.avatar,
                role = a.role,
                outlet = a.outlet,
            )
        }
    }
}
