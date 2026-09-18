package com.sukashawarma.superapp.notif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.sukashawarma.superapp.BuildConfig
import com.sukashawarma.superapp.data.remote.SessionTokenHolder
import com.sukashawarma.superapp.feature.chat.data.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Menerima balasan yang diketik langsung di bilah notifikasi.
 *
 * Balasan dikirim dari proses aplikasi memakai sesi yang sedang hidup. Kalau
 * prosesnya sudah mati dan sesinya ikut hilang, balasan TIDAK dipaksakan lewat
 * jalur lain — pengguna diberi tahu untuk membuka aplikasi. Menyimpan token di
 * disk hanya demi membalas dari notifikasi akan menukar keamanan sesi dengan
 * kenyamanan yang kecil.
 */
class BalasChatReceiver : BroadcastReceiver() {

    companion object {
        const val AKSI_TANDAI_DIBACA = "com.sukashawarma.superapp.TANDAI_DIBACA_CHAT"
        const val AKSI_TEST_NOTIF = "com.sukashawarma.superapp.TEST_NOTIF_CHAT"
    }

    private val lingkup = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AKSI_TANDAI_DIBACA) {
            ChatNotifikasi.tutup(context)
            return
        }
        if (intent.action == AKSI_TEST_NOTIF) {
            val pengirim = intent.getStringExtra("sender") ?: "Ahmad (Dapur)"
            val pesan = intent.getStringExtra("pesan") ?: "Halo mas, stok pita shawarma sisa 3 pack ya!"
            ChatNotifikasi.tampilkan(
                context = context,
                pengirim = pengirim,
                isi = pesan,
                namaGrup = "Chat Tim SS",
            )
            return
        }
        if (intent.action != ChatNotifikasi.AKSI_BALAS) return

        if (true || !BuildConfig.DEBUG) {
            android.util.Log.w("BalasChatReceiver", "Fitur balas notifikasi dinonaktifkan saat chat terkunci.")
            return
        }

        val teks = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(ChatNotifikasi.KUNCI_BALASAN)
            ?.toString()
            ?.trim()
            .orEmpty()
        if (teks.isEmpty()) return

        if (SessionTokenHolder.accessToken.isNullOrBlank()) {
            ChatNotifikasi.tampilkanGagal(context, "Sesi sudah tertutup. Buka aplikasi untuk membalas.")
            return
        }

        // goAsync memberi waktu bagi kiriman jaringan; tanpa itu proses receiver
        // boleh dihentikan sistem begitu onReceive selesai.
        val pending = goAsync()
        val app = context.applicationContext
        lingkup.launch {
            try {
                ChatRepository.kirim(teks)
                ChatNotifikasi.tampilkanBalasanTerkirim(app, teks)
            } catch (e: Exception) {
                android.util.Log.e("BalasChatReceiver", "balas dari notifikasi gagal", e)
                ChatNotifikasi.tampilkanGagal(app, e.message ?: "Coba lagi dari dalam aplikasi.")
            } finally {
                pending.finish()
            }
        }
    }
}
