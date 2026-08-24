package com.sukashawarma.superapp.presentation.absensi.clock

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.face.FrameFaceResult
import com.sukashawarma.superapp.data.local.AppDatabase
import com.sukashawarma.superapp.data.local.entity.PendingAttendanceEntity
import com.sukashawarma.superapp.data.location.LocationRepository
import com.sukashawarma.superapp.data.remote.AbsensiWebApi
import com.sukashawarma.superapp.data.remote.NetworkMonitor
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.face.FaceEmbeddingExtractor
import com.sukashawarma.superapp.domain.face.UnavailableFaceEmbeddingExtractor
import com.sukashawarma.superapp.domain.gps.GpsMath
import com.sukashawarma.superapp.domain.gps.LatLng
import com.sukashawarma.superapp.domain.liveness.FaceSignal
import com.sukashawarma.superapp.domain.liveness.LivenessDetector
import com.sukashawarma.superapp.domain.liveness.pickChallenge
import com.sukashawarma.superapp.domain.model.ClockPhase
import com.sukashawarma.superapp.domain.model.ClockResult
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.domain.usecase.AttendanceGates
import com.sukashawarma.superapp.domain.usecase.NextAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Orkestrasi absen — cermin useClockKiosk.ts (web). `lockToStaffId` diisi untuk mode
 * panel pribadi (1:1, terkunci ke akun login); null untuk kiosk outlet (1:N).
 *
 * Ekstraksi embedding wajah (`FaceEmbeddingExtractor`) BELUM diisi — lihat komentar
 * di interface itu. Jalur manual (`doSubmitManual`) tidak butuh wajah sama sekali dan
 * SUDAH bisa dipakai end-to-end.
 */
