package com.example.data.local

data class SubscriptionEntity(
    val url: String,
    val name: String,
    val lastUpdated: Long = System.currentTimeMillis(),
    val nodeCount: Int = 0
)
