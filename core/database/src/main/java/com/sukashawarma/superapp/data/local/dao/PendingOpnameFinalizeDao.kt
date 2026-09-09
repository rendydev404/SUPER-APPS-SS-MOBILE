package com.sukashawarma.superapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sukashawarma.superapp.data.local.entity.PendingOpnameFinalizeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingOpnameFinalizeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PendingOpnameFinalizeEntity)

    @Query("SELECT * FROM pending_opname_finalize ORDER BY createdAtMs ASC")
    suspend fun getAll(): List<PendingOpnameFinalizeEntity>

    @Query("DELETE FROM pending_opname_finalize WHERE opnameId = :opnameId")
    suspend fun delete(opnameId: String)

    @Query(
        "UPDATE pending_opname_finalize " +
            "SET attemptCount = attemptCount + 1, lastError = :error WHERE opnameId = :opnameId"
    )
    suspend fun markFailedAttempt(opnameId: String, error: String?)

    @Query("SELECT COUNT(*) FROM pending_opname_finalize")
    fun countFlow(): Flow<Int>
}
