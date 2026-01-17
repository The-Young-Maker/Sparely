package com.example.sparely.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.example.sparely.ui.components.SparelyTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.sparely.data.local.MainAccountTransactionType
import com.example.sparely.domain.model.MainAccountTransaction
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.text.KeyboardOptions
import com.example.sparely.ui.components.SparelyButton
import com.example.sparely.ui.components.SparelyTextButton
import com.example.sparely.ui.theme.MaterialSymbols
import com.example.sparely.ui.theme.MaterialSymbolIcon
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.sparely.ui.components.SparelyTonalButton
import com.sparely.app.R
import androidx.compose.ui.text.style.TextOverflow
import com.example.sparely.domain.model.IncomeCategory
import com.example.sparely.domain.model.displayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAccountScreen(
    currentBalance: Double,
    transactions: List<MainAccountTransaction>,
    onDeposit: (Double, String, com.example.sparely.domain.model.IncomeCategory?) -> Unit,
    onWithdraw: (Double, String) -> Unit,
    onAdjust: (Double, String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var showDepositDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var showAdjustDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(MainAccountFilter.ALL) }

    val filteredTransactions = remember(transactions, selectedFilter) {
        when (selectedFilter) {
            MainAccountFilter.ALL -> transactions
            MainAccountFilter.INCOME -> transactions.filter { 
                it.type == MainAccountTransactionType.DEPOSIT 
            }
            MainAccountFilter.EXPENSE -> transactions.filter { 
                it.type == MainAccountTransactionType.WITHDRAWAL || 
                it.type == MainAccountTransactionType.EXPENSE || 
                it.type == MainAccountTransactionType.VAULT_CONTRIBUTION ||
                it.type == MainAccountTransactionType.CREDIT_CARD_PAYMENT
            }
        }
    }

    Scaffold(
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            // Balance Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp), // Updated to 24.dp
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.main_account_current_balance),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formatCurrency(currentBalance),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SparelyButton(
                        onClick = { showDepositDialog = true },
                        modifier = Modifier.weight(1f),
                        icon = { MaterialSymbolIcon(icon = MaterialSymbols.ADD, null, modifier = Modifier.size(18.dp)) }
                    ) {
                        Text(stringResource(R.string.vault_deposit))
                    }
                    
                    SparelyTonalButton(
                        onClick = { showWithdrawDialog = true },
                        modifier = Modifier.weight(1f),
                        icon = { MaterialSymbolIcon(icon = MaterialSymbols.REMOVE, null, modifier = Modifier.size(18.dp)) }
                    ) {
                        Text(stringResource(R.string.vault_withdraw))
                    }
                }
            }

            item {
                SparelyTextButton(
                    onClick = { showAdjustDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    icon = { MaterialSymbolIcon(icon = MaterialSymbols.EDIT, null, modifier = Modifier.size(18.dp)) }
                ) {
                    Text(stringResource(R.string.main_account_adjust_balance))
                }
            }

            // Filter Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        MainAccountFilter.ALL to stringResource(R.string.filter_all),
                        MainAccountFilter.INCOME to stringResource(R.string.filter_income),
                        MainAccountFilter.EXPENSE to stringResource(R.string.filter_expenses)
                    )
                    
                    filters.forEach { (filter, label) ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(label) },
                            leadingIcon = if (selectedFilter == filter) {
                                { MaterialSymbolIcon(icon = MaterialSymbols.CHECK, contentDescription = null, size = 18.dp) }
                            } else null
                        )
                    }
                }
            }

            // Transaction History Header
            item {
                Text(
                    text = stringResource(R.string.main_account_transaction_history),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }



            if (filteredTransactions.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                             Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                modifier = Modifier.size(80.dp)
                            ) {
                                 Box(contentAlignment = Alignment.Center) {
                                     MaterialSymbolIcon(
                                         icon = MaterialSymbols.RECEIPT,
                                         contentDescription = null,
                                         tint = MaterialTheme.colorScheme.secondary,
                                         size = 40.dp
                                     )
                                 }
                            }
                            Text(
                                text = if (transactions.isEmpty()) stringResource(R.string.main_account_no_transactions) 
                                       else stringResource(R.string.history_search_clear), // Reuse or specific string
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                items(filteredTransactions) { transaction ->
                    TransactionItem(transaction)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showDepositDialog) {
        TransactionDialog(
            title = stringResource(R.string.main_account_deposit_funds),
            icon = MaterialSymbols.ADD,
            positiveLabel = stringResource(R.string.vault_deposit),
            descriptionLabel = stringResource(R.string.income_source_label),
            isIncome = true,
            onDismiss = { showDepositDialog = false },
            onConfirm = { amount, description, category ->
                onDeposit(amount, description, category)
                showDepositDialog = false
            }
        )
    }

    if (showWithdrawDialog) {
        TransactionDialog(
            title = stringResource(R.string.main_account_withdraw_funds),
            icon = MaterialSymbols.REMOVE,
            positiveLabel = stringResource(R.string.vault_withdraw),
            descriptionLabel = stringResource(R.string.vault_reason_label),
            onDismiss = { showWithdrawDialog = false },
            onConfirm = { amount, description, _ ->
                onWithdraw(amount, description)
                showWithdrawDialog = false
            }
        )
    }

    if (showAdjustDialog) {
        AdjustBalanceDialog(
            currentBalance = currentBalance,
            onDismiss = { showAdjustDialog = false },
            onConfirm = { newBalance, reason ->
                onAdjust(newBalance, reason)
                showAdjustDialog = false
            }
        )
    }
}

@Composable
private fun TransactionItem(transaction: MainAccountTransaction) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, HH:mm") } // Shortened date
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(24.dp) // Updated to 24.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp), // Increased padding
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            val (icon, color) = when (transaction.type) {
                MainAccountTransactionType.DEPOSIT -> MaterialSymbols.ARROW_DOWNWARD to MaterialTheme.colorScheme.primary
                MainAccountTransactionType.WITHDRAWAL -> MaterialSymbols.ARROW_UPWARD to MaterialTheme.colorScheme.error
                MainAccountTransactionType.EXPENSE -> MaterialSymbols.SHOPPING_CART to MaterialTheme.colorScheme.tertiary
                MainAccountTransactionType.VAULT_CONTRIBUTION -> MaterialSymbols.SAVINGS to MaterialTheme.colorScheme.secondary
                MainAccountTransactionType.ADJUSTMENT -> MaterialSymbols.EDIT to MaterialTheme.colorScheme.outline
                MainAccountTransactionType.CREDIT_CARD_PAYMENT -> MaterialSymbols.CREDIT_CARD to MaterialTheme.colorScheme.error
            }
            
            Surface(
                shape = RoundedCornerShape(16.dp), // Updated shape
                color = color.copy(alpha = 0.15f), // Slightly more opaque
                modifier = Modifier.size(48.dp) // Larger icon container
            ) {
                Box(contentAlignment = Alignment.Center) {
                    MaterialSymbolIcon(
                        icon = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp), // Larger icon
                        size = 24.dp
                    )
                }
            }

            // Transaction Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (transaction.type == MainAccountTransactionType.DEPOSIT && transaction.incomeCategory != null) {
                    Text(
                        text = transaction.incomeCategory.displayName(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = transaction.timestamp.format(
                        java.time.format.DateTimeFormatter.ofPattern("MMM dd, HH:mm")
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Amount
            Column(
                horizontalAlignment = Alignment.End
            ) {
                val sign = when (transaction.type) {
                    MainAccountTransactionType.DEPOSIT -> "+"
                    MainAccountTransactionType.WITHDRAWAL, 
                    MainAccountTransactionType.EXPENSE,
                    MainAccountTransactionType.VAULT_CONTRIBUTION,
                    MainAccountTransactionType.CREDIT_CARD_PAYMENT -> "-"
                    MainAccountTransactionType.ADJUSTMENT -> ""
                }
                val amountColor = when (transaction.type) {
                    MainAccountTransactionType.DEPOSIT -> MaterialTheme.colorScheme.primary
                    MainAccountTransactionType.WITHDRAWAL,
                    MainAccountTransactionType.EXPENSE,
                    MainAccountTransactionType.VAULT_CONTRIBUTION,
                    MainAccountTransactionType.CREDIT_CARD_PAYMENT -> MaterialTheme.colorScheme.error
                    MainAccountTransactionType.ADJUSTMENT -> MaterialTheme.colorScheme.onSurface
                }
                
                Text(
                    text = "$sign${formatCurrency(kotlin.math.abs(transaction.amount))}",
                    style = MaterialTheme.typography.titleMedium, // Larger amount
                    fontWeight = FontWeight.ExtraBold,
                    color = amountColor
                )
                Text(
                    text = formatCurrency(transaction.balanceAfter),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDialog(
    title: String,
    icon: Int,
    positiveLabel: String,
    descriptionLabel: String,
    isIncome: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, com.example.sparely.domain.model.IncomeCategory?) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<com.example.sparely.domain.model.IncomeCategory?>(null) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            MaterialSymbolIcon(icon = icon, contentDescription = null)
        },
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SparelyTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text(stringResource(R.string.vault_amount_label)) },
                    prefix = { Text(stringResource(R.string.currency_prefix)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                if (isIncome) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        SparelyTextField(
                            value = selectedCategory?.displayName() ?: "Select Category",
                            onValueChange = {},
                            label = { Text("Category") },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { showCategoryDropdown = true }) {
                                    MaterialSymbolIcon(icon = MaterialSymbols.ARROW_DROP_DOWN, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = showCategoryDropdown,
                            onDismissRequest = { showCategoryDropdown = false }
                        ) {
                            com.example.sparely.domain.model.IncomeCategory.values().forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.displayName()) },
                                    onClick = {
                                        selectedCategory = category
                                        showCategoryDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                SparelyTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(descriptionLabel) },
                    placeholder = { Text(stringResource(R.string.main_account_optional_note)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            SparelyButton(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue != null && amountValue > 0) {
                        onConfirm(amountValue, description.ifEmpty { title }, selectedCategory)
                    }
                },
                enabled = amount.toDoubleOrNull()?.let { it > 0 } == true
            ) {
                Text(positiveLabel)
            }
        },
        dismissButton = {
            SparelyTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdjustBalanceDialog(
    currentBalance: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var newBalance by remember { mutableStateOf(String.format("%.2f", currentBalance)) }
    var reason by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            MaterialSymbolIcon(icon = MaterialSymbols.EDIT, contentDescription = null)
        },
        title = { Text(stringResource(R.string.main_account_adjust_balance)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.vault_current, formatCurrency(currentBalance)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                SparelyTextField(
                    value = newBalance,
                    onValueChange = { newBalance = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text(stringResource(R.string.main_account_new_balance)) },
                    prefix = { Text(stringResource(R.string.currency_prefix)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                SparelyTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(stringResource(R.string.vault_reason_label)) },
                    placeholder = { Text(stringResource(R.string.main_account_adjust_reason_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            SparelyButton(
                onClick = {
                    val balanceValue = newBalance.toDoubleOrNull()
                    if (balanceValue != null && balanceValue >= 0) {
                        onConfirm(balanceValue, reason.ifEmpty { context.getString(R.string.main_account_adjustment_default_reason) })
                    }
                },
                enabled = newBalance.toDoubleOrNull()?.let { it >= 0 } == true
            ) {
                Text(stringResource(R.string.main_account_adjust_button))
            }
        },
        dismissButton = {
            SparelyTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

private fun formatCurrency(value: Double): String = "$" + String.format("%,.2f", value)

private enum class MainAccountFilter {
    ALL, INCOME, EXPENSE
}

