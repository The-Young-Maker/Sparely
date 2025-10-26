package com.example.sparely.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import com.example.sparely.ui.theme.MaterialSymbols
import com.example.sparely.ui.theme.MaterialSymbolIcon
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAccountScreen(
    currentBalance: Double,
    transactions: List<MainAccountTransaction>,
    onDeposit: (Double, String) -> Unit,
    onWithdraw: (Double, String) -> Unit,
    onAdjust: (Double, String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var showDepositDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var showAdjustDialog by remember { mutableStateOf(false) }

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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Current Balance",
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
                    Button(
                        onClick = { showDepositDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        MaterialSymbolIcon(icon = MaterialSymbols.ADD, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Deposit")
                    }
                    
                    OutlinedButton(
                        onClick = { showWithdrawDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        MaterialSymbolIcon(icon = MaterialSymbols.REMOVE, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Withdraw")
                    }
                }
            }

            item {
                TextButton(
                    onClick = { showAdjustDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MaterialSymbolIcon(icon = MaterialSymbols.EDIT, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Adjust Balance")
                }
            }

            // Transaction History Header
            item {
                Text(
                    text = "Transaction History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (transactions.isEmpty()) {
                item {
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
                            MaterialSymbolIcon(icon = MaterialSymbols.RECEIPT,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No transactions yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(transactions) { transaction ->
                    TransactionItem(transaction)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showDepositDialog) {
        TransactionDialog(
            title = "Deposit Funds",
            icon = MaterialSymbols.ADD,
            positiveLabel = "Deposit",
            onDismiss = { showDepositDialog = false },
            onConfirm = { amount, description ->
                onDeposit(amount, description)
                showDepositDialog = false
            }
        )
    }

    if (showWithdrawDialog) {
        TransactionDialog(
            title = "Withdraw Funds",
            icon = MaterialSymbols.REMOVE,
            positiveLabel = "Withdraw",
            onDismiss = { showWithdrawDialog = false },
            onConfirm = { amount, description ->
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
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm") }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            val (icon, color) = when (transaction.type) {
                MainAccountTransactionType.DEPOSIT -> MaterialSymbols.ARROW_DOWNWARD to MaterialTheme.colorScheme.primary
                MainAccountTransactionType.WITHDRAWAL -> MaterialSymbols.ARROW_UPWARD to MaterialTheme.colorScheme.error
                MainAccountTransactionType.EXPENSE -> MaterialSymbols.SHOPPING_CART to MaterialTheme.colorScheme.tertiary
                MainAccountTransactionType.VAULT_CONTRIBUTION -> MaterialSymbols.SAVINGS to MaterialTheme.colorScheme.secondary
                MainAccountTransactionType.ADJUSTMENT -> MaterialSymbols.EDIT to MaterialTheme.colorScheme.outline
            }
            
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    MaterialSymbolIcon(
                        icon = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp),
                        size = 20.dp
                    )
                }
            }

            // Transaction Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = transaction.timestamp.format(dateFormatter),
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
                    MainAccountTransactionType.VAULT_CONTRIBUTION -> "-"
                    MainAccountTransactionType.ADJUSTMENT -> ""
                }
                val amountColor = when (transaction.type) {
                    MainAccountTransactionType.DEPOSIT -> MaterialTheme.colorScheme.primary
                    MainAccountTransactionType.WITHDRAWAL,
                    MainAccountTransactionType.EXPENSE,
                    MainAccountTransactionType.VAULT_CONTRIBUTION -> MaterialTheme.colorScheme.error
                    MainAccountTransactionType.ADJUSTMENT -> MaterialTheme.colorScheme.onSurface
                }
                
                Text(
                    text = "$sign${formatCurrency(transaction.amount)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                Text(
                    text = formatCurrency(transaction.balanceAfter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

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
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Amount") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("Optional note") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue != null && amountValue > 0) {
                        onConfirm(amountValue, description.ifEmpty { title })
                    }
                },
                enabled = amount.toDoubleOrNull()?.let { it > 0 } == true
            ) {
                Text(positiveLabel)
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
private fun AdjustBalanceDialog(
    currentBalance: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var newBalance by remember { mutableStateOf(String.format("%.2f", currentBalance)) }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            MaterialSymbolIcon(icon = MaterialSymbols.EDIT, contentDescription = null)
        },
        title = { Text("Adjust Balance") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Current: ${formatCurrency(currentBalance)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = newBalance,
                    onValueChange = { newBalance = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("New Balance") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
                    placeholder = { Text("Why adjust the balance?") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val balanceValue = newBalance.toDoubleOrNull()
                    if (balanceValue != null && balanceValue >= 0) {
                        onConfirm(balanceValue, reason.ifEmpty { "Balance adjustment" })
                    }
                },
                enabled = newBalance.toDoubleOrNull()?.let { it >= 0 } == true
            ) {
                Text("Adjust")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatCurrency(value: Double): String = "$" + String.format("%,.2f", value)
