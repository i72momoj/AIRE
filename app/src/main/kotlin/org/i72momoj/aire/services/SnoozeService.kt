package org.i72momoj.aire.services

import android.app.IntentService
import android.content.Intent
import org.i72momoj.aire.extensions.alarmController
import org.i72momoj.aire.extensions.config
import org.i72momoj.aire.helpers.ALARM_ID

class SnoozeService : IntentService("Snooze") {
    override fun onHandleIntent(intent: Intent?) {
        val id = intent!!.getIntExtra(ALARM_ID, -1)
        alarmController.snoozeAlarm(id, config.snoozeTime)
    }
}
