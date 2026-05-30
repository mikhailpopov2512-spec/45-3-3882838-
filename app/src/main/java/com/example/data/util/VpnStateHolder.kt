package com.example.data.util

import com.example.data.local.VpnProfileEntity
import kotlinx.coroutines.flow.MutableStateFlow

object VpnStateHolder {
    val isConnected = MutableStateFlow(false)
    val isConnecting = MutableStateFlow(false)
    val activeProfile = MutableStateFlow<VpnProfileEntity?>(null)
    val bytesReceived = MutableStateFlow(0L)
    val bytesTransmitted = MutableStateFlow(0L)
    val currentDurationSec = MutableStateFlow(0)
    val currentIp = MutableStateFlow("0.0.0.0")
    val currentCountry = MutableStateFlow("Определение...")
    val downloadSpeedKbps = MutableStateFlow(0f)
    val uploadSpeedKbps = MutableStateFlow(0f)

    val vpnLogs = MutableStateFlow<List<String>>(listOf("[System] Ядро Ray-core готово к запуску"))

    fun log(msg: String) {
        val current = vpnLogs.value.toMutableList()
        current.add("[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}] $msg")
        if (current.size > 60) {
            current.removeAt(0)
        }
        vpnLogs.value = current
    }

    fun clearLogs() {
        vpnLogs.value = listOf("[System] Очистка консоли ядра")
    }
}
