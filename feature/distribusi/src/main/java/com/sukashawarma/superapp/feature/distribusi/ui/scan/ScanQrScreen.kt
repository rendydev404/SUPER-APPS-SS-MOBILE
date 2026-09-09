package com.sukashawarma.superapp.feature.distribusi.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.sukashawarma.superapp.presentation.theme.SukaBrown
import com.sukashawarma.superapp.presentation.theme.SukaCream
import com.sukashawarma.superapp.presentation.theme.SukaGray200
import com.sukashawarma.superapp.presentation.theme.SukaGray500
import com.sukashawarma.superapp.presentation.theme.SukaInk
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaOrange
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private val ScanError = Color(0xFFB91C1C)
private val ScanErrorSurface = Color(0xFFFFF1F2)

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
                "Izin kamera ditolak. Gunakan kode verifikasi enam karakter di bawah."
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

    // Pemindai dan executor-nya diangkat ke luar AndroidView supaya tidak
    // dibuat ulang saat Compose melakukan recompose. KartuPemindai memiliki
    // lifecycle effect sendiri untuk melepas CameraX secara asynchronous.
    val pemindai = remember { BarcodeScanning.getClient() }
    val pelaksana = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            pelaksana.shutdown()
            pemindai.close()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(SukaCream),
    ) {
        KepalaScan(onKeluar = onKeluar)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            KartuLangkahScan()

            KartuPemindai(
                izinKamera = izinKamera,
                kameraGagal = state.kameraGagal,
                memproses = state.memproses,
                pemilikDaurHidup = pemilikDaurHidup,
                pemindai = pemindai,
                pelaksana = pelaksana,
                onKodeDitemukan = viewModel::pindai,
                onKameraGagal = viewModel::tandaiKameraGagal,
            )

            if (state.memproses) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = SukaBrown.copy(alpha = 0.08f),
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = SukaBrown,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(
                            "Memeriksa surat jalan...",
                            color = SukaBrown,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            KartuKodeManual(
                kode = state.kodeManual,
                memproses = state.memproses,
                hasil = hasil,
                onKodeBerubah = viewModel::ubahKodeManual,
                onKirim = viewModel::kirimKodeManual,
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = SukaGray500,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    "Pastikan kode pada surat jalan terlihat jelas dan belum pernah diverifikasi.",
                    color = SukaGray500,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

@Composable
private fun KepalaScan(onKeluar: () -> Unit) {
    Surface(color = SukaCream) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onKeluar) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "Scan QR",
                    color = SukaOnSurface,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "Verifikasi surat jalan",
                    color = SukaGray500,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Surface(
                shape = CircleShape,
                color = SukaOrange.copy(alpha = 0.16f),
                modifier = Modifier.size(42.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = SukaBrown,
                        modifier = Modifier.size(23.dp),
                    )
                }
            }
            Spacer(Modifier.size(8.dp))
        }
    }
}

