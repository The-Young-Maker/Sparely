package com.example.sparely.data.local

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class MainAccountTransactionDetails(
    @Embedded val transaction: MainAccountTransactionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = TransactionVaultContributionCrossRef::class,
            parentColumn = "transactionId",
            entityColumn = "contributionId"
        )
    )
    val vaultContributions: List<VaultContributionEntity>
)
