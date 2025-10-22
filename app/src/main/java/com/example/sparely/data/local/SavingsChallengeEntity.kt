package com.example.sparely.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.sparely.domain.model.ChallengeType
import java.time.LocalDate

@Entity(tableName = "savings_challenges")
data class SavingsChallengeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: ChallengeType,
    val title: String,
    val description: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isActive: Boolean = true,
    val isCompleted: Boolean = false,
    val completedDate: LocalDate? = null,
    val streakDays: Int = 0
)

@Entity(
    tableName = "challenge_milestones",
    foreignKeys = [
        ForeignKey(
            entity = SavingsChallengeEntity::class,
            parentColumns = ["id"],
            childColumns = ["challengeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["challengeId"])]
)
data class ChallengeMilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val challengeId: Long,
    val description: String,
    val targetAmount: Double,
    val isAchieved: Boolean = false,
    val achievedDate: LocalDate? = null,
    val rewardPoints: Int = 10
)
