package com.example.sparely.domain.logic

import com.example.sparely.domain.model.*
import java.time.LocalDate
import java.time.YearMonth

/**
 * Generates starter budgets and vaults based on user profile.
 * Helps new users get started quickly with sensible defaults.
 */
object OnboardingHelper {
    
    /**
     * Generate recommended starter budgets based on user profile.
     */
    fun generateStarterBudgets(profile: UserProfileSetup): List<CategoryBudget> {
        val monthlyIncome = profile.monthlyIncome
        if (monthlyIncome <= 0.0) return emptyList()
        
        val budgets = mutableListOf<CategoryBudget>()
        
        // Get recommended category allocations based on profile
        val shares = getRecommendedCategoryShares(profile)
        
        // Create budgets for major categories
        shares.forEach { (category, share) ->
            // Only create budgets for categories with meaningful allocation (> 2%)
            if (share >= 0.02) {
                val amount = monthlyIncome * share
                budgets.add(
                    CategoryBudget(
                        id = 0, // Will be assigned by database
                        category = category,
                        monthlyLimit = amount,
                        yearMonth = YearMonth.now(),
                        isActive = true
                    )
                )
            }
        }
        
        return budgets
    }
    
    /**
     * Get recommended category spending shares based on user profile.
     */
    private fun getRecommendedCategoryShares(profile: UserProfileSetup): Map<ExpenseCategory, Double> {
        val baseCategoryShares = mapOf(
            ExpenseCategory.GROCERIES to 0.18,
            ExpenseCategory.DINING to 0.08,
            ExpenseCategory.TRANSPORTATION to 0.12,
            ExpenseCategory.ENTERTAINMENT to 0.08,
            ExpenseCategory.UTILITIES to 0.17,
            ExpenseCategory.HEALTH to 0.08,
            ExpenseCategory.EDUCATION to 0.07,
            ExpenseCategory.SHOPPING to 0.10,
            ExpenseCategory.TRAVEL to 0.07,
            ExpenseCategory.OTHER to 0.05
        )
        
        val adjustedShares = baseCategoryShares.toMutableMap()
        
        // Apply age-based adjustments
        when {
            profile.age < 25 -> {
                adjustedShares[ExpenseCategory.DINING] = adjustedShares[ExpenseCategory.DINING]!! * 1.15
                adjustedShares[ExpenseCategory.ENTERTAINMENT] = adjustedShares[ExpenseCategory.ENTERTAINMENT]!! * 1.12
                adjustedShares[ExpenseCategory.TRAVEL] = adjustedShares[ExpenseCategory.TRAVEL]!! * 1.1
                adjustedShares[ExpenseCategory.HEALTH] = adjustedShares[ExpenseCategory.HEALTH]!! * 0.85
                adjustedShares[ExpenseCategory.UTILITIES] = adjustedShares[ExpenseCategory.UTILITIES]!! * 0.9
            }
            profile.age in 25..40 -> {
                adjustedShares[ExpenseCategory.SHOPPING] = adjustedShares[ExpenseCategory.SHOPPING]!! * 1.05
            }
            profile.age in 41..55 -> {
                adjustedShares[ExpenseCategory.HEALTH] = adjustedShares[ExpenseCategory.HEALTH]!! * 1.12
                adjustedShares[ExpenseCategory.TRAVEL] = adjustedShares[ExpenseCategory.TRAVEL]!! * 1.05
                adjustedShares[ExpenseCategory.ENTERTAINMENT] = adjustedShares[ExpenseCategory.ENTERTAINMENT]!! * 0.9
            }
            profile.age > 55 -> {
                adjustedShares[ExpenseCategory.HEALTH] = adjustedShares[ExpenseCategory.HEALTH]!! * 1.2
                adjustedShares[ExpenseCategory.UTILITIES] = adjustedShares[ExpenseCategory.UTILITIES]!! * 1.05
                adjustedShares[ExpenseCategory.SHOPPING] = adjustedShares[ExpenseCategory.SHOPPING]!! * 0.85
                adjustedShares[ExpenseCategory.DINING] = adjustedShares[ExpenseCategory.DINING]!! * 0.9
            }
        }
        
        // Apply employment status adjustments
        when (profile.employmentStatus) {
            EmploymentStatus.STUDENT -> {
                adjustedShares[ExpenseCategory.EDUCATION] = adjustedShares[ExpenseCategory.EDUCATION]!! * 1.4
                adjustedShares[ExpenseCategory.DINING] = adjustedShares[ExpenseCategory.DINING]!! * 0.9
                adjustedShares[ExpenseCategory.TRAVEL] = adjustedShares[ExpenseCategory.TRAVEL]!! * 0.8
            }
            EmploymentStatus.SELF_EMPLOYED -> {
                adjustedShares[ExpenseCategory.UTILITIES] = adjustedShares[ExpenseCategory.UTILITIES]!! * 1.1
                adjustedShares[ExpenseCategory.TRANSPORTATION] = adjustedShares[ExpenseCategory.TRANSPORTATION]!! * 1.1
                adjustedShares[ExpenseCategory.TRAVEL] = adjustedShares[ExpenseCategory.TRAVEL]!! * 0.85
            }
            EmploymentStatus.PART_TIME -> {
                adjustedShares[ExpenseCategory.GROCERIES] = adjustedShares[ExpenseCategory.GROCERIES]!! * 1.05
                adjustedShares[ExpenseCategory.ENTERTAINMENT] = adjustedShares[ExpenseCategory.ENTERTAINMENT]!! * 0.9
                adjustedShares[ExpenseCategory.TRANSPORTATION] = adjustedShares[ExpenseCategory.TRANSPORTATION]!! * 1.05
            }
            EmploymentStatus.UNEMPLOYED -> {
                adjustedShares[ExpenseCategory.GROCERIES] = adjustedShares[ExpenseCategory.GROCERIES]!! * 1.1
                adjustedShares[ExpenseCategory.UTILITIES] = adjustedShares[ExpenseCategory.UTILITIES]!! * 1.1
                adjustedShares[ExpenseCategory.SHOPPING] = adjustedShares[ExpenseCategory.SHOPPING]!! * 0.8
                adjustedShares[ExpenseCategory.TRAVEL] = adjustedShares[ExpenseCategory.TRAVEL]!! * 0.75
            }
            EmploymentStatus.RETIRED -> {
                adjustedShares[ExpenseCategory.HEALTH] = adjustedShares[ExpenseCategory.HEALTH]!! * 1.2
                adjustedShares[ExpenseCategory.TRAVEL] = adjustedShares[ExpenseCategory.TRAVEL]!! * 1.1
                adjustedShares[ExpenseCategory.TRANSPORTATION] = adjustedShares[ExpenseCategory.TRANSPORTATION]!! * 0.85
            }
            else -> Unit
        }
        
        // Apply education status adjustments
        when (profile.educationStatus) {
            EducationStatus.HIGH_SCHOOL -> {
                adjustedShares[ExpenseCategory.EDUCATION] = adjustedShares[ExpenseCategory.EDUCATION]!! * 1.15
            }
            EducationStatus.UNIVERSITY -> {
                adjustedShares[ExpenseCategory.EDUCATION] = adjustedShares[ExpenseCategory.EDUCATION]!! * 1.35
            }
            else -> Unit
        }
        
        // Normalize shares to sum to 1.0
        val total = adjustedShares.values.sum()
        return adjustedShares.mapValues { it.value / total }
    }
    
