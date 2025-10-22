package com.example.sparely.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.sparely.domain.model.SavingsCategory
import java.time.LocalDate

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetAmount: Double,
    val category: SavingsCategory,
    val targetDate: LocalDate?,
    val createdAt: LocalDate,
    val notes: String?,
    val archived: Boolean
)
