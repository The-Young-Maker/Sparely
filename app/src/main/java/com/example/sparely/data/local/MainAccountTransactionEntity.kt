package com.example.sparely.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "main_account_transactions")
data class MainAccountTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val type: MainAccountTransactionType,
    val amount: Double,
    val balanceAfter: Double,
    val timestamp: LocalDateTime,
    val description: String,
    val relatedExpenseId: Long? = null,
    val relatedVaultContributionIds: String? = null // Comma-separated IDs if multiple
)

enum class MainAccountTransactionType {
    DEPOSIT,           // Manual deposit/income
    WITHDRAWAL,        // Manual withdrawal
    EXPENSE,           // Expense deduction
    VAULT_CONTRIBUTION, // Saving tax to vaults
    ADJUSTMENT         // Manual correction
}
