package com.example.data.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.MainActivity
import kotlinx.coroutines.*

class MyVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val ACTION_CONNECT = "com.example.happvpn.CONNECT"
        const val ACTION_DISCONNECT = "com.example.happvpn.DISCONNECT"
        private const val CHANNEL_ID = "vpn_notifications"
        private const val NOTIFICATION_ID = 8828
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_CONNECT) {
            val serverIp = intent.getStringExtra("server") ?: "127.0.0.1"
            val serverPort = intent.getIntExtra("port", 443)
            val profileName = intent.getStringExtra("name") ?: "Happ VPN"
            
            connectVpn(serverIp, serverPort, profileName)
        } else if (action == ACTION_DISCONNECT) {
            disconnectVpn()
        }
        return START_NOT_STICKY
    }

    private fun connectVpn(serverIp: String, serverPort: Int, profileName: String) {
        disconnectVpn()
        
        VpnStateHolder.isConnecting.value = true
        VpnStateHolder.isConnected.value = false
        
        createNotificationChannel()
        val notification = createNotification("Подключение к $profileName...")
        startForeground(NOTIFICATION_ID, notification)

        vpnJob = serviceScope.launch {
            try {
                val builder = Builder()
                    .addAddress("10.0.0.2", 24)
                    .addRoute("0.0.0.0", 0)
                    .addDnsServer("8.8.8.8")
                    .setSession(profileName)

                vpnInterface = builder.establish()

                if (vpnInterface != null) {
                    VpnStateHolder.isConnecting.value = false
                    VpnStateHolder.isConnected.value = true
                    
                    updateNotification("Happ VPN: Подключено к $profileName")
                    
                    runTunnelSimulation()
                } else {
                    Log.e("MyVpnService", "Failed to establish VPN interface.")
                    disconnectVpn()
                }
            } catch (e: Exception) {
                Log.e("MyVpnService", "Exception in network worker", e)
                disconnectVpn()
            }
        }
    }

    private fun runTunnelSimulation() = serviceScope.launch {
        var rx = 0L
        var tx = 0L
        var duration = 0
        VpnStateHolder.bytesReceived.value = 0
        VpnStateHolder.bytesTransmitted.value = 0
        VpnStateHolder.currentDurationSec.value = 0

        while (isActive && vpnInterface != null) {
            delay(1000)
            duration++
            rx += (150..2200).random().toLong()
            tx += (80..1800).random().toLong()

            VpnStateHolder.bytesReceived.value = rx
            VpnStateHolder.bytesTransmitted.value = tx
            VpnStateHolder.currentDurationSec.value = duration
        }
    }

    private fun disconnectVpn() {
        vpnJob?.cancel()
        vpnJob = null
        
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        vpnInterface = null

        VpnStateHolder.isConnecting.value = false
        VpnStateHolder.isConnected.value = false
        VpnStateHolder.bytesReceived.value = 0
        VpnStateHolder.bytesTransmitted.value = 0
        VpnStateHolder.currentDurationSec.value = 0
        
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        disconnectVpn()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Состояние Happ VPN"
            val descriptionText = "Отображает статус подключения Happ VPN"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("Happ VPN")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(text))
    }
}
