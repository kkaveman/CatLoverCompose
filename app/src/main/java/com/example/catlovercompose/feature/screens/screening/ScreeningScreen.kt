package com.example.catlovercompose.feature.screens.screening

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.catlovercompose.core.components.DetectionOverlay
import com.example.catlovercompose.core.ml.Detector
import com.example.catlovercompose.core.model.BoundingBox
import com.example.catlovercompose.core.model.DetectionConstants
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScreeningScreen(
    viewModel: ScreeningViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var detector by remember { mutableStateOf<Detector?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Initialize detector
    LaunchedEffect(Unit) {
        cameraExecutor.execute {
            detector = Detector(
                context = context,
                modelPath = DetectionConstants.MODEL_PATH,
                labelPath = DetectionConstants.LABELS_PATH,
                detectorListener = object : Detector.DetectorListener {
                    override fun onEmptyDetect() {
                        viewModel.onEmptyDetection()
                    }

                    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
                        viewModel.onDetectionResult(boundingBoxes, inferenceTime)
                    }
                }
            )
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            detector?.close()
            cameraExecutor.shutdown()
        }
    }

    // GPU toggle effect
    LaunchedEffect(uiState.isGpuEnabled) {
        detector?.let { d ->
            cameraExecutor.execute {
                d.restart(uiState.isGpuEnabled)
            }
        }
    }

    // Zoom control effect
    LaunchedEffect(uiState.zoomLevel) {
        camera?.cameraControl?.setLinearZoom(uiState.zoomLevel)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (cameraPermissionState.status.isGranted) {
            CameraPreview(
                detector = detector,
                cameraExecutor = cameraExecutor,
                onCameraReady = { cam ->
                    camera = cam
                }
            )

            // Detection overlay
            DetectionOverlay(
                boundingBoxes = uiState.boundingBoxes,
                modifier = Modifier.fillMaxSize()
            )

            // Inference time display
            Text(
                text = "${uiState.inferenceTime}ms",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(32.dp)
            )

            // Bottom controls
            BottomControls(
                isGpuEnabled = uiState.isGpuEnabled,
                onGpuToggle = { viewModel.toggleGpu(it) },
                zoomLevel = uiState.zoomLevel,
                onZoomChange = { viewModel.updateZoom(it) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        } else {
            // Request camera permission
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Camera permission is required for object detection",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("Grant Camera Permission")
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    detector: Detector?,
    cameraExecutor: java.util.concurrent.ExecutorService,
    onCameraReady: (Camera) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            val bitmapBuffer = Bitmap.createBitmap(
                                imageProxy.width,
                                imageProxy.height,
                                Bitmap.Config.ARGB_8888
                            )
                            imageProxy.use {
                                bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer)
                            }

                            val matrix = Matrix().apply {
                                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                            }

                            val rotatedBitmap = Bitmap.createBitmap(
                                bitmapBuffer,
                                0,
                                0,
                                bitmapBuffer.width,
                                bitmapBuffer.height,
                                matrix,
                                true
                            )

                            detector?.detect(rotatedBitmap)
                        }
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )

                    // Notify that camera is ready
                    onCameraReady(camera)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun BottomControls(
    isGpuEnabled: Boolean,
    onGpuToggle: (Boolean) -> Unit,
    zoomLevel: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // GPU Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("CPU", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = isGpuEnabled,
                onCheckedChange = onGpuToggle
            )
            Text("GPU", style = MaterialTheme.typography.bodyMedium)
        }

        // Zoom Slider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Zoom", modifier = Modifier.width(50.dp))
            Slider(
                value = zoomLevel,
                onValueChange = onZoomChange,
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(zoomLevel * 100).toInt()}%",
                modifier = Modifier.width(50.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}