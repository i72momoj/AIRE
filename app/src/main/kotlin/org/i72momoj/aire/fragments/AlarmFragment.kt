package org.i72momoj.aire.fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import org.i72momoj.aire.activities.MainActivity
import org.i72momoj.aire.activities.SimpleActivity
import org.i72momoj.aire.adapters.AlarmsAdapter
import org.i72momoj.aire.databinding.FragmentAlarmBinding
import org.i72momoj.aire.dialogs.ChangeAlarmSortDialog
import org.i72momoj.aire.dialogs.EditAlarmDialog
import org.i72momoj.aire.extensions.alarmController
import org.i72momoj.aire.extensions.cancelAlarmClock
import org.i72momoj.aire.extensions.config
import org.i72momoj.aire.extensions.createNewAlarm
import org.i72momoj.aire.extensions.dbHelper
import org.i72momoj.aire.extensions.firstDayOrder
import org.i72momoj.aire.extensions.handleFullScreenNotificationsPermission
import org.i72momoj.aire.extensions.updateWidgets
import org.i72momoj.aire.helpers.DEFAULT_ALARM_MINUTES
import org.i72momoj.aire.helpers.EDITAR_ALARMA
import org.i72momoj.aire.helpers.SORT_BY_ALARM_TIME
import org.i72momoj.aire.helpers.SORT_BY_DATE_AND_TIME
import org.i72momoj.aire.helpers.getTomorrowBit
import org.i72momoj.aire.interfaces.ToggleAlarmInterface
import org.i72momoj.aire.models.Alarm
import org.i72momoj.aire.models.AlarmEvent
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.helpers.SORT_BY_CUSTOM
import org.fossify.commons.helpers.SORT_BY_DATE_CREATED
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.models.AlarmSound
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.i72momoj.aire.models.Interprete


open class AlarmFragment : Fragment(), ToggleAlarmInterface {
    private var alarms = ArrayList<Alarm>()
    private var currentEditAlarmDialog: EditAlarmDialog? = null
    private lateinit var binding: FragmentAlarmBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAlarmBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onResume() {
        super.onResume()
        setupViews()
    }