    /**
     * Generate recommended starter vaults based on user profile.
     */
    fun generateStarterVaults(profile: UserProfileSetup): List<SmartVault> {
        val vaults = mutableListOf<SmartVault>()
        val monthlyIncome = profile.monthlyIncome
        
        // Emergency Fund - always recommend
        val emergencyFundTarget = when {
            profile.age < 25 -> monthlyIncome * 3  // 3 months
            profile.age < 35 -> monthlyIncome * 4  // 4 months
            profile.age < 50 -> monthlyIncome * 6  // 6 months
            else -> monthlyIncome * 8               // 8 months
        }
        
        vaults.add(
            SmartVault(
                id = 0,
                name = "Emergency Fund",
                targetAmount = emergencyFundTarget,
                currentBalance = 0.0,
                targetDate = LocalDate.now().plusMonths(12),
                priority = VaultPriority.HIGH,
                type = VaultType.SHORT_TERM,
                allocationMode = VaultAllocationMode.DYNAMIC_AUTO,
                manualAllocationPercent = null,
                nextExpectedContribution = null,
                lastContributionDate = null,
                autoDepositSchedule = null,
                savingTaxRateOverride = null,
                archived = false
            )
        )
        
        // Vacation/Fun - if user is young or has stable income
        if (profile.age < 40 || profile.employmentStatus == EmploymentStatus.FULL_TIME) {
            vaults.add(
                SmartVault(
                    id = 0,
                    name = "Vacation Fund",
                    targetAmount = monthlyIncome * 2,
                    currentBalance = 0.0,
                    targetDate = LocalDate.now().plusMonths(6),
                    priority = VaultPriority.MEDIUM,
                    type = VaultType.SHORT_TERM,
                    allocationMode = VaultAllocationMode.DYNAMIC_AUTO,
                    manualAllocationPercent = null,
                    nextExpectedContribution = null,
                    lastContributionDate = null,
                    autoDepositSchedule = null,
                    savingTaxRateOverride = null,
                    archived = false
                )
            )
        }
        
        // Education fund - if student
        if (profile.educationStatus == EducationStatus.UNIVERSITY || profile.employmentStatus == EmploymentStatus.STUDENT) {
            vaults.add(
                SmartVault(
                    id = 0,
                    name = "Education Expenses",
                    targetAmount = monthlyIncome * 3,
                    currentBalance = 0.0,
                    targetDate = LocalDate.now().plusMonths(9),
                    priority = VaultPriority.HIGH,
                    type = VaultType.SHORT_TERM,
                    allocationMode = VaultAllocationMode.DYNAMIC_AUTO,
                    manualAllocationPercent = null,
                    nextExpectedContribution = null,
                    lastContributionDate = null,
                    autoDepositSchedule = null,
                    savingTaxRateOverride = null,
                    archived = false
                )
            )
        }
        
        // Long-term investment vault - for older users or high earners
        if (profile.age >= 25 && monthlyIncome > 3000) {
            vaults.add(
                SmartVault(
                    id = 0,
                    name = "Investment Fund",
                    targetAmount = monthlyIncome * 12,
                    currentBalance = 0.0,
                    targetDate = LocalDate.now().plusYears(2),
                    priority = VaultPriority.MEDIUM,
                    type = VaultType.LONG_TERM,
                    allocationMode = VaultAllocationMode.DYNAMIC_AUTO,
                    manualAllocationPercent = null,
                    nextExpectedContribution = null,
                    lastContributionDate = null,
                    autoDepositSchedule = null,
                    savingTaxRateOverride = null,
                    archived = false
                )
            )
        }
        
        // Debt payoff vault - if user has debts
        if (profile.hasDebts) {
            vaults.add(
                SmartVault(
                    id = 0,
                    name = "Debt Payoff",
                    targetAmount = monthlyIncome * 6,
                    currentBalance = 0.0,
                    targetDate = LocalDate.now().plusMonths(12),
                    priority = VaultPriority.HIGH,
                    type = VaultType.SHORT_TERM,
                    allocationMode = VaultAllocationMode.DYNAMIC_AUTO,
                    manualAllocationPercent = null,
                    nextExpectedContribution = null,
                    lastContributionDate = null,
                    autoDepositSchedule = null,
                    savingTaxRateOverride = null,
                    archived = false
                )
            )
        }
        
        return vaults
    }
    
