package com.example.sparely.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.sparely.R
import com.example.sparely.domain.model.AutoDepositFrequency
import com.example.sparely.domain.model.SmartVault
import com.example.sparely.domain.model.VaultAllocationMode
import com.example.sparely.domain.model.VaultPriority
import com.example.sparely.domain.model.VaultType
import com.example.sparely.ui.components.ExpressiveCard
import com.example.sparely.ui.components.SingleLineText
import com.example.sparely.ui.theme.MaterialSymbolIcon
import com.example.sparely.ui.theme.MaterialSymbols
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
    var vaultToEdit by remember { mutableStateOf<SmartVault?>(null) }
    var vaultToDeposit by remember { mutableStateOf<SmartVault?>(null) }
    var vaultToWithdraw by remember { mutableStateOf<SmartVault?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(vaults.size) { index ->
            val vault = vaults[index]
            VaultCard(
                vault = vault,
                onEdit = { vaultToEdit = vault },
                onDelete = { onDeleteVault(vault.id) },
                onDeposit = onManualDeposit?.let { { vaultToDeposit = vault } },
                onWithdraw = onManualWithdrawal?.let { { vaultToWithdraw = vault } },
                onViewHistory = onViewHistory?.let { { it(vault.id) } }
            )
        }
    }

    // Edit dialog
    vaultToEdit?.let { vault ->
        VaultEditorDialog(
            vault = vault,
            onSave = { updatedVault ->
                onUpdateVault(updatedVault)
                vaultToEdit = null
            },
            onDismiss = { vaultToEdit = null }
        )
    }

    // Deposit dialog
    vaultToDeposit?.let { vault ->
        ManualAdjustmentDialog(
            vaultName = vault.name,
            isDeposit = true,
            onConfirm = { amount, reason ->
                onManualDeposit?.invoke(vault.id, amount, reason)
                vaultToDeposit = null
            },
            onDismiss = { vaultToDeposit = null }
        )
    }

    // Withdrawal dialog
    vaultToWithdraw?.let { vault ->
        ManualAdjustmentDialog(
            vaultName = vault.name,
            isDeposit = false,
            onConfirm = { amount, reason ->
                onManualWithdrawal?.invoke(vault.id, amount, reason)
                vaultToWithdraw = null
            },
            onDismiss = { vaultToWithdraw = null }
        )
    }
}

