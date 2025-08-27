package com.example.quickqr.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickqr.data.model.QrData
import com.example.quickqr.data.model.QrCodeType
import com.example.quickqr.data.repository.QrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val qrRepository: QrRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Initial)
    val uiState: StateFlow<HomeUiState> = _uiState

    fun processScannedContent(content: String, type: QrCodeType) {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val qrData = QrData(
                    content = content,
                    type = type
                )
                qrRepository.insertQrCode(qrData)
                _uiState.value = HomeUiState.Success(qrData)
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun resetState() {
        _uiState.value = HomeUiState.Initial
    }
}

sealed class HomeUiState {
    object Initial : HomeUiState()
    object Loading : HomeUiState()
    data class Success(val qrData: QrData) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
