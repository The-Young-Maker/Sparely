package com.example.sparely.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
        entities = [
        ExpenseEntity::class,
        SavingsTransferEntity::class,
        CategoryBudgetEntity::class,
        RecurringExpenseEntity::class,
        SavingsChallengeEntity::class,
        ChallengeMilestoneEntity::class,
        AchievementEntity::class,
        SavingsAccountEntity::class,
        SmartVaultEntity::class,
        VaultAutoDepositEntity::class,
        VaultContributionEntity::class,
        VaultBalanceAdjustmentEntity::class,
            FrozenFundEntity::class,
        AllocationHistoryEntity::class,
        MainAccountTransactionEntity::class
    ],
    version = 13,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SparelyDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun transferDao(): SavingsTransferDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun achievementDao(): AchievementDao
    abstract fun savingsAccountDao(): SavingsAccountDao
    abstract fun smartVaultDao(): SmartVaultDao
    abstract fun allocationHistoryDao(): AllocationHistoryDao
    abstract fun mainAccountDao(): MainAccountDao
    abstract fun frozenFundDao(): FrozenFundDao

    companion object {
        @Volatile
        private var INSTANCE: SparelyDatabase? = null

        fun getInstance(context: Context): SparelyDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): SparelyDatabase {
            return Room.databaseBuilder(
                context,
                SparelyDatabase::class.java,
                "sparely.db"
            )
                .addMigrations(MIGRATION_11_12, MIGRATION_12_13)
                .build()
        }

        // Migration: add new columns introduced in v12 (SmartVault additions and related fields)
        val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Helper to detect existing columns (PRAGMA table_info)
                fun hasColumn(columnName: String): Boolean {
                    val cursor = database.query("PRAGMA table_info(smart_vaults)")
                    cursor.use { c ->
                        val nameIndex = c.getColumnIndex("name")
                        while (c.moveToNext()) {
                            val existing = c.getString(nameIndex)
                            if (existing == columnName) return true
                        }
                    }
                    return false
                }

                fun addColumnIfMissing(sql: String, columnName: String) {
                    if (!hasColumn(columnName)) {
                        database.execSQL(sql)
                    }
                }

                // smart_vaults: add new nullable columns (LocalDate stored as INTEGER epochDay via converters)
                addColumnIfMissing("ALTER TABLE smart_vaults ADD COLUMN startDate INTEGER", "startDate")
                addColumnIfMissing("ALTER TABLE smart_vaults ADD COLUMN endDate INTEGER", "endDate")
                addColumnIfMissing("ALTER TABLE smart_vaults ADD COLUMN monthlyNeed REAL", "monthlyNeed")
                addColumnIfMissing("ALTER TABLE smart_vaults ADD COLUMN priorityWeight REAL NOT NULL DEFAULT 1.0", "priorityWeight")
                addColumnIfMissing("ALTER TABLE smart_vaults ADD COLUMN autoSaveEnabled INTEGER NOT NULL DEFAULT 1", "autoSaveEnabled")
                addColumnIfMissing("ALTER TABLE smart_vaults ADD COLUMN priority TEXT", "priority")
                addColumnIfMissing("ALTER TABLE smart_vaults ADD COLUMN type TEXT", "type")
                addColumnIfMissing("ALTER TABLE smart_vaults ADD COLUMN interestRate REAL", "interestRate")
                addColumnIfMissing("ALTER TABLE smart_vaults ADD COLUMN allocationMode TEXT", "allocationMode")
                addColumnIfMissing("ALTER TABLE smart_vaults ADD COLUMN manualAllocationPercent REAL", "manualAllocationPercent")
                addColumnIfMissing("ALTER TABLE smart_vaults ADD COLUMN nextExpectedContribution REAL", "nextExpectedContribution")
                addColumnIfMissing("ALTER TABLE smart_vaults ADD COLUMN lastContributionDate INTEGER", "lastContributionDate")
                addColumnIfMissing("ALTER TABLE smart_vaults ADD COLUMN savingTaxRateOverride REAL", "savingTaxRateOverride")
                addColumnIfMissing("ALTER TABLE smart_vaults ADD COLUMN archived INTEGER NOT NULL DEFAULT 0", "archived")
                addColumnIfMissing("ALTER TABLE smart_vaults ADD COLUMN accountType TEXT", "accountType")
                addColumnIfMissing("ALTER TABLE smart_vaults ADD COLUMN accountNumber TEXT", "accountNumber")
                addColumnIfMissing("ALTER TABLE smart_vaults ADD COLUMN accountNotes TEXT", "accountNotes")

                // vault_auto_deposits: add executeAutomatically flag
                fun hasAutoDepositColumn(columnName: String): Boolean {
                    val cursor = database.query("PRAGMA table_info(vault_auto_deposits)")
                    cursor.use { c ->
                        val nameIndex = c.getColumnIndex("name")
                        while (c.moveToNext()) {
                            val existing = c.getString(nameIndex)
                            if (existing == columnName) return true
                        }
                    }
                    return false
                }

                if (!hasAutoDepositColumn("executeAutomatically")) {
                    database.execSQL("ALTER TABLE vault_auto_deposits ADD COLUMN executeAutomatically INTEGER NOT NULL DEFAULT 0")
                }

                // recurring_expenses: add executeAutomatically flag
                fun hasRecurringColumn(columnName: String): Boolean {
                    val cursor = database.query("PRAGMA table_info(recurring_expenses)")
                    cursor.use { c ->
                        val nameIndex = c.getColumnIndex("name")
                        while (c.moveToNext()) {
                            val existing = c.getString(nameIndex)
                            if (existing == columnName) return true
                        }
                    }
                    return false
                }

                if (!hasRecurringColumn("executeAutomatically")) {
                    database.execSQL("ALTER TABLE recurring_expenses ADD COLUMN executeAutomatically INTEGER NOT NULL DEFAULT 0")
                }

                // Create allocation_history table to persist allocation suggestions
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS allocation_history (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "vaultId INTEGER NOT NULL, " +
                            "amount REAL NOT NULL, " +
                            "date INTEGER NOT NULL, " +
                            "source TEXT, " +
                            "note TEXT, " +
                            "FOREIGN KEY(vaultId) REFERENCES smart_vaults(id) ON DELETE CASCADE)"
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_allocation_history_vaultId ON allocation_history(vaultId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_allocation_history_date ON allocation_history(date)")

                // Create frozen_funds table (used to track amounts reserved/pending without altering main account canonical balance)
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS frozen_funds (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "pendingType TEXT NOT NULL, " +
                            "pendingId INTEGER NOT NULL, " +
                            "amount REAL NOT NULL, " +
                            "createdAt INTEGER NOT NULL, " +
                            "description TEXT)"
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_frozen_funds_pending ON frozen_funds(pendingType, pendingId)")
            }
        }

        // Migration: 12 -> 13
        // Adds excludedFromAutoAllocation to smart_vaults and adds scheduling columns to vault_auto_deposits
        val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // helper to check columns
                fun hasColumn(table: String, columnName: String): Boolean {
                    val cursor = database.query("PRAGMA table_info($table)")
                    cursor.use { c ->
                        val nameIndex = c.getColumnIndex("name")
                        while (c.moveToNext()) {
                            val existing = c.getString(nameIndex)
                            if (existing == columnName) return true
                        }
                    }
                    return false
                }

                // smart_vaults: excludedFromAutoAllocation default 0
                if (!hasColumn("smart_vaults", "excludedFromAutoAllocation")) {
                    database.execSQL("ALTER TABLE smart_vaults ADD COLUMN excludedFromAutoAllocation INTEGER NOT NULL DEFAULT 0")
                }

                // vault_auto_deposits: scheduling columns (nullable)
                if (!hasColumn("vault_auto_deposits", "dayOfMonth")) {
                    database.execSQL("ALTER TABLE vault_auto_deposits ADD COLUMN dayOfMonth INTEGER")
                }
                if (!hasColumn("vault_auto_deposits", "dayOfWeek")) {
                    database.execSQL("ALTER TABLE vault_auto_deposits ADD COLUMN dayOfWeek INTEGER")
                }
                if (!hasColumn("vault_auto_deposits", "customIntervalDays")) {
                    database.execSQL("ALTER TABLE vault_auto_deposits ADD COLUMN customIntervalDays INTEGER")
                }
                if (!hasColumn("vault_auto_deposits", "nextRunAt")) {
                    database.execSQL("ALTER TABLE vault_auto_deposits ADD COLUMN nextRunAt INTEGER")
                }
            }
        }
    }
}