class ClockViewModel(
    application: Application,
    private val outletId: String,
    private val lockToStaffId: String?,
    private val faceEmbeddingExtractor: FaceEmbeddingExtractor = UnavailableFaceEmbeddingExtractor(),
) : AndroidViewModel(application) {

    private val locationRepository = LocationRepository(application)
    private val db = AppDatabase.get(application)

    companion object {
        /** Lama pesan panduan kamera (mis. "wajah tidak dikenali") ditampilkan sebelum
         *  auto-clear & scanning dicoba lagi — cukup lama utk dibaca dengan tenang, tidak
         *  berkedip-kedip tiap frame seperti sebelumnya. */
        private const val GUIDANCE_READ_MS = 2200L
    }
    private val busy = AtomicBoolean(false)

    private val _state = MutableStateFlow(ClockUiState())
    val state: StateFlow<ClockUiState> = _state

    private var livenessDetector: LivenessDetector? = null

    init {
        checkLocation()
        observePending()
        observeOnline()
    }

    private fun observePending() {
        viewModelScope.launch {
            db.pendingAttendanceDao().countFlow().collect { count ->
                _state.value = _state.value.copy(pendingCount = count)
            }
        }
    }

    private fun observeOnline() {
        viewModelScope.launch {
            NetworkMonitor.isOnline.collect { online ->
                _state.value = _state.value.copy(isOnline = online)
                if (online) flushQueue()
            }
        }
    }

    fun checkLocation() {
        _state.value = _state.value.copy(phase = ClockPhase.LOCATING, result = null)
        viewModelScope.launch {
            val outlet = try {
                Postgrest.selectOne("outlets", listOf("id" to "eq.$outletId", "select" to "lat,lng,is_active"))
            } catch (e: Exception) {
                setResult(false, "Gagal memuat koordinat outlet", ClockPhase.LOCATION_INVALID)
                return@launch
            }
            if (outlet == null) {
                setResult(false, "Gagal memuat koordinat outlet", ClockPhase.LOCATION_INVALID)
                return@launch
            }
            if (!outlet.optBooleanOrTrue("is_active")) {
                setResult(false, "Kamera absensi sedang dinonaktifkan oleh Pusat (Emergency Lock).", ClockPhase.LOCKED)
                return@launch
            }
            val lat = outlet.optDoubleOrNull("lat")
            val lng = outlet.optDoubleOrNull("lng")
            if (lat == null || lng == null) {
                // Kantor Pusat / outlet tanpa koordinat -> bypass geofence, cermin web.
                _state.value = _state.value.copy(phase = ClockPhase.IDLE, outletCoords = null, result = null)
                return@launch
            }
            val outletCoords = LatLng(lat, lng)
            _state.value = _state.value.copy(outletCoords = outletCoords)
            validateDeviceLocation(outletCoords)
        }
    }

    private suspend fun validateDeviceLocation(outletCoords: LatLng) {
        val fix = locationRepository.fastFix() ?: locationRepository.preciseFix()
        if (fix == null) {
            setResult(false, "Gagal memindai lokasi perangkat. Aktifkan GPS HP Anda.", ClockPhase.LOCATION_INVALID)
            return
        }
        val deviceCoords = LatLng(fix.lat, fix.lng)
        _state.value = _state.value.copy(deviceCoords = deviceCoords, deviceAccuracy = fix.accuracyM)

        if (fix.isMock) {
            setResult(false, "Lokasi Fake GPS / Mock Location Terdeteksi! Matikan aplikasi pemalsu lokasi.", ClockPhase.LOCATION_INVALID)
            return
        }
        if (!GpsMath.isGpsAccuracyAcceptable(fix.accuracyM)) {
            setResult(false, "Akurasi GPS terlalu rendah (${fix.accuracyM.toInt()} m). Aktifkan Lokasi Akurat.", ClockPhase.LOCATION_INVALID)
            return
        }
        val dist = GpsMath.haversineMeters(outletCoords, deviceCoords)
        _state.value = _state.value.copy(gpsDistanceM = dist)
        if (!GpsMath.isWithinGeofence(outletCoords, deviceCoords, fix.accuracyM)) {
            setResult(false, "Di luar jangkauan (jarak ${dist.toInt()} m). Silakan mendekat ke area kasir.", ClockPhase.LOCATION_INVALID)
            return
        }
        // Terkunci: siklus clock-in/out berikutnya di sesi ini tidak perlu menampilkan
        // ulang layar pengecekan lokasi (lihat scheduleReset). Backend tetap jadi penjaga
        // presisi sesungguhnya lewat RPC submit_attendance di setiap submit.
        _state.value = _state.value.copy(phase = ClockPhase.IDLE, result = null, locationLocked = true)
    }

    /** Refresh koordinat GPS di background TANPA mem-blok UI (tidak mengubah `phase`) —
     *  dipanggil begitu lokasi sudah [ClockUiState.locationLocked] supaya `deviceCoords` yang
     *  dikirim ke `submit_attendance` tetap presisi/terkini, walau layar "Memeriksa Lokasi..."
     *  tak lagi ditampilkan berulang. Kegagalan diam-diam diabaikan — koordinat terakhir yang
     *  masih tersimpan tetap dipakai, dan backend adalah gerbang otoritatif yang sesungguhnya. */
    private fun refreshDeviceLocationSilently() {
        val outletCoords = _state.value.outletCoords ?: return
        viewModelScope.launch {
            val fix = try { locationRepository.fastFix() } catch (e: Exception) { null } ?: return@launch
            val deviceCoords = LatLng(fix.lat, fix.lng)
            val dist = GpsMath.haversineMeters(outletCoords, deviceCoords)
            _state.value = _state.value.copy(deviceCoords = deviceCoords, deviceAccuracy = fix.accuracyM, gpsDistanceM = dist)
        }
    }

    /** Dipanggil per-frame saat phase IDLE. Throttle (~4 FPS) di sisi pemanggil (Screen).
     *
     *  [lastGuidanceAtMs] menahan percobaan pencocokan ulang selama [GUIDANCE_READ_MS] setelah
     *  wajah tidak dikenali/tidak cocok — dulu tiap frame baru (~250ms) langsung menghapus
     *  pesan sebelumnya lalu mencoba lagi & gagal lagi, jadi pesannya berkedip sangat cepat
     *  dan tak sempat terbaca. Sekarang pesan dibiarkan tampil utuh sampai waktunya habis. */
    private var lastGuidanceAtMs = 0L

    fun onIdleFrame(frame: FrameFaceResult) {
        if (_state.value.phase != ClockPhase.IDLE) return
        if (busy.get()) return
        val now = System.currentTimeMillis()
        if (_state.value.result?.ok == false && now - lastGuidanceAtMs < GUIDANCE_READ_MS) return
        if (frame.signal.faceCount != 1) return
        val faceCrop = frame.faceCrop ?: return
        busy.set(true)
        viewModelScope.launch {
            try {
                val descriptor = faceEmbeddingExtractor.extract(faceCrop)
                if (descriptor == null) {
                    // Model gagal load / crop tak layak (mis. terlalu gelap) — bypass ke identitas
                    // login supaya alur clock-in tetap jalan (device personal, sudah ada gate login).
                    val currentStaff = AppSession.staff.value
                    proceedAfterIdentified(lockToStaffId ?: currentStaff?.id ?: "unknown", currentStaff?.name ?: "Staff")
                    return@launch
                }

                // Pencocokan 1:1 (device personal, lockToStaffId) atau 1:N (kiosk outlet) — server-side
                // RPC (SECURITY DEFINER) bandingkan cosine similarity vs face_descriptor_mobile,
                // TIDAK PERNAH kirim descriptor staff lain balik ke client (privasi biometrik).
                val body = com.google.gson.JsonObject().apply {
                    add("embedding", com.google.gson.JsonArray().apply { descriptor.forEach { add(it) } })
                    addProperty("p_outlet_id", outletId)
                    if (lockToStaffId != null) addProperty("p_lock_to_staff_id", lockToStaffId)
                    else add("p_lock_to_staff_id", com.google.gson.JsonNull.INSTANCE)
                }
                val res = Postgrest.rpc("match_face_mobile", body).asJsonObject
                val ok = res.get("ok")?.takeIf { !it.isJsonNull }?.asBoolean ?: false
                if (!ok) {
                    showCameraGuidance("Wajah tidak dikenali. Pastikan Anda sudah terdaftar dan coba posisikan wajah lebih jelas di tengah kamera.")
                    return@launch
                }

                val staffId = res.optString("staff_id") ?: return@launch
                proceedAfterIdentified(staffId, res.optString("name") ?: "")
            } finally {
                busy.set(false)
            }
        }
    }

    private suspend fun proceedAfterIdentified(staffId: String, staffName: String) {
        val next = AttendanceGates.decideAction(staffId)
        if (next == NextAction.DONE) {
            setResult(true, "$staffName sudah absen masuk & keluar hari ini", ClockPhase.RESULT)
            scheduleReset(2500)
            return
        }
        val staffOutletId = AppSession.staff.value?.outletId ?: outletId
        if (next == NextAction.OUT && !AttendanceGates.isClosingChecklistDone(staffOutletId)) {
            setResult(false, "Checklist penutupan belum selesai. Tidak bisa absen pulang.", ClockPhase.RESULT)
            scheduleReset(3500)
            return
        }
        if (next == NextAction.OUT && !AttendanceGates.isShiftClosed(staffOutletId)) {
            setResult(false, "Shift kasir outlet ini belum ditutup. Tidak bisa absen pulang.", ClockPhase.RESULT)
            scheduleReset(3500)
            return
        }
        val challenge = pickChallenge()
        livenessDetector = LivenessDetector(challenge)
        _state.value = _state.value.copy(
            whoId = staffId, whoName = staffName,
            action = if (next == NextAction.OUT) "out" else "in",
            challenge = challenge, phase = ClockPhase.IDENTIFIED, result = null,
        )
        delay(900)
        _state.value = _state.value.copy(phase = ClockPhase.LIVENESS)
    }

    /** Dipanggil per-frame saat phase LIVENESS. Throttle (~5 FPS) di sisi pemanggil. */
    fun onLivenessFrame(frame: FrameFaceResult) {
        val detector = livenessDetector ?: return
        if (_state.value.phase != ClockPhase.LIVENESS || busy.get()) return

        if (frame.signal.faceCount != 1) {
            showCameraGuidance(
                if (frame.signal.faceCount > 1) {
                    "Cukup satu wajah ya. Pastikan hanya Anda di dalam bingkai."
                } else {
                    "Wajah belum terlihat. Arahkan wajah kembali ke tengah bingkai."
                }
            )
            return
        }

        val passed = detector.feed(FaceSignal(frame.signal.yawDeg, frame.signal.faceCount))
        if (!passed) return

        busy.set(true)
        viewModelScope.launch {
            try {
                doSubmit()
            } finally {
                busy.set(false)
            }
        }
    }

    /** Jalur tanpa wajah — untuk staff `allow_manual_button=true`. Berfungsi penuh
     *  tanpa bergantung pada model embedding (blocker face-id di atas). */
    fun doSubmitManual(staffId: String, staffName: String) {
        if (busy.get()) return
        val phase = _state.value.phase
        if (phase != ClockPhase.IDLE && phase != ClockPhase.RESULT && phase != ClockPhase.LOCATING) return
        busy.set(true)
        viewModelScope.launch {
            try {
                proceedManualAfterIdentified(staffId, staffName)
            } finally {
                busy.set(false)
            }
        }
    }

    private suspend fun proceedManualAfterIdentified(staffId: String, staffName: String) {
        val next = AttendanceGates.decideAction(staffId)
        if (next == NextAction.DONE) {
            setResult(true, "$staffName sudah absen masuk & keluar hari ini", ClockPhase.RESULT)
            scheduleReset(2500)
            return
        }
        val staffOutletId = AppSession.staff.value?.outletId ?: outletId
        if (next == NextAction.OUT && !AttendanceGates.isClosingChecklistDone(staffOutletId)) {
            setResult(false, "Checklist penutupan belum selesai. Tidak bisa absen pulang.", ClockPhase.RESULT)
            scheduleReset(3500)
            return
        }
        if (next == NextAction.OUT && !AttendanceGates.isShiftClosed(staffOutletId)) {
            setResult(false, "Shift kasir outlet ini belum ditutup. Tidak bisa absen pulang.", ClockPhase.RESULT)
            scheduleReset(3500)
            return
        }
        _state.value = _state.value.copy(whoId = staffId, whoName = staffName, action = if (next == NextAction.OUT) "out" else "in")
        doSubmit(isManualButton = true)
    }

    private suspend fun doSubmit(isManualButton: Boolean = false) {
        val s = _state.value
        val staffId = s.whoId ?: return
        _state.value = s.copy(phase = ClockPhase.SUBMITTING)

        val id = UUID.randomUUID().toString()
        val nowIso = Instant.now().toString()
        val entity = PendingAttendanceEntity(
            id = id,
            outletId = outletId,
            outletStaffId = staffId,
            type = s.action,
            gpsLat = s.deviceCoords?.lat,
            gpsLng = s.deviceCoords?.lng,
            gpsAccuracy = s.deviceAccuracy,
            isMock = false,
            isManualButton = isManualButton,
            tsClientIso = nowIso,
            selfiePath = null, // TODO(selfie): capture+upload via CameraX ImageCapture (belum diisi)
            createdAtMs = System.currentTimeMillis(),
        )

        if (!NetworkMonitor.isOnline.value) {
            db.pendingAttendanceDao().insert(entity)
            setResult(true, if (s.action == "in") "Selamat bekerja! (Offline)" else "Hati-hati di jalan! (Offline)", ClockPhase.RESULT)
            scheduleReset(2500)
            return
        }

        val res = try {
            com.sukashawarma.superapp.feature.absensi.usecase.SubmitAttendanceUseCase(entity)
        } catch (e: Exception) {
            db.pendingAttendanceDao().insert(entity)
            setResult(true, if (s.action == "in") "Selamat bekerja! (Tersimpan offline)" else "Hati-hati di jalan! (Tersimpan offline)", ClockPhase.RESULT)
            scheduleReset(2500)
            return
        }

        if (res.ok) {
            // Status was returned in reason when OK in UseCase
            val status = res.reason
            setResult(true, if (s.action == "in") "Selamat bekerja! ($status)" else "Hati-hati di jalan! ($status)", ClockPhase.RESULT)
            scheduleReset(2500)
        } else {
            setResult(false, gagalText(res.reason), ClockPhase.RESULT)
            scheduleReset(1000)
        }
    }

    /** Cermin gagalText() web (useClockKiosk.ts) — peta alasan gagal server. */
    private fun gagalText(reason: String?): String = when (reason) {
        "not_enrolled" -> "Belum enroll wajah"
        "forbidden_role" -> "Akun tak berwenang absen"
        "cross_outlet" -> "Staff beda outlet"
        "unauthenticated" -> "API key salah"
        "terlambat_alpha" -> "Lewat Batas Waktu (Alpha)"
        "too_early_in" -> "Belum waktunya absen masuk"
        "too_early_out" -> "Belum waktunya absen pulang"
        "gps_accuracy_low" -> "Akurasi GPS terlalu rendah — aktifkan Lokasi Akurat"
        "shift_not_closed" -> "Shift kasir belum ditutup"
        "unfinished_orders" -> "Masih ada pesanan yang belum selesai di outlet ini"
        "fake_gps_detected" -> "Lokasi tidak dapat diverifikasi. Matikan Mock Location."
        "teleportation_detected" -> "Perpindahan lokasi instan tidak wajar terdeteksi."
        null -> "Gagal: tidak diketahui"
        else -> "Gagal: $reason"
    }

    private suspend fun flushQueue() {
        val pending = db.pendingAttendanceDao().getAll()
        for (item in pending) {
            try {
                val res = AbsensiWebApi.submitAttendance(item.toPayload())
                if (res.ok || res.reason == "unauthenticated") {
                    // unauthenticated di sini berarti API key salah secara permanen (bukan
                    // transient) -- tetap dibuang supaya antrean tak macet selamanya di baris
                    // yang tak akan pernah sukses. Kasus lain dibiarkan, coba lagi nanti.
                    db.pendingAttendanceDao().delete(item.id)
                }
            } catch (e: Exception) {
                db.pendingAttendanceDao().markFailedAttempt(item.id, e.message)
            }
        }
    }

    private fun PendingAttendanceEntity.toPayload(): JsonObject = JsonObject().apply {
        addProperty("id", id)
        addProperty("outlet_id", outletId)
        addProperty("outlet_staff_id", outletStaffId)
        addProperty("type", type)
        gpsLat?.let { addProperty("gps_lat", it) } ?: add("gps_lat", com.google.gson.JsonNull.INSTANCE)
        gpsLng?.let { addProperty("gps_lng", it) } ?: add("gps_lng", com.google.gson.JsonNull.INSTANCE)
        gpsAccuracy?.let { addProperty("gps_accuracy", it) } ?: add("gps_accuracy", com.google.gson.JsonNull.INSTANCE)
        addProperty("is_mock", isMock)
        addProperty("match_distance", 0)
        selfiePath?.let { addProperty("selfie_path", it) } ?: add("selfie_path", com.google.gson.JsonNull.INSTANCE)
        addProperty("ts_client", tsClientIso)
        addProperty("from_queue", attemptCount > 0)
        addProperty("is_manual_button", isManualButton)
    }

    private fun setResult(ok: Boolean, message: String, phase: ClockPhase) {
        _state.value = _state.value.copy(result = ClockResult(ok, message), phase = phase)
    }

    /** Camera feedback stays non-blocking and clears itself after [GUIDANCE_READ_MS] — cukup
     *  lama utk terbaca jelas, lalu scanning otomatis lanjut tanpa perlu aksi user. */
    private fun showCameraGuidance(message: String) {
        livenessDetector = null
        lastGuidanceAtMs = System.currentTimeMillis()
        _state.value = _state.value.copy(
            phase = ClockPhase.IDLE,
            result = ClockResult(false, message),
            whoId = null,
            whoName = null,
            challenge = null,
        )
        viewModelScope.launch {
            delay(GUIDANCE_READ_MS)
            val current = _state.value
            if (current.phase == ClockPhase.IDLE && current.result?.message == message) {
                _state.value = current.copy(result = null)
            }
        }
    }

    private fun scheduleReset(delayMs: Long) {
        viewModelScope.launch {
            delay(delayMs)
            livenessDetector = null
            val s = _state.value
            val bypassGeofence = s.outletCoords == null
            // Sudah terkunci di siklus sebelumnya -> langsung IDLE, tanpa layar LOCATING lagi.
            val skipRecheck = s.locationLocked && !bypassGeofence
            _state.value = s.copy(
                phase = if (bypassGeofence || skipRecheck) ClockPhase.IDLE else ClockPhase.LOCATING,
                whoId = null, whoName = null, challenge = null, result = null,
            )
            when {
                skipRecheck -> refreshDeviceLocationSilently()
                !bypassGeofence -> checkLocation()
            }
        }
    }
}

private fun JsonObject.optBooleanOrTrue(key: String): Boolean =
    get(key)?.takeIf { !it.isJsonNull }?.asBoolean ?: true

private fun JsonObject.optDoubleOrNull(key: String): Double? =
    get(key)?.takeIf { !it.isJsonNull }?.asDouble

class ClockViewModelFactory(
    private val application: Application,
    private val outletId: String,
    private val lockToStaffId: String?,
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
        ClockViewModel(
            application, outletId, lockToStaffId,
            faceEmbeddingExtractor = com.sukashawarma.superapp.domain.face.TfliteFaceEmbeddingExtractor(application),
        ) as T
}


