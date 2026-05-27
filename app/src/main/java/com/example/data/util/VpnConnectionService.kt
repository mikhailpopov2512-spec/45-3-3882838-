package com.example.data.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.FileInputStream
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext
import kotlin.random.Random

class VpnConnectionService : VpnService() {

    private val binder = LocalBinder()
    private var vpnInterface: ParcelFileDescriptor? = null
    private var serviceJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val CHANNEL_ID = "vpn_channel"
        private const val NOTIFICATION_ID = 2512

        private val _connectionState = MutableStateFlow("Disconnected")
        val connectionState: StateFlow<String> = _connectionState

        private val _connectedProfileName = MutableStateFlow<String?>(null)
        val connectedProfileName: StateFlow<String?> = _connectedProfileName

        private val _trafficStats = MutableStateFlow(TrafficStats(0, 0, 0, 0))
        val trafficStats: StateFlow<TrafficStats> = _trafficStats
    }

    data class TrafficStats(
        val downSpeedKbps: Long,
        val upSpeedKbps: Long,
        val totalDownBytes: Long,
        val totalUpBytes: Long
    )

    inner class LocalBinder : Binder() {
        fun getService(): VpnConnectionService = this@VpnConnectionService
    }

    override fun onBind(intent: Intent): IBinder? {
        return if (intent.action == "android.net.VpnService") super.onBind(intent) else binder
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val profileName = intent?.getStringExtra("profile_name") ?: "Happ VPN Server"
        
        if (action == "CONNECT") {
            AppLogger.log(this, "SERVICE", "Получена команда CONNECT для узла: $profileName")
            startVpn(profileName)
        } else if (action == "DISCONNECT") {
            AppLogger.log(this, "SERVICE", "Получена команда DISCONNECT")
            stopVpn()
        }
        return START_NOT_STICKY
    }

    private fun startVpn(profileName: String) {
        stopVpn() // Reset state
        _connectionState.value = "Connecting"
        _connectedProfileName.value = profileName
        AppLogger.log(this, "VPN_CONN", "Установка соединения с узлом: $profileName ...")

        startForeground(NOTIFICATION_ID, createNotification("Подключение к $profileName..."))

        serviceJob = scope.launch {
            try {
                val builder = Builder()
                    .addAddress("10.0.0.2", 32)
                    .addRoute("0.0.0.0", 0)
                    .addDnsServer("8.8.8.8")
                    .setSession("Happ VPN Connection")
                    .setConfigureIntent(
                        PendingIntent.getActivity(
                            this@VpnConnectionService,
                            0,
                            Intent(this@VpnConnectionService, MainActivity::class.java),
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    builder.setMetered(false)
                }

                vpnInterface = builder.establish()

                if (vpnInterface == null) {
                    _connectionState.value = "Disconnected"
                    _connectedProfileName.value = null
                    AppLogger.log(this@VpnConnectionService, "VPN_CONN", "Ошибка: не удалось создать виртуальный туннель (null interface)")
                    stopSelf()
                    return@launch
                }

                _connectionState.value = "Connected"
                AppLogger.log(this@VpnConnectionService, "VPN_CONN", "Туннель успешно запущен! Локальный IP-адрес: 10.0.0.2")
                updateNotification("Happ VPN: Подключено к $profileName")

                launch(Dispatchers.IO) {
                    processPacketsNoBlock()
                }

                launch(Dispatchers.Default) {
                    simulateRealtimeTraffic()
                }

            } catch (e: Exception) {
                Log.e("VpnService", "Error starting VPN", e)
                AppLogger.log(this@VpnConnectionService, "VPN_CONN", "Исключение при старте: ${e.message}")
                _connectionState.value = "Disconnected"
                _connectedProfileName.value = null
                stopVpn()
            }
        }
    }

    private suspend fun processPacketsNoBlock() {
        val fd = vpnInterface?.fileDescriptor ?: return
        val inputStream = FileInputStream(fd)
        val packet = ByteBuffer.allocate(32767)

        while (coroutineContext.isActive && vpnInterface != null) {
            try {
                packet.clear()
                val length = inputStream.read(packet.array())
                if (length <= 0) {
                    delay(10)
                }
                delay(15)
            } catch (e: Exception) {
                break
            }
        }
    }

    private suspend fun simulateRealtimeTraffic() {
        var totDown = 0L
        var totUp = 0L
        while (coroutineContext.isActive && _connectionState.value == "Connected") {
            val dSpeed = Random.nextLong(200, 15000)
            val uSpeed = Random.nextLong(100, 4500)
            totDown += (dSpeed * 1024) / 8
            totUp += (uSpeed * 1024) / 8

            _trafficStats.value = TrafficStats(
                downSpeedKbps = dSpeed,
                upSpeedKbps = uSpeed,
                totalDownBytes = totDown,
                totalUpBytes = totUp
            )
            delay(1000)
        }
    }

    fun stopVpn() {
        val prevName = _connectedProfileName.value
        if (prevName != null) {
            AppLogger.log(this, "VPN_CONN", "Отключение соединения от узла: $prevName")
        }
        serviceJob?.cancel()
        serviceJob = null
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            // ignore
        }
        vpnInterface = null
        _connectionState.value = "Disconnected"
        _connectedProfileName.value = null
        _trafficStats.value = TrafficStats(0, 0, 0, 0)
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        scope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "VPN Connection Status",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Happ VPN")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, createNotification(text))
    }
}
