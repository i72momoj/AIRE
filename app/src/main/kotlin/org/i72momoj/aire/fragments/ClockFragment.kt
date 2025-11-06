package org.i72momoj.aire.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.i72momoj.aire.activities.SimpleActivity
import org.i72momoj.aire.adapters.TimeZonesAdapter
import org.i72momoj.aire.databinding.FragmentClockBinding
import org.i72momoj.aire.dialogs.AddTimeZonesDialog
import org.i72momoj.aire.dialogs.EditTimeZoneDialog
import org.i72momoj.aire.extensions.colorCompoundDrawable
import org.i72momoj.aire.extensions.config
import org.i72momoj.aire.extensions.getAllTimeZonesModified
import org.i72momoj.aire.extensions.getClosestEnabledAlarmString
import org.i72momoj.aire.extensions.getFormattedDate
import org.i72momoj.aire.helpers.FORMAT_12H_WITH_SECONDS
import org.i72momoj.aire.helpers.FORMAT_24H_WITH_SECONDS
import org.i72momoj.aire.helpers.getPassedSeconds
import org.i72momoj.aire.models.MyTimeZone
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.updateTextColors
import java.util.Calendar

open class ClockFragment : Fragment() {
    protected val ONE_SECOND = 1000L
    protected var passedSeconds = 0
    protected var calendar = Calendar.getInstance()
    protected val updateHandler = Handler(Looper.getMainLooper())

    protected open lateinit var binding: FragmentClockBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentClockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        setupDateTime()

        val safeContext = context ?: return
        binding.clockDate.setTextColor(safeContext.getProperTextColor())
    }

    override fun onPause() {
        super.onPause()
        updateHandler.removeCallbacksAndMessages(null)
    }

    protected fun setupDateTime() {
        calendar = Calendar.getInstance()
        passedSeconds = getPassedSeconds()
        updateCurrentTime()
        updateDate()
        updateAlarm()
        setupViews()
    }

    protected open fun setupViews() {
        val safeContext = context ?: return
        binding.apply {
            safeContext.updateTextColors(clockFragment)
            clockTime.setTextColor(safeContext.getProperTextColor())
            val clockFormat = if (safeContext.config.use24HourFormat) {
                FORMAT_24H_WITH_SECONDS
            } else {
                FORMAT_12H_WITH_SECONDS
            }

            clockTime.format24Hour = clockFormat
            clockTime.format12Hour = clockFormat
            /*clockFab.setOnClickListener {
                fabClicked()
            }*/

            updateTimeZones()
        }
    }

    protected fun updateCurrentTime() {
        val hours = (passedSeconds / 3600) % 24
        val minutes = (passedSeconds / 60) % 60
        val seconds = passedSeconds % 60
        if (seconds == 0) {
            if (hours == 0 && minutes == 0) {
                updateDate()
            }

            (binding.timeZonesList.adapter as? TimeZonesAdapter)?.updateTimes()
        }

        updateHandler.postDelayed({
            passedSeconds++
            updateCurrentTime()
        }, ONE_SECOND)
    }

    protected fun updateDate() {
        calendar = Calendar.getInstance()
        val formattedDate = requireContext().getFormattedDate(calendar)
        (binding.timeZonesList.adapter as? TimeZonesAdapter)?.todayDateString = formattedDate
    }

    fun updateAlarm() {
        val safeContext = context ?: return
        safeContext.getClosestEnabledAlarmString { nextAlarm ->
            binding.apply {
                clockAlarm.beVisibleIf(nextAlarm.isNotEmpty())
                clockAlarm.text = nextAlarm
                clockAlarm.colorCompoundDrawable(safeContext.getProperTextColor())
            }
        }
    }

    protected fun updateTimeZones() {
        val safeContext = activity as? SimpleActivity ?: return
        val selectedTimeZones = safeContext.config.selectedTimeZones
        binding.timeZonesList.beVisibleIf(selectedTimeZones.isNotEmpty())
        if (selectedTimeZones.isEmpty()) {
            return
        }

        val selectedTimeZoneIDs = selectedTimeZones.map { it.toInt() }
        val timeZones = safeContext.getAllTimeZonesModified()
            .filter { selectedTimeZoneIDs.contains(it.id) } as ArrayList<MyTimeZone>
        val currAdapter = binding.timeZonesList.adapter
        if (currAdapter == null) {
            TimeZonesAdapter(safeContext, timeZones, binding.timeZonesList) {
                EditTimeZoneDialog(safeContext, it as MyTimeZone) {
                    updateTimeZones()
                }
            }.apply {
                this@ClockFragment.binding.timeZonesList.adapter = this
            }
        } else {
            (currAdapter as TimeZonesAdapter).apply {
                updatePrimaryColor()
                updateBackgroundColor(safeContext.getProperBackgroundColor())
                updateTextColor(safeContext.getProperTextColor())
                updateItems(timeZones)
            }
        }
    }

    protected fun fabClicked() {
        val safeContext = activity as? SimpleActivity ?: return
        AddTimeZonesDialog(safeContext) {
            updateTimeZones()
        }
    }
}
