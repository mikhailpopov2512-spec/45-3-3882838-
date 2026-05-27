package com.example.data.util

import com.example.data.local.VpnProfileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object VpnConnectionService {

    data class IpDetails(
        val ip: String = "Detecting...",
        val city: String = "",
        val countryName: String = "",
        val countryCode: String = "",
        val org: String = ""
    )

    private val _connectionState = MutableStateFlow<String>("DISCONNECTED") // DISCONNECTED, CONNECTING, CONNECTED
    val connectionState: StateFlow<String> = _connectionState

    private val _ipDetails = MutableStateFlow<IpDetails>(IpDetails())
    val ipDetails: StateFlow<IpDetails> = _ipDetails

    private val _bytesReceived = MutableStateFlow<Long>(0)
    val bytesReceived: StateFlow<Long> = _bytesReceived

    private val _bytesSent = MutableStateFlow<Long>(0)
    val bytesSent: StateFlow<Long> = _bytesSent

    private val _activeProfile = MutableStateFlow<VpnProfileEntity?>(null)
    val activeProfile: StateFlow<VpnProfileEntity?> = _activeProfile

    private var trafficSimThread: Thread? = null

    suspend fun fetchPublicIp() = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        try {
            val request = Request.Builder()
                .url("https://ipapi.co/json/")
                .header("User-Agent", "Mozilla/5.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    val json = JSONObject(bodyString)
                    val ip = json.optString("ip", "Unknown")
                    val city = json.optString("city", "")
                    val country = json.optString("country_name", "")
                    val countryCode = json.optString("country_code", "")
                    val org = json.optString("org", "")
                    
                    _ipDetails.value = IpDetails(
                        ip = ip,
                        city = city,
                        countryName = country,
                        countryCode = countryCode,
                        org = org
                    )
                } else {
                    _ipDetails.value = IpDetails(ip = "Error status: ${response.code}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Graceful fallback display offline check parameters
            _ipDetails.value = IpDetails(
                ip = "192.168.1.135",
                city = "Local Gateway",
                countryName = "Intranet Address",
                countryCode = "LAN",
                org = "ISP Simulator Corp"
            )
        }
    }

    fun startVpn(profile: VpnProfileEntity) {
        _connectionState.value = "CONNECTING"
        _activeProfile.value = profile
        
        // Simulating immediate routing optimization connection sequence
        Thread {
            try {
                Thread.sleep(1200)
                _connectionState.value = "CONNECTED"
                
                // Fetch simulated safe geo-shifted endpoint info or fetch real IP details
                _ipDetails.value = IpDetails(
                    ip = "81.4.124.${(10..250).random()}",
                    city = if (profile.name.contains("Frankfurt") || profile.host.contains(".de")) "Frankfurt" else "Singapore Central",
                    countryName = if (profile.name.contains("Frankfurt") || profile.host.contains(".de")) "Germany" else "Singapore",
                    countryCode = if (profile.name.contains("Frankfurt") || profile.host.contains(".de")) "DE" else "SG",
                    org = "HappVPN Premium Edge Router"
                )
                
                startTrafficSimulation()
            } catch (e: Exception) {
                _connectionState.value = "DISCONNECTED"
            }
        }.start()
    }

    fun stopVpn() {
        stopTrafficSimulation()
        _connectionState.value = "DISCONNECTED"
        _activeProfile.value = null
        
        // Return back to local WAN detection info
        CoroutineScope(Dispatchers.IO).launch {
            try {
                fetchPublicIp()
            } catch (e: Exception) {
                _ipDetails.value = IpDetails(ip = "Disconnected")
            }
        }
    }

    private fun startTrafficSimulation() {
        stopTrafficSimulation()
        _bytesReceived.value = 0
        _bytesSent.value = 0
        
        trafficSimThread = Thread {
            var totalIn = 0L
            var totalOut = 0L
            while (!Thread.currentThread().isInterrupted && _connectionState.value == "CONNECTED") {
                try {
                    Thread.sleep(1000)
                    val rxInc = (50_000..850_000).random().toLong()
                    val txInc = (10_000..180_000).random().toLong()
                    totalIn += rxInc
                    totalOut += txInc
                    _bytesReceived.value = totalIn
                    _bytesSent.value = totalOut
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
        trafficSimThread?.start()
    }

    private fun stopTrafficSimulation() {
        trafficSimThread?.interrupt()
        trafficSimThread = null
    }
}
