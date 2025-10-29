package com.example.sparely.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "allocation_history",
    foreignKeys = [ForeignKey(
        entity = SmartVaultEntity::class,
        parentColumns = ["id"],
        childColumns = ["vaultId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("vaultId"), Index("date")]
)
data class AllocationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val vaultId: Long,
    val amount: Double,
    val date: LocalDate,
    val source: String? = null,
    val note: String? = null
)
