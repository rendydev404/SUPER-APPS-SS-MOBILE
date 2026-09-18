package com.sukashawarma.superapp.feature.stok.data


/**
 * Entri manual ledger dan pelaporan waste — cermin `ManualEntryForm.tsx` dan
 * `WasteModal.tsx` di web.
 *
 * Keduanya satu berkas karena di web pun satu formulir: penyesuaian dan transfer
 * keluar masuk langsung ke `ledger_stok`, sedangkan waste TIDAK — waste harus
 * melewati `stok_waste_reports` dan persetujuan, lalu trigger
 * `process_waste_report_approval` yang menuliskannya ke ledger. Menulis waste
 * langsung ke ledger dari sini akan memotong seluruh rantai persetujuan itu.
 *
 * Tidak ada service-role di sini dan memang tidak perlu: `waste_reports_insert`
 * berbunyi `TO authenticated WITH CHECK (outlet_id IN accessible_outlet_ids())`,
 * jadi JWT pengguna sudah cukup. Web memakai service-role hanya karena seluruh
 * Server Action-nya memang begitu.
 */
object EntriManualRepository {

    /** Alasan waste — daftar yang sama persis dengan dropdown di `WasteModal.tsx`. */
    val ALASAN_WASTE = listOf(
        "Basi / Expired",
        "Jatuh / Tumpah",
        "Gosong / Rusak Masak",
        "Kualitas Buruk (dari supplier)",
        "Lainnya",
    )

    // Pengunggahan foto dan penulisan `stok_waste_reports` pindah ke
    // `feature/stok/offline/AksiStokOffline.kt` (WasteOffline). Keduanya kini memakai jalur
    // yang sama dengan antrean offline, sehingga laporan waste tetap bisa dibuat saat
    // internet mati — dan tidak ada dua salinan logika penulisan yang bisa menyimpang.
}
