package com.example.catlovercompose.feature.screens.screening

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catlovercompose.core.model.BoundingBox
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScreeningUiState(
    val boundingBoxes: List<BoundingBox> = emptyList(),
    val inferenceTime: Long = 0L,
    val isGpuEnabled: Boolean = true,
    val zoomLevel: Float = 0f
)

@HiltViewModel
class ScreeningViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ScreeningUiState())
    val uiState = _uiState.asStateFlow()

    fun onDetectionResult(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    boundingBoxes = boundingBoxes,
                    inferenceTime = inferenceTime
                )
            }
        }
    }

    fun onEmptyDetection() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(boundingBoxes = emptyList())
            }
        }
    }

    fun toggleGpu(isEnabled: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isGpuEnabled = isEnabled)
            }
        }
    }

    fun updateZoom(zoom: Float) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(zoomLevel = zoom)
            }
        }
    }
}