    /**
     * Get a personalized welcome message based on user profile.
     */
    fun getWelcomeMessage(profile: UserProfileSetup): String {
        val name = profile.name?.takeIf { it.isNotBlank() } ?: "there"
        
        return when {
            profile.educationStatus == EducationStatus.UNIVERSITY || profile.employmentStatus == EmploymentStatus.STUDENT -> 
                "Welcome, $name! We've created budgets and vaults to help you manage student life. Focus on your Emergency Fund first!"
            
            profile.age < 25 -> 
                "Welcome, $name! Starting early is great! We've set up budgets and savings goals to build good habits."
            
            profile.hasDebts -> 
                "Welcome, $name! We've prioritized your Debt Payoff vault. Tackling debt early will free up more money for your goals."
            
            profile.monthlyIncome > 5000 -> 
                "Welcome, $name! With your income level, we've created diversified vaults including investment opportunities."
            
            else -> 
                "Welcome, $name! We've created starter budgets and vaults based on your profile. You can customize them anytime!"
        }
    }
    
    /**
     * Get onboarding tips based on user profile.
     */
    fun getOnboardingTips(profile: UserProfileSetup): List<String> {
        val tips = mutableListOf<String>()
        
        // Everyone gets basic tips
        tips.add("Start by logging your expenses to see where your money goes")
        tips.add("The app automatically saves a portion of each expense to your vaults")
        
        // Age-specific tips
        when {
            profile.age < 25 -> {
                tips.add("Build your emergency fund to cover at least 3 months of expenses")
                tips.add("Take advantage of compound growth by starting to save early")
            }
            profile.age < 40 -> {
                tips.add("Aim for 6 months of expenses in your emergency fund")
                tips.add("Balance short-term fun with long-term security")
            }
            else -> {
                tips.add("Consider increasing your emergency fund to 8-12 months")
                tips.add("Focus on long-term investments for retirement")
            }
        }
        
        // Employment-specific tips
        if (profile.employmentStatus == EmploymentStatus.PART_TIME || 
            profile.employmentStatus == EmploymentStatus.SELF_EMPLOYED) {
            tips.add("Irregular income? Build a larger emergency fund for stability")
        }
        
        // Student-specific tips
        if (profile.educationStatus == EducationStatus.UNIVERSITY || profile.employmentStatus == EmploymentStatus.STUDENT) {
            tips.add("Track textbook and supply costs to better budget for next semester")
        }
        
        // Debt-specific tips
        if (profile.hasDebts) {
            tips.add("Pay off high-interest debt first to save on interest charges")
        }
        
        return tips
    }
}
