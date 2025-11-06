package org.i72momoj.aire.dialogs

import org.i72momoj.aire.activities.SimpleActivity
import org.i72momoj.aire.databinding.DialogEditTimeZoneBinding
import org.i72momoj.aire.extensions.config
import org.i72momoj.aire.extensions.getEditedTimeZonesMap
import org.i72momoj.aire.extensions.getModifiedTimeZoneTitle
import org.i72momoj.aire.helpers.EDITED_TIME_ZONE_SEPARATOR
import org.i72momoj.aire.helpers.getDefaultTimeZoneTitle
import org.i72momoj.aire.models.MyTimeZone
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.showKeyboard
import org.fossify.commons.extensions.value

class EditTimeZoneDialog(val activity: SimpleActivity, val myTimeZone: MyTimeZone, val callback: () -> Unit) {

    init {
        val binding = DialogEditTimeZoneBinding.inflate(activity.layoutInflater).apply {
            editTimeZoneTitle.setText(activity.getModifiedTimeZoneTitle(myTimeZone.id))
            editTimeZoneLabel.setText(getDefaultTimeZoneTitle(myTimeZone.id))
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok) { dialog, which -> dialogConfirmed(binding.editTimeZoneTitle.value) }
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->
                    alertDialog.showKeyboard(binding.editTimeZoneTitle)
                }
            }
    }

    private fun dialogConfirmed(newTitle: String) {
        val editedTitlesMap = activity.getEditedTimeZonesMap()

        if (newTitle.isEmpty()) {
            editedTitlesMap.remove(myTimeZone.id)
        } else {
            editedTitlesMap[myTimeZone.id] = newTitle
        }

        val newTitlesSet = HashSet<String>()
        for ((key, value) in editedTitlesMap) {
            newTitlesSet.add("$key$EDITED_TIME_ZONE_SEPARATOR$value")
        }

        activity.config.editedTimeZoneTitles = newTitlesSet
        callback()
    }
}
