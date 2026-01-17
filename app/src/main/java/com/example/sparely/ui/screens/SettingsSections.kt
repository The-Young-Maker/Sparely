package com.example.sparely.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.sparely.domain.model.*
import com.example.sparely.ui.components.*
import com.example.sparely.ui.utils.filterCurrencyInput
import com.example.sparely.ui.utils.toSafeDatePickerMillis
import com.example.sparely.ui.utils.toSafeDouble
import com.sparely.app.R
import java.time.Instant
import java.time.ZoneOffset
import com.example.sparely.ui.theme.MaterialSymbols
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider

@Composable
fun SettingsMainAccountCard(
    monthlyIncomeText: String,
    onMonthlyIncomeTextChange: (String) -> Unit,
    onUpdateIncome: () -> Unit,
    mainAccountBalanceText: String,
    onMainAccountBalanceTextChange: (String) -> Unit,
    onUpdateBalance: () -> Unit,
    includeTax: Boolean,
    onIncludeTaxToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.settings_income_tax_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            SparelyTextField(
                value = monthlyIncomeText,
                onValueChange = { onMonthlyIncomeTextChange(it.filterCurrencyInput()) },
                label = { Text(stringResource(R.string.settings_monthly_income_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            SparelyTonalButton(onClick = onUpdateIncome) {
                Text(stringResource(R.string.settings_update_income))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.settings_main_account_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.settings_main_account_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            SparelyTextField(
                value = mainAccountBalanceText,
                onValueChange = { onMainAccountBalanceTextChange(it.filterCurrencyInput()) },
                label = { Text(stringResource(R.string.settings_main_account_balance_label)) },
                prefix = { Text("$") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            
            SparelyTonalButton(
                onClick = onUpdateBalance,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_main_account_update_balance))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_include_tax))
                Switch(checked = includeTax, onCheckedChange = onIncludeTaxToggle)
            }
        }
    }
}

@Composable
fun SettingsSmartSavingsCard(
    settings: SparelySettings,
    selectedAllocationMode: VaultAllocationMode,
    onAllocationModeChange: (VaultAllocationMode) -> Unit,
    savingTaxRatePercent: Float,
    onSavingTaxRatePercentChange: (Float) -> Unit,
    onSavingTaxRateCommit: (Double) -> Unit,
    activeSavingTaxRate: Double,
    onDynamicSavingTaxToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.settings_vault_automation_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SparelyChip(
                    selected = selectedAllocationMode == VaultAllocationMode.DYNAMIC_AUTO,
                    onClick = { onAllocationModeChange(VaultAllocationMode.DYNAMIC_AUTO) },
                    label = { Text(stringResource(R.string.settings_vault_mode_dynamic)) }
                )
                SparelyChip(
                    selected = selectedAllocationMode == VaultAllocationMode.MANUAL,
                    onClick = { onAllocationModeChange(VaultAllocationMode.MANUAL) },
                    label = { Text(stringResource(R.string.settings_vault_mode_manual)) }
                )
            }
            Text(
                text = when (selectedAllocationMode) {
                    VaultAllocationMode.DYNAMIC_AUTO -> stringResource(R.string.settings_vault_mode_dynamic_desc)
                    VaultAllocationMode.MANUAL -> stringResource(R.string.settings_vault_mode_manual_desc)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_saving_tax_title), style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = settings.dynamicSavingTaxEnabled,
                    onCheckedChange = onDynamicSavingTaxToggle
                )
            }

            val displayedSavingTaxRate = if (settings.dynamicSavingTaxEnabled) {
                activeSavingTaxRate
            } else {
                (savingTaxRatePercent / 100f).toDouble()
            }

            fun formatPer(v: Double): String = String.format("%.1f%%", v * 100)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (settings.dynamicSavingTaxEnabled) {
                        stringResource(R.string.settings_saving_tax_auto, formatPer(displayedSavingTaxRate))
                    } else {
                        stringResource(R.string.settings_saving_tax_manual, formatPer(displayedSavingTaxRate))
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                SparelyTextButton(
                    onClick = {
                        val baseline = SparelySettings().savingTaxRate
                        onSavingTaxRatePercentChange((baseline * 100).toFloat())
                        onSavingTaxRateCommit(baseline)
                    },
                    enabled = !settings.dynamicSavingTaxEnabled
                ) {
                    Text(stringResource(R.string.action_reset))
                }
            }
            Slider(
                value = savingTaxRatePercent,
                onValueChange = { updated ->
                    if (!settings.dynamicSavingTaxEnabled) {
                        onSavingTaxRatePercentChange(updated.coerceIn(0f, 25f))
                    }
                },
                valueRange = 0f..25f,
                enabled = !settings.dynamicSavingTaxEnabled,
                onValueChangeFinished = {
                    if (!settings.dynamicSavingTaxEnabled) {
                        onSavingTaxRateCommit((savingTaxRatePercent / 100f).toDouble())
                    }
                }
            )
            Text(
                text = stringResource(R.string.settings_saving_tax_desc_extended),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (settings.dynamicSavingTaxEnabled) {
                Text(
                    text = stringResource(R.string.settings_saving_tax_automation_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SettingsBudgetCard(
    autoModeEnabled: Boolean,
    onAutoToggle: (Boolean) -> Unit,
    recommendation: RecommendationResult?,
    emergency: Float,
    invest: Float,
    funPercent: Float,
    onEmergencyChange: (Float) -> Unit,
    onInvestChange: (Float) -> Unit,
    onFunChange: (Float) -> Unit
) {
    fun formatPer(v: Double): String = String.format("%.0f%%", v * 100)
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(stringResource(R.string.settings_auto_recommendations_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = stringResource(R.string.settings_auto_recommendations_toggle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = autoModeEnabled, onCheckedChange = onAutoToggle)
            }
            recommendation?.let {
                Text(
                    text = stringResource(R.string.settings_auto_recommendations_latest, formatPer(it.recommendedPercentages.emergency), formatPer(it.recommendedPercentages.invest)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (!autoModeEnabled) {
                Text(stringResource(R.string.settings_manual_percentages_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                SettingsSlider(label = stringResource(R.string.settings_category_emergency), value = emergency, onValueChange = onEmergencyChange)
                SettingsSlider(label = stringResource(R.string.settings_category_invest), value = invest, onValueChange = onInvestChange)
                SettingsSlider(label = stringResource(R.string.settings_category_fun), value = funPercent, onValueChange = onFunChange)
                
                val total = emergency + invest + funPercent
                Text(
                    text = stringResource(R.string.settings_total_percent, formatPer(total.toDouble() / 100.0)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SettingsSecurityCard(
    biometricEnabled: Boolean,
    onBiometricEnabledChange: (Boolean) -> Unit,
    onAuthenticateUser: ((Boolean) -> Unit) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.settings_security_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_biometric_unlock_title), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = stringResource(R.string.settings_biometric_unlock_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = biometricEnabled,
                    onCheckedChange = { matched ->
                        if (!matched) {
                            // Disabling: require authentication
                            onAuthenticateUser { success ->
                                if (success) {
                                    onBiometricEnabledChange(false)
                                }
                            }
                        } else {
                            // Enabling: just do it
                            onBiometricEnabledChange(true)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDataCard(
    settings: SparelySettings,
    expensesSize: Int,
    onResetHistory: (Boolean) -> Unit,
    onExpenseHistoryRetentionChange: (ExpenseHistoryRetention) -> Unit,
    brandfetchClientId: String?,
    onBrandfetchClientIdChange: (String) -> Unit,
    onExportBackupClick: () -> Unit,
    onImportBackupClick: () -> Unit,
    onExportCsvClick: () -> Unit
) {
    var brandfetchKey by remember(brandfetchClientId) { mutableStateOf(brandfetchClientId ?: "") }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.settings_data_privacy_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                text = stringResource(R.string.settings_backup_info),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SparelyTonalButton(
                onClick = onExportBackupClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(painter = androidx.compose.ui.res.painterResource(id = MaterialSymbols.UPLOAD_FILE), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.settings_export_backup))
            }
            
            SparelyTonalButton(
                onClick = onExportCsvClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(painter = androidx.compose.ui.res.painterResource(id = MaterialSymbols.CSV), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.settings_export_csv))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            SparelyTonalButton(
                onClick = onImportBackupClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(painter = androidx.compose.ui.res.painterResource(id = MaterialSymbols.DOWNLOAD), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.settings_restore_backup))
            }
            
            
            Spacer(modifier = Modifier.height(16.dp))

            // Expense History Retention
            Text(stringResource(R.string.settings_insights_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            var retentionExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = retentionExpanded,
                onExpandedChange = { retentionExpanded = it }
            ) {
                SparelyTextField(
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    value = settings.expenseHistoryRetention.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.settings_history_retention_all).substringBefore(" ")) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = retentionExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = retentionExpanded,
                    onDismissRequest = { retentionExpanded = false }
                ) {
                    ExpenseHistoryRetention.entries.forEach { retention ->
                         DropdownMenuItem(
                             text = { Text(retention.label) },
                             onClick = {
                                 onExpenseHistoryRetentionChange(retention)
                                 retentionExpanded = false
                             }
                         )
                    }
                }
            }
            
            val currentRetention = settings.expenseHistoryRetention
            if (currentRetention == ExpenseHistoryRetention.INDEFINITELY) {
                 Text(
                    text = stringResource(R.string.settings_history_retention_all, expensesSize),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                 )
            } else {
                 Text(
                    text = stringResource(R.string.settings_history_retention_info, currentRetention.label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                 )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Brandfetch Integration
            Text(stringResource(R.string.settings_brandfetch_info).substringBefore(" "), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = stringResource(R.string.settings_brandfetch_info),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SparelyTextField(
                value = brandfetchKey,
                onValueChange = { brandfetchKey = it },
                label = { Text(stringResource(R.string.settings_brandfetch_info).substringBefore(" ")) },
                placeholder = { Text(stringResource(R.string.settings_brandfetch_info).substringAfter(" ")) },
                modifier = Modifier.fillMaxWidth()
            )
            SparelyTonalButton(
                onClick = { 
                    onBrandfetchClientIdChange(brandfetchKey.trim().takeIf { it.isNotEmpty() } ?: "")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_save))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Reset Expense History
            var showResetConfirmation by remember { mutableStateOf(false) }

            if (showResetConfirmation) {
                AlertDialog(
                    onDismissRequest = { showResetConfirmation = false },
                    title = { Text(stringResource(R.string.settings_reset_history_confirm_title)) },
                    text = { Text(stringResource(R.string.settings_reset_history_confirm_message)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onResetHistory(false)
                                showResetConfirmation = false
                            }
                        ) {
                            Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetConfirmation = false }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                )
            }

            Button(
                onClick = { showResetConfirmation = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(painter = androidx.compose.ui.res.painterResource(id = MaterialSymbols.DELETE), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.settings_reset_history))
            }
        }
    }
}


enum class SettingsTab(val titleRes: Int, val icon: Int) {
    General(R.string.settings_tab_general, MaterialSymbols.PERSON),
    Finances(R.string.settings_tab_finances, MaterialSymbols.PAYMENTS),
    System(R.string.settings_tab_system, MaterialSymbols.SETTINGS)
}

@Composable
fun SettingsSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("${value.toInt()}%", style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = value,
            onValueChange = { onValueChange(it) },
            valueRange = 0f..100f
        )
    }
}
