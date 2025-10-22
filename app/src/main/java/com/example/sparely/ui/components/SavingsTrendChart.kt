package com.example.sparely.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.sparely.domain.model.TrendPoint

@Composable
fun SavingsTrendCard(
    points: List<TrendPoint>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            text = "30-day savings trend",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
        SavingsTrendChart(
            points = points,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.8f)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
fun SavingsTrendChart(points: List<TrendPoint>, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = modifier) {
        val samples = if (points.size > 30) points.takeLast(30) else points
        if (samples.isEmpty()) {
            drawLine(
                color = primaryColor.copy(alpha = 0.3f),
                start = Offset(0f, size.height * 0.7f),
                end = Offset(size.width, size.height * 0.7f),
                strokeWidth = 4f
            )
            return@Canvas
        }
        val maxValue = samples.maxOf {
            maxOf(it.cumulativeSaved, it.cumulativeInvested)
        }.coerceAtLeast(1.0)
        val stepX = if (samples.size == 1) 0f else size.width / (samples.size - 1)
        val savedPath = Path()
        val investPath = Path()
        samples.forEachIndexed { index, point ->
            val x = stepX * index
            val savedY = size.height - (point.cumulativeSaved / maxValue).toFloat() * size.height
            val investY = size.height - (point.cumulativeInvested / maxValue).toFloat() * size.height
            if (index == 0) {
                savedPath.moveTo(x, savedY)
                investPath.moveTo(x, investY)
            } else {
                savedPath.lineTo(x, savedY)
                investPath.lineTo(x, investY)
            }
        }

        drawPath(
            path = savedPath,
            color = primaryColor,
            style = Stroke(width = 6f)
        )
        drawPath(
            path = investPath,
            color = tertiaryColor,
            style = Stroke(width = 6f)
        )
    }
}
