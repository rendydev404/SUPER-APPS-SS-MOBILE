package com.sukashawarma.superapp.notif

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Postgrest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Mengikat device ini ke akun yang sedang login, supaya server tahu ke mana
 * notifikasi harus dikirim.
 *
 * Pendaftaran lewat RPC `register_fcm_token`, bukan upsert langsung ke tabel:
 * baris yang bentrok bisa milik akun SEBELUMNYA di HP yang sama, dan policy
 * UPDATE `fcm_tokens` sengaja hanya mengizinkan `staff_id = auth.uid()`. RPC-nya
 * SECURITY DEFINER dan mengambil `staff_id` paksa dari `auth.uid()`, jadi
 * pemanggil tidak pernah bisa mengaku sebagai orang lain. Kontraknya sama persis
 * dengan yang dipakai app POS di produksi.
 *
 * Tidak ada pasangan "unregister" saat logout, dan itu disengaja: satu device =
 * satu baris, dan login berikutnya MENGAMBIL ALIH baris itu. Akun terakhir tetap
 * menerima notifikasi sampai ada yang login lagi di HP tersebut.
 */
object FcmTokenRegistrar {

    private const val TAG = "FcmTokenRegistrar"

    /**
     * true bila Firebase punya konfigurasi di APK ini.
     *
     * Tanpa `google-services.json`, plugin google-services tidak dipasang dan
     * `FirebaseMessaging.getInstance()` melempar IllegalStateException. Dicek di
     * sini supaya build tanpa konfigurasi tetap jalan normal — hanya tanpa push.
     */
    fun siap(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    suspend fun daftarkan(context: Context, stafId: String, outletId: String?) {
        if (!siap(context)) {
            Log.i(TAG, "Firebase belum dikonfigurasi di APK ini, pendaftaran token dilewati.")
            return
        }
        val token = try {
            ambilToken() ?: run {
                Log.e(TAG, "Firebase tidak memberi token untuk device ini.")
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengambil token FCM", e)
            return
        }
        // DUA pendaftaran yang BERDIRI SENDIRI. Sempat ditulis bersarang, dan itu
        // salah: kegagalan pada daftar bersama POS (`fcm_tokens`) ikut membatalkan
        // pendaftaran daftar chat, sehingga perangkat diam-diam berhenti menerima
        // pesan chat gara-gara masalah di jalur yang sama sekali lain.
        try {
            Postgrest.rpc(
                "register_fcm_token",
                JsonObject().apply {
                    addProperty("p_token", token)
                    if (outletId != null) addProperty("p_outlet_id", outletId)
                },
            )
            Log.d(TAG, "Token umum terdaftar untuk staf=$stafId outlet=$outletId")
        } catch (e: Exception) {
            // Gagal mendaftar tidak boleh mengganggu apa pun; pengguna tetap bisa
            // bekerja, hanya tidak menerima notifikasi sampai percobaan berikutnya.
            Log.e(TAG, "Pendaftaran token umum ditolak server", e)
        }

        // Daftar khusus chat. `fcm_tokens` dipakai bersama app POS, dan mengirim
        // pesan chat ke seluruh isinya membuat setiap pesan mendarat di HP kasir
        // — POS bahkan membacanya sebagai "pesan dari owner". `chat_push_tokens`
        // hanya diisi aplikasi ini, jadi push chat berhenti di sini.
        try {
            Postgrest.rpc(
                "register_chat_push_token",
                JsonObject().apply { addProperty("p_token", token) },
            )
            Log.d(TAG, "Token chat terdaftar untuk staf=$stafId")
        } catch (e: Exception) {
            Log.e(TAG, "Pendaftaran token chat ditolak server", e)
        }
    }

    private suspend fun ambilToken(): String? = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { tugas ->
            if (tugas.isSuccessful) {
                cont.resume(tugas.result)
            } else {
                Log.e(TAG, "FirebaseMessaging.getToken gagal", tugas.exception)
                cont.resume(null)
            }
        }
    }
}
