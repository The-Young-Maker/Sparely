package com.example.sparely.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Database entity representing a store or website where expenses can be made.
 */
@Entity(
    tableName = "stores",
    indices = [Index(value = ["name"])]
)
data class StoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String? = null,
    val createdAt: LocalDate = LocalDate.now(),
    val websiteUrl: String? = null
)
