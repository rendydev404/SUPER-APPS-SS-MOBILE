package com.sukashawarma.superapp.feature.leader.data

import com.sukashawarma.superapp.feature.leader.domain.BahanCabang
import com.sukashawarma.superapp.feature.leader.domain.bahanCabang
import com.sukashawarma.superapp.feature.leader.domain.urutkanStok
import com.sukashawarma.superapp.feature.stok.data.StokRepository
import com.sukashawarma.superapp.feature.stok.domain.UnitScale
import com.sukashawarma.superapp.feature.stok.domain.bolehTampilDiOutlet

/**
 * Sisa bahan baku satu cabang.
 *
 * Bukan query sendiri: barisnya diambil dari [StokRepository.monitoringOutlet] dan
 * ambang porsinya dari [StokRepository.accessibleOutlets] — persis yang dipakai
 * `MonitoringViewModel` modul Stok. Dengan begitu daftar bahan, angka saldo, dan
 * status Kritis/Menipis di sini tidak mungkin berbeda dari layar Stok.
 *
 * Halaman web membaca `inventory_batches`/`inventory_items`/`inventory_units`.
 * Ketiganya TIDAK ADA di database ini — migrasi `20260709000001_merge_fifo_po`
 * membatalkan rencana FIFO dan malah menghapus `inventory_items`. Karena itu layar
 * stok leader di web selalu berbunyi "Tidak Ada Data"; native membacanya dari
 * tempat stok yang sungguhan tersimpan.
 */
object StokCabangRepository {

    suspend fun bahan(outletId: String): List<BahanCabang> {
        val marquee = StokRepository.accessibleOutlets()
            .firstOrNull { it.id == outletId }
            ?.marqueeWarningThreshold
            ?: UnitScale.DEFAULT_MARQUEE_WARNING
        val baris = StokRepository.monitoringOutlet(outletId)
            .filter { bolehTampilDiOutlet(it.itemName, it.outletName) }
            .map { bahanCabang(it, marquee) }
        return urutkanStok(baris)
    }
}
