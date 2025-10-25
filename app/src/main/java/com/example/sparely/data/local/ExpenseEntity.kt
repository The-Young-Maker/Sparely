package com.example.sparely.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.sparely.domain.model.ExpenseCategory
import com.example.sparely.domain.model.RiskLevel
import java.time.LocalDate

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val amount: Double,
    val category: ExpenseCategory,
    val date: LocalDate,
    val includesTax: Boolean,
    val emergencyAmount: Double,
    val investmentAmount: Double,
    val funAmount: Double,
    val safeInvestmentAmount: Double,
    val highRiskInvestmentAmount: Double,
    val autoRecommended: Boolean,
    val appliedPercentEmergency: Double,
    val appliedPercentInvest: Double,
    val appliedPercentFun: Double,
    val appliedSafeSplit: Double,
    val riskLevelUsed: RiskLevel,
    val deductedFromVaultId: Long? = null
)
