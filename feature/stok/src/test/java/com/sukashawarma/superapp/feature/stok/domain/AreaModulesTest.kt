package com.sukashawarma.superapp.feature.stok.domain

import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.feature.stok.data.*
import com.sukashawarma.superapp.feature.stok.ui.area.priceCsv
import org.junit.Assert.*
import org.junit.Test

class AreaModulesTest {
    private fun purchase(price: Double) = PricePurchase("po-$price", "bahan", "PO/1", "2026-09-05", "Vendor, \"A\"", price, 2.0, price * 2)
    private fun material(vararg prices: Double) = MaterialPrice("bahan", "Bawang", "Bumbu", "Bal", 80.0, prices.map(::purchase))
    @Test fun `master fallback is not counted as stable PO`() {
        val item = material()
        assertEquals(80.0, item.effective!!, 0.0)
        assertFalse(item.matchesStatus("stabil"))
        assertNull(item.percent)
    }
    @Test fun `first PO counted stable like web but has no previous comparison`() {
        val item = material(100.0)
        assertTrue(item.matchesStatus("stabil"))
        assertEquals(100.0, item.effective!!, 0.0)
        assertNull(item.delta)
    }
    @Test fun `comparison uses previous received PO rather than master`() {
        val item = material(110.0, 100.0)
        assertEquals(10.0, item.delta!!, 0.0)
        assertEquals(0.1, item.percent!!, 0.00001)
        assertTrue(item.matchesStatus("naik"))
        assertFalse(item.matchesStatus("stabil"))
        assertTrue(material(90.0, 100.0).matchesStatus("turun"))
        assertTrue(material(100.0, 100.0).matchesStatus("stabil"))
    }
    @Test fun `CSV quotes vendor and preserves master-only absence of purchase`() {
        val csv = priceCsv(listOf(material(110.0, 100.0), material()))
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("\"Vendor, \"\"A\"\"\""))
        assertTrue(csv.contains("\"10.00%\""))
        assertFalse(csv.substringAfterLast("\r\n").contains("80.0"))
    }
    @Test fun `area modules hidden from crew and mitra`() {
        assertTrue(AreaModuleAccess.allowed(Role.AREA_MANAGER))
        assertTrue(AreaModuleAccess.allowed(Role.LEADER))
        assertFalse(AreaModuleAccess.allowed(Role.CREW))
        assertFalse(AreaModuleAccess.allowed(Role.MITRA))
        assertFalse(AreaModuleAccess.allowed(null))
    }
    @Test fun `waste approval keeps roles the web grants beyond the area modules`() {
        listOf(Role.KITCHEN, Role.ADMIN, Role.OWNER, Role.PURCHASING, Role.DEVELOPER).forEach { role ->
            assertFalse("$role bukan pengelola area", AreaModuleAccess.allowed(role))
            assertTrue("$role menyetujui waste di web", WasteApprovalAccess.allowed(role))
        }
        assertTrue(WasteApprovalAccess.allowed(Role.AREA_MANAGER))
        assertTrue(WasteApprovalAccess.allowed(Role.LEADER))
        assertFalse(WasteApprovalAccess.allowed(Role.CREW))
        assertFalse(WasteApprovalAccess.allowed(Role.ADMIN_FINANCE))
        assertFalse(WasteApprovalAccess.allowed(null))
    }
    @Test fun `waste comparison uses large units and handles unknown conversion`() {
        val meta = UnitMeta("Bal", "Pack", "Gram", 10.0, 1000.0)
        val report = WasteReview("w", "outlet", "bahan", "Bawang", "Outlet", "Staff", null, 1.5, "Rusak", null, meta,
            UnitScale.besarFromSmallest(1000.0, meta))
        assertTrue(report.deficit)
        assertFalse(report.copy(balance = 1.5).deficit)
        assertTrue(report.copy(balance = null).deficit)
        assertEquals("1 Bal · 5 Pack", report.quantityLabel)
    }
    @Test fun `waste history item correctly reflects approval and rejection status with decider`() {
        val meta = UnitMeta("Kg", null, null, null, null)
        val approved = WasteHistoryItem(
            id = "w1",
            outletId = "out-1",
            outletName = "Cibinong",
            bahanId = "b1",
            bahanName = "Lettuce",
            qty = 2.0,
            reason = "Busuk",
            photoUrl = null,
            status = "APPROVED",
            rejectionReason = null,
            reporterName = "Yunus",
            deciderName = "Indra Adam Sami",
            createdAt = "2026-09-21T15:02:00Z",
            updatedAt = "2026-09-21T15:11:00Z",
            meta = meta
        )
        assertTrue(approved.isApproved)
        assertFalse(approved.isRejected)
        assertEquals("Indra Adam Sami", approved.deciderName)
        assertEquals("2 Kg", approved.quantityLabel)

        val rejected = approved.copy(
            id = "w2",
            status = "REJECTED",
            rejectionReason = "Foto kurang jelas",
            deciderName = "Muhtar Arifin"
        )
        assertFalse(rejected.isApproved)
        assertTrue(rejected.isRejected)
        assertEquals("Muhtar Arifin", rejected.deciderName)
        assertEquals("Foto kurang jelas", rejected.rejectionReason)
    }
    @Test fun `waste approval tabs have expected enum values`() {
        val tabs = com.sukashawarma.superapp.feature.stok.ui.area.WasteApprovalTab.values()
        assertEquals(2, tabs.size)
        assertEquals(com.sukashawarma.superapp.feature.stok.ui.area.WasteApprovalTab.MENUNGGU, tabs[0])
        assertEquals(com.sukashawarma.superapp.feature.stok.ui.area.WasteApprovalTab.RIWAYAT, tabs[1])
    }
    @Test fun `waste history status filter filters correctly`() {
        val meta = UnitMeta("Kg", null, null, null, null)
        val approved = WasteHistoryItem("1", "out", "Out", "b", "Bahan", 1.0, "r", null, "APPROVED", null, "Staff", "Approver", null, null, meta)
        val rejected = WasteHistoryItem("2", "out", "Out", "b", "Bahan", 1.0, "r", null, "REJECTED", "Alasan", "Staff", "Rejector", null, null, meta)
        val state = com.sukashawarma.superapp.feature.stok.ui.area.WasteState(history = listOf(approved, rejected))

        assertEquals(2, state.filteredHistory.size)

        val approvedOnly = state.copy(historyFilter = com.sukashawarma.superapp.feature.stok.ui.area.WasteHistoryStatusFilter.DISETUJUI)
        assertEquals(1, approvedOnly.filteredHistory.size)
        assertEquals("APPROVED", approvedOnly.filteredHistory.first().status)

        val rejectedOnly = state.copy(historyFilter = com.sukashawarma.superapp.feature.stok.ui.area.WasteHistoryStatusFilter.DITOLAK)
        assertEquals(1, rejectedOnly.filteredHistory.size)
        assertEquals("REJECTED", rejectedOnly.filteredHistory.first().status)
    }
}
