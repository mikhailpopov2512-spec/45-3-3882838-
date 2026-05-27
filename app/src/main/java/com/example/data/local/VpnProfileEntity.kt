package com.example.data.local

data class VpnProfileEntity(
    val id: Int = 0,
    val name: String,
    val protocol: String, // VLESS, VMESS, SS, TROJAN, HYSTERIA2, TUIC
    val host: String,
    val port: Int,
    val fullConfig: String,
    val ping: Int = -1, // -1: untested, -2: fail
    val dateAdded: Long = System.currentTimeMillis(),
    val subscriptionUrl: String? = null
)
