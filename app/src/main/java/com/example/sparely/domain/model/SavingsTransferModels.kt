package com.example.sparely.domain.model

import java.time.LocalDate

/**
 * Captures a manual savings transfer made outside expense logging.
 */
data class SavingsTransfer(
    val id: Long,
    val category: SavingsCategory,
    val amount: Double,
    val date: LocalDate,
    val sourceAccountId: Long? = null,
    val destinationAccountId: Long? = null,
    val note: String? = null
)
