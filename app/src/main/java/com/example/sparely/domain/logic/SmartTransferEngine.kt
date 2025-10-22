package com.example.sparely.domain.logic

import com.example.sparely.domain.model.SmartTransferDefaults
import com.example.sparely.domain.model.SmartTransferRecommendation
import com.example.sparely.domain.model.SmartTransferSnapshot
import com.example.sparely.domain.model.SmartTransferStatus

/**
 * Translates raw pending transfer state into a user-facing recommendation.
 */
object SmartTransferEngine {

    fun evaluate(snapshot: SmartTransferSnapshot, nowEpochMillis: Long = System.currentTimeMillis()): SmartTransferRecommendation? {
        val hasActive = snapshot.hasPending || snapshot.isAwaitingConfirmation
        if (!hasActive) return null

        if (snapshot.isAwaitingConfirmation) {
            return SmartTransferRecommendation(
                status = SmartTransferStatus.AWAITING_CONFIRMATION,
                totalAmount = snapshot.awaitingConfirmationAmount,
                emergencyPortion = snapshot.awaitingEmergencyAmount,
                investmentPortion = snapshot.awaitingInvestmentAmount,
                pendingExpenseCount = snapshot.awaitingConfirmationExpenseCount,
                holdUntilEpochMillis = snapshot.holdUntilEpochMillis,
                minimumTransferAmount = SmartTransferDefaults.MIN_TRANSFER_AMOUNT,
                lastExpenseEpochMillis = snapshot.lastExpenseEpochMillis,
                awaitingConfirmationAmount = snapshot.awaitingConfirmationAmount,
                awaitingEmergencyAmount = snapshot.awaitingEmergencyAmount,
                awaitingInvestmentAmount = snapshot.awaitingInvestmentAmount,
                confirmationStartedEpochMillis = snapshot.confirmationStartedEpochMillis,
                awaitingConfirmationExpenseCount = snapshot.awaitingConfirmationExpenseCount
            )
        }

        val total = snapshot.totalAmount
        val status = when {
            total < SmartTransferDefaults.MIN_TRANSFER_AMOUNT -> SmartTransferStatus.STANDBY
            snapshot.holdUntilEpochMillis != null && snapshot.holdUntilEpochMillis > nowEpochMillis -> SmartTransferStatus.ACCUMULATING
            else -> SmartTransferStatus.READY
        }

        return SmartTransferRecommendation(
            status = status,
            totalAmount = total,
            emergencyPortion = snapshot.pendingEmergencyAmount,
            investmentPortion = snapshot.pendingInvestmentAmount,
            pendingExpenseCount = snapshot.pendingExpenseCount,
            holdUntilEpochMillis = snapshot.holdUntilEpochMillis,
            minimumTransferAmount = SmartTransferDefaults.MIN_TRANSFER_AMOUNT,
            lastExpenseEpochMillis = snapshot.lastExpenseEpochMillis,
            awaitingConfirmationAmount = snapshot.awaitingConfirmationAmount,
            awaitingEmergencyAmount = snapshot.awaitingEmergencyAmount,
            awaitingInvestmentAmount = snapshot.awaitingInvestmentAmount,
            confirmationStartedEpochMillis = snapshot.confirmationStartedEpochMillis,
            awaitingConfirmationExpenseCount = snapshot.awaitingConfirmationExpenseCount
        )
    }
}
