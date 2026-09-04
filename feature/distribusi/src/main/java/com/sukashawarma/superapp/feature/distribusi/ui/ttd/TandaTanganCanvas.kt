package com.sukashawarma.superapp.feature.distribusi.ui.ttd

import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
 */
@Composable
fun TandaTanganCanvas(onSelesai: (String) -> Unit, onBatal: () -> Unit) {
    val jalur = remember { mutableStateListOf<MutableList<Offset>>() }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            Modifier.fillMaxWidth().height(180.dp)
                .clip(RoundedCornerShape(14.dp)).background(Color.White)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { titik -> jalur.add(mutableListOf(titik)) },
                        onDrag = { perubahan, _ ->
                            perubahan.consume()
                            jalur.lastOrNull()?.add(perubahan.position)
                        },
                    )
                }
        ) {
            jalur.forEach { garis ->
                for (i in 1 until garis.size) {
                    drawLine(
                        color = Color.Black,
                        start = garis[i - 1],
                        end = garis[i],
                        strokeWidth = 4f,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { jalur.clear() }, modifier = Modifier.weight(1f)) {
                Text("Hapus")
            }
            OutlinedButton(onClick = onBatal, modifier = Modifier.weight(1f)) { Text("Batal") }
            Button(
                onClick = { onSelesai(bitmapKeDataUrlPng(renderJalur(jalur, 600, 240))) },
                enabled = jalur.any { it.size > 1 },
                modifier = Modifier.weight(1f),
            ) { Text("Simpan") }
        }
    }
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
