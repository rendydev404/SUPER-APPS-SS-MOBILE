package com.sukashawarma.superapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sukashawarma.superapp.data.local.dao.PendingAttendanceDao
import com.sukashawarma.superapp.data.local.dao.PendingOpnameFinalizeDao
import com.sukashawarma.superapp.data.local.entity.PendingAttendanceEntity
import com.sukashawarma.superapp.data.local.entity.PendingOpnameFinalizeEntity

@Database(
    entities = [PendingAttendanceEntity::class, PendingOpnameFinalizeEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingAttendanceDao(): PendingAttendanceDao
    abstract fun pendingOpnameFinalizeDao(): PendingOpnameFinalizeDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /**
         * v1 -> v2: menambah antrean finalisasi opname.
         *
         * Ditulis sebagai migrasi, BUKAN `fallbackToDestructiveMigration()`: tabel
         * `pending_attendance` bisa sedang memuat absensi yang belum terkirim, dan
         * menghapusnya berarti kehilangan jam masuk seseorang yang tidak ada
         * salinannya di mana pun.
         *
         * Skema di bawah harus persis sama dengan yang dihasilkan Room dari
         * [PendingOpnameFinalizeEntity] — Room memverifikasinya saat membuka database,
         * jadi ketidakcocokan akan muncul sebagai crash saat start, bukan diam-diam.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pending_opname_finalize` (
                        `opnameId` TEXT NOT NULL,
                        `outletId` TEXT NOT NULL,
                        `createdAtMs` INTEGER NOT NULL,
                        `attemptCount` INTEGER NOT NULL,
                        `lastError` TEXT,
                        PRIMARY KEY(`opnameId`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "suka_superapp.db")
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
