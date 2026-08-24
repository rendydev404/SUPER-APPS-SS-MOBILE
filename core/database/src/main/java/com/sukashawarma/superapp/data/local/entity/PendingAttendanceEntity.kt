package com.sukashawarma.superapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Antrean absensi offline — DURABLE (Room), beda dari kiosk web yang antreannya
 * in-memory dan hilang kalau tab ditutup. `id` = UUID idempotency key yang sama
 * dikirim ke server, jadi retry ganda tidak membuat baris dobel (server upsert
 * onConflict:id).
 */
@Entity(tableName = "pending_attendance")
data class PendingAttendanceEntity(
    @PrimaryKey val id: String,
    val outletId: String,
    val outletStaffId: String,
    val type: String, // "in" | "out"
    val gpsLat: Double?,
    val gpsLng: Double?,
    val gpsAccuracy: Float?,
    val isMock: Boolean,
    val isManualButton: Boolean,
    val tsClientIso: String,
    val selfiePath: String?,
    val createdAtMs: Long,
    val attemptCount: Int = 0,
    val lastError: String? = null,
)

