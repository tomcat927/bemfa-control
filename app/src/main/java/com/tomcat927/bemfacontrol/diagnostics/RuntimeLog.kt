package com.tomcat927.bemfacontrol.diagnostics

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object RuntimeLog {

    private const val MAX_ENTRIES = 300
    private val formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS")
    private val entries = ArrayDeque<String>(MAX_ENTRIES)

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

    fun snapshot(): List<String> = synchronized(entries) { entries.toList() }

    fun clear() = synchronized(entries) { entries.clear() }

    private fun append(level: String, message: String, throwable: Throwable?) {
        synchronized(entries) {
            entries.addLast(buildString {
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
            })
            while (entries.size > MAX_ENTRIES) entries.removeFirst()
        }
    }
}
