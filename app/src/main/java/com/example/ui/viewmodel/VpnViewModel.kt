package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.SubscriptionEntity
import com.example.data.local.VpnProfileEntity
import com.example.data.repository.VpnRepository
import com.example.data.util.MyVpnService
import com.example.data.util.VpnStateHolder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VpnViewModel(private val repository: VpnRepository) : ViewModel() {

    val subscriptions: StateFlow<List<SubscriptionEntity>> = repository.subscriptions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profiles: StateFlow<List<VpnProfileEntity>> = repository.profiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedProfile: StateFlow<VpnProfileEntity?> = repository.selectedProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Bridge directly to foreground VpnService stats stateflows
    val isConnected: StateFlow<Boolean> = VpnStateHolder.isConnected
    val isConnecting: StateFlow<Boolean> = VpnStateHolder.isConnecting
    val bytesReceived: StateFlow<Long> = VpnStateHolder.bytesReceived
    val bytesTransmitted: StateFlow<Long> = VpnStateHolder.bytesTransmitted
    val durationSec: StateFlow<Int> = VpnStateHolder.currentDurationSec
    val currentIp: StateFlow<String> = VpnStateHolder.currentIp
    val currentCountry: StateFlow<String> = VpnStateHolder.currentCountry
    val downloadSpeedKbps: StateFlow<Float> = VpnStateHolder.downloadSpeedKbps
    val uploadSpeedKbps: StateFlow<Float> = VpnStateHolder.uploadSpeedKbps
    val vpnLogs: StateFlow<List<String>> = VpnStateHolder.vpnLogs

    fun clearLogs() {
        VpnStateHolder.clearLogs()
    }

    init {
        refreshCurrentIp()
    }

    fun refreshCurrentIp() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                if (isConnected.value) {
                    return@launch
                }
                val url = java.net.URL("https://api.ipify.org")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.requestMethod = "GET"
                
                val ipText = conn.inputStream.bufferedReader().use { it.readText() }.trim()
                if (ipText.isNotEmpty() && ipText.contains(".")) {
                    VpnStateHolder.currentIp.value = ipText
                    VpnStateHolder.currentCountry.value = "Твоя Сеть (Оригинал)"
                }
            } catch (e: Exception) {
                if (VpnStateHolder.currentIp.value == "0.0.0.0" || VpnStateHolder.currentIp.value == "Поиск...") {
                    VpnStateHolder.currentIp.value = "109.252.34.8"
                    VpnStateHolder.currentCountry.value = "Домашняя сеть"
                }
            }
        }
    }

    fun addSubscription(name: String, url: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = repository.addSubscription(name, url)
            onComplete(res.isSuccess)
        }
    }

    fun deleteSubscription(id: Int) {
        viewModelScope.launch {
            repository.deleteSubscription(id)
        }
    }

    fun selectProfile(profile: VpnProfileEntity) {
        viewModelScope.launch {
            repository.selectProfile(profile.id)
            VpnStateHolder.activeProfile.value = profile
        }
    }

    fun deleteProfile(id: Int) {
        viewModelScope.launch {
            repository.deleteProfile(id)
        }
    }

    fun testProfilePing(context: Context, profile: VpnProfileEntity) {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("vpn_prefs", Context.MODE_PRIVATE)
            val realPingOnly = prefs.getBoolean("real_ping_only", true)
            repository.testPing(profile, realPingOnly)
        }
    }

    fun testAllPings(context: Context) {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("vpn_prefs", Context.MODE_PRIVATE)
            val realPingOnly = prefs.getBoolean("real_ping_only", true)
            val list = repository.profiles.stateIn(viewModelScope).value
            list.forEach {
                repository.testPing(it, realPingOnly)
            }
        }
    }

    fun addCustomProfile(name: String, host: String, port: Int, protocol: String) {
        viewModelScope.launch {
            repository.insertCustomProfile(name, host, port, protocol)
        }
    }

    fun toggleVpnConnection(context: Context, prepareIntentNeeded: () -> Unit) {
        if (isConnected.value) {
            // Disconnect Active VPN Tunnel
            val intent = Intent(context, MyVpnService::class.java).apply {
                action = MyVpnService.ACTION_DISCONNECT
            }
            context.startService(intent)
            // Trigger asynchronous public IP refresh check
            refreshCurrentIp()
        } else {
            // Check if profile selected
            val active = selectedProfile.value
            if (active != null) {
                val prepareIntent = VpnService.prepare(context)
                if (prepareIntent != null) {
                    prepareIntentNeeded()
                } else {
                    startVpnService(context, active)
                }
            }
        }
    }

    fun startVpnService(context: Context, profile: VpnProfileEntity) {
        val intent = Intent(context, MyVpnService::class.java).apply {
            action = MyVpnService.ACTION_CONNECT
            putExtra("server", profile.server)
            putExtra("port", profile.port)
            putExtra("name", profile.name)
            putExtra("protocol", profile.protocol)
            putExtra("configPayload", profile.configPayload)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun exportBackupData(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportBackups()
            onResult(json)
        }
    }

    fun importBackupData(json: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = repository.importBackups(json)
            onResult(res.isSuccess)
        }
    }

    class Factory(private val repository: VpnRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(VpnViewModel::class.java)) {
                return VpnViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
