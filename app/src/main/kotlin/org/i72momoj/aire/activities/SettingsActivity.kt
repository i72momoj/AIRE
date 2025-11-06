package org.i72momoj.aire.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import org.i72momoj.aire.activities.SimpleActivity
import org.i72momoj.aire.activities.WidgetDigitalConfigureActivity
import org.i72momoj.aire.databinding.ActivitySettingsBinding
import org.i72momoj.aire.dialogs.ExportDataDialog
import org.i72momoj.aire.extensions.config
import org.i72momoj.aire.extensions.dbHelper
import org.i72momoj.aire.extensions.timerDb
import org.i72momoj.aire.extensions.updateWidgets
import org.i72momoj.aire.helpers.DEFAULT_MAX_ALARM_REMINDER_SECS
import org.i72momoj.aire.helpers.DEFAULT_MAX_TIMER_REMINDER_SECS
import org.i72momoj.aire.helpers.EXPORT_BACKUP_MIME_TYPE
import org.i72momoj.aire.helpers.ExportHelper
import org.i72momoj.aire.helpers.IMPORT_BACKUP_MIME_TYPES
import org.i72momoj.aire.helpers.ImportHelper
import org.i72momoj.aire.helpers.TAB_ALARM
import org.i72momoj.aire.helpers.TAB_CLOCK
import org.i72momoj.aire.helpers.TAB_STOPWATCH
import org.i72momoj.aire.helpers.TAB_TIMER
import org.i72momoj.aire.helpers.TimerHelper
import org.i72momoj.aire.models.AlarmTimerBackup
import org.fossify.commons.R
import org.fossify.commons.dialogs.RadioGroupDialog
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.formatMinutesToTimeString
import org.fossify.commons.extensions.formatSecondsToTimeString
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.showPickSecondsDialog
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.ExportResult
import org.fossify.commons.helpers.IS_CUSTOMIZING_COLORS
import org.fossify.commons.helpers.MINUTE_SECONDS
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.models.RadioItem
import java.time.DayOfWeek

