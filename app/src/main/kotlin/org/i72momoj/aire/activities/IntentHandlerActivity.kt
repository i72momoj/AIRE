package org.i72momoj.aire.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.media.RingtoneManager
import android.os.Bundle
import android.provider.AlarmClock
import androidx.core.net.toUri
import org.i72momoj.aire.activities.SimpleActivity
import org.i72momoj.aire.dialogs.EditAlarmDialog
import org.i72momoj.aire.dialogs.EditTimerDialog
import org.i72momoj.aire.dialogs.SelectAlarmDialog
import org.i72momoj.aire.extensions.alarmController
import org.i72momoj.aire.extensions.alarmManager
import org.i72momoj.aire.extensions.config
import org.i72momoj.aire.extensions.createNewAlarm
import org.i72momoj.aire.extensions.createNewTimer
import org.i72momoj.aire.extensions.dbHelper
import org.i72momoj.aire.extensions.getHideTimerPendingIntent
import org.i72momoj.aire.extensions.getSkipUpcomingAlarmPendingIntent
import org.i72momoj.aire.extensions.isBitSet
import org.i72momoj.aire.extensions.secondsToMillis
import org.i72momoj.aire.extensions.timerHelper
import org.i72momoj.aire.helpers.DEFAULT_ALARM_MINUTES
import org.i72momoj.aire.helpers.TODAY_BIT
import org.i72momoj.aire.helpers.TOMORROW_BIT
import org.i72momoj.aire.helpers.UPCOMING_ALARM_NOTIFICATION_ID
import org.i72momoj.aire.helpers.getBitForCalendarDay
import org.i72momoj.aire.helpers.getCurrentDayMinutes
import org.i72momoj.aire.helpers.getTodayBit
import org.i72momoj.aire.helpers.getTomorrowBit
import org.i72momoj.aire.models.Alarm
import org.i72momoj.aire.models.AlarmEvent
import org.i72momoj.aire.models.Timer
import org.i72momoj.aire.models.TimerEvent
import org.i72momoj.aire.models.TimerState
import org.fossify.commons.R
import org.fossify.commons.dialogs.PermissionRequiredDialog
import org.fossify.commons.extensions.getDefaultAlarmSound
import org.fossify.commons.extensions.getFilenameFromUri
import org.fossify.commons.extensions.openNotificationSettings
import org.fossify.commons.helpers.SILENT
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.models.AlarmSound
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit

class IntentHandlerActivity : SimpleActivity() {
    companion object {
        @SuppressLint("InlinedApi")
        val HANDLED_ACTIONS = listOf(
            AlarmClock.ACTION_SET_ALARM,
            AlarmClock.ACTION_SET_TIMER,
            AlarmClock.ACTION_DISMISS_ALARM,
            AlarmClock.ACTION_DISMISS_TIMER
        )

        private const val URI_SCHEME = "id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intentToHandle: Intent) {
        intentToHandle.apply {
            when (action) {
                AlarmClock.ACTION_SET_ALARM -> setNewAlarm()
                AlarmClock.ACTION_SET_TIMER -> setNewTimer()
                AlarmClock.ACTION_DISMISS_ALARM -> dismissAlarm()
                AlarmClock.ACTION_DISMISS_TIMER -> dismissTimer()
                else -> finish()
            }
        }
    }

    private fun Intent.setNewAlarm() {
        val hour = getIntExtra(AlarmClock.EXTRA_HOUR, 0).coerceIn(0, 23)
        val minute = getIntExtra(AlarmClock.EXTRA_MINUTES, 0).coerceIn(0, 59)
        val days = getIntegerArrayListExtra(AlarmClock.EXTRA_DAYS)
            ?: getIntArrayExtra(AlarmClock.EXTRA_DAYS)?.toList()
        val message = getStringExtra(AlarmClock.EXTRA_MESSAGE)
        val ringtone = getStringExtra(AlarmClock.EXTRA_RINGTONE)
        val vibrate = getBooleanExtra(AlarmClock.EXTRA_VIBRATE, true)
        val skipUi = getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false)
        val defaultAlarmSound = getDefaultAlarmSound(RingtoneManager.TYPE_ALARM)

        var weekDays = 0
        days?.forEach {
            weekDays += getBitForCalendarDay(it)
        }
        val soundToUse = ringtone?.let {
            if (it == AlarmClock.VALUE_RINGTONE_SILENT) {
                AlarmSound(0, getString(R.string.no_sound), SILENT)
            } else {
                try {
                    val uri = it.toUri()
                    var filename = getFilenameFromUri(uri)
                    if (filename.isEmpty()) {
                        filename = getString(R.string.alarm)
                    }
                    AlarmSound(0, filename, it)
                } catch (e: Exception) {
                    null
                }
            }
        } ?: defaultAlarmSound

