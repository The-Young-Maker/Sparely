package com.example.sparely.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sparely.domain.model.*
import com.example.sparely.ui.state.SparelyUiState
import java.time.YearMonth
import kotlin.math.abs

@Composable
fun BudgetScreen(
    uiState: SparelyUiState,
    onAddBudget: (BudgetInput) -> Unit,
    onUpdateBudget: (CategoryBudget) -> Unit,
    onDeleteBudget: (Long) -> Unit,
    onNavigateBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var budgetToEdit by remember { mutableStateOf<CategoryBudget?>(null) }
    val budgetLookup = remember(uiState.budgets) {
        uiState.budgets.associateBy { it.category to it.yearMonth }
    }
    val suggestions = uiState.budgetSuggestions
    val summary = uiState.budgetSummary
    val currentMonth = YearMonth.now()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, "Add Budget")
                }
            }
        }

        summary?.let {
            item {
                BudgetSummaryCard(it)
            }
        }

        if (suggestions.isNotEmpty()) {
            item {
                Text(
                    text = "Smart suggestions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            items(suggestions, key = { it.category.name }) { suggestion ->
                val existing = budgetLookup[suggestion.category to currentMonth]
                BudgetSuggestionCard(
                    suggestion = suggestion,
                    currentBudget = existing,
                    onApply = { amount ->
                        val sanitized = amount.coerceAtLeast(0.0)
                        if (existing != null) {
                            onUpdateBudget(existing.copy(monthlyLimit = sanitized))
                        } else {
                            onAddBudget(BudgetInput(suggestion.category, sanitized))
                        }
                    }
                )
            }
        }

        if (summary?.categoryStatuses?.isNotEmpty() == true) {
            item {
                Text(
                    text = "Category Budgets",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            items(summary.categoryStatuses) { status ->
                val budget = budgetLookup[status.category to status.yearMonth]
                CategoryBudgetCard(
                    status = status,
                    onEdit = {
                        budget?.let { budgetToEdit = it }
                    },
                    onDelete = {
                        budget?.let { onDeleteBudget(it.id) }
                    }
                )
            }
        }

        if (uiState.budgets.isEmpty()) {
            item {
                EmptyBudgetState(onAddBudget = { showAddDialog = true })
            }
        }
    }

    if (showAddDialog) {
        AddBudgetDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { budgetInput ->
                onAddBudget(budgetInput)
                showAddDialog = false
            }
        )
    }

    budgetToEdit?.let { editable ->
        EditBudgetDialog(
            budget = editable,
            onDismiss = { budgetToEdit = null },
            onConfirm = { amount ->
                onUpdateBudget(editable.copy(monthlyLimit = amount))
                budgetToEdit = null
            },
            onDelete = {
                onDeleteBudget(editable.id)
                budgetToEdit = null
            }
        )
    }
}

