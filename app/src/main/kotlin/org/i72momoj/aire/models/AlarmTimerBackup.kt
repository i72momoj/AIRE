package org.i72momoj.aire.models

import androidx.annotation.Keep

@Keep
@kotlinx.serialization.Serializable
data class AlarmTimerBackup(
    val alarms: List<Alarm>,
    val timers: List<Timer>,
)
