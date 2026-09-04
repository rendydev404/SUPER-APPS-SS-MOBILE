package com.sukashawarma.superapp.presentation.absensi.enroll

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.sukashawarma.superapp.core.storage.StorageUtil
import com.sukashawarma.superapp.data.face.FaceCropUtil
import com.sukashawarma.superapp.data.face.FrameFaceResult
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.face.FaceEmbeddingExtractor
import com.sukashawarma.superapp.domain.face.UnavailableFaceEmbeddingExtractor
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.domain.session.AppSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

data class EnrollCrewOption(val id: String, val name: String, val alreadyEnrolled: Boolean)
data class EnrollOutletOption(val id: String, val name: String)

enum class EnrollStage {
    SCANNING,
    CAPTURING,
    VERIFYING,
    REVIEWING,
    SAVING,
    SUCCESS,
}

data class EnrollUiState(
    val loadingOutlets: Boolean = false,
    val loadingCrew: Boolean = true,
    val error: String? = null,
    val outlets: List<EnrollOutletOption> = emptyList(),
    val selectedOutletId: String? = null,
    val crew: List<EnrollCrewOption> = emptyList(),
    /** Baris `outlet_staff` milik user yang sedang login. Dipisah dari [crew] karena
     *  ketersediaannya tidak boleh bergantung pada outlet yang sedang dipilih. */
    val self: EnrollCrewOption? = null,
    val selectedStaffId: String? = null,
    val stage: EnrollStage = EnrollStage.SCANNING,
    val scanHint: EnrollScanHint = EnrollScanHint.FIND_FACE,
    val scanProgress: Float = 0f,
    val captureRequestId: Long? = null,
    val previewBitmap: Bitmap? = null,
    val capturing: Boolean = false,
    val captureResult: String? = null,
    val captureOk: Boolean = false,
)

/** Enrollment foto wajah crew (SPV-tier saja, gate [[ENROLL_ALLOWED_ROLES]] di
 *  [[AbsensiHubScreen]]) — tulis ke kolom `*_mobile` di `outlet_staff`
 *  (face_descriptor_mobile/ref_photo_url_mobile/mobile_enrolled_at/mobile_enrolled_by),
 *  Kolom descriptor mobile ditimpa dengan embedding ArcFace 512d saat enrollment;
 *  descriptor 192d sebelumnya tidak pernah dipertahankan atau dicampurkan.
 *  Foto dan descriptor hanya ditulis SETELAH frame lolos quality gate, descriptor 512d
 *  berhasil dibuat, dan supervisor mengonfirmasi kandidat di layar review. */
