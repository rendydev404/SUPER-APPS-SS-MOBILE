package com.sukashawarma.superapp.presentation.absensi.enroll

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

/** Preview kamera depan + kemampuan snap satu foto JPEG (beda dari [[FaceCameraPreview]]
 *  yang cuma streaming analysis) — dipakai khusus alur enrollment yang perlu 1 foto
 *  utuh untuk disimpan, bukan tiap-frame. */
@Composable
fun EnrollCameraPreview(modifier: Modifier = Modifier, onCaptureReady: (ImageCapture) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier.fillMaxSize())

    DisposableEffect(lifecycleOwner, previewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        var disposed = false
        cameraProviderFuture.addListener({
            if (disposed) return@addListener
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val imageCapture = ImageCapture.Builder().build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageCapture)
                onCaptureReady(imageCapture)
            } catch (e: Exception) {
                // ignore
            }
        }, executor)

        onDispose {
            disposed = true
            try {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}

fun ImageCapture.captureJpeg(executor: java.util.concurrent.Executor, onResult: (Result<ByteArray>) -> Unit) {
    takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            image.close()
            onResult(Result.success(bytes))
        }

        override fun onError(exception: ImageCaptureException) {
            onResult(Result.failure(exception))
        }
    })
}
