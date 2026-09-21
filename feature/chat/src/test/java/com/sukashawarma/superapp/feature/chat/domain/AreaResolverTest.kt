package com.sukashawarma.superapp.feature.chat.domain

import com.google.gson.JsonObject
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.feature.chat.data.AnggotaGrup
import com.sukashawarma.superapp.feature.chat.data.parsePesanAreaChat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AreaResolverTest {

    @Test
    fun `area manager otomatis dipetakan ke areanya sendiri`() {
        val area = AreaResolver.tentukanAreaPengguna(
            role = Role.AREA_MANAGER,
            roleRaw = "area_manager",
            namaPengguna = "Abu Bakar",
            outletNama = null
        )
        assertEquals("abu_bakar", area.areaId)
        assertEquals("Abu Bakar", area.amName)
    }

    @Test
    fun `kru outlet empang dipetakan ke area abu bakar`() {
        val area = AreaResolver.tentukanAreaPengguna(
            role = Role.CREW,
            roleRaw = "crew",
            namaPengguna = "Ahmad Kru",
            outletNama = "SS EMPANG"
        )
        assertEquals("abu_bakar", area.areaId)
        assertTrue(area.outlets.contains("EMPANG"))
    }

    @Test
    fun `kru outlet sentul dipetakan ke area muchtar`() {
        val area = AreaResolver.tentukanAreaPengguna(
            role = Role.CREW,
            roleRaw = "crew",
            namaPengguna = "Budi Kru",
            outletNama = "SS SENTUL"
        )
        assertEquals("muchtar", area.areaId)
        assertTrue(area.outlets.contains("SENTUL"))
    }

    @Test
    fun `kru outlet jatiasih dipetakan ke area mulyadi`() {
        val area = AreaResolver.tentukanAreaPengguna(
            role = Role.LEADER,
            roleRaw = "leader",
            namaPengguna = "Siti Leader",
            outletNama = "SS JATIASIH"
        )
        assertEquals("mulyadi", area.areaId)
        assertTrue(area.outlets.contains("JATIASIH"))
    }

    @Test
    fun `hak ganti area hanya untuk pimpinan dan pengembang`() {
        assertTrue(AreaResolver.bolehGantiArea(Role.REGIONAL_MANAGER, "regional_manager"))
        assertTrue(AreaResolver.bolehGantiArea(Role.DEVELOPER, "developer"))
        assertTrue(AreaResolver.bolehGantiArea(Role.OWNER, "owner"))
        assertTrue(AreaResolver.bolehGantiArea(Role.ADMIN, "admin"))

        assertFalse(AreaResolver.bolehGantiArea(Role.CREW, "crew"))
        assertFalse(AreaResolver.bolehGantiArea(Role.LEADER, "leader"))
        assertFalse(AreaResolver.bolehGantiArea(Role.AREA_MANAGER, "area_manager"))
    }

    @Test
    fun `saring anggota area memfilter kru sesuai outlet binaan area`() {
        val areaAbuBakar = AreaResolver.cariBerdasarkanId("abu_bakar")
        val daftarSemua = listOf(
            AnggotaGrup("1", "Abu Bakar", "abubakar", null, "area_manager", null),
            AnggotaGrup("2", "Kru Empang", null, null, "crew", "SS EMPANG"),
            AnggotaGrup("3", "Kru Cibinong", null, null, "crew", "SS CIBINONG"), // Milik Muchtar
            AnggotaGrup("4", "Leader BCC", null, null, "leader", "SS BCC"),
        )

        val anggotaArea = AreaResolver.saringAnggotaArea(daftarSemua, areaAbuBakar)
        assertEquals(3, anggotaArea.size)
        assertTrue(anggotaArea.any { it.nama == "Abu Bakar" })
        assertTrue(anggotaArea.any { it.nama == "Kru Empang" })
        assertTrue(anggotaArea.any { it.nama == "Leader BCC" })
        assertFalse(anggotaArea.any { it.nama == "Kru Cibinong" })
    }

    @Test
    fun `parse pesan area chat membaca informasi peran dan outlet pengirim`() {
        val json = JsonObject().apply {
            addProperty("id", "msg-123")
            addProperty("area_id", "abu_bakar")
            addProperty("sender_id", "user-456")
            addProperty("sender_name", "Farhan")
            addProperty("sender_role", "crew")
            addProperty("outlet_name", "SS Empang")
            addProperty("body", "Halo tim area!")
            addProperty("created_at", "2026-09-19T10:00:00Z")
        }

        val pesan = parsePesanAreaChat(json)
        assertNotNull(pesan)
        assertEquals("msg-123", pesan?.id)
        assertEquals("abu_bakar", pesan?.areaId)
        assertEquals("Farhan", pesan?.senderName)
        assertEquals("SS Empang", pesan?.outletName)
        assertTrue(pesan?.isCrew == true)
        assertFalse(pesan?.isAreaManager == true)
    }

    @Test
    fun `verifikasi seluruh 5 area resmi terdaftar dengan outlet binaan`() {
        val areaIds = AreaResolver.DAFTAR_AREA.map { it.areaId }
        assertTrue(areaIds.contains("abu_bakar"))
        assertTrue(areaIds.contains("muchtar"))
        assertTrue(areaIds.contains("chairul_rizky"))
        assertTrue(areaIds.contains("tri_rizky"))
        assertTrue(areaIds.contains("mulyadi"))
        assertEquals(5, AreaResolver.DAFTAR_AREA.size)
    }
}
