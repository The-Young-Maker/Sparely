package com.example.sparely.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sparely.R
import com.example.sparely.domain.model.*
import com.example.sparely.ui.components.SavingsTrendCard
import com.example.sparely.ui.state.SparelyUiState
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    uiState: SparelyUiState,
    onAddExpense: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToBudgets: () -> Unit = {},
    onNavigateToChallenges: () -> Unit = {},
    onNavigateToHealth: () -> Unit = {},
    onNavigateToRecurring: () -> Unit = {},
    onConfirmSmartTransfer: () -> Unit = {},
    onSnoozeSmartTransfer: () -> Unit = {},
    onDismissSmartTransfer: () -> Unit = {},
    onCompleteSmartTransfer: () -> Unit = {},
    onCancelSmartTransfer: (Boolean) -> Unit = {},
    onManageVaults: () -> Unit = {},
    onNavigateToVaultTransfers: () -> Unit = {}
) {
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            DashboardHeader(
                totalBalance = uiState.totalVaultBalance,
                monthlyIncome = uiState.settings.monthlyIncome,
                onAddExpense = onAddExpense,
                onNavigateToHistory = onNavigateToHistory
            )
        }

        if (uiState.smartVaults.isNotEmpty()) {
            item {
                SmartVaultsCard(
                    vaults = uiState.smartVaults,
                    totalBalance = uiState.totalVaultBalance,
                    pendingCount = uiState.pendingVaultContributions.size,
                    onManageVaults = onManageVaults,
                    onNavigateToTransfers = onNavigateToVaultTransfers
                )
            }
        }

        uiState.smartSavingSummary?.let { summary ->
            item {
                SmartSavingSnapshotCard(summary = summary, monthlyIncome = uiState.settings.monthlyIncome)
            }
        }

        uiState.smartTransfer?.let { smartRecommendation ->
            item {
                SmartTransferCard(
                    recommendation = smartRecommendation,
                    onConfirm = onConfirmSmartTransfer,
                    onSnooze = onSnoozeSmartTransfer,
                    onDismiss = onDismissSmartTransfer,
                    onComplete = onCompleteSmartTransfer,
                    onReturnToPending = { onCancelSmartTransfer(true) }
                )
            }
        }

        uiState.emergencyFundGoal?.let { goal ->
            item {
                EmergencyFundCard(goal = goal, settings = uiState.settings)
            }
        }
        
        // Financial Health Score - New Feature!
        uiState.financialHealthScore?.let { healthScore ->
            item {
                QuickHealthScoreCard(healthScore, onNavigateToHealth)
            }
        }
        
        item {
            MetricsRow(uiState)
        }

        if (uiState.detectedRecurringTransactions.isNotEmpty()) {
            item {
                RecurringInsightsCard(insights = uiState.detectedRecurringTransactions)
            }
        }
        
        val budgetSummary = uiState.budgetSummary
        item {
            if (budgetSummary != null) {
                QuickBudgetCard(budgetSummary, onNavigateToBudgets)
            } else {
                BudgetEmptyCard(onNavigateToBudgets)
            }
        }

        item {
            UpcomingRecurringCard(
                items = uiState.upcomingRecurring,
                hasRecurring = uiState.recurringExpenses.isNotEmpty(),
                onManageRecurring = onNavigateToRecurring
            )
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun DashboardHeader(
    totalBalance: Double,
    monthlyIncome: Double,
    onAddExpense: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Section with gradient card
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Saved",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = formatCurrency(totalBalance),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.Savings,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    
                    if (monthlyIncome > 0) {
                        val savingsRate = if (totalBalance > 0) (totalBalance / monthlyIncome) * 100 else 0.0
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "${String.format("%.1f", savingsRate)}% of monthly income",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }
        
        // Quick Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(
                onClick = onAddExpense,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Purchase")
            }
            Button(
                onClick = onNavigateToHistory,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("History")
            }
        }
    }
}

