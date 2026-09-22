package com.sukashawarma.superapp.feature.distribusi.ui.ttd

import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream

/** Batas panjang data URL tanda tangan — cermin `MAX_SIGNATURE_SIZE` di web. */
const val BATAS_TANDA_TANGAN = 50_000

fun tandaTanganTerlaluBesar(dataUrl: String): Boolean = dataUrl.length > BATAS_TANDA_TANGAN

/** PNG -> data URL, format yang sama dengan `canvas.toDataURL()` di browser,
 *  supaya gambar dari HP bisa ditampilkan web tanpa penanganan khusus. */
fun bitmapKeDataUrlPng(bitmap: Bitmap): String {
    val keluaran = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, keluaran)
    val base64 = Base64.encodeToString(keluaran.toByteArray(), Base64.NO_WRAP)
    return "data:image/png;base64,$base64"
}

/**
 * Papan goresan tanda tangan. Jalur direkam sebagai daftar titik, lalu
 * dirender ulang ke `Bitmap` saat disimpan — merender dari data yang sama
 * dengan yang dilihat pengguna, bukan menangkap ulang layar.
 *
 * Performa: tiap goresan disimpan sebagai satu `Path` (satu draw call per
 * goresan, bukan satu `drawLine` per titik). Gerakan jari hanya menaikkan
 * [versi], yang dibaca di dalam fase draw — jadi menggores tidak pernah
 * memicu recomposition, cukup redraw kanvas ini saja.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TandaTanganCanvas(onSelesai: (String) -> Unit, onBatal: () -> Unit) {
    // Sumber kebenaran untuk ekspor PNG; sengaja bukan state.
    val jalur = remember { ArrayList<ArrayList<Offset>>() }
    val garis = remember { ArrayList<Path>() }
    val versi = remember { mutableIntStateOf(0) }
    var adaGoresan by remember { mutableStateOf(false) }
    val kuas = remember { Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            Modifier.fillMaxWidth().height(180.dp)
                .clip(RoundedCornerShape(14.dp)).background(Color.White)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        // Tanpa touch slop: goresan mulai tepat di titik sentuh,
                        // dan konsumsi langsung mencegah LazyColumn induk ikut menggulir.
                        val turun = awaitFirstDown()
                        turun.consume()
                        val titik = arrayListOf(turun.position)
                        val path = Path().apply { moveTo(turun.position.x, turun.position.y) }
                        jalur.add(titik)
                        garis.add(path)
                        versi.intValue++

                        while (true) {
                            val event = awaitPointerEvent()
                            val perubahan = event.changes.firstOrNull { it.id == turun.id } ?: break
                            if (!perubahan.pressed) break
                            // Titik historis = sampel sentuhan di antara dua frame;
                            // tanpa ini garis cepat terlihat patah-patah.
                            perubahan.historical.forEach { tambahTitik(titik, path, it.position) }
                            tambahTitik(titik, path, perubahan.position)
                            perubahan.consume()
                            versi.intValue++
                        }
                        if (!adaGoresan && titik.size > 1) adaGoresan = true
                    }
                }
        ) {
            versi.intValue // baca di fase draw: invalidasi hanya menggambar ulang
            garis.forEach { drawPath(it, Color.Black, style = kuas) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    jalur.clear()
                    garis.clear()
                    adaGoresan = false
                    versi.intValue++
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("Hapus")
            }
            OutlinedButton(onClick = onBatal, modifier = Modifier.weight(1f)) { Text("Batal") }
            Button(
                onClick = { onSelesai(bitmapKeDataUrlPng(renderJalur(jalur, 600, 240))) },
                enabled = adaGoresan,
                modifier = Modifier.weight(1f),
            ) { Text("Simpan") }
        }
    }
}

/** Kurva kuadratik lewat titik tengah: garis halus tanpa sudut patah,
 *  dengan biaya yang sama dengan `lineTo`. */
private fun tambahTitik(titik: ArrayList<Offset>, path: Path, baru: Offset) {
    val lama = titik.last()
    if (lama == baru) return
    path.quadraticBezierTo(lama.x, lama.y, (lama.x + baru.x) / 2f, (lama.y + baru.y) / 2f)
    titik.add(baru)
}

/** Menggambar ulang jalur ke bitmap berlatar putih pada ukuran tetap, supaya
 *  besar berkasnya dapat diperkirakan dan tidak bergantung ukuran layar. */
private fun renderJalur(jalur: List<List<Offset>>, lebar: Int, tinggi: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(lebar, tinggi, Bitmap.Config.ARGB_8888)
    val kanvas = android.graphics.Canvas(bitmap)
    kanvas.drawColor(android.graphics.Color.WHITE)

    val semuaTitik = jalur.flatten()
    if (semuaTitik.isEmpty()) return bitmap
    val maksX = semuaTitik.maxOf { it.x }.coerceAtLeast(1f)
    val maksY = semuaTitik.maxOf { it.y }.coerceAtLeast(1f)
    val skala = minOf(lebar / maksX, tinggi / maksY)

    val kuas = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 4f
        style = android.graphics.Paint.Style.STROKE
        strokeCap = android.graphics.Paint.Cap.ROUND
        isAntiAlias = true
    }
    jalur.forEach { garis ->
        for (i in 1 until garis.size) {
            kanvas.drawLine(
                garis[i - 1].x * skala, garis[i - 1].y * skala,
                garis[i].x * skala, garis[i].y * skala,
                kuas,
            )
        }
    }
    return bitmap
}
