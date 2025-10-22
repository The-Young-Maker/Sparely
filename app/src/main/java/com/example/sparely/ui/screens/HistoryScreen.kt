package com.example.sparely.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sparely.domain.model.AnalyticsSnapshot
import com.example.sparely.domain.model.DateRangeFilter
import com.example.sparely.domain.model.Expense
import com.example.sparely.domain.model.ExpenseCategory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Filter by date range and category to review your allocations.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            FilterRow(
                dateFilter = dateFilter,
                onDateSelected = { dateFilter = it },
                categoryFilter = categoryFilter,
                onCategorySelected = { categoryFilter = it }
            )
        }
        item {
            SummaryCard(analytics = analytics, filteredExpenses = filteredExpenses)
        }
        items(filteredExpenses) { expense ->
            ExpenseHistoryRow(expense = expense, onDelete = { expenseToDelete = expense })
        }
        if (filteredExpenses.isEmpty()) {
            item {
                EmptyHistoryNotice()
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
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
            DateRangeFilter.values().forEach { filter ->
                FilterChip(
                    selected = filter == dateFilter,
                    onClick = { onDateSelected(filter) },
                    label = { Text(filter.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = { onCategorySelected(null) },
                label = { Text("All categories") },
                leadingIcon = {
                    if (categoryFilter == null) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null
                        )
                    }
                }
            )
            ExpenseCategory.values().forEach { category ->
                FilterChip(
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
private fun SummaryCard(
    analytics: AnalyticsSnapshot,
    filteredExpenses: List<Expense>
) {
    val totalFilteredSpent = filteredExpenses.sumOf { it.amount }
    val totalFilteredReserve = filteredExpenses.sumOf { it.allocation.totalSetAside }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Overview", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Total spent: ${formatCurrency(totalFilteredSpent)}")
            Text("Set aside: ${formatCurrency(totalFilteredReserve)}")
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Lifetime saved: ${formatCurrency(analytics.totalReserved)}",
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun ExpenseHistoryRow(expense: Expense, onDelete: () -> Unit) {
    val formatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(expense.description, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = expense.date.format(formatter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Delete")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Amount ${formatCurrency(expense.amount)}")
            Text(
                text = "Emergency ${formatCurrency(expense.allocation.emergencyAmount)} | Invest ${formatCurrency(expense.allocation.investmentAmount)} | Fun ${formatCurrency(expense.allocation.funAmount)}",
                style = MaterialTheme.typography.bodySmall
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "Safe ${formatCurrency(expense.allocation.safeInvestmentAmount)} • High-risk ${formatCurrency(expense.allocation.highRiskInvestmentAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Expense?") },
        text = {
            Column {
                Text("Are you sure you want to delete this expense?")
                Spacer(modifier = Modifier.height(12.dp))
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
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
