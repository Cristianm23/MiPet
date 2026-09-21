package com.example.mipet.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mipet.ui.theme.MiPetTheme
import com.example.mipet.ui.theme.PetroleumBlue
import com.example.mipet.ui.theme.Turquoise

@Composable
fun MiPetLogo(modifier: Modifier = Modifier, color: Color = PetroleumBlue) {
    Canvas(modifier = modifier.size(100.dp)) {
        val w = size.width
        val h = size.height

        // Central Pad (The "palm") - Shaped like a rounded heart-bottom
        val mainPadPath = Path().apply {
            moveTo(w * 0.5f, h * 0.95f)
            cubicTo(w * 0.1f, h * 0.85f, w * 0.05f, h * 0.45f, w * 0.5f, h * 0.4f)
            cubicTo(w * 0.95f, h * 0.45f, w * 0.9f, h * 0.85f, w * 0.5f, h * 0.95f)
        }
        drawPath(mainPadPath, color)

        // The Medical Cross inside the main pad
        val crossWidth = w * 0.25f
        val crossThickness = w * 0.08f
        // Horizontal
        drawRect(
            color = Color.White,
            topLeft = Offset(w * 0.5f - crossWidth / 2, h * 0.65f - crossThickness / 2),
            size = Size(crossWidth, crossThickness)
        )
        // Vertical
        drawRect(
            color = Color.White,
            topLeft = Offset(w * 0.5f - crossThickness / 2, h * 0.65f - crossWidth / 2),
            size = Size(crossThickness, crossWidth)
        )

        // The four toe pads - Arranged to complete a heart shape overall
        val toeColor = Turquoise
        
        // Far left toe
        drawOval(
            toeColor,
            topLeft = Offset(w * 0.05f, h * 0.25f),
            size = Size(w * 0.2f, h * 0.25f)
        )
        
        // Inner left toe
        drawOval(
            toeColor,
            topLeft = Offset(w * 0.28f, h * 0.1f),
            size = Size(w * 0.2f, h * 0.25f)
        )
        
        // Inner right toe
        drawOval(
            toeColor,
            topLeft = Offset(w * 0.52f, h * 0.1f),
            size = Size(w * 0.2f, h * 0.25f)
        )
        
        // Far right toe
        drawOval(
            toeColor,
            topLeft = Offset(w * 0.75f, h * 0.25f),
            size = Size(w * 0.2f, h * 0.25f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MiPetLogoPreview() {
    MiPetTheme {
        MiPetLogo(modifier = Modifier.padding(16.dp))
    }
}