        // We don't want to accidentally edit existing alarm, so allow reuse only when skipping UI
        if (hasExtra(AlarmClock.EXTRA_HOUR) && skipUi) {
            var daysToCompare = weekDays
            val timeInMinutes = hour * 60 + minute
            if (weekDays <= 0) {
                daysToCompare = if (timeInMinutes > getCurrentDayMinutes()) {
                    TODAY_BIT
                } else {
                    TOMORROW_BIT
                }
            }
            val existingAlarm = dbHelper.getAlarms().firstOrNull {
                it.days == daysToCompare
                        && it.vibrate == vibrate
                        && it.soundTitle == soundToUse.title
                        && it.soundUri == soundToUse.uri
                        && it.label == (message ?: "")
                        && it.timeInMinutes == timeInMinutes
            }

            if (existingAlarm != null && !existingAlarm.isEnabled) {
                existingAlarm.isEnabled = true
                startAlarm(existingAlarm)
                finish()
            }
        }

        val newAlarm = createNewAlarm(DEFAULT_ALARM_MINUTES, 0)
        newAlarm.isEnabled = true
        newAlarm.days = weekDays
        newAlarm.vibrate = vibrate
        newAlarm.soundTitle = soundToUse.title
        newAlarm.soundUri = soundToUse.uri
        if (message != null) {
            newAlarm.label = message
        }

