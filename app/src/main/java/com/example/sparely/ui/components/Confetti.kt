package com.example.sparely.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun ConfettiExplosion(
    modifier: Modifier = Modifier,
    durationMillis: Int = 3000,
    particleCount: Int = 75,
    colors: List<Color> = listOf(
        Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Cyan, Color.Magenta, Color(0xFFFFA500) // Orange
    ),
    onComplete: () -> Unit
) {
    val particles = remember {
        List(particleCount) {
            ConfettiParticle(
                color = colors.random(),
                startX = 0.5f,
                startY = 0.5f,
                angle = Random.nextFloat() * 2 * PI.toFloat(),
                speed = Random.nextFloat() * 0.7f + 0.3f, 
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = Random.nextFloat() * 20f - 10f
            )
        }
    }

    val animatable = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis, easing = LinearOutSlowInEasing)
        )
        onComplete()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerX = width * 0.5f
        val centerY = height * 0.5f
        val progress = animatable.value

        particles.forEach { particle ->
            // Physics simulation
            // Spread out initially based on angle and speed
            val blastDistance = particle.speed * (progress * 0.8f) * minOf(width, height) // Reduced blast radius relative to screen
            
            // Gravity effect increases with time^2
            val gravityDrop = (progress * progress * height * 0.8f) 

            val currentX = centerX + cos(particle.angle) * blastDistance
            val currentY = centerY + sin(particle.angle) * blastDistance + gravityDrop - (height * 0.1f) // Start slightly higher

            // Fade out only near the end
            val alpha = if (progress < 0.8f) 1f else (1f - (progress - 0.8f) * 5f).coerceIn(0f, 1f)
            
            withTransform({
                translate(currentX, currentY)
                rotate(particle.rotation + particle.rotationSpeed * progress * 10f)
                val scale = 1f - (progress * 0.3f) // Don't shrink too much
                scale(scale, scale)
            }) {
                drawRect(
                    color = particle.color.copy(alpha = alpha),
                    topLeft = Offset(-12f, -12f),
                    size = androidx.compose.ui.geometry.Size(24f, 24f)
                )
            }
        }
    }
}

private data class ConfettiParticle(
    val color: Color,
    val startX: Float,
    val startY: Float,
    val angle: Float,
    val speed: Float,
    val rotation: Float,
    val rotationSpeed: Float
)
