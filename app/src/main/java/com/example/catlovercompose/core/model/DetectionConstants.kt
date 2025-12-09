package com.example.catlovercompose.core.model

import androidx.compose.ui.graphics.Color

object DetectionConstants {
    const val MODEL_PATH = "model.tflite"
    const val LABELS_PATH = "labels.txt"

    val BOUNDING_BOX_COLORS = listOf(
        Color.Red,
        Color.Green,
        Color.Blue,
        Color.Cyan,
        Color.Magenta,
        Color.Yellow,
        Color.White,
        Color.Black
    )
}