package com.example.sparely.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.example.sparely.ui.components.SparelyChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import com.example.sparely.ui.components.SparelyTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.sparely.domain.model.ExpenseCategory
import com.example.sparely.domain.model.ExpenseInput
import com.example.sparely.domain.model.RecommendationResult
import com.example.sparely.domain.model.SmartVault
import com.example.sparely.domain.model.SparelySettings
import com.example.sparely.domain.model.SavingsPercentages
import com.example.sparely.ui.components.SparelyButton
import com.example.sparely.ui.components.SparelyTonalButton
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import com.example.sparely.ui.utils.toSafeDatePickerMillis
import java.time.Instant
import java.time.ZoneOffset
import com.example.sparely.ui.components.SparelyTextButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseEntryScreen(
    settings: SparelySettings,
    recommendation: RecommendationResult?,
    vaults: List<SmartVault> = emptyList(),
    onSave: (ExpenseInput) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ExpenseCategory.OTHER) }
    var includeTax by remember { mutableStateOf(settings.includeTaxByDefault) }
    var deductFromMainAccount by remember { mutableStateOf(false) }
    var deductFromVaultId by remember { mutableStateOf<Long?>(null) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var manualMode by remember { mutableStateOf(!settings.autoRecommendationsEnabled) }
    var emergencyPercent by remember { mutableFloatStateOf(settings.defaultPercentages.emergency.toFloat()) }
    var investPercent by remember { mutableFloatStateOf(settings.defaultPercentages.invest.toFloat()) }
    var funPercent by remember { mutableFloatStateOf(settings.defaultPercentages.`fun`.toFloat()) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var vaultDropdownExpanded by remember { mutableStateOf(false) }
    
    // Date Picker State
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.toSafeDatePickerMillis()
    )

    val activeVaults = remember(vaults) { vaults.filter { !it.archived } }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Log purchase",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = if (manualMode) {
                "You are manually controlling percentages."
            } else {
                "Auto mode applies the latest recommendation for you."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SparelyTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )
        SparelyTextField(
            value = amountText,
            onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
            label = { Text("Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        CategorySelector(selected = category, onSelect = { category = it })
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column {
                Text("Purchase date")
                Text(selectedDate.format(dateFormatter), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SparelyTonalButton(
                onClick = { showDatePicker = true },
                modifier = Modifier
            ) {
                Text("Change")
            }
        }
        
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    SparelyTextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        showDatePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    SparelyTextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Auto allocation")
                Text(
                    text = "Let Sparely pick percentages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = !manualMode, onCheckedChange = { manualMode = !it })
        }
        if (!manualMode) {
            recommendation?.let {
                Surface(
                   color = MaterialTheme.colorScheme.surfaceContainerHigh,
                   shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                   modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Applied suggestion", 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Emergency ${formatPercent(it.recommendedPercentages.emergency)}  •  Invest ${formatPercent(it.recommendedPercentages.invest)}  •  Fun ${formatPercent(it.recommendedPercentages.`fun`)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            PercentSliders(
                emergency = emergencyPercent,
                invest = investPercent,
                funValue = funPercent,
                onEmergencyChange = { emergencyPercent = it },
                onInvestChange = { investPercent = it },
                onFunChange = { funPercent = it }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Amount includes tax?", style = MaterialTheme.typography.bodyMedium)
            Checkbox(checked = includeTax, onCheckedChange = { includeTax = it })
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Deduct from main account", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "Reduce your main account balance by this expense",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Checkbox(
                checked = deductFromMainAccount,
                onCheckedChange = { deductFromMainAccount = it }
            )
        }
        
        // Vault selection dropdown
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Deduct from vault (optional)", style = MaterialTheme.typography.titleSmall)
            Text(
                text = if (activeVaults.isEmpty()) {
                    "No active vaults available. Create a vault in the Vaults screen."
                } else if (deductFromVaultId != null && deductFromMainAccount) {
                    "If expense exceeds vault balance, overflow will be deducted from main account"
                } else {
                    "Choose a vault to deduct this expense from"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (deductFromVaultId != null && deductFromMainAccount) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            if (activeVaults.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = vaultDropdownExpanded,
                    onExpandedChange = { vaultDropdownExpanded = it }
                ) {
                    SparelyTextField(
                        value = deductFromVaultId?.let { id -> 
                            activeVaults.find { it.id == id }?.name ?: "None"
                        } ?: "None",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vaultDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        label = { Text("Select vault") }
                    )
                    ExposedDropdownMenu(
                        expanded = vaultDropdownExpanded,
                        onDismissRequest = { vaultDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text("None")
                                    Text(
                                        text = "Don't deduct from any vault",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                deductFromVaultId = null
                                vaultDropdownExpanded = false
                            }
                        )
                        for (vault in activeVaults) {
                    DropdownMenuItem(
                        text = { Text(vault.name) },
                        onClick = {
                            deductFromVaultId = vault.id
                            vaultDropdownExpanded = false
                        },
                        trailingIcon = {
                            Text(
                                text = "$${String.format("%.2f", vault.currentBalance)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
                    }
                }
            }
        }
        
        errorText?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SparelyButton(onClick = {
                val amount = amountText.toDoubleOrNull()
                if (amount == null || amount <= 0.0) {
                    errorText = "Enter a valid amount"
                    return@SparelyButton
                }
                val manualPercentages = if (manualMode) {
                    SavingsPercentages(
                        emergency = emergencyPercent.toDouble(),
                        invest = investPercent.toDouble(),
                        `fun` = funPercent.toDouble(),
                        safeInvestmentSplit = settings.defaultPercentages.safeInvestmentSplit
                    ).adjustWithinBudget()
                } else {
                    null
                }
                errorText = null
                onSave(
                    ExpenseInput(
                        description = description,
                        amount = amount,
                        category = category,
                        date = selectedDate,
                        includesTax = includeTax,
                        manualPercentages = manualPercentages,
                        deductFromMainAccount = deductFromMainAccount,
                        deductFromVaultId = deductFromVaultId
                    )
                )
            }, modifier = Modifier.weight(1f)) {
                Text("Save")
            }
            SparelyTonalButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySelector(
    selected: ExpenseCategory,
    onSelect: (ExpenseCategory) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Category", style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (category in ExpenseCategory.entries) {
                SparelyChip(
                    selected = selected == category,
                    onClick = { onSelect(category) },
                    label = { Text(category.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }
    }
}

@Composable
private fun PercentSliders(
    emergency: Float,
    invest: Float,
    funValue: Float,
    onEmergencyChange: (Float) -> Unit,
    onInvestChange: (Float) -> Unit,
    onFunChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Manual allocation", style = MaterialTheme.typography.titleSmall)
        AllocationSlider(label = "Emergency", value = emergency, onValueChange = onEmergencyChange)
        AllocationSlider(label = "Invest", value = invest, onValueChange = onInvestChange)
        AllocationSlider(label = "Fun", value = funValue, onValueChange = onFunChange)
        val total = emergency + invest + funValue
        Text("Total ${formatPercent(total.toDouble())}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
    }
}

@Composable
private fun AllocationSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label)
            Text(formatPercent(value.toDouble()))
        }
        Slider(
            value = value,
            onValueChange = { onValueChange(it.coerceIn(0f, 0.5f)) },
            valueRange = 0f..0.5f
        )
    }
}

private fun formatPercent(value: Double): String = String.format("%.1f%%", value.coerceIn(0.0, 1.0) * 100)