class EnrollViewModel(
    application: Application,
    private val faceEmbeddingExtractor: FaceEmbeddingExtractor = UnavailableFaceEmbeddingExtractor(),
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(EnrollUiState())
    val state: StateFlow<EnrollUiState> = _state

    private val faceScanTracker = EnrollFaceScanTracker()
    private var nextCaptureRequestId = 0L
    private var candidateGeneration = 0L
    private var pendingJpeg: ByteArray? = null
    private var pendingDescriptor: FloatArray? = null
    private var pendingStaffId: String? = null

    init {
        loadSelf()
        val staff = AppSession.staff.value
        // outletId disalin ke variabel lokal: StaffProfile ada di modul lain, jadi Kotlin
        // tidak mau smart-cast propertinya jadi non-null di dalam `when`.
        val outletId = staff?.outletId
        when {
            staff?.role == Role.REGIONAL_MANAGER -> loadOutlets()
            outletId != null -> selectOutlet(outletId)
            // Admin/HR/owner/kitchen tidak selalu terikat outlet. Dulu ini error keras yang
            // mengosongkan seluruh layar; sekarang hanya daftar crew yang kosong, enrollment
            // diri sendiri tetap tersedia di atasnya.
            else -> _state.value = _state.value.copy(
                loadingCrew = false,
                error = "Akun Anda tidak terhubung ke outlet, jadi daftar crew tidak dapat dimuat. Enrollment diri sendiri tetap bisa dilakukan.",
            )
        }
    }

    /** Siapa pun yang boleh membuka halaman ini boleh mendaftarkan wajahnya sendiri, tanpa
     *  perlu menemukan namanya di daftar crew outlet mana pun — Regional Manager dan staff
     *  pusat sering tidak muncul di outlet yang sedang mereka pilih. */
    private fun loadSelf() {
        val me = AppSession.staff.value ?: return
        _state.value = _state.value.copy(self = EnrollCrewOption(me.id, me.name, alreadyEnrolled = false))
        viewModelScope.launch {
            try {
                val row = Postgrest.selectOne(
                    "outlet_staff",
                    listOf("id" to "eq.${me.id}", "select" to "id,name,mobile_enrolled_at"),
                ) ?: return@launch
                _state.value = _state.value.copy(
                    self = EnrollCrewOption(
                        id = row.optString("id") ?: me.id,
                        name = row.optString("name") ?: me.name,
                        alreadyEnrolled = row.optString("mobile_enrolled_at") != null,
                    )
                )
            } catch (e: Exception) {
                // Status enroll gagal dibaca bukan alasan menyembunyikan tombolnya: kartu
                // tetap tampil dengan status default "belum enroll".
            }
        }
    }

    /** Daftar outlet tidak dibatasi di client. RLS backend menentukan outlet mana yang
     * benar-benar boleh dilihat Regional Manager. */
    private fun loadOutlets() {
        _state.value = _state.value.copy(loadingOutlets = true, loadingCrew = false, error = null)
        viewModelScope.launch {
            try {
                val rows = Postgrest.select(
                    "outlets",
                    listOf(
                        "is_active" to "eq.true",
                        "select" to "id,name",
                        "order" to "name.asc",
                    )
                )
                val outlets = rows.map { el ->
                    val o = el.asJsonObject
                    EnrollOutletOption(
                        id = o.optString("id") ?: "",
                        name = o.optString("name") ?: "-",
                    )
                }
                _state.value = _state.value.copy(loadingOutlets = false, outlets = outlets)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loadingOutlets = false, error = "Gagal memuat daftar outlet: ${e.message}")
            }
        }
    }

    fun selectOutlet(outletId: String) {
        resetPendingCandidate()
        _state.value = _state.value.copy(
            selectedOutletId = outletId,
            selectedStaffId = null,
            crew = emptyList(),
            captureResult = null,
            stage = EnrollStage.SCANNING,
            scanHint = EnrollScanHint.FIND_FACE,
            scanProgress = 0f,
            previewBitmap = null,
            loadingCrew = true,
            error = null,
        )
        loadCrew(outletId)
    }

    private fun loadCrew(outletId: String) {
        viewModelScope.launch {
            try {
                val rows = Postgrest.select(
                    "outlet_staff",
                    listOf(
                        "outlet_id" to "eq.$outletId",
                        "status" to "eq.active",
                        "select" to "id,name,mobile_enrolled_at",
                        "order" to "name.asc",
                    )
                )
                val crew = rows.map { el ->
                    val o = el.asJsonObject
                    EnrollCrewOption(
                        id = o.optString("id") ?: "",
                        name = o.optString("name") ?: "-",
                        alreadyEnrolled = o.optString("mobile_enrolled_at") != null,
                    )
                }
                _state.value = _state.value.copy(loadingCrew = false, crew = crew)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loadingCrew = false, error = "Gagal memuat daftar crew: ${e.message}")
            }
        }
    }

    fun selectStaff(staffId: String?) {
        resetPendingCandidate()
        _state.value = _state.value.copy(
            selectedStaffId = staffId,
            stage = EnrollStage.SCANNING,
            scanHint = EnrollScanHint.FIND_FACE,
            scanProgress = 0f,
            captureRequestId = null,
            previewBitmap = null,
            capturing = false,
            captureResult = null,
            captureOk = false,
        )
    }

    fun closeCamera() = selectStaff(null)

    /** Dipanggil dari stream ML Kit. Tracker memastikan capture hanya satu kali setelah stabil. */
    fun onScanFrame(frame: FrameFaceResult) {
        val current = _state.value
        if (current.selectedStaffId == null || current.stage != EnrollStage.SCANNING) return

        val feedback = faceScanTracker.update(
            faceCount = frame.signal.faceCount,
            faceBox = frame.faceBox,
            yawDeg = frame.signal.yawDeg,
            nowMs = System.currentTimeMillis(),
        )
        if (feedback.captureReady) {
            _state.value = current.copy(
                stage = EnrollStage.CAPTURING,
                scanHint = feedback.hint,
                scanProgress = 1f,
                captureRequestId = ++nextCaptureRequestId,
                capturing = true,
                captureResult = null,
            )
        } else {
            _state.value = current.copy(
                scanHint = feedback.hint,
                scanProgress = feedback.progress,
                captureResult = current.captureResult?.takeIf { !feedback.placementAccepted },
            )
        }
    }

    fun onCaptureFailed() {
        if (_state.value.stage != EnrollStage.CAPTURING) return
        faceScanTracker.reset()
        _state.value = _state.value.copy(
            stage = EnrollStage.SCANNING,
            scanProgress = 0f,
            captureRequestId = null,
            capturing = false,
            captureOk = false,
            captureResult = "Kamera belum berhasil mengambil gambar. Tahan wajah dan coba lagi.",
        )
    }

    fun onPhotoCaptured(jpegBytes: ByteArray) {
        val staffId = _state.value.selectedStaffId ?: return
        if (_state.value.stage != EnrollStage.CAPTURING) return
        val generation = candidateGeneration
        _state.value = _state.value.copy(
            stage = EnrollStage.VERIFYING,
            captureRequestId = null,
            capturing = true,
            captureResult = null,
        )
        viewModelScope.launch {
            val result = runCatching {
                val bitmap = decodeScaledJpeg(jpegBytes)
                    ?: error("Foto kamera tidak dapat dibaca")
                // ArcFace wajib menerima wajah yang disejajarkan oleh lima landmark.
                val alignedFace = FaceCropUtil.detectAndAlignForArcFace(bitmap)
                    ?: error("Wajah tidak terdeteksi jelas")
                val descriptor = faceEmbeddingExtractor.extract(alignedFace)
                    ?: error("Wajah belum dapat diproses")
                Triple(bitmap, descriptor, jpegBytes)
            }

            if (candidateGeneration != generation || _state.value.selectedStaffId != staffId) {
                return@launch
            }
            result.onSuccess { (bitmap, descriptor, bytes) ->
                pendingJpeg = bytes
                pendingDescriptor = descriptor
                pendingStaffId = staffId
                _state.value = _state.value.copy(
                    stage = EnrollStage.REVIEWING,
                    capturing = false,
                    previewBitmap = bitmap,
                    captureOk = false,
                    captureResult = null,
                )
            }.onFailure {
                faceScanTracker.reset()
                _state.value = _state.value.copy(
                    stage = EnrollStage.SCANNING,
                    scanHint = EnrollScanHint.FIND_FACE,
                    scanProgress = 0f,
                    capturing = false,
                    captureOk = false,
                    captureResult = "Wajah belum cukup jelas. Pastikan cahaya merata, lalu scan ulang.",
                )
            }
        }
    }

    fun retakePhoto() {
        resetPendingCandidate()
        _state.value = _state.value.copy(
            stage = EnrollStage.SCANNING,
            scanHint = EnrollScanHint.FIND_FACE,
            scanProgress = 0f,
            captureRequestId = null,
            previewBitmap = null,
            capturing = false,
            captureResult = null,
            captureOk = false,
        )
    }

    /** Satu-satunya jalur yang melakukan upload dan update `outlet_staff`. */
    fun confirmEnrollment() {
        val current = _state.value
        val staffId = pendingStaffId
        val jpegBytes = pendingJpeg
        val descriptor = pendingDescriptor
        val enrolledBy = AppSession.staff.value?.id
        if (
            current.stage != EnrollStage.REVIEWING ||
            staffId == null || staffId != current.selectedStaffId ||
            jpegBytes == null || descriptor == null || enrolledBy == null
        ) return

        val generation = candidateGeneration
        _state.value = current.copy(
            stage = EnrollStage.SAVING,
            capturing = true,
            captureResult = null,
        )
        viewModelScope.launch {
            try {
                val path = StorageUtil.uploadJpeg(
                    "face-refs",
                    "$staffId/${UUID.randomUUID()}.jpg",
                    jpegBytes,
                )
                val updated = Postgrest.update(
                    "outlet_staff",
                    listOf("id" to "eq.$staffId"),
                    JsonObject().apply {
                        addProperty("ref_photo_url_mobile", path)
                        addProperty("mobile_enrolled_at", Instant.now().toString())
                        addProperty("mobile_enrolled_by", enrolledBy)
                        add("face_descriptor_mobile", JsonArray().apply { descriptor.forEach { add(it) } })
                    },
                )
                if (updated.size() == 0) error("Crew tidak ditemukan")
                if (candidateGeneration != generation) return@launch
                _state.value = _state.value.copy(
                    stage = EnrollStage.SUCCESS,
                    capturing = false,
                    captureOk = true,
                    captureResult = "Wajah berhasil didaftarkan.",
                )
                _state.value.selectedOutletId?.let(::loadCrew)
                loadSelf()
            } catch (_: Exception) {
                if (candidateGeneration != generation) return@launch
                _state.value = _state.value.copy(
                    stage = EnrollStage.REVIEWING,
                    capturing = false,
                    captureOk = false,
                    captureResult = "Belum berhasil menyimpan enrollment. Periksa koneksi lalu coba lagi.",
                )
            }
        }
    }

    fun finishEnrollment() = closeCamera()

    private fun resetPendingCandidate() {
        candidateGeneration += 1
        pendingJpeg = null
        pendingDescriptor = null
        pendingStaffId = null
        faceScanTracker.reset()
    }

    private fun decodeScaledJpeg(bytes: ByteArray, maxSide: Int = 1_600): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > maxSide) {
            sampleSize *= 2
        }
        return BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sampleSize },
        )
    }
}

class EnrollViewModelFactory(private val application: Application) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
        EnrollViewModel(
            application,
            faceEmbeddingExtractor = com.sukashawarma.superapp.domain.face.NcnnArcFaceEmbeddingExtractor(application),
        ) as T
}
