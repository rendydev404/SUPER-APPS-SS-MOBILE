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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.sukashawarma.superapp.core.ui.ios.BilahJudulIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.warnaKolomIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.presentation.theme.SukaOrange
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

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
            .background(WarnaIos.Latar),
    ) {
        KepalaScan(onKeluar = onKeluar)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = UkuranIos.TepiLayar, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
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
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(UkuranIos.SudutBlok)
                        .background(WarnaIos.Aksen.copy(alpha = 0.10f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = WarnaIos.Aksen,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(
                        "Memeriksa surat jalan...",
                        color = NadaIos.AKSEN.teks,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
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
                    .padding(start = 4.dp, end = 4.dp, bottom = 20.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    IkonIos.ErrorOutline,
                    contentDescription = null,
                    tint = WarnaIos.Abu,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    "Pastikan kode pada surat jalan terlihat jelas dan belum pernah diverifikasi.",
                    style = TipeIos.Catatan,
                )
            }
        }
    }
}

@Composable
private fun KepalaScan(onKeluar: () -> Unit) {
    BilahJudulIos(
        judul = "Scan QR",
        subjudul = "Verifikasi surat jalan",
        onKembali = onKeluar,
    ) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(WarnaIos.Aksen.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                IkonIos.QrCodeScanner,
                contentDescription = null,
                tint = WarnaIos.Aksen,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun KartuLangkahScan() {
    KartuIos {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(WarnaIos.Aksen),
                contentAlignment = Alignment.Center,
            ) {
                Text("1", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Mulai dari QR", style = TipeIos.Utama)
                Text("Arahkan kamera ke QR pada surat jalan kurir.", style = TipeIos.Catatan)
            }
            Spacer(Modifier.width(8.dp))
            LencanaIos("1 / 2", NadaIos.AKSEN, titik = false)
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
    KartuIos(padding = PaddingValues(12.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 0.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Area pemindaian", Modifier.weight(1f), style = TipeIos.Utama)
            if (memproses) {
                LencanaIos("Memeriksa", NadaIos.AKSEN)
            } else {
                LencanaIos("Kamera aktif", NadaIos.SUKSES)
            }
        }
        Spacer(Modifier.height(10.dp))

        if (izinKamera && kameraGagal == null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(UkuranIos.SudutBlok)
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
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(UkuranIos.SudutBlok)
                    .background(WarnaIos.Latar)
                    .padding(28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(WarnaIos.Aksen.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        IkonIos.PhotoCamera,
                        contentDescription = null,
                        tint = WarnaIos.Aksen,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    kameraGagal
                        ?: "Kamera belum tersedia. Gunakan kode manual di bawah.",
                    style = TipeIos.SubJudul,
                    textAlign = TextAlign.Center,
                )
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
    KartuIos {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(WarnaIos.Aksen.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Keyboard,
                    contentDescription = null,
                    tint = WarnaIos.Aksen,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.size(12.dp))
            Column {
                Text("Tidak bisa scan?", style = TipeIos.Utama)
                Text("Masukkan kode verifikasi dari surat jalan.", style = TipeIos.Catatan)
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
            shape = UkuranIos.SudutKontrol,
            colors = warnaKolomIos(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        // Teks "Memeriksa..." dipertahankan (bukan hanya pemutar) karena itulah
        // satu-satunya tanda di kartu ini bahwa kode sedang diperiksa.
        TombolUtamaIos(
            teks = if (memproses) "Memeriksa..." else "Lanjutkan verifikasi",
            onKlik = onKirim,
            aktif = !memproses,
            ikon = if (memproses) null else IkonIos.CheckCircle,
        )
        (hasil as? HasilPindai.Ditolak)?.let {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(UkuranIos.SudutKontrol)
                    .background(WarnaIos.Merah.copy(alpha = 0.10f))
                    .padding(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    IkonIos.ErrorOutline,
                    contentDescription = null,
                    tint = NadaIos.BAHAYA.teks,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    it.pesan,
                    color = NadaIos.BAHAYA.teks,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
