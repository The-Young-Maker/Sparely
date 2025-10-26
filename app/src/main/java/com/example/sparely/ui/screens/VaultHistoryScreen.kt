package com.example.sparely.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.example.sparely.ui.components.ExpressiveCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.sparely.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sparely.domain.model.VaultBalanceAdjustment
import com.example.sparely.domain.model.VaultAdjustmentType
import com.example.sparely.ui.theme.MaterialSymbols
import com.example.sparely.ui.theme.MaterialSymbolIcon
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultHistoryScreen(
    vaultName: String,
    adjustments: List<VaultBalanceAdjustment>,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vault_history_title, vaultName)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        MaterialSymbolIcon(icon = MaterialSymbols.ARROW_BACK, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (adjustments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MaterialSymbolIcon(icon = MaterialSymbols.HISTORY,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "No adjustment history yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Manual deposits and withdrawals will appear here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(adjustments) { adjustment ->
                    AdjustmentCard(adjustment)
                }
            }
        }
    }
}

@Composable
private fun AdjustmentCard(adjustment: VaultBalanceAdjustment) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")
    val isDeposit = adjustment.type == VaultAdjustmentType.MANUAL_DEPOSIT
    val isWithdrawal = adjustment.type == VaultAdjustmentType.MANUAL_DEDUCTION
    
    // Calculate old balance from current balance and delta
    val oldBalance = adjustment.resultingBalance - adjustment.delta
    val newBalance = adjustment.resultingBalance
    
    ExpressiveCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MaterialSymbolIcon(
                icon = when (adjustment.type) {
                    VaultAdjustmentType.MANUAL_DEPOSIT -> MaterialSymbols.ADD
                    VaultAdjustmentType.MANUAL_DEDUCTION -> MaterialSymbols.REMOVE
                    VaultAdjustmentType.MANUAL_EDIT -> MaterialSymbols.EDIT
                    VaultAdjustmentType.AUTOMATIC_RECURRING_TRANSFER -> MaterialSymbols.SYNC
                },
                contentDescription = null,
                tint = when {
                    isDeposit -> MaterialTheme.colorScheme.primary
                    isWithdrawal -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp),
                size = 24.dp
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = when (adjustment.type) {
                        VaultAdjustmentType.MANUAL_DEPOSIT -> "Manual Deposit"
                        VaultAdjustmentType.MANUAL_DEDUCTION -> "Manual Withdrawal"
                        VaultAdjustmentType.MANUAL_EDIT -> "Manual Edit"
                        VaultAdjustmentType.AUTOMATIC_RECURRING_TRANSFER -> "Automatic Transfer"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                if (adjustment.reason?.isNotBlank() == true) {
                    Text(
                        text = adjustment.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = adjustment.createdAt.atZone(java.time.ZoneId.systemDefault()).format(dateFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Previous:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${String.format("%.2f", oldBalance)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    MaterialSymbolIcon(icon = MaterialSymbols.ARROW_FORWARD,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${String.format("%.2f", newBalance)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isDeposit -> MaterialTheme.colorScheme.primary
                            isWithdrawal -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${if (adjustment.delta > 0) "+" else ""}$${String.format("%.2f", kotlin.math.abs(adjustment.delta))}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isDeposit -> MaterialTheme.colorScheme.primary
                        isWithdrawal -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}
