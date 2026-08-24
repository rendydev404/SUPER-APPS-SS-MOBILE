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
) {
    val isActive: Boolean get() = status == "active"
}
