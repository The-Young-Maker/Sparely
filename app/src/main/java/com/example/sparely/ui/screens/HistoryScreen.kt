package com.example.sparely.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import com.example.sparely.ui.components.SparelyButton
import com.example.sparely.ui.components.SparelyTextButton
import com.example.sparely.ui.components.SparelyTextField
import com.example.sparely.ui.theme.MaterialSymbols
import com.example.sparely.ui.theme.MaterialSymbolIcon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.sparely.ui.components.ExpressiveCard
import com.example.sparely.ui.components.SparelyChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import com.sparely.app.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.sparely.domain.model.StoreInput
import com.example.sparely.ui.components.SearchableStoreSelector
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
 
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sparely.domain.model.AnalyticsSnapshot
import com.example.sparely.domain.model.DateRangeFilter
import com.example.sparely.domain.model.Expense
import com.example.sparely.domain.model.ExpenseCategory
import com.example.sparely.domain.model.Store
import com.example.sparely.domain.model.displayName
import com.example.sparely.ui.theme.getCategoryColor
import com.example.sparely.ui.theme.getCategoryIcon
import kotlinx.coroutines.launch


@Composable
fun HistoryScreen(
    expenses: List<Expense>,
    analytics: AnalyticsSnapshot,
    stores: List<Store> = emptyList(),
    onDeleteExpense: (Long) -> Unit,
    onEditExpense: (Expense) -> Unit = {},
    onAddExpense: () -> Unit = {},
    onCreateStore: suspend (StoreInput) -> Store? = { null },
    onEditStore: (Store) -> Unit = {},
    onDeleteStore: (Store) -> Unit = {},
    brandfetchClientId: String? = null,
    brandSearchResults: List<com.example.sparely.data.remote.BrandfetchBrand> = emptyList(),
    onBrandSearch: (String) -> Unit = {},
    onRefundExpense: (Long, Double) -> Unit = { _, _ -> },
    paymentMethods: List<com.example.sparely.domain.model.PaymentMethod> = emptyList(),
    vaults: List<com.example.sparely.domain.model.SmartVault> = emptyList()
) {
    var dateFilter by remember { mutableStateOf(DateRangeFilter.LAST_30_DAYS) }
    var categoryFilter by remember { mutableStateOf<ExpenseCategory?>(null) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    var expenseToRefund by remember { mutableStateOf<Expense?>(null) }
    
    // Bulk selection mode
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedExpenseIds by remember { mutableStateOf(setOf<Long>()) }
    var showBulkDeleteConfirmation by remember { mutableStateOf(false) }
    
    // Search & custom date range
    var searchQuery by remember { mutableStateOf("") }
    var customStartDate by remember { mutableStateOf<LocalDate?>(null) }
    var customEndDate by remember { mutableStateOf<LocalDate?>(null) }

    val filteredExpenses = remember(expenses, dateFilter, categoryFilter, searchQuery, customStartDate, customEndDate) {
        expenses.filter { expense ->
            val matchesSearch = searchQuery.isBlank() ||
                expense.description.contains(searchQuery, ignoreCase = true) ||
                expense.notes?.contains(searchQuery, ignoreCase = true) == true ||
                stores.find { it.id == expense.storeId }?.name?.contains(searchQuery, ignoreCase = true) == true
            
            matchesSearch &&
            matchesDate(expense.date, dateFilter, customStartDate, customEndDate) &&
                (categoryFilter == null || categoryFilter == expense.category)
        }
    }

    val groupedExpenses = remember(filteredExpenses) {
        filteredExpenses.groupBy { it.date }.toSortedMap(compareByDescending { it })
    }

    // Store Analytics - calculated outside LazyColumn scope
    val storeStats = remember(filteredExpenses, stores) {
        filteredExpenses
            .filter { it.storeId != null }
            .groupBy { it.storeId }
            .mapNotNull { (storeId, expenses) ->
                val store = stores.find { it.id == storeId }
                if (store != null) {
                    Triple(store, expenses.sumOf { it.amount }, expenses.size)
                } else null
            }
            .sortedByDescending { it.second }
            .take(5)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                // Use a single tonal background instead of a gradient
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Search Bar
                androidx.compose.material3.OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.history_search_hint)) },
                    leadingIcon = {
                        MaterialSymbolIcon(
                            icon = MaterialSymbols.SEARCH,
                            contentDescription = null,
                            size = 20.dp
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                MaterialSymbolIcon(
                                    icon = MaterialSymbols.CLOSE,
                                    contentDescription = stringResource(R.string.history_search_clear),
                                    size = 20.dp
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }
            item {
                FilterRow(
                    dateFilter = dateFilter,
                    onDateSelected = { 
                        dateFilter = it
                        // Reset custom dates when switching away from CUSTOM
                        if (it != DateRangeFilter.CUSTOM) {
                            customStartDate = null
                            customEndDate = null
                        }
                    },
                    categoryFilter = categoryFilter,
                    onCategorySelected = { categoryFilter = it },
                    customStartDate = customStartDate,
                    customEndDate = customEndDate,
                    onCustomStartDateChange = { customStartDate = it },
                    onCustomEndDateChange = { customEndDate = it }
                )
            }
            item {
                ModernSummaryCard(analytics = analytics, filteredExpenses = filteredExpenses, dateFilter = dateFilter)
            }
            
            // Store Analytics Card - only show if there are expenses with stores
            if (storeStats.isNotEmpty()) {
                item {
                    StoreAnalyticsCard(storeStats = storeStats, totalSpent = filteredExpenses.sumOf { it.amount })
                }
            }
            
            groupedExpenses.forEach { (date, dailyExpenses) ->
                stickyHeader {
                    DateHeader(date = date, total = dailyExpenses.sumOf { it.amount })
                }
                items(dailyExpenses) { expense ->
                    ModernExpenseCard(
                        expense = expense,
                        store = stores.find { it.id == expense.storeId },
                        brandfetchClientId = brandfetchClientId,
                        onDelete = { expenseToDelete = expense },
                        onEdit = { expenseToEdit = expense },
                        onRefund = { expenseToRefund = expense },
                        isSelectionMode = isSelectionMode,
                        isSelected = selectedExpenseIds.contains(expense.id),
                        onLongPress = {
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedExpenseIds = setOf(expense.id)
                            }
                        },
                        onToggleSelection = {
                            selectedExpenseIds = if (selectedExpenseIds.contains(expense.id)) {
                                val newSet = selectedExpenseIds - expense.id
                                if (newSet.isEmpty()) isSelectionMode = false
                                newSet
                            } else {
                                selectedExpenseIds + expense.id
                            }
                        }
                    )
                }
            }

            if (filteredExpenses.isEmpty()) {
                item {
                    EmptyHistoryNotice()
                }
            }
            item { Spacer(modifier = Modifier.height(if (isSelectionMode) 100.dp else 80.dp)) }
        }
        
        // Bulk Action Bar - shown when in selection mode
        if (isSelectionMode) {
            BulkActionBar(
                selectedCount = selectedExpenseIds.size,
                onDelete = { showBulkDeleteConfirmation = true },
                onClear = {
                    selectedExpenseIds = emptySet()
                    isSelectionMode = false
                },
                onSelectAll = {
                    selectedExpenseIds = filteredExpenses.map { it.id }.toSet()
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        } else {
            // FAB for adding expense - only show when not in selection mode
            androidx.compose.material3.FloatingActionButton(
                onClick = onAddExpense,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                MaterialSymbolIcon(
                    icon = MaterialSymbols.ADD,
                    contentDescription = stringResource(R.string.expense_entry_title),
                    size = 24.dp
                )
            }
        }
    }
    
    // Confirmation dialog
    expenseToDelete?.let { expense ->
        DeleteExpenseConfirmationDialog(
            expense = expense,
            onConfirm = {
                onDeleteExpense(expense.id)
                expenseToDelete = null
            },
            onDismiss = { expenseToDelete = null }
        )
    }
    
    // Edit dialog
    expenseToEdit?.let { expense ->
        EditExpenseDialog(
            expense = expense,
            stores = stores,
            paymentMethods = paymentMethods,
            vaults = vaults,
            onConfirm = { editedExpense ->
                onEditExpense(editedExpense)
                expenseToEdit = null
            },
            onDismiss = { expenseToEdit = null },
            onCreateStore = onCreateStore,
            onEditStore = onEditStore,
            onDeleteStore = onDeleteStore,
            brandfetchClientId = brandfetchClientId,
            brandSearchResults = brandSearchResults,
            onBrandSearch = onBrandSearch
        )
    }

    // Refund Dialog
    expenseToRefund?.let { expense ->
        RefundExpenseDialog(
            expense = expense,
            onConfirm = { amount ->
                onRefundExpense(expense.id, amount)
                expenseToRefund = null
            },
            onDismiss = { expenseToRefund = null }
        )
    }
    
    // Bulk Delete Confirmation Dialog
    if (showBulkDeleteConfirmation) {
        BulkDeleteConfirmationDialog(
            count = selectedExpenseIds.size,
            onConfirm = {
                selectedExpenseIds.forEach { id -> onDeleteExpense(id) }
                selectedExpenseIds = emptySet()
                isSelectionMode = false
                showBulkDeleteConfirmation = false
            },
            onDismiss = { showBulkDeleteConfirmation = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FilterRow(
    dateFilter: DateRangeFilter,
    onDateSelected: (DateRangeFilter) -> Unit,
    categoryFilter: ExpenseCategory?,
    onCategorySelected: (ExpenseCategory?) -> Unit,
    customStartDate: LocalDate? = null,
    customEndDate: LocalDate? = null,
    onCustomStartDateChange: (LocalDate?) -> Unit = {},
    onCustomEndDateChange: (LocalDate?) -> Unit = {}
) {
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (filter in DateRangeFilter.entries) {
                SparelyChip(
                    selected = filter == dateFilter,
                    onClick = { onDateSelected(filter) },
                    label = { 
                        Text(
                            when(filter) {
                                DateRangeFilter.YEAR_TO_DATE -> stringResource(R.string.history_filter_this_year)
                                else -> filter.displayName()
                            },
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        ) 
                    }
                )
            }
        }
        
        // Custom date range pickers - only show when CUSTOM filter is selected
        if (dateFilter == DateRangeFilter.CUSTOM) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Start date button
                androidx.compose.material3.OutlinedButton(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    MaterialSymbolIcon(
                        icon = MaterialSymbols.CALENDAR_MONTH,
                        contentDescription = null,
                        size = 16.dp,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = customStartDate?.format(dateFormatter) 
                            ?: stringResource(R.string.history_custom_start_date),
                        maxLines = 1
                    )
                }
                // End date button
                androidx.compose.material3.OutlinedButton(
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    MaterialSymbolIcon(
                        icon = MaterialSymbols.CALENDAR_MONTH,
                        contentDescription = null,
                        size = 16.dp,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = customEndDate?.format(dateFormatter)
                            ?: stringResource(R.string.history_custom_end_date),
                        maxLines = 1
                    )
                }
            }
        }
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SparelyChip(
                selected = categoryFilter == null,
                onClick = { onCategorySelected(null) },
                label = { Text(stringResource(R.string.history_all_categories)) },
                leadingIcon = {
                    if (categoryFilter == null) {
                        MaterialSymbolIcon(icon = MaterialSymbols.DELETE,
                            contentDescription = null
                        )
                    }
                }
            )
            for (category in ExpenseCategory.entries) {
                SparelyChip(
                    selected = categoryFilter == category,
                    onClick = {
                        onCategorySelected(if (categoryFilter == category) null else category)
                    },
                    label = { Text(category.displayName()) }
                )
            }
        }
    }
    
    // Date picker dialogs
    if (showStartDatePicker) {
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = customStartDate?.atStartOfDay(java.time.ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                SparelyButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneOffset.UTC)
                                .toLocalDate()
                            onCustomStartDateChange(date)
                        }
                        showStartDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = {
                SparelyTextButton(onClick = { showStartDatePicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        ) {
            androidx.compose.material3.DatePicker(state = datePickerState)
        }
    }
    
    if (showEndDatePicker) {
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = customEndDate?.atStartOfDay(java.time.ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                SparelyButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneOffset.UTC)
                                .toLocalDate()
                            onCustomEndDateChange(date)
                        }
                        showEndDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = {
                SparelyTextButton(onClick = { showEndDatePicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        ) {
            androidx.compose.material3.DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ModernSummaryCard(
    analytics: AnalyticsSnapshot,
    filteredExpenses: List<Expense>,
    dateFilter: DateRangeFilter
) {
    val totalFilteredSpent = filteredExpenses.sumOf { it.amount }
    val totalFilteredReserve = filteredExpenses.sumOf { it.allocation.totalSetAside }
    val savingsRate = if (totalFilteredSpent > 0) totalFilteredReserve / totalFilteredSpent else 0.0
    
    val animatedRate by animateFloatAsState(
        targetValue = savingsRate.toFloat(),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "savingsRate"
    )
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
                
                // Graph Section (Spending Trend)
                if (filteredExpenses.isNotEmpty()) {
                    SpendingGraph(expenses = filteredExpenses, dateFilter = dateFilter)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.history_total_spent),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(totalFilteredSpent),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.history_savings),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                         Text(
                            text = formatCurrency(totalFilteredReserve),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                
                // Savings Rate with progress
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.history_savings_rate),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            MaterialSymbolIcon(icon = MaterialSymbols.TRENDING_UP,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${String.format("%.1f", savingsRate * 100)}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { animatedRate.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                    )
                }
            }
        }
    }

@Composable
fun SpendingGraph(expenses: List<Expense>, dateFilter: DateRangeFilter) {
    // Determine aggregation (Daily for < 90 days, Monthly otherwise)
    val isMonthly = dateFilter == DateRangeFilter.YEAR_TO_DATE || dateFilter == DateRangeFilter.ALL_TIME || dateFilter == DateRangeFilter.LAST_90_DAYS
    
    val dataPoints = remember(expenses, isMonthly) {
        if (isMonthly) {
            expenses.groupBy { java.time.YearMonth.from(it.date) }
                .mapValues { it.value.sumOf { e -> e.amount } }
                .entries.sortedBy { it.key }
                .takeLast(6) // Last 6 months
                .map { it.key.month.name.take(3) to it.value }
        } else {
            // Daily - take last 7 days with data or just fill dates
            val last7Days = (0..6).map { LocalDate.now().minusDays(it.toLong()) }.reversed()
            val expenseMap = expenses.groupBy { it.date }
            
            last7Days.map { date ->
                val amount = expenseMap[date]?.sumOf { it.amount } ?: 0.0
                date.format(DateTimeFormatter.ofPattern("EEE")) to amount
            }
        }
    }
    
    val maxAmount = dataPoints.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0
    
    Column(
        modifier = Modifier.fillMaxWidth().height(150.dp), // Increased height slightly
        verticalArrangement = Arrangement.Bottom
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            dataPoints.forEach { (label, amount) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Bottom 
                ) {
                    // Bar Container (Takes flexible space)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        val heightFraction = (amount / maxAmount).toFloat().coerceIn(0.02f, 1f)
                        // Only show bar if amount > 0 or for visual placeholder? 
                        // If amount is 0, fraction is 0.02f (tiny bar). 
                        // If we want to hide 0 bars, we can check amount > 0. 
                        // Let's keep a tiny blip for 0 to show the slot exists, or just 0 height.
                        // User said "hides the text", so primary fix is layout.
                        
                        val displayedFraction = if (amount > 0) heightFraction else 0.005f
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .fillMaxHeight(displayedFraction)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    if (amount > 0) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DateHeader(date: LocalDate, total: Double) {
    val dateText = date.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    val isToday = date == LocalDate.now()
    val isYesterday = date == LocalDate.now().minusDays(1)
    
    val displayDate = when {
        isToday -> stringResource(R.string.history_today)
        isYesterday -> stringResource(R.string.history_yesterday)
        else -> dateText
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background, // Match items background so it covers scrolling content
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayDate,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                 color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = formatCurrency(total),
                style = MaterialTheme.typography.titleMedium,
                 fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun ModernExpenseCard(
    expense: Expense,
    store: Store?,
    brandfetchClientId: String?,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {},
    onRefund: () -> Unit = {},
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onLongPress: () -> Unit = {},
    onToggleSelection: () -> Unit = {}
) {
    val formatter = remember { DateTimeFormatter.ofPattern("MMM d") }
    val savingsRate = if (expense.amount > 0) expense.allocation.totalSetAside / expense.amount else 0.0
    val categoryColor = getCategoryColor(expense.category)
    val colorScheme = MaterialTheme.colorScheme
    
    // Selection border color
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(
                if (isSelectionMode) {
                    Modifier.clickable { onToggleSelection() }
                } else {
                    Modifier.combinedClickable(
                        onClick = { onEdit() },
                        onLongClick = { onLongPress() }
                    )
                }
            ),
        shape = RoundedCornerShape(24.dp),
        color = if (isSelected) colorScheme.primaryContainer.copy(alpha = 0.3f) else colorScheme.surfaceContainerHigh,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, colorScheme.primary) else null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle Category Gradient Background (matching vault cards)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                categoryColor.copy(alpha = 0.06f),
                                colorScheme.surface.copy(alpha = 0.5f)
                            )
                        )
                    )
            )

            // Watermark Icon
            MaterialSymbolIcon(
                icon = getCategoryIcon(expense.category),
                contentDescription = null,
                size = 120.dp,
                tint = categoryColor.copy(alpha = 0.04f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 30.dp, y = 10.dp)
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
                        // Icon Circle (48dp like vault cards)
                        val hasStoreLogo = store?.getBrandfetchLogoUrl(brandfetchClientId) != null
                        Surface(
                            shape = CircleShape,
                            color = if (hasStoreLogo) Color.Transparent else categoryColor.copy(alpha = 0.1f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (hasStoreLogo) {
                                    com.example.sparely.ui.components.StoreIcon(
                                        store = store!!,
                                        brandfetchClientId = brandfetchClientId,
                                        size = 48
                                    )
                                } else {
                                    MaterialSymbolIcon(
                                        icon = getCategoryIcon(expense.category),
                                        contentDescription = null,
                                        tint = categoryColor,
                                        size = 24.dp
                                    )
                                }
                            }
                        }

                        Column {
                            Text(
                                text = expense.description,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                color = colorScheme.onSurface
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = expense.category.displayName(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = categoryColor,
                                    fontWeight = FontWeight.Bold
                                )
                                if (store != null) {
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = store.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                if (expense.isRecurring) {
                                    MaterialSymbolIcon(
                                        icon = MaterialSymbols.AUTORENEW,
                                        size = 14.dp,
                                        tint = colorScheme.tertiary,
                                        contentDescription = stringResource(R.string.recurring_paused)
                                    )
                                }
                            }
                        }
                    }
                }

                // Amount Row (like vault balance)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Amount",
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(expense.amount),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = expense.date.format(formatter),
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            MaterialSymbolIcon(
                                icon = MaterialSymbols.TRENDING_UP,
                                contentDescription = null,
                                size = 16.dp,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${String.format("%.0f", savingsRate * 100)}% saved",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Notes (if present)
                expense.notes?.let { noteText ->
                    if (noteText.isNotBlank()) {
                        Text(
                            text = noteText,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                // Refund Badge (if refunded)
                if (expense.refundedAmount > 0) {
                    Surface(
                        color = colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MaterialSymbolIcon(
                                icon = MaterialSymbols.REFRESH,
                                contentDescription = "Refunded",
                                size = 16.dp,
                                tint = colorScheme.tertiary
                            )
                            Text(
                                text = if (expense.isRefunded) "Refunded ${formatCurrency(expense.refundedAmount)}"
                                       else "Partially Refunded: ${formatCurrency(expense.refundedAmount)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                // Line Items Section (if expense has items)
                if (expense.items.isNotEmpty()) {
                    var expanded by remember { mutableStateOf(false) }
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded },
                        shape = RoundedCornerShape(12.dp),
                        color = colorScheme.surfaceContainerLow
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${expense.items.size} item${if (expense.items.size > 1) "s" else ""}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colorScheme.onSurfaceVariant
                                )
                                MaterialSymbolIcon(
                                    icon = if (expanded) MaterialSymbols.ARROW_DROP_UP else MaterialSymbols.ARROW_DROP_DOWN,
                                    contentDescription = if (expanded) "Collapse" else "Expand",
                                    size = 20.dp,
                                    tint = colorScheme.onSurfaceVariant
                                )
                            }
                            
                            if (expanded) {
                                Spacer(modifier = Modifier.height(8.dp))
                                expense.items.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${item.quantity}× ${item.name}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = formatCurrency(item.totalPrice),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Edit Button
                    IconButton(onClick = onEdit) {
                        MaterialSymbolIcon(
                            icon = MaterialSymbols.EDIT,
                            contentDescription = stringResource(R.string.edit),
                            size = 20.dp,
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Refund Button (if not fully refunded)
                    if (!expense.isRefunded) {
                        IconButton(onClick = onRefund) {
                            MaterialSymbolIcon(
                                icon = MaterialSymbols.REFRESH,
                                contentDescription = "Refund",
                                size = 20.dp,
                                tint = colorScheme.tertiary
                            )
                        }
                    }
                    
                    // Delete Button
                    IconButton(onClick = onDelete) {
                        MaterialSymbolIcon(
                            icon = MaterialSymbols.DELETE,
                            contentDescription = stringResource(R.string.delete),
                            size = 20.dp,
                            tint = colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun AllocationChip(
    label: String,
    amount: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatCurrency(amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun EmptyHistoryNotice() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.history_empty_title), style = MaterialTheme.typography.titleSmall)
        Text(
            text = stringResource(R.string.history_empty_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DeleteExpenseConfirmationDialog(
    expense: Expense,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                 // Warning Icon
                 Surface(
                     shape = CircleShape,
                     color = MaterialTheme.colorScheme.errorContainer,
                     modifier = Modifier.size(72.dp)
                 ) {
                     Box(contentAlignment = Alignment.Center) {
                         MaterialSymbolIcon(
                             icon = MaterialSymbols.DELETE,
                             contentDescription = null,
                             modifier = Modifier.size(32.dp),
                             tint = MaterialTheme.colorScheme.error
                         )
                     }
                 }

                Text(
                    text = stringResource(R.string.history_delete_confirmation_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                 Text(
                    text = stringResource(R.string.history_delete_confirmation_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 8.dp),
                     textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // Expense Preview
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                             Text(
                                text = expense.description,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${formatCurrency(expense.amount)} • ${expense.date.format(formatter)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SparelyButton(
                        onClick = onConfirm,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ) {
                        Text("Delete")
                    }
                    
                     SparelyTextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                         Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun EditExpenseDialog(
    expense: Expense,
    stores: List<Store>,
    paymentMethods: List<com.example.sparely.domain.model.PaymentMethod> = emptyList(),
    vaults: List<com.example.sparely.domain.model.SmartVault> = emptyList(),
    onConfirm: (Expense) -> Unit,
    onDismiss: () -> Unit,
    onCreateStore: suspend (StoreInput) -> Store?,
    onEditStore: (Store) -> Unit,
    onDeleteStore: (Store) -> Unit,
    brandfetchClientId: String? = null,
    brandSearchResults: List<com.example.sparely.data.remote.BrandfetchBrand> = emptyList(),
    onBrandSearch: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var description by remember { mutableStateOf(expense.description) }
    var amountText by remember { mutableStateOf(expense.amount.toString()) }
    var category by remember { mutableStateOf(expense.category) }
    // Initialize with current stores, then update if store appears later (e.g. after loading)
    var selectedStore by remember { mutableStateOf(stores.find { it.id == expense.storeId }) }
    var searchQuery by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf(expense.notes ?: "") }
    var orderNumber by remember { mutableStateOf(expense.orderNumber ?: "") }
    var selectedPaymentMethod by remember { mutableStateOf(paymentMethods.find { it.id == expense.paymentMethodId }) }
    var selectedDate by remember { mutableStateOf(expense.date) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    LaunchedEffect(stores) {
        if (selectedStore == null && expense.storeId != null) {
            stores.find { it.id == expense.storeId }?.let { selectedStore = it }
        }
    }
    LaunchedEffect(paymentMethods) {
        if (selectedPaymentMethod == null && expense.paymentMethodId != null) {
            paymentMethods.find { it.id == expense.paymentMethodId }?.let { selectedPaymentMethod = it }
        }
    }
    var showError by remember { mutableStateOf(false) }
    
    // Line items state
    val expenseItems = remember { 
        androidx.compose.runtime.mutableStateListOf<com.example.sparely.domain.model.ExpenseItem>().apply {
            addAll(expense.items)
        }
    }
    var showAddItemDialog by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Expense",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                if (showError) {
                    Text(
                        text = "Please fill out all required fields correctly.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Description
                androidx.compose.material3.OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Amount
                androidx.compose.material3.OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Amount") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )
                
                // Category Selector
                Column {
                    Text("Category", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (cat in ExpenseCategory.entries) {
                            com.example.sparely.ui.components.SparelyChip(
                                onClick = { category = cat },
                                label = { Text(cat.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                selected = category == cat
                            )
                        }
                    }
                }
                
                // Store Selector
                SearchableStoreSelector(
                    stores = stores,
                    selectedStore = selectedStore,
                    onStoreSelected = { selectedStore = it },
                    onCreateStore = onCreateStore,
                    onEditStore = onEditStore,
                    onDeleteStore = onDeleteStore,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    brandfetchClientId = brandfetchClientId,
                    brandSearchResults = brandSearchResults,
                    onBrandSearch = onBrandSearch
                )
                
                // Notes
                SparelyTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.expense_notes_label)) },
                    placeholder = { Text(stringResource(R.string.expense_notes_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4
                )
                
                // Order Number
                SparelyTextField(
                    value = orderNumber,
                    onValueChange = { orderNumber = it },
                    label = { Text(stringResource(R.string.expense_order_number_label)) },
                    placeholder = { Text(stringResource(R.string.expense_order_number_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Date Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.expense_entry_date_label),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    SparelyTextButton(onClick = { showDatePicker = true }) {
                        Text(stringResource(R.string.expense_entry_change_date_button))
                    }
                }
                
                // Payment Method Selector (if payment methods exist)
                if (paymentMethods.isNotEmpty()) {
                    Column {
                        Text(
                            text = "Payment Method",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // None option
                            com.example.sparely.ui.components.SparelyChip(
                                onClick = { selectedPaymentMethod = null },
                                label = { Text("None") },
                                selected = selectedPaymentMethod == null
                            )
                            paymentMethods.forEach { pm ->
                                com.example.sparely.ui.components.SparelyChip(
                                    onClick = { selectedPaymentMethod = pm },
                                    label = { Text(pm.name) },
                                    selected = selectedPaymentMethod?.id == pm.id
                                )
                            }
                        }
                    }
                }
                
                // Line Items Section
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Line Items (${expenseItems.size})",
                            style = MaterialTheme.typography.labelLarge
                        )
                        SparelyTextButton(onClick = { showAddItemDialog = true }) {
                            Text("+ Add Item")
                        }
                    }
                    
                    if (expenseItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        expenseItems.forEachIndexed { index, item ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${item.quantity} × ${formatCurrency(item.unitPrice)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = formatCurrency(item.totalPrice),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        androidx.compose.material3.IconButton(
                                            onClick = { expenseItems.removeAt(index) }
                                        ) {
                                        MaterialSymbolIcon(
                                                icon = MaterialSymbols.DELETE,
                                                contentDescription = "Remove",
                                                tint = MaterialTheme.colorScheme.error,
                                                size = 20.dp
                                            )
                                        }
                                    }
                                }
                            }
                            if (index < expenseItems.lastIndex) {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
                
                // Actions
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
                    SparelyButton(
                        onClick = {
                            val amount = amountText.toDoubleOrNull()
                            if (description.isBlank() || amount == null || amount <= 0) {
                                showError = true
                                return@SparelyButton
                            }
                            
                            scope.launch {
                                var finalStoreId = selectedStore?.id
                                
                                // Auto-resolve store if name typed but not selected
                                if (finalStoreId == null && searchQuery.isNotBlank()) {
                                     val existing = stores.find { it.name.equals(searchQuery.trim(), ignoreCase = true) }
                                     if (existing != null) {
                                         finalStoreId = existing.id
                                     } else {
                                         // Create new store
                                         val newStore = onCreateStore(StoreInput(name = searchQuery.trim()))
                                         finalStoreId = newStore?.id
                                     }
                                }

                                onConfirm(
                                    expense.copy(
                                        description = description.trim(),
                                        amount = amount,
                                        category = category,
                                        storeId = finalStoreId,
                                        notes = notes.trim().takeIf { it.isNotBlank() },
                                        orderNumber = orderNumber.trim().takeIf { it.isNotBlank() },
                                        date = selectedDate,
                                        paymentMethodId = selectedPaymentMethod?.id,
                                        items = expenseItems.toList()
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            androidx.compose.material3.DatePicker(state = datePickerState)
        }
    }
    
    // Add Item Dialog
    if (showAddItemDialog) {
        var itemName by remember { mutableStateOf("") }
        var itemQuantity by remember { mutableStateOf("1") }
        var itemUnitPrice by remember { mutableStateOf("") }
        
        Dialog(onDismissRequest = { showAddItemDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Add Item",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    SparelyTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = { Text("Item Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SparelyTextField(
                            value = itemQuantity,
                            onValueChange = { itemQuantity = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Qty") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )
                        SparelyTextField(
                            value = itemUnitPrice,
                            onValueChange = { itemUnitPrice = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("Unit Price") },
                            modifier = Modifier.weight(2f),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                            )
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SparelyTextButton(
                            onClick = { showAddItemDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        SparelyButton(
                            onClick = {
                                val qty = itemQuantity.toIntOrNull() ?: 1
                                val price = itemUnitPrice.toDoubleOrNull() ?: 0.0
                                if (itemName.isNotBlank() && price > 0) {
                                    expenseItems.add(
                                        com.example.sparely.domain.model.ExpenseItem(
                                            id = 0L,
                                            expenseId = expense.id,
                                            name = itemName.trim(),
                                            quantity = qty.coerceAtLeast(1),
                                            unitPrice = price,
                                            totalPrice = qty * price
                                        )
                                    )
                                    showAddItemDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreAnalyticsCard(
    storeStats: List<Triple<Store, Double, Int>>,
    totalSpent: Double
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Top Stores",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                MaterialSymbolIcon(
                    icon = MaterialSymbols.SHOPPING_BAG,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    size = 24.dp
                )
            }
            
            storeStats.forEach { (store, amount, count) ->
                val percentage = if (totalSpent > 0) (amount / totalSpent * 100) else 0.0
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = store.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$count purchase${if (count > 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatCurrency(amount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${String.format("%.1f", percentage)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Progress bar showing percentage of total spending
                LinearProgressIndicator(
                    progress = { (percentage / 100).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

private fun matchesDate(
    date: LocalDate,
    filter: DateRangeFilter,
    customStart: LocalDate? = null,
    customEnd: LocalDate? = null
): Boolean {
    val today = LocalDate.now()
    return when (filter) {
        DateRangeFilter.LAST_7_DAYS -> !date.isBefore(today.minusDays(6))
        DateRangeFilter.LAST_30_DAYS -> !date.isBefore(today.minusDays(29))
        DateRangeFilter.LAST_90_DAYS -> !date.isBefore(today.minusDays(89))
        DateRangeFilter.YEAR_TO_DATE -> date.year == today.year
        DateRangeFilter.ALL_TIME -> true
        DateRangeFilter.CUSTOM -> {
            val start = customStart ?: LocalDate.MIN
            val end = customEnd ?: LocalDate.MAX
            !date.isBefore(start) && !date.isAfter(end)
        }
    }
}

@Composable
fun DateRangeFilter.displayName(): String = when(this) {
    DateRangeFilter.LAST_7_DAYS -> stringResource(R.string.history_filter_7_days)
    DateRangeFilter.LAST_30_DAYS -> stringResource(R.string.history_filter_30_days)
    DateRangeFilter.LAST_90_DAYS -> stringResource(R.string.history_filter_90_days)
    DateRangeFilter.YEAR_TO_DATE -> stringResource(R.string.history_filter_this_year)
    DateRangeFilter.ALL_TIME -> stringResource(R.string.history_filter_all_time)
    DateRangeFilter.CUSTOM -> stringResource(R.string.history_filter_custom)
}

private fun formatCurrency(value: Double): String = "$" + String.format("%,.2f", value)

// Category colors and icons are now sourced from com.example.sparely.ui.theme.CategoryUtils

@Composable
private fun RefundExpenseDialog(
    expense: Expense,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    val maxRefundable = expense.amount - expense.refundedAmount
    var amountText by remember { mutableStateOf(maxRefundable.toString()) }
    var showError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Refund Expense",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Enter amount to refund. Max refundable: ${formatCurrency(maxRefundable)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                androidx.compose.material3.OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Amount") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    isError = showError
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SparelyTextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    SparelyButton(
                        onClick = {
                            val amount = amountText.toDoubleOrNull()
                            if (amount == null || amount <= 0 || amount > maxRefundable) {
                                showError = true
                            } else {
                                onConfirm(amount)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Refund")
                    }
                }
            }
        }
    }
}

@Composable
fun BulkActionBar(
    selectedCount: Int,
    onDelete: () -> Unit,
    onClear: () -> Unit,
    onSelectAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selected count
            Text(
                text = stringResource(R.string.history_bulk_selected, selectedCount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Select All
                IconButton(onClick = onSelectAll) {
                    MaterialSymbolIcon(
                        icon = MaterialSymbols.CHECK_CIRCLE,
                        contentDescription = stringResource(R.string.history_bulk_select_all),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        size = 24.dp
                    )
                }
                
                // Delete
                IconButton(onClick = onDelete) {
                    MaterialSymbolIcon(
                        icon = MaterialSymbols.DELETE,
                        contentDescription = stringResource(R.string.history_bulk_delete),
                        tint = MaterialTheme.colorScheme.error,
                        size = 24.dp
                    )
                }
                
                // Clear selection
                IconButton(onClick = onClear) {
                    MaterialSymbolIcon(
                        icon = MaterialSymbols.CLOSE,
                        contentDescription = stringResource(R.string.history_bulk_clear),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        size = 24.dp
                    )
                }
            }
        }
    }
}

@Composable
fun BulkDeleteConfirmationDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.history_bulk_delete_confirm_title, count))
        },
        text = {
            Text(stringResource(R.string.history_bulk_delete_confirm_message))
        },
        confirmButton = {
            SparelyButton(
                onClick = onConfirm
            ) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            SparelyTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
