package com.example.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VpnDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "vpn_database.db"
        private const val DATABASE_VERSION = 2

        @Volatile
        private var INSTANCE: VpnDatabaseHelper? = null

        fun getInstance(context: Context): VpnDatabaseHelper {
            return INSTANCE ?: synchronized(this) {
                val instance = VpnDatabaseHelper(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    private val _profilesFlow = MutableStateFlow<List<VpnProfileEntity>>(emptyList())
    val profilesFlow: StateFlow<List<VpnProfileEntity>> = _profilesFlow.asStateFlow()

    private val _subscriptionsFlow = MutableStateFlow<List<SubscriptionEntity>>(emptyList())
    val subscriptionsFlow: StateFlow<List<SubscriptionEntity>> = _subscriptionsFlow.asStateFlow()

    init {
        refresh()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE vpn_profiles (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                protocol TEXT NOT NULL,
                host TEXT NOT NULL,
                port INTEGER NOT NULL,
                fullConfig TEXT NOT NULL,
                ping INTEGER NOT NULL,
                dateAdded INTEGER NOT NULL,
                subscriptionUrl TEXT
            )
        """)
        db.execSQL("""
            CREATE TABLE subscriptions (
                url TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                lastUpdated INTEGER NOT NULL,
                nodeCount INTEGER NOT NULL
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS vpn_profiles")
        db.execSQL("DROP TABLE IF EXISTS subscriptions")
        onCreate(db)
    }

    fun refresh() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val profiles = getAllProfilesInternal()
                val subscriptions = getAllSubscriptionsInternal()
                _profilesFlow.value = profiles
                _subscriptionsFlow.value = subscriptions
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun getAllProfilesInternal(): List<VpnProfileEntity> {
        val list = mutableListOf<VpnProfileEntity>()
        return try {
            val db = readableDatabase
            val cursor = db.query("vpn_profiles", null, null, null, null, null, "dateAdded DESC")
            cursor.use { c ->
                val idCol = c.getColumnIndex("id")
                val nameCol = c.getColumnIndex("name")
                val protoCol = c.getColumnIndex("protocol")
                val hostCol = c.getColumnIndex("host")
                val portCol = c.getColumnIndex("port")
                val configCol = c.getColumnIndex("fullConfig")
                val pingCol = c.getColumnIndex("ping")
                val dateCol = c.getColumnIndex("dateAdded")
                val subCol = c.getColumnIndex("subscriptionUrl")

                if (idCol != -1 && nameCol != -1 && protoCol != -1 && hostCol != -1 && portCol != -1 && configCol != -1 && pingCol != -1 && dateCol != -1 && subCol != -1) {
                    while (c.moveToNext()) {
                        list.add(
                            VpnProfileEntity(
                                id = c.getInt(idCol),
                                name = c.getString(nameCol),
                                protocol = c.getString(protoCol),
                                host = c.getString(hostCol),
                                port = c.getInt(portCol),
                                fullConfig = c.getString(configCol),
                                ping = c.getInt(pingCol),
                                dateAdded = c.getLong(dateCol),
                                subscriptionUrl = c.getString(subCol)
                            )
                        )
                    }
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun insertProfile(profile: VpnProfileEntity): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("name", profile.name)
            put("protocol", profile.protocol)
            put("host", profile.host)
            put("port", profile.port)
            put("fullConfig", profile.fullConfig)
            put("ping", profile.ping)
            put("dateAdded", profile.dateAdded)
            put("subscriptionUrl", profile.subscriptionUrl)
        }
        val id = db.insert("vpn_profiles", null, values)
        refresh()
        return id
    }

    fun insertProfiles(profiles: List<VpnProfileEntity>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (profile in profiles) {
                val values = ContentValues().apply {
                    put("name", profile.name)
                    put("protocol", profile.protocol)
                    put("host", profile.host)
                    put("port", profile.port)
                    put("fullConfig", profile.fullConfig)
                    put("ping", profile.ping)
                    put("dateAdded", profile.dateAdded)
                    put("subscriptionUrl", profile.subscriptionUrl)
                }
                db.insert("vpn_profiles", null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refresh()
    }

    fun deleteProfileById(id: Int) {
        val db = writableDatabase
        db.delete("vpn_profiles", "id = ?", arrayOf(id.toString()))
        refresh()
    }

    fun deleteUnreachableProfiles() {
        val db = writableDatabase
        db.delete("vpn_profiles", "ping = -2", null)
        refresh()
    }

    fun updatePing(id: Int, ping: Int) {
        synchronized(this) {
            try {
                val db = writableDatabase
                val values = ContentValues().apply {
                    put("ping", ping)
                }
                db.update("vpn_profiles", values, "id = ?", arrayOf(id.toString()))
            } catch (e: Exception) {
                // ignore
            }
            val currentList = _profilesFlow.value
            val index = currentList.indexOfFirst { it.id == id }
            if (index != -1) {
                val updated = currentList.toMutableList()
                updated[index] = updated[index].copy(ping = ping)
                _profilesFlow.value = updated
            }
        }
    }

    fun deleteProfilesBySubscription(subscriptionUrl: String) {
        val db = writableDatabase
        db.delete("vpn_profiles", "subscriptionUrl = ?", arrayOf(subscriptionUrl))
        refresh()
    }

    fun deleteAllProfiles() {
        val db = writableDatabase
        db.delete("vpn_profiles", null, null)
        refresh()
    }

    private fun getAllSubscriptionsInternal(): List<SubscriptionEntity> {
        val list = mutableListOf<SubscriptionEntity>()
        return try {
            val db = readableDatabase
            val cursor = db.query("subscriptions", null, null, null, null, null, "lastUpdated DESC")
            cursor.use { c ->
                val urlCol = c.getColumnIndex("url")
                val nameCol = c.getColumnIndex("name")
                val lastCol = c.getColumnIndex("lastUpdated")
                val countCol = c.getColumnIndex("nodeCount")

                if (urlCol != -1 && nameCol != -1 && lastCol != -1 && countCol != -1) {
                    while (c.moveToNext()) {
                        list.add(
                            SubscriptionEntity(
                                url = c.getString(urlCol),
                                name = c.getString(nameCol),
                                lastUpdated = c.getLong(lastCol),
                                nodeCount = c.getInt(countCol)
                            )
                        )
                    }
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun insertSubscription(sub: SubscriptionEntity) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("url", sub.url)
            put("name", sub.name)
            put("lastUpdated", sub.lastUpdated)
            put("nodeCount", sub.nodeCount)
        }
        db.insertWithOnConflict("subscriptions", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        refresh()
    }

    fun deleteSubscriptionByUrl(url: String) {
        val db = writableDatabase
        db.delete("subscriptions", "url = ?", arrayOf(url))
        refresh()
    }
}
