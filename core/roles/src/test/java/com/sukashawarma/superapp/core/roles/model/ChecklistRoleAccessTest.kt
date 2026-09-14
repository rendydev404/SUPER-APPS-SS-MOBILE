package com.sukashawarma.superapp.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Memastikan hak akses menu Checklist:
 * - Manajemen Checklist: HANYA RM (regional_manager), Admin (admin), dan HR (admin_hr).
 * - Monitoring Checklist: Diizinkan untuk Leader, Area Manager, RM, Admin, HR, SPV (SPV_TIER_ROLES).
 * - Leader dan Area Manager TIDAK boleh mengakses Manajemen Checklist.
 */
class ChecklistRoleAccessTest {

    @Test
    fun `manajemen checklist hanya untuk RM, Admin, dan HR`() {
        // Harus diizinkan
        assertTrue("Admin harus bisa kelola checklist", Role.ADMIN in CHECKLIST_MANAGE_ROLES)
        assertTrue("Admin HR harus bisa kelola checklist", Role.ADMIN_HR in CHECKLIST_MANAGE_ROLES)
        assertTrue("Regional Manager harus bisa kelola checklist", Role.REGIONAL_MANAGER in CHECKLIST_MANAGE_ROLES)

        // Harus DITOLAK
        assertFalse("Leader tidak boleh kelola checklist", Role.LEADER in CHECKLIST_MANAGE_ROLES)
        assertFalse("Area Manager tidak boleh kelola checklist", Role.AREA_MANAGER in CHECKLIST_MANAGE_ROLES)
        assertFalse("SPV tidak boleh kelola checklist", Role.SPV in CHECKLIST_MANAGE_ROLES)
        assertFalse("Crew tidak boleh kelola checklist", Role.CREW in CHECKLIST_MANAGE_ROLES)
        assertFalse("Kitchen tidak boleh kelola checklist", Role.KITCHEN in CHECKLIST_MANAGE_ROLES)
        assertFalse("Kiosk tidak boleh kelola checklist", Role.KIOSK in CHECKLIST_MANAGE_ROLES)
    }

    @Test
    fun `leader dan area manager boleh monitoring checklist`() {
        assertTrue("Leader harus bisa monitor checklist", Role.LEADER in SPV_TIER_ROLES)
        assertTrue("Area Manager harus bisa monitor checklist", Role.AREA_MANAGER in SPV_TIER_ROLES)
        assertTrue("Regional Manager harus bisa monitor checklist", Role.REGIONAL_MANAGER in SPV_TIER_ROLES)
        assertTrue("Admin harus bisa monitor checklist", Role.ADMIN in SPV_TIER_ROLES)
        assertTrue("Admin HR harus bisa monitor checklist", Role.ADMIN_HR in SPV_TIER_ROLES)

        // Crew biasa tidak boleh monitor
        assertFalse("Crew tidak boleh monitor checklist", Role.CREW in SPV_TIER_ROLES)
    }
}
