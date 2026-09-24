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
    /** `outlets.slug` outlet efektif (termasuk override BKO hari ini). Dipakai mengenali
     *  Kantor Pusat — `outlets.type = 'office'` juga memuat Gudang Pusat, jadi tak bisa
     *  dipakai. null pada snapshot offline lama yang belum menyimpan kolom ini. */
    val outletSlug: String? = null,
) {
    val isActive: Boolean get() = status == "active"

    /** Yang ditampilkan ke layar. Bila username (displayUsername) diisi,
     *  username itulah yang tampil. Bila kosong, memakai nama kepegawaian resmi. */
    val namaTampil: String
        get() = displayUsername?.takeIf { it.isNotBlank() }
            ?: displayName?.takeIf { it.isNotBlank() }
            ?: name

    val diKantorPusat: Boolean get() = outletSlug == SLUG_KANTOR_PUSAT

    companion object {
        /** Slug Kantor Pusat — identitas yang dijamin migration pembuatnya (ON CONFLICT (slug)). */
        const val SLUG_KANTOR_PUSAT = "kantor-pusat"
    }
}
