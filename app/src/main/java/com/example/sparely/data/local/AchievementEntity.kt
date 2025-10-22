package com.example.sparely.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.sparely.domain.model.AchievementCategory
import java.time.LocalDate

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val icon: String,
    val earnedDate: LocalDate,
    val category: AchievementCategory
)
