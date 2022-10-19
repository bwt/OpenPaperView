package net.phbwt.paperwork.helper

import android.content.Context
import android.content.Intent
import android.util.Log

private const val TAG = "MiscHelper"

fun Intent?.startActivitySafely(ctxt: Context) = this?.let {
    try {
        ctxt.startActivity(this)
    } catch (ex: Exception) {
        // TODO: show snackbar
        Log.w(TAG, "Could not start activity $this", ex)
    }
}