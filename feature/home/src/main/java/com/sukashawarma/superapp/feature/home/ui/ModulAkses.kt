package com.sukashawarma.superapp.presentation.home

import com.sukashawarma.superapp.domain.model.Role

/*
 * Siapa boleh melihat kartu modul apa di beranda.
 *
 * Dipakai bersama oleh HomeScreen (menampilkan kartu) dan HomeViewModel
 * (memutuskan query sorotan mana yang perlu ditembak), jadi tidak ada kartu yang
 * tampil tanpa angkanya, dan tidak ada query untuk kartu yang tidak tampil.
 */
/**
 * Role operasional outlet — cermin `isLeaderOrSPV` di `BottomNav.tsx` web, ditambah
 * `crew`.
 *
 * Perbedaan antar role ini hanya pada cakupan outlet, bukan pada fitur: leader dan
 * area manager memegang beberapa outlet binaan lewat `staff_outlets`, regional manager
 * memegang seluruh outlet, dan crew hanya outletnya sendiri. Pembedaan itu sudah
 * ditangani `accessible_outlet_ids()` di database, jadi tidak ada cabang role di sini.
 */
internal val STOK_ROLES_OUTLET = setOf(
    Role.CREW,
    Role.LEADER,
    Role.SPV,
    Role.AREA_MANAGER,
    Role.REGIONAL_MANAGER,
)

/**
 * Role pusat — cermin `isKitchenOrAdmin` di `BottomNav.tsx` web.
 *
 * Merekalah yang menyetujui permintaan bahan (approval memanggil `create_surat_jalan`,
 * artinya barang benar-benar keluar dari Gudang Pusat) dan menerima PO supplier.
 * Sebelumnya himpunan ini tidak ada, sehingga seluruh jalur persetujuan yang sudah
 * dibangun di native tidak bisa dijangkau siapa pun.
 *
 * Mereka TIDAK mendapat tab Dashboard: monitoring saldo bersifat per-outlet, sedangkan
 * role pusat tidak terikat outlet mana pun.
 */
internal val STOK_ROLES_PUSAT = setOf(
    Role.KITCHEN,
    Role.PURCHASING,
    Role.ADMIN,
    Role.ADMIN_FINANCE,
    Role.OWNER,
    Role.DEVELOPER,
)

internal val STOK_ROLES = STOK_ROLES_OUTLET + STOK_ROLES_PUSAT

/**
 * Role yang boleh membuka modul Distribusi. Sama persis dengan
 * `DistribusiAkses.ROLE_MODUL`, disalin ke sini supaya `:feature:home` tidak
 * perlu bergantung pada `:feature:distribusi` hanya untuk satu himpunan.
 *
 * `kitchen` dan `admin` sengaja tidak masuk: penerbitan surat jalan tetap di web,
 * dan database memang hanya mengizinkan mereka menerbitkannya.
 */
internal val DISTRIBUSI_ROLES = setOf(
    Role.CREW,
    Role.LEADER,
    Role.AREA_MANAGER,
    Role.REGIONAL_MANAGER,
)

/**
 * Role yang boleh membuka modul Manager. Sama persis dengan `ManagerAkses.ROLE_MODUL`,
 * disalin ke sini supaya `:feature:home` tidak perlu bergantung pada `:feature:manager`
 * hanya untuk satu himpunan — pola yang sama dipakai STOK_ROLES dan DISTRIBUSI_ROLES.
 *
 * Daftarnya sempit karena memang begitu di web: `middleware.ts` app manager menolak
 * siapa pun selain kedua role ini, termasuk owner dan admin.
 */
internal val MANAGER_ROLES = setOf(
    Role.AREA_MANAGER,
    Role.REGIONAL_MANAGER,
)


/**
 * Role yang boleh membuka modul Leader. Sama persis dengan `LeaderAkses.ROLE_MODUL`,
 * disalin ke sini supaya `:feature:home` tidak perlu bergantung pada `:feature:leader`
 * hanya untuk satu himpunan — pola yang sama dipakai STOK_ROLES, DISTRIBUSI_ROLES,
 * dan MANAGER_ROLES.
 *
 * Cuma satu role, dan memang begitu di web: seluruh isi kelompok "Leader Dashboard"
 * di `navConfig.ts` ditandai `roles: ['LEADER']`, dan `RoleContext.tsx` mengembalikan
 * role itu ke sana setiap kali ia menyimpang.
 */
internal val LEADER_ROLES = setOf(
    Role.LEADER,
)
