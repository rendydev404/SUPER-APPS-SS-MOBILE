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

data class EnrollUiState(
    val loadingOutlets: Boolean = false,
    val loadingCrew: Boolean = true,
    val error: String? = null,
    val outlets: List<EnrollOutletOption> = emptyList(),
    val selectedOutletId: String? = null,
    val crew: List<EnrollCrewOption> = emptyList(),
    val selectedStaffId: String? = null,
    val capturing: Boolean = false,
    val captureResult: String? = null,
    val captureOk: Boolean = false,
)

/** Enrollment foto wajah crew (SPV-tier saja, gate [[ENROLL_ALLOWED_ROLES]] di
 *  [[AbsensiHubScreen]]) — tulis ke kolom `*_mobile` di `outlet_staff`
 *  (face_descriptor_mobile/ref_photo_url_mobile/mobile_enrolled_at/mobile_enrolled_by),
 *  Kolom descriptor mobile ditimpa dengan embedding ArcFace 512d saat enrollment;
 *  descriptor 192d sebelumnya tidak pernah dipertahankan atau dicampurkan.
 *  Foto tetap tersimpan walau descriptor gagal diekstrak (mis. wajah tak terdeteksi jelas
 *  di foto) — SPV bisa ulangi capture tanpa mengulang seluruh alur pilih-staff. */
class EnrollViewModel(
    application: Application,
    private val faceEmbeddingExtractor: FaceEmbeddingExtractor = UnavailableFaceEmbeddingExtractor(),
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(EnrollUiState())
    val state: StateFlow<EnrollUiState> = _state

    init {
        val staff = AppSession.staff.value
        if (staff?.role == Role.REGIONAL_MANAGER) loadOutlets()
        else staff?.outletId?.let(::selectOutlet)
            ?: run { _state.value = EnrollUiState(loadingCrew = false, error = "Akun tidak terhubung ke outlet.") }
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
        _state.value = _state.value.copy(
            selectedOutletId = outletId,
            selectedStaffId = null,
            crew = emptyList(),
            captureResult = null,
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
        _state.value = _state.value.copy(selectedStaffId = staffId, captureResult = null)
    }

    fun closeCamera() = selectStaff(null)

    fun onPhotoCaptured(jpegBytes: ByteArray) {
        val staffId = _state.value.selectedStaffId ?: return
        val enrolledBy = AppSession.staff.value?.id ?: return
        _state.value = _state.value.copy(capturing = true, captureResult = null)
        viewModelScope.launch {
            try {
                val bitmap: Bitmap? = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
                // ArcFace wajib menerima wajah yang disejajarkan oleh lima landmark.
                val alignedFace = bitmap?.let { FaceCropUtil.detectAndAlignForArcFace(it) }
                val descriptor = alignedFace?.let { faceEmbeddingExtractor.extract(it) }

                val path = StorageUtil.uploadJpeg("face-refs", "$staffId/${UUID.randomUUID()}.jpg", jpegBytes)

                Postgrest.update(
                    "outlet_staff",
                    listOf("id" to "eq.$staffId"),
                    JsonObject().apply {
                        addProperty("ref_photo_url_mobile", path)
                        addProperty("mobile_enrolled_at", Instant.now().toString())
                        addProperty("mobile_enrolled_by", enrolledBy)
                        if (descriptor != null) {
                            add("face_descriptor_mobile", JsonArray().apply { descriptor.forEach { add(it) } })
                        }
                    }
                )

                _state.value = _state.value.copy(
                    capturing = false,
                    captureOk = true,
                    captureResult = if (descriptor != null) "Foto & wajah berhasil didaftarkan."
                    else "Foto tersimpan, tapi wajah tak terdeteksi jelas di foto ini (coba pencahayaan lebih terang / wajah lebih dekat & frontal) — ulangi capture, atau pakai tombol absen manual sementara.",
                )
                _state.value.selectedOutletId?.let(::loadCrew)
            } catch (e: Exception) {
                _state.value = _state.value.copy(capturing = false, captureOk = false, captureResult = "Gagal mendaftarkan: ${e.message}")
            }
        }
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