@Composable
private fun VaultCard(
    vault: SmartVault,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onDeposit: (() -> Unit)?,
    onWithdraw: (() -> Unit)?,
    onViewHistory: (() -> Unit)?
) {
    val colorScheme = MaterialTheme.colorScheme
    val progress = if (vault.targetAmount > 0) (vault.currentBalance / vault.targetAmount).toFloat() else 0f
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = colorScheme.surface
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
                    Text(
                        text = vault.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VaultBadgeChip(label = vault.priority.name, icon = MaterialSymbols.FLAG, tint = MaterialTheme.colorScheme.tertiary)
                        VaultBadgeChip(label = vault.type.name.replace("_", " "), icon = MaterialSymbols.ACCOUNT_BALANCE, tint = MaterialTheme.colorScheme.secondary)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    onEdit?.let {
                        IconButton(onClick = it) {
                            MaterialSymbolIcon(icon = MaterialSymbols.EDIT, contentDescription = stringResource(R.string.vault_edit), size = 20.dp)
                        }
                    }
                    onDelete?.let {
                        IconButton(onClick = it) {
                            MaterialSymbolIcon(icon = MaterialSymbols.DELETE, contentDescription = stringResource(R.string.vault_delete), size = 20.dp, tint = colorScheme.error)
                        }
                    }
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = colorScheme.primary,
                trackColor = colorScheme.onSurface.copy(alpha = 0.08f),
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    onDeposit?.let {
                        VaultActionButton(
                            modifier = Modifier.weight(1f),
                            label = stringResource(R.string.vault_deposit),
                            icon = MaterialSymbols.ADD,
                            tint = MaterialTheme.colorScheme.primary,
                            onClick = it
                        )
                    }
                    onWithdraw?.let {
                        VaultActionButton(
                            modifier = Modifier.weight(1f),
                            label = stringResource(R.string.vault_withdraw),
                            icon = MaterialSymbols.REMOVE,
                            tint = MaterialTheme.colorScheme.error,
                            onClick = it
                        )
                    }
                    onViewHistory?.let {
                        VaultActionButton(
                            modifier = Modifier.weight(1f),
                            label = stringResource(R.string.vault_history),
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
        modifier = modifier.heightIn(min = 50.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = tint.copy(alpha = 0.12f),
            contentColor = tint
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        MaterialSymbolIcon(
            icon = icon,
            contentDescription = label,
            size = 18.dp,
            tint = tint
        )
        Spacer(modifier = Modifier.width(8.dp))
        SingleLineText(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = tint
        )
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MaterialSymbolIcon(
                icon = icon,
                contentDescription = null,
                size = 16.dp,
                tint = tint
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = tint
            )
        }
    }
}

@Composable
private fun ManualAdjustmentDialog(
    vaultName: String,
    isDeposit: Boolean,
    onConfirm: (Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    val title = if (isDeposit) 
        stringResource(R.string.vault_manual_deposit_title)
    else 
        stringResource(R.string.vault_manual_withdrawal_title)
    
    val note = if (isDeposit)
        stringResource(R.string.vault_deposit_note)
    else
        stringResource(R.string.vault_withdrawal_note)

    Dialog(onDismissRequest = onDismiss) {
        ExpressiveCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text(stringResource(R.string.vault_amount_label)) },
                    prefix = { Text("$") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(stringResource(R.string.vault_reason_label)) },
                    placeholder = { Text(stringResource(R.string.vault_reason_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )

                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }

                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull()
                            if (amount != null && amount > 0) {
                                onConfirm(amount, reason.trim())
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = amountText.toDoubleOrNull()?.let { it > 0 } == true
                    ) {
                        Text(stringResource(android.R.string.ok))
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

    var accountType by remember { mutableStateOf(vault?.accountType) }
    var accountNumber by remember { mutableStateOf(vault?.accountNumber ?: "") }
    var accountNotes by remember { mutableStateOf(vault?.accountNotes ?: "") }
    var showAccountTypeDialog by remember { mutableStateOf(false) }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    val isValid = name.isNotBlank() && targetAmount.toDoubleOrNull()?.let { it > 0 } == true

    Dialog(onDismissRequest = onDismiss) {
        ExpressiveCard(
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
                        text = if (vault == null) stringResource(R.string.vault_editor_add_title) else stringResource(R.string.vault_editor_edit_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.vault_name_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = targetAmount,
                        onValueChange = { targetAmount = it },
                        label = { Text(stringResource(R.string.vault_target_amount_label)) },
                        prefix = { Text("$") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = currentBalance,
                        onValueChange = { currentBalance = it },
                        label = { Text(stringResource(R.string.vault_current_balance_label)) },
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
                            onValueChange = { manualPercent = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text(stringResource(R.string.vault_manual_percent_label)) },
                            suffix = { Text("%") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                        )
                    }
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
                            Text(stringResource(android.R.string.cancel))
                        }

                        Button(
                            onClick = {
                                val target = targetAmount.toDoubleOrNull() ?: return@Button
                                val balance = currentBalance.toDoubleOrNull() ?: 0.0
                                val manualAlloc = if (allocationMode == VaultAllocationMode.MANUAL) {
                                    manualPercent.toDoubleOrNull()?.div(100.0)?.coerceIn(0.0, 1.0)
                                } else null

                                val updatedVault = SmartVault(
                                    id = vault?.id ?: 0L,
                                    name = name.trim(),
                                    targetAmount = target,
                                    currentBalance = balance,
                                    priority = priority,
                                    type = type,
                                    allocationMode = allocationMode,
                                    manualAllocationPercent = manualAlloc,
                                    targetDate = targetDate,
                                    accountType = accountType,
                                    accountNumber = accountNumber.takeIf { it.isNotBlank() },
                                    accountNotes = accountNotes.takeIf { it.isNotBlank() },
                                    autoDepositSchedule = null // Simplified for now
                                )
                                onSave(updatedVault)
                            },
                            modifier = Modifier.weight(1f),
                            enabled = isValid
                        ) {
                            Text(stringResource(android.R.string.ok))
                        }
                    }
                }
            }
        }
    }
}
