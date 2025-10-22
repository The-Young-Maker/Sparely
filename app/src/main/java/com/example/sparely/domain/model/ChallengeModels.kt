package com.example.sparely.domain.model

import java.time.LocalDate

/**
 * Savings challenge to gamify the savings experience.
 */
data class SavingsChallenge(
    val id: Long = 0,
    val type: ChallengeType,
    val title: String,
    val description: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isActive: Boolean = true,
    val isCompleted: Boolean = false,
    val completedDate: LocalDate? = null,
    val streakDays: Int = 0,
    val milestones: List<ChallengeMilestone> = emptyList()
) {
    val progressPercent: Double = if (targetAmount > 0) (currentAmount / targetAmount).coerceIn(0.0, 1.0) else 0.0
    val daysRemaining: Int = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), endDate).toInt()
    val nextMilestone: ChallengeMilestone? = milestones.firstOrNull { !it.isAchieved }
}

/**
 * Types of savings challenges.
 */
enum class ChallengeType {
    FIFTY_TWO_WEEK,      // Save incrementally each week for 52 weeks
    NO_SPEND_DAYS,       // Challenge to not spend on certain categories
    CATEGORY_LIMIT,      // Stay under budget in specific category
    SAVE_WINDFALL,       // Save 50%+ of any unexpected income
    ROUND_UP,            // Round up purchases and save difference
    DAILY_SAVINGS,       // Save fixed amount daily
    CUSTOM               // User-defined challenge
}

/**
 * Milestone within a challenge.
 */
data class ChallengeMilestone(
    val description: String,
    val targetAmount: Double,
    val isAchieved: Boolean = false,
    val achievedDate: LocalDate? = null,
    val rewardPoints: Int = 10
)

/**
 * Input for creating a new challenge.
 */
data class ChallengeInput(
    val type: ChallengeType,
    val title: String,
    val description: String,
    val targetAmount: Double,
    val endDate: LocalDate
)

/**
 * Achievement/badge earned by user.
 */
data class Achievement(
    val id: Long = 0,
    val title: String,
    val description: String,
    val icon: String,
    val earnedDate: LocalDate,
    val category: AchievementCategory
)

enum class AchievementCategory {
    SAVINGS_MILESTONE,
    STREAK_MASTER,
    BUDGET_CHAMPION,
    GOAL_ACHIEVER,
    CHALLENGE_WINNER
}
