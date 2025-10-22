package com.example.sparely.domain.model

/**
 * Enumerations used across the Sparely domain layer.
 */
enum class RiskLevel {
    CONSERVATIVE,
    BALANCED,
    AGGRESSIVE
}

enum class SavingsCategory {
    EMERGENCY,
    INVESTMENT,
    FUN
}

enum class BankSyncProvider {
    PLAID_SANDBOX,
    MOCK
}

enum class SmartAllocationMode {
    MANUAL,
    GUIDED,
    AUTOMATIC
}

enum class ExpenseCategory {
    GROCERIES,
    DINING,
    TRANSPORTATION,
    ENTERTAINMENT,
    UTILITIES,
    HEALTH,
    EDUCATION,
    SHOPPING,
    TRAVEL,
    OTHER
}

enum class AlertType {
    INFO,
    WARNING,
    SUCCESS
}

enum class VaultPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class VaultType {
    SHORT_TERM,
    LONG_TERM,
    PASSIVE_INVESTMENT
}

enum class VaultAllocationMode {
    DYNAMIC_AUTO,
    MANUAL
}

enum class AutoDepositFrequency {
    WEEKLY,
    BIWEEKLY,
    MONTHLY
}

enum class IncomeTrackingMode {
    MANUAL_PER_PAYCHECK,
    SCHEDULED,
    HYBRID
}

enum class PayInterval {
    WEEKLY,
    BIWEEKLY,
    SEMI_MONTHLY,
    MONTHLY,
    CUSTOM
}
