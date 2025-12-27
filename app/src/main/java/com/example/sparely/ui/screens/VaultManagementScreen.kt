package com.example.sparely.ui.screens

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.window.Dialog
import com.example.sparely.R
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
import kotlin.math.abs
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
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
    onViewHistory: ((Long) -> Unit)? = null
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
                        contentDescription = "Create vault",
                        size = 24.dp
                    )
                    Text("Create Vault", style = MaterialTheme.typography.labelLarge)
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
                            title = "Urgent Goals",
                            subtitle = "${urgentVaults.size} goal(s) need attention soon",
                            icon = MaterialSymbols.LOCAL_FIRE_DEPARTMENT
                        )
                    }
                    items(urgentVaults.size) { index ->
                        EnhancedVaultCard(
                            vault = urgentVaults[index],
                            onEdit = { vaultToEdit = urgentVaults[index] },
                            onDelete = { vaultToDelete = urgentVaults[index] },
                            onDeposit = onManualDeposit?.let { { vaultToDeposit = urgentVaults[index] } },
                            onWithdraw = onManualWithdrawal?.let { { vaultToWithdraw = urgentVaults[index] } },
                            onViewHistory = onViewHistory?.let { { it(urgentVaults[index].id) } }
                        )
                    }
                }

                if (activeVaults.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Active Flow Goals",
                            subtitle = "${activeVaults.size} recurring goal(s) currently active",
                            icon = MaterialSymbols.TRENDING_UP
                        )
                    }
                    items(activeVaults.size) { index ->
                        EnhancedVaultCard(
                            vault = activeVaults[index],
                            onEdit = { vaultToEdit = activeVaults[index] },
                            onDelete = { vaultToDelete = activeVaults[index] },
                            onDeposit = onManualDeposit?.let { { vaultToDeposit = activeVaults[index] } },
                            onWithdraw = onManualWithdrawal?.let { { vaultToWithdraw = activeVaults[index] } },
                            onViewHistory = onViewHistory?.let { { it(activeVaults[index].id) } }
                        )
                    }
                }

                if (plannedVaults.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Planned Goals",
                            subtitle = "${plannedVaults.size} goal(s) in progress",
                            icon = MaterialSymbols.ACCOUNT_BALANCE_WALLET
                        )
                    }
                    items(plannedVaults.size) { index ->
                        EnhancedVaultCard(
                            vault = plannedVaults[index],
                            onEdit = { vaultToEdit = plannedVaults[index] },
                            onDelete = { vaultToDelete = plannedVaults[index] },
                            onDeposit = onManualDeposit?.let { { vaultToDeposit = plannedVaults[index] } },
                            onWithdraw = onManualWithdrawal?.let { { vaultToWithdraw = plannedVaults[index] } },
                            onViewHistory = onViewHistory?.let { { it(plannedVaults[index].id) } }
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
                        text = "Total Saved",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${String.format("%.2f", totalBalance)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "of $${String.format("%.2f", totalTarget)} target",
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
                        text = "$vaultCount active vault${if (vaultCount != 1) "s" else ""}",
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
                        label = "Savings Rate",
                        value = "$displaySavingsRate%",
                        isHealthy = displaySavingsRate >= 20
                    )
                    HealthIndicator(
                        label = "Monthly Spending",
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
                text = "No vaults yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Create smart vaults to automatically save for your goals. The system will intelligently allocate funds based on urgency and priorities.",
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
                Text("Create Your First Vault")
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
    val scheduleFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a") }
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
    
    // Projected Completion Logic
    val projectedCompletionDate = remember(vault.currentBalance, vault.monthlyNeed, vault.targetAmount, vault.schedules) {
        if (remaining <= 0) null
        else {
            val monthlyContribution = if (vault.monthlyNeed != null && vault.monthlyNeed > 0) {
                 vault.monthlyNeed
            } else {
                 vault.schedules
                    .filter { it.enabled && it.direction == VaultTransferDirection.MAIN_TO_VAULT }
                    .sumOf { schedule ->
                        when (schedule.type) {
                            VaultScheduleType.DAY_OF_MONTH -> schedule.amount ?: 0.0
                            VaultScheduleType.DAY_OF_WEEK -> (schedule.amount ?: 0.0) * 4.33
                            else -> 0.0 
                        }
                    }
            }
            
            if (monthlyContribution > 0) {
                val monthsNeeded = (remaining / monthlyContribution).toLong()
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
        progress >= 1.0f -> "Goal Reached"
        isOverdue -> "Overdue"
        isUrgent -> "Urgent (${daysUntilTarget} days)"
        vault.monthlyNeed != null && vault.startDate?.let { it <= LocalDate.now() } == true -> "Active Flow"
        else -> "In Progress"
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
                                    contentDescription = "Edit",
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
                                text = "Current Balance",
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
                                    text = "Target: $${String.format("%.0f", vault.targetAmount)}",
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
                                 text = "$${String.format("%.2f", remaining)} to go",
                                 style = MaterialTheme.typography.bodySmall,
                                 color = colorScheme.onSurfaceVariant,
                                 fontWeight = FontWeight.Medium
                             )
                         }
                         
                         if(projectedCompletionDate != null && remaining > 0) {
                             Text(
                                 text = "On track for ${projectedCompletionDate.format(dateFormatter)}",
                                 style = MaterialTheme.typography.bodySmall,
                                 color = colorScheme.tertiary,
                                 fontWeight = FontWeight.Bold
                             )
                         } else if (vault.targetDate != null && remaining > 0) {
                             Text(
                                 text = "Due ${vault.targetDate.format(dateFormatter)}",
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
                                label = "Add",
                                icon = MaterialSymbols.ADD,
                                tint = MaterialTheme.colorScheme.primary,
                                onClick = it
                            )
                        }
                        onWithdraw?.let {
                            VaultActionButton(
                                modifier = Modifier.weight(1f),
                                label = "Withdraw",
                                icon = MaterialSymbols.REMOVE,
                                tint = MaterialTheme.colorScheme.error,
                                onClick = it
                            )
                        }
                        onViewHistory?.let {
                             // Assuming we might want a smaller button or just an icon for history
                            VaultActionButton(
                                modifier = Modifier.weight(1f),
                                label = "History",
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
                    fontFamily = com.example.sparely.ui.theme.PoppinsFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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

    val title = if (isDeposit) "Add to Vault" else "Withdraw from Vault"
    val icon = if (isDeposit) MaterialSymbols.ADD else MaterialSymbols.REMOVE
    val affectMainLabel = if (isDeposit) "Deduct from main account" else "Credit back to main account"
    val affectMainHelper = if (isDeposit) {
        "Subtract this amount from your main balance when adding it to the vault"
    } else {
        "Return this amount to your main balance after withdrawing from the vault"
    }

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
                            text = "Current Balance",
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
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Amount") },
                    prefix = { Text("$") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = !isDeposit && amountText.toDoubleOrNull()?.let { it > currentBalance } == true,
                    supportingText = {
                        if (!isDeposit && amountText.toDoubleOrNull()?.let { it > currentBalance } == true) {
                            Text(
                                text = "Amount exceeds current balance",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )

                // Quick amount suggestions
                if (suggestedAmounts.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Quick amounts",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (amount in suggestedAmounts.take(4)) {
                                SparelyChip(
                                    selected = amountText.toDoubleOrNull() == amount,
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
                    label = { Text("Note (optional)") },
                    placeholder = { Text("e.g., Birthday gift, Emergency expense...") },
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
                        Text("Cancel")
                    }

                    if (isDeposit) {
                        SparelyButton(
                            onClick = {
                                val amount = amountText.toDoubleOrNull()
                                if (amount != null && amount > 0) {
                                    val defaultReason = "Manual deposit"
                                    val finalReason = reason.trim().ifBlank { defaultReason }
                                    onConfirm(amount, finalReason, affectMainAccount)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = amountText.toDoubleOrNull()?.let { it > 0 } == true
                        ) {
                            Text("Add")
                        }
                    } else {
                        // For withdraw, we want a red button, so we might need a custom Sparely button or just configure SparelyButton
                        // Since SparelyButton uses primary color, we can't easily change it to error color without adding a param.
                        // Let's use Button with the shape/style of SparelyButton manually or add a SparelyErrorButton.
                        // For now, I'll use Button but style it to match SparelyButton (height 48, radius 16, bold text).
                        Button(
                            onClick = {
                                val amount = amountText.toDoubleOrNull()
                                if (amount != null && amount > 0 && amount <= currentBalance) {
                                    val defaultReason = "Manual withdrawal"
                                    val finalReason = reason.trim().ifBlank { defaultReason }
                                    onConfirm(amount, finalReason, affectMainAccount)
                                }
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            enabled = amountText.toDoubleOrNull()?.let { 
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
                                Text("Withdraw")
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
    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MaterialSymbolIcon(
                        icon = MaterialSymbols.WARNING,
                        contentDescription = null,
                        size = 32.dp,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Delete Vault?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Are you sure you want to delete '${vault.name}'?",
                    style = MaterialTheme.typography.bodyLarge
                )

                if (vault.currentBalance > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "⚠️ Important",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "This vault has a balance of ${String.format("%.2f", vault.currentBalance)}. This amount will be returned to your main account.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Text(
                    text = "This action cannot be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SparelyTextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                         ProvideTextStyle(
                            value = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = com.example.sparely.ui.theme.PoppinsFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
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
            name.isBlank() -> ValidationResult(false, "Vault name is required")
            isFlowGoal && monthlyNeed.toDoubleOrNull()?.let { it <= 0 } != false -> 
                ValidationResult(false, "Monthly need must be greater than 0")
            !isFlowGoal && targetAmount.toDoubleOrNull()?.let { it <= 0 } != false -> 
                ValidationResult(false, "Target amount must be greater than 0")
            autoDepositEnabled && autoDepositAmount.toDoubleOrNull()?.let { it <= 0 } != false ->
                ValidationResult(false, "Auto-deposit amount must be greater than 0")
            autoDepositEnabled && !nextRunCandidate.isAfter(LocalDateTime.now()) ->
                ValidationResult(false, "Next run must be scheduled in the future")
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
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() 
                    }
                    if (isFlowGoal) endDate = targetDate
                    showTargetDatePicker = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showTargetDatePicker = false }) { Text("Cancel") }
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
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() 
                    }
                    showStartPicker = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("Cancel") }
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
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() 
                    }
                    if (isFlowGoal) targetDate = endDate
                    showEndPicker = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text("Cancel") }
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
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    } ?: autoDepositNextRunDate
                    showScheduleDatePicker = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showScheduleDatePicker = false }) { Text("Cancel") }
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
                            text = if (vault == null) "Create Smart Vault" else "Edit Vault",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "The system will automatically allocate funds based on urgency and priority",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Goal type selector (prominent position)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Goal Type",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GoalTypeCard(
                                modifier = Modifier.weight(1f),
                                title = "Fixed Goal",
                                description = "Save a specific amount by a deadline",
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
                                title = "Flow Goal",
                                description = "Recurring monthly expenses",
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
                            text = "Vault Icon",
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
                        label = { Text("Vault Name") },
                        placeholder = { Text("e.g., Car Fund, Emergency, Tuition") },
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
                            onValueChange = { monthlyNeed = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("Monthly Need") },
                            prefix = { Text("$") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            supportingText = {
                                val amount = monthlyNeed.toDoubleOrNull()
                                if (amount != null && monthlyIncome > 0) {
                                    val percent = (amount / monthlyIncome * 100).toInt()
                                    Text("${percent}% of your monthly income")
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
                                text = startDate?.format(dateFormatter) ?: "Set start date (optional)"
                            )
                        }
                        if (startDate != null) {
                            TextButton(
                                onClick = { startDate = null },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Clear start date")
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
                                text = endDate?.format(dateFormatter) ?: "Set end date (optional)"
                            )
                        }
                        if (endDate != null) {
                            TextButton(
                                onClick = { endDate = null },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Clear end date")
                            }
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
                                onValueChange = { targetAmount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                label = { Text("Target Amount") },
                                prefix = { Text("$") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
 
                            if (vault != null) {
                                SparelyTextField(
                                    value = currentBalance,
                                    onValueChange = { currentBalance = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                    label = { Text("Current") },
                                    prefix = { Text("$") },
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
                                text = targetDate?.format(dateFormatter) ?: "Set deadline (optional)"
                            )
                        }
                        if (targetDate != null) {
                            val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), targetDate)
                            val monthsUntil = daysUntil / 30
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$daysUntil days ($monthsUntil months) until deadline",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(onClick = { targetDate = null }) {
                                    Text("Clear")
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
                            value = type.name.replace("_", " "),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = typeMenuExpanded,
                            onDismissRequest = { typeMenuExpanded = false }
                        ) {
                            for (vaultType in listOf(VaultType.GOAL, VaultType.EMERGENCY, VaultType.INVESTMENT, VaultType.SHORT_TERM, VaultType.LONG_TERM)) {
                                DropdownMenuItem(
                                    text = { Text(vaultType.name.replace("_", " ")) },
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
                            value = priority.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Priority") },
                            supportingText = {
                                if (priority != suggestedPriority) {
                                    Text("Suggested: ${suggestedPriority.name} based on your settings")
                                }
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
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
                                            Text(vaultPriority.name)
                                            if (vaultPriority == suggestedPriority) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = "Suggested",
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
                                    text = "💡 Budget Insight",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Your other vaults already use ${String.format("%.1f", totalExistingAllocation)}% of your monthly income. Keep total allocation under 60% for comfort.",
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
                        label = { Text("Notes (optional)") },
                        placeholder = { Text("Add details about this goal...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Manual transfer defaults",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Choose how Sparely adjusts your main account when you edit this vault manually.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Deduct deposits from main", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "Toggle off if deposits come from another source.",
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
                                Text("Credit withdrawals back to main", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "Turn off if cashing out elsewhere.",
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
                            Text("Exclude from automatic funding", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "This vault will only receive money from manual and scheduled transfers.",
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
                                Text("Auto-deposit schedule", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "Schedule automatic transfers into this vault",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = autoDepositEnabled, onCheckedChange = { autoDepositEnabled = it })
                        }

                        if (autoDepositEnabled) {
                            SparelyTextField(
                                value = autoDepositAmount,
                                onValueChange = { autoDepositAmount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                label = { Text("Amount") },
                                prefix = { Text("$") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // Frequency chips
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (freq in listOf(AutoDepositFrequency.WEEKLY, AutoDepositFrequency.BIWEEKLY, AutoDepositFrequency.MONTHLY)) {
                                    SparelyChip(
                                        selected = autoDepositFrequency == freq,
                                        onClick = { autoDepositFrequency = freq },
                                        label = { Text(freq.name.lowercase().replaceFirstChar { it.titlecase() }) }
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
                                        label = { Text("Run time") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = autoDepositTimeMenuExpanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor()
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
                                text = "Next run: ${autoDepositNextRunDate.format(dateFormatter)} at ${autoDepositNextRunTime.format(timeFormatter)}",
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
                                    Text("Protect main balance", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "Skip the transfer if there isn't enough in your main account.",
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
                                    Text("Reminder before transfer", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "Get a notification so you can prepare or cancel manually.",
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
                                    Text("Confirmation after run", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "Receive a heads-up once money moves.",
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
                                    Text("Alert me if it fails", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "We'll ping you when a transfer is skipped or blocked.",
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
                                Text("Delete Vault")
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SparelyTonalButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }

                            SparelyButton(
                                onClick = {
                                    val balance = if (vault != null) currentBalance.toDoubleOrNull() ?: 0.0 else 0.0
                                    val monthly = monthlyNeed.toDoubleOrNull()
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
                                        (targetAmount.toDoubleOrNull() ?: 0.0)
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

                                    val scheduleAmount = autoDepositAmount.toDoubleOrNull()
                                    val scheduleType = when (autoDepositFrequency) {
                                        AutoDepositFrequency.WEEKLY, AutoDepositFrequency.BIWEEKLY -> VaultScheduleType.DAY_OF_WEEK
                                        AutoDepositFrequency.MONTHLY -> VaultScheduleType.DAY_OF_MONTH
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
                                                AutoDepositFrequency.MONTHLY -> null
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
                                modifier = Modifier.weight(1f),
                                enabled = validationResult.isValid,
                                icon = {
                                    MaterialSymbolIcon(
                                        icon = if (vault == null) MaterialSymbols.ADD else MaterialSymbols.CHECK,
                                        contentDescription = null,
                                        size = 18.dp
                                    )
                                }
                            ) {
                                Text(if (vault == null) "Create Vault" else "Save Changes")
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

private data class ValidationResult(
    val isValid: Boolean,
    val message: String
)