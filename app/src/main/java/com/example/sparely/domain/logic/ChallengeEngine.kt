package com.example.sparely.domain.logic

import com.example.sparely.domain.model.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Engine for managing savings challenges and gamification.
 */
object ChallengeEngine {

    /**
     * Create a 52-week savings challenge.
     */
    fun createFiftyTwoWeekChallenge(startDate: LocalDate = LocalDate.now()): SavingsChallenge {
        val endDate = startDate.plusWeeks(52)
        val targetAmount = (1..52).sum().toDouble() // $1,378 total
        
        val milestones = listOf(
            ChallengeMilestone("Quarter 1 Complete", 78.0, rewardPoints = 25),
            ChallengeMilestone("Halfway There!", 378.0, rewardPoints = 50),
            ChallengeMilestone("Three Quarters Done", 858.0, rewardPoints = 75),
            ChallengeMilestone("Challenge Complete!", targetAmount, rewardPoints = 150)
        )

        return SavingsChallenge(
            type = ChallengeType.FIFTY_TWO_WEEK,
            title = "52-Week Money Challenge",
            description = "Save $1 in week 1, $2 in week 2, and so on. You'll save $1,378 in one year!",
            targetAmount = targetAmount,
            startDate = startDate,
            endDate = endDate,
            milestones = milestones
        )
    }

    /**
     * Create a no-spend challenge for specific categories.
     */
    fun createNoSpendChallenge(
        categories: List<ExpenseCategory>,
        durationDays: Int,
        startDate: LocalDate = LocalDate.now()
    ): SavingsChallenge {
        val endDate = startDate.plusDays(durationDays.toLong())
        val categoryNames = categories.joinToString(", ") { it.name.lowercase() }
        
        return SavingsChallenge(
            type = ChallengeType.NO_SPEND_DAYS,
            title = "No-Spend Challenge: $categoryNames",
            description = "Commit to not spending on $categoryNames for $durationDays days",
            targetAmount = 0.0, // Success is binary, not amount-based
            startDate = startDate,
            endDate = endDate
        )
    }

    /**
     * Create a daily savings challenge.
     */
    fun createDailySavingsChallenge(
        dailyAmount: Double,
        durationDays: Int,
        startDate: LocalDate = LocalDate.now()
    ): SavingsChallenge {
        val endDate = startDate.plusDays(durationDays.toLong())
        val targetAmount = dailyAmount * durationDays
        
        val milestones = listOf(
            ChallengeMilestone("Week 1 Complete", dailyAmount * 7, rewardPoints = 10),
            ChallengeMilestone("30 Days Strong", dailyAmount * 30, rewardPoints = 30),
            ChallengeMilestone("Challenge Complete!", targetAmount, rewardPoints = 50)
        )

        return SavingsChallenge(
            type = ChallengeType.DAILY_SAVINGS,
            title = "Daily Savings: $$dailyAmount",
            description = "Save $$dailyAmount every day for $durationDays days",
            targetAmount = targetAmount,
            startDate = startDate,
            endDate = endDate,
            milestones = milestones
        )
    }

    /**
     * Update challenge progress based on new savings.
     */
    fun updateChallengeProgress(
        challenge: SavingsChallenge,
        newAmount: Double
    ): SavingsChallenge {
        val updatedAmount = challenge.currentAmount + newAmount
        val isCompleted = updatedAmount >= challenge.targetAmount

        val updatedMilestones = challenge.milestones.map { milestone ->
            if (!milestone.isAchieved && updatedAmount >= milestone.targetAmount) {
                milestone.copy(
                    isAchieved = true,
                    achievedDate = LocalDate.now()
                )
            } else {
                milestone
            }
        }

        return challenge.copy(
            currentAmount = updatedAmount,
            isCompleted = isCompleted,
            completedDate = if (isCompleted) LocalDate.now() else null,
            milestones = updatedMilestones
        )
    }

