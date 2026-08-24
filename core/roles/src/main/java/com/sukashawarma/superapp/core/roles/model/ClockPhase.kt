package com.sukashawarma.superapp.domain.model

/** Cermin KioskPhase di useClockKiosk.ts (web). */
enum class ClockPhase {
    LOCATING, LOCATION_INVALID, LOCKED, IDLE, IDENTIFIED, LIVENESS, SUBMITTING, RESULT
}

data class ClockResult(val ok: Boolean, val message: String)
