package com.example.sparely.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import com.example.sparely.ui.theme.MaterialSymbols
import com.example.sparely.ui.theme.MaterialSymbolIcon
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.sparely.domain.model.Goal
import com.example.sparely.domain.model.GoalInput
import com.example.sparely.domain.model.RecommendationResult
import com.example.sparely.domain.model.SavingsCategory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun GoalsScreen(
    goals: List<Goal>,
    recommendation: RecommendationResult?,
    onAddGoal: (GoalInput) -> Unit,
    onArchiveToggle: (Long, Boolean) -> Unit,
    onDeleteGoal: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            recommendation?.let {
                Text(
                    text = "Tip: investing split ${formatPercent(it.safeInvestmentRatio)} safe / ${formatPercent(it.highRiskInvestmentRatio)} high risk",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            GoalComposer(onAddGoal = onAddGoal)
        }
        items(goals) { goal ->
            GoalCard(
                goal = goal,
                onArchiveToggle = { onArchiveToggle(goal.id, it) },
                onDeleteGoal = { onDeleteGoal(goal.id) }
            )
        }
        if (goals.isEmpty()) {
            item {
                EmptyGoalsState()
            }
        }
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun GoalComposer(onAddGoal: (GoalInput) -> Unit) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var targetAmountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(SavingsCategory.EMERGENCY) }
    var targetDate by remember { mutableStateOf<LocalDate?>(null) }
    var notes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    if (showDatePicker) {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                targetDate = LocalDate.of(year, month + 1, day)
                showDatePicker = false
            },
            (targetDate ?: LocalDate.now()).year,
            (targetDate ?: LocalDate.now()).monthValue - 1,
            (targetDate ?: LocalDate.now()).dayOfMonth
        ).apply {
            setOnDismissListener { showDatePicker = false }
        }.show()
    }

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Create goal", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = targetAmountText,
                onValueChange = { targetAmountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text("Target amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            CategorySegmentedControl(
                selected = selectedCategory,
                onChange = { selectedCategory = it }
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("Target date")
                    Text(
                        text = targetDate?.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) ?: "Flexible",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(onClick = { showDatePicker = true }) {
                    Text(if (targetDate == null) "Set" else "Change")
                }
            }
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
            Button(onClick = {
                val targetAmount = targetAmountText.toDoubleOrNull()
                if (targetAmount == null || targetAmount <= 0.0) {
                    error = "Enter a target amount"
                    return@Button
                }
                error = null
                onAddGoal(
                    GoalInput(
                        title = title,
                        targetAmount = targetAmount,
                        category = selectedCategory,
                        targetDate = targetDate,
                        notes = notes.ifBlank { null }
                    )
                )
                title = ""
                targetAmountText = ""
                notes = ""
                targetDate = null
            }) {
                Text("Add goal")
            }
        }
    }
}

@Composable
private fun CategorySegmentedControl(
    selected: SavingsCategory,
    onChange: (SavingsCategory) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (category in SavingsCategory.values()) {
            FilterChip(
                selected = category == selected,
                onClick = { onChange(category) },
                label = { Text(category.name.lowercase().replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}

@Composable
private fun GoalCard(
    goal: Goal,
    onArchiveToggle: (Boolean) -> Unit,
    onDeleteGoal: () -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(goal.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Target ${formatCurrency(goal.targetAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDeleteGoal) {
                    MaterialSymbolIcon(icon = MaterialSymbols.DELETE, contentDescription = "Delete goal")
                }
            }
            LinearProgressIndicator(
            progress = { goal.progressPercent.coerceIn(0.0, 1.0).toFloat() },
            modifier = Modifier.fillMaxWidth(),
            color = ProgressIndicatorDefaults.linearColor,
            trackColor = ProgressIndicatorDefaults.linearTrackColor,
            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
            )
            Text(
                text = "Saved ${formatCurrency(goal.progressAmount)} (${formatPercent(goal.progressPercent)})",
                style = MaterialTheme.typography.bodySmall
            )
            goal.targetDate?.let { date ->
                Text(
                    text = "Deadline ${date.format(formatter)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            goal.projectedCompletion?.let { projection ->
                Text(
                    text = "Projected completion ${projection.format(formatter)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (goal.archived) "Archived" else "Active")
                TextButton(onClick = { onArchiveToggle(!goal.archived) }) {
                    Text(if (goal.archived) "Restore" else "Archive")
                }
            }
        }
    }
}

@Composable
private fun EmptyGoalsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No goals yet", style = MaterialTheme.typography.titleSmall)
        Text(
            text = "Create a savings goal to see projections.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatCurrency(value: Double): String = "$" + String.format("%,.2f", value)

private fun formatPercent(value: Double): String = String.format("%.1f%%", value.coerceIn(0.0, 1.0) * 100)
