package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val url: String,
    val lastUpdated: Long = System.currentTimeMillis()
)
