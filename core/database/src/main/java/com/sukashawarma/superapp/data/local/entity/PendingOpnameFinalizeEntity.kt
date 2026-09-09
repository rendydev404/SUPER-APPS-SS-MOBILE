package com.sukashawarma.superapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Antrean finalisasi opname yang belum sampai ke server — cermin
 * `useOfflineQueue('stok-opname-finalize')` di web, tetapi DURABLE: web menyimpannya
 * di memori tab, native di Room sehingga selamat dari aplikasi yang ditutup.
 *
 * Yang diantre hanya finalisasinya, bukan hitungan fisiknya. Itu disengaja dan sama
 * dengan web: item sudah lebih dulu ditulis ke `opname_item` lewat upsert, jadi bila
 * finalisasi gagal, angka kru TIDAK hilang — yang tertunda cuma pemotongan stoknya.
 * Mengantre itemnya juga berarti menyimpan hitungan ganda yang bisa berbeda dari isi
 * server, dan itu sumber kekeliruan yang lebih mahal daripada finalisasi yang telat.
 *
 * [opnameId] sekaligus primary key: memfinalisasi opname yang sama dua kali tidak
 * berarti apa-apa, jadi antreannya cukup memuat satu baris per opname.
 */
@Entity(tableName = "pending_opname_finalize")
data class PendingOpnameFinalizeEntity(
    @PrimaryKey val opnameId: String,
    val outletId: String,
    val createdAtMs: Long,
    val attemptCount: Int = 0,
    val lastError: String? = null,
)
