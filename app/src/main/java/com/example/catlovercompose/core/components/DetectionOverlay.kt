package com.example.catlovercompose.core.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.catlovercompose.core.model.BoundingBox
import com.example.catlovercompose.core.model.DetectionConstants

@Composable
fun DetectionOverlay(
    boundingBoxes: List<BoundingBox>,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.fillMaxSize()) {
        boundingBoxes.forEach { box ->
            val left = box.x1 * size.width
            val top = box.y1 * size.height
            val right = box.x2 * size.width
            val bottom = box.y2 * size.height

            val boxColor = DetectionConstants.BOUNDING_BOX_COLORS[
                box.cls % DetectionConstants.BOUNDING_BOX_COLORS.size
            ]

            // Draw bounding box
            drawRect(
                color = boxColor,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = 8f)
            )

            // Draw label background and text
            val labelText = "${box.clsName} ${String.format("%.2f", box.cnf)}"
            val textLayoutResult = textMeasurer.measure(
                text = labelText,
                style = TextStyle(fontSize = 16.sp)
            )

            // Background for text
            drawRect(
                color = Color.Black.copy(alpha = 0.7f),
                topLeft = Offset(left, top),
                size = Size(
                    textLayoutResult.size.width.toFloat() + 16f,
                    textLayoutResult.size.height.toFloat() + 8f
                )
            )

            // Draw text
            drawText(
                textMeasurer = textMeasurer,
                text = labelText,
                topLeft = Offset(left + 8f, top + 4f),
                style = TextStyle(
                    color = Color.White,
                    fontSize = 16.sp
                )
            )
        }
    }
}