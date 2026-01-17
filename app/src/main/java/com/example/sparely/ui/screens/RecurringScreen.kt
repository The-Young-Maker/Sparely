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
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.FilledTonalButton
import com.example.sparely.ui.components.SparelyTextField
import com.example.sparely.ui.components.SparelyChip
import com.example.sparely.domain.model.PaymentMethod
import com.example.sparely.ui.components.PaymentMethodSelector
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import com.example.sparely.ui.utils.toSafeDatePickerMillis
import com.example.sparely.ui.utils.filterCurrencyInput
import com.example.sparely.ui.utils.toSafeDouble
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
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.sparely.domain.model.ExpenseCategory
import com.example.sparely.domain.model.RecurringExpense
import com.example.sparely.domain.model.RecurringExpenseInput
import com.example.sparely.domain.model.RecurringFrequency
import com.example.sparely.domain.model.SmartVault
import com.example.sparely.domain.model.Store
import com.example.sparely.domain.model.StoreInput
import com.example.sparely.domain.model.displayName
import com.example.sparely.domain.model.predictNextAmount
import com.example.sparely.ui.components.SearchableStoreSelector
import com.example.sparely.ui.components.SparelyButton
import com.example.sparely.ui.components.SparelyTextButton
import com.example.sparely.ui.components.SparelyTonalButton
import com.example.sparely.ui.theme.MaterialSymbolIcon
import com.example.sparely.ui.theme.MaterialSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import com.sparely.app.R
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
    stores: List<Store> = emptyList(),
    onAddRecurring: (RecurringExpenseInput) -> Unit,
    onUpdateRecurring: (RecurringExpense) -> Unit,
    onDeleteRecurring: (Long) -> Unit,
    onMarkProcessed: (Long) -> Unit,
    onCreateStore: suspend (StoreInput) -> Store? = { null },
    onEditStore: (Store) -> Unit = {},
    onDeleteStore: (Store) -> Unit = {},
    brandfetchClientId: String? = null,
    paymentMethods: List<PaymentMethod> = emptyList(),
    onManagePaymentMethods: () -> Unit = {},
    brandSearchResults: List<com.example.sparely.data.remote.BrandfetchBrand> = emptyList(),
    onBrandSearch: (String) -> Unit = {}
) {
    var isDialogVisible by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<RecurringExpense?>(null) }
    var expenseToDelete by remember { mutableStateOf<RecurringExpense?>(null) }
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
                val store = stores.find { it.id == expense.storeId }
                RecurringExpenseRow(
                    expense = expense,
                    store = store,
                    brandfetchClientId = brandfetchClientId,
                    onEdit = {
                        editingExpense = expense
                        isDialogVisible = true
                    },
                    onDelete = { expenseToDelete = expense }, // Set expense to delete
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

        androidx.compose.material3.FloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            onClick = {
                editingExpense = null
                isDialogVisible = true
            },
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            MaterialSymbolIcon(icon = MaterialSymbols.ADD, contentDescription = stringResource(R.string.recurring_add_title))
        }
    }

    if (isDialogVisible) {
        RecurringExpenseDialog(
            expense = editingExpense,
            smartVaults = smartVaults,
            stores = stores,
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
                            storeId = input.storeId,
                            includesTax = input.includesTax,
                            deductFromMainAccount = input.deductFromMainAccount,
                            deductedFromVaultId = input.deductedFromVaultId,
                            manualPercentages = input.manualPercentages,
                            executeAutomatically = input.executeAutomatically,
                            paymentMethodId = input.paymentMethodId
                        )
                    )
                }
                isDialogVisible = false
                editingExpense = null
            },
            onCreateStore = onCreateStore,
            onEditStore = onEditStore,
            onDeleteStore = onDeleteStore,
            brandfetchClientId = brandfetchClientId,
            paymentMethods = paymentMethods,
            onManagePaymentMethods = onManagePaymentMethods,
            brandSearchResults = brandSearchResults,
            onBrandSearch = onBrandSearch
        )
    }

    if (expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text(stringResource(R.string.recurring_delete_confirm_title)) },
            text = { Text(stringResource(R.string.recurring_delete_confirm_message, expenseToDelete?.description.orEmpty())) },
            confirmButton = {
                SparelyTextButton(onClick = {
                    expenseToDelete?.let { onDeleteRecurring(it.id) }
                    expenseToDelete = null
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                SparelyTextButton(onClick = { expenseToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
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
                title = stringResource(R.string.recurring_title),
                detail = stringResource(R.string.recurring_active_paused_count, activeExpenses.size, pausedCount)
            ),
            RecurringHighlight(
                title = stringResource(R.string.recurring_auto_log_enabled),
                detail = stringResource(R.string.recurring_auto_log_count, autoLogActive.size)
            ),
            upcomingPreviews.firstOrNull()?.let {
                RecurringHighlight(
                    title = stringResource(R.string.recurring_next_due_label),
                    detail = stringResource(R.string.recurring_next_due_stat, it.expense.description, it.dueDate.format(dateFormatter), formatCountdown(it.daysUntil))
                )
            } ?: RecurringHighlight(
                title = stringResource(R.string.recurring_next_due_label),
                detail = stringResource(R.string.recurring_no_upcoming)
            )
        )
        RecurringOverviewMode.SMART_REMINDERS -> if (reminderMatches.isEmpty()) {
            listOf(RecurringHighlight(stringResource(R.string.recurring_reminders_all_clear), stringResource(R.string.recurring_reminders_none)))
        } else {
            reminderMatches.take(3).map {
                RecurringHighlight(
                    title = it.expense.description,
                    detail = stringResource(R.string.recurring_reminder_stat, it.expense.reminderDaysBefore, it.dueDate.format(dateFormatter), formatCountdown(it.daysUntil))
                )
            }
        }
        RecurringOverviewMode.AUTO_LOGGING -> when {
            autoLogActive.isEmpty() -> listOf(RecurringHighlight(stringResource(R.string.recurring_auto_log_off), stringResource(R.string.recurring_auto_log_tutorial)))
            autoLogUpcoming.isNotEmpty() -> autoLogUpcoming.take(3).map {
                RecurringHighlight(
                    title = it.expense.description,
                    detail = stringResource(R.string.recurring_auto_log_stat, it.dueDate.format(dateFormatter), formatCountdown(it.daysUntil))
                )
            }
            else -> autoLogActive.take(3).map {
                RecurringHighlight(
                    title = it.description,
                    detail = stringResource(R.string.recurring_auto_log_ready)
                )
            }
        }
        RecurringOverviewMode.UPCOMING -> if (upcomingPreviews.isEmpty()) {
            listOf(RecurringHighlight(stringResource(R.string.recurring_nothing_scheduled), stringResource(R.string.recurring_add_suggestion)))
        } else {
            upcomingPreviews.take(3).map {
                RecurringHighlight(
                    title = it.expense.description,
                    detail = stringResource(R.string.recurring_due_stat, it.dueDate.format(dateFormatter), formatCountdown(it.daysUntil))
                )
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(R.string.recurring_insights_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.recurring_insights_desc),
                style = MaterialTheme.typography.bodyMedium
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (mode in RecurringOverviewMode.entries) {
                    SparelyChip(
                        selected = selectedMode == mode,
                        onClick = { onModeChange(mode) },
                        label = { Text(mode.displayName()) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            for (highlight in highlights) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(highlight.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    highlight.detail?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
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
    store: Store?,
    brandfetchClientId: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onMarkProcessed: () -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("MMM d") }
    val nextDue = calculateNextDue(expense)
    val daysUntil = nextDue?.let { ChronoUnit.DAYS.between(LocalDate.now(), it).toInt() }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Box
                val hasStoreLogo = store?.getBrandfetchLogoUrl(brandfetchClientId) != null
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = if (hasStoreLogo) androidx.compose.ui.graphics.Color.Transparent else getCategoryColor(expense.category).copy(alpha = 0.15f),
                    modifier = Modifier.size(56.dp)
                ) {
                     Box(contentAlignment = Alignment.Center) {
                         if (hasStoreLogo) {
                             com.example.sparely.ui.components.StoreIcon(
                                 store = store!!,
                                 brandfetchClientId = brandfetchClientId,
                                 size = 56
                             )
                         } else {
                             MaterialSymbolIcon(
                                 icon = getCategoryIcon(expense.category),
                                 contentDescription = null,
                                 tint = getCategoryColor(expense.category),
                                 size = 28.dp
                             )
                         }
                     }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.description,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = expense.category.displayName(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    // Show amount with variable indicator if applicable
                    if (expense.isVariableAmount) {
                        val predictedAmount = expense.predictNextAmount()
                        Text(
                            text = "~${formatCurrency(predictedAmount)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = stringResource(R.string.recurring_variable_amount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    } else {
                        Text(
                            text = formatCurrency(expense.amount),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = expense.frequency.displayName(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Status / Next Due
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (statusIcon, statusColor, statusText) = when {
                        !expense.isActive -> Triple(MaterialSymbols.BLOCK, MaterialTheme.colorScheme.onSurfaceVariant, stringResource(R.string.recurring_paused))
                        daysUntil != null && daysUntil <= expense.reminderDaysBefore -> Triple(MaterialSymbols.WARNING, MaterialTheme.colorScheme.error, stringResource(R.string.recurring_due_soon))
                        else -> Triple(MaterialSymbols.SCHEDULE, MaterialTheme.colorScheme.primary, stringResource(R.string.recurring_active))
                    }
                    
                    MaterialSymbolIcon(
                        icon = statusIcon, 
                        contentDescription = null, 
                        tint = statusColor,
                        size = 18.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = nextDue?.let { stringResource(R.string.recurring_due_date, it.format(formatter)) } ?: statusText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = statusColor
                    )
                }

                // Actions
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        MaterialSymbolIcon(icon = MaterialSymbols.EDIT, contentDescription = stringResource(R.string.edit), size = 18.dp)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        MaterialSymbolIcon(icon = MaterialSymbols.DELETE, contentDescription = stringResource(R.string.delete), size = 18.dp)
                    }
                    Switch(
                        checked = expense.isActive,
                        onCheckedChange = onToggleActive,
                        modifier = Modifier.scale(0.8f)
                    )
                }
            }
            
            if (daysUntil != null && daysUntil <= 5 && expense.isActive) {
                Spacer(modifier = Modifier.height(8.dp))
                SparelyTonalButton(
                    onClick = onMarkProcessed, 
                    modifier = Modifier.fillMaxWidth(),
                    icon = { MaterialSymbolIcon(icon = MaterialSymbols.CHECK, contentDescription = null, size = 18.dp) }
                ) {
                    Text(stringResource(R.string.recurring_mark_paid))
                }
            }
        }
    }
}

@Composable
private fun getCategoryColor(category: ExpenseCategory): androidx.compose.ui.graphics.Color {
    return when (category.name.uppercase()) {
        "FOOD" -> androidx.compose.ui.graphics.Color(0xFFEF5350)
        "TRANSPORT" -> androidx.compose.ui.graphics.Color(0xFF42A5F5)
        "HOUSING" -> androidx.compose.ui.graphics.Color(0xFFFFA726)
        "UTILITIES" -> androidx.compose.ui.graphics.Color(0xFF7E57C2)
        "ENTERTAINMENT" -> androidx.compose.ui.graphics.Color(0xFFEC407A)
        "HEALTH" -> androidx.compose.ui.graphics.Color(0xFF26A69A)
        "EDUCATION" -> androidx.compose.ui.graphics.Color(0xFF5C6BC0)
        "SHOPPING" -> androidx.compose.ui.graphics.Color(0xFF8D6E63)
        else -> MaterialTheme.colorScheme.primary
    }
}

@Composable
private fun getCategoryIcon(category: ExpenseCategory): Int {
    return when (category.name.uppercase()) {
        "FOOD" -> MaterialSymbols.RESTAURANT
        "TRANSPORT" -> MaterialSymbols.DIRECTIONS_CAR
        "HOUSING" -> MaterialSymbols.HOME
        "UTILITIES" -> MaterialSymbols.LIGHTBULB
        "ENTERTAINMENT" -> MaterialSymbols.CELEBRATION
        "HEALTH" -> MaterialSymbols.HEALTH_AND_SAFETY
        "EDUCATION" -> MaterialSymbols.SCHOOL
        "SHOPPING" -> MaterialSymbols.SHOPPING_BAG
        "SAVINGS" -> MaterialSymbols.SAVINGS
        "DEBT" -> MaterialSymbols.ATTACH_MONEY
        else -> MaterialSymbols.INFO
    }
}

@Composable
private fun EmptyRecurringState(onAddRecurring: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.recurring_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.recurring_empty_desc),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            SparelyButton(onClick = onAddRecurring) {
                Text(stringResource(R.string.recurring_add_button))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringExpenseDialog(
    expense: RecurringExpense?,
    smartVaults: List<SmartVault>,
    stores: List<Store>,
    onDismiss: () -> Unit,
    onConfirm: (RecurringExpenseInput, RecurringExpense?) -> Unit,
    onCreateStore: suspend (StoreInput) -> Store?,
    onEditStore: (Store) -> Unit,
    onDeleteStore: (Store) -> Unit,
    brandfetchClientId: String?,
    paymentMethods: List<PaymentMethod>,
    onManagePaymentMethods: () -> Unit,
    brandSearchResults: List<com.example.sparely.data.remote.BrandfetchBrand> = emptyList(),
    onBrandSearch: (String) -> Unit
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
    var isVariableAmount by remember { mutableStateOf(expense?.isVariableAmount ?: false) }
    
    // Store selection state
    var selectedStore by remember(stores, expense?.storeId) { 
        mutableStateOf(stores.find { it.id == expense?.storeId }) 
    }
    var storeSearchQuery by remember { mutableStateOf("") }

    var selectedPaymentMethod by remember { mutableStateOf<PaymentMethod?>(null) }
    
    // Initialize payment method
    androidx.compose.runtime.LaunchedEffect(paymentMethods, expense) {
        if (selectedPaymentMethod == null) {
            if (expense?.paymentMethodId != null) {
                selectedPaymentMethod = paymentMethods.find { it.id == expense.paymentMethodId }
            } else {
                 val default = paymentMethods.find { it.isDefault }
                 if (default != null) {
                     selectedPaymentMethod = default
                     // Only set default deduct if creating new expense
                     if (expense == null) {
                        deductFromMainAccount = default.defaultDeductFromMainAccount
                     }
                 }
            }
        }
    }

    val activeVaults = remember(smartVaults) { smartVaults.filter { !it.archived } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (expense == null) stringResource(R.string.recurring_add_title) else stringResource(R.string.recurring_edit_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (showError) {
                    Text(
                        text = stringResource(R.string.recurring_error_fields),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // --- SECTION 1: GENERAL DETAILS ---
                Text(
                    text = stringResource(R.string.recurring_section_general),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                SparelyTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.onboarding_financial_description_label)) },
                    singleLine = true
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SparelyTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filterCurrencyInput() },
                        label = { Text(stringResource(R.string.onboarding_financial_amount_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                     SparelyTextField(
                        value = reminderDays,
                        onValueChange = { reminderDays = it.filter { ch -> ch.isDigit() } },
                        label = { Text(stringResource(R.string.onboarding_financial_reminder_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Variable Amount Toggle
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.recurring_variable_amount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(R.string.recurring_variable_amount_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isVariableAmount,
                                onCheckedChange = { isVariableAmount = it },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                        
                        // Show predicted amount if variable and has history
                        if (isVariableAmount && expense?.amountHistory?.isNotEmpty() == true) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val predictedAmount = expense.predictNextAmount()
                            Text(
                                text = stringResource(R.string.recurring_predicted_amount) + ": ${formatCurrency(predictedAmount)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                CategorySelector(selected = category, onSelected = { category = it })

                FrequencySelector(selected = frequency, onSelected = { frequency = it })
                
                DateSelector(
                    label = stringResource(R.string.onboarding_financial_start_date_label),
                    date = startDate,
                    onDateSelected = { startDate = it }
                )
                 SparelyTextField(
                    value = endDateText,
                    onValueChange = { endDateText = it },
                    label = { Text(stringResource(R.string.onboarding_financial_end_date_label)) },
                    singleLine = true
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                
                // --- SECTION 2: PAYMENT & DEDUCTION ---
                Text(
                    text = stringResource(R.string.recurring_section_payment),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                // Store/Website selector
                SearchableStoreSelector(
                    stores = stores,
                    selectedStore = selectedStore,
                    onStoreSelected = { selectedStore = it },
                    onCreateStore = onCreateStore,
                    onEditStore = onEditStore,
                    onDeleteStore = onDeleteStore,
                    searchQuery = storeSearchQuery,
                    onSearchQueryChange = { storeSearchQuery = it },
                    brandfetchClientId = brandfetchClientId,
                    brandSearchResults = brandSearchResults,
                    onBrandSearch = onBrandSearch
                )

                PaymentMethodSelector(
                    paymentMethods = paymentMethods,
                    selectedMethod = selectedPaymentMethod,
                    onMethodSelected = { method ->
                        selectedPaymentMethod = method
                        // SMART DEFAULT LOGIC:
                        // If Credit Card -> Deduct OFF by default (it adds to debt, doesn't reduce cash yet)
                        // If Debit/Cash -> Deduct ON by default
                        method?.let {
                            deductFromMainAccount = !it.isCreditCard
                        }
                    },
                    onManageMethods = onManagePaymentMethods
                )
                
                 // Deduct from Main Account Toggle with Enhanced Explanation
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.recurring_deduct_main_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Switch(
                                checked = deductFromMainAccount,
                                onCheckedChange = { deductFromMainAccount = it },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                        
                        val helperText = when {
                            deductFromMainAccount && selectedPaymentMethod?.isCreditCard == true -> 
                                stringResource(R.string.recurring_deduct_main_desc_credit_on_warning)
                            deductFromMainAccount -> 
                                stringResource(R.string.recurring_deduct_main_desc_debit)
                            !deductFromMainAccount && selectedPaymentMethod?.isCreditCard == true -> 
                                stringResource(R.string.recurring_deduct_main_desc_credit_off)
                            else -> stringResource(R.string.recurring_deduct_main_desc_debit)
                        }
                        
                        val textColor = if (deductFromMainAccount && selectedPaymentMethod?.isCreditCard == true) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant

                        Text(
                            text = helperText,
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor
                        )
                    }
                }

                // Vault selection dropdown (matching ExpenseEntryScreen)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.recurring_deduct_vault), style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = if (activeVaults.isEmpty()) stringResource(R.string.recurring_no_vaults) else stringResource(R.string.recurring_choose_vault),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (activeVaults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
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
                                label = { Text(stringResource(R.string.recurring_choose_vault)) }
                            )
                            ExposedDropdownMenu(
                                expanded = vaultDropdownExpanded,
                                onDismissRequest = { vaultDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { 
                                        Column {
                                            Text(stringResource(R.string.recurring_vault_none))
                                            Text(
                                                text = stringResource(R.string.recurring_vault_none_desc),
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
                                        text = { 
                                            Column {
                                                Text(vault.name)
                                                Text(
                                                    text = stringResource(R.string.recurring_vault_balance, formatCurrency(vault.currentBalance)),
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
                
                // Advanced Options
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                 Text(
                    text = "Options",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.recurring_auto_log_history), style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = autoLog, onCheckedChange = { autoLog = it }, modifier = Modifier.scale(0.8f))
                }
                
                // Expense-related fields
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.recurring_includes_tax), style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = includesTax, onCheckedChange = { includesTax = it }, modifier = Modifier.scale(0.8f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.recurring_execute_auto), style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = executeAutomatically, onCheckedChange = { executeAutomatically = it }, modifier = Modifier.scale(0.8f))
                }

                SparelyTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.onboarding_financial_notes_label)) },
                    singleLine = false
                )
            }
        },
        confirmButton = {
            SparelyTextButton(onClick = {
                val amount = amountText.toSafeDouble()
                val reminder = reminderDays.toIntOrNull()
                val endDate = endDateText.takeIf { it.isNotBlank() }?.let run@{
                    kotlin.runCatching { LocalDate.parse(it) }.getOrNull()
                }
                if (description.isBlank() || amount == null || amount <= 0 || reminder == null) {
                    showError = true
                    return@SparelyTextButton
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
                    storeId = selectedStore?.id,
                    includesTax = includesTax,
                    deductFromMainAccount = deductFromMainAccount,
                    deductedFromVaultId = deductFromVaultId,
                    paymentMethodId = selectedPaymentMethod?.id,
                    isVariableAmount = isVariableAmount
                )
                onConfirm(input, expense)
            }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            SparelyTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySelector(selected: ExpenseCategory, onSelected: (ExpenseCategory) -> Unit) {
    Column {
        Text(stringResource(R.string.onboarding_financial_category_label), style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (category in ExpenseCategory.entries) {
                SparelyChip(
                    onClick = { onSelected(category) },
                    label = { Text(category.displayName()) },
                    enabled = true,
                    selected = selected == category
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FrequencySelector(selected: RecurringFrequency, onSelected: (RecurringFrequency) -> Unit) {
    Column {
        Text(stringResource(R.string.onboarding_financial_frequency_label), style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (frequency in RecurringFrequency.entries) {
                SparelyChip(
                    onClick = { onSelected(frequency) },
                    label = { Text(frequency.displayName()) },
                    enabled = true,
                    selected = selected == frequency
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
    val millis = remember(date) { date.toSafeDatePickerMillis() }
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = millis)
    LaunchedEffect(millis) {
        pickerState.selectedDateMillis = millis
    }

    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(4.dp))
        SparelyTextButton(onClick = { showDialog = true }) {
            Text(date.format(formatter))
        }
    }

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                SparelyTextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millisSelected ->
                        val selectedDate = Instant.ofEpochMilli(millisSelected).atZone(ZoneOffset.UTC).toLocalDate()
                        onDateSelected(selectedDate)
                    }
                    showDialog = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                SparelyTextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun RecurringFrequency.displayName(): String = when (this) {
    RecurringFrequency.DAILY -> stringResource(R.string.freq_daily)
    RecurringFrequency.WEEKLY -> stringResource(R.string.freq_weekly)
    RecurringFrequency.BIWEEKLY -> stringResource(R.string.freq_biweekly)
    RecurringFrequency.MONTHLY -> stringResource(R.string.freq_monthly)
    RecurringFrequency.QUARTERLY -> stringResource(R.string.freq_quarterly)
    RecurringFrequency.YEARLY -> stringResource(R.string.freq_yearly)
}

@Composable
private fun RecurringOverviewMode.displayName(): String = when (this) {
    RecurringOverviewMode.OVERVIEW -> stringResource(R.string.overview_mode_overview)
    RecurringOverviewMode.SMART_REMINDERS -> stringResource(R.string.overview_mode_reminders)
    RecurringOverviewMode.AUTO_LOGGING -> stringResource(R.string.overview_mode_autolog)
    RecurringOverviewMode.UPCOMING -> stringResource(R.string.overview_mode_upcoming)
}

@Composable
private fun formatCountdown(daysUntil: Int): String = when {
    daysUntil < 0 -> stringResource(R.string.countdown_overdue, -daysUntil)
    daysUntil == 0 -> stringResource(R.string.countdown_today)
    daysUntil == 1 -> stringResource(R.string.countdown_tomorrow)
    else -> stringResource(R.string.countdown_days, daysUntil)
}

private fun calculateNextDue(expense: RecurringExpense, today: LocalDate = LocalDate.now()): LocalDate? {
    if (!expense.isActive) return null
    
    val baseDate = expense.lastProcessedDate ?: expense.startDate.minusDays(1)
    var nextDue = addFrequencyInterval(baseDate, expense.frequency)
    
    // If next due is still in the past, advance until we reach a future date
    while (nextDue.isBefore(today) || nextDue.isEqual(baseDate)) {
        nextDue = addFrequencyInterval(nextDue, expense.frequency)
    }
    
    expense.endDate?.let { if (nextDue.isAfter(it)) return null }
    return nextDue
}

/**
 * Add one frequency interval to a date.
 * For monthly/quarterly/yearly, this preserves the day of month (e.g., 25th stays 25th).
 */
private fun addFrequencyInterval(date: LocalDate, frequency: RecurringFrequency): LocalDate {
    return when (frequency) {
        RecurringFrequency.DAILY -> date.plusDays(1)
        RecurringFrequency.WEEKLY -> date.plusWeeks(1)
        RecurringFrequency.BIWEEKLY -> date.plusWeeks(2)
        RecurringFrequency.MONTHLY -> date.plusMonths(1)
        RecurringFrequency.QUARTERLY -> date.plusMonths(3)
        RecurringFrequency.YEARLY -> date.plusYears(1)
    }
}

private fun formatCurrency(value: Double): String = "$" + String.format("%,.2f", value)
