package com.example.sparely.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.sparely.domain.model.AutoDepositFrequency
import com.example.sparely.domain.model.VaultAllocationMode
import com.example.sparely.domain.model.VaultContributionSource
import com.example.sparely.domain.model.VaultPriority
import com.example.sparely.domain.model.VaultType
import java.time.LocalDate

@Entity(tableName = "smart_vaults")
data class SmartVaultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val targetAmount: Double,
    val currentBalance: Double,
    val targetDate: LocalDate?,
    val priority: VaultPriority,
    val type: VaultType,
    val interestRate: Double?,
    val allocationMode: VaultAllocationMode,
    val manualAllocationPercent: Double?,
    val nextExpectedContribution: Double?,
    val lastContributionDate: LocalDate?,
    val savingTaxRateOverride: Double?,
    val archived: Boolean
)

@Entity(
    tableName = "vault_auto_deposits",
    foreignKeys = [
        ForeignKey(
            entity = SmartVaultEntity::class,
            parentColumns = ["id"],
            childColumns = ["vaultId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vaultId")]
)
data class VaultAutoDepositEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val vaultId: Long,
    val amount: Double,
    val frequency: AutoDepositFrequency,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val sourceAccountId: Long?,
    val lastExecutionDate: LocalDate?,
    val active: Boolean
)

@Entity(
    tableName = "vault_contributions",
    foreignKeys = [
        ForeignKey(
            entity = SmartVaultEntity::class,
            parentColumns = ["id"],
            childColumns = ["vaultId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vaultId"), Index("date")]
)
data class VaultContributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val vaultId: Long,
    val amount: Double,
    val date: LocalDate,
    val source: VaultContributionSource,
    val note: String?,
    val reconciled: Boolean
)

data class SmartVaultWithSchedule(
    @Embedded val vault: SmartVaultEntity,
    @Relation(parentColumn = "id", entityColumn = "vaultId") val schedules: List<VaultAutoDepositEntity>
)
