package com.sukashawarma.superapp.feature.manager.domain

/** Satu baris rincian pesanan yang diminta dibatalkan. */
data class ItemPesananVoid(val nama: String, val qty: Int, val subtotal: Long)

/**
 * Pengajuan pembatalan transaksi dari kasir.
 *
 * Hanya berisi pengajuan yang pesanannya BENAR-BENAR boleh dilihat pengguna.
 * Pengajuan yang pesanannya tidak terbaca dibuang di repository — lihat
 * `PersetujuanRepository.petakanVoid` untuk alasannya. Karena itu seluruh kolom di
 * sini non-null: tidak ada lagi keadaan "ada barisnya tapi isinya tidak diketahui".
 */
data class PengajuanVoid(
    val id: String,
    val alasan: String,
    val dibuatPada: String,
    val nomorOrder: String,
    val namaPelanggan: String,
    val total: Long,
    val outletNama: String,
    val pemohon: String,
    val items: List<ItemPesananVoid>,
)

/** Pengajuan bypass kuncian POS dari kasir. */
data class PengajuanBypass(
    val id: String,
    val outletId: String,
    val outletNama: String,
    val pemohon: String,
    val alasan: String,
    val dibuatPada: String,
)

enum class TabPersetujuan(val label: String) {
    VOID("Void Transaksi"),
    BYPASS("Bypass POS"),
}

/** Menyaring pengajuan pada rentang tanggal terpilih. */
fun <T> saringPeriode(
    daftar: List<T>,
    rentang: RentangTanggal,
    tanggalDari: (T) -> String,
): List<T> = daftar.filter { item ->
    val tanggal = tanggalJakarta(tanggalDari(item)) ?: return@filter false
    !tanggal.isBefore(rentang.dari) && !tanggal.isAfter(rentang.sampai)
}
