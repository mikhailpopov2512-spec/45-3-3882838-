package com.example.data.repository

import com.example.data.local.*
import com.example.data.util.VpnLinkParser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors

class VpnRepository(private val dbHelper: VpnDatabaseHelper) {

    val allProfiles: Flow<List<VpnProfileEntity>> = dbHelper.profilesFlow
    val allSubscriptions: Flow<List<SubscriptionEntity>> = dbHelper.subscriptionsFlow

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val pingDispatcher = Executors.newFixedThreadPool(12).asCoroutineDispatcher()

    fun insertProfile(profile: VpnProfileEntity) = dbHelper.insertProfile(profile)

    fun insertProfiles(profiles: List<VpnProfileEntity>) = dbHelper.insertProfiles(profiles)

    fun deleteProfileById(id: Int) = dbHelper.deleteProfileById(id)

    fun deleteUnreachableProfiles() = dbHelper.deleteUnreachableProfiles()

    fun deleteAllProfiles() = dbHelper.deleteAllProfiles()

    fun insertSubscription(subscription: SubscriptionEntity) = dbHelper.insertSubscription(subscription)

    fun deleteSubscriptionByUrl(url: String) {
        dbHelper.deleteProfilesBySubscription(url)
        dbHelper.deleteSubscriptionByUrl(url)
    }

    suspend fun importFromText(text: String, subscriptionUrl: String? = null): Int {
        val profiles = VpnLinkParser.parseSubscriptionContent(text, subscriptionUrl)
        if (profiles.isNotEmpty()) {
            dbHelper.insertProfiles(profiles)
        }
        return profiles.size
    }

    suspend fun updateSubscription(subscriptionUrl: String, customName: String? = null) {
        withContext(Dispatchers.IO) {
            val name = customName ?: subscriptionUrl.substringAfter("://").substringBefore("/").takeIf { it.isNotEmpty() } ?: "Subscription"
            try {
                val request = Request.Builder().url(subscriptionUrl).build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("Response code: ${response.code}")
                    val body = response.body?.string() ?: ""
                    val profiles = VpnLinkParser.parseSubscriptionContent(body, subscriptionUrl)

                    dbHelper.deleteProfilesBySubscription(subscriptionUrl)
                    if (profiles.isNotEmpty()) {
                        dbHelper.insertProfiles(profiles)
                    }

                    dbHelper.insertSubscription(
                        SubscriptionEntity(
                            url = subscriptionUrl,
                            name = name,
                            lastUpdated = System.currentTimeMillis(),
                            nodeCount = profiles.size
                        )
                    )
                }
            } catch (e: Exception) {
                dbHelper.insertSubscription(
                    SubscriptionEntity(
                        url = subscriptionUrl,
                        name = "$name",
                        lastUpdated = System.currentTimeMillis(),
                        nodeCount = 0
                    )
                )
                throw e
            }
        }
    }

    suspend fun testAllPings(profiles: List<VpnProfileEntity>, onProgress: (Int, Int) -> Unit) {
        val total = profiles.size
        var completed = 0

        withContext(pingDispatcher) {
            val deferreds = profiles.map { profile ->
                async {
                    val pingResult = testSinglePing(profile.host, profile.port)
                    dbHelper.updatePing(profile.id, pingResult)
                    synchronized(this@VpnRepository) {
                        completed++
                        onProgress(completed, total)
                    }
                }
            }
            deferreds.awaitAll()
        }
    }

    suspend fun testSinglePing(profileId: Int, host: String, port: Int): Int {
        val result = withContext(pingDispatcher) {
            testSinglePing(host, port)
        }
        dbHelper.updatePing(profileId, result)
        return result
    }

    private fun testSinglePing(host: String, port: Int): Int {
        var socket: Socket? = null
        val startTime = System.currentTimeMillis()
        return try {
            socket = Socket()
            socket.connect(InetSocketAddress(host, port), 1200)
            val elapsed = (System.currentTimeMillis() - startTime).toInt()
            if (elapsed == 0) 1 else elapsed
        } catch (e: Exception) {
            -2
        } finally {
            try {
                socket?.close()
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
