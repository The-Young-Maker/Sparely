package com.example.sparely.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCardPaymentDao {
    @Query("SELECT * FROM credit_card_payments WHERE paymentMethodId = :paymentMethodId ORDER BY date DESC")
    fun getPaymentsForCard(paymentMethodId: Long): Flow<List<CreditCardPaymentEntity>>

    @Query("SELECT * FROM credit_card_payments ORDER BY date DESC")
    fun getAllPayments(): Flow<List<CreditCardPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: CreditCardPaymentEntity): Long

    @Delete
    suspend fun deletePayment(payment: CreditCardPaymentEntity)

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM credit_card_payments WHERE paymentMethodId = :paymentMethodId")
    suspend fun getTotalPaymentsForCard(paymentMethodId: Long): Double

    @Query("DELETE FROM credit_card_payments WHERE paymentMethodId = :paymentMethodId")
    suspend fun deleteAllForCard(paymentMethodId: Long)

    @Query("DELETE FROM credit_card_payments")
    suspend fun deleteAll()
}
