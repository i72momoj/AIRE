package org.i72momoj.aire.dialogs

import android.app.TimePickerDialog
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.RingtoneManager
import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.i72momoj.aire.R
import org.i72momoj.aire.activities.SimpleActivity
import org.i72momoj.aire.databinding.DialogEditAlarmBinding
import org.i72momoj.aire.extensions.checkAlarmsWithDeletedSoundUri
import org.i72momoj.aire.extensions.colorCompoundDrawable
import org.i72momoj.aire.extensions.config
import org.i72momoj.aire.extensions.dbHelper
import org.i72momoj.aire.extensions.getFormattedTime
import org.i72momoj.aire.extensions.handleFullScreenNotificationsPermission
import org.i72momoj.aire.extensions.rotateWeekdays
import org.i72momoj.aire.helpers.PICK_AUDIO_FILE_INTENT_ID
import org.i72momoj.aire.helpers.getCurrentDayMinutes
import org.i72momoj.aire.helpers.updateNonRecurringAlarmDay
import org.i72momoj.aire.models.Alarm
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.dialogs.SelectAlarmSoundDialog
import org.fossify.commons.extensions.addBit
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.getDefaultAlarmSound
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.getTimePickerDialogTheme
import org.fossify.commons.extensions.isDynamicTheme
import org.fossify.commons.extensions.removeBit
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.value
import org.fossify.commons.models.AlarmSound
import org.i72momoj.aire.helpers.CONFIRMAR_VOZ
import org.i72momoj.aire.helpers.EDITAR_ALARMA
import org.i72momoj.aire.helpers.INSERTAR_O_ACTUALIZAR_ALARMA
import org.i72momoj.aire.models.Interprete

