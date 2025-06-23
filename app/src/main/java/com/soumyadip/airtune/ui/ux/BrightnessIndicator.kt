
package com.soumyadip.airtune.ui.ux

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soumyadip.airtune.R

@Composable
fun BrightnessIndicator(
    brightnessLevel: Int,
    modifier: Modifier = Modifier
) {
    val barHeight = 8.dp
    val barCornerRadius = 4.dp
    val iconSize = 24.dp
    val iconPadding = 4.dp

    val currentBrightnessPercentage = (brightnessLevel / 255f)

    val boxWidthPx = remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .width(200.dp)
            .background(Color(0xB3000000), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Brightness: ${(currentBrightnessPercentage * 100).toInt()}%",
            color = Color.White,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val density = LocalDensity.current

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight + iconSize + iconPadding * 2)
                .onSizeChanged { size ->
                    boxWidthPx.value = size.width
                }
            // You can optionally add contentAlignment if you want to center everything inside this Box
            // contentAlignment = Alignment.CenterVertically
        ) {
            // Background (Empty) Bar - Drawn using Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight)
                // REMOVED .align(Alignment.CenterVertically) here
                // Instead, we will explicitly draw the bar in the center of the Canvas
            ) {
                val canvasHeight = size.height
                val barTop = (canvasHeight - barHeight.toPx()) / 2f
                val barBottom = barTop + barHeight.toPx()

                // Draw the full grey bar
                drawRoundRect(
                    color = Color.Gray,
                    cornerRadius = CornerRadius(barCornerRadius.toPx()),
                    topLeft = Offset(0f, barTop), // Start at the calculated top
                    size = Size(size.width, barHeight.toPx())
                )

                // Draw the white filled portion
                if (currentBrightnessPercentage > 0) {
                    val filledWidth = size.width * currentBrightnessPercentage
                    drawRoundRect(
                        color = Color.White,
                        cornerRadius = CornerRadius(barCornerRadius.toPx()),
                        topLeft = Offset(0f, barTop), // Start at the calculated top
                        size = Size(filledWidth, barHeight.toPx())
                    )
                }
            }

            // Sun Icon - Positioned using Modifier.offset
            val sunIconPainter = painterResource(id = R.drawable.peace)

            val iconXOffset = with(density) {
                val currentBoxWidthPx = boxWidthPx.value.toFloat()

                if (currentBoxWidthPx == 0f) {
                    return@with 0.dp
                }

                val totalTravelRangePx = currentBoxWidthPx - iconSize.toPx()
                val offsetPx = totalTravelRangePx * currentBrightnessPercentage
                offsetPx.toDp()
            }

            Image(
                painter = sunIconPainter,
                contentDescription = "Brightness Indicator",
                modifier = Modifier
                    .size(iconSize)
                    .align(Alignment.CenterStart) // This will center the icon vertically within the Box
                    .offset(x = iconXOffset, y = 0.dp)
            )
        }
    }
}