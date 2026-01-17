package com.example.sparely.domain.model

import java.time.LocalDate

data class PaymentMethod(
    val id: Long = 0,
    val name: String,
    val type: PaymentMethodType,
    val defaultDeductFromMainAccount: Boolean,
    val isDefault: Boolean = false,
    val iconName: String? = null,
    // Credit card specific fields
    val isCreditCard: Boolean = false,
    val creditLimit: Double? = null,
    val currentBalance: Double = 0.0,
    val billingCycleDay: Int? = null,
    val lastPaymentDate: LocalDate? = null,
    val lastPaymentAmount: Double? = null
) {
    /** Credit utilization as a percentage (0.0 to 1.0+) */
    val utilizationPercent: Double
        get() = if (isCreditCard && creditLimit != null && creditLimit > 0)
            (currentBalance / creditLimit) else 0.0

    /** Utilization is healthy (≤30%) - good for credit score */
    val isUtilizationHealthy: Boolean get() = utilizationPercent <= 0.30

    /** Utilization is concerning (30-50%) - may affect credit score */
    val isUtilizationWarning: Boolean get() = utilizationPercent > 0.30 && utilizationPercent <= 0.50

    /** Utilization is high (>50%) - likely hurting credit score */
    val isUtilizationDanger: Boolean get() = utilizationPercent > 0.50

    /** Available credit remaining */
    val availableCredit: Double
        get() = if (isCreditCard && creditLimit != null) 
            (creditLimit - currentBalance).coerceAtLeast(0.0) else 0.0
}

enum class PaymentMethodType {
    CASH, CARD, OTHER
}