@Composable
private fun KartuLangkahScan() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SukaBrown,
        shadowElevation = 3.dp,
    ) {
        Row(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = SukaOrange.copy(alpha = 0.18f),
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "1",
                        color = SukaOrange,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Mulai dari QR",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "Arahkan kamera ke QR pada surat jalan kurir.",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
            }
            Text(
                "1 / 2",
                color = SukaOrange,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
            )
        }
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
private fun KartuPemindai(
    izinKamera: Boolean,
    kameraGagal: String?,
    memproses: Boolean,
    pemilikDaurHidup: androidx.lifecycle.LifecycleOwner,
    pemindai: com.google.mlkit.vision.barcode.BarcodeScanner,
    pelaksana: ExecutorService,
    onKodeDitemukan: (String) -> Unit,
    onKameraGagal: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = SukaInk,
        shadowElevation = 8.dp,
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "AREA PEMINDAIAN",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = CircleShape,
                    color = if (memproses) SukaOrange else Color.White.copy(alpha = 0.12f),
                ) {
                    Text(
                        if (memproses) "MEMERIKSA" else "KAMERA AKTIF",
                        Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        color = if (memproses) Color.White else SukaOrange,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.7.sp,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            if (izinKamera && kameraGagal == null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black),
                ) {
                    PemindaiKamera(
                        lifecycleOwner = pemilikDaurHidup,
                        pemindai = pemindai,
                        pelaksana = pelaksana,
                        onKodeDitemukan = onKodeDitemukan,
                        onKameraGagal = onKameraGagal,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(30.dp),
                    ) {
                        BingkaiQr()
                    }
                    Surface(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.62f),
                    ) {
                        Text(
                            "Posisikan QR di dalam kotak",
                            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            } else {
                Surface(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.08f),
                ) {
                    Column(
                        Modifier.padding(28.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = SukaOrange,
                            modifier = Modifier.size(42.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            kameraGagal
                                ?: "Kamera belum tersedia. Gunakan kode manual di bawah.",
                            color = Color.White,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Kamera dipisahkan dari kartu UI agar binding dan pelepasannya mengikuti
 * lifecycle composable, bukan lifecycle Activity. Future CameraX tidak boleh
 * dipanggil dengan get() saat onDispose karena get() dapat memblokir main
 * thread ketika halaman sedang dianimasikan keluar.
 */
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
private fun PemindaiKamera(
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    pemindai: com.google.mlkit.vision.barcode.BarcodeScanner,
    pelaksana: ExecutorService,
    onKodeDitemukan: (String) -> Unit,
    onKameraGagal: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val konteks = LocalContext.current
    val callbackKode by rememberUpdatedState(onKodeDitemukan)
    val callbackGagal by rememberUpdatedState(onKameraGagal)
    var tampilan by remember { mutableStateOf<PreviewView?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                // COMPATIBLE menghindari jalur TextureView yang lebih mahal
                // pada perangkat low/mid-range saat page transition berjalan.
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }.also { tampilan = it }
        },
    )

    DisposableEffect(lifecycleOwner, tampilan) {
        val previewView = tampilan
            ?: return@DisposableEffect onDispose { }
        val penyediaFuture = ProcessCameraProvider.getInstance(konteks)
        val mainExecutor = ContextCompat.getMainExecutor(konteks)
        val selesai = AtomicBoolean(false)
        val sedangMenganalisis = AtomicBoolean(false)
        val analisis = ImageAnalysis.Builder()
            // QR tidak membutuhkan resolusi sensor penuh. Resolusi kecil
            // menurunkan tekanan CPU dan jumlah frame yang tertahan di queue.
            .setTargetResolution(Size(480, 480))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        analisis.setAnalyzer(pelaksana) { bingkai ->
            if (selesai.get() || !sedangMenganalisis.compareAndSet(false, true)) {
                bingkai.close()
                return@setAnalyzer
            }

            val gambar = bingkai.image
            if (gambar == null) {
                sedangMenganalisis.set(false)
                bingkai.close()
                return@setAnalyzer
            }

            val masukan = try {
                InputImage.fromMediaImage(gambar, bingkai.imageInfo.rotationDegrees)
            } catch (_: Exception) {
                null
            }

            if (masukan == null) {
                sedangMenganalisis.set(false)
                bingkai.close()
                return@setAnalyzer
            }

            pemindai.process(masukan)
                .addOnSuccessListener { kode ->
                    if (!selesai.get()) {
                        kode.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }
                            ?.rawValue
                            ?.let(callbackKode)
                    }
                }
                .addOnCompleteListener {
                    sedangMenganalisis.set(false)
                    bingkai.close()
                }
        }

        penyediaFuture.addListener({
            if (selesai.get()) return@addListener
            try {
                val kamera = penyediaFuture.get()
                val pratinjau = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                kamera.unbindAll()
                kamera.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    pratinjau,
                    analisis,
                )
            } catch (_: Exception) {
                if (!selesai.get()) {
                    callbackGagal(
                        "Kamera tidak bisa dibuka. Gunakan kode verifikasi di bawah."
                    )
                }
            }
        }, mainExecutor)

        onDispose {
            selesai.set(true)
            analisis.clearAnalyzer()

            // Listener ini dijadwalkan tanpa get() sinkron. Jika provider belum
            // siap, unbind akan terjadi segera setelah future selesai, tanpa
            // menahan main thread dan tanpa balapan dengan transisi halaman.
            penyediaFuture.addListener({
                try {
                    if (!penyediaFuture.isCancelled) penyediaFuture.get().unbindAll()
                } catch (_: Exception) {
                    // Camera teardown harus aman meski provider dibatalkan.
                }
            }, mainExecutor)
        }
    }
}

@Composable
private fun BingkaiQr() {
    Canvas(Modifier.fillMaxSize()) {
        val panjang = size.minDimension * 0.18f
        val garis = 4.dp.toPx()
        val warna = SukaOrange

        drawLine(
            warna,
            androidx.compose.ui.geometry.Offset(0f, panjang),
            androidx.compose.ui.geometry.Offset(0f, 0f),
            garis,
            StrokeCap.Round,
        )
        drawLine(
            warna,
            androidx.compose.ui.geometry.Offset(0f, 0f),
            androidx.compose.ui.geometry.Offset(panjang, 0f),
            garis,
            StrokeCap.Round,
        )
        drawLine(
            warna,
            androidx.compose.ui.geometry.Offset(size.width - panjang, 0f),
            androidx.compose.ui.geometry.Offset(size.width, 0f),
            garis,
            StrokeCap.Round,
        )
        drawLine(
            warna,
            androidx.compose.ui.geometry.Offset(size.width, 0f),
            androidx.compose.ui.geometry.Offset(size.width, panjang),
            garis,
            StrokeCap.Round,
        )
        drawLine(
            warna,
            androidx.compose.ui.geometry.Offset(0f, size.height - panjang),
            androidx.compose.ui.geometry.Offset(0f, size.height),
            garis,
            StrokeCap.Round,
        )
        drawLine(
            warna,
            androidx.compose.ui.geometry.Offset(0f, size.height),
            androidx.compose.ui.geometry.Offset(panjang, size.height),
            garis,
            StrokeCap.Round,
        )
        drawLine(
            warna,
            androidx.compose.ui.geometry.Offset(size.width - panjang, size.height),
            androidx.compose.ui.geometry.Offset(size.width, size.height),
            garis,
            StrokeCap.Round,
        )
        drawLine(
            warna,
            androidx.compose.ui.geometry.Offset(size.width, size.height - panjang),
            androidx.compose.ui.geometry.Offset(size.width, size.height),
            garis,
            StrokeCap.Round,
        )
    }
}

@Composable
private fun KartuKodeManual(
    kode: String,
    memproses: Boolean,
    hasil: HasilPindai,
    onKodeBerubah: (String) -> Unit,
    onKirim: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SukaGray200),
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SukaOrange.copy(alpha = 0.14f),
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Keyboard,
                            contentDescription = null,
                            tint = SukaBrown,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(Modifier.size(10.dp))
                Column {
                    Text(
                        "Tidak bisa scan?",
                        color = SukaOnSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        "Masukkan kode verifikasi dari surat jalan.",
                        color = SukaGray500,
                        fontSize = 11.sp,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = kode,
                onValueChange = onKodeBerubah,
                label = { Text("Kode verifikasi") },
                placeholder = { Text("Contoh: A7K9P2") },
                supportingText = { Text("6 karakter · huruf dan angka") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onKirim,
                enabled = !memproses,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (memproses) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.size(8.dp))
                } else {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                }
                Text(if (memproses) "Memeriksa..." else "Lanjutkan verifikasi")
            }
            (hasil as? HasilPindai.Ditolak)?.let {
                Spacer(Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = ScanErrorSurface,
                ) {
                    Text(
                        it.pesan,
                        Modifier.padding(12.dp),
                        color = ScanError,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
