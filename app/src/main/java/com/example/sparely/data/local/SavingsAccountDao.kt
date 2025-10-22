package com.example.sparely.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.sparely.domain.model.BankSyncProvider
import com.example.sparely.domain.model.SavingsCategory
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsAccountDao {
    @Query("SELECT * FROM savings_accounts ORDER BY isPrimary DESC, name ASC")
    fun observeAccounts(): Flow<List<SavingsAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: SavingsAccountEntity): Long

    @Query("DELETE FROM savings_accounts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM savings_accounts")
    suspend fun clearAll()

    @Query("UPDATE savings_accounts SET currentBalance = :balance WHERE id = :id")
    suspend fun updateBalance(id: Long, balance: Double)

    @Query("UPDATE savings_accounts SET currentBalance = currentBalance + :delta WHERE id = :id")
    suspend fun incrementBalance(id: Long, delta: Double)

    @Query("UPDATE savings_accounts SET lastSyncedAt = :lastSyncedAt, currentBalance = :balance WHERE id = :id")
    suspend fun updateSyncedBalance(id: Long, balance: Double, lastSyncedAt: Instant?)

    @Query("UPDATE savings_accounts SET syncProvider = :provider, externalAccountId = :externalAccountId, autoRefreshEnabled = :autoRefreshEnabled WHERE id = :id")
    suspend fun updateLinkMetadata(id: Long, provider: BankSyncProvider?, externalAccountId: String?, autoRefreshEnabled: Boolean)

    @Query("SELECT * FROM savings_accounts WHERE category = :category ORDER BY isPrimary DESC, id ASC")
    suspend fun findByCategory(category: SavingsCategory): List<SavingsAccountEntity>

    @Query("SELECT IFNULL(SUM(currentBalance), 0.0) FROM savings_accounts")
    suspend fun getTotalBalance(): Double

    @Query("SELECT * FROM savings_accounts WHERE syncProvider IS NOT NULL AND autoRefreshEnabled = 1")
    suspend fun getLinkedAccounts(): List<SavingsAccountEntity>

    @Transaction
    suspend fun setPrimaryForCategory(category: SavingsCategory, id: Long) {
        val accounts = findByCategory(category)
        accounts.forEach { account ->
            val makePrimary = account.id == id
            if (account.isPrimary != makePrimary) {
                upsert(account.copy(isPrimary = makePrimary))
            }
        }
    }
}
