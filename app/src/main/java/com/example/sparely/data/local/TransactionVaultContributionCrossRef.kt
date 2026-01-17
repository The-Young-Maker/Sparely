package com.example.sparely.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "transaction_vault_contribution_cross_ref",
    primaryKeys = ["transactionId", "contributionId"],
    foreignKeys = [
        ForeignKey(
            entity = MainAccountTransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = VaultContributionEntity::class,
            parentColumns = ["id"],
            childColumns = ["contributionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["transactionId"]),
        Index(value = ["contributionId"])
    ]
)
data class TransactionVaultContributionCrossRef(
    val transactionId: Long,
    val contributionId: Long
)
