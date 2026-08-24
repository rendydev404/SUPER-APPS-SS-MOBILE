package com.sukashawarma.superapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sukashawarma.superapp.data.local.entity.PendingAttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingAttendanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PendingAttendanceEntity)

    @Query("SELECT * FROM pending_attendance ORDER BY createdAtMs ASC")
    suspend fun getAll(): List<PendingAttendanceEntity>

    @Query("DELETE FROM pending_attendance WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE pending_attendance SET attemptCount = attemptCount + 1, lastError = :error WHERE id = :id")
    suspend fun markFailedAttempt(id: String, error: String?)

    @Query("SELECT COUNT(*) FROM pending_attendance")
    fun countFlow(): Flow<Int>
}

