package com.example.sparely.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.sparely.domain.model.*
import com.example.sparely.ui.components.ExpressiveCard
import com.example.sparely.ui.components.SingleLineText
import com.example.sparely.ui.state.SparelyUiState
import com.example.sparely.ui.theme.MaterialSymbolIcon
import com.example.sparely.ui.theme.MaterialSymbols
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
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Add budget button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalButton(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    MaterialSymbolIcon(
                        icon = MaterialSymbols.ADD,
                        contentDescription = "Add Budget",
                        size = 18.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    SingleLineText(text = "Add Budget")
                }
            }
        }

        summary?.let {
            item {
                BudgetSummaryCard(it)
            }
        }
        
        // Warning if total budgets exceed monthly income
        val totalBudgets = uiState.budgets.filter { it.isActive && it.yearMonth == currentMonth }.sumOf { it.monthlyLimit }
        val monthlyIncome = uiState.settings.monthlyIncome
        if (totalBudgets > monthlyIncome && monthlyIncome > 0.0) {
            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MaterialSymbolIcon(
                            icon = MaterialSymbols.WARNING,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            size = 24.dp
                        )
                        Column {
                            Text(
                                text = "Budget exceeds income",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Total budgets (${formatCurrency(totalBudgets)}) exceed your monthly income (${formatCurrency(monthlyIncome)}). Consider adjusting your budgets.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        // Filter suggestions to only show those with meaningful differences
        val meaningfulSuggestions = suggestions.filter { suggestion ->
            val existing = budgetLookup[suggestion.category to currentMonth]
            if (existing == null) {
                // Show if no budget exists
                true
            } else {
                // Only show if difference is significant (more than $1)
                abs(suggestion.suggestedLimit - existing.monthlyLimit) >= 1.0
            }
        }

        if (meaningfulSuggestions.isNotEmpty()) {
            item {
                Text(
                    text = "Smart suggestions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            items(meaningfulSuggestions, key = { it.category.name }) { suggestion ->
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
    val gradientColors = when (summary.overallHealth) {
        BudgetHealthStatus.HEALTHY -> listOf(Color(0xFF4CAF50), Color(0xFF66BB6A))
        BudgetHealthStatus.WARNING -> listOf(Color(0xFFFFC107), Color(0xFFFFD54F))
        BudgetHealthStatus.CRITICAL -> listOf(Color(0xFFFF9800), Color(0xFFFFB74D))
        BudgetHealthStatus.OVER_BUDGET -> listOf(Color(0xFFF44336), Color(0xFFE57373))
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = YearMonth.now().month.name.lowercase().replaceFirstChar { it.uppercase() } + " Budget",
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
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp)),
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
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MaterialSymbolIcon(
                                icon = MaterialSymbols.WARNING,
                                contentDescription = null,
                                tint = Color.White,
                                size = 18.dp
                            )
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
    }
}

@Composable
fun CategoryBudgetCard(
    status: BudgetStatus,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = status.category.displayName(),
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
                        MaterialSymbolIcon(
                            icon = MaterialSymbols.EDIT,
                            contentDescription = "Edit budget",
                            size = 20.dp
                        )
                    }
                    IconButton(onClick = onDelete) {
                        MaterialSymbolIcon(
                            icon = MaterialSymbols.DELETE,
                            contentDescription = "Delete budget",
                            size = 20.dp,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { (status.percentageUsed.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = when (status.status) {
                    BudgetHealthStatus.HEALTHY -> Color(0xFF4CAF50)
                    BudgetHealthStatus.WARNING -> Color(0xFFFFC107)
                    BudgetHealthStatus.CRITICAL -> Color(0xFFFF9800)
                    BudgetHealthStatus.OVER_BUDGET -> Color(0xFFF44336)
                },
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
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

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
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

            FilledTonalButton(
                onClick = { onApply(suggestion.suggestedLimit) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                SingleLineText(text = if (currentBudget == null) "Create with suggestion" else "Apply suggestion")
            }
        }
    }
}

@Composable
fun StatusBadge(status: BudgetHealthStatus) {
    Surface(
        color = when (status) {
            BudgetHealthStatus.HEALTHY -> Color(0xFF4CAF50)
            BudgetHealthStatus.WARNING -> Color(0xFFFFC107)
            BudgetHealthStatus.CRITICAL -> Color(0xFFFF9800)
            BudgetHealthStatus.OVER_BUDGET -> Color(0xFFF44336)
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = when (status) {
                BudgetHealthStatus.HEALTHY -> "On Track"
                BudgetHealthStatus.WARNING -> "Warning"
                BudgetHealthStatus.CRITICAL -> "Critical"
                BudgetHealthStatus.OVER_BUDGET -> "Over"
            },
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun EmptyBudgetState(onAddBudget: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MaterialSymbolIcon(
                icon = MaterialSymbols.ACCOUNT_BALANCE_WALLET,
                contentDescription = null,
                size = 64.dp,
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
            FilledTonalButton(
                onClick = onAddBudget,
                shape = RoundedCornerShape(12.dp)
            ) {
                MaterialSymbolIcon(icon = MaterialSymbols.ADD, contentDescription = null, size = 18.dp)
                Spacer(modifier = Modifier.width(8.dp))
                SingleLineText(text = "Create Budget")
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

    Dialog(onDismissRequest = onDismiss) {
        ExpressiveCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Set Budget",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Column {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory.displayName(),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            ExpenseCategory.values().forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.displayName()) },
                                    onClick = {
                                        selectedCategory = category
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Monthly Limit") },
                    prefix = { Text("$") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            amount.toDoubleOrNull()?.let { limitAmount ->
                                if (limitAmount > 0) {
                                    onConfirm(BudgetInput(selectedCategory, limitAmount))
                                }
                            }
                        },
                        enabled = amount.toDoubleOrNull()?.let { it > 0 } == true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Set Budget")
                    }
                }
            }
        }
    }
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

    Dialog(onDismissRequest = onDismiss) {
        ExpressiveCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit ${budget.category.displayName()} budget",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Monthly Limit") },
                    prefix = { Text("$") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                }

                Button(
                    onClick = {
                        amount.toDoubleOrNull()?.let { value ->
                            if (value > 0) {
                                onConfirm(value)
                            }
                        }
                    },
                    enabled = amount.toDoubleOrNull()?.let { it > 0 } == true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save")
                }
            }
        }
    }
}

private fun formatCurrency(value: Double): String = "$" + String.format("%,.0f", value)
private fun formatPercent(value: Double): String = String.format("%.1f%%", value.coerceIn(0.0, 2.0) * 100)

private fun SuggestionConfidence.displayLabel(): String = when (this) {
    SuggestionConfidence.HIGH -> "High confidence"
    SuggestionConfidence.MEDIUM -> "Medium confidence"
    SuggestionConfidence.LOW -> "Emerging trend"
}

// Extension function for ExpenseCategory.displayName() - using the one from RecurringScreen.kt
// (It's defined as a public function there, so we can use it directly)