// MODIFICADO -> restore, layout, interprete
class EditAlarmDialog(
    val activity: SimpleActivity,
    val alarm: Alarm,
    val onDismiss: () -> Unit = {},
    val restore: Boolean = true,
    val layout: Int = EDITAR_ALARMA,
    val interprete: Interprete? = null,
    val accion: Int = INSERTAR_O_ACTUALIZAR_ALARMA,
    val callback: (alarmId: Int) -> Unit,
) {
    private val binding = DialogEditAlarmBinding.inflate(activity.layoutInflater)
    private val textColor = activity.getProperTextColor()

    init {
        if (restore)
            restoreLastAlarm()

        updateAlarmTime()

        binding.apply {

            // Aplicamos modificamos el layout de acuerdo a quien invoca el Dialog
            if(layout == EDITAR_ALARMA)
                mensaje.height = 0
            else {
                val params = mensaje.layoutParams
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT

                mensaje.layoutParams = params
                mensaje.text = "¿Desea confirmar la creación?"
            }

            // Si la confirmación por voz está desactivada, eliminamos el espacio
            // reservado para el visualizer
            if(layout != CONFIRMAR_VOZ) {
                val layoutParams = visualizer.layoutParams
                layoutParams.height = 0
                visualizer.layoutParams = layoutParams
            }

            editAlarmTime.setOnClickListener {
                if (activity.isDynamicTheme()) {
                    val timeFormat = if (activity.config.use24HourFormat) {
                        TimeFormat.CLOCK_24H
                    } else {
                        TimeFormat.CLOCK_12H
                    }

                    val timePicker = MaterialTimePicker.Builder()
                        .setTimeFormat(timeFormat)
                        .setHour(alarm.timeInMinutes / 60)
                        .setMinute(alarm.timeInMinutes % 60)
                        .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                        .build()

                    timePicker.addOnPositiveButtonClickListener {
                        timePicked(timePicker.hour, timePicker.minute)
                    }

                    timePicker.show(activity.supportFragmentManager, "")
                } else {
                    TimePickerDialog(
                        root.context,
                        root.context.getTimePickerDialogTheme(),
                        timeSetListener,
                        alarm.timeInMinutes / 60,
                        alarm.timeInMinutes % 60,
                        activity.config.use24HourFormat
                    ).show()
                }
            }

            editAlarmSound.colorCompoundDrawable(textColor)
            editAlarmSound.text = alarm.soundTitle
            editAlarmSound.setOnClickListener {
                SelectAlarmSoundDialog(
                    activity = activity,
                    currentUri = alarm.soundUri,
                    audioStream = AudioManager.STREAM_ALARM,
                    pickAudioIntentId = PICK_AUDIO_FILE_INTENT_ID,
                    type = RingtoneManager.TYPE_ALARM,
                    loopAudio = true,
                    onAlarmPicked = {
                        if (it != null) {
                            updateSelectedAlarmSound(it)
                        }
                    },
                    onAlarmSoundDeleted = {
                        if (alarm.soundUri == it.uri) {
                            val defaultAlarm =
                                root.context.getDefaultAlarmSound(RingtoneManager.TYPE_ALARM)
                            updateSelectedAlarmSound(defaultAlarm)
                        }
                        activity.checkAlarmsWithDeletedSoundUri(it.uri)
                    })
            }

            editAlarmVibrateIcon.setColorFilter(textColor)
            editAlarmVibrate.isChecked = alarm.vibrate
            editAlarmVibrateHolder.setOnClickListener {
                editAlarmVibrate.toggle()
                alarm.vibrate = editAlarmVibrate.isChecked
            }

            editAlarmLabelImage.applyColorFilter(textColor)
            editAlarm.setText(alarm.label)

            val dayLetters =
                activity.resources.getStringArray(org.fossify.commons.R.array.week_day_letters)
                    .toList() as ArrayList<String>
            val dayIndexes = activity.rotateWeekdays(arrayListOf(0, 1, 2, 3, 4, 5, 6))

            dayIndexes.forEach {
                val bitmask = 1 shl it
                val day = activity.layoutInflater.inflate(
                    R.layout.alarm_day, editAlarmDaysHolder, false
                ) as TextView
                day.text = dayLetters[it]

                val isDayChecked = alarm.isRecurring() && alarm.days and bitmask != 0
                day.background = getProperDayDrawable(isDayChecked)

                day.setTextColor(if (isDayChecked) root.context.getProperBackgroundColor() else textColor)
                day.setOnClickListener {
                    if (!alarm.isRecurring()) {
                        alarm.days = 0
                    }

                    val selectDay = alarm.days and bitmask == 0
                    if (selectDay) {
                        alarm.days = alarm.days.addBit(bitmask)
                    } else {
                        alarm.days = alarm.days.removeBit(bitmask)
                    }
                    day.background = getProperDayDrawable(selectDay)
                    day.setTextColor(if (selectDay) root.context.getProperBackgroundColor() else textColor)
                    checkDaylessAlarm()
                }

                editAlarmDaysHolder.addView(day)
            }
        }

        // Si el asistente no está configurado para usar la confirmación por voz, mostramos
        // el diálogo con un botón de aceptar y rechazar
        if(layout != CONFIRMAR_VOZ) {
            activity.getAlertDialogBuilder()
                .setOnDismissListener { onDismiss() }
                .setPositiveButton(org.fossify.commons.R.string.ok, null)
                .setNegativeButton(org.fossify.commons.R.string.cancel, null)
                .apply {
                    activity.setupDialogStuff(binding.root, this) { alertDialog ->
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            if (!activity.config.wasAlarmWarningShown) {
                                ConfirmationDialog(
                                    activity = activity,
                                    messageId = org.fossify.commons.R.string.alarm_warning,
                                    positive = org.fossify.commons.R.string.ok,
                                    negative = 0
                                ) {
                                    activity.config.wasAlarmWarningShown = true
                                    it.performClick()
                                }

                                return@setOnClickListener
                            }

                            updateNonRecurringAlarmDay(alarm)

                            alarm.label = binding.editAlarm.value
                            alarm.isEnabled = true
                            alarm.oneShot = false

                            var alarmId = alarm.id
                            activity.handleFullScreenNotificationsPermission { granted ->
                                if (granted) {
                                    if(accion == INSERTAR_O_ACTUALIZAR_ALARMA) {
                                        if (alarm.id == 0) {
                                            alarmId = activity.dbHelper.insertAlarm(alarm)
                                            if (alarmId == -1) {
                                                activity.toast(org.fossify.commons.R.string.unknown_error_occurred)
                                            }
                                        } else {
                                            if (!activity.dbHelper.updateAlarm(alarm)) {
                                                activity.toast(org.fossify.commons.R.string.unknown_error_occurred)
                                            }
                                        }

                                        activity.config.alarmLastConfig = alarm
                                    }
                                    else {
                                        val alarmas  = arrayListOf(alarm)
                                        activity.dbHelper.deleteAlarms(alarmas)
                                    }

                                    callback(alarmId)
                                    alertDialog.dismiss()
                                }
                            }
                        }
                    }
                }
        }
        // En caso contrario, eliminamos los botones y preparamos el asistente para
        // obtener una respuesta
        else {
            activity.getAlertDialogBuilder()
                .setOnDismissListener { interprete?.stop() }
                .apply {
                    activity.setupDialogStuff(binding.root, this) { alertDialog ->
                        binding.apply{
                            var respuesta: String? = ""
                            CoroutineScope(Dispatchers.IO).launch {
                                respuesta = interprete?.grabar(visualizer)

                                if(respuesta in listOf<String>("si", "sí", "venga", "dale papi")) {
                                    updateNonRecurringAlarmDay(alarm)

                                    alarm.label = binding.editAlarm.value
                                    alarm.isEnabled = true
                                    alarm.oneShot = false

                                    var alarmId = alarm.id
                                    activity.handleFullScreenNotificationsPermission { granted ->
                                        if (granted) {
                                            if(accion == INSERTAR_O_ACTUALIZAR_ALARMA) {
                                                if (alarm.id == 0) {
                                                    alarmId = activity.dbHelper.insertAlarm(alarm)
                                                    if (alarmId == -1) {
                                                        activity.toast(org.fossify.commons.R.string.unknown_error_occurred)
                                                    }
                                                } else {
                                                    if (!activity.dbHelper.updateAlarm(alarm)) {
                                                        activity.toast(org.fossify.commons.R.string.unknown_error_occurred)
                                                    }
                                                }

                                                activity.config.alarmLastConfig = alarm
                                            }
                                            else {
                                                val alarmas  = arrayListOf(alarm)
                                                activity.dbHelper.deleteAlarms(alarmas)
                                            }
                                            callback(alarmId)
                                            alertDialog.dismiss()
                                        }
                                    }
                                }
                                else
                                    alertDialog.dismiss()
                            }
                        }
                    }
                }
        }
    }



    private fun restoreLastAlarm() {
        if (alarm.id == 0) {
            activity.config.alarmLastConfig?.let { lastConfig ->
                alarm.label = lastConfig.label
                alarm.days = lastConfig.days
                alarm.soundTitle = lastConfig.soundTitle
                alarm.soundUri = lastConfig.soundUri
                alarm.timeInMinutes = lastConfig.timeInMinutes
                alarm.vibrate = lastConfig.vibrate
            }
        }
    }

    private val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
        timePicked(hourOfDay, minute)
    }

    private fun timePicked(hours: Int, minutes: Int) {
        alarm.timeInMinutes = hours * 60 + minutes
        updateAlarmTime()
    }

    private fun updateAlarmTime() {
        binding.editAlarmTime.text = activity.getFormattedTime(
            passedSeconds = alarm.timeInMinutes * 60,
            showSeconds = false,
            makeAmPmSmaller = true
        )
        checkDaylessAlarm()
    }

    private fun checkDaylessAlarm() {
        if (!alarm.isRecurring()) {
            val textId = if (alarm.timeInMinutes > getCurrentDayMinutes()) {
                org.fossify.commons.R.string.today
            } else {
                org.fossify.commons.R.string.tomorrow
            }

            binding.editAlarmDaylessLabel.text = "(${activity.getString(textId)})"
        }
        binding.editAlarmDaylessLabel.beVisibleIf(!alarm.isRecurring())
    }

    private fun getProperDayDrawable(selected: Boolean): Drawable {
        val drawableId = if (selected) {
            R.drawable.circle_background_filled
        } else {
            R.drawable.circle_background_stroke
        }

        val drawable = activity.resources.getDrawable(drawableId)
        drawable.applyColorFilter(textColor)
        return drawable
    }

    fun updateSelectedAlarmSound(alarmSound: AlarmSound) {
        alarm.soundTitle = alarmSound.title
        alarm.soundUri = alarmSound.uri
        binding.editAlarmSound.text = alarmSound.title
    }
}
