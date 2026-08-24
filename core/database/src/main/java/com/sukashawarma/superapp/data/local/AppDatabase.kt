package com.sukashawarma.superapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sukashawarma.superapp.data.local.dao.PendingAttendanceDao
import com.sukashawarma.superapp.data.local.entity.PendingAttendanceEntity

@Database(entities = [PendingAttendanceEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingAttendanceDao(): PendingAttendanceDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "suka_superapp.db")
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

