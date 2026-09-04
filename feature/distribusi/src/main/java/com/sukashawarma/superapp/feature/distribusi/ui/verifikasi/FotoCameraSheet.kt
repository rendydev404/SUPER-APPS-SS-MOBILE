package com.sukashawarma.superapp.feature.distribusi.ui.verifikasi

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream

/**
 * Pengambilan foto bukti memakai CameraX di dalam aplikasi, bukan intent ke
 * aplikasi kamera bawaan. Alasannya dua: hasil intent `TakePicturePreview`
 * hanya thumbnail beresolusi rendah — tidak layak jadi bukti — dan jalur
 * `TakePicture` beresolusi penuh menuntut `FileProvider` beserta berkas
 * sementara yang harus dibersihkan sendiri.
 */
@Composable
fun FotoCameraSheet(onDiambil: (Bitmap) -> Unit, onBatal: () -> Unit) {
    val konteks = LocalContext.current
    val pemilikDaurHidup = LocalLifecycleOwner.current
    val penangkap = remember { ImageCapture.Builder().build() }
    var mengambil by remember { mutableStateOf(false) }

    // `bindToLifecycle` di bawah terikat ke `pemilikDaurHidup`, yang di
    // aplikasi satu-Activity ini adalah daur hidup Activity — bukan daur
    // hidup layar ini. Artinya CameraX tidak pernah melepas kameranya
    // sendiri saat composable ini dibuang (mis. lembar foto ditutup).
    // Lembar ini dibuka-tutup berulang, sekali per item barang, jadi tanpa
    // pelepasan eksplisit di sini kamera akan tetap terikat dan bocor tiap
    // kali. Jangan hapus blok ini.
    DisposableEffect(Unit) {
        onDispose {
            try {
                ProcessCameraProvider.getInstance(konteks).get().unbindAll()
            } catch (e: Exception) {
                // Melepas kamera saat lembar foto ditutup tidak boleh menjatuhkan aplikasi.
            }
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(16.dp)).background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    val tampilan = PreviewView(ctx)
                    val penyedia = ProcessCameraProvider.getInstance(ctx)
                    penyedia.addListener({
                        val kamera = penyedia.get()
                        val pratinjau = Preview.Builder().build().also {
                            it.setSurfaceProvider(tampilan.surfaceProvider)
                        }
                        kamera.unbindAll()
                        kamera.bindToLifecycle(
                            pemilikDaurHidup,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            pratinjau,
                            penangkap,
                        )
                    }, ContextCompat.getMainExecutor(ctx))
                    tampilan
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBatal, modifier = Modifier.weight(1f)) { Text("Batal") }
            Button(
                onClick = {
                    mengambil = true
                    penangkap.takePicture(
                        ContextCompat.getMainExecutor(konteks),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(gambar: ImageProxy) {
                                val bitmap = gambar.keBitmap()
                                gambar.close()
                                mengambil = false
                                onDiambil(bitmap)
                            }

                            override fun onError(galat: ImageCaptureException) {
                                mengambil = false
                            }
                        },
                    )
                },
                enabled = !mengambil,
                modifier = Modifier.weight(1f),
            ) { Text(if (mengambil) "Mengambil..." else "Ambil Foto") }
        }
    }
}

/** `ImageProxy` datang dalam orientasi sensor. Tanpa rotasi ini, foto bukti
 *  tersimpan miring 90 derajat pada sebagian besar HP. */
private fun ImageProxy.keBitmap(): Bitmap {
    val penyangga = planes[0].buffer
    val bytes = ByteArray(penyangga.remaining())
    penyangga.get(bytes)
    val asal = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    val derajat = imageInfo.rotationDegrees
    if (derajat == 0) return asal
    val matriks = Matrix().apply { postRotate(derajat.toFloat()) }
    return Bitmap.createBitmap(asal, 0, 0, asal.width, asal.height, matriks, true)
}
