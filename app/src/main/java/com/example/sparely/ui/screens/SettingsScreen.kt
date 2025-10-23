package com.example.sparely.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Divider
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.sparely.domain.model.AlertMessage
import com.example.sparely.domain.model.EducationStatus
import com.example.sparely.domain.model.EmploymentStatus
import com.example.sparely.domain.model.RecommendationResult
import com.example.sparely.domain.model.RiskLevel
import com.example.sparely.domain.model.SavingsCategory
import com.example.sparely.domain.model.SavingsPlan
import com.example.sparely.domain.model.SavingsTransfer
import com.example.sparely.domain.model.SavingsPercentages
import com.example.sparely.domain.model.SparelySettings
import com.example.sparely.domain.model.PayScheduleSettings
import com.example.sparely.domain.model.IncomeTrackingMode
import com.example.sparely.domain.model.PayInterval
import com.example.sparely.domain.model.VaultAllocationMode
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SparelySettings,
    activeSaveRate: Double,
    activeSavingTaxRate: Double,
    automationNotes: List<String>,
    autoModeEnabled: Boolean,
    recommendation: RecommendationResult?,
    savingsPlan: SavingsPlan?,
    manualTransfers: List<SavingsTransfer>,
    alerts: List<AlertMessage>,
    onPercentagesChange: (SavingsPercentages) -> Unit,
    onAutoToggle: (Boolean) -> Unit,
    onRiskChange: (RiskLevel) -> Unit,
    onAgeChange: (Int) -> Unit,
    onEducationStatusChange: (EducationStatus) -> Unit,
    onEmploymentStatusChange: (EmploymentStatus) -> Unit,
    onHasDebtsChange: (Boolean) -> Unit,
    onEmergencyFundChange: (Double) -> Unit,
    onPrimaryGoalChange: (String?) -> Unit,
    onDisplayNameChange: (String?) -> Unit,
    onBirthdayChange: (LocalDate?) -> Unit,
    onLogTransfer: (SavingsCategory, Double) -> Unit,
    onMonthlyIncomeChange: (Double) -> Unit,
    onIncludeTaxToggle: (Boolean) -> Unit,
    onVaultAllocationModeChange: (VaultAllocationMode) -> Unit,
    onSavingTaxRateChange: (Double) -> Unit,
    onDynamicSavingTaxToggle: (Boolean) -> Unit,
    onReminderChange: (Boolean, Int, Int) -> Unit,
    onResetHistory: (Boolean) -> Unit,
    onPayScheduleChange: (PayScheduleSettings) -> Unit,
    onRecordPaycheck: (Double, LocalDate, Boolean, Boolean) -> Unit,
    onAutoDepositsEnabledChange: (Boolean) -> Unit,
    onAutoDepositCheckHourChange: (Int) -> Unit,
    onManualAutoDepositTrigger: () -> Unit,
    autoDepositsEnabled: Boolean,
    autoDepositCheckHour: Int,
    onRegionalSettingsChange: (String, String, String, Double?) -> Unit // countryCode, languageCode, currencyCode, customTaxRate
) {
    var emergency by remember(settings.defaultPercentages) { mutableStateOf(settings.defaultPercentages.emergency.toFloat()) }
    var invest by remember(settings.defaultPercentages) { mutableStateOf(settings.defaultPercentages.invest.toFloat()) }
    var funPercent by remember(settings.defaultPercentages) { mutableStateOf(settings.defaultPercentages.`fun`.toFloat()) }
    var monthlyIncomeText by remember(settings.monthlyIncome) { mutableStateOf(settings.monthlyIncome.toString()) }
    var remindersEnabled by remember(settings.remindersEnabled) { mutableStateOf(settings.remindersEnabled) }
    var reminderHour by remember(settings.reminderHour) { mutableStateOf(settings.reminderHour) }
    var reminderFrequency by remember(settings.reminderFrequencyDays) { mutableStateOf(settings.reminderFrequencyDays) }
    var age by remember(settings.effectiveAge) { mutableStateOf(settings.effectiveAge) }
    var displayName by remember(settings.displayName) { mutableStateOf(settings.displayName.orEmpty()) }
    var hasDebts by remember(settings.hasDebts) { mutableStateOf(settings.hasDebts) }
    var emergencyFundText by remember(settings.currentEmergencyFund) {
        mutableStateOf(settings.currentEmergencyFund.takeIf { it > 0.0 }?.let { String.format("%.0f", it) } ?: "")
    }
    var primaryGoal by remember(settings.primaryGoal) { mutableStateOf(settings.primaryGoal.orEmpty()) }
    var educationExpanded by remember { mutableStateOf(false) }
    var employmentExpanded by remember { mutableStateOf(false) }
    var selectedEducation by remember(settings.educationStatus) { mutableStateOf(settings.educationStatus) }
    var selectedEmployment by remember(settings.employmentStatus) { mutableStateOf(settings.employmentStatus) }
    var showBirthdayPicker by remember { mutableStateOf(false) }
    var selectedAllocationMode by remember(settings.vaultAllocationMode) { mutableStateOf(settings.vaultAllocationMode) }
    var savingTaxRatePercent by remember(settings.savingTaxRate, activeSavingTaxRate, settings.dynamicSavingTaxEnabled) {
        mutableStateOf(
            if (settings.dynamicSavingTaxEnabled) {
                (activeSavingTaxRate * 100).toFloat()
            } else {
                (settings.savingTaxRate * 100).toFloat()
            }
        )
    }
    LaunchedEffect(settings.dynamicSavingTaxEnabled, settings.savingTaxRate, activeSavingTaxRate) {
        savingTaxRatePercent = if (settings.dynamicSavingTaxEnabled) {
            (activeSavingTaxRate * 100).toFloat()
        } else {
            (settings.savingTaxRate * 100).toFloat()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Smart vault automation", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedAllocationMode == VaultAllocationMode.DYNAMIC_AUTO,
                            onClick = {
                                selectedAllocationMode = VaultAllocationMode.DYNAMIC_AUTO
                                onVaultAllocationModeChange(VaultAllocationMode.DYNAMIC_AUTO)
                            },
                            label = { Text("Dynamic") }
                        )
                        FilterChip(
                            selected = selectedAllocationMode == VaultAllocationMode.MANUAL,
                            onClick = {
                                selectedAllocationMode = VaultAllocationMode.MANUAL
                                onVaultAllocationModeChange(VaultAllocationMode.MANUAL)
                            },
                            label = { Text("Manual weights") }
                        )
                    }
                    Text(
                        text = when (selectedAllocationMode) {
                            VaultAllocationMode.DYNAMIC_AUTO -> "Sparely prioritizes vaults using urgency, priority, and progress."
                            VaultAllocationMode.MANUAL -> "You control each vault's share in its settings."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Automate saving tax rate", style = MaterialTheme.typography.bodyMedium)
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (settings.dynamicSavingTaxEnabled) {
                                "Saving tax rate (auto): ${formatPercent(displayedSavingTaxRate)}"
                            } else {
                                "Saving tax rate: ${formatPercent(displayedSavingTaxRate)}"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(
                            onClick = {
                                val baseline = SparelySettings().savingTaxRate
                                savingTaxRatePercent = (baseline * 100).toFloat()
                                onSavingTaxRateChange(baseline)
                            },
                            enabled = !settings.dynamicSavingTaxEnabled
                        ) {
                            Text("Reset")
                        }
                    }
                    Slider(
                        value = savingTaxRatePercent,
                        onValueChange = { updated ->
                            if (!settings.dynamicSavingTaxEnabled) {
                                savingTaxRatePercent = updated.coerceIn(0f, 25f)
                            }
                        },
                        valueRange = 0f..25f,
                        enabled = !settings.dynamicSavingTaxEnabled,
                        onValueChangeFinished = {
                            if (!settings.dynamicSavingTaxEnabled) {
                                onSavingTaxRateChange((savingTaxRatePercent / 100f).toDouble())
                            }
                        }
                    )
                    Text(
                        text = "We skim this percentage from each expense and distribute it across your vaults.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (settings.dynamicSavingTaxEnabled) {
                        Text(
                            text = "Automation keeps the rate aligned with your latest paychecks and spending trends.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

        AutomationOverviewCard(
            settings = settings,
            activeSaveRate = activeSaveRate,
            activeSavingTaxRate = activeSavingTaxRate,
            automationNotes = automationNotes
        )

        // Auto-Deposits Card
        AutoDepositsCard(
            enabled = autoDepositsEnabled,
            checkHour = autoDepositCheckHour,
            onEnabledChange = onAutoDepositsEnabledChange,
            onCheckHourChange = onAutoDepositCheckHourChange,
            onManualTrigger = onManualAutoDepositTrigger
        )

        IncomeSettingsCard(
            schedule = settings.paySchedule,
            activeSaveRate = activeSaveRate,
            automationNotes = automationNotes,
            onScheduleSave = onPayScheduleChange,
            onRecordPaycheck = onRecordPaycheck
        )

        LaunchedEffect(emergency, invest, funPercent, autoModeEnabled) {
            if (!autoModeEnabled) {
                onPercentagesChange(
                    SavingsPercentages(
                        emergency = emergency.toDouble(),
                        invest = invest.toDouble(),
                        `fun` = funPercent.toDouble(),
                        safeInvestmentSplit = settings.defaultPercentages.safeInvestmentSplit
                    )
                )
            }
        }

        ProfileCard(
            displayName = displayName,
            onDisplayNameChange = {
                displayName = it
                onDisplayNameChange(it.trim().takeIf { trimmed -> trimmed.isNotEmpty() })
            },
            hasDebts = hasDebts,
            onHasDebtsChange = {
                hasDebts = it
                onHasDebtsChange(it)
            },
            emergencyFundText = emergencyFundText,
            onEmergencyFundChange = { valueText ->
                emergencyFundText = valueText
                valueText.toDoubleOrNull()?.let(onEmergencyFundChange)
            },
            primaryGoal = primaryGoal,
            onPrimaryGoalChange = {
                primaryGoal = it
                onPrimaryGoalChange(it.trim().takeIf { trimmed -> trimmed.isNotEmpty() })
            },
            birthday = settings.birthday,
            effectiveAge = age,
            onEditBirthday = { showBirthdayPicker = true },
            onClearBirthday = {
                onBirthdayChange(null)
                showBirthdayPicker = false
            }
        )

        if (showBirthdayPicker) {
            val initialMillis = settings.birthday?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
            LaunchedEffect(initialMillis) {
                if (initialMillis != null && datePickerState.selectedDateMillis != initialMillis) {
                    datePickerState.selectedDateMillis = initialMillis
                }
            }
            DatePickerDialog(
                onDismissRequest = { showBirthdayPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        val selectedDate = selectedMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                        onBirthdayChange(selectedDate)
                        showBirthdayPicker = false
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBirthdayPicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        EducationEmploymentCard(
            selectedEducation = selectedEducation,
            onEducationSelected = {
                selectedEducation = it
                onEducationStatusChange(it)
            },
            educationExpanded = educationExpanded,
            onEducationExpandedChange = { educationExpanded = it },
            selectedEmployment = selectedEmployment,
            onEmploymentSelected = {
                selectedEmployment = it
                onEmploymentStatusChange(it)
            },
            employmentExpanded = employmentExpanded,
            onEmploymentExpandedChange = { employmentExpanded = it }
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Auto recommendations", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Let Sparely tune percentages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = autoModeEnabled, onCheckedChange = onAutoToggle)
                }
                recommendation?.let {
                    Text(
                        text = "Latest suggestion: ${formatPercent(it.recommendedPercentages.emergency)} emergency / ${formatPercent(it.recommendedPercentages.invest)} invest",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (!autoModeEnabled) {
                    Text("Manual percentages", style = MaterialTheme.typography.titleSmall)
                    SettingsSlider(label = "Emergency", value = emergency, onValueChange = { emergency = it })
                    SettingsSlider(label = "Invest", value = invest, onValueChange = { invest = it })
                    SettingsSlider(label = "Fun", value = funPercent, onValueChange = { funPercent = it })
                    Text(
                        text = "Total ${formatPercent((emergency + invest + funPercent).toDouble())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        RiskLevelCard(current = settings.riskLevel, onRiskChange = onRiskChange)

        LifeStageCard(age = age, onAgeChange = { updated ->
            age = updated
            onAgeChange(updated)
        })

        savingsPlan?.let { plan ->
            SavingsPlanCard(
                plan = plan,
                autoModeEnabled = autoModeEnabled,
                manualTransfers = manualTransfers,
                onLogTransfer = onLogTransfer
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Income & tax", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = monthlyIncomeText,
                    onValueChange = { monthlyIncomeText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Monthly income") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = { monthlyIncomeText.toDoubleOrNull()?.let(onMonthlyIncomeChange) }) {
                    Text("Update income")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Amounts include tax by default")
                    Switch(checked = settings.includeTaxByDefault, onCheckedChange = onIncludeTaxToggle)
                }
            }
        }

        RegionalSettingsCard(
            settings = settings,
            onRegionalSettingsChange = onRegionalSettingsChange
        )

        ReminderCard(
            remindersEnabled = remindersEnabled,
            reminderHour = reminderHour,
            reminderFrequency = reminderFrequency,
            onReminderChange = { enabled, hour, days ->
                remindersEnabled = enabled
                reminderHour = hour
                reminderFrequency = days
                onReminderChange(enabled, hour, days)
            }
        )

        if (alerts.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Insights", style = MaterialTheme.typography.titleMedium)
                    for (alert in alerts) {
                        Text(alert.title, style = MaterialTheme.typography.titleSmall)
                        Text(alert.description, style = MaterialTheme.typography.bodySmall)
                        HorizontalDivider()
                    }
                }
            }
        }
        Button(onClick = { onResetHistory(false) }, modifier = Modifier.fillMaxWidth()) {
            Text("Reset expense history")
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ProfileCard(
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    hasDebts: Boolean,
    onHasDebtsChange: (Boolean) -> Unit,
    emergencyFundText: String,
    onEmergencyFundChange: (String) -> Unit,
    primaryGoal: String,
    onPrimaryGoalChange: (String) -> Unit,
    birthday: LocalDate?,
    effectiveAge: Int,
    onEditBirthday: () -> Unit,
    onClearBirthday: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Profile basics", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = displayName,
                onValueChange = onDisplayNameChange,
                label = { Text("Display name (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Column {
                val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
                Text(
                    text = buildString {
                        append("Birthday: ")
                        append(birthday?.format(formatter) ?: "Add your birthday")
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onEditBirthday) { Text(if (birthday == null) "Set birthday" else "Change birthday") }
                    if (birthday != null) {
                        TextButton(onClick = onClearBirthday) { Text("Clear") }
                    }
                }
                Text(
                    text = "Current age: $effectiveAge",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Active debts", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "We adjust recommendations if debts are in play.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = hasDebts, onCheckedChange = onHasDebtsChange)
            }
            OutlinedTextField(
                value = emergencyFundText,
                onValueChange = { text ->
                    val filtered = text.filter { it.isDigit() || it == '.' }
                    onEmergencyFundChange(filtered)
                },
                label = { Text("Emergency fund saved") },
                prefix = { Text("$") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = primaryGoal,
                onValueChange = onPrimaryGoalChange,
                label = { Text("Primary savings goal") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EducationEmploymentCard(
    selectedEducation: EducationStatus,
    onEducationSelected: (EducationStatus) -> Unit,
    educationExpanded: Boolean,
    onEducationExpandedChange: (Boolean) -> Unit,
    selectedEmployment: EmploymentStatus,
    onEmploymentSelected: (EmploymentStatus) -> Unit,
    employmentExpanded: Boolean,
    onEmploymentExpandedChange: (Boolean) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Life context", style = MaterialTheme.typography.titleSmall)
            ExposedDropdownMenuBox(
                expanded = educationExpanded,
                onExpandedChange = onEducationExpandedChange
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    value = selectedEducation.displayLabel(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Education status") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = educationExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = educationExpanded,
                    onDismissRequest = { onEducationExpandedChange(false) }
                ) {
                    EducationStatus.entries.forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status.displayLabel()) },
                            onClick = {
                                onEducationSelected(status)
                                onEducationExpandedChange(false)
                            }
                        )
                    }
                }
            }
            ExposedDropdownMenuBox(
                expanded = employmentExpanded,
                onExpandedChange = onEmploymentExpandedChange
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    value = selectedEmployment.displayLabel(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Employment status") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = employmentExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = employmentExpanded,
                    onDismissRequest = { onEmploymentExpandedChange(false) }
                ) {
                    EmploymentStatus.entries.forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status.displayLabel()) },
                            onClick = {
                                onEmploymentSelected(status)
                                onEmploymentExpandedChange(false)
                            }
                        )
                    }
                }
            }
            Text(
                text = "Tweaking these helps Sparely tailor suggestions to your life stage.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsSlider(
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

@Composable
private fun SavingsPlanCard(
    plan: SavingsPlan,
    autoModeEnabled: Boolean,
    manualTransfers: List<SavingsTransfer>,
    onLogTransfer: (SavingsCategory, Double) -> Unit
) {
    val inputs = remember(plan.entries) {
        mutableStateMapOf<SavingsCategory, String>().apply {
            plan.entries.forEach { put(it.category, "") }
        }
    }
    val recentTransfers = remember(manualTransfers) {
        manualTransfers.sortedByDescending { it.date }.take(3)
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Monthly transfers", style = MaterialTheme.typography.titleSmall)
            Text(
                text = if (autoModeEnabled) {
                    "Sparely auto-adjusted these targets from your recent spending."
                } else {
                    "Targets follow the percentages you set manually."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Target ${formatCurrency(plan.totalTarget)} • Remaining ${formatCurrency(plan.totalRemaining)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            plan.entries.forEach { entry ->
                val amountText = inputs[entry.category] ?: ""
                val parsedAmount = amountText.toDoubleOrNull()
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(entry.category.displayName(), style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = buildString {
                            append("Target ")
                            append(formatCurrency(entry.targetAmount))
                            append(" • Set aside ")
                            append(formatCurrency(entry.alreadySetAside))
                            append(" • Remaining ")
                            append(formatCurrency(entry.remainingAmount))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (entry.recommendedSafeAmount != null && entry.recommendedHighRiskAmount != null) {
                        Text(
                            text = "Suggested split: ${formatCurrency(entry.recommendedSafeAmount)} safe / ${formatCurrency(entry.recommendedHighRiskAmount)} growth",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { newText ->
                            val filtered = newText.filter { it.isDigit() || it == '.' }
                            inputs[entry.category] = filtered
                        },
                        label = { Text("Log manual transfer") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                parsedAmount?.let {
                                    onLogTransfer(entry.category, it)
                                    inputs[entry.category] = ""
                                }
                            },
                            enabled = parsedAmount != null && parsedAmount > 0.0
                        ) {
                            Text("Log amount")
                        }
                        if (entry.remainingAmount > 0.0) {
                            TextButton(onClick = {
                                onLogTransfer(entry.category, entry.remainingAmount)
                                inputs[entry.category] = ""
                            }) {
                                Text("Log remaining ${formatCurrency(entry.remainingAmount)}")
                            }
                        }
                    }
                }
                HorizontalDivider()
            }
            if (recentTransfers.isNotEmpty()) {
                Text("Recent manual transfers", style = MaterialTheme.typography.titleSmall)
                recentTransfers.forEach { transfer ->
                    Text(
                        text = "${transfer.date.format(monthDayFormatter)} • ${transfer.category.displayName()} ${formatCurrency(transfer.amount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LifeStageCard(
    age: Int,
    onAgeChange: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Profile", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "Age $age",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = age.toFloat(),
                onValueChange = { raw ->
                    val coerced = raw.roundToInt().coerceIn(13, 80)
                    if (coerced != age) {
                        onAgeChange(coerced)
                    }
                },
                valueRange = 13f..80f,
                steps = 66
            )
            Text(
                text = "Under 18? Sparely reduces emergency savings suggestions and shifts focus toward study goals.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RiskLevelCard(
    current: RiskLevel,
    onRiskChange: (RiskLevel) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Risk profile", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (level in RiskLevel.values()) {
                    FilterChip(
                        selected = level == current,
                        onClick = { onRiskChange(level) },
                        label = { Text(level.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            Text(
                text = when (current) {
                    RiskLevel.CONSERVATIVE -> "Prioritises safety with higher emergency savings and safer investments."
                    RiskLevel.BALANCED -> "Balances growth and safety with diversified allocations."
                    RiskLevel.AGGRESSIVE -> "Focuses on growth with higher investing share and more risk tolerance."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReminderCard(
    remindersEnabled: Boolean,
    reminderHour: Int,
    reminderFrequency: Int,
    onReminderChange: (Boolean, Int, Int) -> Unit
) {
    var hour by remember(reminderHour) { mutableStateOf(reminderHour) }
    var frequency by remember(reminderFrequency) { mutableStateOf(reminderFrequency) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Daily reminders", style = MaterialTheme.typography.titleSmall)
                Switch(checked = remindersEnabled, onCheckedChange = { enabled ->
                    onReminderChange(enabled, hour, frequency)
                })
            }
            if (remindersEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hour (${hour}:00)")
                    Slider(
                        value = hour.toFloat(),
                        onValueChange = {
                            hour = it.toInt().coerceIn(0, 23)
                            onReminderChange(true, hour, frequency)
                        },
                        valueRange = 0f..23f,
                        steps = 22
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Frequency ${frequency} day(s)")
                    Slider(
                        value = frequency.toFloat(),
                        onValueChange = {
                            frequency = it.toInt().coerceIn(1, 7)
                            onReminderChange(true, hour, frequency)
                        },
                        valueRange = 1f..7f,
                        steps = 5
                    )
                }
            }
        }
    }
}

@Composable
private fun AutomationOverviewCard(
    settings: SparelySettings,
    activeSaveRate: Double,
    activeSavingTaxRate: Double,
    automationNotes: List<String>
) {
    val schedule = settings.paySchedule
    val payReference = listOf(schedule.lastPayAmount, schedule.defaultNetPay).firstOrNull { it > 0.0 } ?: 0.0
    val estimatedPerPay = (payReference * activeSaveRate).takeIf { payReference > 0.0 }
    val estimatedMonthly = settings.monthlyIncome.takeIf { it > 0.0 }?.let { it * activeSaveRate }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Automation at a glance", style = MaterialTheme.typography.titleSmall)

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(if (schedule.dynamicSaveRateEnabled) "Save rate: Automatic" else "Save rate: Manual") },
                    colors = AssistChipDefaults.assistChipColors(disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                )
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(if (settings.dynamicSavingTaxEnabled) "Saving tax: Automatic" else "Saving tax: Manual") },
                    colors = AssistChipDefaults.assistChipColors(disabledContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f))
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Paycheck save rate", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = formatPercent(activeSaveRate.coerceIn(0.0, 1.0)),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (estimatedPerPay != null) {
                    Text(
                        text = "≈ ${formatCurrency(estimatedPerPay)} moved each paycheck",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (estimatedMonthly != null) {
                    Text(
                        text = "≈ ${formatCurrency(estimatedMonthly)} per month at this pace",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Saving tax skim", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = formatPercent(activeSavingTaxRate.coerceIn(0.0, 1.0)),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Taken from each purchase before vault allocation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val notesToShow = automationNotes.take(4)
            if (notesToShow.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Why these numbers", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    notesToShow.forEach { note ->
                        Text(
                            text = "• ${note}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (automationNotes.size > notesToShow.size) {
                        Text(
                            text = "Automation insights trimmed — view full history from recent paychecks.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoDepositsCard(
    enabled: Boolean,
    checkHour: Int,
    onEnabledChange: (Boolean) -> Unit,
    onCheckHourChange: (Int) -> Unit,
    onManualTrigger: () -> Unit
) {
    var showHourPicker by remember { mutableStateOf(false) }
    var selectedHour by remember(checkHour) { mutableStateOf(checkHour) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-Deposit Checks",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = if (enabled) "Active" else "Disabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }

            if (enabled) {
                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showHourPicker = true }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daily check time",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${checkHour.toString().padStart(2, '0')}:00",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedButton(
                    onClick = onManualTrigger,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Check Now")
                }

                Text(
                    text = "Sparely checks daily for scheduled auto-deposits and creates pending contributions you can transfer manually.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Enable to automatically track scheduled vault deposits.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showHourPicker) {
        AlertDialog(
            onDismissRequest = { showHourPicker = false },
            title = { Text("Select check time") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose the hour of day (0-23) to check for due auto-deposits:")
                    Slider(
                        value = selectedHour.toFloat(),
                        onValueChange = { selectedHour = it.toInt() },
                        valueRange = 0f..23f,
                        steps = 22
                    )
                    Text(
                        text = "${selectedHour.toString().padStart(2, '0')}:00",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showHourPicker = false
                        onCheckHourChange(selectedHour)
                    }
                ) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHourPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun IncomeSettingsCard(
    schedule: PayScheduleSettings,
    activeSaveRate: Double,
    automationNotes: List<String>,
    onScheduleSave: (PayScheduleSettings) -> Unit,
    onRecordPaycheck: (Double, LocalDate, Boolean, Boolean) -> Unit
) {
        var trackingMode by remember(schedule) { mutableStateOf(schedule.trackingMode) }
        var interval by remember(schedule) { mutableStateOf(schedule.interval) }
        var defaultPayText by remember(schedule) {
            mutableStateOf(if (schedule.defaultNetPay > 0.0) String.format("%.2f", schedule.defaultNetPay) else "")
        }
        var dynamicSave by remember(schedule) { mutableStateOf(schedule.dynamicSaveRateEnabled) }
        var manualSaveRateSnapshot by remember(schedule) { mutableStateOf((schedule.defaultSaveRate * 100).toFloat()) }
        var saveRate by remember(schedule, activeSaveRate) {
            mutableStateOf(
                if (schedule.dynamicSaveRateEnabled) {
                    (activeSaveRate * 100).toFloat()
                } else {
                    (schedule.defaultSaveRate * 100).toFloat()
                }
            )
        }
        LaunchedEffect(schedule.dynamicSaveRateEnabled, activeSaveRate) {
            if (schedule.dynamicSaveRateEnabled) {
                saveRate = (activeSaveRate * 100).toFloat()
            }
        }
        var weeklyDay by remember(schedule) { mutableStateOf(schedule.weeklyDayOfWeek) }
        var semiDay1 by remember(schedule) { mutableStateOf(schedule.semiMonthlyDay1) }
        var semiDay2 by remember(schedule) { mutableStateOf(schedule.semiMonthlyDay2) }
        var monthlyDay by remember(schedule) { mutableStateOf(schedule.monthlyDay) }
        var customDays by remember(schedule) { mutableStateOf(schedule.customDaysBetween ?: 14) }
        var nextPayDate by remember(schedule) { mutableStateOf(schedule.nextPayDate) }
        var manualPayDate by remember(schedule) { mutableStateOf(schedule.nextPayDate ?: LocalDate.now()) }
        var autoDistribute by remember(schedule) { mutableStateOf(schedule.autoDistributeToVaults) }
        var autoPending by remember(schedule) { mutableStateOf(schedule.autoCreatePendingTransfers) }
        var manualAmountText by remember(schedule) {
            mutableStateOf(if (schedule.defaultNetPay > 0.0) String.format("%.2f", schedule.defaultNetPay) else "")
        }
        var manualDistribute by remember(schedule) { mutableStateOf(schedule.autoDistributeToVaults) }
        var manualPending by remember(schedule) { mutableStateOf(schedule.autoCreatePendingTransfers) }
        var showNextDatePicker by remember { mutableStateOf(false) }
        var showManualDatePicker by remember { mutableStateOf(false) }
        var intervalExpanded by remember { mutableStateOf(false) }

        fun buildSchedule(nextDateOverride: LocalDate? = nextPayDate): PayScheduleSettings {
            val defaultPayAmount = defaultPayText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
            val effectiveNext = if (trackingMode == IncomeTrackingMode.MANUAL_PER_PAYCHECK) null else nextDateOverride
            return schedule.copy(
                trackingMode = trackingMode,
                interval = interval,
                defaultNetPay = defaultPayAmount,
                defaultSaveRate = (manualSaveRateSnapshot / 100f).toDouble().coerceIn(0.0, 1.0),
                weeklyDayOfWeek = weeklyDay,
                semiMonthlyDay1 = semiDay1.coerceIn(1, 28),
                semiMonthlyDay2 = semiDay2.coerceIn(1, 28),
                monthlyDay = monthlyDay.coerceIn(1, 28),
                customDaysBetween = if (interval == PayInterval.CUSTOM) customDays.coerceAtLeast(1) else null,
                nextPayDate = effectiveNext,
                autoDistributeToVaults = autoDistribute,
                autoCreatePendingTransfers = autoPending,
                dynamicSaveRateEnabled = dynamicSave
            )
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Income & paydays", style = MaterialTheme.typography.titleSmall)

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        IncomeTrackingMode.MANUAL_PER_PAYCHECK to "Manual each paycheck",
                        IncomeTrackingMode.SCHEDULED to "Scheduled",
                        IncomeTrackingMode.HYBRID to "Hybrid"
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = trackingMode == mode,
                            onClick = {
                                trackingMode = mode
                                if (mode == IncomeTrackingMode.MANUAL_PER_PAYCHECK) {
                                    nextPayDate = null
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = intervalExpanded,
                    onExpandedChange = { intervalExpanded = !intervalExpanded }
                ) {
                    OutlinedTextField(
                        value = when (interval) {
                            PayInterval.WEEKLY -> "Weekly"
                            PayInterval.BIWEEKLY -> "Bi-weekly"
                            PayInterval.SEMI_MONTHLY -> "Semi-monthly"
                            PayInterval.MONTHLY -> "Monthly"
                            PayInterval.CUSTOM -> "Custom"
                        },
                        onValueChange = {},
                        label = { Text("Pay interval") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = intervalExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = intervalExpanded,
                        onDismissRequest = { intervalExpanded = false }
                    ) {
                        PayInterval.values().forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (option) {
                                            PayInterval.WEEKLY -> "Weekly"
                                            PayInterval.BIWEEKLY -> "Bi-weekly"
                                            PayInterval.SEMI_MONTHLY -> "Semi-monthly"
                                            PayInterval.MONTHLY -> "Monthly"
                                            PayInterval.CUSTOM -> "Custom"
                                        }
                                    )
                                },
                                onClick = {
                                    interval = option
                                    intervalExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = defaultPayText,
                    onValueChange = { text ->
                        defaultPayText = text.filter { ch -> ch.isDigit() || ch == '.' }
                    },
                    label = { Text("Typical net pay") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Automate savings rate", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = dynamicSave,
                        onCheckedChange = { enabled ->
                            dynamicSave = enabled
                            if (enabled) {
                                saveRate = (activeSaveRate * 100).toFloat()
                            } else {
                                saveRate = manualSaveRateSnapshot
                            }
                            onScheduleSave(buildSchedule())
                        }
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (dynamicSave) {
                            "Active savings rate ${formatPercent(activeSaveRate.coerceIn(0.0, 1.0))}"
                        } else {
                            "Default savings rate ${formatPercent((saveRate / 100f).toDouble())}"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = saveRate,
                        onValueChange = { updated ->
                            if (!dynamicSave) {
                                val coerced = updated.coerceIn(0f, 60f)
                                saveRate = coerced
                                manualSaveRateSnapshot = coerced
                            }
                        },
                        valueRange = 0f..60f,
                        enabled = !dynamicSave,
                        steps = 59
                    )
                    if (dynamicSave) {
                        Text(
                            text = "Sparely recalculates this target for every paycheck you log.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Adjust the slider, then tap \"Save pay defaults\" below to keep the change.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (dynamicSave && automationNotes.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Automation insights", style = MaterialTheme.typography.titleSmall)
                        automationNotes.take(4).forEach { note ->
                            Text(
                                text = "• $note",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                when (interval) {
                    PayInterval.WEEKLY, PayInterval.BIWEEKLY -> {
                        val days = DayOfWeek.values()
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            days.forEach { day ->
                                FilterChip(
                                    selected = weeklyDay == day,
                                    onClick = { weeklyDay = day },
                                    label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) }
                                )
                            }
                        }
                    }

                    PayInterval.SEMI_MONTHLY -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = semiDay1.toString(),
                                onValueChange = { value ->
                                    semiDay1 = value.toIntOrNull()?.coerceIn(1, 28) ?: semiDay1
                                },
                                label = { Text("First day") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = semiDay2.toString(),
                                onValueChange = { value ->
                                    semiDay2 = value.toIntOrNull()?.coerceIn(1, 28) ?: semiDay2
                                },
                                label = { Text("Second day") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    PayInterval.MONTHLY -> {
                        OutlinedTextField(
                            value = monthlyDay.toString(),
                            onValueChange = { value ->
                                monthlyDay = value.toIntOrNull()?.coerceIn(1, 28) ?: monthlyDay
                            },
                            label = { Text("Payday (day of month)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    PayInterval.CUSTOM -> {
                        OutlinedTextField(
                            value = customDays.toString(),
                            onValueChange = { value ->
                                customDays = value.toIntOrNull()?.coerceAtLeast(1) ?: customDays
                            },
                            label = { Text("Days between paychecks") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Next payday", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = nextPayDate?.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) ?: "Not set",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { showNextDatePicker = true }) {
                        Text("Pick date")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto distribute to vaults", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = autoDistribute, onCheckedChange = { autoDistribute = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Create pending transfers", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = autoPending, onCheckedChange = { autoPending = it })
                }

                Button(onClick = { onScheduleSave(buildSchedule()) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Save pay defaults")
                }

                HorizontalDivider()

                Text("Log a paycheck", style = MaterialTheme.typography.titleSmall)

                OutlinedTextField(
                    value = manualAmountText,
                    onValueChange = { text ->
                        manualAmountText = text.filter { ch -> ch.isDigit() || ch == '.' }
                    },
                    label = { Text("Amount received") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Payday", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            manualPayDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { showManualDatePicker = true }) {
                        Text("Pick date")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Distribute using vault plan", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = manualDistribute, onCheckedChange = { manualDistribute = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Keep contributions pending", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = manualPending, onCheckedChange = { manualPending = it })
                }

                val manualAmount = manualAmountText.toDoubleOrNull()?.coerceAtLeast(0.0)
                Button(
                    onClick = {
                        manualAmount?.let {
                            onRecordPaycheck(it, manualPayDate, manualDistribute, manualPending)
                            manualAmountText = ""
                        }
                    },
                    enabled = manualAmount != null && manualAmount > 0.0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Record paycheck")
                }

                schedule.lastPayDate?.let { lastDate ->
                    val lastAmount = formatCurrency(schedule.lastPayAmount)
                    Text(
                        text = "Last logged pay: ${lastAmount} on ${lastDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (showNextDatePicker) {
            val initialMillis = nextPayDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
            LaunchedEffect(initialMillis) {
                if (initialMillis != null && datePickerState.selectedDateMillis != initialMillis) {
                    datePickerState.selectedDateMillis = initialMillis
                }
            }
            DatePickerDialog(
                onDismissRequest = { showNextDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val selected = datePickerState.selectedDateMillis?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        nextPayDate = selected
                        if (selected != null) {
                            manualPayDate = selected
                        }
                        if (trackingMode != IncomeTrackingMode.MANUAL_PER_PAYCHECK) {
                            onScheduleSave(buildSchedule(selected))
                        }
                        showNextDatePicker = false
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNextDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (showManualDatePicker) {
            val initialMillis = manualPayDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
            DatePickerDialog(
                onDismissRequest = { showManualDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val selected = datePickerState.selectedDateMillis?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        if (selected != null) {
                            manualPayDate = selected
                        }
                        showManualDatePicker = false
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showManualDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
}

private val monthDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")

private fun SavingsCategory.displayName(): String = when (this) {
    SavingsCategory.EMERGENCY -> "Emergency buffer"
    SavingsCategory.INVESTMENT -> "Study & investing"
    SavingsCategory.FUN -> "Fun"
}

private fun formatCurrency(amount: Double): String = NumberFormat.getCurrencyInstance().format(amount)

private fun formatPercent(value: Double): String = String.format("%.1f%%", value.coerceIn(0.0, 1.0) * 100)

private fun EducationStatus.displayLabel(): String = when (this) {
    EducationStatus.HIGH_SCHOOL -> "High school"
    EducationStatus.UNIVERSITY -> "University/College"
    EducationStatus.GRADUATED -> "Graduated"
    EducationStatus.OTHER -> "Other"
}

private fun EmploymentStatus.displayLabel(): String = when (this) {
    EmploymentStatus.STUDENT -> "Student"
    EmploymentStatus.PART_TIME -> "Part-time"
    EmploymentStatus.FULL_TIME, EmploymentStatus.EMPLOYED -> "Full-time"
    EmploymentStatus.SELF_EMPLOYED -> "Self-employed"
    EmploymentStatus.UNEMPLOYED -> "Unemployed"
    EmploymentStatus.RETIRED -> "Retired"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegionalSettingsCard(
    settings: SparelySettings,
    onRegionalSettingsChange: (String, String, String, Double?) -> Unit
) {
    val regionalSettings = settings.regionalSettings
    val currentCountry = regionalSettings.getCountryConfig()
    
    var showCountryPicker by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var customTaxRate by remember(regionalSettings.customIncomeTaxRate) {
        mutableStateOf(regionalSettings.customIncomeTaxRate?.let { (it * 100).toString() } ?: "")
    }
    
    val allCountries = listOf(
        com.example.sparely.domain.model.CountryProfiles.UNITED_STATES,
        com.example.sparely.domain.model.CountryProfiles.UNITED_KINGDOM,
        com.example.sparely.domain.model.CountryProfiles.CANADA,
        com.example.sparely.domain.model.CountryProfiles.FRANCE,
        com.example.sparely.domain.model.CountryProfiles.GERMANY,
        com.example.sparely.domain.model.CountryProfiles.SPAIN,
        com.example.sparely.domain.model.CountryProfiles.JAPAN,
        com.example.sparely.domain.model.CountryProfiles.AUSTRALIA,
        com.example.sparely.domain.model.CountryProfiles.INDIA,
        com.example.sparely.domain.model.CountryProfiles.MEXICO,
        com.example.sparely.domain.model.CountryProfiles.BRAZIL
    )
    
    // Available languages for the selected country
    val availableLanguages = when (regionalSettings.countryCode) {
        "CA" -> listOf("en" to "English", "fr" to "Français")
        "BE" -> listOf("en" to "English", "fr" to "Français", "nl" to "Nederlands")
        "CH" -> listOf("en" to "English", "fr" to "Français", "de" to "Deutsch", "it" to "Italiano")
        else -> listOf(
            "en" to "English",
            "es" to "Español",
            "fr" to "Français",
            "de" to "Deutsch",
            "pt" to "Português",
            "ja" to "日本語"
        )
    }
    
    // Available currencies
    val availableCurrencies = listOf("USD", "CAD", "GBP", "EUR", "JPY", "AUD", "INR", "MXN", "BRL")
    
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Regional Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Country Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Country",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = currentCountry?.countryName ?: regionalSettings.countryCode,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = { showCountryPicker = true }) {
                    Text("Change")
                }
            }
            
            // Language Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Language",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = availableLanguages.find { it.first == regionalSettings.languageCode }?.second 
                            ?: regionalSettings.languageCode,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = { showLanguagePicker = true }) {
                    Text("Change")
                }
            }
            
            // Currency Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Currency",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${regionalSettings.currencyCode} (${getCurrencySymbol(regionalSettings.currencyCode)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = { showCurrencyPicker = true }) {
                    Text("Change")
                }
            }
            
            HorizontalDivider()
            
            // Custom Tax Rate
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Custom Income Tax Rate",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Override the default tax rate for ${currentCountry?.countryName ?: "your country"}. " +
                            "Default: ${currentCountry?.taxConfig?.incomeTaxRate?.times(100)?.toInt() ?: 0}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customTaxRate,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d{0,2}(\\.\\d{0,2})?$"))) {
                                customTaxRate = newValue
                            }
                        },
                        label = { Text("Tax Rate (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedButton(
                        onClick = {
                            val taxRate = customTaxRate.toDoubleOrNull()?.div(100)
                            onRegionalSettingsChange(
                                regionalSettings.countryCode,
                                regionalSettings.languageCode,
                                regionalSettings.currencyCode,
                                taxRate
                            )
                        }
                    ) {
                        Text("Save")
                    }
                    if (customTaxRate.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                customTaxRate = ""
                                onRegionalSettingsChange(
                                    regionalSettings.countryCode,
                                    regionalSettings.languageCode,
                                    regionalSettings.currencyCode,
                                    null
                                )
                            }
                        ) {
                            Text("Reset")
                        }
                    }
                }
            }
        }
    }
    
    // Country Picker Dialog
    if (showCountryPicker) {
        AlertDialog(
            onDismissRequest = { showCountryPicker = false },
            title = { Text("Select Country") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allCountries.forEach { country ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onRegionalSettingsChange(
                                        country.countryCode,
                                        country.languageCode,
                                        country.defaultCurrency,
                                        regionalSettings.customIncomeTaxRate
                                    )
                                    showCountryPicker = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (country.countryCode == regionalSettings.countryCode)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = country.countryName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${country.defaultCurrency} • ${country.languageName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCountryPicker = false }) {
                    Text("Close")
                }
            }
        )
    }
    
    // Language Picker Dialog
    if (showLanguagePicker) {
        AlertDialog(
            onDismissRequest = { showLanguagePicker = false },
            title = { Text("Select Language") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableLanguages.forEach { (code, name) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onRegionalSettingsChange(
                                        regionalSettings.countryCode,
                                        code,
                                        regionalSettings.currencyCode,
                                        regionalSettings.customIncomeTaxRate
                                    )
                                    showLanguagePicker = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (code == regionalSettings.languageCode)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(
                                text = name,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguagePicker = false }) {
                    Text("Close")
                }
            }
        )
    }
    
    // Currency Picker Dialog
    if (showCurrencyPicker) {
        AlertDialog(
            onDismissRequest = { showCurrencyPicker = false },
            title = { Text("Select Currency") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableCurrencies.forEach { currency ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onRegionalSettingsChange(
                                        regionalSettings.countryCode,
                                        regionalSettings.languageCode,
                                        currency,
                                        regionalSettings.customIncomeTaxRate
                                    )
                                    showCurrencyPicker = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (currency == regionalSettings.currencyCode)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(
                                text = "$currency (${getCurrencySymbol(currency)})",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyPicker = false }) {
                    Text("Close")
                }
            }
        )
    }
}

private fun getCurrencySymbol(currencyCode: String): String = when (currencyCode) {
    "USD", "CAD", "AUD", "MXN" -> "$"
    "GBP" -> "£"
    "EUR" -> "€"
    "JPY" -> "¥"
    "INR" -> "₹"
    "BRL" -> "R$"
    else -> currencyCode
}
