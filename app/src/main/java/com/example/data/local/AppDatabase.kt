package com.example.data.local

import android.content.Context

class AppDatabase private constructor(context: Context) {

    private val dbHelper = AppDbHelper(context)
    private val vpnDao = VpnDao(dbHelper)

    fun vpnDao(): VpnDao {
        return vpnDao
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = AppDatabase(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
