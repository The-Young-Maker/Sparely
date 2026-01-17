package com.example.sparely.domain.model

import java.time.LocalDate

/**
 * Domain model for a credit card payment.
 */
data class CreditCardPayment(
    val id: Long = 0,
    val paymentMethodId: Long,
    val amount: Double,
    val date: LocalDate,
    val note: String? = null
)