@Composable
private fun SmartVaultsCard(
    vaults: List<SmartVault>,
    totalBalance: Double,
    pendingCount: Int,
    onManageVaults: () -> Unit,
    onNavigateToTransfers: () -> Unit
) {
    val accentYellow = Color(0xFFFACC15)
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Smart Vaults",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (pendingCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = pendingCount.toString(),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Net saved ${formatCurrency(totalBalance)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(onClick = onManageVaults) {
                    Text("Manage", fontWeight = FontWeight.SemiBold)
                }
            }

            // Vault List
            val previewVaults = vaults.take(3)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                previewVaults.forEach { vault ->
                    VaultItem(vault = vault, accentYellow = accentYellow, dateFormatter = dateFormatter)
                }
            }
            
            if (vaults.size > 3) {
                Text(
                    text = "+${vaults.size - 3} more vault${if (vaults.size - 3 == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            if (pendingCount > 0) {
                HorizontalDivider()
                Button(
                    onClick = onNavigateToTransfers,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("View $pendingCount Pending Transfer${if (pendingCount == 1) "" else "s"}")
                }
            }
        }
    }
}

@Composable
private fun VaultItem(
    vault: SmartVault,
    accentYellow: Color,
    dateFormatter: DateTimeFormatter
) {
    val progress = if (vault.targetAmount <= 0) 0f 
                  else (vault.currentBalance / vault.targetAmount).toFloat().coerceIn(0f, 1f)
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "progress"
    )
    
    val urgencyColor = when (vault.priority) {
        VaultPriority.CRITICAL -> MaterialTheme.colorScheme.error
        VaultPriority.HIGH -> accentYellow
        VaultPriority.MEDIUM -> MaterialTheme.colorScheme.primary
        VaultPriority.LOW -> MaterialTheme.colorScheme.secondary
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.size(52.dp),
                            color = urgencyColor,
                            strokeWidth = 5.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
                        )
                        Text(
                            text = String.format("%.0f%%", progress * 100),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(
                            vault.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val targetText = buildString {
                            append("Goal: ")
                            append(formatCurrency(vault.targetAmount))
                            vault.targetDate?.let {
                                append(" • ${it.format(dateFormatter)}")
                            }
                        }
                        Text(
                            text = targetText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(vault.currentBalance),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = urgencyColor
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = urgencyColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = vault.type.displayName(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = urgencyColor
                        )
                    }
                }
            }
            
            vault.nextExpectedContribution?.takeIf { it > 0 }?.let { nextAmount ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = urgencyColor
                    )
                    Text(
                        text = "Next contribution ${formatCurrency(nextAmount)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = urgencyColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SmartSavingSnapshotCard(summary: SmartSavingSummary, monthlyIncome: Double) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Smart saving",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
            )
            Text(
                text = "${formatPercent(summary.actualSavingsRate)} saved vs target ${formatPercent(summary.targetSavingsRate)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = when (summary.allocationMode) {
                    SmartAllocationMode.MANUAL -> "Manual mode — tuning your own percentages"
                    SmartAllocationMode.GUIDED -> "Guided mode — Sparely suggests tweaks"
                    SmartAllocationMode.AUTOMATIC -> "Automatic mode — Sparely adjusts for you"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Recommended split", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    Text(
                        text = "${formatPercent(summary.recommendedSplit.emergency)} emergency / ${formatPercent(summary.recommendedSplit.invest)} invest / ${formatPercent(summary.recommendedSplit.`fun`)} fun",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Manual split", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    Text(
                        text = "${formatPercent(summary.manualSplit.emergency)} / ${formatPercent(summary.manualSplit.invest)} / ${formatPercent(summary.manualSplit.`fun`)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            if (monthlyIncome > 0.0) {
                val monthlyTarget = monthlyIncome * summary.targetSavingsRate
                Text(
                    text = "Aim for ${formatCurrency(monthlyTarget)} per month to hit target",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun RecurringInsightsCard(insights: List<DetectedRecurringTransaction>) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Recurring patterns", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            val previewInsights = insights.take(4)
            val formatter = DateTimeFormatter.ofPattern("MMM d")
            previewInsights.forEach { insight ->
                Column {
                    Text(insight.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(
                        text = "${formatCurrency(insight.averageAmount)} every ${insight.cadenceDays}d",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Last on ${insight.lastOccurrence.format(formatter)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (insight != previewInsights.last()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = DividerDefaults.Thickness,
                        color = DividerDefaults.color
                    )
                }
            }
        }
    }
}

@Composable
private fun UpcomingRecurringCard(
    items: List<UpcomingRecurringExpense>,
    hasRecurring: Boolean,
    onManageRecurring: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("MMM d")
    Card(
        onClick = onManageRecurring,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Upcoming bills",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (hasRecurring) "Tap to manage recurring payments" else "Tap to add recurring payments",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onManageRecurring) {
                    Text(if (hasRecurring) "Manage" else "Add")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (items.isEmpty()) {
                Text(
                    text = if (hasRecurring) "You're all caught up!" else "Log your subscriptions and bills to get reminders.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                items.forEach { upcoming ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = upcoming.recurringExpense.description,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = upcoming.recurringExpense.merchantName ?: upcoming.recurringExpense.category.displayName(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatCurrency(upcoming.recurringExpense.amount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Due ${upcoming.dueDate.format(formatter)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    if (upcoming != items.last()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = DividerDefaults.Thickness,
                            color = DividerDefaults.color
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartTransferCard(
    recommendation: SmartTransferRecommendation,
    onConfirm: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
    onReturnToPending: () -> Unit
) {
    val (containerColor, onContainerColor) = when (recommendation.status) {
        SmartTransferStatus.READY -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        SmartTransferStatus.ACCUMULATING -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        SmartTransferStatus.STANDBY -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        SmartTransferStatus.AWAITING_CONFIRMATION -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }
    val totalForDisplay = when (recommendation.status) {
        SmartTransferStatus.AWAITING_CONFIRMATION -> recommendation.awaitingConfirmationAmount
        else -> recommendation.totalAmount
    }
    val totalLabel = formatCurrency(totalForDisplay)
    val countLabel = if (recommendation.pendingExpenseCount == 1) {
        "1 purchase"
    } else {
        "${recommendation.pendingExpenseCount} purchases"
    }
    val holdMinutes = recommendation.holdUntilEpochMillis?.let { expiry ->
        val remaining = expiry - System.currentTimeMillis()
        if (remaining > 0) remaining / 60000.0 else 0.0
    }
    val headline = when (recommendation.status) {
        SmartTransferStatus.READY -> "Move $totalLabel to savings"
        SmartTransferStatus.ACCUMULATING -> "Holding $totalLabel for the next expense"
        SmartTransferStatus.STANDBY -> "$totalLabel sitting in standby"
        SmartTransferStatus.AWAITING_CONFIRMATION -> "Don't forget to move $totalLabel"
    }
    val supportingText = when (recommendation.status) {
        SmartTransferStatus.READY -> "Great time to shift the cash before it gets spent elsewhere."
        SmartTransferStatus.ACCUMULATING -> when {
            holdMinutes != null && holdMinutes > 0.1 -> "We'll wait about ${String.format("%.1f", holdMinutes)} min in case you log another purchase."
            else -> "We'll keep the pot warm while you finish logging expenses."
        }
        SmartTransferStatus.STANDBY -> {
            val shortfall = recommendation.shortfallToThreshold
            if (shortfall > 0.0) {
                "Needs ${formatCurrency(recommendation.minimumTransferAmount)} total. ${formatCurrency(shortfall)} more will trigger the move."
            } else {
                "Below your preferred threshold, but ready whenever you are."
            }
        }
        SmartTransferStatus.AWAITING_CONFIRMATION -> "Keep this pinned while you move the funds, then mark it done."
    }

    Card(colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Smart transfer",
                style = MaterialTheme.typography.labelMedium,
                color = onContainerColor.copy(alpha = 0.7f)
            )
            Text(
                text = headline,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = onContainerColor
            )
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = onContainerColor
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Emergency",
                        style = MaterialTheme.typography.labelSmall,
                        color = onContainerColor.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatCurrency(recommendation.emergencyPortion),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = onContainerColor
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Investing",
                        style = MaterialTheme.typography.labelSmall,
                        color = onContainerColor.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatCurrency(recommendation.investmentPortion),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = onContainerColor
                    )
                }
            }
            Text(
                text = "Built from $countLabel",
                style = MaterialTheme.typography.bodySmall,
                color = onContainerColor.copy(alpha = 0.7f)
            )
            when (recommendation.status) {
                SmartTransferStatus.READY -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(onClick = onConfirm, modifier = Modifier.weight(1f)) {
                            Text("Move now")
                        }
                        OutlinedButton(onClick = onSnooze, modifier = Modifier.weight(1f)) {
                            Text("Log another first")
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Dismiss")
                        }
                    }
                }
                SmartTransferStatus.ACCUMULATING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(onClick = onConfirm, modifier = Modifier.weight(1f)) {
                            Text("Move now")
                        }
                        OutlinedButton(onClick = onSnooze, modifier = Modifier.weight(1f)) {
                            Text("Keep waiting")
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Dismiss")
                        }
                    }
                }
                SmartTransferStatus.STANDBY -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(onClick = onConfirm, modifier = Modifier.weight(1f)) {
                            Text("Move anyway")
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Reset")
                        }
                    }
                }
                SmartTransferStatus.AWAITING_CONFIRMATION -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(onClick = onComplete, modifier = Modifier.weight(1f)) {
                                Text("Mark done")
                            }
                            OutlinedButton(onClick = onReturnToPending, modifier = Modifier.weight(1f)) {
                                Text("I'll do it later")
                            }
                            TextButton(onClick = onDismiss) {
                                Text("Clear")
                            }
                        }
                        Text(
                            text = "Amounts won't be logged until you tap Mark done.",
                            style = MaterialTheme.typography.bodySmall,
                            color = onContainerColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmergencyFundCard(goal: EmergencyFundGoal, settings: SparelySettings) {
    val coverage = goal.coverageRatio.coerceIn(0.0, 1.0)
    val animatedCoverage by animateFloatAsState(
        targetValue = coverage.toFloat(),
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "coverage"
    )
    val coverageLabel = String.format("%.0f%%", coverage * 100)
    val savedAmount = (goal.targetAmount - goal.shortfallAmount).coerceAtLeast(0.0)
    val shortfall = goal.shortfallAmount.coerceAtLeast(0.0)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Emergency Runway",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${formatMonths(goal.targetMonths)} month goal • ${formatCurrency(goal.targetAmount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Animated Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = coverageLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = formatCurrency(savedAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                LinearProgressIndicator(
                    progress = { animatedCoverage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                )
            }

            // Stats Grid
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DetailRow(
                        label = "Current cushion",
                        value = formatCurrency(savedAmount)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow(
                        label = "Shortfall",
                        value = formatCurrency(shortfall),
                        valueColor = if (shortfall > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    if (goal.recommendedMonthlyContribution > 0.0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailRow(
                            label = "Monthly target",
                            value = formatCurrency(goal.recommendedMonthlyContribution),
                            valueColor = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            if (goal.recommendedMonthlyContribution <= 0.0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Goal reached! Keep contributing to stay ahead of inflation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

@Composable
private fun MetricsRow(uiState: SparelyUiState) {
    val totalsByType = uiState.smartVaults
        .groupBy { it.type }
        .mapValues { (_, vaults) -> vaults.sumOf { it.currentBalance } }

    val shortTermTotal = totalsByType[VaultType.SHORT_TERM] ?: 0.0
    val longTermTotal = totalsByType[VaultType.LONG_TERM] ?: 0.0
    val passiveTotal = totalsByType[VaultType.PASSIVE_INVESTMENT] ?: 0.0

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Savings Breakdown",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModernMetricCard(
                title = "Short-term",
                value = shortTermTotal,
                icon = Icons.Outlined.Savings,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            ModernMetricCard(
                title = "Long-term",
                value = longTermTotal,
                icon = Icons.Outlined.AccountBalance,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModernMetricCard(
                title = "Passive growth",
                value = passiveTotal,
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
            ModernMetricCard(
                title = "Monthly avg",
                value = uiState.analytics.averageMonthlyReserve,
                subtitle = "Projected\n${formatCurrency(uiState.analytics.projectedReserveSixMonths)}\nin 6 months",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ModernMetricCard(
    title: String,
    value: Double,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    ElevatedCard(
        modifier = modifier.shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = formatCurrency(value),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecommendationCard(recommendation: RecommendationResult) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Suggested allocations",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            AllocationRow(label = "Emergency", value = recommendation.recommendedPercentages.emergency)
            AllocationRow(label = "Invest", value = recommendation.recommendedPercentages.invest)
            AllocationRow(label = "Fun", value = recommendation.recommendedPercentages.`fun`)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Investments: ${formatPercent(recommendation.safeInvestmentRatio)} safe / ${formatPercent(recommendation.highRiskInvestmentRatio)} high risk",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = recommendation.rationale,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun AllocationRow(label: String, value: Double) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Text(formatPercent(value))
    }
}

@Composable
private fun AlertsSection(alerts: List<AlertMessage>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Insights",
            style = MaterialTheme.typography.titleMedium
        )
        alerts.forEach { alert ->
            AssistChip(
                onClick = {},
                label = { Text(alert.title) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
            Text(
                text = alert.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        }
    }
}

@Composable
private fun GoalsSnapshot(uiState: SparelyUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Goals progress",
            style = MaterialTheme.typography.titleMedium
        )
        uiState.goals.take(3).forEach { goal ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(goal.title, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${formatCurrency(goal.progressAmount)} of ${formatCurrency(goal.targetAmount)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ProgressBar(progress = goal.progressPercent)
                    goal.projectedCompletion?.let { date ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Projected completion ${date.format(DateTimeFormatter.ISO_DATE)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressBar(progress: Double) {
    val clamped = progress.coerceIn(0.0, 1.0).toFloat()
    LinearProgressIndicator(
    progress = { clamped },
    modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
    color = MaterialTheme.colorScheme.primary,
    trackColor = MaterialTheme.colorScheme.surfaceVariant,
    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
    )
}

@Composable
private fun QuickHealthScoreCard(healthScore: FinancialHealthScore, onClick: () -> Unit) {
    Card(
        onClick = onClick,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Financial Health",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = healthScore.healthLevel.label,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Text(
                text = "${healthScore.overallScore}",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun QuickBudgetCard(budgetSummary: BudgetSummary, onClick: () -> Unit) {
    val statusColors = when (budgetSummary.overallHealth) {
        BudgetHealthStatus.HEALTHY -> BudgetStatusColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            indicatorColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        BudgetHealthStatus.WARNING -> BudgetStatusColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            indicatorColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
        BudgetHealthStatus.CRITICAL -> BudgetStatusColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            indicatorColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
        BudgetHealthStatus.OVER_BUDGET -> BudgetStatusColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            indicatorColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = statusColors.containerColor,
            contentColor = statusColors.contentColor
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Budget This Month",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatPercent(budgetSummary.percentageUsed),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = statusColors.indicatorColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
            progress = { budgetSummary.percentageUsed.toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
            color = statusColors.indicatorColor,
            trackColor = ProgressIndicatorDefaults.linearTrackColor,
            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${formatCurrency(budgetSummary.totalRemaining)} of ${formatCurrency(budgetSummary.totalBudget)} remaining",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun BudgetEmptyCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Budget status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Set up category budgets to start tracking progress with alerts.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onClick) {
                Text("Create your first budget")
            }
        }
    }
}

@Composable
private fun QuickChallengesCard(challenges: List<SavingsChallenge>, onClick: () -> Unit) {
    val streakColor = MaterialTheme.colorScheme.tertiary
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Active Challenges",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            challenges.forEach { challenge ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = challenge.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (challenge.targetAmount > 0) {
                            Text(
                                text = "${formatPercent(challenge.progressPercent)} complete",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    if (challenge.streakDays > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${challenge.streakDays}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = streakColor
                            )
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = streakColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                if (challenge != challenges.last()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = DividerDefaults.Thickness,
                        color = DividerDefaults.color
                    )
                }
            }
        }
    }
}

@Composable
private fun ChallengesEmptyCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Savings challenges",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start a challenge to build streaks and unlock achievements.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onClick) {
                Text("Browse challenges")
            }
        }
    }
}

private data class BudgetStatusColors(
    val containerColor: Color,
    val indicatorColor: Color,
    val contentColor: Color
)

private fun formatMonths(months: Double): String =
    if (months % 1.0 == 0.0) months.toInt().toString() else String.format("%.1f", months)

private fun formatCurrency(value: Double): String = "$" + String.format("%,.2f", value)

private fun formatPercent(value: Double): String = String.format("%.1f%%", value.coerceIn(0.0, 1.0) * 100)

private fun VaultType.displayName(): String = when (this) {
    VaultType.SHORT_TERM -> "Short-term"
    VaultType.LONG_TERM -> "Long-term"
    VaultType.PASSIVE_INVESTMENT -> "Passive"
}

