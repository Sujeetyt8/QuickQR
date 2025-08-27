package com.example.quickqr.Fragment

import android.Manifest
import android.animation.AnimatorInflater
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.quickqr.R
import com.example.quickqr.databinding.FragmentHomeBinding
import com.example.quickqr.Fragment.QrCodeType
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class HomeFragment : Fragment(), SquareResultDialogFragment.OnDialogDismissListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Optimized camera and scanning components
    private lateinit var cameraExecutor: ExecutorService
    private var camera: Camera? = null
    private var barcodeScanner: BarcodeScanner? = null

    // Thread-safe scanning state management
    private val isProcessing = AtomicBoolean(false)
    private val isQrCodeHandled = AtomicBoolean(false)
    private val isFlashlightOn = AtomicBoolean(false)

    // Handler for UI updates
    private val mainHandler = Handler(Looper.getMainLooper())

    // Scanning cooldown to prevent rapid duplicate scans
    private var lastScanTime = 0L
    private val scanCooldownMs = 500L // 500ms cooldown

    // --- ACTIVITY LAUNCHERS ---
    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission is required.", Toast.LENGTH_SHORT).show()
            }
        }

    private val requestGalleryPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                pickImageLauncher.launch("image/*")
            } else {
                Toast.makeText(requireContext(), "Storage permission is required.", Toast.LENGTH_SHORT).show()
            }
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { processImageFromGallery(it) }
        }

    // --- LIFECYCLE METHODS ---
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize executor and scanner
        cameraExecutor = Executors.newSingleThreadExecutor()
        initializeBarcodeScanner()

        // Start camera
        checkCameraPermission()

        // Setup UI animations
        setupScannerAnimation()

        // Setup click listeners
        binding.flashlightButton.setOnClickListener { toggleFlashlight() }
        binding.galleryButton.setOnClickListener { checkGalleryPermissionAndPickImage() }
    }

    override fun onResume() {
        super.onResume()
        // Reset scanning state
        isQrCodeHandled.set(false)
        isProcessing.set(false)
    }

    override fun onPause() {
        super.onPause()
        // Pause scanning when fragment is not visible
        isProcessing.set(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Cleanup resources
        barcodeScanner?.close()
        cameraExecutor.shutdown()
        _binding = null
    }

    override fun onDialogDismissed() {
        // Reset scanning state with slight delay to prevent immediate re-scanning
        mainHandler.postDelayed({
            isQrCodeHandled.set(false)
        }, 300)
    }

    // --- INITIALIZATION ---
    private fun initializeBarcodeScanner() {
        // Optimized barcode scanner options for better performance
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E
            )
            .build()

        barcodeScanner = BarcodeScanning.getClient(options)
    }

    private fun setupScannerAnimation() {
        try {
            val animator = AnimatorInflater.loadAnimator(requireContext(), R.animator.scanner_animation)
            animator.setTarget(binding.laserLine)
            animator.start()
        } catch (e: Exception) {
            Log.w("HomeFragment", "Scanner animation failed to load", e)
        }
    }

    // --- PERMISSION & CAMERA LOGIC ---
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun checkGalleryPermissionAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            pickImageLauncher.launch("image/*")
        } else {
            requestGalleryPermissionLauncher.launch(permission)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Optimized preview configuration
                val preview = Preview.Builder()
                    .setTargetResolution(Size(1280, 720)) // Balanced resolution for performance
                    .build()
                    .also {
                        it.setSurfaceProvider(binding.previewView.surfaceProvider)
                    }

                // Optimized image analysis configuration
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetResolution(Size(640, 480)) // Lower resolution for faster processing
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, QRCodeAnalyzer { qrValue ->
                            // Add cooldown and processing checks
                            val currentTime = System.currentTimeMillis()
                            if (!isQrCodeHandled.get() && !isProcessing.get() &&
                                (currentTime - lastScanTime) >= scanCooldownMs) {
                                lastScanTime = currentTime
                                handleQRCode(qrValue)
                            }
                        })
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Bind use cases
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this.viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )

            } catch (exc: Exception) {
                Log.e("HomeFragment", "Camera binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    // --- QR CODE HANDLING ---
    private fun handleQRCode(qrValue: String) {
        // Double-check to prevent race conditions
        if (!isQrCodeHandled.compareAndSet(false, true)) {
            return
        }

        // Vibrate immediately for instant feedback
        vibratePhone()

        mainHandler.post {
            // Check if dialog is already showing
            val existingDialog = childFragmentManager.findFragmentByTag("SquareResultDialogFragment")
            if (existingDialog != null && existingDialog.isAdded) {
                isQrCodeHandled.set(false)
                return@post
            }

            // Handle UPI payments immediately
            if (qrValue.startsWith("upi://")) {
                openUpiPayment(qrValue)
                return@post
            }

            // Determine QR code type and show dialog
            val qrType = when {
                qrValue.startsWith("http://", ignoreCase = true) ||
                        qrValue.startsWith("https://", ignoreCase = true) -> QrCodeType.URL
                qrValue.startsWith("WIFI:", ignoreCase = true) -> QrCodeType.WIFI
                else -> QrCodeType.TEXT
            }

            try {
                val dialog = SquareResultDialogFragment.newInstance(qrValue, qrType)
                dialog.setOnDismissListener(this@HomeFragment)
                dialog.show(childFragmentManager, "SquareResultDialogFragment")
            } catch (e: Exception) {
                Log.e("HomeFragment", "Failed to show dialog", e)
                isQrCodeHandled.set(false)
            }
        }
    }

    private fun processImageFromGallery(uri: Uri) {
        try {
            val image = InputImage.fromFilePath(requireContext(), uri)
            barcodeScanner?.process(image)
                ?.addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        barcodes.firstOrNull()?.rawValue?.let { qrValue ->
                            handleQRCode(qrValue)
                        }
                    } else {
                        Toast.makeText(requireContext(), "No QR code found in the image.", Toast.LENGTH_SHORT).show()
                    }
                }
                ?.addOnFailureListener { exception ->
                    Log.e("HomeFragment", "Gallery QR scanning failed", exception)
                    Toast.makeText(requireContext(), "Failed to read QR code.", Toast.LENGTH_SHORT).show()
                }
        } catch (e: IOException) {
            Log.e("HomeFragment", "Failed to process gallery image", e)
            Toast.makeText(requireContext(), "Failed to process image.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openUpiPayment(upiUriString: String) {
        // Reset flag immediately for UPI payments
        isQrCodeHandled.set(false)

        try {
            val uri = Uri.parse(upiUriString)
            val upiIntent = Intent(Intent.ACTION_VIEW, uri)
            val chooser = Intent.createChooser(upiIntent, "Pay with")

            if (chooser.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(chooser)
            } else {
                Toast.makeText(requireContext(), "No UPI app found.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Failed to open UPI payment", e)
            Toast.makeText(requireContext(), "Failed to open payment app.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun vibratePhone() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)) // Shorter vibration
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        } catch (e: Exception) {
            Log.w("HomeFragment", "Vibration failed", e)
        }
    }

    // --- UI LOGIC ---
    private fun toggleFlashlight() {
        camera?.let { cam ->
            if (cam.cameraInfo.hasFlashUnit()) {
                val newState = !isFlashlightOn.get()
                isFlashlightOn.set(newState)
                cam.cameraControl.enableTorch(newState)

                // Update UI
                val iconResource = if (newState) {
                    R.drawable.ic_flashlight_off
                } else {
                    R.drawable.ic_flashlight_on
                }
                binding.flashlightButton.setImageResource(iconResource)
            } else {
                Toast.makeText(requireContext(), "Flash not available", Toast.LENGTH_SHORT).show()
            }
        }
    }
}