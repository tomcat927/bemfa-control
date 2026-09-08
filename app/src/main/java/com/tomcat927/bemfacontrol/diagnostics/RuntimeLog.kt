package com.tomcat927.bemfacontrol.diagnostics

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object RuntimeLog {

    private const val MAX_ENTRIES = 300
    private val formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS")
    private val _entries = MutableStateFlow<List<String>>(emptyList())
    val entries: StateFlow<List<String>> = _entries.asStateFlow()

    @Volatile
    var enabled: Boolean = false

    fun debug(message: String, throwable: Throwable? = null) {
        if (enabled) append("DEBUG", message, throwable)
    }

    fun info(message: String, throwable: Throwable? = null) {
        if (enabled) append("INFO", message, throwable)
    }

    fun error(message: String, throwable: Throwable? = null) {
        append("ERROR", message, throwable)
    }

    fun snapshot(): List<String> = _entries.value

    fun clear() {
        _entries.value = emptyList()
    }

    private fun append(level: String, message: String, throwable: Throwable?) {
        val newEntry = buildString {
            append(LocalDateTime.now().format(formatter))
            append(' ')
            append(level)
            append(' ')
            append(message)
            throwable?.let { error ->
                append(" | ")
                append(error.javaClass.name)
                append(": ")
                append(error.message ?: "(no message)")
                error.cause?.let { cause ->
                    append(" | caused by ")
                    append(cause.javaClass.name)
                    append(": ")
                    append(cause.message ?: "(no message)")
                }
            }
        }
        _entries.update { current ->
            val result = current + newEntry
            if (result.size > MAX_ENTRIES) result.takeLast(MAX_ENTRIES) else result
        }
    }
}
