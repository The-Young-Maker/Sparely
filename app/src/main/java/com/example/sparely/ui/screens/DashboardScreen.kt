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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sparely.R
import com.example.sparely.domain.model.*
import com.example.sparely.ui.components.ExpressiveCard
import com.example.sparely.ui.components.SavingsTrendCard
import com.example.sparely.ui.components.SingleLineText
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
    onNavigateToMainAccount: () -> Unit = {}
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        floatingActionButton = {
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
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = spacing.lg,
                end = spacing.lg,
                top = 0.dp,
                bottom = innerPadding.calculateBottomPadding() + spacing.xl
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.lg)
        ) {
            item {
                DashboardHeroSection(
                    totalBalance = uiState.totalVaultBalance,
                    monthlyIncome = uiState.settings.monthlyIncome,
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
                SmartVaultsCard(
                    vaults = uiState.smartVaults,
                    totalBalance = uiState.totalVaultBalance,
                    pendingCount = uiState.pendingVaultContributions.size,
                    onManageVaults = onManageVaults,
                    onNavigateToTransfers = onNavigateToVaultTransfers
                )
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
                if (uiState.activeChallenges.isNotEmpty()) {
                    QuickChallengesCard(
                        challenges = uiState.activeChallenges,
                        onClick = onNavigateToChallenges
                    )
                } else {
                    ChallengesEmptyCard(onClick = onNavigateToChallenges)
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
    onAddExpense: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onManageVaults: () -> Unit
) {
    val spacing = MaterialTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        ExpressiveCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            contentPadding = spacing.lg,
            tonalElevation = 12.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        Text(
                            text = stringResource(R.string.dashboard_total_saved),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = formatCurrency(totalBalance),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    MaterialSymbolIcon(
                        icon = MaterialSymbols.SAVINGS,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }

                if (monthlyIncome > 0) {
                    val savingsRate = if (totalBalance > 0) (totalBalance / monthlyIncome) * 100 else 0.0
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        MaterialSymbolIcon(
                            icon = MaterialSymbols.TRENDING_UP,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = stringResource(R.string.dashboard_savings_rate, savingsRate),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            FilledTonalButton(
                onClick = onAddExpense,
                modifier = Modifier.weight(1f)
            ) {
                MaterialSymbolIcon(
                    icon = MaterialSymbols.ADD,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(spacing.xs))
                SingleLineText(stringResource(R.string.dashboard_log_purchase))
            }
            OutlinedButton(
                onClick = onManageVaults,
                modifier = Modifier.weight(1f)
            ) {
                MaterialSymbolIcon(
                    icon = MaterialSymbols.ACCOUNT_BALANCE,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(spacing.xs))
                SingleLineText(stringResource(R.string.dashboard_manage))
            }
            TextButton(
                onClick = onNavigateToHistory,
                modifier = Modifier.weight(1f)
            ) {
                MaterialSymbolIcon(
                    icon = MaterialSymbols.HISTORY,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(spacing.xs))
                SingleLineText(stringResource(R.string.dashboard_history))
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
    val spacing = MaterialTheme.spacing

    ExpressiveCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        contentPadding = spacing.lg
    ) {
        Column(
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
                        MaterialSymbolIcon(icon = MaterialSymbols.ACCOUNT_BALANCE,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.dashboard_smart_vaults),
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
                                        modifier = Modifier.padding(horizontal = spacing.xs, vertical = spacing.xxs),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                        Text(
                            text = stringResource(R.string.dashboard_net_saved, formatCurrency(totalBalance)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(onClick = onManageVaults) {
                    SingleLineText(
                        text = stringResource(R.string.dashboard_manage),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            // Vault List
            val previewVaults = vaults.take(3)
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                previewVaults.forEach { vault ->
                    VaultItem(vault = vault, accentYellow = accentYellow, dateFormatter = dateFormatter)
                }
            }

            if (vaults.size > 3) {
                val remainingCount = vaults.size - 3
                Text(
                    text = if (remainingCount == 1)
                        stringResource(R.string.dashboard_more_vaults, remainingCount)
                    else
                        stringResource(R.string.dashboard_more_vaults_plural, remainingCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = spacing.xs)
                )
            }

            if (pendingCount > 0) {
                HorizontalDivider()
                Button(
                    onClick = onNavigateToTransfers,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
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
}

@Composable
private fun VaultItem(
    vault: SmartVault,
    accentYellow: Color,
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
        VaultPriority.HIGH -> accentYellow
        VaultPriority.MEDIUM -> MaterialTheme.colorScheme.primary
        VaultPriority.LOW -> MaterialTheme.colorScheme.secondary
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(spacing.md)) {
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
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.dashboard_smart_saving),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
            )
            Text(
                text = stringResource(R.string.dashboard_saved_vs_target, formatPercent(summary.actualSavingsRate), formatPercent(summary.targetSavingsRate)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = when (summary.allocationMode) {
                    SmartAllocationMode.MANUAL -> stringResource(R.string.dashboard_allocation_manual_mode)
                    SmartAllocationMode.GUIDED -> stringResource(R.string.dashboard_allocation_guided_mode)
                    SmartAllocationMode.AUTOMATIC -> stringResource(R.string.dashboard_allocation_automatic_mode)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.dashboard_recommended_split), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    Text(
                        text = stringResource(R.string.dashboard_allocation_breakdown, formatPercent(summary.recommendedSplit.emergency), formatPercent(summary.recommendedSplit.invest), formatPercent(summary.recommendedSplit.`fun`)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.dashboard_manual_split), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    Text(
                        text = stringResource(R.string.dashboard_allocation_breakdown, formatPercent(summary.manualSplit.emergency), formatPercent(summary.manualSplit.invest), formatPercent(summary.manualSplit.`fun`)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            if (monthlyIncome > 0.0) {
                val monthlyTarget = monthlyIncome * summary.targetSavingsRate
                Text(
                    text = stringResource(R.string.dashboard_aim_for_target, formatCurrency(monthlyTarget)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
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
            previewInsights.forEach { insight ->
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
    Card(
        onClick = onManageRecurring,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.dashboard_upcoming_bills),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (hasRecurring) stringResource(R.string.dashboard_tap_manage_recurring) else stringResource(R.string.dashboard_tap_add_recurring),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onManageRecurring) {
                    val label = if (hasRecurring) {
                        stringResource(R.string.dashboard_manage)
                    } else {
                        stringResource(R.string.dashboard_add)
                    }
                    SingleLineText(label)
                }
            }
            Spacer(modifier = Modifier.height(spacing.sm))
            if (items.isEmpty()) {
                Text(
                    text = if (hasRecurring) stringResource(R.string.dashboard_all_caught_up) else stringResource(R.string.dashboard_log_subscriptions_reminders),
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
                                text = stringResource(R.string.dashboard_due_on, upcoming.dueDate.format(formatter)),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    if (upcoming != items.last()) {
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
    val coverageLabel = String.format("%.0f%%", coverage * 100)
    val savedAmount = (goal.targetAmount - goal.shortfallAmount).coerceAtLeast(0.0)
    val shortfall = goal.shortfallAmount.coerceAtLeast(0.0)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            // Header with icon
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
                        MaterialSymbolIcon(icon = MaterialSymbols.SECURITY,
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
                            text = stringResource(R.string.dashboard_month_goal, goal.targetMonths, formatCurrency(goal.targetAmount)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Animated Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
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
                Column(modifier = Modifier.padding(spacing.md)) {
                    DetailRow(
                        label = stringResource(R.string.dashboard_current_cushion),
                        value = formatCurrency(savedAmount)
                    )
                    Spacer(modifier = Modifier.height(spacing.xs))
                    DetailRow(
                        label = stringResource(R.string.dashboard_shortfall),
                        value = formatCurrency(shortfall),
                        valueColor = if (shortfall > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    if (goal.recommendedMonthlyContribution > 0.0) {
                        Spacer(modifier = Modifier.height(spacing.xs))
                        DetailRow(
                            label = stringResource(R.string.dashboard_monthly_target),
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
                        modifier = Modifier.padding(spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        MaterialSymbolIcon(icon = MaterialSymbols.TRENDING_UP,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.dashboard_goal_reached),
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
                    color = Color(0xFF4CAF50),
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
                    text = stringResource(R.string.dashboard_financial_health),
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
    val spacing = MaterialTheme.spacing

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = statusColors.containerColor,
            contentColor = statusColors.contentColor
        )
    ) {
        Column(modifier = Modifier.padding(spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.dashboard_budget_this_month),
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
            Spacer(modifier = Modifier.height(spacing.xs))
            LinearProgressIndicator(
            progress = { budgetSummary.percentageUsed.toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
            color = statusColors.indicatorColor,
            trackColor = ProgressIndicatorDefaults.linearTrackColor,
            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
            )
            Spacer(modifier = Modifier.height(spacing.xs))
            Text(
                text = stringResource(R.string.dashboard_remaining_of_budget, formatCurrency(budgetSummary.totalRemaining), formatCurrency(budgetSummary.totalBudget)),
                style = MaterialTheme.typography.bodySmall
            )
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
private fun QuickChallengesCard(challenges: List<SavingsChallenge>, onClick: () -> Unit) {
    val streakColor = MaterialTheme.colorScheme.tertiary
    val spacing = MaterialTheme.spacing
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(spacing.md)) {
            Text(
                text = stringResource(R.string.dashboard_active_challenges),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(spacing.sm))
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
                                text = stringResource(R.string.dashboard_challenge_progress, formatPercent(challenge.progressPercent)),
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
                            MaterialSymbolIcon(
                                icon = MaterialSymbols.LOCAL_FIRE_DEPARTMENT,
                                contentDescription = null,
                                tint = streakColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                if (challenge != challenges.last()) {
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
private fun ChallengesEmptyCard(onClick: () -> Unit) {
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
                text = stringResource(R.string.dashboard_savings_challenges),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(spacing.xs))
            Text(
                text = stringResource(R.string.dashboard_challenges_description),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(spacing.sm))
            TextButton(onClick = onClick) {
                SingleLineText(stringResource(R.string.dashboard_browse_challenges))
            }
        }
    }
}

@Composable
private fun MainAccountBalanceCard(balance: Double, onClick: () -> Unit = {}) {
    val spacing = MaterialTheme.spacing
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.xxs)
            ) {
                Text(
                    text = stringResource(R.string.dashboard_main_account),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                )
                Text(
                    text = formatCurrency(balance),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = stringResource(R.string.dashboard_available_liquidity),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                )
            }
            MaterialSymbolIcon(icon = MaterialSymbols.ACCOUNT_BALANCE,
                contentDescription = null,
                modifier = Modifier.size(42.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
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
}

