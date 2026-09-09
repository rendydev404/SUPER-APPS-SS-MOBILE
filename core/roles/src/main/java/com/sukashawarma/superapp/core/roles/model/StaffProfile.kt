package com.sukashawarma.superapp.domain.model

data class StaffProfile(
    val id: String,
    val outletId: String?,
    val outletName: String?,
    val name: String,
    val role: Role?,
    val roleRaw: String,
    val status: String,
    val username: String?,
    val refPhotoUrl: String?,
    val allowManualButton: Boolean,
    val faceDescriptor: List<Float>?,
    /** Nama panggilan yang diatur staff sendiri. null = belum diatur; `name` (nama
     *  kepegawaian, milik admin/HR) tetap dipakai. Jangan pakai untuk payroll. */
    val displayName: String? = null,
    /** Username tampilan. BUKAN kredensial login — login tetap lewat [username]. */
    val displayUsername: String? = null,
    /** Path objek di bucket `avatars` ("avatars/<id>/<file>.jpg"), bukan URL penuh.
     *  Berbeda dari [refPhotoUrl] yang dipakai pencocokan wajah saat absen. */
    val avatarUrl: String? = null,
) {
    val isActive: Boolean get() = status == "active"

    /** Yang ditampilkan ke layar. Satu tempat supaya nama panggilan tidak muncul
     *  di sebagian layar saja sementara sisanya masih memakai nama kepegawaian. */
    val namaTampil: String get() = displayName?.takeIf { it.isNotBlank() } ?: name
}
