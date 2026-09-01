package com.sukashawarma.superapp.domain.session

import com.sukashawarma.superapp.domain.model.MitraProfile
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.domain.model.StaffProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class StartDestinationTest {

    private fun staff(role: Role?) = StaffProfile(
        id = "u-1",
        outletId = "o-1",
        outletName = "MITRA PALEDANG",
        name = "Bapak Anis",
        role = role,
        roleRaw = role?.value ?: "",
        status = "active",
        username = "mitra_anis",
        refPhotoUrl = null,
        allowManualButton = false,
        faceDescriptor = null,
    )

    private val profil = MitraProfile(
        id = "p-1",
        userId = "u-1",
        namaMitra = "Mitra Paledang",
        outletIds = listOf("o-1"),
        profitSharingPct = 50.0,
        bankName = "BCA",
        bankAccountNumber = "123",
        bankAccountHolder = "Bapak Anis",
        noPks = null,
        tanggalPks = null,
        tanggalBerakhirPks = null,
        status = "aktif",
    )

    @Test
    fun `tanpa staff selalu ke login`() {
        assertEquals(StartDestination.LOGIN, resolveStartDestination(null, null, false))
        assertEquals(StartDestination.LOGIN, resolveStartDestination(null, profil, true))
    }

    @Test
    fun `mitra dengan profil ke dashboard mitra`() {
        assertEquals(
            StartDestination.MITRA_DASHBOARD,
            resolveStartDestination(staff(Role.MITRA), profil, false)
        )
    }

    @Test
    fun `mitra tanpa profil ke layar penjelas`() {
        assertEquals(
            StartDestination.MITRA_NO_PROFILE,
            resolveStartDestination(staff(Role.MITRA), null, false)
        )
    }

    @Test
    fun `mitra yang profilnya gagal dimuat ke layar galat bukan layar penjelas`() {
        assertEquals(
            StartDestination.MITRA_LOAD_ERROR,
            resolveStartDestination(staff(Role.MITRA), null, true)
        )
    }

    @Test
    fun `role lain tetap ke home meski flag mitra menyala`() {
        assertEquals(StartDestination.HOME, resolveStartDestination(staff(Role.CREW), null, true))
        assertEquals(StartDestination.HOME, resolveStartDestination(staff(Role.SPV), null, false))
        assertEquals(StartDestination.HOME, resolveStartDestination(staff(Role.OWNER), profil, false))
    }

    @Test
    fun `role tak dikenal diperlakukan seperti role biasa`() {
        assertEquals(StartDestination.HOME, resolveStartDestination(staff(null), null, false))
    }
}
