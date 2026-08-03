package com.mj.yaja.util

import android.os.SystemClock
import android.os.Trace
import android.util.Log

object PerformanceTrace {
    private const val MAX_TRACE_SECTION_LENGTH = 120

    fun <T> trace(section: String, block: () -> T): T {
        Trace.beginSection(section.safeTraceSection())
        return try {
            block()
        } finally {
            Trace.endSection()
        }
    }

    fun <T> measure(
        section: String,
        logPerf: (String, Long) -> Unit,
        block: () -> T
    ): T {
        val startedAt = SystemClock.elapsedRealtime()
        return trace(section) {
            try {
                block()
            } finally {
                logPerf(section, SystemClock.elapsedRealtime() - startedAt)
            }
        }
    }

    suspend fun <T> measureSuspend(
        section: String,
        logPerf: (String, Long) -> Unit,
        block: suspend () -> T
    ): T {
        val startedAt = SystemClock.elapsedRealtime()
        Trace.beginSection(section.safeTraceSection())
        return try {
            block()
        } finally {
            Trace.endSection()
            logPerf(section, SystemClock.elapsedRealtime() - startedAt)
        }
    }

    fun log(tag: String, phase: String, elapsedMs: Long) {
        Log.d(tag, "perf:$phase=${elapsedMs}ms")
    }

    fun nowMs(): Long = SystemClock.elapsedRealtime()

    private fun String.safeTraceSection(): String =
        replace('\n', ' ')
            .replace('\r', ' ')
            .take(MAX_TRACE_SECTION_LENGTH)
}
