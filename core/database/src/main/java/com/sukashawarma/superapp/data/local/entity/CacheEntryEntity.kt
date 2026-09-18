package com.sukashawarma.superapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Salinan terakhir sebuah pembacaan yang berhasil, dipakai HANYA sebagai fallback saat
 * jaringan mati — bukan untuk menjadikan aplikasi offline-first. Layar tetap memanggil
 * server lebih dulu; baris di sini cuma menahan agar layar tidak jadi kosong total.
 *
 * [payload] sengaja disimpan sebagai JSON mentah, bukan tabel per-fitur. Satu tabel generik
 * berarti menambah layar baru ke cache tidak butuh migrasi Room lagi, dan bentuk yang
 * disimpan persis sama dengan yang dikembalikan PostgREST sehingga tidak ada dua
 * representasi yang bisa berbeda diam-diam.
 *
 * [scope] menampung id outlet/staf agar cache milik outlet lain bisa dibuang sekaligus saat
 * seseorang pindah outlet — tanpa ini, angka stok outlet lama bisa muncul di outlet baru.
 */
@Entity(tableName = "cache_entry")
data class CacheEntryEntity(
    @PrimaryKey val kunci: String,
    val payload: String,
    val fetchedAtMs: Long,
    val scope: String? = null,
)
