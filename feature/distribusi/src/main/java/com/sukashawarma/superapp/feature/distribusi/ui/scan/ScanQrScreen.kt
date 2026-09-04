package com.sukashawarma.superapp.feature.distribusi.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.sukashawarma.superapp.presentation.theme.SukaGray500
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaSurface
import java.util.concurrent.Executors

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun ScanQrScreen(
    onKeluar: () -> Unit,
    onTerbuka: (String) -> Unit,
    viewModel: ScanQrViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val konteks = LocalContext.current
    val pemilikDaurHidup = LocalLifecycleOwner.current

    var izinKamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(konteks, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val pemintaIzin = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { diberi ->
        izinKamera = diberi
        if (!diberi) {
            viewModel.tandaiKameraGagal(
                "Izin kamera ditolak. Ketik kode verifikasi enam karakter di bawah."
            )
        }
    }

    LaunchedEffect(Unit) {
        if (!izinKamera) pemintaIzin.launch(Manifest.permission.CAMERA)
    }

    val hasil = state.hasil
    LaunchedEffect(hasil) {
        if (hasil is HasilPindai.Terbuka) onTerbuka(hasil.suratJalanId)
    }

    Column(Modifier.fillMaxSize().background(SukaSurface)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onKeluar) { Icon(Icons.Default.ArrowBack, "Kembali") }
            Text(
                "Pindai QR Surat Jalan",
                color = SukaOnSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        Text(
            "Arahkan kamera ke kode QR pada lembar surat jalan yang dibawa kurir.",
            Modifier.padding(horizontal = 16.dp),
            color = SukaGray500,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(12.dp))

        if (izinKamera && state.kameraGagal == null) {
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .aspectRatio(1f).clip(RoundedCornerShape(20.dp)).background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        val tampilan = PreviewView(ctx)
                        val penyedia = ProcessCameraProvider.getInstance(ctx)
                        penyedia.addListener({
                            try {
                                val kamera = penyedia.get()
                                val pratinjau = Preview.Builder().build().also {
                                    it.setSurfaceProvider(tampilan.surfaceProvider)
                                }
                                val pemindai = BarcodeScanning.getClient()
                                val analisis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(
                                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                                    )
                                    .build()
                                analisis.setAnalyzer(Executors.newSingleThreadExecutor()) { bingkai ->
                                    val gambar = bingkai.image
                                    if (gambar == null) {
                                        bingkai.close()
                                        return@setAnalyzer
                                    }
                                    val masukan = InputImage.fromMediaImage(
                                        gambar,
                                        bingkai.imageInfo.rotationDegrees,
                                    )
                                    pemindai.process(masukan)
                                        .addOnSuccessListener { kode ->
                                            kode.firstOrNull {
                                                it.format == Barcode.FORMAT_QR_CODE
                                            }?.rawValue?.let { viewModel.pindai(it) }
                                        }
                                        .addOnCompleteListener { bingkai.close() }
                                }
                                kamera.unbindAll()
                                kamera.bindToLifecycle(
                                    pemilikDaurHidup,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    pratinjau,
                                    analisis,
                                )
                            } catch (e: Exception) {
                                viewModel.tandaiKameraGagal(
                                    "Kamera tidak bisa dibuka. Ketik kode verifikasi di bawah."
                                )
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        tampilan
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            Surface(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
            ) {
                Text(
                    state.kameraGagal
                        ?: "Kamera belum tersedia. Gunakan kode verifikasi enam karakter.",
                    Modifier.padding(14.dp),
                    color = SukaOnSurface,
                    fontSize = 12.sp,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.kodeManual,
                onValueChange = viewModel::ubahKodeManual,
                label = { Text("Kode verifikasi (6 karakter)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = viewModel::kirimKodeManual,
                enabled = !state.memproses,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.memproses) "Memeriksa..." else "Buka Verifikasi")
            }
            (hasil as? HasilPindai.Ditolak)?.let {
                Text(it.pesan, color = Color(0xFFB91C1C), fontSize = 12.sp)
            }
        }
    }
}
