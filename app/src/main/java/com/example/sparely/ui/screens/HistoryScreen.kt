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
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.example.sparely.ui.components.SparelyButton
import com.example.sparely.ui.components.SparelyTextButton
import com.example.sparely.ui.theme.MaterialSymbols
import com.example.sparely.ui.theme.MaterialSymbolIcon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedCard
import com.example.sparely.ui.components.ExpressiveCard
import com.example.sparely.ui.components.SparelyChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.sparely.R
import com.example.sparely.ui.components.SparelyTextButton
import com.example.sparely.ui.components.SingleLineText
import com.example.sparely.ui.theme.getCategoryColor
import com.example.sparely.ui.theme.getCategoryIcon


@Composable
fun HistoryScreen(
    expenses: List<Expense>,
    analytics: AnalyticsSnapshot,
    onDeleteExpense: (Long) -> Unit
) {
    var dateFilter by remember { mutableStateOf(DateRangeFilter.LAST_30_DAYS) }
    var categoryFilter by remember { mutableStateOf<ExpenseCategory?>(null) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    val filteredExpenses = remember(expenses, dateFilter, categoryFilter) {
        expenses.filter { expense ->
            matchesDate(expense.date, dateFilter) &&
                (categoryFilter == null || categoryFilter == expense.category)
        }
    }

    val groupedExpenses = remember(filteredExpenses) {
        filteredExpenses.groupBy { it.date }.toSortedMap(compareByDescending { it })
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            // Use a single tonal background instead of a gradient
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header removed to avoid duplication if scaffold header exists, or just keep one. 
        // User said there are 2 headers. 
        // If this is the main screen content inside a Scaffold which already has a top bar 'Expense History', then this is duplicate.
        // Assuming the scaffold top bar is the other one.

        item {
            FilterRow(
                dateFilter = dateFilter,
                onDateSelected = { dateFilter = it },
                categoryFilter = categoryFilter,
                onCategorySelected = { categoryFilter = it }
            )
        }
        item {
            ModernSummaryCard(analytics = analytics, filteredExpenses = filteredExpenses, dateFilter = dateFilter)
        }
        
        groupedExpenses.forEach { (date, dailyExpenses) ->
            stickyHeader {
                DateHeader(date = date, total = dailyExpenses.sumOf { it.amount })
            }
            items(dailyExpenses) { expense ->
                ModernExpenseCard(expense = expense, onDelete = { expenseToDelete = expense })
            }
        }

        if (filteredExpenses.isEmpty()) {
            item {
                EmptyHistoryNotice()
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) } // Extra padding for FAB/BottomBar
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterRow(
    dateFilter: DateRangeFilter,
    onDateSelected: (DateRangeFilter) -> Unit,
    categoryFilter: ExpenseCategory?,
    onCategorySelected: (ExpenseCategory?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (filter in DateRangeFilter.entries) {
                SparelyChip(
                    selected = filter == dateFilter,
                    onClick = { onDateSelected(filter) },
                    label = { 
                        Text(
                            when(filter) {
                                DateRangeFilter.YEAR_TO_DATE -> "This Year"
                                else -> filter.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
                            },
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        ) 
                    }
                )
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
                    label = { Text(category.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
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
                            text = "Total Spent",
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
                            text = "Savings",
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
                            text = "Savings Rate",
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
        isToday -> "Today"
        isYesterday -> "Yesterday"
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
private fun ModernExpenseCard(expense: Expense, onDelete: () -> Unit) {
    val formatter = remember { DateTimeFormatter.ofPattern("MMM d") }
    val savingsRate = if (expense.amount > 0) expense.allocation.totalSetAside / expense.amount else 0.0
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Main Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = getCategoryColor(expense.category).copy(alpha = 0.15f),
                    modifier = Modifier.size(48.dp)
                ) {
                     Box(contentAlignment = Alignment.Center) {
                         MaterialSymbolIcon(
                             icon = getCategoryIcon(expense.category),
                             contentDescription = null,
                             tint = getCategoryColor(expense.category),
                             size = 24.dp
                         )
                     }
                }
                
                Spacer(modifier = Modifier.width(16.dp))

                // Title & Category
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.description,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                     Text(
                        text = expense.category.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Amount & Date
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(expense.amount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                       color = MaterialTheme.colorScheme.onSurface
                    )
                     Text(
                        text = expense.date.format(formatter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Footer: Allocations & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                 // Savings Badge
                 Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MaterialSymbolIcon(icon = MaterialSymbols.TRENDING_UP,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${String.format("%.0f", savingsRate * 100)}% Saved",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Subtle Delete Button
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    modifier = Modifier
                        .size(36.dp)
                        .clickable(onClick = onDelete)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        MaterialSymbolIcon(
                            icon = MaterialSymbols.DELETE,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            size = 18.dp
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
        Text("No entries yet", style = MaterialTheme.typography.titleSmall)
        Text(
            text = "Start logging purchases to see analytics.",
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
                    text = "Delete Expense?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                 Text(
                    text = "Are you sure you want to delete this expense? This action cannot be undone.",
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

private fun matchesDate(date: LocalDate, filter: DateRangeFilter): Boolean {
    val today = LocalDate.now()
    return when (filter) {
        DateRangeFilter.LAST_7_DAYS -> !date.isBefore(today.minusDays(6))
        DateRangeFilter.LAST_30_DAYS -> !date.isBefore(today.minusDays(29))
        DateRangeFilter.LAST_90_DAYS -> !date.isBefore(today.minusDays(89))
        DateRangeFilter.YEAR_TO_DATE -> date.year == today.year
        DateRangeFilter.ALL_TIME -> true
    }
}

private fun formatCurrency(value: Double): String = "$" + String.format("%,.2f", value)

// Category colors and icons are now sourced from com.example.sparely.ui.theme.CategoryUtils
