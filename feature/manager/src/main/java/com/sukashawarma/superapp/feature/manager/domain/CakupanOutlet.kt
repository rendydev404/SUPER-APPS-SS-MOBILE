package com.sukashawarma.superapp.feature.manager.domain

import com.sukashawarma.superapp.domain.model.Role

/**
 * Outlet mana yang boleh dilihat pengguna.
 *
 * Ini SATU-SATUNYA hal di modul ini yang tidak bisa diserahkan ke RLS. Tabel
 * `outlets` punya policy `outlets_select_authenticated` berisi `USING (true)` —
 * siapa pun yang bersesi membaca seluruh cabang. Tabel transaksinya (`orders`,
 * `attendance`, `stok_waste_reports`, `opname`, `petty_cash_topups`,
 * `inventaris_submissions`) sudah discope `accessible_outlet_ids()`, jadi ANGKAnya
 * benar; yang salah tanpa penyaring ini adalah DAFTAR outletnya.
 *
 * Akibatnya nyata: area manager melihat ~30 cabang di "Status Outlet" — sebagian
 * besar tertulis tutup dengan omzet nol — padahal ia hanya membina beberapa.
 *
 * Web menyaringnya dengan cara yang sama (`getScopedOutletIds` dan
 * `accessibleOutlets` di `app/page.tsx`), bukan dengan mengubah policy.
 */
sealed interface CakupanOutlet {

    /** Regional manager dan role pusat: seluruh cabang. */
    data object Semua : CakupanOutlet

    /**
     * Area manager: hanya cabang binaannya, sesuai baris `staff_outlets` miliknya.
     * Himpunan kosong berarti belum ditugaskan ke outlet mana pun — dan itu berarti
     * TIDAK melihat apa pun, bukan melihat semuanya.
     */
    data class Binaan(val ids: Set<String>) : CakupanOutlet

    fun mencakup(outletId: String): Boolean = when (this) {
        Semua -> true
        is Binaan -> outletId in ids
    }
}

/**
 * Role yang memang memegang seluruh cabang.
 *
 * Disalin dari cabang pertama `accessible_outlet_ids()` di database supaya
 * keduanya bisa dibandingkan baris per baris. `area_manager` sengaja TIDAK ada di
 * sini — di fungsi database pun ia masuk cabang `staff_outlets`, bukan cabang ini.
 */
val ROLE_SELURUH_OUTLET: Set<Role> = setOf(
    Role.REGIONAL_MANAGER,
    Role.ADMIN,
    Role.ADMIN_HR,
    Role.OWNER,
    Role.SPV,
    Role.KITCHEN,
    Role.ADMIN_FINANCE,
    Role.PURCHASING,
)

/**
 * @param idBinaan outlet dari `staff_outlets` milik pengguna; diabaikan untuk role
 *   yang memegang seluruh cabang.
 */
fun cakupanUntuk(role: Role?, idBinaan: Set<String>): CakupanOutlet =
    if (role in ROLE_SELURUH_OUTLET) CakupanOutlet.Semua else CakupanOutlet.Binaan(idBinaan)

/** Menyaring daftar apa pun berdasarkan outlet id-nya. */
fun <T> saringCakupan(daftar: List<T>, cakupan: CakupanOutlet, outletId: (T) -> String): List<T> =
    when (cakupan) {
        CakupanOutlet.Semua -> daftar
        is CakupanOutlet.Binaan -> daftar.filter { outletId(it) in cakupan.ids }
    }
