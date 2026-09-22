package com.sukashawarma.superapp.notif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.sukashawarma.superapp.data.remote.SessionTokenHolder
import com.sukashawarma.superapp.feature.chat.data.PrivateChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Aksi notifikasi pesan PRIBADI: balas langsung dari bilah notifikasi dan
 * tandai dibaca.
 *
 * Terpisah dari [BalasChatReceiver] (Chat Tim) karena tujuannya berbeda: setiap
 * balasan harus sampai ke SATU orang yang percakapannya diketuk, bukan ke grup.
 * Id lawan bicara dibawa di extras Intent, yang dibangun per orang oleh
 * [ChatPribadiNotifikasi].
 *
 * Balasan dikirim memakai sesi yang sedang hidup di proses aplikasi. Kalau
 * prosesnya sudah mati dan sesinya ikut hilang, pengguna diberi tahu untuk
 * membuka aplikasi — token tidak disimpan ke disk hanya demi fitur ini.
 */
class BalasChatPribadiReceiver : BroadcastReceiver() {

    private val lingkup = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val partnerId = intent.getStringExtra(ChatPribadiNotifikasi.EXTRA_PARTNER_ID)
            ?.takeIf { it.isNotBlank() } ?: return
        val app = context.applicationContext

        when (intent.action) {
            ChatPribadiNotifikasi.AKSI_TANDAI_DIBACA -> {
                ChatPribadiNotifikasi.tutup(app, partnerId)
                if (SessionTokenHolder.accessToken.isNullOrBlank()) return
                val pending = goAsync()
                lingkup.launch {
                    try {
                        PrivateChatRepository.tandaiDibaca(partnerId)
                    } catch (e: Exception) {
                        android.util.Log.w("BalasChatPribadi", "tandai dibaca dari notifikasi gagal: ${e.message}")
                    } finally {
                        pending.finish()
                    }
                }
            }

            ChatPribadiNotifikasi.AKSI_BALAS -> {
                val teks = RemoteInput.getResultsFromIntent(intent)
                    ?.getCharSequence(ChatPribadiNotifikasi.KUNCI_BALASAN)
                    ?.toString()
                    ?.trim()
                    .orEmpty()
                if (teks.isEmpty()) return

                if (SessionTokenHolder.accessToken.isNullOrBlank()) {
                    ChatPribadiNotifikasi.tampilkanGagal(app, partnerId, "Sesi sudah tertutup. Buka aplikasi untuk membalas.")
                    return
                }

                // goAsync memberi waktu bagi kiriman jaringan; tanpa itu proses
                // receiver boleh dihentikan sistem begitu onReceive selesai.
                val pending = goAsync()
                lingkup.launch {
                    try {
                        PrivateChatRepository.kirimPesan(recipientId = partnerId, body = teks)
                        // Membalas berarti pesan lawan bicara sudah dibaca — centang biru.
                        runCatching { PrivateChatRepository.tandaiDibaca(partnerId) }
                        ChatPribadiNotifikasi.tampilkanBalasanTerkirim(app, partnerId, teks)
                    } catch (e: Exception) {
                        android.util.Log.e("BalasChatPribadi", "balas dari notifikasi gagal", e)
                        ChatPribadiNotifikasi.tampilkanGagal(app, partnerId, e.message ?: "Coba lagi dari dalam aplikasi.")
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}
