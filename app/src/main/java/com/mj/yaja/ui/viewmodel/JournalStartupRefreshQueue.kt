package com.mj.yaja.ui.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal enum class StartupRefreshPriority(val order: Int) {
    VISIBLE_STATE(0),
    SECONDARY_STATE(1),
    BACKGROUND_STATE(2)
}

internal data class StartupRefreshTask(
    val name: String,
    val label: String,
    val priority: StartupRefreshPriority,
    val timeoutMs: Long,
    val block: suspend () -> Unit
)

internal fun launchStartupRefreshQueueWorkflow(
    currentJob: Job?,
    scope: CoroutineScope,
    tasks: List<StartupRefreshTask>,
    initialDelayMs: Long,
    betweenTaskDelayMs: Long,
    backgroundWorkLabel: MutableStateFlow<String?>,
    logPerf: (String, Long) -> Unit,
    logWarning: (String, String) -> Unit
): Job {
    currentJob?.cancel()
    return scope.launch {
        try {
            delay(initialDelayMs)
            tasks.sortedBy { it.priority.order }
                .forEach { task ->
                    val startedAt = System.currentTimeMillis()
                    backgroundWorkLabel.value = task.label
                    val completed = withTimeoutOrNull(task.timeoutMs) {
                        task.block()
                        true
                    } == true
                    val elapsed = System.currentTimeMillis() - startedAt
                    logPerf("startupQueue.${task.name}", elapsed)
                    if (!completed) {
                        logWarning(
                            "Startup refresh timed out",
                            "task=${task.name} elapsedMs=$elapsed timeoutMs=${task.timeoutMs}"
                        )
                    }
                    delay(betweenTaskDelayMs)
                }
        } finally {
            backgroundWorkLabel.value = null
        }
    }
}
