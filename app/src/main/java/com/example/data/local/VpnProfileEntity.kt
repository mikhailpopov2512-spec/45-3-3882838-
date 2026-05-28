package com.example.data.local

import kotlinx.serialization.Serializable

@Serializable
data class VpnProfileEntity(
    val id: Int = 0,
    val subscriptionId: Int? = null,
    val name: String,
    val server: String,
    val port: Int,
    val protocol: String, // "VLESS", "VMESS", "SHADOWSOCKS", "TROJAN"
    val configPayload: String,
    val pingMs: Int = -1,
    val countryCode: String = "UN",
    val isSelected: Boolean = false
)
