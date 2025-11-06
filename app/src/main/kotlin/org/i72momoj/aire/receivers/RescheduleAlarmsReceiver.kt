package org.i72momoj.aire.receivers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.i72momoj.aire.extensions.alarmController
import org.i72momoj.aire.extensions.goAsync

/**
 * Receiver responsible for rescheduling alarms in background.
 */
class RescheduleAlarmsReceiver : BroadcastReceiver() {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        goAsync {
            context.alarmController.rescheduleEnabledAlarms()
        }
    }
}