    fun showSortingDialog() {
        ChangeAlarmSortDialog(activity as SimpleActivity) {
            setupAlarms()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun setupViews() {
        binding.apply {
            requireContext().updateTextColors(alarmFragment)

            // El usuario pulsa sobre el botón de crear alarma
            alarmFab.setOnClickListener {
                // Creamos una nueva alarma y, por defecto, se le ponen 8 horas y la hacemos
                // no periódica
                val newAlarm = root.context.createNewAlarm(DEFAULT_ALARM_MINUTES, 0)

                // Por defecto establece el día a mañana
                newAlarm.days = getTomorrowBit()
                openEditAlarm(newAlarm)
            }
        }
        setupAlarms()
    }

    private fun getSortedAlarms(callback: (alarms: ArrayList<Alarm>) -> Unit) {
        val safeContext = context ?: return
        ensureBackgroundThread {
            var newAlarms = context?.dbHelper?.getAlarms()
            if (newAlarms == null) {
                activity?.runOnUiThread {
                    callback(arrayListOf())
                }
                return@ensureBackgroundThread
            }

            when (safeContext.config.alarmSort) {
                SORT_BY_ALARM_TIME -> newAlarms.sortBy { it.timeInMinutes }
                SORT_BY_DATE_CREATED -> newAlarms.sortBy { it.id }
                SORT_BY_DATE_AND_TIME -> newAlarms.sortWith(compareBy<Alarm> {
                    safeContext.firstDayOrder(it.days)
                }.thenBy {
                    it.timeInMinutes
                })

                SORT_BY_CUSTOM -> {
                    val customAlarmsSortOrderString = activity?.config?.alarmsCustomSorting
                    if (customAlarmsSortOrderString == "") {
                        newAlarms.sortBy { it.id }
                    } else {
                        val customAlarmsSortOrder: List<Int> =
                            customAlarmsSortOrderString?.split(", ")?.map { it.toInt() }!!
                        val alarmsIdValueMap = newAlarms.associateBy { it.id }

                        val sortedAlarms: ArrayList<Alarm> = ArrayList()
                        customAlarmsSortOrder.map { id ->
                            if (alarmsIdValueMap[id] != null) {
                                sortedAlarms.add(alarmsIdValueMap[id] as Alarm)
                            }
                        }

                        newAlarms =
                            (sortedAlarms + newAlarms.filter { it !in sortedAlarms }) as ArrayList<Alarm>
                    }
                }
            }

            activity?.runOnUiThread {
                callback(newAlarms)
            }
        }
    }

    protected fun setupAlarms() {
        getSortedAlarms { sortedAlarms ->
            alarms = sortedAlarms
            val safeActivity = activity as? SimpleActivity ?: return@getSortedAlarms
            var currAdapter = binding.alarmsList.adapter as? AlarmsAdapter
            if (currAdapter == null) {
                currAdapter = AlarmsAdapter(
                    activity = safeActivity,
                    alarms = alarms,
                    toggleAlarmInterface = this,
                    recyclerView = binding.alarmsList
                ) {
                    openEditAlarm(it as Alarm)
                }.apply {
                    binding.alarmsList.adapter = this
                }
            } else {
                currAdapter.apply {
                    updatePrimaryColor()
                    updateBackgroundColor(safeActivity.getProperBackgroundColor())
                    updateTextColor(safeActivity.getProperTextColor())
                    updateItems(alarms)
                }
            }
            binding.alarmsPlaceholder.beVisibleIf(alarms.isEmpty())
        }
    }

    // MODIFICADO -> +restore, +layout, +interprete
    protected fun openEditAlarm(
        alarm: Alarm,
        restore: Boolean = true,
        layout: Int = EDITAR_ALARMA,
        interprete: Interprete? = null,
        callback: (Int) -> Unit = {}
    ) {
        currentEditAlarmDialog = EditAlarmDialog(
            activity as SimpleActivity,
            alarm, restore = restore,
            layout = layout,
            interprete = interprete
        ) {
            alarm.id = it
            currentEditAlarmDialog = null
            setupAlarms()
            checkAlarmState(alarm)

            // Comprobamos que no han habido errores al añadir la alarma a la BD
            if (it > -1)
                activity?.toast("Alarma añadida con éxito")
        }
    }

    override fun alarmToggled(id: Int, isEnabled: Boolean) {
        (activity as SimpleActivity).handleFullScreenNotificationsPermission { granted ->
            if (granted) {
                if (requireContext().dbHelper.updateAlarmEnabledState(id, isEnabled)) {
                    val alarm = alarms.firstOrNull { it.id == id }
                        ?: return@handleFullScreenNotificationsPermission
                    alarm.isEnabled = isEnabled
                    checkAlarmState(alarm)
                    if (!alarm.isEnabled && alarm.oneShot) {
                        requireContext().dbHelper.deleteAlarms(arrayListOf(alarm))
                        setupAlarms()
                    }
                    
                } else {
                    requireActivity().toast(org.fossify.commons.R.string.unknown_error_occurred)
                }
                requireContext().updateWidgets()
            } else {
                setupAlarms()
            }
        }
    }

    protected fun checkAlarmState(alarm: Alarm) {
        val activity = activity as? MainActivity ?: return
        if (alarm.isEnabled) {
            activity.alarmController.scheduleNextOccurrence(alarm = alarm, showToasts = true)
        } else {
            activity.cancelAlarmClock(alarm)
        }
        activity.updateClockTabAlarm()
    }

    fun updateAlarmSound(alarmSound: AlarmSound) {
        currentEditAlarmDialog?.updateSelectedAlarmSound(alarmSound)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(@Suppress("unused") event: AlarmEvent.Refresh) {
        setupAlarms()
    }
}
