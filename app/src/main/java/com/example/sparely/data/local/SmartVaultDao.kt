package com.example.sparely.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface SmartVaultDao {
    @Transaction
    @Query("SELECT * FROM smart_vaults WHERE archived = 0 ORDER BY CASE priority WHEN 'CRITICAL' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 ELSE 4 END, targetDate IS NULL, targetDate ASC")
    fun observeActiveVaults(): Flow<List<SmartVaultWithSchedule>>

    @Transaction
    @Query("SELECT * FROM smart_vaults ORDER BY archived ASC, name ASC")
    fun observeAllVaults(): Flow<List<SmartVaultWithSchedule>>

    @Query("SELECT * FROM smart_vaults WHERE id = :id")
    suspend fun getVaultById(id: Long): SmartVaultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVault(entity: SmartVaultEntity): Long

    @Query("DELETE FROM smart_vaults")
    suspend fun clearAllVaults()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAutoDeposit(entity: VaultAutoDepositEntity): Long

    @Update
    suspend fun updateAutoDeposit(entity: VaultAutoDepositEntity)

    @Query("DELETE FROM vault_auto_deposits WHERE id = :id")
    suspend fun deleteAutoDeposit(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContribution(entity: VaultContributionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdjustment(entity: VaultBalanceAdjustmentEntity): Long

    @Query("SELECT * FROM vault_contributions WHERE vaultId = :vaultId ORDER BY date DESC")
    suspend fun getContributionsForVault(vaultId: Long): List<VaultContributionEntity>

    @Query("SELECT * FROM vault_balance_adjustments WHERE vaultId = :vaultId ORDER BY createdAt DESC")
    suspend fun getAdjustmentsForVault(vaultId: Long): List<VaultBalanceAdjustmentEntity>

    @Query("SELECT * FROM vault_contributions WHERE reconciled = 0 AND source = 'AUTO_DEPOSIT'")
    suspend fun getPendingAutoDeposits(): List<VaultContributionEntity>
    
    @Query("SELECT * FROM vault_contributions WHERE reconciled = 0 ORDER BY date DESC")
    suspend fun getPendingContributions(): List<VaultContributionEntity>
    
    @Query("SELECT * FROM vault_contributions WHERE id = :id")
    suspend fun getContributionById(id: Long): VaultContributionEntity?

    @Query("UPDATE smart_vaults SET currentBalance = currentBalance + :delta, lastContributionDate = :date WHERE id = :vaultId")
    suspend fun incrementVaultBalance(vaultId: Long, delta: Double, date: LocalDate?)

    @Query("UPDATE smart_vaults SET currentBalance = :balance, lastContributionDate = :date WHERE id = :vaultId")
    suspend fun setVaultBalance(vaultId: Long, balance: Double, date: LocalDate?)

    @Query(
        "UPDATE smart_vaults SET archived = :archived WHERE id = :vaultId"
    )
    suspend fun updateVaultArchived(vaultId: Long, archived: Boolean)

    @Query("DELETE FROM smart_vaults WHERE id = :vaultId")
    suspend fun deleteVault(vaultId: Long)

    @Query("UPDATE vault_contributions SET reconciled = 1 WHERE id = :contributionId")
    suspend fun markContributionReconciled(contributionId: Long)

    @Transaction
    suspend fun attachAutoDeposit(vaultId: Long, schedule: VaultAutoDepositEntity) {
        val existing = getAutoDepositForVault(vaultId)
        if (existing != null) {
            updateAutoDeposit(schedule.copy(id = existing.id, vaultId = vaultId))
        } else {
            upsertAutoDeposit(schedule.copy(vaultId = vaultId))
        }
    }

    @Query("SELECT * FROM vault_auto_deposits WHERE vaultId = :vaultId LIMIT 1")
    suspend fun getAutoDepositForVault(vaultId: Long): VaultAutoDepositEntity?
    
    @Query("SELECT * FROM vault_auto_deposits WHERE active = 1")
    suspend fun getActiveAutoDepositSchedules(): List<VaultAutoDepositEntity>

    @Transaction
    suspend fun removeAutoDepositForVault(vaultId: Long) {
        getAutoDepositForVault(vaultId)?.let { deleteAutoDeposit(it.id) }
    }
}
