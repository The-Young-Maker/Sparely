package com.example.sparely.ui.screens

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultManagementScreen(
    vaults: List<SmartVault>,
    monthlyIncome: Double = 0.0,
    recentMonthlyExpenses: Double = 0.0,
    onAddVault: (SmartVault) -> Unit,
    onUpdateVault: (SmartVault) -> Unit,
    onDeleteVault: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    onManualDeposit: ((Long, Double, String) -> Unit)? = null,
    onManualWithdrawal: ((Long, Double, String) -> Unit)? = null,
    onViewHistory: ((Long) -> Unit)? = null
) {
    var vaultToEdit by remember { mutableStateOf<SmartVault?>(null) }
    var vaultToDeposit by remember { mutableStateOf<SmartVault?>(null) }
    var vaultToWithdraw by remember { mutableStateOf<SmartVault?>(null) }
    var vaultToDelete by remember { mutableStateOf<SmartVault?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    // Smart sorting: urgent first, then by priority, then by progress
    val sortedVaults = remember(vaults) {
        vaults.filter { !it.archived }.sortedWith(
            compareByDescending<SmartVault> { vault ->
                // Urgency score based on deadline
                vault.targetDate?.let { date ->
                    val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), date)
                    when {
                        daysUntil < 0 -> 1000.0 // Overdue
                        daysUntil <= 30 -> 100.0 // Critical: 1 month
                        daysUntil <= 90 -> 50.0 // Urgent: 3 months
                        daysUntil <= 180 -> 25.0 // Important: 6 months
                        else -> 10.0 / (daysUntil / 30.0) // Decreasing urgency
                    }
                } ?: 0.0
            }.thenByDescending { it.priorityWeight }
                .thenBy { 
                    // Show less complete vaults first (need more attention)
                    if (it.targetAmount > 0) it.currentBalance / it.targetAmount else 0.0 
                }
        )
    }

    // Financial health indicators
    val totalVaultBalance = remember(vaults) { vaults.sumOf { it.currentBalance } }
    val totalTargetAmount = remember(vaults) { vaults.sumOf { it.targetAmount } }
    val overallProgress = remember(totalVaultBalance, totalTargetAmount) {
        if (totalTargetAmount > 0) (totalVaultBalance / totalTargetAmount * 100).toInt() else 0
    }

    Scaffold(
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
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Smart Vaults",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Automated savings allocation based on your goals and spending",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Overall progress card
            if (vaults.isNotEmpty()) {
                item {
                    OverallProgressCard(
                        totalBalance = totalVaultBalance,
                        totalTarget = totalTargetAmount,
                        overallProgress = overallProgress,
                        vaultCount = vaults.size,
                        monthlyIncome = monthlyIncome,
                        recentExpenses = recentMonthlyExpenses
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
            onDismiss = { vaultToEdit = null }
        )
    }

    vaultToDeposit?.let { vault ->
        ManualAdjustmentDialog(
            vaultName = vault.name,
            currentBalance = vault.currentBalance,
            isDeposit = true,
            onConfirm = { amount, reason ->
                onManualDeposit?.invoke(vault.id, amount, reason)
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
            onConfirm = { amount, reason ->
                onManualWithdrawal?.invoke(vault.id, amount, reason)
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
    recentExpenses: Double
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
                val savingsRate = (totalBalance / monthlyIncome * 100).toInt()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HealthIndicator(
                        label = "Savings Rate",
                        value = "$savingsRate%",
                        isHealthy = savingsRate >= 20
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
            tint = if (isHealthy) Color(0xFF4CAF50) else Color(0xFFFFA726)
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
    val progress = if (vault.targetAmount > 0) (vault.currentBalance / vault.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    
    // Calculate financial insights
    val daysUntilTarget = vault.targetDate?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }
    val isOverdue = daysUntilTarget != null && daysUntilTarget < 0
    val isUrgent = daysUntilTarget != null && daysUntilTarget in 0..90
    val remaining = vault.targetAmount - vault.currentBalance
    
    // Smart status indicator
    val statusColor = when {
        progress >= 1.0f -> Color(0xFF4CAF50) // Completed
        isOverdue -> Color(0xFFF44336) // Overdue
        isUrgent -> Color(0xFFFFA726) // Urgent
        else -> colorScheme.primary
    }
    
    val statusText = when {
        progress >= 1.0f -> "✓ Goal Reached"
        isOverdue -> "! Overdue"
        isUrgent -> "⚠ Urgent (${daysUntilTarget} days)"
        vault.monthlyNeed != null && vault.startDate?.let { it <= LocalDate.now() } == true -> "Active Flow"
        else -> "In Progress"
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isUrgent || isOverdue) 
                statusColor.copy(alpha = 0.05f) 
            else 
                colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = vault.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = statusColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = statusText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VaultBadgeChip(
                            label = vault.priority.name,
                            icon = MaterialSymbols.FLAG,
                            tint = when (vault.priority) {
                                VaultPriority.HIGH -> Color(0xFFF44336)
                                VaultPriority.MEDIUM -> Color(0xFFFFA726)
                                VaultPriority.LOW -> Color(0xFF4CAF50)
                                VaultPriority.CRITICAL -> MaterialTheme.colorScheme.error
                            }
                        )
                        VaultBadgeChip(
                            label = vault.type.name.replace("_", " "),
                            icon = when (vault.type) {
                                VaultType.EMERGENCY -> MaterialSymbols.LOCAL_FIRE_DEPARTMENT
                                VaultType.INVESTMENT -> MaterialSymbols.TRENDING_UP
                                else -> MaterialSymbols.ACCOUNT_BALANCE
                            },
                            tint = colorScheme.secondary
                        )
                        if (vault.monthlyNeed != null) {
                            VaultBadgeChip(
                                label = "Flow",
                                icon = MaterialSymbols.REFRESH,
                                tint = colorScheme.tertiary
                            )
                        }
                    }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    onEdit?.let {
                        IconButton(onClick = it) {
                            MaterialSymbolIcon(
                                icon = MaterialSymbols.EDIT,
                                contentDescription = "Edit",
                                size = 20.dp
                            )
                        }
                    }
                    onDelete?.let {
                        IconButton(onClick = it) {
                            MaterialSymbolIcon(
                                icon = MaterialSymbols.DELETE,
                                contentDescription = "Delete",
                                size = 20.dp,
                                tint = colorScheme.error
                            )
                        }
                    }
                }
            }}

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = statusColor,
                trackColor = colorScheme.onSurface.copy(alpha = 0.08f),
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "$${String.format("%.2f", vault.currentBalance)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (remaining > 0) {
                        Text(
                            text = "$${String.format("%.2f", remaining)} remaining",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    vault.monthlyNeed?.let { monthlyNeed ->
                        Text(
                            text = "$${String.format("%.2f", monthlyNeed)}/month needed",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.tertiary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${String.format("%.1f", progress * 100)}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    vault.targetDate?.let { date ->
                        val daysText = if (daysUntilTarget != null) {
                            when {
                                daysUntilTarget < 0 -> "${abs(daysUntilTarget)} days ago"
                                daysUntilTarget == 0L -> "Today!"
                                daysUntilTarget <= 30 -> "$daysUntilTarget days left"
                                else -> date.format(dateFormatter)
                            }
                        } else {
                            date.format(dateFormatter)
                        }
                        Text(
                            text = daysText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOverdue || isUrgent) statusColor else colorScheme.onSurfaceVariant,
                            fontWeight = if (isUrgent) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }
            // Allocation mode indicator with better explanation
            val allocationText = when {
                vault.monthlyNeed != null -> "Smart flow allocation: System adjusts monthly contributions"
                vault.allocationMode == VaultAllocationMode.MANUAL -> {
                    val percent = (vault.manualAllocationPercent ?: 0.0) * 100
                    "Manual: ${String.format("%.1f", percent)}% of each expense"
                }
                else -> "Smart auto-allocation: Priority-based distribution"
            }
            
            Text(
                text = allocationText,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.primary.copy(alpha = 0.8f)
            )

            // Action buttons with better layout
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
                        VaultActionButton(
                            modifier = Modifier.weight(1f),
                            label = "History",
                            icon = MaterialSymbols.LIST,
                            tint = MaterialTheme.colorScheme.secondary,
                            onClick = it
                        )
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
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = tint.copy(alpha = 0.12f),
            contentColor = tint
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            MaterialSymbolIcon(
                icon = icon,
                contentDescription = label,
                size = 20.dp,
                tint = tint
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint
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
    onConfirm: (Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    val title = if (isDeposit) "Add to Vault" else "Withdraw from Vault"
    val icon = if (isDeposit) MaterialSymbols.ADD else MaterialSymbols.REMOVE

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

                OutlinedTextField(
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
                            suggestedAmounts.take(4).forEach { amount ->
                                FilterChip(
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

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Note (optional)") },
                    placeholder = { Text("e.g., Birthday gift, Emergency expense...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull()
                            if (amount != null && amount > 0) {
                                if (isDeposit || amount <= currentBalance) {
                                    onConfirm(amount, reason.trim().ifBlank { if (isDeposit) "Manual deposit" else "Manual withdrawal" })
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = amountText.toDoubleOrNull()?.let { 
                            it > 0 && (isDeposit || it <= currentBalance)
                        } == true,
                        colors = if (isDeposit) 
                            ButtonDefaults.buttonColors()
                        else
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                    ) {
                        Text(if (isDeposit) "Add" else "Withdraw")
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
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Delete")
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
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(vault?.name ?: "") }
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
    var excludedFromAutoAllocation by remember { mutableStateOf(vault?.excludedFromAutoAllocation ?: false) }

    // Auto-deposit editing
    var autoDepositEnabled by remember { mutableStateOf(vault?.autoDepositSchedule != null) }
    var autoDepositAmount by remember { mutableStateOf(vault?.autoDepositSchedule?.amount?.toString() ?: "") }
    var autoDepositFrequency by remember { mutableStateOf(vault?.autoDepositSchedule?.frequency ?: AutoDepositFrequency.MONTHLY) }
    var autoDepositStartDate by remember { mutableStateOf(vault?.autoDepositSchedule?.startDate ?: LocalDate.now()) }
    var autoDepositEndDate by remember { mutableStateOf(vault?.autoDepositSchedule?.endDate) }
    var autoDepositExecuteAutomatically by remember { mutableStateOf(vault?.autoDepositSchedule?.executeAutomatically ?: false) }

    var priorityMenuExpanded by remember { mutableStateOf(false) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var showTargetDatePicker by remember { mutableStateOf(false) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    
    // Smart validation with helpful feedback
    val validationResult = remember(name, targetAmount, monthlyNeed, isFlowGoal, targetDate, endDate) {
        when {
            name.isBlank() -> ValidationResult(false, "Vault name is required")
            isFlowGoal && monthlyNeed.toDoubleOrNull()?.let { it <= 0 } != false -> 
                ValidationResult(false, "Monthly need must be greater than 0")
            !isFlowGoal && targetAmount.toDoubleOrNull()?.let { it <= 0 } != false -> 
                ValidationResult(false, "Target amount must be greater than 0")
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
        val initialMillis = targetDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
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
        val initialStartMillis = startDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
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
        val initialEndMillis = endDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
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
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Vault Name") },
                        placeholder = { Text("e.g., Car Fund, Emergency, Tuition") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            MaterialSymbolIcon(
                                icon = MaterialSymbols.ACCOUNT_BALANCE_WALLET,
                                contentDescription = null,
                                size = 20.dp
                            )
                        }
                    )
                }

                // Different fields based on goal type
                if (isFlowGoal) {
                    item {
                        OutlinedTextField(
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
                        OutlinedButton(
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
                        OutlinedButton(
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
                            OutlinedTextField(
                                value = targetAmount,
                                onValueChange = { targetAmount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                label = { Text("Target Amount") },
                                prefix = { Text("$") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )

                            if (vault != null) {
                                OutlinedTextField(
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
                        OutlinedButton(
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
                        OutlinedTextField(
                            value = type.name.replace("_", " "),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = typeMenuExpanded,
                            onDismissRequest = { typeMenuExpanded = false }
                        ) {
                            listOf(VaultType.GOAL, VaultType.EMERGENCY, VaultType.INVESTMENT, VaultType.SHORT_TERM, VaultType.LONG_TERM).forEach { vaultType ->
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
                        OutlinedTextField(
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
                                .menuAnchor(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = priorityMenuExpanded,
                            onDismissRequest = { priorityMenuExpanded = false }
                        ) {
                            VaultPriority.entries.forEach { vaultPriority ->
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
                    OutlinedTextField(
                        value = accountNotes,
                        onValueChange = { accountNotes = it },
                        label = { Text("Notes (optional)") },
                        placeholder = { Text("Add details about this goal...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )
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
                            OutlinedTextField(
                                value = autoDepositAmount,
                                onValueChange = { autoDepositAmount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                label = { Text("Amount") },
                                prefix = { Text("$") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // Frequency chips
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(AutoDepositFrequency.WEEKLY, AutoDepositFrequency.BIWEEKLY, AutoDepositFrequency.MONTHLY).forEach { freq ->
                                    AssistChip(
                                        onClick = { autoDepositFrequency = freq },
                                        label = { Text(freq.name.lowercase().replaceFirstChar { it.titlecase() }) },
                                        enabled = true,
                                        border = if (autoDepositFrequency == freq) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Execute automatically when due", style = MaterialTheme.typography.bodyMedium)
                                Switch(checked = autoDepositExecuteAutomatically, onCheckedChange = { autoDepositExecuteAutomatically = it })
                            }
                        }
                    }
                }
                item {
                    HorizontalDivider()
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
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
                                    excludedFromAutoAllocation = excludedFromAutoAllocation,
                                    autoDepositSchedule = if (autoDepositEnabled) {
                                        AutoDepositSchedule(
                                            amount = autoDepositAmount.toDoubleOrNull() ?: 0.0,
                                            frequency = autoDepositFrequency,
                                            startDate = autoDepositStartDate,
                                            endDate = autoDepositEndDate,
                                            executeAutomatically = autoDepositExecuteAutomatically
                                        )
                                    } else null,
                                    archived = vault?.archived ?: false
                                )
                                onSave(updatedVault)
                            },
                            modifier = Modifier.weight(1f),
                            enabled = validationResult.isValid
                        ) {
                            MaterialSymbolIcon(
                                icon = if (vault == null) MaterialSymbols.ADD else MaterialSymbols.CHECK,
                                contentDescription = null,
                                size = 18.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (vault == null) "Create Vault" else "Save Changes")
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