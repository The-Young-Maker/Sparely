package com.example.sparely.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.sparely.domain.model.ExpenseCategory
import com.example.sparely.domain.model.RecurringExpense
import com.example.sparely.domain.model.RecurringExpenseInput
import com.example.sparely.domain.model.RecurringFrequency
import com.example.sparely.domain.model.SmartVault
import com.example.sparely.ui.theme.MaterialSymbolIcon
import com.example.sparely.ui.theme.MaterialSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private enum class RecurringOverviewMode {
    OVERVIEW,
    SMART_REMINDERS,
    AUTO_LOGGING,
    UPCOMING
}

private data class RecurringHighlight(val title: String, val detail: String? = null)

private data class RecurringUpcomingPreview(
    val expense: RecurringExpense,
    val dueDate: LocalDate,
    val daysUntil: Int
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecurringExpensesScreen(
    recurringExpenses: List<RecurringExpense>,
    smartVaults: List<SmartVault> = emptyList(),
    onAddRecurring: (RecurringExpenseInput) -> Unit,
    onUpdateRecurring: (RecurringExpense) -> Unit,
    onDeleteRecurring: (Long) -> Unit,
    onMarkProcessed: (Long) -> Unit
) {
    var isDialogVisible by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<RecurringExpense?>(null) }
    var overviewMode by remember { mutableStateOf(RecurringOverviewMode.OVERVIEW) }

    val sortedExpenses = remember(recurringExpenses) {
        recurringExpenses.sortedBy { calculateNextDue(it) ?: it.startDate }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                RecurringOverviewCard(
                    expenses = sortedExpenses,
                    selectedMode = overviewMode,
                    onModeChange = { overviewMode = it }
                )
            }
            items(sortedExpenses, key = { it.id }) { expense ->
                RecurringExpenseRow(
                    expense = expense,
                    onEdit = {
                        editingExpense = expense
                        isDialogVisible = true
                    },
                    onDelete = { onDeleteRecurring(expense.id) },
                    onToggleActive = { active ->
                        onUpdateRecurring(expense.copy(isActive = active))
                    },
                    onMarkProcessed = { onMarkProcessed(expense.id) }
                )
            }
            if (sortedExpenses.isEmpty()) {
                item {
                    EmptyRecurringState(onAddRecurring = {
                        editingExpense = null
                        isDialogVisible = true
                    })
                }
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }

        FloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            onClick = {
                editingExpense = null
                isDialogVisible = true
            }
        ) {
            MaterialSymbolIcon(icon = MaterialSymbols.ADD, contentDescription = "Add recurring expense")
        }
    }

    if (isDialogVisible) {
        RecurringExpenseDialog(
            expense = editingExpense,
            smartVaults = smartVaults,
            onDismiss = {
                isDialogVisible = false
                editingExpense = null
            },
            onConfirm = { input, existing ->
                if (existing == null) {
                    onAddRecurring(input)
                } else {
                    onUpdateRecurring(
                        existing.copy(
                            description = input.description,
                            amount = input.amount,
                            category = input.category,
                            frequency = input.frequency,
                            startDate = input.startDate,
                            endDate = input.endDate,
                            autoLog = input.autoLog,
                            reminderDaysBefore = input.reminderDaysBefore,
                            notes = input.notes,
                            includesTax = input.includesTax,
                            deductFromMainAccount = input.deductFromMainAccount,
                            deductedFromVaultId = input.deductedFromVaultId,
                            manualPercentages = input.manualPercentages
                        )
                    )
                }
                isDialogVisible = false
                editingExpense = null
            }
        )
    }
}

