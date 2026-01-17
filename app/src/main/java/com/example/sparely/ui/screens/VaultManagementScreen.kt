package com.example.sparely.ui.screens

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import com.sparely.app.R
import com.example.sparely.domain.model.*
import com.example.sparely.ui.components.ExpressiveCard
import com.example.sparely.ui.components.SingleLineText
import com.example.sparely.ui.theme.MaterialSymbolIcon
import com.example.sparely.ui.theme.MaterialSymbols
import com.example.sparely.ui.components.*
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.format.TextStyle
import java.util.Locale
import com.example.sparely.ui.utils.toSafeDatePickerMillis
import com.example.sparely.ui.utils.filterCurrencyInput
import com.example.sparely.ui.utils.toSafeDouble
import kotlin.math.abs
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import com.example.sparely.ui.theme.PoppinsFontFamily
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultManagementScreen(
    vaults: List<SmartVault>,
    monthlyIncome: Double = 0.0,
    recentMonthlyExpenses: Double = 0.0,
    savingsRate: Double = 0.0,
    onAddVault: (SmartVault) -> Unit,
    onUpdateVault: (SmartVault) -> Unit,
    onDeleteVault: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    onManualDeposit: ((Long, Double, String?, Boolean) -> Unit)? = null,
    onManualWithdrawal: ((Long, Double, String?, Boolean) -> Unit)? = null,
    onViewHistory: ((Long) -> Unit)? = null,
    onBalanceOverride: ((Long, Double, String?) -> Unit)? = null
) {

    var vaultToEdit by remember { mutableStateOf<SmartVault?>(null) }
    var vaultToDeposit by remember { mutableStateOf<SmartVault?>(null) }
    var vaultToWithdraw by remember { mutableStateOf<SmartVault?>(null) }
    var vaultToDelete by remember { mutableStateOf<SmartVault?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    // Vaults are pre-sorted by urgency in the ViewModel
    val sortedVaults = remember(vaults) {
        vaults.filter { !it.archived }
    }

    // Financial health indicators
    val totalVaultBalance = remember(vaults) { vaults.sumOf { it.currentBalance } }
    val totalTargetAmount = remember(vaults) { vaults.sumOf { it.targetAmount } }
    val overallProgress = remember(totalVaultBalance, totalTargetAmount) {
        if (totalTargetAmount > 0) (totalVaultBalance / totalTargetAmount * 100).toInt() else 0
    }

    // Effect to detect new completions and trigger confetti
    var completedVaultIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showConfetti by remember { mutableStateOf(false) }
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(vaults) {
        val currentlyCompleted = vaults.filter { it.targetAmount > 0 && it.currentBalance >= it.targetAmount }.map { it.id }.toSet()
        
        if (!isInitialized) {
            completedVaultIds = currentlyCompleted
            isInitialized = true
        } else {
            val newCompletions = currentlyCompleted - completedVaultIds
            if (newCompletions.isNotEmpty()) {
                showConfetti = true
            }
            completedVaultIds = currentlyCompleted
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) { // Wrap Scaffold in Box to overlay confetti


    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MaterialSymbolIcon(
                        icon = MaterialSymbols.ADD,
                        contentDescription = stringResource(R.string.vault_management_create_desc),
                        size = 24.dp
                    )
                    Text(stringResource(R.string.vault_management_add), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header removed to use global TopAppBar


            // Overall progress card
            if (vaults.isNotEmpty()) {
                item {
                    OverallProgressCard(
                        totalBalance = totalVaultBalance,
                        totalTarget = totalTargetAmount,
                        overallProgress = overallProgress,
                        vaultCount = vaults.size,
                        monthlyIncome = monthlyIncome,
                        recentExpenses = recentMonthlyExpenses,
                        savingsRate = savingsRate
                    )
                }
            }

            if (sortedVaults.isEmpty()) {
                item {
                    EmptyVaultsCard(onCreateVault = { showCreateDialog = true })
                }
            } else {
                // Group vaults by urgency for better organization
                val urgentVaults = sortedVaults.filter { 
                    it.targetDate?.let { date -> ChronoUnit.DAYS.between(LocalDate.now(), date) <= 90 } == true
                }
                val activeVaults = sortedVaults.filter { 
                    it.monthlyNeed != null && it.startDate?.let { date -> date <= LocalDate.now() } == true
                }
                val plannedVaults = sortedVaults - urgentVaults.toSet() - activeVaults.toSet()

                if (urgentVaults.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.vault_urgent_goals),
                            subtitle = stringResource(R.string.vault_urgent_goals_count, urgentVaults.size),
                            icon = MaterialSymbols.LOCAL_FIRE_DEPARTMENT
                        )
                    }
                    items(urgentVaults, key = { it.id }) { vault ->
                        EnhancedVaultCard(
                            vault = vault,
                            onEdit = { vaultToEdit = vault },
                            onDelete = { vaultToDelete = vault },
                            onDeposit = onManualDeposit?.let { { vaultToDeposit = vault } },
                            onWithdraw = onManualWithdrawal?.let { { vaultToWithdraw = vault } },
                            onViewHistory = onViewHistory?.let { { it(vault.id) } }
                        )
                    }
                }

                if (activeVaults.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.vault_active_flow_goals),
                            subtitle = stringResource(R.string.vault_active_flow_goals_count, activeVaults.size),
                            icon = MaterialSymbols.TRENDING_UP
                        )
                    }
                    items(activeVaults, key = { it.id }) { vault ->
                        EnhancedVaultCard(
                            vault = vault,
                            onEdit = { vaultToEdit = vault },
                            onDelete = { vaultToDelete = vault },
                            onDeposit = onManualDeposit?.let { { vaultToDeposit = vault } },
                            onWithdraw = onManualWithdrawal?.let { { vaultToWithdraw = vault } },
                            onViewHistory = onViewHistory?.let { { it(vault.id) } }
                        )
                    }
                }

                if (plannedVaults.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.vault_planned_goals),
                            subtitle = stringResource(R.string.vault_planned_goals_count, plannedVaults.size),
                            icon = MaterialSymbols.ACCOUNT_BALANCE_WALLET
                        )
                    }
                    items(plannedVaults, key = { it.id }) { vault ->
                        EnhancedVaultCard(
                            vault = vault,
                            onEdit = { vaultToEdit = vault },
                            onDelete = { vaultToDelete = vault },
                            onDeposit = onManualDeposit?.let { { vaultToDeposit = vault } },
                            onWithdraw = onManualWithdrawal?.let { { vaultToWithdraw = vault } },
                            onViewHistory = onViewHistory?.let { { it(vault.id) } }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCreateDialog) {
        SmartVaultEditorDialog(
            vault = null,
            existingVaults = vaults,
            monthlyIncome = monthlyIncome,
            onSave = { newVault ->
                onAddVault(newVault)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    vaultToEdit?.let { vault ->
        SmartVaultEditorDialog(
            vault = vault,
            existingVaults = vaults,
            monthlyIncome = monthlyIncome,
            onSave = { updatedVault ->
                // Detect if balance changed and record it in history
                val balanceDelta = updatedVault.currentBalance - vault.currentBalance
                if (balanceDelta != 0.0 && onBalanceOverride != null) {
                    onBalanceOverride(vault.id, updatedVault.currentBalance, "Balance edited in vault settings")
                }
                onUpdateVault(updatedVault)
                vaultToEdit = null

            },
            onDelete = {
                vaultToDelete = vault
                vaultToEdit = null
            },
            onDismiss = { vaultToEdit = null }
        )
    }

    vaultToDeposit?.let { vault ->
        ManualAdjustmentDialog(
            vaultName = vault.name,
            currentBalance = vault.currentBalance,
            isDeposit = true,
            defaultAffectMainAccount = vault.defaultManualDepositDeductFromMain,
            onConfirm = { amount, reason, adjustMain ->
                onManualDeposit?.invoke(vault.id, amount, reason, adjustMain)
                vaultToDeposit = null
            },
            onDismiss = { vaultToDeposit = null }
        )
    }

    vaultToWithdraw?.let { vault ->
        ManualAdjustmentDialog(
            vaultName = vault.name,
            currentBalance = vault.currentBalance,
            isDeposit = false,
            defaultAffectMainAccount = vault.defaultManualWithdrawalCreditMain,
            onConfirm = { amount, reason, creditMain ->
                onManualWithdrawal?.invoke(vault.id, amount, reason, creditMain)
                vaultToWithdraw = null
            },
            onDismiss = { vaultToWithdraw = null }
        )
    }

    vaultToDelete?.let { vault ->
        DeleteConfirmationDialog(
            vault = vault,
            onConfirm = {
                onDeleteVault(vault.id)
                vaultToDelete = null
            },
            onDismiss = { vaultToDelete = null }
        )
    }

    if (showConfetti) {
        ConfettiExplosion(onComplete = { showConfetti = false })
    }
    }
}

@Composable
private fun OverallProgressCard(
    totalBalance: Double,
    totalTarget: Double,
    overallProgress: Int,
    vaultCount: Int,
    monthlyIncome: Double,
    recentExpenses: Double,
    savingsRate: Double
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.vault_label_total_saved),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${String.format("%.2f", totalBalance)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.vault_label_of_target, String.format("%.2f", totalTarget)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$overallProgress%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.vault_label_active_vaults_count, vaultCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LinearProgressIndicator(
                progress = { (overallProgress / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            )

            // Financial health indicator
            if (monthlyIncome > 0) {
                val displaySavingsRate = (savingsRate * 100).toInt()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HealthIndicator(
                        label = stringResource(R.string.vault_health_savings_rate),
                        value = "$displaySavingsRate%",
                        isHealthy = displaySavingsRate >= 20
                    )
                    HealthIndicator(
                        label = stringResource(R.string.vault_health_monthly_spending),
                        value = "$${String.format("%.0f", recentExpenses)}",
                        isHealthy = recentExpenses < monthlyIncome * 0.5
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthIndicator(
    label: String,
    value: String,
    isHealthy: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MaterialSymbolIcon(
            icon = if (isHealthy) MaterialSymbols.CHECK_CIRCLE else MaterialSymbols.WARNING,
            contentDescription = null,
            size = 16.dp,
            tint = if (isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    @DrawableRes icon: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MaterialSymbolIcon(
            icon = icon,
            contentDescription = null,
            size = 24.dp,
            tint = MaterialTheme.colorScheme.primary
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyVaultsCard(onCreateVault: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MaterialSymbolIcon(
                icon = MaterialSymbols.ACCOUNT_BALANCE_WALLET,
                contentDescription = null,
                size = 64.dp,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Text(
                text = stringResource(R.string.vault_empty_title_short),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.vault_empty_desc_detailed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onCreateVault,
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                MaterialSymbolIcon(
                    icon = MaterialSymbols.ADD,
                    contentDescription = null,
                    size = 18.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.vault_management_add))
            }
        }
    }
}

@Composable
private fun EnhancedVaultCard(
    vault: SmartVault,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onDeposit: (() -> Unit)?,
    onWithdraw: (() -> Unit)?,
    onViewHistory: (() -> Unit)?
) {
    val colorScheme = MaterialTheme.colorScheme
    val progressTarget = if (vault.targetAmount > 0) (vault.currentBalance / vault.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val progress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "Progress Animation"
    )
    
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    val atText = stringResource(R.string.vault_date_time_at)
    val scheduleFormatter = remember(atText) { DateTimeFormatter.ofPattern("MMM d, yyyy '$atText' h:mm a") }
    val locale = remember { Locale.getDefault() }
    val primarySchedule = remember(vault.schedules) {
        vault.schedules
            .filter { it.enabled }.minByOrNull { it.nextRunAt ?: LocalDateTime.MAX }
    }
    
    // Calculate financial insights
    val daysUntilTarget = vault.targetDate?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }
    val isOverdue = daysUntilTarget != null && daysUntilTarget < 0
    val isUrgent = daysUntilTarget != null && daysUntilTarget in 0..90
    val remaining = (vault.targetAmount - vault.currentBalance).coerceAtLeast(0.0)
    
    // Projected Completion Logic - calculates estimated date to reach goal
    val projectedCompletionDate = remember(vault.currentBalance, vault.monthlyNeed, vault.targetAmount, vault.schedules) {
        if (remaining <= 0) null
        else {
            // Calculate effective monthly contribution from all sources
            val monthlyContribution = if (vault.monthlyNeed != null && vault.monthlyNeed > 0) {
                 vault.monthlyNeed
            } else {
                 vault.schedules
                    .filter { it.enabled && it.direction == VaultTransferDirection.MAIN_TO_VAULT }
                    .sumOf { schedule ->
                        val amount = schedule.amount ?: 0.0
                        when (schedule.type) {
                            // Daily: multiply by average days per month
                            VaultScheduleType.DAILY -> amount * 30.44
                            // Monthly: happens once per month
                            VaultScheduleType.DAY_OF_MONTH -> amount
                            // Weekly: approximately 4.33 weeks per month
                            VaultScheduleType.DAY_OF_WEEK -> {
                                val interval = schedule.weekInterval ?: 1
                                amount * (4.33 / interval)
                            }
                            // Quarterly: divide by 3 to get monthly equivalent
                            VaultScheduleType.QUARTERLY -> amount / 3.0
                            // Specific date: one-time contribution, treated as 0 for ongoing projection
                            VaultScheduleType.SPECIFIC_DATE -> 0.0
                        }
                    }
            }
            
            if (monthlyContribution > 0) {
                val monthsNeeded = kotlin.math.ceil(remaining / monthlyContribution).toLong()
                LocalDate.now().plusMonths(monthsNeeded)
            } else null
        }
    }


    // Smart status indicator
    val statusColor = when {
        progress >= 1.0f -> MaterialTheme.colorScheme.primary // Completed
        isOverdue -> MaterialTheme.colorScheme.error // Overdue
        isUrgent -> MaterialTheme.colorScheme.secondary // Urgent
        else -> colorScheme.primary
    }
    
    val statusText = when {
        progress >= 1.0f -> stringResource(R.string.vault_status_goal_reached)
        isOverdue -> stringResource(R.string.vault_status_overdue)
        isUrgent -> stringResource(R.string.vault_status_urgent, daysUntilTarget ?: 0)
        vault.monthlyNeed != null && vault.startDate?.let { it <= LocalDate.now() } == true -> stringResource(R.string.vault_status_active_flow)
        else -> stringResource(R.string.vault_status_in_progress)
    }
    
    val statusIcon = when {
        progress >= 1.0f -> MaterialSymbols.CHECK
        isOverdue -> MaterialSymbols.WARNING
        isUrgent -> MaterialSymbols.WARNING
        else -> null
    }

    val displayIcon = MaterialSymbols.getIconByName(vault.iconName) ?: when (vault.type) {
        VaultType.EMERGENCY -> MaterialSymbols.LOCAL_FIRE_DEPARTMENT
        VaultType.INVESTMENT -> MaterialSymbols.TRENDING_UP
        VaultType.SHORT_TERM -> MaterialSymbols.ATTACH_MONEY
        VaultType.LONG_TERM -> MaterialSymbols.ROCKET_LAUNCH
        else -> MaterialSymbols.ACCOUNT_BALANCE_WALLET
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // Add slight spacing between cards
        shape = RoundedCornerShape(24.dp), // Softer corners
        color = colorScheme.surfaceContainerHigh
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle Gradient Background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                statusColor.copy(alpha = 0.08f),
                                colorScheme.surface.copy(alpha = 0.5f)
                            )
                        )
                    )
            )

            // Watermark Icon
            MaterialSymbolIcon(
                icon = displayIcon,
                contentDescription = null,
                size = 180.dp,
                tint = statusColor.copy(alpha = 0.05f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 40.dp, y = 20.dp)
                    .rotate(-15f)
            )

            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon Circle
                        Surface(
                            shape = CircleShape,
                            color = statusColor.copy(alpha = 0.1f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                MaterialSymbolIcon(
                                    icon = displayIcon,
                                    contentDescription = null,
                                    size = 24.dp,
                                    tint = statusColor
                                )
                            }
                        }

                        Column {
                            Text(
                                text = vault.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                statusIcon?.let { icon ->
                                    MaterialSymbolIcon(
                                        icon = icon,
                                        contentDescription = null,
                                        size = 16.dp,
                                        tint = statusColor
                                    )
                                }
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Action Buttons (simplified)
                    Row {
                        onEdit?.let {
                            IconButton(onClick = it) {
                                MaterialSymbolIcon(
                                    icon = MaterialSymbols.EDIT,
                                    contentDescription = stringResource(R.string.vault_management_edit_desc),
                                    size = 20.dp,
                                    tint = colorScheme.onSurfaceVariant
                                )
                            }
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
                                text = stringResource(R.string.vault_label_current_balance),
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$${String.format("%.2f", vault.currentBalance)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = colorScheme.onSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                             if (remaining > 0) {
                                Text(
                                    text = stringResource(R.string.vault_label_target_with_amount, String.format("%.0f", vault.targetAmount)),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${String.format("%.1f", progress * 100)}%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = statusColor,
                        trackColor = statusColor.copy(alpha = 0.2f),
                        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                    )
                    
                    // Forecast & Remaining
                    Row(
                         modifier = Modifier.fillMaxWidth(),
                         horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                         if (remaining > 0) {
                             Text(
                                 text = stringResource(R.string.vault_label_remaining_to_go, String.format("%.2f", remaining)),
                                 style = MaterialTheme.typography.bodySmall,
                                 color = colorScheme.onSurfaceVariant,
                                 fontWeight = FontWeight.Medium
                             )
                         }
                         
                         if(projectedCompletionDate != null && remaining > 0) {
                             Text(
                                 text = stringResource(R.string.vault_label_on_track, projectedCompletionDate.format(dateFormatter)),
                                 style = MaterialTheme.typography.bodySmall,
                                 color = colorScheme.tertiary,
                                 fontWeight = FontWeight.Bold
                             )
                         } else if (vault.targetDate != null && remaining > 0) {
                             Text(
                                 text = stringResource(R.string.vault_label_due_date, vault.targetDate.format(dateFormatter)),
                                 style = MaterialTheme.typography.bodySmall,
                                 color = colorScheme.onSurfaceVariant
                             )
                         }
                    }
                }

                // Quick Actions Row
                if (onDeposit != null || onWithdraw != null || onViewHistory != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        onDeposit?.let {
                            VaultActionButton(
                                modifier = Modifier.weight(1f),
                                label = stringResource(R.string.vault_action_add_short),
                                icon = MaterialSymbols.ADD,
                                tint = MaterialTheme.colorScheme.primary,
                                onClick = it
                            )
                        }
                        onWithdraw?.let {
                            VaultActionButton(
                                modifier = Modifier.weight(1f),
                                label = stringResource(R.string.vault_action_withdraw_short),
                                icon = MaterialSymbols.REMOVE,
                                tint = MaterialTheme.colorScheme.error,
                                onClick = it
                            )
                        }
                        onViewHistory?.let {
                             // Assuming we might want a smaller button or just an icon for history
                            VaultActionButton(
                                modifier = Modifier.weight(1f),
                                label = stringResource(R.string.vault_action_history_short),
                                icon = MaterialSymbols.HISTORY,
                                tint = MaterialTheme.colorScheme.secondary,
                                onClick = it
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultActionButton(
    modifier: Modifier = Modifier,
    label: String,
    @DrawableRes icon: Int,
    tint: Color,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 32.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = tint.copy(alpha = 0.12f),
            contentColor = tint
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MaterialSymbolIcon(
                icon = icon,
                contentDescription = null,
                size = 16.dp,
                tint = tint
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun VaultBadgeChip(
    modifier: Modifier = Modifier,
    label: String,
    @DrawableRes icon: Int,
    tint: Color
) {
    Surface(
        modifier = modifier,
        color = tint.copy(alpha = 0.12f),
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MaterialSymbolIcon(
                icon = icon,
                contentDescription = null,
                size = 14.dp,
                tint = tint
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun ManualAdjustmentDialog(
    vaultName: String,
    currentBalance: Double,
    isDeposit: Boolean,
    defaultAffectMainAccount: Boolean,
    onConfirm: (Double, String?, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var affectMainAccount by remember(defaultAffectMainAccount) { mutableStateOf(defaultAffectMainAccount) }

    val title = if (isDeposit) stringResource(R.string.vault_action_add) else stringResource(R.string.vault_action_withdraw)
    val icon = if (isDeposit) MaterialSymbols.ADD else MaterialSymbols.REMOVE
    val affectMainLabel = if (isDeposit) stringResource(R.string.vault_affect_main_deduct) else stringResource(R.string.vault_affect_main_credit)
    val affectMainHelper = if (isDeposit) {
        stringResource(R.string.vault_affect_main_deduct_helper)
    } else {
        stringResource(R.string.vault_affect_main_credit_helper)
    }
    
    val defaultDepositReason = stringResource(R.string.vault_manual_deposit_default)
    val defaultWithdrawReason = stringResource(R.string.vault_manual_withdrawal_default)

    // Smart suggestions based on context
    val suggestedAmounts = remember(currentBalance, isDeposit) {
        if (isDeposit) {
            listOf(50.0, 100.0, 250.0, 500.0)
        } else {
            listOf(
                currentBalance * 0.25,
                currentBalance * 0.5,
                currentBalance * 0.75,
                currentBalance
            ).filter { it > 0 }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        ExpressiveCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MaterialSymbolIcon(
                        icon = icon,
                        contentDescription = null,
                        size = 32.dp,
                        tint = if (isDeposit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = vaultName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Affect main account toggle
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = affectMainLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = affectMainHelper,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = affectMainAccount,
                            onCheckedChange = { affectMainAccount = it }
                        )
                    }
                }

                // Current balance display
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.vault_current_balance_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format("%.2f", currentBalance),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                SparelyTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filterCurrencyInput() },
                    label = { Text(stringResource(R.string.vault_amount_label)) },
                    prefix = { Text(stringResource(R.string.currency_prefix)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = !isDeposit && amountText.toSafeDouble()?.let { it > currentBalance } == true,
                    supportingText = {
                        if (!isDeposit && amountText.toSafeDouble()?.let { it > currentBalance } == true) {
                            Text(
                                text = stringResource(R.string.vault_error_insufficient_balance),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )

                // Quick amount suggestions
                if (suggestedAmounts.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.vault_quick_amounts),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (amount in suggestedAmounts.take(4)) {
                                SparelyChip(
                                    selected = amountText.toSafeDouble() == amount,
                                    onClick = { amountText = String.format("%.0f", amount) },
                                    label = {
                                        SingleLineText(
                                            text = String.format("%.0f", amount),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                SparelyTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(stringResource(R.string.vault_note_optional)) },
                    placeholder = { Text(stringResource(R.string.vault_note_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    // minLines not supported in simple SparelyTextField yet, but singleLine=false defaults.
                    // Assuming SparelyTextField handles multiline if singleLine is false (default is true).
                    // I will remove min/maxLines for now as my SparelyTextField definition was simple.
                    // Wait, SparelyTextField definition hardcodes singleLine=true default but allows override.
                    // But it passes singleLine to internal TextField.
                    // However, internal TextField in SparelyUiComponents uses singleLine=singleLine.
                    // And it does NOT expose minLines/maxLines.
                    // To avoid regression, I'll pass singleLine=false.
                    singleLine = false
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        SparelyTonalButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }

                    if (isDeposit) {
                        SparelyButton(
                            onClick = {
                                val amount = amountText.toSafeDouble()
                                if (amount != null && amount > 0) {
                                    val finalReason = reason.trim().ifBlank { defaultDepositReason }
                                    onConfirm(amount, finalReason, affectMainAccount)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = amountText.toSafeDouble()?.let { it > 0 } == true
                        ) {
                            Text(stringResource(R.string.vault_management_add))
                        }
                    } else {
                        // For withdraw, we want a red button, so we might need a custom Sparely button or just configure SparelyButton
                        // Since SparelyButton uses primary color, we can't easily change it to error color without adding a param.
                        // Let's use Button with the shape/style of SparelyButton manually or add a SparelyErrorButton.
                        // For now, I'll use Button but style it to match SparelyButton (height 48, radius 16, bold text).
                        Button(
                            onClick = {
                                val amount = amountText.toSafeDouble()
                                if (amount != null && amount > 0 && amount <= currentBalance) {
                                    val finalReason = reason.trim().ifBlank { defaultWithdrawReason }
                                    onConfirm(amount, finalReason, affectMainAccount)
                                }
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            enabled = amountText.toSafeDouble()?.let { 
                                it > 0 && it <= currentBalance
                            } == true,
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                             ProvideTextStyle(
                                value = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold // Bolder text
                                )
                            ) {
                                Text(stringResource(R.string.vault_withdraw))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    vault: SmartVault,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            MaterialSymbolIcon(
                icon = MaterialSymbols.WARNING,
                contentDescription = null,
                size = 32.dp,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = stringResource(R.string.vault_delete_confirm_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.vault_delete_confirm_message, vault.name),
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.vault_important),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        
                        if (vault.currentBalance > 0) {
                            Text(
                                text = stringResource(R.string.vault_delete_balance_return, vault.currentBalance),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        
                        Text(
                            text = stringResource(R.string.vault_undone_warning),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmartVaultEditorDialog(
    vault: SmartVault?,
    existingVaults: List<SmartVault>,
    monthlyIncome: Double,
    onSave: (SmartVault) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val existingSchedule = remember(vault?.id) { vault?.schedules?.firstOrNull() }

    fun scheduleToFrequency(schedule: VaultSchedule?): AutoDepositFrequency {
        if (schedule == null) return AutoDepositFrequency.MONTHLY
        return when (schedule.type) {
            VaultScheduleType.DAY_OF_WEEK -> {
                val interval = schedule.weekInterval ?: 1
                if (interval >= 2) AutoDepositFrequency.BIWEEKLY else AutoDepositFrequency.WEEKLY
            }
            else -> AutoDepositFrequency.MONTHLY
        }
    }

    var name by remember { mutableStateOf(vault?.name ?: "") }
    var iconName by remember { mutableStateOf(vault?.iconName) }
    var targetAmount by remember { mutableStateOf(vault?.targetAmount?.toString() ?: "") }
    var currentBalance by remember { mutableStateOf(vault?.currentBalance?.toString() ?: "0") }
    var monthlyNeed by remember { mutableStateOf(vault?.monthlyNeed?.toString() ?: "") }
    var isFlowGoal by remember { mutableStateOf(vault?.monthlyNeed != null) }
    var startDate by remember { mutableStateOf(vault?.startDate) }
    var endDate by remember { mutableStateOf(vault?.endDate) }
    var priority by remember { mutableStateOf(vault?.priority ?: VaultPriority.MEDIUM) }
    var type by remember { mutableStateOf(vault?.type ?: VaultType.GOAL) }
    var targetDate by remember { mutableStateOf(vault?.targetDate) }
    var accountNotes by remember { mutableStateOf(vault?.accountNotes ?: "") }
    var priorityWeight by remember { mutableStateOf(vault?.priorityWeight?.toString() ?: "1.0") }
    var excludedFromAutoAllocation by remember { mutableStateOf(!(vault?.allowAutoIncome ?: true)) }
    var defaultManualDepositDeductFromMain by remember(vault?.id) { mutableStateOf(vault?.defaultManualDepositDeductFromMain ?: true) }
    var defaultManualWithdrawalCreditMain by remember(vault?.id) { mutableStateOf(vault?.defaultManualWithdrawalCreditMain ?: true) }

    // Auto-schedule editing (supports single primary schedule for now)
    var autoDepositEnabled by remember(vault?.id) { mutableStateOf(existingSchedule != null) }
    var autoDepositAmount by remember(vault?.id) {
        mutableStateOf(
            existingSchedule?.amount
                ?.takeIf { it > 0.0 }
                ?.let { it.toString() }
                ?: ""
        )
    }
    var autoDepositFrequency by remember(vault?.id) { mutableStateOf(scheduleToFrequency(existingSchedule)) }
    var autoDepositNextRunDate by remember(vault?.id) {
        mutableStateOf(
            existingSchedule?.nextRunAt?.toLocalDate()
                ?: existingSchedule?.lastRunAt?.toLocalDate()
                ?: LocalDate.now().plusDays(1)
        )
    }
    var autoDepositNextRunTime by remember(vault?.id) {
        mutableStateOf(
            existingSchedule?.nextRunAt?.toLocalTime()
                ?: LocalTime.of(9, 0)
        )
    }
    var autoDepositOnlyIfBalanceAvailable by remember(vault?.id) { mutableStateOf(existingSchedule?.onlyIfBalanceAvailable ?: true) }
    var autoDepositNotifyBefore by remember(vault?.id) { mutableStateOf(existingSchedule?.notifyBefore ?: false) }
    var autoDepositNotifyAfter by remember(vault?.id) { mutableStateOf(existingSchedule?.notifyAfter ?: true) }
    var autoDepositNotifyOnFailure by remember(vault?.id) { mutableStateOf(existingSchedule?.notifyOnFailure ?: true) }
    var showScheduleDatePicker by remember { mutableStateOf(false) }
    var autoDepositTimeMenuExpanded by remember { mutableStateOf(false) }

    val timeOptions = remember(existingSchedule?.nextRunAt) {
        val defaults = listOf(6, 8, 9, 12, 15, 18, 21).map { LocalTime.of(it, 0) }.toMutableList()
        val existingTime = existingSchedule?.nextRunAt?.toLocalTime()
        if (existingTime != null && defaults.none { it == existingTime }) {
            defaults.add(existingTime)
        }
        defaults.sorted()
    }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

    var priorityMenuExpanded by remember { mutableStateOf(false) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var showTargetDatePicker by remember { mutableStateOf(false) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    
    // Smart validation with helpful feedback
    val context = LocalContext.current
    val validationResult = remember(
        name,
        targetAmount,
        monthlyNeed,
        isFlowGoal,
        targetDate,
        endDate,
        autoDepositEnabled,
        autoDepositAmount,
        autoDepositNextRunDate,
        autoDepositNextRunTime
    ) {
        val nextRunCandidate = LocalDateTime.of(autoDepositNextRunDate, autoDepositNextRunTime)
        when {
            name.isBlank() -> ValidationResult(false, context.getString(R.string.vault_error_name_required))
            isFlowGoal && monthlyNeed.toSafeDouble()?.let { it <= 0 } != false -> 
                ValidationResult(false, context.getString(R.string.vault_error_monthly_need_positive))
            !isFlowGoal && targetAmount.toSafeDouble()?.let { it <= 0 } != false -> 
                ValidationResult(false, context.getString(R.string.vault_error_target_amount_positive))
            autoDepositEnabled && autoDepositAmount.toSafeDouble()?.let { it <= 0 } != false ->
                ValidationResult(false, context.getString(R.string.vault_error_auto_deposit_positive))
            autoDepositEnabled && !nextRunCandidate.isAfter(LocalDateTime.now()) ->
                ValidationResult(false, context.getString(R.string.vault_error_schedule_future))
            else -> ValidationResult(true, "")
        }
    }

    // Smart suggestions based on context
    val suggestedPriority = remember(type, isFlowGoal, targetDate) {
        when {
            type == VaultType.EMERGENCY -> VaultPriority.HIGH
            targetDate?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) <= 90 } == true -> VaultPriority.HIGH
            isFlowGoal -> VaultPriority.MEDIUM
            else -> VaultPriority.LOW
        }
    }

    // Financial insights
    val totalExistingAllocation = remember(existingVaults, monthlyIncome) {
        if (monthlyIncome > 0) {
            existingVaults.filter { it.id != vault?.id }
                .sumOf { it.monthlyNeed ?: 0.0 } / monthlyIncome * 100
        } else 0.0
    }

    if (showTargetDatePicker) {
        val initialMillis = targetDate.toSafeDatePickerMillis()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showTargetDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selected = datePickerState.selectedDateMillis
                    targetDate = selected?.let { 
                        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() 
                    }
                    if (isFlowGoal) endDate = targetDate
                    showTargetDatePicker = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showTargetDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showStartPicker) {
        val initialStartMillis = startDate.toSafeDatePickerMillis()
        val startPickerState = rememberDatePickerState(initialSelectedDateMillis = initialStartMillis)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selected = startPickerState.selectedDateMillis
                    startDate = selected?.let { 
                        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() 
                    }
                    showStartPicker = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) {
            DatePicker(state = startPickerState)
        }
    }

    if (showEndPicker) {
        val initialEndMillis = endDate.toSafeDatePickerMillis()
        val endPickerState = rememberDatePickerState(initialSelectedDateMillis = initialEndMillis)
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selected = endPickerState.selectedDateMillis
                    endDate = selected?.let { 
                        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() 
                    }
                    if (isFlowGoal) targetDate = endDate
                    showEndPicker = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) {
            DatePicker(state = endPickerState)
        }
    }

    if (showScheduleDatePicker) {
        val initialScheduleMillis = autoDepositNextRunDate.toSafeDatePickerMillis() ?: System.currentTimeMillis()
        val schedulePickerState = rememberDatePickerState(initialSelectedDateMillis = initialScheduleMillis)
        DatePickerDialog(
            onDismissRequest = { showScheduleDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selected = schedulePickerState.selectedDateMillis
                    autoDepositNextRunDate = selected?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    } ?: autoDepositNextRunDate
                    showScheduleDatePicker = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showScheduleDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) {
            DatePicker(state = schedulePickerState)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard(
            modifier = Modifier
                .widthIn(min = 360.dp, max = 840.dp)
                .heightIn(max = 780.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (vault == null) stringResource(R.string.vault_editor_add_title) else stringResource(R.string.vault_editor_edit_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.vault_allocation_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Goal type selector (prominent position)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.vault_goal_type),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GoalTypeCard(
                                modifier = Modifier.weight(1f),
                                title = stringResource(R.string.vault_fixed_goal),
                                description = stringResource(R.string.vault_fixed_goal_desc),
                                icon = MaterialSymbols.FLAG,
                                isSelected = !isFlowGoal,
                                onClick = {
                                    if (isFlowGoal) {
                                        targetDate = endDate
                                        endDate = null
                                        monthlyNeed = ""
                                    }
                                    isFlowGoal = false
                                }
                            )
                            GoalTypeCard(
                                modifier = Modifier.weight(1f),
                                title = stringResource(R.string.vault_flow_goal),
                                description = stringResource(R.string.vault_flow_goal_desc),
                                icon = MaterialSymbols.REFRESH,
                                isSelected = isFlowGoal,
                                onClick = {
                                    if (!isFlowGoal) {
                                        endDate = targetDate
                                        targetDate = null
                                    }
                                    isFlowGoal = true
                                }
                            )
                        }
                    }
                }

                item {
                    val icons = listOf(
                        MaterialSymbols.ACCOUNT_BALANCE_WALLET,
                        MaterialSymbols.SAVINGS,
                        MaterialSymbols.DIRECTIONS_CAR,
                        MaterialSymbols.HOME,
                        MaterialSymbols.FLIGHT,
                        MaterialSymbols.SCHOOL,
                        MaterialSymbols.SHOPPING_BAG,
                        MaterialSymbols.PETS,
                        MaterialSymbols.RESTAURANT,
                        MaterialSymbols.COMPUTER,
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.vault_management_vault_icon_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            Modifier.fillMaxWidth().height(56.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (iconRes in icons) {
                                val iconStableName = MaterialSymbols.getNameByIcon(iconRes)
                                val isSelected = (iconName == null && iconRes == MaterialSymbols.ACCOUNT_BALANCE_WALLET) || (iconName == iconStableName)
                                Surface(
                                    modifier = Modifier.size(40.dp).clickable { iconName = iconStableName },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        MaterialSymbolIcon(
                                            icon = iconRes,
                                            contentDescription = null,
                                            size = 24.dp,
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    SparelyTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.vault_name_label)) },
                        placeholder = { Text(stringResource(R.string.vault_name_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            val displayIcon = MaterialSymbols.getIconByName(iconName) ?: MaterialSymbols.ACCOUNT_BALANCE_WALLET
                            MaterialSymbolIcon(
                                icon = displayIcon,
                                contentDescription = null,
                                size = 20.dp
                            )
                        }
                    )
                }

                // Different fields based on goal type
                if (isFlowGoal) {
                    item {
                        SparelyTextField(
                            value = monthlyNeed,
                            onValueChange = { monthlyNeed = it.filterCurrencyInput() },
                            label = { Text(stringResource(R.string.vault_monthly_need)) },
                            prefix = { Text(stringResource(R.string.currency_prefix)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            supportingText = {
                                val amount = monthlyNeed.toSafeDouble()
                                if (amount != null && monthlyIncome > 0) {
                                    val percent = (amount / monthlyIncome * 100).toInt()
                                    Text(stringResource(R.string.vault_income_percent, percent))
                                }
                            }
                        )
                    }

                    item {
                        FilledTonalButton(
                            onClick = { showStartPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MaterialSymbolIcon(
                                icon = MaterialSymbols.CALENDAR_MONTH,
                                contentDescription = null,
                                size = 18.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = startDate?.format(dateFormatter) ?: stringResource(R.string.vault_set_start_date)
                            )
                        }
                        if (startDate != null) {
                            TextButton(
                                onClick = { startDate = null },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.vault_clear_start_date))
                            }
                        }
                    }

                    item {
                        FilledTonalButton(
                            onClick = { showEndPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MaterialSymbolIcon(
                                icon = MaterialSymbols.CALENDAR_MONTH,
                                contentDescription = null,
                                size = 18.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = endDate?.format(dateFormatter) ?: stringResource(R.string.vault_set_end_date)
                            )
                        }
                        if (endDate != null) {
                            TextButton(
                                onClick = { endDate = null },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.vault_clear_end_date))
                            }
                        }
                    }

                    // Allow editing current balance for existing flow vaults
                    if (vault != null) {
                        item {
                            SparelyTextField(
                                value = currentBalance,
                                onValueChange = { currentBalance = it.filterCurrencyInput() },
                                label = { Text(stringResource(R.string.vault_current_balance_label)) },
                                prefix = { Text(stringResource(R.string.currency_prefix)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                supportingText = {
                                    Text(
                                        text = stringResource(R.string.vault_balance_edit_note),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                    }

                } else {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SparelyTextField(
                                value = targetAmount,
                                onValueChange = { targetAmount = it.filterCurrencyInput() },
                                label = { Text(stringResource(R.string.vault_target_amount_label)) },
                                prefix = { Text(stringResource(R.string.currency_prefix)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
 
                            if (vault != null) {
                                SparelyTextField(
                                    value = currentBalance,
                                    onValueChange = { currentBalance = it.filterCurrencyInput() },
                                    label = { Text(stringResource(R.string.vault_current_balance_label)) },
                                    prefix = { Text(stringResource(R.string.currency_prefix)) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                )
                            }
                        }
                    }

                    item {
                        FilledTonalButton(
                            onClick = { showTargetDatePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MaterialSymbolIcon(
                                icon = MaterialSymbols.CALENDAR_MONTH,
                                contentDescription = null,
                                size = 18.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = targetDate?.format(dateFormatter) ?: stringResource(R.string.vault_set_deadline)
                            )
                        }
                        if (targetDate != null) {
                            val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), targetDate)
                            val monthsUntil = ChronoUnit.MONTHS.between(LocalDate.now(), targetDate)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.vault_days_until_deadline, daysUntil, monthsUntil),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(onClick = { targetDate = null }) {
                                    Text(stringResource(R.string.action_clear))
                                }
                            }
                        }
                    }
                }

                item {
                    ExposedDropdownMenuBox(
                        expanded = typeMenuExpanded,
                        onExpandedChange = { typeMenuExpanded = it }
                    ) {
                        SparelyTextField(
                            value = type.displayName(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.vault_type_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                        )
                        ExposedDropdownMenu(
                            expanded = typeMenuExpanded,
                            onDismissRequest = { typeMenuExpanded = false }
                        ) {
                            for (vaultType in listOf(VaultType.GOAL, VaultType.EMERGENCY, VaultType.INVESTMENT, VaultType.SHORT_TERM, VaultType.LONG_TERM)) {
                                DropdownMenuItem(
                                    text = { Text(vaultType.displayName()) },
                                    onClick = {
                                        type = vaultType
                                        typeMenuExpanded = false
                                    },
                                    leadingIcon = {
                                        MaterialSymbolIcon(
                                            icon = when (vaultType) {
                                                VaultType.EMERGENCY -> MaterialSymbols.LOCAL_FIRE_DEPARTMENT
                                                VaultType.INVESTMENT -> MaterialSymbols.TRENDING_UP
                                                VaultType.SHORT_TERM -> MaterialSymbols.ATTACH_MONEY
                                                VaultType.LONG_TERM -> MaterialSymbols.ROCKET_LAUNCH
                                                else -> MaterialSymbols.ACCOUNT_BALANCE
                                            },
                                            contentDescription = null,
                                            size = 20.dp
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    ExposedDropdownMenuBox(
                        expanded = priorityMenuExpanded,
                        onExpandedChange = { priorityMenuExpanded = it }
                    ) {
                        SparelyTextField(
                            value = priority.displayName(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.vault_priority_label)) },
                            supportingText = {
                                if (priority != suggestedPriority) {
                                    Text(stringResource(R.string.vault_suggested_priority, suggestedPriority.displayName()))
                                }
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            onClick = { priorityMenuExpanded = true }
                        )
                        ExposedDropdownMenu(
                            expanded = priorityMenuExpanded,
                            onDismissRequest = { priorityMenuExpanded = false }
                        ) {
                            for (vaultPriority in VaultPriority.entries) {
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(vaultPriority.displayName())
                                            if (vaultPriority == suggestedPriority) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = stringResource(R.string.vault_suggested_label),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onClick = {
                                        priority = vaultPriority
                                        priorityMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Financial insights
                if (monthlyIncome > 0 && totalExistingAllocation > 0) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.vault_budget_insight_title),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.vault_budget_insight_desc, totalExistingAllocation),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                item {
                    SparelyTextField(
                        value = accountNotes,
                        onValueChange = { accountNotes = it },
                        label = { Text(stringResource(R.string.vault_account_notes_label)) },
                        placeholder = { Text(stringResource(R.string.vault_notes_placeholder_detailed)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.vault_manual_transfer_defaults),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.vault_manual_transfer_defaults_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.vault_deduct_deposits), style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = stringResource(R.string.vault_deduct_deposits_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = defaultManualDepositDeductFromMain,
                                onCheckedChange = { defaultManualDepositDeductFromMain = it }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.vault_credit_withdrawals), style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = stringResource(R.string.vault_credit_withdrawals_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = defaultManualWithdrawalCreditMain,
                                onCheckedChange = { defaultManualWithdrawalCreditMain = it }
                            )
                        }
                    }
                }

                // Exclude from automatic allocation toggle
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(stringResource(R.string.vault_exclude_auto_funding), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = stringResource(R.string.vault_exclude_auto_funding_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = excludedFromAutoAllocation, onCheckedChange = { excludedFromAutoAllocation = it })
                    }
                }

                // Validation feedback
                if (!validationResult.isValid) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MaterialSymbolIcon(
                                    icon = MaterialSymbols.WARNING,
                                    contentDescription = null,
                                    size = 20.dp,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = validationResult.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Auto-deposit editor (small, focused)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(stringResource(R.string.vault_auto_deposit_schedule), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = stringResource(R.string.vault_auto_deposit_schedule_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = autoDepositEnabled, onCheckedChange = { autoDepositEnabled = it })
                        }

                        if (autoDepositEnabled) {
                            SparelyTextField(
                                value = autoDepositAmount,
                                onValueChange = { autoDepositAmount = it.filterCurrencyInput() },
                                label = { Text(stringResource(R.string.vault_amount_label)) },
                                prefix = { Text(stringResource(R.string.currency_prefix)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // Frequency chips - using FlowRow for wrapping
                            androidx.compose.foundation.layout.FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val frequencies = listOf(
                                    AutoDepositFrequency.DAILY,
                                    AutoDepositFrequency.WEEKLY,
                                    AutoDepositFrequency.BIWEEKLY,
                                    AutoDepositFrequency.MONTHLY,
                                    AutoDepositFrequency.QUARTERLY
                                )
                                frequencies.forEach { freq ->
                                    SparelyChip(
                                        selected = autoDepositFrequency == freq,
                                        onClick = { autoDepositFrequency = freq },
                                        label = { Text(freq.displayName()) }
                                    )
                                }
                            }


                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalButton(
                                    onClick = { showScheduleDatePicker = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    MaterialSymbolIcon(
                                        icon = MaterialSymbols.CALENDAR_MONTH,
                                        contentDescription = null,
                                        size = 18.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(autoDepositNextRunDate.format(dateFormatter))
                                }

                                ExposedDropdownMenuBox(
                                    expanded = autoDepositTimeMenuExpanded,
                                    onExpandedChange = { autoDepositTimeMenuExpanded = it },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    SparelyTextField(
                                        value = autoDepositNextRunTime.format(timeFormatter),
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(stringResource(R.string.vault_run_time)) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = autoDepositTimeMenuExpanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = autoDepositTimeMenuExpanded,
                                        onDismissRequest = { autoDepositTimeMenuExpanded = false }
                                    ) {
                                        for (option in timeOptions) {
                                            DropdownMenuItem(
                                                text = { Text(option.format(timeFormatter)) },
                                                onClick = {
                                                    autoDepositNextRunTime = option
                                                    autoDepositTimeMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = stringResource(R.string.vault_next_run, autoDepositNextRunDate.format(dateFormatter), autoDepositNextRunTime.format(timeFormatter)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.vault_protect_main_balance), style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = stringResource(R.string.vault_protect_main_balance_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = autoDepositOnlyIfBalanceAvailable,
                                    onCheckedChange = { autoDepositOnlyIfBalanceAvailable = it }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.vault_reminder_before), style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = stringResource(R.string.vault_reminder_before_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = autoDepositNotifyBefore,
                                    onCheckedChange = { autoDepositNotifyBefore = it }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.vault_confirmation_after), style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = stringResource(R.string.vault_heads_up_moves),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = autoDepositNotifyAfter,
                                    onCheckedChange = { autoDepositNotifyAfter = it }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.vault_alert_failure), style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = stringResource(R.string.vault_ping_skipped),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = autoDepositNotifyOnFailure,
                                    onCheckedChange = { autoDepositNotifyOnFailure = it }
                                )
                            }
                        }
                    }
                }
                item {
                    HorizontalDivider()
                }

                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Delete button (only for existing vaults)
                        if (vault != null && onDelete != null) {
                            SparelyTextButton(
                                onClick = onDelete,
                                modifier = Modifier.fillMaxWidth(),
                                contentColor = MaterialTheme.colorScheme.error,
                                icon = {
                                    MaterialSymbolIcon(
                                        icon = MaterialSymbols.DELETE,
                                        contentDescription = null,
                                        size = 18.dp,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            ) {
                                Text(stringResource(R.string.vault_delete_vault))
                            }
                        }
                        
                        // Button row - using FlowRow for narrow screen support
                        androidx.compose.foundation.layout.FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            maxItemsInEachRow = 2
                        ) {
                            SparelyTonalButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f).widthIn(min = 100.dp)
                            ) {
                                Text(stringResource(R.string.action_cancel))
                            }


                            SparelyButton(
                                onClick = {
                                    val balance = if (vault != null) currentBalance.toSafeDouble() ?: 0.0 else 0.0
                                    val monthly = monthlyNeed.toSafeDouble()
                                    // For flow goals, compute a sensible target: monthly need * number of months
                                    // Determine the months span (inclusive). Use startDate if available, otherwise today.
                                    val target = if (isFlowGoal && monthly != null) {
                                        val start = (startDate ?: LocalDate.now()).withDayOfMonth(1)
                                        // If endDate provided, use it; otherwise default to a 12-month window starting at start
                                        val end = (endDate ?: start.plusMonths(11)).withDayOfMonth(1)
                                        var months = ChronoUnit.MONTHS.between(start, end).toInt() + 1
                                        months = max(1, months)
                                        monthly * months
                                    } else {
                                        (targetAmount.toSafeDouble() ?: 0.0)
                                    }
                                    
                                    // Smart priority weight calculation
                                    val weight = when (priority) {
                                        VaultPriority.CRITICAL -> 4.0
                                        VaultPriority.HIGH -> 3.0
                                        VaultPriority.MEDIUM -> 2.0
                                        VaultPriority.LOW -> 1.0
                                    }

                                    val existingSchedulesList = vault?.schedules ?: emptyList()
                                    val preservedSchedules = if (existingSchedule != null) {
                                        existingSchedulesList.filter { it.id != existingSchedule.id }
                                    } else {
                                        existingSchedulesList
                                    }

                                    val scheduleAmount = autoDepositAmount.toSafeDouble()
                                    val scheduleType = when (autoDepositFrequency) {
                                        AutoDepositFrequency.DAILY -> VaultScheduleType.DAILY
                                        AutoDepositFrequency.WEEKLY, AutoDepositFrequency.BIWEEKLY -> VaultScheduleType.DAY_OF_WEEK
                                        AutoDepositFrequency.MONTHLY -> VaultScheduleType.DAY_OF_MONTH
                                        AutoDepositFrequency.QUARTERLY -> VaultScheduleType.QUARTERLY
                                    }
                                    val newSchedule = if (autoDepositEnabled) {
                                        val nextRunAt = LocalDateTime.of(autoDepositNextRunDate, autoDepositNextRunTime)
                                        VaultSchedule(
                                            id = existingSchedule?.id ?: 0L,
                                            vaultId = vault?.id ?: 0L,
                                            type = scheduleType,
                                            amount = scheduleAmount?.takeIf { it > 0.0 },
                                            percentage = null,
                                            direction = VaultTransferDirection.MAIN_TO_VAULT,
                                            dayOfMonth = if (scheduleType == VaultScheduleType.DAY_OF_MONTH) autoDepositNextRunDate.dayOfMonth else null,
                                            dayOfWeek = if (scheduleType == VaultScheduleType.DAY_OF_WEEK) autoDepositNextRunDate.dayOfWeek.value else null,
                                            weekInterval = when (autoDepositFrequency) {
                                                AutoDepositFrequency.BIWEEKLY -> 2
                                                AutoDepositFrequency.WEEKLY -> 1
                                                AutoDepositFrequency.DAILY,
                                                AutoDepositFrequency.MONTHLY,
                                                AutoDepositFrequency.QUARTERLY -> null
                                            },
                                            onlyIfBalanceAvailable = autoDepositOnlyIfBalanceAvailable,
                                            notifyBefore = autoDepositNotifyBefore,
                                            notifyAfter = autoDepositNotifyAfter,
                                            notifyOnFailure = autoDepositNotifyOnFailure,
                                            nextRunAt = nextRunAt,
                                            lastRunAt = existingSchedule?.lastRunAt,
                                            enabled = true,
                                            createdAt = existingSchedule?.createdAt ?: Instant.now(),
                                            updatedAt = Instant.now()
                                        )
                                    } else null

                                    val updatedSchedules = when {
                                        newSchedule != null -> listOf(newSchedule) + preservedSchedules
                                        else -> preservedSchedules
                                    }

                                    val updatedVault = SmartVault(
                                        id = vault?.id ?: 0L,
                                        name = name.trim(),
                                        targetAmount = target,
                                        currentBalance = balance,
                                        priority = priority,
                                        priorityWeight = weight,
                                        type = type,
                                        allocationMode = VaultAllocationMode.DYNAMIC_AUTO,
                                        manualAllocationPercent = null,
                                        targetDate = if (isFlowGoal) endDate else targetDate,
                                        startDate = if (isFlowGoal) startDate else null,
                                        endDate = if (isFlowGoal) endDate else null,
                                        monthlyNeed = monthly,
                                        accountNotes = accountNotes.takeIf { it.isNotBlank() },
                                        allowAutoIncome = !excludedFromAutoAllocation,
                                        defaultManualDepositDeductFromMain = defaultManualDepositDeductFromMain,
                                        defaultManualWithdrawalCreditMain = defaultManualWithdrawalCreditMain,
                                        schedules = updatedSchedules,
                                        archived = vault?.archived ?: false,
                                        iconName = iconName
                                    )
                                    onSave(updatedVault)
                                },
                                modifier = Modifier.weight(1f).widthIn(min = 100.dp),

                                enabled = validationResult.isValid,
                                icon = {
                                    MaterialSymbolIcon(
                                        icon = if (vault == null) MaterialSymbols.ADD else MaterialSymbols.CHECK,
                                        contentDescription = null,
                                        size = 18.dp
                                    )
                                }
                            ) {
                                Text(if (vault == null) stringResource(R.string.vault_management_add) else stringResource(R.string.vault_save_changes))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalTypeCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    @DrawableRes icon: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MaterialSymbolIcon(
                icon = icon,
                contentDescription = null,
                size = 32.dp,
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (isSelected) {
                MaterialSymbolIcon(
                    icon = MaterialSymbols.CHECK_CIRCLE,
                    contentDescription = null,
                    size = 20.dp,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}




@Composable
fun AutoDepositFrequency.displayName(): String = when (this) {
    AutoDepositFrequency.DAILY -> stringResource(R.string.vault_frequency_daily)
    AutoDepositFrequency.WEEKLY -> stringResource(R.string.vault_frequency_weekly)
    AutoDepositFrequency.BIWEEKLY -> stringResource(R.string.vault_frequency_biweekly)
    AutoDepositFrequency.MONTHLY -> stringResource(R.string.vault_frequency_monthly)
    AutoDepositFrequency.QUARTERLY -> stringResource(R.string.vault_frequency_quarterly)
}

private data class ValidationResult(
    val isValid: Boolean,
    val message: String
)
