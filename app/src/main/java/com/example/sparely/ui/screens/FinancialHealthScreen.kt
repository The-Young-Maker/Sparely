package com.example.sparely.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sparely.domain.model.*
import com.example.sparely.ui.state.SparelyUiState

@Composable
fun FinancialHealthScreen(
    uiState: SparelyUiState,
    onNavigateBack: () -> Unit
) {
    val healthScore = uiState.financialHealthScore
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        if (healthScore != null) {
            item {
                HealthScoreCard(healthScore)
            }

            item {
                Text(
                    text = "Score Breakdown",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                ScoreBreakdownCard(healthScore)
            }

            if (healthScore.topStrengths.isNotEmpty()) {
                item {
                    Text(
                        text = "Your Strengths",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(healthScore.topStrengths) { strength ->
                    StrengthCard(strength)
                }
            }

            if (healthScore.improvementAreas.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ways to Improve",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(healthScore.improvementAreas) { tip ->
                    ImprovementTipCard(tip)
                }
            }
        } else {
            item {
                Text("Loading health data...")
            }
        }
    }
}

@Composable
fun HealthScoreCard(healthScore: FinancialHealthScore) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (healthScore.healthLevel) {
                HealthLevel.EXCELLENT -> Color(0xFF4CAF50)
                HealthLevel.GOOD -> Color(0xFF8BC34A)
                HealthLevel.FAIR -> Color(0xFFFFC107)
                HealthLevel.NEEDS_WORK -> Color(0xFFFF9800)
                HealthLevel.CRITICAL -> Color(0xFFF44336)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Your Score",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            AnimatedScoreCircle(
                score = healthScore.overallScore,
                size = 200.dp,
                strokeWidth = 20.dp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = healthScore.healthLevel.label,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                text = "Financial Health",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun AnimatedScoreCircle(
    score: Int,
    size: Dp = 150.dp,
    strokeWidth: Dp = 16.dp
) {
    var animatedScore by remember { mutableStateOf(0f) }
    
    LaunchedEffect(score) {
        animate(
            initialValue = animatedScore,
            targetValue = score.toFloat(),
            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
        ) { value, _ ->
            animatedScore = value
        }
    }

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = this.size.minDimension
            val strokeWidthPx = strokeWidth.toPx()
            
            // Background circle
            drawArc(
                color = Color.White.copy(alpha = 0.3f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(strokeWidthPx, cap = StrokeCap.Round),
                size = Size(diameter - strokeWidthPx, diameter - strokeWidthPx),
                topLeft = Offset(strokeWidthPx * 0.5f, strokeWidthPx * 0.5f)
            )
            
            // Score arc
            drawArc(
                color = Color.White,
                startAngle = -90f,
                sweepAngle = (animatedScore / 100f) * 360f,
                useCenter = false,
                style = Stroke(strokeWidthPx, cap = StrokeCap.Round),
                size = Size(diameter - strokeWidthPx, diameter - strokeWidthPx),
                topLeft = Offset(strokeWidthPx * 0.5f, strokeWidthPx * 0.5f)
            )
        }
        
        Text(
            text = "${animatedScore.toInt()}",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun ScoreBreakdownCard(healthScore: FinancialHealthScore) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            healthScore.scoreBreakdown.forEach { (category, score) ->
                ScoreBreakdownRow(category, score)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun ScoreBreakdownRow(category: String, score: Int) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = category,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "$score",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    score >= 80 -> Color(0xFF4CAF50)
                    score >= 60 -> Color(0xFFFFC107)
                    else -> Color(0xFFF44336)
                }
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
        progress = { (score / 100f).coerceIn(0f, 1f) },
        modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
        color = when {
                        score >= 80 -> Color(0xFF4CAF50)
                        score >= 60 -> Color(0xFFFFC107)
                        else -> Color(0xFFF44336)
                    },
        trackColor = ProgressIndicatorDefaults.linearTrackColor,
        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
        )
    }
}

@Composable
fun StrengthCard(strength: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = strength,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun ImprovementTipCard(tip: ImprovementTip) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (tip.priority) {
                Priority.HIGH -> MaterialTheme.colorScheme.errorContainer
                Priority.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer
                Priority.LOW -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tip.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                AssistChip(
                    onClick = {},
                    label = { Text("+${tip.potentialScoreGain} pts") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        labelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = tip.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = tip.actionable,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
