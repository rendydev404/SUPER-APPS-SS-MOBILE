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

    // ---------------------------------------------------------------------------
    // Gerbang menu Analisis & Laporan.
    //
    // Daftar di bawah disalin baris-per-baris dari `AppSidebar.tsx` web, bukan
    // disederhanakan jadi pusat/pengawas. Web memang memakai daftar peran yang
    // berbeda-beda untuk tiap menu — Nilai Persediaan lebih ketat daripada Master
    // Harga, HPP lebih longgar daripada Laporan Penjualan — dan meringkasnya jadi
    // dua kelompok akan membuka menu untuk peran yang di web tidak melihatnya.
    // ---------------------------------------------------------------------------

    /**
     * Master Harga Bahan Baku — cermin `canViewVendorPrices`.
     *
     * Sengaja longgar (pengawas outlet ikut masuk) karena halaman harga di web
     * memakai Server Action ber-service-role, bukan pembacaan langsung ber-RLS.
     */
    private val HARGA_BAHAN = setOf(
        Role.KITCHEN, Role.PURCHASING, Role.ADMIN_FINANCE, Role.ADMIN, Role.OWNER,
        Role.SPV, Role.REGIONAL_MANAGER, Role.LEADER, Role.AREA_MANAGER, Role.DEVELOPER,
    )

    /**
     * Nilai Persediaan — cermin `canViewNilaiPersediaan`.
     *
     * Web menegaskan gerbang ini HARUS sama dengan policy RLS `bbh_read` di
     * `bahan_baku_harga`: datanya dibaca lewat view `security_invoker`, jadi peran
     * di luar daftar akan membuka layar lalu melihat total nol dan menyangka
     * stoknya kosong. Karena itu lebih ketat daripada [HARGA_BAHAN].
     */
    private val NILAI_PERSEDIAAN = setOf(
        Role.ADMIN, Role.OWNER, Role.KITCHEN, Role.PURCHASING, Role.ADMIN_FINANCE,
    )

    /** HPP Setiap Menu — cermin `canViewHPP`. */
    private val HPP_MENU = setOf(
        Role.KITCHEN, Role.PURCHASING, Role.ADMIN_FINANCE, Role.ADMIN, Role.OWNER,
        Role.SPV, Role.REGIONAL_MANAGER, Role.DEVELOPER,
    )

    /**
     * Laporan Penjualan dan Plafon & Belanja Outlet — cermin `canViewSales`.
     * Web menempatkan keduanya di balik satu gerbang yang sama.
     */
    private val PENJUALAN = setOf(
        Role.KITCHEN, Role.ADMIN, Role.OWNER, Role.ADMIN_FINANCE,
        Role.DEVELOPER, Role.PURCHASING,
    )

    /**
     * Kartu analisis selisih dan badge persentase di Detail Opname — cermin
     * `canViewThresholdAndLoss` di `components/stok/OpnameDetail.tsx`.
     *
     * Yang digerbangi hanya penafsirannya, bukan datanya: daftar item beserta qty
     * sistem, fisik, dan selisihnya tetap terlihat semua peran, persis seperti web.
     */
    private val ANALISIS_SELISIH = setOf(
        Role.KITCHEN, Role.ADMIN, Role.ADMIN_FINANCE, Role.OWNER,
        Role.DEVELOPER, Role.PURCHASING,
    )

    /**
     * Pengaturan Threshold (titik pesan ulang per outlet).
     *
     * Dua hal yang wajib diingat sebelum melonggarkan daftar ini:
     *
     * 1. `outlet_reorder_point` TIDAK punya satu pun `CREATE POLICY` di seluruh
     *    migration — tabelnya dibuat manual di luar migration. Database tidak akan
     *    menolak peran mana pun. Gerbang ini satu-satunya pengaman yang ada, tidak
     *    seperti gerbang lain di kelas ini yang cuma merapikan tampilan.
     * 2. `THRESHOLD_EDITOR_ROLES` di `app/actions/threshold.ts` menyebut tujuh peran,
     *    tetapi Server Action itu tidak dipakai siapa pun — `ThresholdPage` menulis
     *    lewat client biasa dan halamannya menolak semua kecuali `admin`. Yang
     *    ditiru di sini adalah perilaku web yang benar-benar berjalan, bukan daftar
     *    di kode mati.
     */
    private val THRESHOLD = setOf(Role.ADMIN)

    fun melihatHargaBahan(role: Role?): Boolean = role != null && role in HARGA_BAHAN
    fun melihatNilaiPersediaan(role: Role?): Boolean = role != null && role in NILAI_PERSEDIAAN
    fun melihatHppMenu(role: Role?): Boolean = role != null && role in HPP_MENU
    fun melihatLaporanPenjualan(role: Role?): Boolean = role != null && role in PENJUALAN
    fun melihatPlafonBelanja(role: Role?): Boolean = role != null && role in PENJUALAN
    fun melihatAnalisisSelisih(role: Role?): Boolean = role != null && role in ANALISIS_SELISIH
    fun melihatThreshold(role: Role?): Boolean = role != null && role in THRESHOLD
}