@Composable
fun BudgetSummaryCard(summary: BudgetSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (summary.overallHealth) {
                BudgetHealthStatus.HEALTHY -> Color(0xFF4CAF50)
                BudgetHealthStatus.WARNING -> Color(0xFFFFC107)
                BudgetHealthStatus.CRITICAL -> Color(0xFFFF9800)
                BudgetHealthStatus.OVER_BUDGET -> Color(0xFFF44336)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = YearMonth.now().month.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Budget",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = formatCurrency(summary.totalBudget),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Spent",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = formatCurrency(summary.totalSpent),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { (summary.percentageUsed.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${formatPercent(summary.percentageUsed)} of budget used • ${formatCurrency(summary.totalRemaining)} remaining",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f)
            )

            if (summary.categoriesOverBudget > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${summary.categoriesOverBudget} ${if (summary.categoriesOverBudget == 1) "category" else "categories"} over budget",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryBudgetCard(
    status: BudgetStatus,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = status.category.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${formatCurrency(status.spent)} of ${formatCurrency(status.limit)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StatusBadge(status.status)
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit budget")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete budget")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { (status.percentageUsed.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = when (status.status) {
                    BudgetHealthStatus.HEALTHY -> Color(0xFF4CAF50)
                    BudgetHealthStatus.WARNING -> Color(0xFFFFC107)
                    BudgetHealthStatus.CRITICAL -> Color(0xFFFF9800)
                    BudgetHealthStatus.OVER_BUDGET -> Color(0xFFF44336)
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (status.isOverBudget) {
                        "Over by ${formatCurrency(status.spent - status.limit)}"
                    } else {
                        "${formatCurrency(status.remaining)} left • ${status.daysRemainingInMonth} days"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = formatPercent(status.percentageUsed),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BudgetSuggestionCard(
    suggestion: BudgetSuggestion,
    currentBudget: CategoryBudget?,
    onApply: (Double) -> Unit
) {
    val difference = currentBudget?.let { suggestion.suggestedLimit - it.monthlyLimit }
    val differenceLabel = when {
        difference == null && currentBudget == null -> "Creates a new budget"
        difference == null -> "Matches suggestion"
        abs(difference) < 1.0 -> "≈ current limit"
        difference > 0 -> "Increase by ${formatCurrency(abs(difference))}"
        else -> "Reduce by ${formatCurrency(abs(difference))}"
    }
    val differenceColor = when {
        difference == null -> MaterialTheme.colorScheme.secondary
        abs(difference) < 1.0 -> MaterialTheme.colorScheme.onSurfaceVariant
        difference > 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    val chipColors = when (suggestion.confidence) {
        SuggestionConfidence.HIGH -> AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        SuggestionConfidence.MEDIUM -> AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
        SuggestionConfidence.LOW -> AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = suggestion.category.displayName(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Suggested ${formatCurrency(suggestion.suggestedLimit)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = {},
                    label = { Text(suggestion.confidence.displayLabel()) },
                    colors = chipColors
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Current limit", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = currentBudget?.let { formatCurrency(it.monthlyLimit) } ?: "Not set",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("Historic avg", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = formatCurrency(suggestion.historicalAverage),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Profile target",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatCurrency(suggestion.profileTarget),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = differenceLabel,
                style = MaterialTheme.typography.bodySmall,
                color = differenceColor
            )

            Text(
                text = suggestion.rationale,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(onClick = { onApply(suggestion.suggestedLimit) }) {
                Text(if (currentBudget == null) "Create with suggestion" else "Apply suggestion")
            }
        }
    }
}

@Composable
fun StatusBadge(status: BudgetHealthStatus) {
    AssistChip(
        onClick = {},
        label = {
            Text(
                text = when (status) {
                    BudgetHealthStatus.HEALTHY -> "On Track"
                    BudgetHealthStatus.WARNING -> "Warning"
                    BudgetHealthStatus.CRITICAL -> "Critical"
                    BudgetHealthStatus.OVER_BUDGET -> "Over"
                },
                style = MaterialTheme.typography.labelSmall
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = when (status) {
                BudgetHealthStatus.HEALTHY -> Color(0xFF4CAF50)
                BudgetHealthStatus.WARNING -> Color(0xFFFFC107)
                BudgetHealthStatus.CRITICAL -> Color(0xFFFF9800)
                BudgetHealthStatus.OVER_BUDGET -> Color(0xFFF44336)
            },
            labelColor = Color.White
        )
    )
}

@Composable
fun EmptyBudgetState(onAddBudget: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Budgets Set",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Set category budgets to track and control your spending",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAddBudget) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Budget")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetDialog(
    onDismiss: () -> Unit,
    onConfirm: (BudgetInput) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.GROCERIES) }
    var amount by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Budget") },
        text = {
            Column {
                Text("Category")
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.name.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        ExpenseCategory.values().forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Monthly Limit") },
                    prefix = { Text("$") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    amount.toDoubleOrNull()?.let { limitAmount ->
                        onConfirm(BudgetInput(selectedCategory, limitAmount))
                    }
                },
                enabled = amount.toDoubleOrNull() != null && amount.toDoubleOrNull()!! > 0
            ) {
                Text("Set Budget")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBudgetDialog(
    budget: CategoryBudget,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
    onDelete: () -> Unit
) {
    var amount by remember { mutableStateOf(budget.monthlyLimit.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit ${budget.category.name.lowercase().replaceFirstChar { it.uppercase() }} budget")
        },
        text = {
            Column {
                Text("Monthly Limit")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    prefix = { Text("$") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    amount.toDoubleOrNull()?.let { value ->
                        if (value > 0) {
                            onConfirm(value)
                        }
                    }
                },
                enabled = amount.toDoubleOrNull()?.let { it > 0 } == true
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDelete) {
                    Text("Delete")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

private fun formatCurrency(value: Double): String = "$" + String.format("%,.2f", value)
private fun formatPercent(value: Double): String = String.format("%.1f%%", value.coerceIn(0.0, 2.0) * 100)

private fun SuggestionConfidence.displayLabel(): String = when (this) {
    SuggestionConfidence.HIGH -> "High confidence"
    SuggestionConfidence.MEDIUM -> "Medium confidence"
    SuggestionConfidence.LOW -> "Emerging trend"
}

