package com.example.sparely.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Represents an amount reserved (frozen) on the main account pending a user action
 * (for example: pending vault contribution or pending recurring payment).
 */
@Entity(tableName = "frozen_funds")
data class FrozenFundEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val pendingType: String,
    val pendingId: Long,
    val amount: Double,
    val createdAt: LocalDateTime,
    val description: String? = null
)
