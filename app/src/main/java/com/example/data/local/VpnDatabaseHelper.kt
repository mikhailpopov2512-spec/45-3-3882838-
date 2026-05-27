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
        private const val DATABASE_VERSION = 3

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

    // New reactive flows for User Database, Support & Announcements
    private val _usersFlow = MutableStateFlow<List<UserAccountEntity>>(emptyList())
    val usersFlow: StateFlow<List<UserAccountEntity>> = _usersFlow.asStateFlow()

    private val _supportMessagesFlow = MutableStateFlow<List<SupportMessageEntity>>(emptyList())
    val supportMessagesFlow: StateFlow<List<SupportMessageEntity>> = _supportMessagesFlow.asStateFlow()

    private val _announcementsFlow = MutableStateFlow<List<AnnouncementEntity>>(emptyList())
    val announcementsFlow: StateFlow<List<AnnouncementEntity>> = _announcementsFlow.asStateFlow()

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
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS users_accounts (
                username TEXT PRIMARY KEY,
                password TEXT NOT NULL,
                role TEXT NOT NULL,
                is_blocked INTEGER DEFAULT 0
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS support_messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sender TEXT NOT NULL,
                text TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                reply_by TEXT,
                reply_text TEXT,
                reply_role TEXT
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS global_announcements (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sender TEXT NOT NULL,
                text TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
        """)

        // Seed default accounts
        seedDefaultAccounts(db)
    }

    private fun seedDefaultAccounts(db: SQLiteDatabase) {
        val defaultUsers = listOf(
            ContentValues().apply {
                put("username", "Михаил Попов")
                put("password", "creator")
                put("role", "CREATOR")
                put("is_blocked", 0)
            },
            ContentValues().apply {
                put("username", "Старший администратор")
                put("password", "admin")
                put("role", "SENIOR_ADMIN")
                put("is_blocked", 0)
            },
            ContentValues().apply {
                put("username", "Младший администратор")
                put("password", "moderator")
                put("role", "JUNIOR_ADMIN")
                put("is_blocked", 0)
            },
            ContentValues().apply {
                put("username", "Роскомнадзор")
                put("password", "blockme")
                put("role", "ROSKOMNADZOR")
                put("is_blocked", 1) // Blocked by default as an easter egg!
            }
        )
        for (user in defaultUsers) {
            db.insertWithOnConflict("users_accounts", null, user, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE IF NOT EXISTS users_accounts (username TEXT PRIMARY KEY, password TEXT NOT NULL, role TEXT NOT NULL, is_blocked INTEGER DEFAULT 0)")
            db.execSQL("CREATE TABLE IF NOT EXISTS support_messages (id INTEGER PRIMARY KEY AUTOINCREMENT, sender TEXT NOT NULL, text TEXT NOT NULL, timestamp INTEGER NOT NULL, reply_by TEXT, reply_text TEXT, reply_role TEXT)")
            db.execSQL("CREATE TABLE IF NOT EXISTS global_announcements (id INTEGER PRIMARY KEY AUTOINCREMENT, sender TEXT NOT NULL, text TEXT NOT NULL, timestamp INTEGER NOT NULL)")
            seedDefaultAccounts(db)
        }
    }

    fun refresh() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = readableDatabase
                // 1. Refresh Profiles
                val profiles = getAllProfilesInternal()
                _profilesFlow.value = profiles

                // 2. Refresh Subscriptions
                val subscriptions = getAllSubscriptionsInternal()
                _subscriptionsFlow.value = subscriptions

                // 3. Refresh Users
                val users = mutableListOf<UserAccountEntity>()
                val userCursor = db.query("users_accounts", null, null, null, null, null, "username ASC")
                userCursor.use { c ->
                    val userCol = c.getColumnIndex("username")
                    val roleCol = c.getColumnIndex("role")
                    val blockCol = c.getColumnIndex("is_blocked")
                    if (userCol != -1 && roleCol != -1 && blockCol != -1) {
                        while (c.moveToNext()) {
                            users.add(
                                UserAccountEntity(
                                    username = c.getString(userCol),
                                    role = c.getString(roleCol),
                                    isBlocked = c.getInt(blockCol) == 1
                                )
                            )
                        }
                    }
                }
                _usersFlow.value = users

                // 4. Refresh Support messages
                val messages = mutableListOf<SupportMessageEntity>()
                val msgCursor = db.query("support_messages", null, null, null, null, null, "timestamp ASC")
                msgCursor.use { c ->
                    val idCol = c.getColumnIndex("id")
                    val senderCol = c.getColumnIndex("sender")
                    val textCol = c.getColumnIndex("text")
                    val timeCol = c.getColumnIndex("timestamp")
                    val repByCol = c.getColumnIndex("reply_by")
                    val repTextCol = c.getColumnIndex("reply_text")
                    val repRoleCol = c.getColumnIndex("reply_role")
                    if (idCol != -1 && senderCol != -1 && textCol != -1 && timeCol != -1) {
                        while (c.moveToNext()) {
                            messages.add(
                                SupportMessageEntity(
                                    id = c.getInt(idCol),
                                    sender = c.getString(senderCol),
                                    text = c.getString(textCol),
                                    timestamp = c.getLong(timeCol),
                                    replyBy = if (repByCol != -1) c.getString(repByCol) else null,
                                    replyText = if (repTextCol != -1) c.getString(repTextCol) else null,
                                    replyRole = if (repRoleCol != -1) c.getString(repRoleCol) else null
                                )
                            )
                        }
                    }
                }
                _supportMessagesFlow.value = messages

                // 5. Refresh Announcements
                val ann = mutableListOf<AnnouncementEntity>()
                val annCursor = db.query("global_announcements", null, null, null, null, null, "timestamp DESC")
                annCursor.use { c ->
                    val idCol = c.getColumnIndex("id")
                    val senderCol = c.getColumnIndex("sender")
                    val textCol = c.getColumnIndex("text")
                    val timeCol = c.getColumnIndex("timestamp")
                    if (idCol != -1 && senderCol != -1 && textCol != -1 && timeCol != -1) {
                        while (c.moveToNext()) {
                            ann.add(
                                AnnouncementEntity(
                                    id = c.getInt(idCol),
                                    sender = c.getString(senderCol),
                                    text = c.getString(textCol),
                                    timestamp = c.getLong(timeCol)
                                )
                            )
                        }
                    }
                }
                _announcementsFlow.value = ann

            } catch (e: Exception) {
                // ignore
            }
        }
    }

    // Accessors and operators for new user management
    fun insertUser(user: UserAccountEntity, pass: String): Boolean {
        return try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put("username", user.username)
                put("password", pass)
                put("role", user.role)
                put("is_blocked", if (user.isBlocked) 1 else 0)
            }
            val id = db.insertWithOnConflict("users_accounts", null, values, SQLiteDatabase.CONFLICT_IGNORE)
            refresh()
            id != -1L
        } catch (e: Exception) {
            false
        }
    }

    fun authenticateUser(username: String, pass: String): UserAccountEntity? {
        return try {
            val db = readableDatabase
            var user: UserAccountEntity? = null
            val cursor = db.query(
                "users_accounts",
                null,
                "username = ? AND password = ?",
                arrayOf(username, pass),
                null, null, null
            )
            cursor.use { c ->
                val userCol = c.getColumnIndex("username")
                val roleCol = c.getColumnIndex("role")
                val blockCol = c.getColumnIndex("is_blocked")
                if (c.moveToFirst() && userCol != -1 && roleCol != -1 && blockCol != -1) {
                    user = UserAccountEntity(
                        username = c.getString(userCol),
                        role = c.getString(roleCol),
                        isBlocked = c.getInt(blockCol) == 1
                    )
                }
            }
            user
        } catch (e: Exception) {
            null
        }
    }

    fun getUserByUsername(username: String): UserAccountEntity? {
        return try {
            val db = readableDatabase
            var user: UserAccountEntity? = null
            val cursor = db.query(
                "users_accounts",
                null,
                "username = ?",
                arrayOf(username),
                null, null, null
            )
            cursor.use { c ->
                val userCol = c.getColumnIndex("username")
                val roleCol = c.getColumnIndex("role")
                val blockCol = c.getColumnIndex("is_blocked")
                if (c.moveToFirst() && userCol != -1 && roleCol != -1 && blockCol != -1) {
                    user = UserAccountEntity(
                        username = c.getString(userCol),
                        role = c.getString(roleCol),
                        isBlocked = c.getInt(blockCol) == 1
                    )
                }
            }
            user
        } catch (e: Exception) {
            null
        }
    }

    fun setUserBlockState(username: String, isBlocked: Boolean) {
        try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put("is_blocked", if (isBlocked) 1 else 0)
            }
            db.update("users_accounts", values, "username = ?", arrayOf(username))
            refresh()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun insertSupportMessage(sender: String, text: String) {
        try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put("sender", sender)
                put("text", text)
                put("timestamp", System.currentTimeMillis())
            }
            db.insert("support_messages", null, values)
            refresh()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun replyToSupportMessage(id: Int, replyBy: String, replyText: String, replyRole: String) {
        try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put("reply_by", replyBy)
                put("reply_text", replyText)
                put("reply_role", replyRole)
            }
            db.update("support_messages", values, "id = ?", arrayOf(id.toString()))
            refresh()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun insertAnnouncement(sender: String, text: String) {
        try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put("sender", sender)
                put("text", text)
                put("timestamp", System.currentTimeMillis())
            }
            db.insert("global_announcements", null, values)
            refresh()
        } catch (e: Exception) {
            // ignore
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

// Entity models for Account Manager & Support Service
data class UserAccountEntity(
    val username: String,
    val role: String,
    val isBlocked: Boolean
)

data class SupportMessageEntity(
    val id: Int,
    val sender: String,
    val text: String,
    val timestamp: Long,
    val replyBy: String?,
    val replyText: String?,
    val replyRole: String?
)

data class AnnouncementEntity(
    val id: Int,
    val sender: String,
    val text: String,
    val timestamp: Long
)

