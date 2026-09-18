package com.sukashawarma.superapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sukashawarma.superapp.data.local.entity.FaceDescriptorEntity

@Dao
abstract class FaceDescriptorDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun simpanSemua(items: List<FaceDescriptorEntity>)

    @Query("SELECT * FROM face_descriptor_cache WHERE outletId = :outletId")
    abstract suspend fun untukOutlet(outletId: String): List<FaceDescriptorEntity>

    @Query("SELECT MAX(updatedAtMs) FROM face_descriptor_cache WHERE outletId = :outletId")
    abstract suspend fun disegarkanTerakhir(outletId: String): Long?

    @Query("DELETE FROM face_descriptor_cache WHERE outletId = :outletId")
    abstract suspend fun hapusOutlet(outletId: String)

    @Query("DELETE FROM face_descriptor_cache")
    abstract suspend fun hapusSemua()

    /**
     * Mengganti isi satu outlet dalam satu transaksi.
     *
     * Hapus-lalu-simpan, bukan upsert: staf yang sudah resign atau pindah harus HILANG dari
     * perangkat, bukan tertinggal dan tetap bisa absen.
     *
     * Transaksinya bukan hiasan. Sinkronisasi berjalan di latar setiap kali layar absensi
     * dibuka, sementara kamera terus memindai wajah dari coroutine lain. Tanpa transaksi ada
     * jendela beberapa milidetik ketika tabel outlet itu KOSONG, dan crew yang kebetulan
     * memindai persis di jendela itu akan ditolak dengan "data wajah belum tersalin" padahal
     * datanya ada.
     */
    @Transaction
    open suspend fun gantiOutlet(outletId: String, items: List<FaceDescriptorEntity>) {
        hapusOutlet(outletId)
        simpanSemua(items)
    }
}
