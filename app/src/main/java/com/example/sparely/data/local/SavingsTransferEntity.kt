package com.example.sparely.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.sparely.domain.model.SavingsCategory
import com.example.sparely.domain.model.SavingsTransfer
import java.time.LocalDate

@Entity(
    tableName = "savings_transfers",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = SavingsAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceAccountId"],
            onDelete = androidx.room.ForeignKey.SET_NULL
        ),
        androidx.room.ForeignKey(
            entity = SavingsAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["destinationAccountId"],
            onDelete = androidx.room.ForeignKey.SET_NULL
        )
    ],
    indices = [
        androidx.room.Index("sourceAccountId"),
        androidx.room.Index("destinationAccountId")
    ]
)
data class SavingsTransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: SavingsCategory,
    val amount: Double,
    val date: LocalDate = LocalDate.now(),
    val sourceAccountId: Long? = null,
    val destinationAccountId: Long? = null,
    val note: String? = null
)

fun SavingsTransferEntity.toDomain(): SavingsTransfer =
    SavingsTransfer(
        id = id,
        category = category,
        amount = amount,
        date = date,
        sourceAccountId = sourceAccountId,
        destinationAccountId = destinationAccountId,
        note = note
    )

fun SavingsTransfer.toEntity(): SavingsTransferEntity =
    SavingsTransferEntity(
        id = id,
        category = category,
        amount = amount,
        date = date,
        sourceAccountId = sourceAccountId,
        destinationAccountId = destinationAccountId,
        note = note
    )
