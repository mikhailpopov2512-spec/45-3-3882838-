package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vpn_profiles")
data class VpnProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val protocol: String, // VLESS, VMess, Shadowsocks, Trojan, etc.
    val host: String,
    val port: Int,
    val uuid: String, // Or password for Trojan/Shadowsocks
    val security: String = "tls", // none, tls
    val sni: String = "",
    val path: String = "",
    val flow: String = "",
    val remarks: String = "",
    val ping: Int = -1, // in milliseconds, -1 means untested / unreachable
    val isActive: Boolean = false,
    val isSubProfile: Boolean = false,
    val subUrl: String? = null
)
