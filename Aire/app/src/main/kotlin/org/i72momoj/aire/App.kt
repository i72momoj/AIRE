package org.i72momoj.aire

import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import org.i72momoj.aire.extensions.config
import org.i72momoj.aire.extensions.getOpenTimerTabIntent
import org.i72momoj.aire.extensions.getTimerNotification
import org.i72momoj.aire.extensions.hideNotification
import org.i72momoj.aire.extensions.timerHelper
import org.i72momoj.aire.helpers.Stopwatch
import org.i72momoj.aire.helpers.Stopwatch.State
import org.i72momoj.aire.models.Timer
import org.i72momoj.aire.models.TimerEvent
import org.i72momoj.aire.models.TimerState
import org.i72momoj.aire.services.StopwatchStopService
import org.i72momoj.aire.services.TimerStopService
import org.i72momoj.aire.services.startStopwatchService
import org.i72momoj.aire.services.startTimerService
import org.fossify.commons.FossifyApp
import org.fossify.commons.extensions.notificationManager
import org.fossify.commons.extensions.showErrorToast
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlin.Int
import kotlin.collections.get

class App : FossifyApp(), LifecycleObserver {

    private var countDownTimers = mutableMapOf<Int, CountDownTimer>()

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        EventBus.getDefault().register(this)
    }

    override fun onTerminate() {
        EventBus.getDefault().unregister(this)
        super.onTerminate()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun onAppBackgrounded() {
        timerHelper.getTimers { timers ->
            if (timers.any { it.state is TimerState.Running }) {
                startTimerService(this)
            }
        }
        if (Stopwatch.state == State.RUNNING) {
            startStopwatchService(this)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private fun onAppForegrounded() {
        EventBus.getDefault().post(TimerStopService)
        timerHelper.getTimers { timers ->
            val runningTimers = timers.filter { it.state is TimerState.Running }
            runningTimers.forEach { timer ->
                if (countDownTimers[timer.id] == null) {
                    EventBus.getDefault().post(
                        TimerEvent.Start(
                            timerId = timer.id!!,
                            duration = (timer.state as TimerState.Running).tick
                        )
                    )
                }
            }
        }
        if (Stopwatch.state == State.RUNNING) {
            EventBus.getDefault().post(StopwatchStopService)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: TimerEvent.Reset) {
        updateTimerState(event.timerId, TimerState.Idle)
        countDownTimers[event.timerId]?.cancel()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: TimerEvent.Delete) {
        countDownTimers[event.timerId]?.cancel()
        timerHelper.deleteTimer(event.timerId) {
            EventBus.getDefault().post(TimerEvent.Refresh)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: TimerEvent.Start) {
        val countDownTimer = object : CountDownTimer(event.duration, 1000) {
            override fun onTick(tick: Long) {
                updateTimerState(event.timerId, TimerState.Running(event.duration, tick))
            }

            override fun onFinish() {
                EventBus.getDefault().post(TimerEvent.Finish(event.timerId, event.duration))
                EventBus.getDefault().post(TimerStopService)
            }
        }.start()
        countDownTimers[event.timerId] = countDownTimer
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: TimerEvent.Finish) {
        timerHelper.getTimer(event.timerId) { timer ->
            val pendingIntent = getOpenTimerTabIntent(event.timerId)
            val notification = getTimerNotification(timer, pendingIntent)

            try {
                notificationManager.notify(event.timerId, notification)
            } catch (e: Exception) {
                showErrorToast(e)
            }

            updateTimerState(event.timerId, TimerState.Finished)
            Handler(Looper.getMainLooper()).postDelayed({
                hideNotification(event.timerId)
            }, config.timerMaxReminderSecs * 1000L)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: TimerEvent.Pause) {
        timerHelper.getTimer(event.timerId) { timer ->
            updateTimerState(
                event.timerId,
                TimerState.Paused(event.duration, (timer.state as TimerState.Running).tick)
            )
            countDownTimers[event.timerId]?.cancel()
        }
    }

    private fun updateTimerState(timerId: Int, state: TimerState) {
        timerHelper.getTimer(timerId) { timer ->
            val newTimer = Timer(
                id = timer.id,
                seconds = timer.seconds,
                state = state,
                vibrate = timer.vibrate,
                soundUri = timer.soundUri,
                soundTitle = timer.soundTitle,
                label = timer.label,
                createdAt = timer.createdAt,
                channelId = timer.channelId,
                oneShot = timer.oneShot
            )
                //timer.copy(state = state)
            if (newTimer.oneShot && state is TimerState.Idle) {
                timerHelper.deleteTimer(newTimer.id!!) {
                    EventBus.getDefault().post(TimerEvent.Refresh)
                }
            } else {
                timerHelper.insertOrUpdateTimer(newTimer) {
                    EventBus.getDefault().post(TimerEvent.Refresh)
                }
            }
        }
    }
}
