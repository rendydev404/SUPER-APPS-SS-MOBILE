package com.sukashawarma.superapp.presentation.components

import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.sukashawarma.superapp.data.face.FaceDetectionAnalyzer
import com.sukashawarma.superapp.data.face.FrameFaceResult

private const val TAG = "FaceCameraPreview"

/** Preview kamera depan + analisis wajah tiap frame. Dipakai kiosk (1:N) & panel
 *  absen pribadi (1:1) — beda hanya di pemanggil yang mengelola throttle & state. */
@Composable
fun FaceCameraPreview(
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    needsCrop: () -> Boolean = { true },
    onFrame: (FrameFaceResult) -> Unit,
    onImageCaptureReady: (ImageCapture) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentNeedsCrop by rememberUpdatedState(needsCrop)
    val currentOnFrame by rememberUpdatedState(onFrame)
    val currentOnImageCaptureReady by rememberUpdatedState(onImageCaptureReady)

    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }.also {
                previewView = it
            }
        },
        modifier = modifier.fillMaxSize()
    )

    DisposableEffect(lifecycleOwner, previewView, isActive) {
        val targetView = previewView
        if (!isActive || targetView == null) {
            return@DisposableEffect onDispose {}
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        var disposed = false

        cameraProviderFuture.addListener({
            if (disposed) return@addListener
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(targetView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(480, 640))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(executor, FaceDetectionAnalyzer(
                            needsCrop = { currentNeedsCrop() },
                            onResult = { frame -> currentOnFrame(frame) }
                        ))
                    }

                val imageCapture = ImageCapture.Builder().build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    analysis,
                    imageCapture,
                )
                currentOnImageCaptureReady(imageCapture)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind camera to lifecycle", e)
            }
        }, executor)

        onDispose {
            disposed = true
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unbind camera", e)
            }
        }
    }
}
