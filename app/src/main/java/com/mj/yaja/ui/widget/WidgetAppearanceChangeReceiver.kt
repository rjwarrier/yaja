package com.mj.yaja.ui.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class WidgetAppearanceChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("WidgetAppearance", "received ${intent.action}")
        WidgetAppearanceHelper.refreshWidgetsIfAppearanceChanged(
            context = context.applicationContext,
            immediate = false
        )
    }
}
