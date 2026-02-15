package org.i72momoj.aire.extensions

import android.util.Log
import org.i72momoj.aire.BuildConfig

fun <A> A.log(tag: String) = apply { if (BuildConfig.DEBUG) Log.wtf(tag, this.toString()) }
fun <A> A.log(first: String, tag: String) = apply { if (BuildConfig.DEBUG) Log.wtf(tag, first) }
