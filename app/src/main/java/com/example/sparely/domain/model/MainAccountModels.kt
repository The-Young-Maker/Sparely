package com.example.sparely.domain.model

import com.example.sparely.data.local.MainAccountTransactionType
import java.time.LocalDateTime

enum class AccountType(val displayName: String, val description: String) {
    CHECKING("Checking Account", "Standard checking account for daily transactions"),
    SAVINGS("Savings Account", "Regular savings account"),
    TFSA("TFSA", "Tax-Free Savings Account (Canada)"),
    RRSP("RRSP", "Registered Retirement Savings Plan (Canada)"),
    FHSA("FHSA", "First Home Savings Account (Canada)"),
    RESP("RESP", "Registered Education Savings Plan (Canada)"),
    FOUR_OH_ONE_K("401(k)", "401(k) retirement account (USA)"),
    IRA("IRA", "Individual Retirement Account (USA)"),
    ROTH_IRA("Roth IRA", "Roth Individual Retirement Account (USA)"),
    HSA("HSA", "Health Savings Account (USA)"),
    ISA("ISA", "Individual Savings Account (UK)"),
    PENSION("Pension", "Pension account"),
    OTHER("Other", "Other account type")
}

data class MainAccountTransaction(
    val id: Long = 0L,
    val type: MainAccountTransactionType,
    val amount: Double,
    val balanceAfter: Double,
    val timestamp: LocalDateTime,
    val description: String,
    val relatedExpenseId: Long? = null,
    val relatedVaultContributionIds: List<Long>? = null
)

data class MainAccountSummary(
    val currentBalance: Double,
    val recentTransactions: List<MainAccountTransaction>,
    val totalDeposits: Double,
    val totalWithdrawals: Double,
    val totalExpenses: Double,
    val totalVaultContributions: Double
)
