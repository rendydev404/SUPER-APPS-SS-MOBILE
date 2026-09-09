package com.sukashawarma.superapp.feature.stok.domain

import com.sukashawarma.superapp.domain.model.Role

/**
 * Matriks role approval — cermin `apps/stok/src/lib/stok/approver.ts` di web.
 *
 * Ini murni untuk menyembunyikan tombol, dan HANYA itu.
 *
 * Jangan percaya kalimat lama di tempat ini yang menyebut "yang melindungi adalah
 * RLS pada tabel yang disentuh RPC". Itu keliru: `approve_opname`, `reject_opname`,
 * `set_opname_pending`, dan `finalize_opname` semuanya SECURITY DEFINER, yang justru
 * MEM-BYPASS RLS. Diperiksa langsung ke database pada 9 September 2026 — keempatnya
 * dijawab sampai ke pemeriksaan status opname bahkan oleh pemanggil anonim tanpa
 * sesi, jadi tidak ada pemeriksaan auth.uid(), peran, maupun outlet di dalamnya.
 *
 * Akses anonim ditutup migrasi web `20300206000000_opname_rpc_cabut_akses_anon`.
 * Pengguna yang sudah login dengan peran apa pun MASIH bisa memanggilnya langsung;
 * penutupnya menuntut pemeriksaan peran di dalam badan fungsi, dan badan itu belum
 * ada di repo migration mana pun. Sampai itu dikerjakan, anggap daftar di bawah ini
 * sebagai kerapian tampilan, bukan pengaman.
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

    /**
     * Cermin `WASTE_APPROVER_ROLES` di `apps/stok/src/app/actions/waste.ts`, ditambah
     * `area_manager` yang menyetujui tabel yang sama lewat `apps/manager`. Sengaja
     * dipisah dari gerbang modul Area Manager: RLS `stok_waste_reports` hanya menuntut
     * outlet masuk `accessible_outlet_ids()`, jadi mempersempit daftar ini akan
     * menghilangkan kewenangan yang dipunyai role tersebut di web.
     */
    private val WASTE_APPROVER = setOf(
        Role.LEADER, Role.REGIONAL_MANAGER, Role.SPV, Role.KITCHEN,
        Role.ADMIN, Role.OWNER, Role.PURCHASING, Role.DEVELOPER, Role.AREA_MANAGER,
    )

    fun bolehReviewPermintaan(role: Role?): Boolean = role != null && role in PERMINTAAN_VIEWER
    fun bolehApprovePermintaan(role: Role?): Boolean = role != null && role in PERMINTAAN_APPROVER
    fun bolehApproveOpname(role: Role?): Boolean = role != null && role in OPNAME_APPROVER
    fun bolehApproveWaste(role: Role?): Boolean = role != null && role in WASTE_APPROVER
}
