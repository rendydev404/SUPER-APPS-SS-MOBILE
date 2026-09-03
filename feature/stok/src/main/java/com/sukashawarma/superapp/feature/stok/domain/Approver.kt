package com.sukashawarma.superapp.feature.stok.domain

import com.sukashawarma.superapp.domain.model.Role

/**
 * Matriks role approval — cermin `apps/stok/src/lib/stok/approver.ts` di web.
 *
 * Ini murni untuk menyembunyikan tombol; keputusan sebenarnya tetap di database.
 * Web memakai daftar ini sebagai gerbang server-side karena RPC-nya SECURITY
 * DEFINER dan tidak memeriksa role sama sekali. Native tidak punya lapisan server
 * sendiri, jadi yang melindungi adalah RLS pada tabel yang disentuh RPC serta
 * `accessible_outlet_ids()` — bukan daftar di bawah ini.
 */
object Approver {

    /** Boleh masuk pengalaman review permintaan. `spv` dipertahankan sebagai legacy. */
    private val PERMINTAAN_VIEWER = setOf(
        Role.KITCHEN, Role.ADMIN_FINANCE, Role.SPV, Role.LEADER,
        Role.REGIONAL_MANAGER, Role.PURCHASING,
    )

    /**
     * Boleh menyetujui permintaan. Lebih ketat daripada viewer: approval memanggil
     * `create_surat_jalan()`, artinya barang benar-benar keluar dari Gudang Pusat.
     */
    private val PERMINTAAN_APPROVER = setOf(
        Role.KITCHEN, Role.ADMIN_FINANCE, Role.ADMIN, Role.OWNER, Role.PURCHASING,
    )

    private val OPNAME_APPROVER = setOf(
        Role.LEADER, Role.REGIONAL_MANAGER, Role.SPV, Role.KITCHEN,
        Role.ADMIN_FINANCE, Role.ADMIN, Role.OWNER, Role.PURCHASING,
    )

    fun bolehReviewPermintaan(role: Role?): Boolean = role != null && role in PERMINTAAN_VIEWER
    fun bolehApprovePermintaan(role: Role?): Boolean = role != null && role in PERMINTAAN_APPROVER
    fun bolehApproveOpname(role: Role?): Boolean = role != null && role in OPNAME_APPROVER
}
