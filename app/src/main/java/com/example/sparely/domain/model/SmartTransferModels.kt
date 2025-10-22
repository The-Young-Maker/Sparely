package com.example.sparely.domain.model

/**
 * Stores the raw persisted state for the smart transfer automation.
 * Values are tracked in cents to keep the arithmetic stable.
 */
data class SmartTransferSnapshot(
    val pendingEmergencyCents: Long = 0L,
    val pendingInvestmentCents: Long = 0L,
    val pendingExpenseCount: Int = 0,
    val lastExpenseEpochMillis: Long? = null,
    val holdUntilEpochMillis: Long? = null,
    val awaitingConfirmationEmergencyCents: Long = 0L,
    val awaitingConfirmationInvestmentCents: Long = 0L,
    val awaitingConfirmationExpenseCount: Int = 0,
    val confirmationStartedEpochMillis: Long? = null
) {
    val totalCents: Long = pendingEmergencyCents + pendingInvestmentCents
    val totalAmount: Double = totalCents.toCurrency()
    val pendingEmergencyAmount: Double = pendingEmergencyCents.toCurrency()
    val pendingInvestmentAmount: Double = pendingInvestmentCents.toCurrency()
    val hasPending: Boolean = totalCents > 0L
    val awaitingConfirmationCents: Long = awaitingConfirmationEmergencyCents + awaitingConfirmationInvestmentCents
    val awaitingConfirmationAmount: Double = awaitingConfirmationCents.toCurrency()
    val awaitingEmergencyAmount: Double = awaitingConfirmationEmergencyCents.toCurrency()
    val awaitingInvestmentAmount: Double = awaitingConfirmationInvestmentCents.toCurrency()
    val isAwaitingConfirmation: Boolean = awaitingConfirmationCents > 0L
    val activeExpenseCount: Int =
        if (isAwaitingConfirmation) awaitingConfirmationExpenseCount else pendingExpenseCount
}

/** Describes the state of the smart transfer recommendation shown to the user. */
data class SmartTransferRecommendation(
    val status: SmartTransferStatus,
    val totalAmount: Double,
    val emergencyPortion: Double,
    val investmentPortion: Double,
    val pendingExpenseCount: Int,
    val holdUntilEpochMillis: Long?,
    val minimumTransferAmount: Double,
    val lastExpenseEpochMillis: Long?,
    val awaitingConfirmationAmount: Double,
    val awaitingEmergencyAmount: Double,
    val awaitingInvestmentAmount: Double,
    val confirmationStartedEpochMillis: Long?,
    val awaitingConfirmationExpenseCount: Int
) {
    val shortfallToThreshold: Double = (minimumTransferAmount - totalAmount).coerceAtLeast(0.0)
}

/** High level state machine for the automated transfer helper. */
enum class SmartTransferStatus {
    /** Collecting smaller amounts until the threshold is reached. */
    STANDBY,

    /** Waiting a short time for the user to finish logging consecutive expenses. */
    ACCUMULATING,

    /** Ready to nudge the user to move the money. */
    READY,

    /** User has started the transfer and still needs to finish it manually. */
    AWAITING_CONFIRMATION
}

/** Tunable defaults for the automation. */
object SmartTransferDefaults {
    const val MIN_TRANSFER_AMOUNT: Double = 10.0
    const val BATCH_WINDOW_MINUTES: Long = 3L
    const val BATCH_WINDOW_MILLIS: Long = BATCH_WINDOW_MINUTES * 60_000L
}

private fun Long.toCurrency(): Double = this / 100.0
