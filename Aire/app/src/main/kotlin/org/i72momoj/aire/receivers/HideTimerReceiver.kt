package org.i72momoj.aire.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.i72momoj.aire.extensions.hideTimerNotification
import org.i72momoj.aire.helpers.INVALID_TIMER_ID
import org.i72momoj.aire.helpers.TIMER_ID
import org.i72momoj.aire.models.TimerEvent
import org.greenrobot.eventbus.EventBus

class HideTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val timerId = intent.getIntExtra(TIMER_ID, INVALID_TIMER_ID)
        context.hideTimerNotification(timerId)
        EventBus.getDefault().post(TimerEvent.Reset(timerId))
    }
}
