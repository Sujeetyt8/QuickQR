package com.example.quickqr.ui.scan

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.quickqr.R
import com.example.quickqr.ui.components.PermissionDialog
import com.example.quickqr.util.PermissionManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScanScreen(
    onNavigateToResult: (String, String) -> Unit,
    viewModel: ScanViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    // Camera permission state
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    // Preview view for camera
    val previewView = remember { PreviewView(context) }
    
    // Camera manager
    val cameraManager = remember {
        CameraManager(
            context = context,
            lifecycleOwner = lifecycleOwner,
            onBarcodeDetected = { barcode ->
                viewModel.processBarcode(barcode)
            }
        )
    }
    
    // Handle UI state
    val uiState = viewModel.uiState.collectAsState()
    
    // Permission launcher for camera
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                cameraManager.startCamera(previewView)
            } else {
                showPermissionDialog = true
            }
        }
    )
    
    // Effect to request camera permission when composable is first launched
    LaunchedEffect(Unit) {
        if (cameraPermissionState.hasPermission) {
            cameraManager.startCamera(previewView)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    // Handle UI state changes
    LaunchedEffect(uiState.value) {
        when (val currentState = uiState.value) {
            is ScanUiState.Success -> {
                onNavigateToResult(currentState.content, currentState.type.name)
                viewModel.resetState()
            }
            else -> { /* Do nothing */ }
        }
    }
    
    // Main content
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        if (cameraPermissionState.hasPermission) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Show message when camera permission is not granted
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Camera permission is required to scan QR codes")
            }
        }
        
        // Flash toggle button
        if (cameraPermissionState.hasPermission) {
            var isFlashOn by remember { mutableStateOf(false) }
            
            IconButton(
                onClick = {
                    isFlashOn = cameraManager.toggleFlash()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = if (isFlashOn) "Turn off flash" else "Turn on flash",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        // Loading indicator when processing
        if (uiState.value is ScanUiState.Processing) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
    
    // Permission dialog
    if (showPermissionDialog) {
        PermissionDialog(
            title = "Camera Permission Required",
            message = "Camera permission is required to scan QR codes. Please grant the permission in app settings.",
            onDismiss = { showPermissionDialog = false },
            onConfirm = {
                showPermissionDialog = false
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
            confirmText = "Open Settings"
        )
    }
}