@Composable
private fun RecurringOverviewCard(
    expenses: List<RecurringExpense>,
    selectedMode: RecurringOverviewMode,
    onModeChange: (RecurringOverviewMode) -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    val today = LocalDate.now()
    val activeExpenses = expenses.filter { it.isActive }
    val pausedCount = expenses.count { !it.isActive }
    val upcomingPreviews = activeExpenses.mapNotNull { expense ->
        calculateNextDue(expense, today)?.let { dueDate ->
            val daysUntil = ChronoUnit.DAYS.between(today, dueDate).toInt()
            RecurringUpcomingPreview(expense, dueDate, daysUntil)
        }
    }.sortedBy { it.dueDate }
    val reminderMatches = upcomingPreviews.filter { preview ->
        val lead = preview.expense.reminderDaysBefore
        preview.daysUntil >= 0 && preview.daysUntil <= lead
    }
    val autoLogActive = activeExpenses.filter { it.autoLog }
    val autoLogUpcoming = upcomingPreviews.filter { it.expense.autoLog }

    val highlights = when (selectedMode) {
        RecurringOverviewMode.OVERVIEW -> listOfNotNull(
            RecurringHighlight(
                title = "Recurring payments",
                detail = "${activeExpenses.size} active • $pausedCount paused"
            ),
            RecurringHighlight(
                title = "Auto-logging enabled",
                detail = "${autoLogActive.size} using auto-log"
            ),
            upcomingPreviews.firstOrNull()?.let {
                RecurringHighlight(
                    title = "Next due",
                    detail = "${it.expense.description} • ${it.dueDate.format(dateFormatter)} (${formatCountdown(it.daysUntil)})"
                )
            } ?: RecurringHighlight(
                title = "Next due",
                detail = "No upcoming payments scheduled"
            )
        )
        RecurringOverviewMode.SMART_REMINDERS -> if (reminderMatches.isEmpty()) {
            listOf(RecurringHighlight("All clear", "No reminders scheduled right now."))
        } else {
            reminderMatches.take(3).map {
                RecurringHighlight(
                    title = it.expense.description,
                    detail = "Reminder ${it.expense.reminderDaysBefore}d before • due ${it.dueDate.format(dateFormatter)} (${formatCountdown(it.daysUntil)})"
                )
            }
        }
        RecurringOverviewMode.AUTO_LOGGING -> when {
            autoLogActive.isEmpty() -> listOf(RecurringHighlight("Auto-log is off", "Enable auto-log when editing a recurring payment."))
            autoLogUpcoming.isNotEmpty() -> autoLogUpcoming.take(3).map {
                RecurringHighlight(
                    title = it.expense.description,
                    detail = "Auto-logs ${it.dueDate.format(dateFormatter)} (${formatCountdown(it.daysUntil)})"
                )
            }
            else -> autoLogActive.take(3).map {
                RecurringHighlight(
                    title = it.description,
                    detail = "Auto-log ready when the next cycle starts."
                )
            }
        }
        RecurringOverviewMode.UPCOMING -> if (upcomingPreviews.isEmpty()) {
            listOf(RecurringHighlight("Nothing scheduled", "Add a recurring payment to see what's coming."))
        } else {
            upcomingPreviews.take(3).map {
                RecurringHighlight(
                    title = it.expense.description,
                    detail = "Due ${it.dueDate.format(dateFormatter)} (${formatCountdown(it.daysUntil)})"
                )
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Recurring insights",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tap a chip to explore reminders, auto-logging, or upcoming charges.",
                style = MaterialTheme.typography.bodyMedium
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RecurringOverviewMode.entries.forEach { mode ->
                    FilterChip(
                        selected = selectedMode == mode,
                        onClick = { onModeChange(mode) },
                        label = { Text(mode.displayName()) }
                    )
                }
            }
            highlights.forEach { highlight ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(highlight.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    highlight.detail?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringExpenseRow(
    expense: RecurringExpense,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onMarkProcessed: () -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    val nextDue = calculateNextDue(expense)
    val daysUntil = nextDue?.let { ChronoUnit.DAYS.between(LocalDate.now(), it).toInt() }

    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.description,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = expense.category.displayName(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatCurrency(expense.amount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onEdit) {
                        MaterialSymbolIcon(icon = MaterialSymbols.EDIT, contentDescription = "Edit recurring expense")
                    }
                    IconButton(onClick = onDelete) {
                        MaterialSymbolIcon(icon = MaterialSymbols.DELETE, contentDescription = "Delete recurring expense")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${expense.frequency.displayName()} • ${if (expense.autoLog) "Auto-log on" else "Manual confirm"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    nextDue?.let {
                        Text(
                            text = "Next due ${it.format(formatter)}${daysUntil?.let { days -> " (${days}d)" } ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MaterialSymbolIcon(icon = MaterialSymbols.NOTIFICATIONS,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${expense.reminderDaysBefore}d before", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Active", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = expense.isActive,
                            onCheckedChange = onToggleActive,
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onMarkProcessed) {
                    Text("Mark as paid")
                }
                if (!expense.notes.isNullOrBlank()) {
                    Text(
                        text = expense.notes.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyRecurringState(onAddRecurring: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "No recurring payments yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Keep tabs on subscriptions and bills and let Sparely remind you before they hit your account.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Button(onClick = onAddRecurring) {
                Text("Add recurring payment")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringExpenseDialog(
    expense: RecurringExpense?,
    smartVaults: List<SmartVault>,
    onDismiss: () -> Unit,
    onConfirm: (RecurringExpenseInput, RecurringExpense?) -> Unit
) {
    var description by remember { mutableStateOf(expense?.description.orEmpty()) }
    var amountText by remember { mutableStateOf(if (expense != null) "${expense.amount}" else "") }
    var category by remember { mutableStateOf(expense?.category ?: ExpenseCategory.OTHER) }
    var frequency by remember { mutableStateOf(expense?.frequency ?: RecurringFrequency.MONTHLY) }
    var startDate by remember { mutableStateOf(expense?.startDate ?: LocalDate.now()) }
    var endDateText by remember { mutableStateOf(expense?.endDate?.toString().orEmpty()) }
    var reminderDays by remember { mutableStateOf(expense?.reminderDaysBefore?.toString() ?: "2") }
    var autoLog by remember { mutableStateOf(expense?.autoLog ?: true) }
    var executeAutomatically by remember { mutableStateOf(expense?.executeAutomatically ?: false) }
    var notes by remember { mutableStateOf(expense?.notes.orEmpty()) }
    var includesTax by remember { mutableStateOf(expense?.includesTax ?: false) }
    var deductFromMainAccount by remember { mutableStateOf(expense?.deductFromMainAccount ?: false) }
    var deductFromVaultId by remember { mutableStateOf(expense?.deductedFromVaultId) }
    var vaultDropdownExpanded by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    val activeVaults = remember(smartVaults) { smartVaults.filter { !it.archived } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (expense == null) "Add recurring payment" else "Edit recurring payment") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (showError) {
                    Text(
                        text = "Please fill out all required fields correctly.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Amount") },
                    singleLine = true
                )
                CategorySelector(selected = category, onSelected = { category = it })
                FrequencySelector(selected = frequency, onSelected = { frequency = it })
                DateSelector(
                    label = "Start date",
                    date = startDate,
                    onDateSelected = { startDate = it }
                )
                OutlinedTextField(
                    value = endDateText,
                    onValueChange = { endDateText = it },
                    label = { Text("End date (optional, YYYY-MM-DD)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = reminderDays,
                    onValueChange = { reminderDays = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Reminder days before") },
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Auto-log to history", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = autoLog, onCheckedChange = { autoLog = it })
                }
                
                // Expense-related fields
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Includes tax", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = includesTax, onCheckedChange = { includesTax = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Execute automatically when due", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = executeAutomatically, onCheckedChange = { executeAutomatically = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Deduct from main account", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = deductFromMainAccount, onCheckedChange = { deductFromMainAccount = it })
                }
                
                // Vault selection dropdown (matching ExpenseEntryScreen)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Deduct from vault (optional)", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = if (activeVaults.isEmpty()) "No active vaults available" else "Choose a vault to deduct from",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (activeVaults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        ExposedDropdownMenuBox(
                            expanded = vaultDropdownExpanded,
                            onExpandedChange = { vaultDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
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
                                activeVaults.forEach { vault ->
                                    DropdownMenuItem(
                                        text = { 
                                            Column {
                                                Text(vault.name)
                                                Text(
                                                    text = "Balance: $${String.format("%.2f", vault.currentBalance)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            deductFromVaultId = vault.id
                                            vaultDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = false
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = amountText.toDoubleOrNull()
                val reminder = reminderDays.toIntOrNull()
                val endDate = endDateText.takeIf { it.isNotBlank() }?.let run@{
                    kotlin.runCatching { LocalDate.parse(it) }.getOrNull()
                }
                if (description.isBlank() || amount == null || amount <= 0 || reminder == null) {
                    showError = true
                    return@TextButton
                }
                val input = RecurringExpenseInput(
                    description = description.trim(),
                    amount = amount,
                    category = category,
                    frequency = frequency,
                    startDate = startDate,
                    endDate = endDate,
                    autoLog = autoLog,
                    executeAutomatically = executeAutomatically,
                    reminderDaysBefore = reminder,
                    notes = notes.takeIf { it.isNotBlank() },
                    includesTax = includesTax,
                    deductFromMainAccount = deductFromMainAccount,
                    deductedFromVaultId = deductFromVaultId
                )
                onConfirm(input, expense)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySelector(selected: ExpenseCategory, onSelected: (ExpenseCategory) -> Unit) {
    Column {
        Text("Category", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ExpenseCategory.entries.forEach { category ->
                AssistChip(
                    onClick = { onSelected(category) },
                    label = { Text(category.displayName()) },
                    enabled = true,
                    border = if (selected == category) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FrequencySelector(selected: RecurringFrequency, onSelected: (RecurringFrequency) -> Unit) {
    Column {
        Text("Frequency", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RecurringFrequency.entries.forEach { frequency ->
                AssistChip(
                    onClick = { onSelected(frequency) },
                    label = { Text(frequency.displayName()) },
                    enabled = true,
                    border = if (selected == frequency) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelector(label: String, date: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    val formatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    var showDialog by remember { mutableStateOf(false) }
    val millis = remember(date) { date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = millis)
    LaunchedEffect(millis) {
        pickerState.selectedDateMillis = millis
    }

    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(4.dp))
        TextButton(onClick = { showDialog = true }) {
            Text(date.format(formatter))
        }
    }

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millisSelected ->
                        val selectedDate = Instant.ofEpochMilli(millisSelected).atZone(ZoneOffset.UTC).toLocalDate()
                        onDateSelected(selectedDate)
                    }
                    showDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private fun RecurringFrequency.displayName(): String = name.lowercase().replaceFirstChar { it.titlecase() }
fun ExpenseCategory.displayName(): String = name.lowercase().replaceFirstChar { it.titlecase() }

private fun RecurringOverviewMode.displayName(): String = when (this) {
    RecurringOverviewMode.OVERVIEW -> "Overview"
    RecurringOverviewMode.SMART_REMINDERS -> "Smart reminders"
    RecurringOverviewMode.AUTO_LOGGING -> "Auto-logging"
    RecurringOverviewMode.UPCOMING -> "Upcoming"
}

private fun formatCountdown(daysUntil: Int): String = when {
    daysUntil < 0 -> "overdue by ${-daysUntil}d"
    daysUntil == 0 -> "due today"
    daysUntil == 1 -> "in 1 day"
    else -> "in ${daysUntil}d"
}

private fun calculateNextDue(expense: RecurringExpense, today: LocalDate = LocalDate.now()): LocalDate? {
    if (!expense.isActive) return null
    val interval = expense.frequency.daysInterval.toLong().coerceAtLeast(1)
    var nextDue = expense.lastProcessedDate?.plusDays(interval) ?: expense.startDate
    if (nextDue.isBefore(today)) {
        val diff = ChronoUnit.DAYS.between(nextDue, today)
        val steps = (diff / interval) + 1
        nextDue = nextDue.plusDays(steps * interval)
    }
    expense.endDate?.let { if (nextDue.isAfter(it)) return null }
    return nextDue
}

private fun formatCurrency(value: Double): String = "$" + String.format("%,.2f", value)
