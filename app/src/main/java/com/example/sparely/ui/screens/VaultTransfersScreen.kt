package com.example.sparely.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import com.example.sparely.ui.theme.MaterialSymbols
import com.example.sparely.ui.theme.MaterialSymbolIcon
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.example.sparely.ui.components.ExpressiveCard
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sparely.domain.model.SmartVault
import com.example.sparely.domain.model.VaultContribution
import com.example.sparely.domain.model.VaultContributionSource
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultTransfersScreen(
    vaults: List<SmartVault>,
    pendingContributions: List<VaultContribution>,
    onApproveContribution: (Long) -> Unit,
    onApproveGroup: (List<Long>) -> Unit = { ids -> ids.forEach(onApproveContribution) },
    onCancelContribution: (Long) -> Unit = {},
    onStartNotificationWorkflow: () -> Unit = {},
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pending Transfers") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (pendingContributions.isEmpty()) {
                item {
                    EmptyStateCard()
                }
            } else {
                item {
                    SummaryCard(pendingContributions, vaults)
                }
                
                item {
                    NotificationWorkflowButton(onStartWorkflow = onStartNotificationWorkflow)
                }

                
                
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Pending Contributions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                val grouped = pendingContributions.groupBy { it.vaultId }
                items(grouped.entries.toList(), key = { it.key ?: -1L }) { (vaultId, contributionsForVault) ->
                    val vault = vaults.find { it.id == vaultId }
                    AggregatedPendingContributionCard(
                        vault = vault,
                        contributions = contributionsForVault,
                        onApproveAll = { ids -> onApproveGroup(ids) },
                        onApproveIndividual = onApproveContribution,
                        onCancelIndividual = onCancelContribution
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                InstructionsCard()
            }
        }
    }
}

@Composable
private fun EmptyStateCard() {
    ExpressiveCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MaterialSymbolIcon(icon = MaterialSymbols.CHECK_CIRCLE,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "All Caught Up!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "You have no pending vault transfers. Keep logging expenses to accumulate saving tax contributions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SummaryCard(
    pendingContributions: List<VaultContribution>,
    vaults: List<SmartVault>
) {
    val totalPending = pendingContributions.sumOf { it.amount }
    val vaultBreakdown = pendingContributions
        .groupBy { it.vaultId }
        .mapValues { (_, contributions) -> contributions.sumOf { it.amount } }
    
    ExpressiveCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer
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
                Text(
                    text = "Total to Transfer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format("$%.2f", totalPending),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            HorizontalDivider()
            
            vaultBreakdown.forEach { (vaultId, amount) ->
                val vault = vaults.find { it.id == vaultId }
                if (vault != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = vault.name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = String.format("$%.2f", amount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AggregatedPendingContributionCard(
    vault: SmartVault?,
    contributions: List<VaultContribution>,
    onApproveAll: (List<Long>) -> Unit,
    onApproveIndividual: (Long) -> Unit,
    onCancelIndividual: (Long) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    var showConfirmAll by remember { mutableStateOf(false) }

    val totalAmount = contributions.sumOf { it.amount }
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val sources = contributions.groupBy { it.source }
    val notes = contributions.mapNotNull { it.note?.takeIf { note -> note.isNotBlank() } }

    ExpressiveCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
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
                        text = vault?.name ?: "Unknown Vault",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${contributions.size} pending ${if (contributions.size == 1) "transfer" else "transfers"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = String.format("$%.2f", totalAmount),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sources.entries.sortedByDescending { it.value.sumOf(VaultContribution::amount) }.forEach { (source, entries) ->
                    val sourceTotal = entries.sumOf { it.amount }
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = {
                            Text(
                                when (source) {
                                    VaultContributionSource.SAVING_TAX -> "Saving tax ${String.format("$%.2f", sourceTotal)}"
                                    VaultContributionSource.INCOME -> "Income ${String.format("$%.2f", sourceTotal)}"
                                    VaultContributionSource.AUTO_DEPOSIT -> "Auto deposit ${String.format("$%.2f", sourceTotal)}"
                                    VaultContributionSource.MANUAL -> "Manual ${String.format("$%.2f", sourceTotal)}"
                                    VaultContributionSource.TRANSFER -> "Transfer ${String.format("$%.2f", sourceTotal)}"
                                }
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }

            val notePreview = notes.take(3)
            if (notePreview.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    notePreview.forEach { note ->
                        Text(
                            text = "• $note",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (notes.size > notePreview.size) {
                        Text(
                            text = "${notes.size - notePreview.size} more note${if (notes.size - notePreview.size == 1) "" else "s"} hidden",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Button(
                onClick = { showConfirmAll = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                MaterialSymbolIcon(icon = MaterialSymbols.CHECK_CIRCLE,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mark all as transferred")
            }

            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Hide individual contributions" else "See individual contributions")
            }

            if (expanded) {
                HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    contributions.sortedByDescending { it.date }.forEach { contribution ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = contribution.date.format(formatter),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    contribution.note?.takeIf { it.isNotBlank() }?.let { note ->
                                        Text(
                                            text = note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = String.format("$%.2f", contribution.amount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AssistChip(
                                    onClick = {},
                                    enabled = false,
                                    label = {
                                        Text(
                                            when (contribution.source) {
                                                VaultContributionSource.SAVING_TAX -> "Saving tax"
                                                VaultContributionSource.INCOME -> "Income"
                                                VaultContributionSource.AUTO_DEPOSIT -> "Auto deposit"
                                                VaultContributionSource.MANUAL -> "Manual"
                                                VaultContributionSource.TRANSFER -> "Transfer"
                                            }
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { onApproveIndividual(contribution.id) }) {
                                        Text("Approve")
                                    }
                                    TextButton(onClick = { onCancelIndividual(contribution.id) }) {
                                        Text("Cancel")
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (showConfirmAll) {
        AlertDialog(
            onDismissRequest = { showConfirmAll = false },
            title = { Text("Confirm transfer") },
            text = {
                Text("Have you moved $${String.format("%.2f", totalAmount)} to ${vault?.name}? All ${contributions.size} entries will be marked as transferred.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onApproveAll(contributions.map { it.id })
                        showConfirmAll = false
                    }
                ) {
                    Text("Yes, all done")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmAll = false }) {
                    Text("Not yet")
                }
            }
        )
    }
}

@Composable
private fun InstructionsCard() {
    ExpressiveCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MaterialSymbolIcon(icon = MaterialSymbols.INFO,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "How It Works",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            InstructionStep(
                number = "1",
                text = "Sparely calculates saving tax contributions from your logged expenses"
            )
            InstructionStep(
                number = "2",
                text = "Open your banking app and transfer the amounts to your actual savings accounts"
            )
            InstructionStep(
                number = "3",
                text = "Return here and tap \"Mark as Transferred\" to update your vault balances"
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f))
                    .padding(12.dp)
            ) {
                Text(
                    text = "💡 Tip: You can batch transfers weekly to minimize transaction fees",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
private fun InstructionStep(number: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.tertiary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiary
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun NotificationWorkflowButton(onStartWorkflow: () -> Unit) {
    ExpressiveCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MaterialSymbolIcon(icon = MaterialSymbols.NOTIFICATIONS,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Transfer via Notification",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = "Use step-by-step notifications to transfer funds without switching back to Sparely. Each notification shows one vault at a time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            Button(
                onClick = onStartWorkflow,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                MaterialSymbolIcon(icon = MaterialSymbols.PLAY_ARROW,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Notification Workflow")
            }
        }
    }
}
