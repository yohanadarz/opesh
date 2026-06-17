package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val costAmount: Double,
    val costFrequency: String, // "DAILY", "WEEKLY", "MONTHLY"
    val quitTimestamp: Long = System.currentTimeMillis()
)
