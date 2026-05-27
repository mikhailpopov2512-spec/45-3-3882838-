package com.example.data.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppLogger {
    private const val TAG = "AppLogger"
    private const val LOG_FILE_NAME = "vpn_app_logs.txt"
    private const val MAX_LOGS_IN_MEMORY = 500

    private val _logsFlow = MutableStateFlow<List<String>>(emptyList())
    val logsFlow: StateFlow<List<String>> = _logsFlow.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    fun init(context: Context) {
        val file = getLogFile(context)
        if (file.exists()) {
            try {
                val lines = file.readLines().takeLast(MAX_LOGS_IN_MEMORY)
                _logsFlow.value = lines
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing logs from file: ${e.message}")
            }
        }
    }

    @Synchronized
    fun log(context: Context, tag: String, message: String) {
        val timeStr = dateFormat.format(Date())
        val logLine = "[$timeStr] [$tag] $message"
        Log.d(tag, message)

        // Write to file
        try {
            val file = getLogFile(context)
            FileWriter(file, true).use { writer ->
                writer.append(logLine).append("\n")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing to log file: ${e.message}")
        }

        // Update memory flow
        val current = _logsFlow.value.toMutableList()
        current.add(logLine)
        if (current.size > MAX_LOGS_IN_MEMORY) {
            current.removeAt(0)
        }
        _logsFlow.value = current
    }

    fun getLogFile(context: Context): File {
        return File(context.filesDir, LOG_FILE_NAME)
    }

    @Synchronized
    fun clearLogs(context: Context) {
        try {
            val file = getLogFile(context)
            if (file.exists()) {
                file.delete()
            }
            file.createNewFile()
            _logsFlow.value = emptyList()
            log(context, "SYSTEM", "Logs cleared successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear logs file: ${e.message}")
        }
    }
}
