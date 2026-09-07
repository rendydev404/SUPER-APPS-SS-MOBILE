package com.sukashawarma.superapp.feature.stok.domain

import com.sukashawarma.superapp.domain.model.Role

/**
 * Pembagian peran di modul Stok — cermin `isKitchenOrAdmin` / `isLeaderOrSPV` di
 * `apps/stok/src/components/common/BottomNav.tsx`.
 *
 * Web memakai kedua predikat itu untuk menyusun tab bawah, dan susunannya memang
 * berbeda per kelompok, bukan sekadar menyembunyikan satu-dua menu. Menyalinnya ke
 * sini membuat native tidak perlu menebak: kelompok yang sama melihat menu yang sama.
 *
 * Satu penyimpangan yang disengaja dari web: role pusat TIDAK mendapat tab Dashboard.
 * Monitoring saldo selalu terikat satu outlet, sedangkan gudang pusat dan admin tidak
 * memegang outlet mana pun — di web tab itu ada tetapi menampilkan outlet acak dari
 * daftar yang bisa mereka akses.
 */
object StokAkses {

    /** Gudang pusat dan kantor: menyetujui permintaan, menerima PO, memegang master harga. */
    private val PUSAT = setOf(
        Role.KITCHEN, Role.PURCHASING, Role.ADMIN,
        Role.ADMIN_FINANCE, Role.OWNER, Role.DEVELOPER,
    )

    /** Pengawas outlet: leader, SPV (legacy), area manager, regional manager. */
    private val PENGAWAS = setOf(
        Role.LEADER, Role.SPV, Role.AREA_MANAGER, Role.REGIONAL_MANAGER,
    )

    fun pusat(role: Role?): Boolean = role != null && role in PUSAT
    fun pengawas(role: Role?): Boolean = role != null && role in PENGAWAS

    /** Crew dan role lain yang hanya memegang outletnya sendiri. */
    fun operasional(role: Role?): Boolean = !pusat(role) && !pengawas(role)

    /**
     * Dashboard monitoring hanya untuk yang benar-benar memegang outlet.
     * Lihat alasannya di KDoc kelas ini.
     */
    fun melihatDashboard(role: Role?): Boolean = !pusat(role)

    /**
     * Penerimaan PO supplier. Web menampilkannya sebagai tab utama untuk role pusat
     * dan sebagai menu "Terima PO Supplier" untuk pengawas outlet.
     */
    fun melihatPenerimaanPo(role: Role?): Boolean = pusat(role) || pengawas(role)

    /** Inbound/Outbound gudang pusat — web membatasinya ke `kitchen` saja. */
    fun melihatInboundOutbound(role: Role?): Boolean = role == Role.KITCHEN
}
