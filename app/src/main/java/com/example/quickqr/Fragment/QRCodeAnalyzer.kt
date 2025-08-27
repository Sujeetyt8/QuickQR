package com.example.quickqr.Fragment

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class QRCodeAnalyzer(
    private val onQRCodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    // FIXED: Flag to control scanning and prevent re-scans.
    private var isScanning = true

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    private val scanner = BarcodeScanning.getClient(options)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        // FIXED: Immediately close the frame if scanning is disabled.
        if (!isScanning) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        // FIXED: Stop scanning as soon as a barcode is detected.
                        isScanning = false
                        barcodes.firstOrNull()?.rawValue?.let { onQRCodeScanned(it) }
                    }
                }
                .addOnCompleteListener { imageProxy.close() }
        }
    }

    // ADDED: Public method to re-enable scanning after a result has been handled.
    fun startScanning() {
        isScanning = true
    }
}