open class SettingsActivity : SimpleActivity() {
    protected open val binding: ActivitySettingsBinding by viewBinding(ActivitySettingsBinding::inflate)
    private val exportActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument(EXPORT_BACKUP_MIME_TYPE)) { uri ->
            if (uri == null) return@registerForActivityResult
            ensureBackgroundThread {
                exportData(uri)
            }
        }

    private val importActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@registerForActivityResult
            ensureBackgroundThread {
                importData(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        updateMaterialActivityViews(
            mainCoordinatorLayout = binding.settingsCoordinator,
            nestedView = binding.settingsHolder,
            useTransparentNavigation = true,
            useTopSearchMenu = false
        )
        setupMaterialScrollListener(binding.settingsNestedScrollview, binding.settingsToolbar)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.settingsToolbar, NavigationIcon.Arrow)

        setupCustomizeColors()
        //setupUseEnglish()
        //setupLanguage()
        setupHourFormat()
        //setupDefaultTab()
        setupPreventPhoneFromSleeping()
        setupStartWeekOn()
        setupAlarmMaxReminder()
        setupUseSameSnooze()
        setupSnoozeTime()
        setupTimerMaxReminder()
        setupIncreaseVolumeGradually()
        setupCustomizeWidgetColors()
        setupExportData()
        setupImportData()
        updateTextColors(binding.settingsHolder)

        arrayOf(
            binding.settingsColorCustomizationSectionLabel,
            binding.settingsGeneralSettingsLabel,
            binding.settingsAlarmTabLabel,
            binding.settingsTimerTabLabel,
            binding.settingsMigratingLabel
        ).forEach {
            it.setTextColor(getProperPrimaryColor())
        }
    }

    private fun setupCustomizeColors() {
        binding.settingsColorCustomizationHolder.setOnClickListener {
            startCustomizationActivity()
        }
    }

    /*private fun setupUseEnglish() {
        binding.settingsUseEnglishHolder.beVisibleIf((config.wasUseEnglishToggled || Locale.getDefault().language != "en") && !isTiramisuPlus())
        binding.settingsUseEnglish.isChecked = config.useEnglish
        binding.settingsUseEnglishHolder.setOnClickListener {
            binding.settingsUseEnglish.toggle()
            config.useEnglish = binding.settingsUseEnglish.isChecked
            exitProcess(0)
        }
    }

    private fun setupLanguage() {
        binding.settingsLanguage.text = Locale.getDefault().displayLanguage
        if (isTiramisuPlus()) {
            binding.settingsLanguageHolder.beVisible()
            binding.settingsLanguageHolder.setOnClickListener {
                launchChangeAppLanguageIntent()
            }
        } else {
            binding.settingsLanguageHolder.beGone()
        }
    }

    private fun setupDefaultTab() {
        binding.settingsDefaultTab.text = getDefaultTabText()
        binding.settingsDefaultTabHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(TAB_CLOCK, getString(R.string.clock)),
                RadioItem(TAB_ALARM, getString(org.fossify.commons.R.string.alarm)),
                RadioItem(TAB_STOPWATCH, getString(R.string.stopwatch)),
                RadioItem(TAB_TIMER, getString(R.string.timer)),
                RadioItem(TAB_LAST_USED, getString(org.fossify.commons.R.string.last_used_tab))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.defaultTab) {
                config.defaultTab = it as Int
                binding.settingsDefaultTab.text = getDefaultTabText()
            }
        }
    }*/

    private fun setupHourFormat() = binding.apply {
        settingsHourFormat.isChecked = config.use24HourFormat
        settingsHourFormatHolder.setOnClickListener {
            settingsHourFormat.toggle()
            config.use24HourFormat = settingsHourFormat.isChecked
            updateWidgets()
        }
    }

    private fun getDefaultTabText() = getString(
        when (config.defaultTab) {
            TAB_CLOCK -> org.i72momoj.aire.R.string.clock
            TAB_ALARM -> R.string.alarm
            TAB_STOPWATCH -> org.i72momoj.aire.R.string.stopwatch
            TAB_TIMER -> org.i72momoj.aire.R.string.timer
            else -> R.string.last_used_tab
        }
    )

    private fun setupPreventPhoneFromSleeping() {
        binding.settingsPreventPhoneFromSleeping.isChecked = config.preventPhoneFromSleeping
        binding.settingsPreventPhoneFromSleepingHolder.setOnClickListener {
            binding.settingsPreventPhoneFromSleeping.toggle()
            config.preventPhoneFromSleeping = binding.settingsPreventPhoneFromSleeping.isChecked
        }
    }

    private fun setupStartWeekOn() {
        binding.settingsStartWeekOn.text = resources.getStringArray(
            R.array.week_days
        )[config.firstDayOfWeek - 1]

        val weekDays = arrayListOf(
            RadioItem(DayOfWeek.SUNDAY.value, getString(R.string.sunday)),
            RadioItem(DayOfWeek.MONDAY.value, getString(R.string.monday)),
            RadioItem(DayOfWeek.TUESDAY.value, getString(R.string.tuesday)),
            RadioItem(DayOfWeek.WEDNESDAY.value, getString(R.string.wednesday)),
            RadioItem(DayOfWeek.THURSDAY.value, getString(R.string.thursday)),
            RadioItem(DayOfWeek.FRIDAY.value, getString(R.string.friday)),
            RadioItem(DayOfWeek.SATURDAY.value, getString(R.string.saturday)),
        )

        binding.settingsStartWeekOnHolder.setOnClickListener {
            RadioGroupDialog(
                activity = this,
                items = weekDays,
                checkedItemId = config.firstDayOfWeek
            ) { day ->
                val firstDayOfWeek = day as Int
                config.firstDayOfWeek = firstDayOfWeek
                binding.settingsStartWeekOn.text = resources.getStringArray(
                    R.array.week_days
                )[config.firstDayOfWeek - 1]
            }
        }
    }

    private fun setupAlarmMaxReminder() {
        updateAlarmMaxReminderText()
        binding.settingsAlarmMaxReminderHolder.setOnClickListener {
            showPickSecondsDialog(
                curSeconds = config.alarmMaxReminderSecs,
                isSnoozePicker = true,
                showSecondsAtCustomDialog = true
            ) {
                config.alarmMaxReminderSecs = if (it != 0) it else DEFAULT_MAX_ALARM_REMINDER_SECS
                updateAlarmMaxReminderText()
            }
        }
    }

    private fun setupUseSameSnooze() {
        binding.settingsSnoozeTimeHolder.beVisibleIf(config.useSameSnooze)
        binding.settingsUseSameSnooze.isChecked = config.useSameSnooze
        binding.settingsUseSameSnoozeHolder.setOnClickListener {
            binding.settingsUseSameSnooze.toggle()
            config.useSameSnooze = binding.settingsUseSameSnooze.isChecked
            binding.settingsSnoozeTimeHolder.beVisibleIf(config.useSameSnooze)
        }
    }

    private fun setupSnoozeTime() {
        updateSnoozeText()
        binding.settingsSnoozeTimeHolder.setOnClickListener {
            showPickSecondsDialog(
                curSeconds = config.snoozeTime * MINUTE_SECONDS,
                isSnoozePicker = true
            ) {
                config.snoozeTime = it / MINUTE_SECONDS
                updateSnoozeText()
            }
        }
    }

    private fun setupTimerMaxReminder() {
        updateTimerMaxReminderText()
        binding.settingsTimerMaxReminderHolder.setOnClickListener {
            showPickSecondsDialog(
                curSeconds = config.timerMaxReminderSecs,
                isSnoozePicker = true,
                showSecondsAtCustomDialog = true
            ) {
                config.timerMaxReminderSecs = if (it != 0) it else DEFAULT_MAX_TIMER_REMINDER_SECS
                updateTimerMaxReminderText()
            }
        }
    }

    private fun setupIncreaseVolumeGradually() {
        binding.settingsIncreaseVolumeGradually.isChecked = config.increaseVolumeGradually
        binding.settingsIncreaseVolumeGraduallyHolder.setOnClickListener {
            binding.settingsIncreaseVolumeGradually.toggle()
            config.increaseVolumeGradually = binding.settingsIncreaseVolumeGradually.isChecked
        }
    }

    private fun updateSnoozeText() {
        binding.settingsSnoozeTime.text = formatMinutesToTimeString(config.snoozeTime)
    }

    private fun updateAlarmMaxReminderText() {
        binding.settingsAlarmMaxReminder.text =
            formatSecondsToTimeString(config.alarmMaxReminderSecs)
    }

    private fun updateTimerMaxReminderText() {
        binding.settingsTimerMaxReminder.text =
            formatSecondsToTimeString(config.timerMaxReminderSecs)
    }

    private fun setupCustomizeWidgetColors() {
        binding.settingsWidgetColorCustomizationHolder.setOnClickListener {
            Intent(this, WidgetDigitalConfigureActivity::class.java).apply {
                putExtra(IS_CUSTOMIZING_COLORS, true)
                startActivity(this)
            }
        }
    }

    private fun setupExportData() {
        binding.settingsExportDataHolder.setOnClickListener {
            tryExportData()
        }
    }

    private fun setupImportData() {
        binding.settingsImportDataHolder.setOnClickListener {
            tryImportData()
        }
    }

    private fun exportData(outputUri: Uri) {
        val alarms = dbHelper.getAlarms()
        val timers = timerDb.getTimers()
        if (alarms.isEmpty() && timers.isEmpty()) {
            toast(R.string.no_entries_for_exporting)
        } else {
            ExportHelper(this).exportData(
                backup = AlarmTimerBackup(alarms, timers),
                outputUri = outputUri,
            ) {
                toast(
                    when (it) {
                        ExportResult.EXPORT_OK -> R.string.exporting_successful
                        else -> R.string.exporting_failed
                    }
                )
            }
        }
    }

    private fun tryExportData() {
        ExportDataDialog(this, config.lastDataExportPath) { file ->
            try {
                exportActivityResultLauncher.launch(file.name)
            } catch (@Suppress("SwallowedException") _: ActivityNotFoundException) {
                toast(
                    id = R.string.system_service_disabled,
                    length = Toast.LENGTH_LONG
                )
            }
        }
    }

    private fun tryImportData() {
        try {
            importActivityResultLauncher.launch(IMPORT_BACKUP_MIME_TYPES.toTypedArray())
        } catch (@Suppress("SwallowedException") _: ActivityNotFoundException) {
            toast(R.string.system_service_disabled, Toast.LENGTH_LONG)
        }
    }

    private fun importData(uri: Uri) {
        val result = ImportHelper(
            context = this,
            dbHelper = dbHelper,
            timerHelper = TimerHelper(this)
        ).importData(uri)

        toast(
            when (result) {
                ImportHelper.ImportResult.IMPORT_OK -> R.string.importing_successful
                ImportHelper.ImportResult.IMPORT_INCOMPLETE -> R.string.no_new_entries_for_importing
                ImportHelper.ImportResult.IMPORT_FAIL -> R.string.no_items_found
            }
        )
    }
}
