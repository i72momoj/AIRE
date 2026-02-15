package org.i72momoj.aire.models

sealed interface AlarmEvent {
    data object Refresh : AlarmEvent
    data class Stopped(val alarmId: Int) : AlarmEvent
}
