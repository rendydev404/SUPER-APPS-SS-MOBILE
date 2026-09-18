package com.sukashawarma.superapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sukashawarma.superapp.data.local.entity.OutboxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboxDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: OutboxEntity)

    /**
     * FIFO, dan hanya yang masih layak dicoba. Urutan penting: permintaan bahan yang dibuat
     * lebih dulu harus sampai lebih dulu supaya nomor antrean di server mengikuti urutan
     * kejadian di lapangan, bukan urutan pulihnya sinyal.
     */
    @Query("SELECT * FROM outbox WHERE status = :status ORDER BY createdAtMs ASC")
    suspend fun menunggu(status: String): List<OutboxEntity>

    @Query("SELECT * FROM outbox WHERE jenis = :jenis AND status = :status ORDER BY createdAtMs ASC")
    suspend fun menungguJenis(jenis: String, status: String): List<OutboxEntity>

    @Query("DELETE FROM outbox WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE outbox SET attemptCount = attemptCount + 1, lastError = :error WHERE id = :id")
    suspend fun markFailedAttempt(id: String, error: String?)

    @Query("UPDATE outbox SET status = :status, lastError = :error WHERE id = :id")
    suspend fun tandaiStatus(id: String, status: String, error: String?)

    /** Untuk pita "N aksi menunggu sinkron". Hanya yang masih akan dicoba lagi. */
    @Query("SELECT COUNT(*) FROM outbox WHERE status = :status")
    fun countFlow(status: String): Flow<Int>

    /** Yang gagal permanen ditampilkan terpisah — user harus mengulanginya sendiri. */
    @Query("SELECT * FROM outbox WHERE status = :status ORDER BY createdAtMs ASC")
    fun gagalFlow(status: String): Flow<List<OutboxEntity>>

    @Query("DELETE FROM outbox")
    suspend fun hapusSemua()
}
