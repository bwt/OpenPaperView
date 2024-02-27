package net.phbwt.paperwork.helper

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Immutable

private const val TAG = "MiscHelper"

fun Intent?.startActivitySafely(ctxt: Context) = this?.let {
    try {
        ctxt.startActivity(this)
    } catch (ex: Exception) {
        // TODO: show snackbar
        Log.w(TAG, "Could not start activity $this", ex)
    }
}

@Immutable
data class ComposeImmutableList<T>(
    private val internalList: List<T>
) : List<T> by internalList

fun <T> List<T>.toComposeImmutable(): ComposeImmutableList<T> = when (this) {
    is ComposeImmutableList -> this
    else -> ComposeImmutableList(this)
}