        if (!hasExtra(AlarmClock.EXTRA_HOUR) || !skipUi) {
            newAlarm.id = -1
            newAlarm.timeInMinutes += minute
            openEditAlarm(newAlarm)
        } else {
            newAlarm.timeInMinutes = hour * 60 + minute
            if (newAlarm.days <= 0) {
                newAlarm.days = if (newAlarm.timeInMinutes > getCurrentDayMinutes()) {
                    TODAY_BIT
                } else {
                    TOMORROW_BIT
                }
                newAlarm.oneShot = true
            }

            ensureBackgroundThread {
                newAlarm.id = dbHelper.insertAlarm(newAlarm)
                runOnUiThread {
                    startAlarm(newAlarm)
                    finish()
                }
            }
        }
    }

    private fun Intent.setNewTimer() {
        val length = getIntExtra(AlarmClock.EXTRA_LENGTH, -1)
        val message = getStringExtra(AlarmClock.EXTRA_MESSAGE)
        val skipUi = getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false)

        fun createAndStartNewTimer() {
            val newTimer = createNewTimer()
            if (message != null) {
                newTimer.label = message
            }

            if (length < 0 || !skipUi) {
                newTimer.id = -1
                openEditTimer(newTimer)
            } else {
                newTimer.seconds = length
                newTimer.oneShot = true

                timerHelper.insertOrUpdateTimer(newTimer) {
                    config.timerLastConfig = newTimer
                    newTimer.id = it.toInt()
                    startTimer(newTimer)
                }
            }
        }

        if (hasExtra(AlarmClock.EXTRA_LENGTH)) {
            timerHelper.findTimers(length, message ?: "") {
                val existingTimer = it.firstOrNull { it.state is TimerState.Idle }

                // We don't want to accidentally edit existing timer, so allow reuse only when skipping UI
                if (existingTimer != null
                    && skipUi
                    && (existingTimer.state is TimerState.Idle || (existingTimer.state is TimerState.Finished && !existingTimer.oneShot))
                ) {
                    startTimer(existingTimer)
                } else {
                    createAndStartNewTimer()
                }
            }
        } else {
            createAndStartNewTimer()
        }
    }

    private fun Intent.dismissAlarm() {
        val searchMode = getStringExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE)
        val uri = data
        if (uri != null) {
            if (uri.scheme == URI_SCHEME) {
                val id = uri.schemeSpecificPart.toIntOrNull()
                if (id != null) {
                    val alarm = dbHelper.getAlarmWithId(id)
                    if (alarm != null) {
                        getSkipUpcomingAlarmPendingIntent(
                            alarmId = alarm.id,
                            notificationId = UPCOMING_ALARM_NOTIFICATION_ID
                        ).send()
                        EventBus.getDefault().post(AlarmEvent.Refresh)
                        finish()
                    }
                }
            }
            finish()
        } else {
            var alarms = dbHelper.getAlarms().filter { it.isEnabled }

            if (searchMode != null) {
                when (searchMode) {
                    AlarmClock.ALARM_SEARCH_MODE_TIME -> {
                        if (hasExtra(AlarmClock.EXTRA_HOUR)) {
                            val hour = getIntExtra(AlarmClock.EXTRA_HOUR, -1).coerceIn(0, 23)
                            alarms = alarms.filter {
                                it.timeInMinutes / 60 == hour || it.timeInMinutes / 60 == hour + 12
                            }
                        }
                        if (hasExtra(AlarmClock.EXTRA_MINUTES)) {
                            val minute = getIntExtra(AlarmClock.EXTRA_MINUTES, -1).coerceIn(0, 59)
                            alarms = alarms.filter { it.timeInMinutes % 60 == minute }
                        }
                        if (hasExtra(AlarmClock.EXTRA_IS_PM)) {
                            val isPm = getBooleanExtra(AlarmClock.EXTRA_IS_PM, false)
                            alarms = alarms.filter {
                                val hour = it.timeInMinutes / 60
                                if (isPm) {
                                    hour in 12..23
                                } else {
                                    hour in 0..11
                                }
                            }
                        }
                    }

                    AlarmClock.ALARM_SEARCH_MODE_NEXT -> {
                        val next = alarmManager.nextAlarmClock
                        val timeInMinutes =
                            TimeUnit.MILLISECONDS.toMinutes(next.triggerTime).toInt()
                        val dayBitToLookFor = if (timeInMinutes <= getCurrentDayMinutes()) {
                            getTomorrowBit()
                        } else {
                            getTodayBit()
                        }
                        val dayToLookFor = if (timeInMinutes <= getCurrentDayMinutes()) {
                            TOMORROW_BIT
                        } else {
                            TODAY_BIT
                        }
                        alarms = alarms.filter {
                            it.timeInMinutes == timeInMinutes && (it.days.isBitSet(dayBitToLookFor) || it.days == dayToLookFor)
                        }
                    }

                    AlarmClock.ALARM_SEARCH_MODE_LABEL -> {
                        val messageToSearchFor = getStringExtra(AlarmClock.EXTRA_MESSAGE)
                        if (messageToSearchFor != null) {
                            alarms = alarms.filter {
                                it.label.contains(
                                    other = messageToSearchFor,
                                    ignoreCase = true
                                )
                            }
                        }
                    }

                    AlarmClock.ALARM_SEARCH_MODE_ALL -> {
                        // no-op - no further filtering needed
                    }
                }
            }

            if (alarms.count() == 1) {
                getSkipUpcomingAlarmPendingIntent(
                    alarmId = alarms.first().id,
                    notificationId = UPCOMING_ALARM_NOTIFICATION_ID
                ).send()
                EventBus.getDefault().post(AlarmEvent.Refresh)
                finish()
            } else if (alarms.count() > 1) {
                SelectAlarmDialog(
                    activity = this@IntentHandlerActivity,
                    alarms = alarms,
                    titleResId = org.i72momoj.aire.R.string.select_alarm_to_dismiss
                ) {
                    if (it != null) {
                        getSkipUpcomingAlarmPendingIntent(
                            alarmId = it.id,
                            notificationId = UPCOMING_ALARM_NOTIFICATION_ID
                        ).send()
                    }
                    EventBus.getDefault().post(AlarmEvent.Refresh)
                    finish()
                }
            } else {
                finish()
            }
        }
    }

    private fun Intent.dismissTimer() {
        val uri = data
        if (uri == null) {
            timerHelper.getTimers {
                it.filter { it.state == TimerState.Finished }.forEach {
                    getHideTimerPendingIntent(it.id!!).send()
                }
                EventBus.getDefault().post(TimerEvent.Refresh)
                finish()
            }
            return
        } else if (uri.scheme == URI_SCHEME) {
            val id = uri.schemeSpecificPart.toIntOrNull()
            if (id != null) {
                timerHelper.tryGetTimer(id) {
                    if (it != null) {
                        getHideTimerPendingIntent(it.id!!).send()
                        EventBus.getDefault().post(TimerEvent.Refresh)
                        finish()
                    } else {
                        finish()
                    }
                }
                return
            }
        }
        finish()
    }

    private fun openEditAlarm(alarm: Alarm) {
        EditAlarmDialog(this, alarm, onDismiss = { finish() }) {
            alarm.id = it
            startAlarm(alarm)
            finish()
        }
    }

    private fun openEditTimer(timer: Timer) {
        EditTimerDialog(this, timer) {
            timer.id = it.toInt()
            startTimer(timer)
        }
    }

    private fun startAlarm(alarm: Alarm) {
        alarmController.scheduleNextOccurrence(alarm, true)
        EventBus.getDefault().post(AlarmEvent.Refresh)
    }

    private fun startTimer(timer: Timer) {
        handleNotificationPermission { granted ->
            val newState = TimerState.Running(
                duration = timer.seconds.secondsToMillis,
                tick = timer.seconds.secondsToMillis
            )
            val newTimer = Timer(
                id = timer.id,
                seconds = timer.seconds,
                state = newState,
                vibrate = timer.vibrate,
                soundUri = timer.soundUri,
                soundTitle = timer.soundTitle,
                label = timer.label,
                createdAt = timer.createdAt,
                channelId = timer.channelId,
                oneShot = timer.oneShot
            )
                //timer.copy(state = newState)
            fun notifyAndStartTimer() {
                EventBus.getDefault().post(
                    TimerEvent.Start(
                        timerId = newTimer.id!!,
                        duration = newTimer.seconds.secondsToMillis
                    )
                )
                EventBus.getDefault().post(TimerEvent.Refresh)
            }

            if (granted) {
                timerHelper.insertOrUpdateTimer(newTimer) {
                    notifyAndStartTimer()
                    finish()
                }
            } else {
                PermissionRequiredDialog(
                    activity = this,
                    textId = R.string.allow_notifications_reminders,
                    positiveActionCallback = {
                        openNotificationSettings()
                        timerHelper.insertOrUpdateTimer(newTimer) {
                            notifyAndStartTimer()
                            finish()
                        }
                    },
                    negativeActionCallback = {
                        finish()
                    }
                )
            }
        }
    }
}
