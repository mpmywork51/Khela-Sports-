package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation properties
    val scale = remember { Animatable(0.7f) }
    val alpha = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        // Parallel animations for entrance and progress loading line
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 1000,
                easing = { t -> t * (2 - t) }
            )
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    LaunchedEffect(key1 = true) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2000, easing = LinearOutSlowInEasing)
        )
        delay(300)
        onSplashComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF030A0F), // Very dark teal/blue
                        Color(0xFF010406)  // Pitch black depth
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // 1. Subtle glowing concentric background radar lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerPoint = this.size.center
            drawCircle(
                color = Color(0xFF00FF87).copy(alpha = 0.05f),
                radius = 120.dp.toPx(),
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF00FF87).copy(alpha = 0.03f),
                radius = 240.dp.toPx(),
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF00FF87).copy(alpha = 0.015f),
                radius = 360.dp.toPx(),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // 2. Main Centered Logo & Text Brand
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(scale.value)
        ) {
            // Elegant TV App Icon Container matching the video layout
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(
                        color = Color(0x1F00FF87), // Glassy translucent green
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(
                        width = 1.5.dp,
                        color = Color(0xFF00FF87).copy(alpha = 0.6f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // TV Icon antennas & body simulation
                Canvas(modifier = Modifier.size(54.dp)) {
                    val w = size.width
                    val h = size.height

                    // Antennas
                    drawLine(
                        color = Color(0xFF00FF87),
                        start = androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.2f),
                        end = androidx.compose.ui.geometry.Offset(w * 0.1f, 0f),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFF00FF87),
                        start = androidx.compose.ui.geometry.Offset(w * 0.75f, h * 0.2f),
                        end = androidx.compose.ui.geometry.Offset(w * 0.9f, 0f),
                        strokeWidth = 2.dp.toPx()
                    )

                    // TV Screen Outer Border
                    drawRoundRect(
                        color = Color(0xFF00FF87),
                        topLeft = androidx.compose.ui.geometry.Offset(0f, h * 0.2f),
                        size = androidx.compose.ui.geometry.Size(w, h * 0.8f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // TV Screen Inner details (classic antennas and play button symbol)
                    val trianglePath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(w * 0.42f, h * 0.45f)
                        lineTo(w * 0.62f, h * 0.6f)
                        lineTo(w * 0.42f, h * 0.75f)
                        close()
                    }
                    drawPath(
                        path = trianglePath,
                        color = Color(0xFF00FF87)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Brand Title "LiveKhela" with neon glow accent
            Text(
                text = "LiveKhela",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp
            )

            // Bengali version
            Text(
                text = "লাইভখেলা",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00FF87),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Subtitle
            Text(
                text = "লাইভ স্পোর্টস স্ট্রিমিং ও বিনোদন",
                fontSize = 13.sp,
                color = Color.LightGray.copy(alpha = 0.7f),
                modifier = Modifier.alpha(alpha.value)
            )
        }

        // 3. Horizontal loading line at the bottom
        Box(
            modifier = Modifier
                .fillMaxWidth(0.35f)
                .height(2.5.dp)
                .background(Color(0x1F00FF87), RoundedCornerShape(1.25.dp))
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.value)
                    .fillMaxHeight()
                    .background(Color(0xFF00FF87), RoundedCornerShape(1.25.dp))
            )
        }
    }
}
