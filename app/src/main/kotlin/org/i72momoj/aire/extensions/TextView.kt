package org.i72momoj.aire.extensions

import android.widget.TextView
import org.fossify.commons.extensions.applyColorFilter

fun TextView.colorCompoundDrawable(color: Int) {
    compoundDrawables.filterNotNull().forEach { drawable ->
        drawable.applyColorFilter(color)
        setCompoundDrawables(drawable, null, null, null)
    }
}
