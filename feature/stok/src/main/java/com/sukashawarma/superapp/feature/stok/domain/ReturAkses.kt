package com.sukashawarma.superapp.feature.stok.domain

import com.sukashawarma.superapp.domain.model.Role

/**
 * Pembagian peran di modul Retur & Refund Bahan — cermin `isManager` / `isKitchen`
 * di `apps/stok/src/app/stok/refund/page.tsx` dan `components/refund/CardReturItem.tsx`.
 *
 * Tiga hal yang perlu diingat sebelum mengubah daftar di sini:
 *
 * 1. **Menu retur TIDAK digerbangi peran.** Di web `/stok/refund` masuk ke
 *    `kitchenAdminMoreItems`, `leaderMoreItems`, DAN `crewMoreItems` sekaligus
 *    (BottomNav.tsx), jadi semua peran melihat menunya. Yang berbeda per peran
 *    hanyalah tab mana yang muncul dan tombol aksi mana yang aktif.
 * 2. **Daftar ini murni merapikan tampilan.** Penjaga sebenarnya ada di RPC:
 *    `approve_retur_by_manager` dan `verifikasi_kitchen_dan_buat_sj` sama-sama
 *    memeriksa `outlet_staff.role` di dalam badan fungsinya dan melempar
 *    `insufficient_privilege` untuk peran di luar daftar. Berbeda dengan
 *    [Approver], di sini pemeriksaannya memang ada di database.
 * 3. **[KITCHEN] di sini sedikit lebih ketat daripada RPC-nya.** RPC
 *    `verifikasi_kitchen_dan_buat_sj` juga meloloskan `admin_finance`, tetapi UI
 *    web tidak pernah menampilkan tombolnya untuk peran itu. Yang ditiru adalah
 *    UI web, supaya tab yang terlihat di HP sama dengan yang terlihat di browser.
 */
object ReturAkses {

    /**
     * Penyetuju klaim retur outlet. Sama persis dengan daftar di dalam
     * `approve_retur_by_manager`, ditambah tidak ada bedanya di UI web.
     *
     * `spv` ikut masuk sebagai legacy, sejalan dengan [Approver.PERMINTAAN_VIEWER].
     */
    private val MANAGER = setOf(
        Role.AREA_MANAGER, Role.REGIONAL_MANAGER, Role.SPV,
        Role.ADMIN, Role.OWNER, Role.DEVELOPER,
    )

    /** Gudang Pusat: menimbang ulang fisik retur dan menerbitkan SJ Pengganti. */
    private val KITCHEN = setOf(
        Role.KITCHEN, Role.ADMIN, Role.OWNER, Role.PURCHASING, Role.DEVELOPER,
    )

    fun manager(role: Role?): Boolean = role != null && role in MANAGER
    fun kitchen(role: Role?): Boolean = role != null && role in KITCHEN

    /**
     * Boleh menyerahkan fisik ke kurir — cermin `isMyOutlet` di `CardReturItem.tsx`.
     *
     * Sengaja longgar: manajer dan gudang pusat ikut boleh walau tiketnya bukan
     * milik outlet mereka, karena merekalah yang mendampingi outlet saat kurir
     * datang dan karena RPC `konfirmasi_serah_terima_logistik` hanya menuntut
     * outletnya ada di `accessible_outlet_ids()`.
     */
    fun serahKurir(role: Role?, outletTiket: String, outletSaya: String?): Boolean =
        outletTiket == outletSaya || manager(role) || kitchen(role)

    /**
     * Tab yang dibuka pertama kali — cermin inisialisasi `activeTab` di web:
     * manajer langsung ke antrean persetujuan, gudang pusat ke antrean timbang,
     * sisanya ke klaim outletnya sendiri.
     */
    fun tabAwal(role: Role?): TabRetur = when {
        manager(role) -> TabRetur.PERSETUJUAN
        kitchen(role) -> TabRetur.KITCHEN
        else -> TabRetur.AKTIF
    }
}

/**
 * Lencana angka pada menu Retur — cermin `usePendingReturBadge` di `hooks/useRetur.ts`.
 *
 * Yang dihitung berbeda per peran, dan memang harus begitu: angka yang sama untuk
 * semua orang akan menyuruh kru menindaklanjuti antrean yang bukan urusannya.
 *
 * Satu penyimpangan yang disengaja dari web: daftar perannya memakai [ReturAkses]
 * apa adanya. Hook web memakai daftar tersendiri yang tidak sinkron dengan
 * halamannya — `spv` punya tab Persetujuan tetapi tidak pernah dapat lencana, dan
 * `developer` punya tab Kitchen tetapi lencananya dihitung sebagai kru. Menyalin
 * ketidaksinkronan itu berarti menyalin bug: tab berisi lima tiket sementara
 * menunya diam.
 */
object LencanaRetur {
    fun hitung(status: List<Pair<StatusRetur, String>>, role: Role?, outletSaya: String?): Int = when {
        ReturAkses.manager(role) -> status.count { it.first == StatusRetur.DIAJUKAN }
        ReturAkses.kitchen(role) -> status.count {
            it.first == StatusRetur.DALAM_PENGIRIMAN || it.first == StatusRetur.DITERIMA_KITCHEN
        }
        else -> status.count { !it.first.tuntas && it.second == outletSaya }
    }
}

/** Tab pada halaman Retur & Refund — cermin `TabType` di `refund/page.tsx`. */
enum class TabRetur(val label: String) {
    AKTIF("Klaim Berjalan"),
    PERSETUJUAN("Persetujuan AM/RM"),
    KITCHEN("Antrean Kitchen"),
    RIWAYAT("Riwayat Selesai"),
}
