package com.example.sparely.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.sparely.domain.model.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultManagementScreen(
    vaults: List<SmartVault>,
    onAddVault: (SmartVault) -> Unit,
    onUpdateVault: (SmartVault) -> Unit,
    onDeleteVault: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    onManualDeposit: ((Long, Double, String) -> Unit)? = null,
    onManualWithdrawal: ((Long, Double, String) -> Unit)? = null,
    onViewHistory: ((Long) -> Unit)? = null
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingVault by remember { mutableStateOf<SmartVault?>(null) }
    var vaultToDelete by remember { mutableStateOf<SmartVault?>(null) }
    var vaultForManualAdjustment by remember { mutableStateOf<Pair<SmartVault, Boolean>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Smart Vaults") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Vault")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (vaults.isEmpty()) {
                item {
                    EmptyVaultsCard()
                }
            } else {
                items(vaults) { vault ->
                    VaultCard(
                        vault = vault,
                        onEdit = { editingVault = vault },
                        onDelete = { vaultToDelete = vault },
                        onDeposit = if (onManualDeposit != null) {{ vaultForManualAdjustment = vault to true }} else null,
                        onWithdraw = if (onManualWithdrawal != null) {{ vaultForManualAdjustment = vault to false }} else null,
                        onViewHistory = if (onViewHistory != null) {{ onViewHistory(vault.id) }} else null
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        VaultEditorDialog(
            vault = null,
            onSave = { newVault ->
                onAddVault(newVault)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    editingVault?.let { vault ->
        VaultEditorDialog(
            vault = vault,
            onSave = { updated ->
                onUpdateVault(updated)
                editingVault = null
            },
            onDismiss = { editingVault = null }
        )
    }

    vaultToDelete?.let { vault ->
        AlertDialog(
            onDismissRequest = { vaultToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Delete Vault?") },
            text = {
                Text("Are you sure you want to delete \"${vault.name}\"? This will remove all associated contributions.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteVault(vault.id)
                        vaultToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { vaultToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    vaultForManualAdjustment?.let { (vault, isDeposit) ->
        ManualAdjustmentDialog(
            vaultName = vault.name,
            isDeposit = isDeposit,
            onConfirm = { amount, reason ->
                if (isDeposit) {
                    onManualDeposit?.invoke(vault.id, amount, reason)
                } else {
                    onManualWithdrawal?.invoke(vault.id, amount, reason)
                }
                vaultForManualAdjustment = null
            },
            onDismiss = { vaultForManualAdjustment = null }
        )
    }
}

@Composable
private fun EmptyVaultsCard() {
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "No Smart Vaults Yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Create your first vault to start automatically saving toward your goals.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun VaultCard(
    vault: SmartVault,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDeposit: (() -> Unit)? = null,
    onWithdraw: (() -> Unit)? = null,
    onViewHistory: (() -> Unit)? = null
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    val progress = if (vault.targetAmount <= 0) 0f else (vault.currentBalance / vault.targetAmount).toFloat().coerceIn(0f, 1f)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = vault.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${vault.priority.name} priority • ${vault.type.name.replace("_", " ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Current: $${String.format("%.2f", vault.currentBalance)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Target: $${String.format("%.2f", vault.targetAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${String.format("%.1f", progress * 100)}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    vault.targetDate?.let { date ->
                        Text(
                            text = "Due: ${date.format(dateFormatter)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (vault.allocationMode == VaultAllocationMode.MANUAL) {
                val percent = (vault.manualAllocationPercent ?: 0.0) * 100
                Text(
                    text = "Manual allocation: ${String.format("%.1f", percent)}% per expense",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "Dynamic auto-allocation based on priority",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Manual adjustment buttons
            if (onDeposit != null || onWithdraw != null || onViewHistory != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    onDeposit?.let {
                        IconButton(
                            onClick = it,
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Deposit",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Deposit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    onWithdraw?.let {
                        IconButton(
                            onClick = it,
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Withdraw",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    "Withdraw",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    onViewHistory?.let {
                        IconButton(
                            onClick = it,
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = "History"
                                )
                                Text(
                                    "History",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultEditorDialog(
    vault: SmartVault?,
    onSave: (SmartVault) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(vault?.name ?: "") }
    var targetAmount by remember { mutableStateOf(vault?.targetAmount?.toString() ?: "") }
    var currentBalance by remember { mutableStateOf(vault?.currentBalance?.toString() ?: "0") }
    var priority by remember { mutableStateOf(vault?.priority ?: VaultPriority.MEDIUM) }
    var type by remember { mutableStateOf(vault?.type ?: VaultType.SHORT_TERM) }
    var allocationMode by remember { mutableStateOf(vault?.allocationMode ?: VaultAllocationMode.DYNAMIC_AUTO) }
    var manualPercent by remember { mutableStateOf(vault?.manualAllocationPercent?.let { (it * 100).toString() } ?: "") }
    var hasTargetDate by remember { mutableStateOf(vault?.targetDate != null) }
    var targetDate by remember { mutableStateOf(vault?.targetDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    var hasAutoDeposit by remember { mutableStateOf(vault?.autoDepositSchedule != null) }
    var autoDepositAmount by remember { mutableStateOf(vault?.autoDepositSchedule?.amount?.toString() ?: "") }
    var autoDepositFrequency by remember { mutableStateOf(vault?.autoDepositSchedule?.frequency ?: AutoDepositFrequency.WEEKLY) }
    
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    val isValid = name.isNotBlank() && targetAmount.toDoubleOrNull()?.let { it > 0 } == true

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 650.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = if (vault == null) "Add Smart Vault" else "Edit Smart Vault",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Vault Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = targetAmount,
                        onValueChange = { targetAmount = it },
                        label = { Text("Target Amount") },
                        prefix = { Text("$") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = currentBalance,
                        onValueChange = { currentBalance = it },
                        label = { Text("Current Balance") },
                        prefix = { Text("$") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Priority", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VaultPriority.entries.forEach { p ->
                                FilterChip(
                                    selected = priority == p,
                                    onClick = { priority = p },
                                    label = { Text(p.name) }
                                )
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Type", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VaultType.entries.forEach { t ->
                                FilterChip(
                                    selected = type == t,
                                    onClick = { type = t },
                                    label = { Text(t.name.replace("_", " ")) }
                                )
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Allocation Mode", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = allocationMode == VaultAllocationMode.DYNAMIC_AUTO,
                                onClick = { allocationMode = VaultAllocationMode.DYNAMIC_AUTO },
                                label = { Text("Auto") }
                            )
                            FilterChip(
                                selected = allocationMode == VaultAllocationMode.MANUAL,
                                onClick = { allocationMode = VaultAllocationMode.MANUAL },
                                label = { Text("Manual") }
                            )
                        }
                    }
                }

                if (allocationMode == VaultAllocationMode.MANUAL) {
                    item {
                        OutlinedTextField(
                            value = manualPercent,
                            onValueChange = { manualPercent = it },
                            label = { Text("Manual Allocation %") },
                            suffix = { Text("%") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            supportingText = { Text("Percentage of each expense's saving tax to allocate") }
                        )
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Target date", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Switch(
                                    checked = hasTargetDate,
                                    onCheckedChange = { 
                                        hasTargetDate = it
                                        if (!it) targetDate = null
                                    }
                                )
                            }
                            
                            if (hasTargetDate) {
                                OutlinedButton(
                                    onClick = { showDatePicker = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(targetDate?.format(dateFormatter) ?: "Select date")
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Automatic deposits", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(
                                        "Simulate recurring bank transfers",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = hasAutoDeposit,
                                    onCheckedChange = { hasAutoDeposit = it }
                                )
                            }
                            
                            if (hasAutoDeposit) {
                                OutlinedTextField(
                                    value = autoDepositAmount,
                                    onValueChange = { autoDepositAmount = it },
                                    label = { Text("Amount per deposit") },
                                    prefix = { Text("$") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Frequency", style = MaterialTheme.typography.labelMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = autoDepositFrequency == AutoDepositFrequency.WEEKLY,
                                            onClick = { autoDepositFrequency = AutoDepositFrequency.WEEKLY },
                                            label = { Text("Weekly") }
                                        )
                                        FilterChip(
                                            selected = autoDepositFrequency == AutoDepositFrequency.BIWEEKLY,
                                            onClick = { autoDepositFrequency = AutoDepositFrequency.BIWEEKLY },
                                            label = { Text("Bi-weekly") }
                                        )
                                        FilterChip(
                                            selected = autoDepositFrequency == AutoDepositFrequency.MONTHLY,
                                            onClick = { autoDepositFrequency = AutoDepositFrequency.MONTHLY },
                                            label = { Text("Monthly") }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val autoSchedule = if (hasAutoDeposit && autoDepositAmount.toDoubleOrNull()?.let { it > 0 } == true) {
                                    AutoDepositSchedule(
                                        amount = autoDepositAmount.toDoubleOrNull() ?: 0.0,
                                        frequency = autoDepositFrequency,
                                        startDate = LocalDate.now(),
                                        endDate = null,
                                        sourceAccountId = null,
                                        lastExecutionDate = null
                                    )
                                } else null
                                
                                val newVault = SmartVault(
                                    id = vault?.id ?: 0L,
                                    name = name.trim(),
                                    targetAmount = targetAmount.toDoubleOrNull() ?: 0.0,
                                    currentBalance = currentBalance.toDoubleOrNull() ?: 0.0,
                                    targetDate = if (hasTargetDate) targetDate else null,
                                    priority = priority,
                                    type = type,
                                    allocationMode = allocationMode,
                                    manualAllocationPercent = if (allocationMode == VaultAllocationMode.MANUAL) {
                                        manualPercent.toDoubleOrNull()?.div(100.0)?.coerceIn(0.0, 1.0)
                                    } else null,
                                    nextExpectedContribution = vault?.nextExpectedContribution,
                                    lastContributionDate = vault?.lastContributionDate,
                                    autoDepositSchedule = autoSchedule,
                                    savingTaxRateOverride = vault?.savingTaxRateOverride,
                                    archived = vault?.archived ?: false
                                )
                                onSave(newVault)
                            },
                            modifier = Modifier.weight(1f),
                            enabled = isValid
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
    
    if (showDatePicker) {
        val initialMillis = targetDate?.atStartOfDay(java.time.ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    targetDate = selectedMillis?.let { 
                        java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate() 
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualAdjustmentDialog(
    vaultName: String,
    isDeposit: Boolean,
    onConfirm: (amount: Double, reason: String) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    val isValid = amount.toDoubleOrNull()?.let { it > 0 } == true
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (isDeposit) Icons.Default.Add else Icons.Default.Clear,
                contentDescription = null
            )
        },
        title = {
            Text(if (isDeposit) "Manual Deposit" else "Manual Withdrawal")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = vaultName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    prefix = { Text("$") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = amount.isNotEmpty() && amount.toDoubleOrNull() == null
                )
                
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    placeholder = { Text("e.g., Bank transfer, Goal reached, etc.") }
                )
                
                Text(
                    text = if (isDeposit) {
                        "This will add funds to the vault and record it in the adjustment history."
                    } else {
                        "This will remove funds from the vault and record it in the adjustment history."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                    onConfirm(parsedAmount, reason.trim())
                },
                enabled = isValid
            ) {
                Text(if (isDeposit) "Deposit" else "Withdraw")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
