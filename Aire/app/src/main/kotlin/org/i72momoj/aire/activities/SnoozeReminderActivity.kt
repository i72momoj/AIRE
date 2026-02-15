package org.i72momoj.aire.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.i72momoj.aire.extensions.alarmController
import org.i72momoj.aire.extensions.config
import org.i72momoj.aire.helpers.ALARM_ID
import org.fossify.commons.extensions.showPickSecondsDialog
import org.fossify.commons.helpers.MINUTE_SECONDS

class SnoozeReminderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val alarmId = intent.getIntExtra(ALARM_ID, -1)
        if (alarmId == -1) {
            finish()
            return
        }

        alarmController.silenceAlarm(alarmId)
        showPickSecondsDialog(
            curSeconds = config.snoozeTime * MINUTE_SECONDS,
            isSnoozePicker = true,
            cancelCallback = {
                alarmController.stopAlarm(alarmId)
                dialogCancelled()
            }
        ) {
            config.snoozeTime = it / MINUTE_SECONDS
            alarmController.snoozeAlarm(alarmId, config.snoozeTime)
            finishActivity()
        }
    }

    private fun dialogCancelled() {
        finishActivity()
    }

    private fun finishActivity() {
        finish()
        overridePendingTransition(0, 0)
    }
}
