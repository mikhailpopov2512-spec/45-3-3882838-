package com.example.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AppDbHelper(context: Context) : SQLiteOpenHelper(context, "vpn_database.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS subscriptions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                url TEXT NOT NULL,
                addedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS vpn_profiles (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                subscriptionId INTEGER,
                name TEXT NOT NULL,
                server TEXT NOT NULL,
                port INTEGER NOT NULL,
                protocol TEXT NOT NULL,
                configPayload TEXT NOT NULL,
                pingMs INTEGER NOT NULL,
                countryCode TEXT NOT NULL,
                isSelected INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS subscriptions")
        db.execSQL("DROP TABLE IF EXISTS vpn_profiles")
        onCreate(db)
    }
}
