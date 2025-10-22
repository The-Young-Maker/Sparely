package com.example.sparely.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.sparely.domain.model.BankSyncProvider
import com.example.sparely.domain.model.SavingsCategory
import java.time.Instant

@Entity(tableName = "savings_accounts")
data class SavingsAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val category: SavingsCategory,
    val institution: String?,
    val accountNumber: String?,
    val currentBalance: Double,
    val targetBalance: Double?,
    val isPrimary: Boolean,
    val reminderFrequencyDays: Int?,
    val reminderEnabled: Boolean,
    val syncProvider: BankSyncProvider? = null,
    val externalAccountId: String? = null,
    val lastSyncedAt: Instant? = null,
    val autoRefreshEnabled: Boolean = false
)
