package com.mj.yaja.ui.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class WidgetAppearanceRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        WidgetRefreshCoordinator.requestAppearanceUpdate(applicationContext)
        return Result.success()
    }
}
