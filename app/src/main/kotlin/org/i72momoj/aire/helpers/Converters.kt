package org.i72momoj.aire.helpers

import androidx.room.TypeConverter
import org.i72momoj.aire.extensions.gson.gson
import org.i72momoj.aire.models.StateWrapper
import org.i72momoj.aire.models.TimerState

class Converters {

    @TypeConverter
    fun jsonToTimerState(value: String?): TimerState {
        if (value.isNullOrEmpty()) return TimerState.Idle
        return try {
            gson.fromJson(value, StateWrapper::class.java).state
        } catch (e: Exception) {
            TimerState.Idle
        }
    }

    @TypeConverter
    fun timerStateToJson(state: TimerState) = gson.toJson(StateWrapper(state))
}
