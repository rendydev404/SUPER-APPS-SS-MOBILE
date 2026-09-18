package com.sukashawarma.superapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sukashawarma.superapp.data.local.dao.CacheEntryDao
import com.sukashawarma.superapp.data.local.dao.FaceDescriptorDao
import com.sukashawarma.superapp.data.local.dao.OutboxDao
import com.sukashawarma.superapp.data.local.dao.PendingAttendanceDao
import com.sukashawarma.superapp.data.local.dao.PendingOpnameFinalizeDao
import com.sukashawarma.superapp.data.local.entity.CacheEntryEntity
import com.sukashawarma.superapp.data.local.entity.FaceDescriptorEntity
import com.sukashawarma.superapp.data.local.entity.OutboxEntity
import com.sukashawarma.superapp.data.local.entity.PendingAttendanceEntity
import com.sukashawarma.superapp.data.local.entity.PendingOpnameFinalizeEntity

@Database(
    entities = [
        PendingAttendanceEntity::class,
        PendingOpnameFinalizeEntity::class,
        CacheEntryEntity::class,
        OutboxEntity::class,
        FaceDescriptorEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingAttendanceDao(): PendingAttendanceDao
    abstract fun pendingOpnameFinalizeDao(): PendingOpnameFinalizeDao
    abstract fun cacheEntryDao(): CacheEntryDao
    abstract fun outboxDao(): OutboxDao
    abstract fun faceDescriptorDao(): FaceDescriptorDao

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

        /** v2 -> v3: nomor shift pilihan crew ikut tersimpan di antrean absen offline. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `pending_attendance` ADD COLUMN `shiftKe` INTEGER")
            }
        }

        /**
         * v3 -> v4: fondasi mode offline umum — cache baca, antrean tulis (outbox), dan
         * salinan descriptor wajah per outlet.
         *
         * `cache_entry` dan `face_descriptor_cache` boleh hilang tanpa akibat (keduanya
         * salinan dari server), tetapi `outbox` TIDAK: isinya kerja yang sudah dilakukan
         * orang di lapangan dan belum ada di mana pun selain perangkat ini. Karena itu
         * tetap migrasi eksplisit, bukan destructive — alasan yang sama dengan MIGRATION_1_2.
         *
         * `outbox.status` diindeks: pita "N aksi menunggu sinkron" mengamatinya lewat Flow
         * yang dievaluasi ulang setiap kali tabelnya berubah.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cache_entry` (
                        `kunci` TEXT NOT NULL,
                        `payload` TEXT NOT NULL,
                        `fetchedAtMs` INTEGER NOT NULL,
                        `scope` TEXT,
                        PRIMARY KEY(`kunci`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `outbox` (
                        `id` TEXT NOT NULL,
                        `jenis` TEXT NOT NULL,
                        `payload` TEXT NOT NULL,
                        `tsClientIso` TEXT NOT NULL,
                        `createdAtMs` INTEGER NOT NULL,
                        `outletId` TEXT,
                        `dibuatOleh` TEXT,
                        `lampiranPath` TEXT,
                        `lampiranBucket` TEXT,
                        `lampiranTujuan` TEXT,
                        `attemptCount` INTEGER NOT NULL,
                        `lastError` TEXT,
                        `status` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_outbox_status` ON `outbox` (`status`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `face_descriptor_cache` (
                        `staffId` TEXT NOT NULL,
                        `outletId` TEXT NOT NULL,
                        `nama` TEXT NOT NULL,
                        `descriptor` BLOB NOT NULL,
                        `updatedAtMs` INTEGER NOT NULL,
                        PRIMARY KEY(`staffId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_face_descriptor_cache_outletId` " +
                        "ON `face_descriptor_cache` (`outletId`)"
                )
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "suka_superapp.db")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
