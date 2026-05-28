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
}
