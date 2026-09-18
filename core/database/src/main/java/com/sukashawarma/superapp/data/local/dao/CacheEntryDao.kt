package com.sukashawarma.superapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sukashawarma.superapp.data.local.entity.CacheEntryEntity

@Dao
interface CacheEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun simpan(item: CacheEntryEntity)

    @Query("SELECT * FROM cache_entry WHERE kunci = :kunci")
    suspend fun ambil(kunci: String): CacheEntryEntity?

    @Query("DELETE FROM cache_entry WHERE kunci = :kunci")
    suspend fun hapus(kunci: String)

    /** Dipanggil saat staf pindah outlet: angka outlet lama tidak boleh muncul di outlet baru. */
    @Query("DELETE FROM cache_entry WHERE scope IS NOT NULL AND scope <> :scope")
    suspend fun hapusSelainScope(scope: String)

    /** Dipanggil saat logout — cache adalah data perusahaan, bukan milik perangkat. */
    @Query("DELETE FROM cache_entry")
    suspend fun hapusSemua()
}
