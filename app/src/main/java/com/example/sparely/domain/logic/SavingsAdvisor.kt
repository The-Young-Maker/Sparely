package com.example.sparely.domain.logic

import com.example.sparely.domain.model.EducationStatus
import com.example.sparely.domain.model.EmploymentStatus
import com.example.sparely.domain.model.RiskLevel
import com.example.sparely.domain.model.SavingsAccountInput
import com.example.sparely.domain.model.SavingsCategory
import com.example.sparely.domain.model.SavingsPercentages
import com.example.sparely.domain.model.SmartVaultSetup
import com.example.sparely.domain.model.UserProfileSetup
import com.example.sparely.domain.model.toSmartVaultSetup
import kotlin.math.max

/**
 * Provides heuristics that tailor savings advice to the user's profile.
 */
object SavingsAdvisor {

    fun recommendedPercentages(profile: UserProfileSetup): SavingsPercentages {
        var emergencyShare = when {
            profile.age < 25 -> 0.22
            profile.age < 40 -> 0.18
            profile.age < 55 -> 0.20
            else -> 0.22
        }
        var investShare = when {
            profile.age < 25 -> 0.06
            profile.age < 40 -> 0.08
            profile.age < 55 -> 0.07
            else -> 0.05
        }
        var funShare = when {
            profile.age < 25 -> 0.05
            profile.age < 40 -> 0.04
            profile.age < 55 -> 0.03
            else -> 0.03
        }

        if (profile.hasDebts) {
            emergencyShare += 0.04
            investShare -= 0.03
            funShare -= 0.01
        }

        if (profile.employmentStatus == EmploymentStatus.STUDENT || profile.employmentStatus == EmploymentStatus.UNEMPLOYED) {
            emergencyShare += 0.03
            investShare -= 0.02
        }

        if (profile.currentEmergencyFund < max(500.0, profile.monthlyIncome)) {
            emergencyShare += 0.03
        }

        when (profile.riskLevel) {
            RiskLevel.AGGRESSIVE -> {
                investShare += 0.03
                emergencyShare -= 0.02
            }
            RiskLevel.CONSERVATIVE -> {
                emergencyShare += 0.02
                investShare -= 0.01
                funShare -= 0.01
            }
            else -> Unit
        }

        emergencyShare = emergencyShare.coerceAtLeast(0.10)
        investShare = investShare.coerceAtLeast(0.04)
        funShare = funShare.coerceAtLeast(0.03)

        val safeSplit = when (profile.riskLevel) {
            RiskLevel.AGGRESSIVE -> 0.4
            RiskLevel.CONSERVATIVE -> 0.8
            else -> 0.65
        }

        val raw = SavingsPercentages(
            emergency = emergencyShare,
            invest = investShare,
            `fun` = funShare,
            safeInvestmentSplit = safeSplit
        )

        val adjusted = raw.adjustWithinBudget(maxTotal = 0.35)
        return adjusted.copy(safeInvestmentSplit = safeSplit)
    }

    fun recommendedAccounts(profile: UserProfileSetup): List<SavingsAccountInput> = recommendedAccounts(
        age = profile.age,
        educationStatus = profile.educationStatus,
        employmentStatus = profile.employmentStatus,
        hasDebts = profile.hasDebts,
        emergencyFund = profile.currentEmergencyFund,
        monthlyIncome = profile.monthlyIncome
    )

    fun recommendedVaults(profile: UserProfileSetup): List<SmartVaultSetup> = recommendedVaults(
        age = profile.age,
        educationStatus = profile.educationStatus,
        employmentStatus = profile.employmentStatus,
        hasDebts = profile.hasDebts,
        emergencyFund = profile.currentEmergencyFund,
        monthlyIncome = profile.monthlyIncome
    )

    fun recommendedVaults(
        age: Int,
        educationStatus: EducationStatus,
        employmentStatus: EmploymentStatus,
        hasDebts: Boolean,
        emergencyFund: Double,
        monthlyIncome: Double
    ): List<SmartVaultSetup> {
        return recommendedAccounts(
            age = age,
            educationStatus = educationStatus,
            employmentStatus = employmentStatus,
            hasDebts = hasDebts,
            emergencyFund = emergencyFund,
            monthlyIncome = monthlyIncome
        ).map { account ->
            account.toSmartVaultSetup(monthlyIncome)
        }
    }

