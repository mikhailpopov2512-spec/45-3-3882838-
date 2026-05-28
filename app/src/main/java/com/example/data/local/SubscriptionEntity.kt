package com.example.data.local

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionEntity(
    val id: Int = 0,
    val name: String,
    val url: String,
    val addedAt: Long = System.currentTimeMillis()
)
