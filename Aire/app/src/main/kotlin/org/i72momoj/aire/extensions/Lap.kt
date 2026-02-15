package org.i72momoj.aire.extensions

import org.i72momoj.aire.helpers.STOPWATCH_LIVE_LAP_ID
import org.i72momoj.aire.models.Lap

fun Lap.isLive() = id == STOPWATCH_LIVE_LAP_ID
