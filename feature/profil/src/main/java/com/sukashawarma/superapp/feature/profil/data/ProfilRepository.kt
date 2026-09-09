package com.sukashawarma.superapp.feature.profil.data

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sukashawarma.superapp.core.storage.StorageUtil
import com.sukashawarma.superapp.core.ui.AvatarStorage
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.SessionTokenHolder
import com.sukashawarma.superapp.data.remote.UpdatePasswordPayload
import com.sukashawarma.superapp.data.remote.authApi
import com.sukashawarma.superapp.domain.session.AppSession
import java.util.UUID

/**
 * Penyuntingan profil sendiri.
 *
 * Nama tampilan, username tampilan, dan foto ditulis lewat RPC `update_my_profile`
 * (SECURITY DEFINER), BUKAN lewat PATCH langsung ke `outlet_staff`. Dua alasan:
 * tidak ada policy RLS yang mengizinkan staff biasa memperbarui barisnya sendiri,
 * dan RLS bekerja per baris — policy apa pun yang membolehkannya akan sekaligus
 * membolehkan staff menulis `role` dan `status` miliknya sendiri. Daftar kolom di
 * dalam badan RPC itulah batasnya.
 *
 * Ganti password TIDAK lewat sana: itu urusan GoTrue, bukan tabel — `PUT /auth/v1/user`
 * dengan access token aktif, sama seperti halaman "Profil & Password" web.
 */
object ProfilRepository {

    /**
     * Mengunggah foto profil dan mengembalikan path objeknya
     * ("avatars/<user_id>/<uuid>.jpg").
     *
     * Folder pertama WAJIB id pengguna: policy `avatars_insert_self` menuntut
     * `(storage.foldername(name))[1] = auth.uid()`, dan `update_my_profile` menolak
     * `avatar_url` yang tidak diawali folder itu.
     *
     * Nama berkas selalu acak, tidak menimpa yang lama. Menimpa path yang sama akan
     * membuat foto lama tetap terlihat sampai cache Coil dan CDN kedaluwarsa — user
     * mengira unggahannya gagal padahal berhasil.
     */
    suspend fun unggahAvatar(jpeg: ByteArray): String {
        val userId = AppSession.staff.value?.id
            ?: throw IllegalStateException("Sesi tidak valid, silakan login ulang.")
        val path = "$userId/${UUID.randomUUID()}.jpg"
        StorageUtil.uploadJpeg(AvatarStorage.BUCKET, path, jpeg)
        return "${AvatarStorage.BUCKET}/$path"
    }

    /**
     * Menyimpan perubahan profil.
     *
     * Konvensi argumen mengikuti RPC-nya: null = "jangan ubah", string kosong =
     * "kosongkan". Tanpa pembedaan itu tidak ada cara menghapus foto profil yang
     * sudah terlanjur diunggah.
     */
    suspend fun simpan(
        namaTampilan: String? = null,
        usernameTampilan: String? = null,
        avatarPath: String? = null,
    ) {
        val body = JsonObject().apply {
            namaTampilan?.let { addProperty("p_display_name", it) }
            usernameTampilan?.let { addProperty("p_display_username", it) }
            avatarPath?.let { addProperty("p_avatar_url", it) }
        }
        if (body.size() == 0) return
        try {
            Postgrest.rpc("update_my_profile", body)
        } catch (e: Postgrest.PostgrestException) {
            throw IllegalStateException(pesanRamah(e), e)
        }
        // Baca ulang dari server, jangan tebak hasilnya dari input: RPC memangkas
        // spasi dan menurunkan huruf username, jadi yang tersimpan bisa berbeda
        // dari yang diketik.
        AppSession.refreshStaff()
    }

    /** Ganti password akun sendiri. GoTrue tidak meminta password lama selama
     *  access token masih berlaku, sama seperti di web. */
    suspend fun gantiPassword(passwordBaru: String) {
        val token = SessionTokenHolder.accessToken
            ?: throw IllegalStateException("Sesi tidak valid, silakan login ulang.")
        val res = authApi.updatePassword("Bearer $token", UpdatePasswordPayload(passwordBaru))
        if (!res.isSuccessful) {
            throw IllegalStateException("Gagal mengganti password (${res.code()}).")
        }
    }

    /** PostgREST membungkus `RAISE EXCEPTION` jadi JSON. Pesan di dalamnya sudah
     *  ditulis untuk dibaca pengguna ("Username x sudah dipakai orang lain."), jadi
     *  yang ditampilkan adalah itu — bukan seluruh badan balasan. */
    private fun pesanRamah(e: Postgrest.PostgrestException): String {
        val pesan = try {
            JsonParser.parseString(e.message.orEmpty()).asJsonObject
                .get("message")?.takeIf { !it.isJsonNull }?.asString
        } catch (_: Exception) {
            null
        }
        return pesan?.takeIf { it.isNotBlank() }
            ?: "Gagal menyimpan profil (${e.code}). Coba lagi."
    }
}
