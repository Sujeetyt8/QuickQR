package com.example.quickqr.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickqr.data.model.QrCodeType
import com.example.quickqr.data.model.QrData
import com.example.quickqr.data.repository.QrRepository
import com.example.quickqr.util.QrCodeProcessor
import com.example.quickqr.util.QrCodeResult
import com.google.mlkit.vision.barcode.common.Barcode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val qrRepository: QrRepository,
    private val qrCodeProcessor: QrCodeProcessor
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private var lastProcessedBarcode: String? = null
    private var lastProcessedTime: Long = 0
    private val SCAN_THROTTLE_MS = 2000L // 2 seconds throttle

    fun processBarcode(barcode: Barcode) {
        val currentTime = System.currentTimeMillis()
        val rawValue = barcode.rawValue ?: return

        // Throttle rapid scans
        if (rawValue == lastProcessedBarcode && 
            (currentTime - lastProcessedTime) < SCAN_THROTTLE_MS) {
            return
        }

        lastProcessedBarcode = rawValue
        lastProcessedTime = currentTime

        viewModelScope.launch {
            _uiState.value = ScanUiState.Processing
            
            when (val result = qrCodeProcessor.processBarcode(barcode)) {
                is QrCodeResult.Success -> {
                    val qrData = QrData(
                        content = result.content,
                        type = result.type,
                        timestamp = System.currentTimeMillis()
                    )
                    
                    // Save to database
                    qrRepository.insertQrCode(qrData)
                    
                    _uiState.value = ScanUiState.Success(
                        content = result.content,
                        type = result.type,
                        timestamp = qrData.timestamp
                    )
                }
                is QrCodeResult.Error -> {
                    _uiState.value = ScanUiState.Error(result.message)
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = ScanUiState.Idle
    }
}

sealed class ScanUiState {
    object Idle : ScanUiState()
    object Processing : ScanUiState()
    data class Success(
        val content: String,
        val type: QrCodeType,
        val timestamp: Long
    ) : ScanUiState()
    
    data class Error(val message: String) : ScanUiState()
}
