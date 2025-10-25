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
        MainAccountTransactionEntity::class
    ],
    version = 11,
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
    abstract fun mainAccountDao(): MainAccountDao

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
            ).fallbackToDestructiveMigration()
                .build()
        }
    }
}