    /**
     * Calculate streak days for a challenge.
     */
    fun calculateStreak(
        challenge: SavingsChallenge,
        expenses: List<Expense>
    ): Int {
        if (challenge.type != ChallengeType.NO_SPEND_DAYS) return 0

        val today = LocalDate.now()
        var streakDays = 0
        var checkDate = today

        while (!checkDate.isBefore(challenge.startDate)) {
            val hasSpendingOnDate = expenses.any { 
                it.date == checkDate && 
                // Would need to track challenged categories
                true
            }

            if (hasSpendingOnDate) break
            
            streakDays++
            checkDate = checkDate.minusDays(1)
        }

        return streakDays
    }

    /**
     * Generate achievements based on user activity.
     */
    fun checkForNewAchievements(
        analytics: AnalyticsSnapshot,
        goals: List<Goal>,
        challenges: List<SavingsChallenge>,
        existingAchievements: List<Achievement>
    ): List<Achievement> {
        val newAchievements = mutableListOf<Achievement>()
        val existingTitles = existingAchievements.map { it.title }.toSet()

        // First goal completed
        if (goals.any { it.progressPercent >= 1.0 } && "First Goal Achieved" !in existingTitles) {
            newAchievements.add(
                Achievement(
                    title = "First Goal Achieved",
                    description = "Completed your first savings goal!",
                    icon = "🎯",
                    earnedDate = LocalDate.now(),
                    category = AchievementCategory.GOAL_ACHIEVER
                )
            )
        }

        // Savings milestone: $1000
        if (analytics.totalReserved >= 1000.0 && "Thousand Dollar Club" !in existingTitles) {
            newAchievements.add(
                Achievement(
                    title = "Thousand Dollar Club",
                    description = "Saved your first $1,000!",
                    icon = "💰",
                    earnedDate = LocalDate.now(),
                    category = AchievementCategory.SAVINGS_MILESTONE
                )
            )
        }

        // Savings milestone: $5000
        if (analytics.totalReserved >= 5000.0 && "Five Grand Master" !in existingTitles) {
            newAchievements.add(
                Achievement(
                    title = "Five Grand Master",
                    description = "Accumulated $5,000 in savings!",
                    icon = "💎",
                    earnedDate = LocalDate.now(),
                    category = AchievementCategory.SAVINGS_MILESTONE
                )
            )
        }

        // Challenge completion
        val completedChallenges = challenges.count { it.isCompleted }
        if (completedChallenges >= 1 && "Challenge Champion" !in existingTitles) {
            newAchievements.add(
                Achievement(
                    title = "Challenge Champion",
                    description = "Completed your first savings challenge!",
                    icon = "🏆",
                    earnedDate = LocalDate.now(),
                    category = AchievementCategory.CHALLENGE_WINNER
                )
            )
        }

        return newAchievements
    }

    /**
     * Get recommended challenges based on user profile.
     */
    fun recommendChallenges(
        settings: SparelySettings,
        analytics: AnalyticsSnapshot,
        activeGoals: List<Goal>
    ): List<ChallengeInput> {
        val recommendations = mutableListOf<ChallengeInput>()

        // Recommend 52-week if no major savings yet
        if (analytics.totalReserved < 2000.0) {
            recommendations.add(
                ChallengeInput(
                    type = ChallengeType.FIFTY_TWO_WEEK,
                    title = "52-Week Money Challenge",
                    description = "Build consistent saving habits with incremental weekly goals",
                    targetAmount = 1378.0,
                    endDate = LocalDate.now().plusWeeks(52)
                )
            )
        }

        // Recommend daily savings based on income
        val dailyAmount = (settings.monthlyIncome * 0.01).coerceAtLeast(5.0)
        recommendations.add(
            ChallengeInput(
                type = ChallengeType.DAILY_SAVINGS,
                title = "30-Day Daily Savings",
                description = "Save a small amount every day to build momentum",
                targetAmount = dailyAmount * 30,
                endDate = LocalDate.now().plusDays(30)
            )
        )

        return recommendations
    }
}