    fun recommendedAccounts(
        age: Int,
        educationStatus: EducationStatus,
        employmentStatus: EmploymentStatus,
        hasDebts: Boolean,
        emergencyFund: Double,
        monthlyIncome: Double
    ): List<SavingsAccountInput> {
        val plans = mutableListOf<SavingsAccountInput>()
        val sanitizedIncome = monthlyIncome.coerceAtLeast(0.0)
        val sanitizedEmergency = emergencyFund.coerceAtLeast(0.0)

        fun addPlan(
            name: String,
            category: SavingsCategory,
            target: Double?,
            primarySuggestion: Boolean,
            currentBalance: Double = 0.0,
            reminderDays: Int = 7,
            reminderEnabled: Boolean = true
        ) {
            plans += SavingsAccountInput(
                name = name,
                category = category,
                institution = null,
                accountNumber = null,
                currentBalance = currentBalance,
                targetBalance = target?.takeIf { it > 0.0 },
                isPrimary = primarySuggestion,
                reminderFrequencyDays = if (reminderEnabled) reminderDays else null,
                reminderEnabled = reminderEnabled
            )
        }

        val emergencyTarget = when {
            sanitizedIncome <= 0.0 -> 1000.0
            age < 30 -> sanitizedIncome * 1.5
            age < 45 -> sanitizedIncome * 3
            age < 60 -> sanitizedIncome * 4
            else -> sanitizedIncome * 5
        }.coerceAtLeast(500.0)
        val emergencyName = when {
            age < 23 -> "Emergency cushion"
            age < 35 -> "Emergency fund"
            age < 50 -> "Emergency reserve"
            else -> "Health & emergency buffer"
        }
        addPlan(
            name = emergencyName,
            category = SavingsCategory.EMERGENCY,
            target = emergencyTarget,
            primarySuggestion = true,
            currentBalance = sanitizedEmergency,
            reminderDays = 7
        )

        val growthName = when {
            age < 23 -> "Future growth stash"
            age < 35 -> "Retirement & growth"
            age < 50 -> "Retirement accelerator"
            else -> "Retirement income bridge"
        }
        val growthTarget = when {
            sanitizedIncome <= 0.0 -> null
            age < 30 -> sanitizedIncome
            age < 45 -> sanitizedIncome * 1.5
            else -> sanitizedIncome * 2
        }
        addPlan(
            name = growthName,
            category = SavingsCategory.INVESTMENT,
            target = growthTarget,
            primarySuggestion = true,
            reminderDays = 14
        )

        val lifestyleName = when {
            age < 23 -> "Fun & experiences"
            age < 35 -> "Lifestyle & adventures"
            age < 50 -> "Family experiences"
            else -> "Leisure & hobbies"
        }
        val lifestyleTarget = if (sanitizedIncome > 0) sanitizedIncome * 0.3 else null
        addPlan(
            name = lifestyleName,
            category = SavingsCategory.FUN,
            target = lifestyleTarget,
            primarySuggestion = true,
            reminderDays = 14
        )

        if (age < 23 && educationStatus == EducationStatus.HIGH_SCHOOL) {
            addPlan(
                name = "University fund",
                category = SavingsCategory.INVESTMENT,
                target = max(1000.0, sanitizedIncome * 6),
                primarySuggestion = false,
                reminderDays = 30
            )
        }
        if (age < 25 && educationStatus == EducationStatus.UNIVERSITY) {
            addPlan(
                name = "Textbooks & supplies",
                category = SavingsCategory.INVESTMENT,
                target = max(1500.0, sanitizedIncome * 4),
                primarySuggestion = false,
                reminderDays = 30
            )
            addPlan(
                name = "First apartment fund",
                category = SavingsCategory.INVESTMENT,
                target = max(2500.0, sanitizedIncome * 6),
                primarySuggestion = false,
                reminderDays = 30
            )
        }

        if (age in 25..40 && sanitizedIncome > 0.0) {
            addPlan(
                name = "Down payment fund",
                category = SavingsCategory.INVESTMENT,
                target = max(20000.0, sanitizedIncome * 10),
                primarySuggestion = false,
                reminderDays = 30
            )
        }

        if (age in 35..55 && (employmentStatus == EmploymentStatus.EMPLOYED || employmentStatus == EmploymentStatus.SELF_EMPLOYED)) {
            addPlan(
                name = "Kids education fund",
                category = SavingsCategory.INVESTMENT,
                target = max(15000.0, sanitizedIncome * 12),
                primarySuggestion = false,
                reminderDays = 30
            )
        }

        if (employmentStatus == EmploymentStatus.SELF_EMPLOYED) {
            addPlan(
                name = "Quarterly tax reserve",
                category = SavingsCategory.EMERGENCY,
                target = max(500.0, sanitizedIncome * 0.35),
                primarySuggestion = false,
                reminderDays = 30
            )
        }

        if (hasDebts) {
            addPlan(
                name = "Debt freedom buffer",
                category = SavingsCategory.EMERGENCY,
                target = max(800.0, sanitizedIncome),
                primarySuggestion = false,
                reminderDays = 14
            )
        }

        if (employmentStatus == EmploymentStatus.RETIRED) {
            addPlan(
                name = "Healthcare buffer",
                category = SavingsCategory.EMERGENCY,
                target = max(1000.0, sanitizedIncome * 0.5),
                primarySuggestion = false,
                reminderDays = 30
            )
            addPlan(
                name = "Legacy planning",
                category = SavingsCategory.INVESTMENT,
                target = max(5000.0, sanitizedIncome * 3),
                primarySuggestion = false,
                reminderDays = 30
            )
        }

        val primaryByCategory = mutableSetOf<SavingsCategory>()
        plans.forEachIndexed { index, input ->
            if (input.isPrimary) {
                primaryByCategory.add(input.category)
            } else if (!primaryByCategory.contains(input.category)) {
                plans[index] = input.copy(isPrimary = true)
                primaryByCategory.add(input.category)
            }
        }

        SavingsCategory.entries.forEach { category ->
            val indices = plans.indices.filter { plans[it].category == category }
            if (indices.isNotEmpty() && indices.none { plans[it].isPrimary }) {
                val firstIndex = indices.first()
                plans[firstIndex] = plans[firstIndex].copy(isPrimary = true)
            }
        }

        return plans
    }
}
