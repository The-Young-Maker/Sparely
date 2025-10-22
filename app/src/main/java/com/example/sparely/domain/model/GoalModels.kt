package com.example.sparely.domain.model

import java.time.LocalDate

/**
 * Domain representation of a savings goal.
 */
data class Goal(
    val id: Long,
    val title: String,
    val targetAmount: Double,
    val category: SavingsCategory,
    val targetDate: LocalDate?,
    val createdAt: LocalDate,
    val notes: String?,
    val archived: Boolean,
    val progressAmount: Double,
    val progressPercent: Double,
    val projectedCompletion: LocalDate?
)

/**
 * User input when adding or editing a goal.
 */
data class GoalInput(
    val title: String,
    val targetAmount: Double,
    val category: SavingsCategory,
    val targetDate: LocalDate?,
    val notes: String?
)
