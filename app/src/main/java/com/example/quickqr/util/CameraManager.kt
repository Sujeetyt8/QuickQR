package com.example.quickqr.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.WindowManager
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.example.quickqr.R
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val onBarcodeDetected: (String) -> Unit
) {
    private val TAG = "CameraManager"
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val isProcessing = AtomicBoolean(false)
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var flashMode = ImageCapture.FLASH_MODE_OFF
    private var imageAnalysis: ImageAnalysis? = null

    private val barcodeScanner: BarcodeScanner by lazy {
        BarcodeScanning.getClient()
    }

    fun startCamera(previewView: androidx.camera.view.PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(previewView)
            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases(previewView: androidx.camera.view.PreviewView) {
        val cameraProvider = cameraProvider ?: return

        // Unbind all use cases before rebinding
        cameraProvider.unbindAll()

        // Camera selector
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        // Preview
        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setTargetRotation(previewView.display.rotation)
            .build()

        // Image analysis for barcode scanning
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(previewView.display.rotation)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    if (!isProcessing.getAndSet(true)) {
                        processImageProxy(imageProxy)
                    } else {
                        imageProxy.close()
                    }
                }
            }

        // Image capture use case for taking photos
        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(flashMode)
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setTargetRotation(previewView.display.rotation)
            .build()

        // Bind use cases to lifecycle
        camera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageAnalysis,
            imageCapture
        )

        // Connect the preview use case to the view
        preview.setSurfaceProvider(previewView.surfaceProvider)
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    barcodes.firstOrNull()?.rawValue?.let { barcode ->
                        onBarcodeDetected(barcode)
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                    isProcessing.set(false)
                }
        } else {
            imageProxy.close()
            isProcessing.set(false)
        }
    }

    fun toggleFlash(): Boolean {
        return if (camera?.cameraInfo?.torchState?.value == TorchState.ON) {
            setFlashMode(ImageCapture.FLASH_MODE_OFF)
            false
        } else {
            setFlashMode(ImageCapture.FLASH_MODE_ON)
            true
        }
    }

    private fun setFlashMode(mode: Int) {
        flashMode = mode
        camera?.cameraControl?.enableTorch(mode == ImageCapture.FLASH_MODE_ON)
    }

    fun switchCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        // Rebind use cases to update camera selection
        cameraProvider?.let { provider ->
            provider.unbindAll()
            bindCameraUseCases(
                (context as? Fragment)?.view?.findViewById(R.id.viewFinder)
                    ?: throw IllegalStateException("View not available")
            )
        }
    }

    fun release() {
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()
    }

    companion object {
        suspend fun checkCameraPermission(context: Context): Boolean = suspendCoroutine { continuation ->
            val permission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            )
            continuation.resume(permission == PackageManager.PERMISSION_GRANTED)
        }

        fun getOptimalPreviewSize(
            context: Context,
            width: Int,
            height: Int
        ): Size {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = windowManager.defaultDisplay
            val screenWidth = display.width
            val screenHeight = display.height

            // Try to find a size that matches the screen aspect ratio
            val targetRatio = screenWidth.toDouble() / screenHeight
            var optimalWidth = width
            var optimalHeight = height

            if (width > height * targetRatio) {
                optimalWidth = (height * targetRatio).toInt()
            } else {
                optimalHeight = (width / targetRatio).toInt()
            }

            return Size(optimalWidth, optimalHeight)
        }
    }
}
