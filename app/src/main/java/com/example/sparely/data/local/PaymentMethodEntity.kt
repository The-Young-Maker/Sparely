package com.example.sparely.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "payment_methods")
data class PaymentMethodEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // CASH, CARD, OTHER
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
)

