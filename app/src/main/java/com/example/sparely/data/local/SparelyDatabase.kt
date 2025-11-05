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
                .addMigrations(
                    MIGRATION_1_12,
                    MIGRATION_2_12,
                    MIGRATION_3_12,
                    MIGRATION_4_12,
                    MIGRATION_5_12,
                    MIGRATION_6_12,
                    MIGRATION_7_12,
                    MIGRATION_8_12,
                    MIGRATION_9_12,
                    MIGRATION_10_12,
                    MIGRATION_11_12,
                    MIGRATION_12_13
                )
                .build()
        }

        // Migrations from earlier versions to v12
        // Since we don\'t have schema exports for earlier versions and the previous approach
        // used .fallbackToDestructiveMigration(), we\'ll provide migrations that attempt
        // to preserve existing data while adding new columns/tables as needed.
        // All migrations apply the same schema updates to reach v12.

        private val migrateToV12: (androidx.sqlite.db.SupportSQLiteDatabase) -> Unit = { database ->
            // Helper to detect existing columns (PRAGMA table_info)
            fun hasColumn(tableName: String, columnName: String): Boolean {
                val cursor = database.query("PRAGMA table_info($tableName)")
                cursor.use { c ->
                    val nameIndex = c.getColumnIndex("name")
                    while (c.moveToNext()) {
                        val existing = c.getString(nameIndex)
                        if (existing == columnName) return true
                    }
                }
                return false
            }

            fun hasTable(tableName: String): Boolean {
                val cursor = database.query("SELECT name FROM sqlite_master WHERE type='table' AND name='$tableName'")
                val exists = cursor.count > 0
                cursor.close()
                return exists
            }

            fun addColumnIfMissing(tableName: String, sql: String, columnName: String) {
                if (!hasColumn(tableName, columnName)) {
                    database.execSQL(sql)
                }
            }

            // Ensure smart_vaults table exists (create if needed for very old versions)
            if (!hasTable("smart_vaults")) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS smart_vaults (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "targetAmount REAL NOT NULL, " +
                        "currentBalance REAL NOT NULL, " +
                        "targetDate INTEGER, " +
                        "startDate INTEGER, " +
                        "endDate INTEGER, " +
                        "monthlyNeed REAL, " +
                        "priorityWeight REAL NOT NULL DEFAULT 1.0, " +
                        "autoSaveEnabled INTEGER NOT NULL DEFAULT 1, " +
                        "priority TEXT, " +
                        "type TEXT, " +
                        "interestRate REAL, " +
                        "allocationMode TEXT, " +
                        "manualAllocationPercent REAL, " +
                        "nextExpectedContribution REAL, " +
                        "lastContributionDate INTEGER, " +
                        "savingTaxRateOverride REAL, " +
                        "archived INTEGER NOT NULL DEFAULT 0, " +
                        "accountType TEXT, " +
                        "accountNumber TEXT, " +
                        "accountNotes TEXT, " +
                        // createdAt defaults to 0 (epoch: Jan 1, 1970) for migrated vaults
                        // This is acceptable as it only affects display/sorting, not functionality
                        "createdAt INTEGER NOT NULL DEFAULT 0)"
                )
            } else {
                // Add missing columns to existing table
                addColumnIfMissing("smart_vaults", "ALTER TABLE smart_vaults ADD COLUMN startDate INTEGER", "startDate")
                addColumnIfMissing("smart_vaults", "ALTER TABLE smart_vaults ADD COLUMN endDate INTEGER", "endDate")
                addColumnIfMissing("smart_vaults", "ALTER TABLE smart_vaults ADD COLUMN monthlyNeed REAL", "monthlyNeed")
                addColumnIfMissing("smart_vaults", "ALTER TABLE smart_vaults ADD COLUMN priorityWeight REAL NOT NULL DEFAULT 1.0", "priorityWeight")
                addColumnIfMissing("smart_vaults", "ALTER TABLE smart_vaults ADD COLUMN autoSaveEnabled INTEGER NOT NULL DEFAULT 1", "autoSaveEnabled")
                addColumnIfMissing("smart_vaults", "ALTER TABLE smart_vaults ADD COLUMN priority TEXT", "priority")
                addColumnIfMissing("smart_vaults", "ALTER TABLE smart_vaults ADD COLUMN type TEXT", "type")
                addColumnIfMissing("smart_vaults", "ALTER TABLE smart_vaults ADD COLUMN interestRate REAL", "interestRate")
                addColumnIfMissing("smart_vaults", "ALTER TABLE smart_vaults ADD COLUMN allocationMode TEXT", "allocationMode")
                addColumnIfMissing("smart_vaults", "ALTER TABLE smart_vaults ADD COLUMN manualAllocationPercent REAL", "manualAllocationPercent")
                addColumnIfMissing("smart_vaults", "ALTER TABLE smart_vaults ADD COLUMN nextExpectedContribution REAL", "nextExpectedContribution")
                addColumnIfMissing("smart_vaults", "ALTER TABLE smart_vaults ADD COLUMN lastContributionDate INTEGER", "lastContributionDate")
                addColumnIfMissing("smart_vaults", "ALTER TABLE smart_vaults ADD COLUMN savingTaxRateOverride REAL", "savingTaxRateOverride")
                addColumnIfMissing("smart_vaults", "ALTER TABLE smart_vaults ADD COLUMN archived INTEGER NOT NULL DEFAULT 0", "archived")
                addColumnIfMissing("smart_vaults", "ALTER TABLE smart_vaults ADD COLUMN accountType TEXT", "accountType")
                addColumnIfMissing("smart_vaults", "ALTER TABLE smart_vaults ADD COLUMN accountNumber TEXT", "accountNumber")
                addColumnIfMissing("smart_vaults", "ALTER TABLE smart_vaults ADD COLUMN accountNotes TEXT", "accountNotes")
                addColumnIfMissing("smart_vaults", "ALTER TABLE smart_vaults ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0", "createdAt")
            }

            // Ensure vault_auto_deposits table exists
            if (!hasTable("vault_auto_deposits")) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS vault_auto_deposits (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "vaultId INTEGER NOT NULL, " +
                        "amount REAL NOT NULL, " +
                        "frequency TEXT NOT NULL, " +
                        "startDate INTEGER NOT NULL, " +
                        "endDate INTEGER, " +
                        "sourceAccountId INTEGER, " +
                        "lastExecutionDate INTEGER, " +
                        "active INTEGER NOT NULL, " +
                        "executeAutomatically INTEGER NOT NULL DEFAULT 0, " +
                        "FOREIGN KEY(vaultId) REFERENCES smart_vaults(id) ON DELETE CASCADE)"
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_vault_auto_deposits_vaultId ON vault_auto_deposits(vaultId)")
            } else {
                addColumnIfMissing("vault_auto_deposits", "ALTER TABLE vault_auto_deposits ADD COLUMN executeAutomatically INTEGER NOT NULL DEFAULT 0", "executeAutomatically")
            }

            // Ensure vault_contributions table exists
            if (!hasTable("vault_contributions")) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS vault_contributions (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "vaultId INTEGER NOT NULL, " +
                        "amount REAL NOT NULL, " +
                        "date INTEGER NOT NULL, " +
                        "source TEXT NOT NULL, " +
                        "note TEXT, " +
                        "reconciled INTEGER NOT NULL DEFAULT 0, " +
                        "FOREIGN KEY(vaultId) REFERENCES smart_vaults(id) ON DELETE CASCADE)"
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_vault_contributions_vaultId ON vault_contributions(vaultId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_vault_contributions_date ON vault_contributions(date)")
            }

            // Ensure vault_balance_adjustments table exists
            if (!hasTable("vault_balance_adjustments")) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS vault_balance_adjustments (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "vaultId INTEGER NOT NULL, " +
                        "type TEXT NOT NULL, " +
                        "delta REAL NOT NULL, " +
                        "resultingBalance REAL NOT NULL, " +
                        "createdAt INTEGER NOT NULL, " +
                        "reason TEXT, " +
                        "FOREIGN KEY(vaultId) REFERENCES smart_vaults(id) ON DELETE CASCADE)"
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_vault_balance_adjustments_vaultId ON vault_balance_adjustments(vaultId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_vault_balance_adjustments_createdAt ON vault_balance_adjustments(createdAt)")
            }

            // Add executeAutomatically to recurring_expenses if the table exists
            if (hasTable("recurring_expenses")) {
                addColumnIfMissing("recurring_expenses", "ALTER TABLE recurring_expenses ADD COLUMN executeAutomatically INTEGER NOT NULL DEFAULT 0", "executeAutomatically")
            }

            // Create allocation_history table
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

            // Create frozen_funds table
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

        val MIGRATION_1_12 = object : androidx.room.migration.Migration(1, 12) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) = migrateToV12(database)
        }

        val MIGRATION_2_12 = object : androidx.room.migration.Migration(2, 12) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) = migrateToV12(database)
        }

        val MIGRATION_3_12 = object : androidx.room.migration.Migration(3, 12) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) = migrateToV12(database)
        }

        val MIGRATION_4_12 = object : androidx.room.migration.Migration(4, 12) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) = migrateToV12(database)
        }

        val MIGRATION_5_12 = object : androidx.room.migration.Migration(5, 12) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) = migrateToV12(database)
        }

        val MIGRATION_6_12 = object : androidx.room.migration.Migration(6, 12) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) = migrateToV12(database)
        }

        val MIGRATION_7_12 = object : androidx.room.migration.Migration(7, 12) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) = migrateToV12(database)
        }

        val MIGRATION_8_12 = object : androidx.room.migration.Migration(8, 12) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) = migrateToV12(database)
        }

        val MIGRATION_9_12 = object : androidx.room.migration.Migration(9, 12) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) = migrateToV12(database)
        }

        val MIGRATION_10_12 = object : androidx.room.migration.Migration(10, 12) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) = migrateToV12(database)
        }

        // Migration: add new columns introduced in v12 (SmartVault additions and related fields)
        val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) = migrateToV12(database)
        }

        val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE smart_vaults ADD COLUMN excludedFromAutoAllocation INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE vault_auto_deposits ADD COLUMN dayOfMonth INTEGER")
                database.execSQL("ALTER TABLE vault_auto_deposits ADD COLUMN dayOfWeek INTEGER")
                database.execSQL("ALTER TABLE vault_auto_deposits ADD COLUMN customIntervalDays INTEGER")
                database.execSQL("ALTER TABLE vault_auto_deposits ADD COLUMN nextRunAt INTEGER")
            }
        }
    }
}
