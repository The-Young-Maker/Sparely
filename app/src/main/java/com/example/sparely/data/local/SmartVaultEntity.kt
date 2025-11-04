package com.example.sparely.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.sparely.domain.model.AccountType
import com.example.sparely.domain.model.AutoDepositFrequency
import com.example.sparely.domain.model.VaultAllocationMode
import com.example.sparely.domain.model.VaultContributionSource
import com.example.sparely.domain.model.VaultAdjustmentType
import com.example.sparely.domain.model.VaultPriority
import com.example.sparely.domain.model.VaultType
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "smart_vaults")
data class SmartVaultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val targetAmount: Double,
    val currentBalance: Double,
    val targetDate: LocalDate?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val monthlyNeed: Double?,
    val priorityWeight: Double,
    val autoSaveEnabled: Boolean,
    // If true this vault will be excluded from any automatic allocation/saving-tax/rounding
    // mechanisms and will only receive manual or explicitly scheduled transfers.
    val excludedFromAutoAllocation: Boolean = false,
    val priority: VaultPriority,
    val type: VaultType,
    val interestRate: Double?,
    val allocationMode: VaultAllocationMode,
    val manualAllocationPercent: Double?,
    val nextExpectedContribution: Double?,
    val lastContributionDate: LocalDate?,
    val savingTaxRateOverride: Double?,
    val archived: Boolean,
    val accountType: AccountType?,
    val accountNumber: String?,
    val accountNotes: String?,
    val createdAt: LocalDate = LocalDate.now()
)

{
    /**
     * Secondary constructor to support older generated code that used a smaller parameter list.
     * Delegates to the primary constructor, providing sensible defaults for newly added fields.
     */
    constructor(
        id: Long,
        name: String,
        targetAmount: Double,
        currentBalance: Double,
        targetDate: LocalDate?,
        priority: com.example.sparely.domain.model.VaultPriority,
        type: com.example.sparely.domain.model.VaultType,
        interestRate: Double?,
        allocationMode: com.example.sparely.domain.model.VaultAllocationMode,
        manualAllocationPercent: Double?,
        nextExpectedContribution: Double?,
        lastContributionDate: LocalDate?,
        savingTaxRateOverride: Double?,
        archived: Boolean,
        accountType: com.example.sparely.domain.model.AccountType?,
        accountNumber: String?,
        accountNotes: String?,
        createdAt: LocalDate?
    ) : this(
        id = id,
        name = name,
        targetAmount = targetAmount,
        currentBalance = currentBalance,
        targetDate = targetDate,
        startDate = null,
        endDate = null,
        monthlyNeed = null,
        priorityWeight = 1.0,
        autoSaveEnabled = true,
        excludedFromAutoAllocation = false,
        priority = priority,
        type = type,
        interestRate = interestRate,
        allocationMode = allocationMode,
        manualAllocationPercent = manualAllocationPercent,
        nextExpectedContribution = nextExpectedContribution,
        lastContributionDate = lastContributionDate,
        savingTaxRateOverride = savingTaxRateOverride,
        archived = archived,
        accountType = accountType,
        accountNumber = accountNumber,
        accountNotes = accountNotes,
        createdAt = createdAt ?: LocalDate.now()
    )
}

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
    val active: Boolean,
    // If true, the worker will attempt to move funds from the main account into the vault
    // automatically when the schedule is due. If false, a pending contribution will be created
    // and the user will need to reconcile it manually.
    val executeAutomatically: Boolean = false
    // Optional scheduling details for more advanced recurring logic
    // dayOfMonth: 1..31 (if null not used for monthly schedules)
    ,val dayOfMonth: Int? = null
    // dayOfWeek: 1..7 (ISO-8601 where Monday=1) for weekly schedules
    ,val dayOfWeek: Int? = null
    // custom interval in days when using custom frequency
    ,val customIntervalDays: Int? = null
    // next predicted run (stored as LocalDate) to help the scheduler; nullable
    ,val nextRunAt: LocalDate? = null
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

@Entity(
    tableName = "vault_balance_adjustments",
    foreignKeys = [
        ForeignKey(
            entity = SmartVaultEntity::class,
            parentColumns = ["id"],
            childColumns = ["vaultId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vaultId"), Index("createdAt")]
)
data class VaultBalanceAdjustmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val vaultId: Long,
    val type: VaultAdjustmentType,
    val delta: Double,
    val resultingBalance: Double,
    val createdAt: Instant,
    val reason: String?
)

data class SmartVaultWithSchedule(
    @Embedded val vault: SmartVaultEntity,
    @Relation(parentColumn = "id", entityColumn = "vaultId") val schedules: List<VaultAutoDepositEntity>
)
