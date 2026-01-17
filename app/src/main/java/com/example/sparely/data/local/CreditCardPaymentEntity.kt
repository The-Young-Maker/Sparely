package com.example.sparely.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Tracks payments made to credit cards.
 */
@Entity(
    tableName = "credit_card_payments",
    foreignKeys = [
        ForeignKey(
            entity = PaymentMethodEntity::class,
            parentColumns = ["id"],
            childColumns = ["paymentMethodId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("paymentMethodId")]
)
data class CreditCardPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val paymentMethodId: Long,
    val amount: Double,
    val date: LocalDate,
    val note: String? = null
)
