package com.sukashawarma.superapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sukashawarma.superapp.data.local.entity.FaceDescriptorEntity

@Dao
interface FaceDescriptorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun simpanSemua(items: List<FaceDescriptorEntity>)

    @Query("SELECT * FROM face_descriptor_cache WHERE outletId = :outletId")
    suspend fun untukOutlet(outletId: String): List<FaceDescriptorEntity>

    @Query("SELECT MAX(updatedAtMs) FROM face_descriptor_cache WHERE outletId = :outletId")
    suspend fun disegarkanTerakhir(outletId: String): Long?

    /**
     * Sinkronisasi mengganti isi outlet, bukan menambah: staf yang sudah resign atau pindah
     * harus HILANG dari perangkat, bukan tertinggal dan tetap bisa absen.
     */
    @Query("DELETE FROM face_descriptor_cache WHERE outletId = :outletId")
    suspend fun hapusOutlet(outletId: String)

    @Query("DELETE FROM face_descriptor_cache")
    suspend fun hapusSemua()
}
