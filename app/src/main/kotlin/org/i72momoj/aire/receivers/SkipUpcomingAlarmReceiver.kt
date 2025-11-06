package org.i72momoj.aire.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.i72momoj.aire.extensions.alarmController
import org.i72momoj.aire.extensions.goAsync
import org.i72momoj.aire.extensions.hideNotification
import org.i72momoj.aire.helpers.ALARM_ID
import org.i72momoj.aire.helpers.NOTIFICATION_ID

/**
 * Receiver responsible for dismissing *UPCOMING* alarms.
 */
class SkipUpcomingAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra(ALARM_ID, -1)
        if (alarmId != -1) {
            goAsync {
                context.alarmController.skipNextOccurrence(alarmId)
            }
        }

        val notificationId = intent.getIntExtra(NOTIFICATION_ID, -1)
        if (notificationId != -1) {
            context.hideNotification(notificationId)
        }
    }
}
