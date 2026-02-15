package org.i72momoj.aire.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.i72momoj.aire.extensions.updateWidgets

class UpdateWidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.updateWidgets()
    }
}
