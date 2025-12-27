package com.example.sparely.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import com.example.sparely.ui.theme.ExpressiveShapes
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button

import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.sparely.R
import com.example.sparely.domain.model.*
import com.example.sparely.ui.components.ExpressiveCard
import com.example.sparely.ui.components.SavingsTrendCard
import com.example.sparely.ui.components.SingleLineText
import com.example.sparely.ui.components.SparelyButton
import com.example.sparely.ui.components.SparelyTextButton
import com.example.sparely.ui.components.SparelyTonalButton
import com.example.sparely.ui.state.SparelyUiState
import com.example.sparely.ui.theme.MaterialSymbolIcon
import com.example.sparely.ui.theme.MaterialSymbols
import com.example.sparely.ui.theme.spacing
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: SparelyUiState,
    onAddExpense: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToBudgets: () -> Unit = {},
    onNavigateToChallenges: () -> Unit = {},
    onNavigateToHealth: () -> Unit = {},
    onNavigateToRecurring: () -> Unit = {},
    onManageVaults: () -> Unit = {},
    onNavigateToVaultTransfers: () -> Unit = {},
    onNavigateToMainAccount: () -> Unit = {},
    // allow parent to hide dashboard's own FAB when a global FAB/menu is provided
    showFloatingFab: Boolean = true
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
    val spacing = MaterialTheme.spacing

    // Removed local TopAppBar - using global SparelyTopBar instead
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (showFloatingFab) {
                ExtendedFloatingActionButton(
                    text = { SingleLineText(stringResource(R.string.dashboard_log_purchase)) },
                    icon = {
                        MaterialSymbolIcon(
                            icon = MaterialSymbols.ADD,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = onAddExpense,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = spacing.lg,
                end = spacing.lg,
                top = spacing.md,
                bottom = spacing.xl
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                DashboardHeroSection(
                    totalBalance = uiState.totalVaultBalance,
                    monthlyIncome = uiState.settings.monthlyIncome,
                    actualSavingsRate = uiState.smartSavingSummary?.actualSavingsRate ?: 0.0,
                    onAddExpense = onAddExpense,
                    onNavigateToHistory = onNavigateToHistory,
                    onManageVaults = onManageVaults
                )
            }

            if (uiState.settings.mainAccountBalance != 0.0 || uiState.mainAccountTransactions.isNotEmpty()) {
                item {
                    MainAccountBalanceCard(
                        balance = uiState.settings.mainAccountBalance,
                        onClick = onNavigateToMainAccount
                    )
                }
            }

            item {
                DashboardVaultsSection(
                    vaults = uiState.smartVaults,
                    totalBalance = uiState.totalVaultBalance,
                    pendingCount = uiState.pendingVaultContributions.size,
                    onManageVaults = onManageVaults,
                    onNavigateToTransfers = onNavigateToVaultTransfers
                )
            }

            // Quick Links / Insights Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    // Health Score Tile
                    uiState.financialHealthScore?.let { healthScore ->
                        Box(modifier = Modifier.weight(1f)) {
                            QuickHealthScoreCard(healthScore, onNavigateToHealth)
                        }
                    }

                    // Budget Tile
                    val budgetSummary = uiState.budgetSummary
                    Box(modifier = Modifier.weight(1f)) {
                        if (budgetSummary != null) {
                            QuickBudgetCard(budgetSummary, onNavigateToBudgets)
                        } else {
                            BudgetEmptyCard(onNavigateToBudgets)
                        }
                    }
                }
            }

            uiState.smartSavingSummary?.let { summary ->
                item {
                    SmartSavingSnapshotCard(
                        summary = summary,
                        monthlyIncome = uiState.settings.monthlyIncome
                    )
                }
            }

            uiState.emergencyFundGoal?.let { goal ->
                item {
                    EmergencyFundCard(goal = goal, settings = uiState.settings)
                }
            }
            
            // Metrics removed in favor of Grid

            if (uiState.detectedRecurringTransactions.isNotEmpty()) {
                item {
                    RecurringInsightsCard(insights = uiState.detectedRecurringTransactions)
                }
            }

            item {
                if (uiState.activeChallenges.isEmpty()) {
                    ChallengesEmptyCard(onClick = onNavigateToChallenges)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                        Text(
                           "Active Challenges", 
                           style = MaterialTheme.typography.titleMedium,
                           fontWeight = FontWeight.Bold
                        )
                        for (challenge in uiState.activeChallenges) {
                            QuickChallengeItem(challenge = challenge, onClick = onNavigateToChallenges)
                        }
                    }
                }
            }

            item {
                UpcomingRecurringCard(
                    items = uiState.upcomingRecurring,
                    hasRecurring = uiState.recurringExpenses.isNotEmpty(),
                    onManageRecurring = onNavigateToRecurring
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)


@Composable
private fun DashboardHeroSection(
    totalBalance: Double,
    monthlyIncome: Double,
    actualSavingsRate: Double,
    onAddExpense: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onManageVaults: () -> Unit
) {
    val spacing = MaterialTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp, horizontal = spacing.lg)
            ) {
                Text(
                    text = stringResource(R.string.dashboard_total_saved),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = formatCurrency(totalBalance),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                if (monthlyIncome > 0) {
                    val displayRate = (actualSavingsRate * 100)
                    Surface(
                        shape = RoundedCornerShape(100),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            MaterialSymbolIcon(
                                icon = MaterialSymbols.TRENDING_UP,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Saving ${String.format("%.1f%%", displayRate)}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            SparelyTonalButton(
                onClick = onManageVaults,
                modifier = Modifier.weight(1f),
                icon = {
                    MaterialSymbolIcon(
                        icon = MaterialSymbols.ACCOUNT_BALANCE,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            ) {
                SingleLineText(stringResource(R.string.dashboard_manage))
            }
            SparelyTextButton(
                onClick = onNavigateToHistory,
                modifier = Modifier.weight(1f),
                icon = {
                    MaterialSymbolIcon(
                        icon = MaterialSymbols.HISTORY,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            ) {
                SingleLineText(stringResource(R.string.dashboard_history))
            }
        }
    }
}

@Composable
private fun DashboardVaultsSection(
    vaults: List<SmartVault>,
    totalBalance: Double,
    pendingCount: Int,
    onManageVaults: () -> Unit,
    onNavigateToTransfers: () -> Unit
) {
    val accentColor = MaterialTheme.colorScheme.tertiary
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    val spacing = MaterialTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.dashboard_smart_vaults),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (pendingCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(100),
                        color = MaterialTheme.colorScheme.error
                    ) {
                        Text(
                            text = "$pendingCount pending",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }
        }

        // Horizontal Carousel
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(vaults.take(5)) { vault ->
                Box(modifier = Modifier.width(320.dp).height(145.dp)) {
                    VaultItem(vault = vault, accentColor = accentColor, dateFormatter = dateFormatter)
                }
            }
            
            item {
                if (vaults.size > 5) {
                    Surface(
                        onClick = onManageVaults,
                        modifier = Modifier.width(120.dp).height(145.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                             MaterialSymbolIcon(
                                icon = MaterialSymbols.ARROW_FORWARD,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "View All", 
                                style = MaterialTheme.typography.labelLarge, 
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
        
        if (pendingCount > 0) {
            SparelyButton(
                onClick = onNavigateToTransfers,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                val label = if (pendingCount == 1) {
                    stringResource(R.string.dashboard_pending_transfer, pendingCount)
                } else {
                    stringResource(R.string.dashboard_pending_transfers, pendingCount)
                }
                SingleLineText(label)
            }
        }
    }
}

@Composable
private fun VaultItem(
    vault: SmartVault,
    accentColor: Color,
    dateFormatter: DateTimeFormatter
) {
    val spacing = MaterialTheme.spacing
    val progress = if (vault.targetAmount <= 0) 0f
                  else (vault.currentBalance / vault.targetAmount).toFloat().coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "progress"
    )

    val urgencyColor = when (vault.priority) {
        VaultPriority.CRITICAL -> MaterialTheme.colorScheme.error
        VaultPriority.HIGH -> accentColor
        VaultPriority.MEDIUM -> MaterialTheme.colorScheme.primary
        VaultPriority.LOW -> MaterialTheme.colorScheme.secondary
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
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
                            append(stringResource(R.string.dashboard_goal_prefix))
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
            
            Spacer(modifier = Modifier.weight(1f))

            vault.nextExpectedContribution?.takeIf { it > 0 }?.let { nextAmount ->
                Spacer(modifier = Modifier.height(spacing.xs))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.xxs)
                ) {
                    MaterialSymbolIcon(icon = MaterialSymbols.TRENDING_UP,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = urgencyColor
                    )
                    Text(
                        text = stringResource(R.string.dashboard_next_contribution, formatCurrency(nextAmount)),
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
    val spacing = MaterialTheme.spacing
    val isOnTrack = summary.actualSavingsRate >= summary.targetSavingsRate
    val statusColor = if (isOnTrack) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
    
    ExpressiveCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        MaterialSymbolIcon(
                            icon = MaterialSymbols.SAVINGS,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.dashboard_smart_saving),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (summary.allocationMode) {
                                SmartAllocationMode.MANUAL -> stringResource(R.string.dashboard_allocation_manual_mode)
                                SmartAllocationMode.GUIDED -> stringResource(R.string.dashboard_allocation_guided_mode)
                                SmartAllocationMode.AUTOMATIC -> stringResource(R.string.dashboard_allocation_automatic_mode)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Savings Rate Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Savings Rate",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatPercent(summary.actualSavingsRate),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Target: ${formatPercent(summary.targetSavingsRate)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MaterialSymbolIcon(
                            icon = if (isOnTrack) MaterialSymbols.CHECK_CIRCLE else MaterialSymbols.WARNING,
                            contentDescription = null,
                            size = 16.dp,
                            tint = statusColor
                        )
                        Text(
                            text = if (isOnTrack) "On Track" else "Below Target",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            // Monthly target hint
            if (monthlyIncome > 0.0) {
                val monthlyTarget = monthlyIncome * summary.targetSavingsRate
                Text(
                    text = stringResource(R.string.dashboard_aim_for_target, formatCurrency(monthlyTarget)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AllocationChip(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RecurringInsightsCard(insights: List<DetectedRecurringTransaction>) {
    val spacing = MaterialTheme.spacing
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Text(stringResource(R.string.dashboard_recurring_patterns), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            val previewInsights = insights.take(4)
            val formatter = DateTimeFormatter.ofPattern("MMM d")
            for (insight in previewInsights) {
                Column {
                    Text(insight.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(
                        text = "${formatCurrency(insight.averageAmount)} ${stringResource(R.string.dashboard_every_days, insight.cadenceDays)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.dashboard_last_on, insight.lastOccurrence.format(formatter)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (insight != previewInsights.last()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = spacing.xs),
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
    val spacing = MaterialTheme.spacing
    val totalUpcoming = items.sumOf { it.recurringExpense.amount }
    
    ExpressiveCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onManageRecurring,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        MaterialSymbolIcon(
                            icon = MaterialSymbols.CALENDAR_MONTH,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.dashboard_upcoming_bills),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (hasRecurring) stringResource(R.string.dashboard_tap_manage_recurring) else stringResource(R.string.dashboard_tap_add_recurring),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Bills list or empty state
            if (items.isEmpty()) {
                Text(
                    text = if (hasRecurring) stringResource(R.string.dashboard_all_caught_up) else stringResource(R.string.dashboard_log_subscriptions_reminders),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Total amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Total Due",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(totalUpcoming),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Text(
                        text = "${items.size} bills",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Bill items
                for (upcoming in items) {
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
                                text = "Due ${upcoming.dueDate.format(formatter)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = formatCurrency(upcoming.recurringExpense.amount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (upcoming != items.last()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = spacing.xs),
                            thickness = DividerDefaults.Thickness,
                            color = DividerDefaults.color.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmergencyFundCard(goal: EmergencyFundGoal, settings: SparelySettings) {
    val spacing = MaterialTheme.spacing
    val coverage = goal.coverageRatio.coerceIn(0.0, 1.0)
    val animatedCoverage by animateFloatAsState(
        targetValue = coverage.toFloat(),
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "coverage"
    )
    val savedAmount = (goal.targetAmount - goal.shortfallAmount).coerceAtLeast(0.0)
    val shortfall = goal.shortfallAmount.coerceAtLeast(0.0)
    val statusColor = if (coverage >= 1.0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary

    ExpressiveCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        MaterialSymbolIcon(
                            icon = MaterialSymbols.SECURITY,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.dashboard_emergency_runway),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${goal.targetMonths} month goal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Balance and Progress
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Current Cushion",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(savedAmount),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Target: ${formatCurrency(goal.targetAmount)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format("%.0f%%", coverage * 100),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { animatedCoverage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = statusColor,
                    trackColor = statusColor.copy(alpha = 0.2f),
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap
                )

                // Shortfall or success message
                if (shortfall > 0.0) {
                    Text(
                        text = "${formatCurrency(shortfall)} to go",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MaterialSymbolIcon(
                            icon = MaterialSymbols.CHECK_CIRCLE,
                            contentDescription = null,
                            size = 16.dp,
                            tint = Color(0xFF4CAF50)
                        )
                        Text(
                            text = stringResource(R.string.dashboard_goal_reached),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
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
    val spacing = MaterialTheme.spacing
    val totalsByType = uiState.smartVaults
        .groupBy { it.type }
        .mapValues { (_, vaults) -> vaults.sumOf { it.currentBalance } }

    val shortTermTotal = totalsByType[VaultType.SHORT_TERM] ?: 0.0
    val longTermTotal = totalsByType[VaultType.LONG_TERM] ?: 0.0
    val passiveTotal = totalsByType[VaultType.PASSIVE_INVESTMENT] ?: 0.0

    Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    MaterialSymbolIcon(
                        icon = MaterialSymbols.SAVINGS,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = "Savings breakdown",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                ModernMetricCard(
                    title = "Short-term",
                    value = shortTermTotal,
                    icon = MaterialSymbols.SAVINGS,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                ModernMetricCard(
                    title = "Long-term",
                    value = longTermTotal,
                    icon = MaterialSymbols.ACCOUNT_BALANCE,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                ModernMetricCard(
                    title = "Passive growth",
                    value = passiveTotal,
                    icon = MaterialSymbols.TRENDING_UP,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                ModernMetricCard(
                    title = "Monthly avg",
                    value = uiState.analytics.averageMonthlyReserve,
                    subtitle = "Projected in 6mo: ${formatCurrency(uiState.analytics.projectedReserveSixMonths)}",
                    icon = MaterialSymbols.TRENDING_UP,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ModernMetricCard(
    title: String,
    value: Double,
    @androidx.annotation.DrawableRes icon: Int,
    color: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val spacing = MaterialTheme.spacing
    ExpressiveCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp,
        contentPadding = spacing.md
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
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
                MaterialSymbolIcon(
                    icon = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp),
                    size = 20.dp
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
        for (alert in alerts) {
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
            text = "Vaults progress",
            style = MaterialTheme.typography.titleMedium
        )
        val mainVaults = uiState.smartVaults.filter { !it.archived }.take(3)
        for (vault in mainVaults) {
            ExpressiveCard(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                tonalElevation = 1.dp,
                contentPadding = 16.dp
            ) {
                Column {
                    Text(vault.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${formatCurrency(vault.currentBalance)} of ${formatCurrency(vault.targetAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ProgressBar(progress = vault.progressPercent)
                    vault.targetDate?.let { date ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Target date ${date.format(DateTimeFormatter.ISO_DATE)}",
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
    val healthColor = when (healthScore.healthLevel) {
        HealthLevel.EXCELLENT -> MaterialTheme.colorScheme.primary
        HealthLevel.GOOD -> MaterialTheme.colorScheme.secondary
        HealthLevel.FAIR -> MaterialTheme.colorScheme.tertiary
        HealthLevel.NEEDS_WORK -> MaterialTheme.colorScheme.errorContainer
        HealthLevel.CRITICAL -> MaterialTheme.colorScheme.error
    }
    
    // Vertical Tile Layout
    Surface(
        onClick = onClick,
        color = healthColor.copy(alpha = 0.1f),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().height(180.dp) // Fixed height for grid
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                 Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                         MaterialSymbolIcon(
                            icon = MaterialSymbols.HEALTH_AND_SAFETY,
                            contentDescription = null,
                            tint = healthColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = "Health",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = healthColor.copy(alpha = 0.8f)
                )
            }
            
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                 CircularProgressIndicator(
                    progress = { healthScore.overallScore.toFloat() / 100f },
                    modifier = Modifier.size(72.dp),
                    color = healthColor,
                    strokeWidth = 8.dp,
                    trackColor = healthColor.copy(alpha = 0.2f),
                    strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap
                )
                Text(
                    text = "${healthScore.overallScore}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = healthColor
                )
            }
            
             Text(
                text = healthScore.healthLevel.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = healthColor,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun QuickBudgetCard(budgetSummary: BudgetSummary, onClick: () -> Unit) {
    val statusColors = when (budgetSummary.overallHealth) {
        BudgetHealthStatus.HEALTHY -> BudgetStatusColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            indicatorColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
        BudgetHealthStatus.WARNING -> BudgetStatusColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
            indicatorColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
        BudgetHealthStatus.CRITICAL -> BudgetStatusColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
            indicatorColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
        BudgetHealthStatus.OVER_BUDGET -> BudgetStatusColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            indicatorColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    }
    
    Surface(
        onClick = onClick,
        color = statusColors.containerColor,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().height(180.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
             Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                 Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                         MaterialSymbolIcon(
                            icon = MaterialSymbols.PIE_CHART,
                            contentDescription = null,
                            tint = statusColors.indicatorColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = "Budget",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColors.contentColor.copy(alpha = 0.8f)
                )
            }
            
            Column(horizontalAlignment = Alignment.Start) {
                 Text(
                    text = formatPercent(budgetSummary.percentageUsed),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = statusColors.indicatorColor
                )
                Text(
                    text = "Used",
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColors.contentColor.copy(alpha = 0.6f)
                )
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(
                    progress = { budgetSummary.percentageUsed.toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = statusColors.indicatorColor,
                    trackColor = statusColors.indicatorColor.copy(alpha = 0.1f),
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                )
                Text(
                    text = stringResource(R.string.dashboard_remaining_of_budget, formatCurrency(budgetSummary.totalRemaining), formatCurrency(budgetSummary.totalBudget)),
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColors.contentColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun BudgetEmptyCard(onClick: () -> Unit) {
    val spacing = MaterialTheme.spacing
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(spacing.md)) {
            Text(
                text = stringResource(R.string.dashboard_budget_status),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(spacing.xs))
            Text(
                text = stringResource(R.string.dashboard_setup_budgets_description),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(spacing.sm))
            TextButton(onClick = onClick) {
                SingleLineText(stringResource(R.string.dashboard_create_first_budget))
            }
        }
    }
}

@Composable
private fun QuickChallengeItem(challenge: SavingsChallenge, onClick: () -> Unit) {
    val streakColor = MaterialTheme.colorScheme.tertiary
    ExpressiveCard(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        contentPadding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = challenge.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (challenge.targetAmount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.dashboard_challenge_progress, formatPercent(challenge.progressPercent)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            if (challenge.streakDays > 0) {
                Surface(
                    color = streakColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(100), // Pill shape
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${challenge.streakDays}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = streakColor
                        )
                        MaterialSymbolIcon(
                            icon = MaterialSymbols.LOCAL_FIRE_DEPARTMENT,
                            contentDescription = null,
                            tint = streakColor,
                            size = 14.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChallengesEmptyCard(onClick: () -> Unit) {
    ExpressiveCard(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        contentPadding = 20.dp
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    MaterialSymbolIcon(
                        icon = MaterialSymbols.SAVINGS,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        size = 24.dp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.dashboard_savings_challenges),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.dashboard_challenges_description),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            FilledTonalButton(
                onClick = onClick,
                shape = ExpressiveShapes.small
            ) {
                Text(stringResource(R.string.dashboard_browse_challenges), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MainAccountBalanceCard(balance: Double, onClick: () -> Unit = {}) {
    ExpressiveCard(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentPadding = 20.dp,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Main Account",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatCurrency(balance),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            MaterialSymbolIcon(
                icon = MaterialSymbols.ACCOUNT_BALANCE_WALLET,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                size = 32.dp
            )
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

@Composable
private fun VaultType.displayName(): String = when (this) {
    VaultType.SHORT_TERM -> stringResource(R.string.vault_type_short_term)
    VaultType.LONG_TERM -> stringResource(R.string.vault_type_long_term)
    VaultType.PASSIVE_INVESTMENT -> stringResource(R.string.vault_type_passive_investment)
    VaultType.GOAL -> "Goal"
    VaultType.EMERGENCY -> "Emergency"
    VaultType.INVESTMENT -> "Investment"
}

