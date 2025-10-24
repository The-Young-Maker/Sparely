package com.example.sparely.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sparely.domain.model.*
import com.example.sparely.ui.state.SparelyUiState
import com.example.sparely.ui.theme.MaterialSymbolIcon
import com.example.sparely.ui.theme.MaterialSymbols
import java.time.format.DateTimeFormatter

@Composable
fun ChallengesScreen(
    uiState: SparelyUiState,
    onStartChallenge: (ChallengeInput) -> Unit,
    onNavigateBack: () -> Unit
) {
    var showChallengeDialog by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { showChallengeDialog = true }) {
                    MaterialSymbolIcon(icon = MaterialSymbols.ADD, "New Challenge")
                }
            }
        }

        item {
            ChallengeOverviewCard()
        }

        if (uiState.achievements.isNotEmpty()) {
            item {
                AchievementsSection(uiState.achievements.take(5))
            }
        }

        if (uiState.activeChallenges.isNotEmpty()) {
            item {
                Text(
                    text = "Active Challenges",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            items(uiState.activeChallenges.filter { it.isActive && !it.isCompleted }) { challenge ->
                ChallengeCard(challenge)
            }
        }

        val completedChallenges = uiState.activeChallenges.filter { it.isCompleted }
        if (completedChallenges.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Completed Challenges",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            items(completedChallenges) { challenge ->
                CompletedChallengeCard(challenge)
            }
        }

        if (uiState.activeChallenges.isEmpty()) {
            item {
                EmptyChallengesState(onStartChallenge = { showChallengeDialog = true })
            }
        }
    }

    if (showChallengeDialog) {
        ChallengeSelectionDialog(
            onDismiss = { showChallengeDialog = false },
            onSelectChallenge = { input ->
                onStartChallenge(input)
                showChallengeDialog = false
            }
        )
    }
}

@Composable
fun ChallengeCard(challenge: SavingsChallenge) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = challenge.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = challenge.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                
                MaterialSymbolIcon(
                    icon = when (challenge.type) {
                        ChallengeType.FIFTY_TWO_WEEK -> MaterialSymbols.CALENDAR_MONTH
                        ChallengeType.NO_SPEND_DAYS -> MaterialSymbols.BLOCK
                        ChallengeType.DAILY_SAVINGS -> MaterialSymbols.TODAY
                        else -> MaterialSymbols.TROPHY
                    },
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary,
                    size = 40.dp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress
            if (challenge.targetAmount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatCurrency(challenge.currentAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "of ${formatCurrency(challenge.targetAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = challenge.progressPercent.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "${String.format("%.0f", challenge.progressPercent * 100)}% complete",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            
            // Streak
            if (challenge.streakDays > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MaterialSymbolIcon(
                        icon = MaterialSymbols.LOCAL_FIRE_DEPARTMENT,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(20.dp),
                        size = 20.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${challenge.streakDays} day streak!",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Next milestone
            challenge.nextMilestone?.let { milestone ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Next Milestone",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = milestone.description,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        AssistChip(
                            onClick = {},
                            label = { Text("+${milestone.rewardPoints} pts") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${challenge.daysRemaining} days remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = "Ends ${challenge.endDate.format(DateTimeFormatter.ofPattern("MMM d"))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ChallengeOverviewCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "How challenges help",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Savings challenges don't move money automatically. Sparely tracks your commitment, nudges you when it's time to contribute, and records progress as you log expenses or manual transfers.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Daily and 52-week challenges add their scheduled amount to your progress so you know what to transfer. No-spend challenges monitor your transactions and keep your streak alive when you avoid the selected categories.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
fun CompletedChallengeCard(challenge: SavingsChallenge) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
        ),
        border = BorderStroke(2.dp, Color(0xFF4CAF50))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MaterialSymbolIcon(
                    icon = MaterialSymbols.CHECK_CIRCLE,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(32.dp),
                    size = 32.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = challenge.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Completed ${challenge.completedDate?.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = formatCurrency(challenge.currentAmount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
fun AchievementsSection(achievements: List<Achievement>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Achievements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                MaterialSymbolIcon(icon = MaterialSymbols.TROPHY, contentDescription = null)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            achievements.forEach { achievement ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = achievement.icon,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = achievement.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = achievement.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
                if (achievement != achievements.last()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
fun EmptyChallengesState(onStartChallenge: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MaterialSymbolIcon(
                icon = MaterialSymbols.TROPHY,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Active Challenges",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start a savings challenge to gamify your financial journey and earn rewards!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onStartChallenge) {
                MaterialSymbolIcon(icon = MaterialSymbols.PLAY_ARROW, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Challenge")
            }
        }
    }
}

@Composable
fun ChallengeSelectionDialog(
    onDismiss: () -> Unit,
    onSelectChallenge: (ChallengeInput) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a Challenge") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChallengeOption(
                    title = "52-Week Challenge",
                    description = "Save incrementally each week",
                    icon = MaterialSymbols.CALENDAR_MONTH,
                    onClick = {
                        onSelectChallenge(
                            ChallengeInput(
                                type = ChallengeType.FIFTY_TWO_WEEK,
                                title = "52-Week Money Challenge",
                                description = "Save $1 in week 1, $2 in week 2, etc.",
                                targetAmount = 1378.0,
                                endDate = java.time.LocalDate.now().plusWeeks(52)
                            )
                        )
                    }
                )
                
                ChallengeOption(
                    title = "Daily Savings",
                    description = "Save $5 every day for 30 days",
                    icon = MaterialSymbols.TODAY,
                    onClick = {
                        onSelectChallenge(
                            ChallengeInput(
                                type = ChallengeType.DAILY_SAVINGS,
                                title = "30-Day Daily Challenge",
                                description = "Save $5 daily",
                                targetAmount = 150.0,
                                endDate = java.time.LocalDate.now().plusDays(30)
                            )
                        )
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ChallengeOption(
    title: String,
    description: String,
    @androidx.annotation.DrawableRes icon: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MaterialSymbolIcon(
                icon = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary,
                size = 32.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatCurrency(value: Double): String = "$" + String.format("%,.2f", value)
