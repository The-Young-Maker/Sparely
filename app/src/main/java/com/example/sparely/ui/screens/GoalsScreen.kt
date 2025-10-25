package com.example.sparely.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import com.example.sparely.ui.theme.MaterialSymbols
import com.example.sparely.ui.theme.MaterialSymbolIcon
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.sparely.domain.model.SmartVault
import com.example.sparely.domain.model.SmartVaultSetup
import com.example.sparely.domain.model.VaultType
import com.example.sparely.domain.model.VaultPriority
import com.example.sparely.domain.model.VaultAllocationMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun GoalsScreen(
    vaults: List<SmartVault>,
    onAddVault: (SmartVaultSetup) -> Unit,
    onToggleArchive: (Long, Boolean) -> Unit,
    onDeleteVault: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Manage your savings vaults",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            VaultComposer(onAddVault = onAddVault)
        }
        items(vaults) { vault ->
            VaultCard(
                vault = vault,
                onArchiveToggle = { onToggleArchive(vault.id, it) },
                onDeleteVault = { onDeleteVault(vault.id) }
            )
        }
        if (vaults.isEmpty()) {
            item {
                EmptyVaultsState()
            }
        }
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultComposer(onAddVault: (SmartVaultSetup) -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var targetAmountText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(VaultType.GOAL) }
    var selectedPriority by remember { mutableStateOf(VaultPriority.MEDIUM) }
    var targetDate by remember { mutableStateOf<LocalDate?>(null) }
    var accountNotes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var priorityMenuExpanded by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                targetDate = LocalDate.of(year, month + 1, day)
                showDatePicker = false
            },
            (targetDate ?: LocalDate.now()).year,
            (targetDate ?: LocalDate.now()).monthValue - 1,
            (targetDate ?: LocalDate.now()).dayOfMonth
        ).apply {
            setOnDismissListener { showDatePicker = false }
        }.show()
    }

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Create vault", style = MaterialTheme.typography.titleMedium)
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Vault name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = targetAmountText,
                onValueChange = { targetAmountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text("Target amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            // Vault Type Dropdown
            ExposedDropdownMenuBox(
                expanded = typeMenuExpanded,
                onExpandedChange = { typeMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedType.displayName(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Vault type") },
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
                    VaultType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName()) },
                            onClick = {
                                selectedType = type
                                typeMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Priority Dropdown
            ExposedDropdownMenuBox(
                expanded = priorityMenuExpanded,
                onExpandedChange = { priorityMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedPriority.displayName(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Priority") },
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
                    VaultPriority.values().forEach { priority ->
                        DropdownMenuItem(
                            text = { Text(priority.displayName()) },
                            onClick = {
                                selectedPriority = priority
                                priorityMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("Target date")
                    Text(
                        text = targetDate?.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) ?: "Flexible",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(onClick = { showDatePicker = true }) {
                    Text(if (targetDate == null) "Set" else "Change")
                }
            }
            
            OutlinedTextField(
                value = accountNotes,
                onValueChange = { accountNotes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            
            error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
            
            Button(onClick = {
                val targetAmount = targetAmountText.toDoubleOrNull()
                if (name.isBlank()) {
                    error = "Enter a vault name"
                    return@Button
                }
                if (targetAmount == null || targetAmount <= 0.0) {
                    error = "Enter a target amount"
                    return@Button
                }
                error = null
                onAddVault(
                    SmartVaultSetup(
                        name = name.trim(),
                        targetAmount = targetAmount,
                        type = selectedType,
                        priority = selectedPriority,
                        targetDate = targetDate,
                        accountNotes = accountNotes.ifBlank { null },
                        allocationMode = VaultAllocationMode.MANUAL
                    )
                )
                name = ""
                targetAmountText = ""
                accountNotes = ""
                targetDate = null
                selectedType = VaultType.GOAL
                selectedPriority = VaultPriority.MEDIUM
            }) {
                Text("Add vault")
            }
        }
    }
}

@Composable
private fun VaultCard(
    vault: SmartVault,
    onArchiveToggle: (Boolean) -> Unit,
    onDeleteVault: () -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(vault.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${vault.type.displayName()} • ${vault.priority.displayName()} priority",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDeleteVault) {
                    MaterialSymbolIcon(icon = MaterialSymbols.DELETE, contentDescription = "Delete vault")
                }
            }
            
            LinearProgressIndicator(
                progress = { vault.progressPercent.coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = ProgressIndicatorDefaults.linearColor,
                trackColor = ProgressIndicatorDefaults.linearTrackColor,
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
            )
            
            Text(
                text = "Balance ${formatCurrency(vault.currentBalance)} of ${formatCurrency(vault.targetAmount)} (${formatPercent(vault.progressPercent)})",
                style = MaterialTheme.typography.bodySmall
            )
            
            vault.targetDate?.let { date ->
                Text(
                    text = "Target date: ${date.format(formatter)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            vault.accountNotes?.let { notes ->
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (vault.archived) "Archived" else "Active")
                TextButton(onClick = { onArchiveToggle(!vault.archived) }) {
                    Text(if (vault.archived) "Restore" else "Archive")
                }
            }
        }
    }
}

@Composable
private fun EmptyVaultsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No vaults yet", style = MaterialTheme.typography.titleSmall)
        Text(
            text = "Create a savings vault to start organizing your money.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun VaultType.displayName(): String = when (this) {
    VaultType.SHORT_TERM -> "Short-term"
    VaultType.LONG_TERM -> "Long-term"
    VaultType.PASSIVE_INVESTMENT -> "Passive Investment"
    VaultType.GOAL -> "Goal"
    VaultType.EMERGENCY -> "Emergency"
    VaultType.INVESTMENT -> "Investment"
}

private fun VaultPriority.displayName(): String = when (this) {
    VaultPriority.LOW -> "Low"
    VaultPriority.MEDIUM -> "Medium"
    VaultPriority.HIGH -> "High"
    VaultPriority.CRITICAL -> "Critical"
}

private fun formatCurrency(value: Double): String = "$" + String.format("%,.2f", value)

private fun formatPercent(value: Double): String = String.format("%.1f%%", value.coerceIn(0.0, 1.0) * 100)