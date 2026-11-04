package com.example.mipet.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mipet.data.model.Vet
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

@Composable
fun NativeComposeMap(
    userLat: Double,
    userLng: Double,
    vets: List<Vet>,
    selectedVet: Vet?,
    showRoute: Boolean,
    onVetSelected: (Vet) -> Unit,
    modifier: Modifier = Modifier
) {
    var zoomLevel by remember { mutableFloatStateOf(14f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current.density

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4F3F0))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    panOffset += pan
                    zoomLevel = (zoomLevel * zoom).coerceIn(11f, 17f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerScreenX = size.width / 2f + panOffset.x
            val centerScreenY = size.height / 2f + panOffset.y

            fun latLngToScreen(lat: Double, lng: Double): Offset {
                val scale = 256.0 * 2.0.pow(zoomLevel.toDouble()) / 360.0
                val x = (lng - userLng) * scale
                val latRad = Math.toRadians(lat)
                val userLatRad = Math.toRadians(userLat)
                val y = (ln(tan(Math.PI / 4 + userLatRad / 2)) - ln(tan(Math.PI / 4 + latRad / 2))) * scale * (180.0 / Math.PI)
                return Offset((centerScreenX + x).toFloat(), (centerScreenY + y).toFloat())
            }

            val userPos = Offset(centerScreenX, centerScreenY)

            val gridSize = 140f
            val startX = (centerScreenX % gridSize) - gridSize
            val startY = (centerScreenY % gridSize) - gridSize

            drawRoundRect(
                color = Color(0xFFC8E6C9),
                topLeft = Offset(centerScreenX - 320f, centerScreenY - 240f),
                size = Size(180f, 120f),
                cornerRadius = CornerRadius(16f, 16f)
            )
            drawRoundRect(
                color = Color(0xFFE8F5E9),
                topLeft = Offset(centerScreenX + 140f, centerScreenY + 120f),
                size = Size(220f, 160f),
                cornerRadius = CornerRadius(24f, 24f)
            )

            var x = startX
            while (x < size.width + gridSize) {
                drawLine(
                    color = Color(0xFFE6E4E0),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 12f
                )
                drawLine(
                    color = Color.White,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 8f
                )
                x += gridSize
            }

            var y = startY
            while (y < size.height + gridSize) {
                drawLine(
                    color = Color(0xFFE6E4E0),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 12f
                )
                drawLine(
                    color = Color.White,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 8f
                )
                y += gridSize
            }

            drawLine(
                color = Color(0xFFFFE082),
                start = Offset(0f, centerScreenY + 200f),
                end = Offset(size.width, centerScreenY - 200f),
                strokeWidth = 14f
            )

            val metersPerPixel = 156543.03392 * cos(Math.toRadians(userLat)) / 2.0.pow(zoomLevel.toDouble())
            val radiusPixels = (5000.0 / metersPerPixel).toFloat()

            drawCircle(
                color = Color(0x154285F4),
                radius = radiusPixels,
                center = userPos
            )
            drawCircle(
                color = Color(0xFF1A73E8),
                radius = radiusPixels,
                center = userPos,
                style = Stroke(
                    width = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                )
            )

            if (showRoute && selectedVet != null) {
                val vetPos = latLngToScreen(selectedVet.latitude, selectedVet.longitude)
                drawLine(
                    color = Color(0xFFD93025),
                    start = userPos,
                    end = vetPos,
                    strokeWidth = 10f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f)
                )
            }

            vets.forEach { vet ->
                val vetPos = latLngToScreen(vet.latitude, vet.longitude)
                val isSelected = vet.id == selectedVet?.id

                val pinColor = if (isSelected) Color(0xFFD93025) else Color(0xFFEA4335)
                val pinRadius = if (isSelected) 22f else 16f

                drawCircle(
                    color = Color(0x40000000),
                    radius = pinRadius + 4f,
                    center = Offset(vetPos.x, vetPos.y + 4f)
                )
                drawCircle(
                    color = Color.White,
                    radius = pinRadius + 3f,
                    center = vetPos
                )
                drawCircle(
                    color = pinColor,
                    radius = pinRadius,
                    center = vetPos
                )
            }

            drawCircle(
                color = Color(0x334285F4),
                radius = 32f,
                center = userPos
            )
            drawCircle(
                color = Color.White,
                radius = 16f,
                center = userPos
            )
            drawCircle(
                color = Color(0xFF1A73E8),
                radius = 12f,
                center = userPos
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()
            val centerScreenX = widthPx / 2f + panOffset.x
            val centerScreenY = heightPx / 2f + panOffset.y

            fun latLngToScreen(lat: Double, lng: Double): Offset {
                val scale = 256.0 * 2.0.pow(zoomLevel.toDouble()) / 360.0
                val x = (lng - userLng) * scale
                val latRad = Math.toRadians(lat)
                val userLatRad = Math.toRadians(userLat)
                val y = (ln(tan(Math.PI / 4 + userLatRad / 2)) - ln(tan(Math.PI / 4 + latRad / 2))) * scale * (180.0 / Math.PI)
                return Offset((centerScreenX + x).toFloat(), (centerScreenY + y).toFloat())
            }

            vets.forEach { vet ->
                val pos = latLngToScreen(vet.latitude, vet.longitude)
                val isSelected = vet.id == selectedVet?.id

                if (pos.x in 0f..widthPx && pos.y in 0f..heightPx) {
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (pos.x / density - 24).dp,
                                y = (pos.y / density - 24).dp
                            )
                            .size(48.dp)
                            .clickable { onVetSelected(vet) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🐾",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    if (isSelected) {
                        Surface(
                            modifier = Modifier
                                .offset(
                                    x = (pos.x / density - 60).dp,
                                    y = (pos.y / density - 65).dp
                                ),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            shadowElevation = 6.dp
                        ) {
                            Text(
                                text = vet.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = Color(0xFF202124)
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { zoomLevel = (zoomLevel + 1f).coerceAtMost(17f) },
                containerColor = Color.White,
                contentColor = Color(0xFF3C4043),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Acercar")
            }

            SmallFloatingActionButton(
                onClick = { zoomLevel = (zoomLevel - 1f).coerceAtLeast(11f) },
                containerColor = Color.White,
                contentColor = Color(0xFF3C4043),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Alejar")
            }
        }
    }
}
