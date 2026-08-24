package com.sukashawarma.superapp.presentation.absensi.clock

import com.sukashawarma.superapp.domain.gps.LatLng
import com.sukashawarma.superapp.domain.liveness.Challenge
import com.sukashawarma.superapp.domain.model.ClockPhase
import com.sukashawarma.superapp.domain.model.ClockResult

data class ClockUiState(
    val phase: ClockPhase = ClockPhase.LOCATING,
    val outletCoords: LatLng? = null,
    val deviceCoords: LatLng? = null,
    val deviceAccuracy: Float? = null,
    val gpsDistanceM: Double? = null,
    /** Sudah lolos verifikasi geofence sekali di sesi ini — clock-in/out berikutnya tidak
     *  perlu menampilkan layar "Memeriksa Lokasi GPS..." lagi (lihat [ClockViewModel.scheduleReset]).
     *  Ini murni pengurang friksi UX di client; validasi geofence yang presisi & otoritatif
     *  TETAP dijalankan server-side tiap submit lewat RPC `submit_attendance` (lihat
     *  SubmitAttendanceUseCase) — jadi mengunci ini tidak melemahkan keamanan absensi. */
    val locationLocked: Boolean = false,
    val result: ClockResult? = null,
    val whoId: String? = null,
    val whoName: String? = null,
    val action: String = "in", // "in" | "out"
    val challenge: Challenge? = null,
    val pendingCount: Int = 0,
    val isOnline: Boolean = true,
)


