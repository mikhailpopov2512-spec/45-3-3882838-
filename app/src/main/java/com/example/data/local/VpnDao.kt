package com.example.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class VpnDao(private val dbHelper: AppDbHelper) {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _subscriptions = MutableStateFlow<List<SubscriptionEntity>>(emptyList())
    val subscriptionsFlow = _subscriptions

    private val _profiles = MutableStateFlow<List<VpnProfileEntity>>(emptyList())
    val profilesFlow = _profiles

    private val _selectedProfile = MutableStateFlow<VpnProfileEntity?>(null)
    val selectedProfileFlow = _selectedProfile

    init {
        refreshAll()
    }

    fun getAllSubscriptions(): Flow<List<SubscriptionEntity>> = subscriptionsFlow
    fun getAllProfilesFlow(): Flow<List<VpnProfileEntity>> = profilesFlow
    fun getSelectedProfileFlow(): Flow<VpnProfileEntity?> = selectedProfileFlow

    fun refreshAll() {
        scope.launch {
            try {
                _subscriptions.value = getAllSubscriptionsSync()
                val allProfs = getAllProfilesSync()
                _profiles.value = allProfs
                _selectedProfile.value = allProfs.firstOrNull { it.isSelected }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getAllSubscriptionsSync(): List<SubscriptionEntity> {
        val list = mutableListOf<SubscriptionEntity>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id, name, url, addedAt FROM subscriptions ORDER BY addedAt DESC", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    SubscriptionEntity(
                        id = c.getInt(0),
                        name = c.getString(1),
                        url = c.getString(2),
                        addedAt = c.getLong(3)
                    )
                )
            }
        }
        return list
    }

    private fun getAllProfilesSync(): List<VpnProfileEntity> {
        val list = mutableListOf<VpnProfileEntity>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id, subscriptionId, name, server, port, protocol, configPayload, pingMs, countryCode, isSelected FROM vpn_profiles ORDER BY id DESC", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                val subIdVal = if (c.isNull(1)) null else c.getInt(1)
                list.add(
                    VpnProfileEntity(
                        id = c.getInt(0),
                        subscriptionId = subIdVal,
                        name = c.getString(2),
                        server = c.getString(3),
                        port = c.getInt(4),
                        protocol = c.getString(5),
                        configPayload = c.getString(6),
                        pingMs = c.getInt(7),
                        countryCode = c.getString(8),
                        isSelected = c.getInt(9) == 1
                    )
                )
            }
        }
        return list
    }

    suspend fun getAllProfiles(): List<VpnProfileEntity> {
        return getAllProfilesSync()
    }

    suspend fun getActiveProfileSync(): VpnProfileEntity? {
        val all = getAllProfilesSync()
        return all.firstOrNull { it.isSelected }
    }

    suspend fun getProfilesForSubscription(subId: Int): List<VpnProfileEntity> {
        return getAllProfilesSync().filter { it.subscriptionId == subId }
    }

    suspend fun insertSubscription(sub: SubscriptionEntity): Long {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            if (sub.id != 0) {
                put("id", sub.id)
            }
            put("name", sub.name)
            put("url", sub.url)
            put("addedAt", sub.addedAt)
        }
        val result = db.insertWithOnConflict("subscriptions", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        refreshAll()
        return result
    }

    suspend fun deleteSubscription(id: Int) {
        val db = dbHelper.writableDatabase
        db.delete("subscriptions", "id = ?", arrayOf(id.toString()))
        db.delete("vpn_profiles", "subscriptionId = ?", arrayOf(id.toString()))
        refreshAll()
    }

    suspend fun insertProfiles(profiles: List<VpnProfileEntity>) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            profiles.forEach { profile ->
                val cv = ContentValues().apply {
                    if (profile.id != 0) {
                        put("id", profile.id)
                    }
                    put("subscriptionId", profile.subscriptionId)
                    put("name", profile.name)
                    put("server", profile.server)
                    put("port", profile.port)
                    put("protocol", profile.protocol)
                    put("configPayload", profile.configPayload)
                    put("pingMs", profile.pingMs)
                    put("countryCode", profile.countryCode)
                    put("isSelected", if (profile.isSelected) 1 else 0)
                }
                db.insertWithOnConflict("vpn_profiles", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refreshAll()
    }

    suspend fun insertProfile(profile: VpnProfileEntity): Long {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            if (profile.id != 0) {
                put("id", profile.id)
            }
            put("subscriptionId", profile.subscriptionId)
            put("name", profile.name)
            put("server", profile.server)
            put("port", profile.port)
            put("protocol", profile.protocol)
            put("configPayload", profile.configPayload)
            put("pingMs", profile.pingMs)
            put("countryCode", profile.countryCode)
            put("isSelected", if (profile.isSelected) 1 else 0)
        }
        val result = db.insertWithOnConflict("vpn_profiles", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        refreshAll()
        return result
    }

    suspend fun deleteProfile(id: Int) {
        val db = dbHelper.writableDatabase
        db.delete("vpn_profiles", "id = ?", arrayOf(id.toString()))
        refreshAll()
    }

    suspend fun deleteProfilesBySubscription(subId: Int) {
        val db = dbHelper.writableDatabase
        db.delete("vpn_profiles", "subscriptionId = ?", arrayOf(subId.toString()))
        refreshAll()
    }

    suspend fun selectProfile(selectedId: Int) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("UPDATE vpn_profiles SET isSelected = 0")
            db.execSQL("UPDATE vpn_profiles SET isSelected = 1 WHERE id = $selectedId")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refreshAll()
    }

    suspend fun updatePing(id: Int, pingMs: Int) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("pingMs", pingMs)
        }
        db.update("vpn_profiles", cv, "id = ?", arrayOf(id.toString()))
        refreshAll()
    }
}
