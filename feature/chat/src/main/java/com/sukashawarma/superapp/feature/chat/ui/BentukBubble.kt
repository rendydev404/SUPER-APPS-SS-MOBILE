package com.sukashawarma.superapp.feature.chat.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.sukashawarma.superapp.feature.chat.domain.PosisiGrup

/**
 * Bentuk gelembung pesan beserta EKOR yang melengkung ke arah foto profil.
 *
 * Dua keputusan yang menentukan rapinya hasil akhir:
 *
 * 1. **Lebar ekor selalu disisihkan, meski ekornya tidak digambar.** Badan
 *    gelembung selalu mulai [EKOR] dari tepi sisi pengirim. Kalau ruang itu
 *    hanya disisihkan pada gelembung berekor, seluruh gelembung dalam satu
 *    rentetan akan bergeser beberapa piksel terhadap satu sama lain — persis
 *    jenis ketidakrapian yang langsung terlihat mata.
 *
 * 2. **Ekor hanya pada gelembung TERAKHIR satu rentetan** ([PosisiGrup.TUNGGAL]
 *    dan [PosisiGrup.AKHIR]), sejajar dengan foto profil yang juga hanya
 *    muncul sekali per rentetan. Memberi ekor pada setiap gelembung membuat
 *    tepiannya terlihat bergerigi.
 */
val EKOR = 7.dp

private val RADIUS_BESAR = 18.dp
private val RADIUS_KECIL = 6.dp

class BentukGelembung(
    private val milikSendiri: Boolean,
    private val posisi: PosisiGrup,
) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val t = with(density) { EKOR.toPx() }
        val rBesar = with(density) { RADIUS_BESAR.toPx() }
        val rKecil = with(density) { RADIUS_KECIL.toPx() }

        val berekor = posisi == PosisiGrup.TUNGGAL || posisi == PosisiGrup.AKHIR
        // Sudut pada sisi yang bersambung dengan gelembung tetangga dibuat tumpul
        // supaya rentetannya terbaca sebagai satu kesatuan, meniru iMessage.
        val atasNyambung = posisi == PosisiGrup.TENGAH || posisi == PosisiGrup.AKHIR
        val bawahNyambung = posisi == PosisiGrup.AWAL || posisi == PosisiGrup.TENGAH

        val path = Path()
        val w = size.width
        val h = size.height

        if (milikSendiri) {
            // Badan berhenti `t` sebelum tepi kanan; ekor menempati sisa itu.
            val kanan = w - t
            val rAtasKanan = if (atasNyambung) rKecil else rBesar
            val rBawahKanan = if (bawahNyambung) rKecil else rBesar

            path.moveTo(rBesar, 0f)
            path.lineTo(kanan - rAtasKanan, 0f)
            path.quadraticBezierTo(kanan, 0f, kanan, rAtasKanan)

            if (berekor) {
                // Sisi kanan turun sampai pangkal ekor, lalu melengkung keluar
                // ke ujung dan kembali masuk ke tepi bawah — lengkung tetes air.
                path.lineTo(kanan, h - t * 1.7f)
                path.quadraticBezierTo(kanan + t * 0.28f, h - t * 0.55f, w, h)
                path.quadraticBezierTo(kanan + t * 0.1f, h, kanan - rBesar * 0.5f, h)
            } else {
                path.lineTo(kanan, h - rBawahKanan)
                path.quadraticBezierTo(kanan, h, kanan - rBawahKanan, h)
            }

            path.lineTo(rBesar, h)
            path.quadraticBezierTo(0f, h, 0f, h - rBesar)
            path.lineTo(0f, rBesar)
            path.quadraticBezierTo(0f, 0f, rBesar, 0f)
        } else {
            // Badan mulai `t` dari tepi kiri; ekor menempati ruang itu.
            val kiri = t
            val rAtasKiri = if (atasNyambung) rKecil else rBesar
            val rBawahKiri = if (bawahNyambung) rKecil else rBesar

            path.moveTo(kiri + rAtasKiri, 0f)
            path.lineTo(w - rBesar, 0f)
            path.quadraticBezierTo(w, 0f, w, rBesar)
            path.lineTo(w, h - rBesar)
            path.quadraticBezierTo(w, h, w - rBesar, h)
            path.lineTo(kiri + rBesar * 0.5f, h)

            if (berekor) {
                path.quadraticBezierTo(kiri - t * 0.1f, h, 0f, h)
                path.quadraticBezierTo(kiri - t * 0.28f, h - t * 0.55f, kiri, h - t * 1.7f)
            } else {
                path.quadraticBezierTo(kiri, h, kiri, h - rBawahKiri)
            }

            path.lineTo(kiri, rAtasKiri)
            path.quadraticBezierTo(kiri, 0f, kiri + rAtasKiri, 0f)
        }

        path.close()
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean =
        other is BentukGelembung && other.milikSendiri == milikSendiri && other.posisi == posisi

    override fun hashCode(): Int = posisi.hashCode() * 31 + milikSendiri.hashCode()
}
