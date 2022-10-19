package net.phbwt.paperwork.helper

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable


fun Long.fmtDtm(context: Context): String {
    return when {
        this <= 0 -> ""
        this > System.currentTimeMillis() - 8 * 3600 * 1000 ->
            DateUtils.formatDateTime(context, this, DateUtils.FORMAT_SHOW_TIME)
        else ->
            DateUtils.formatDateTime(context, this, DateUtils.FORMAT_SHOW_DATE)
    }
}

fun Long.fmtDtmSec(context: Context) = (this * 1000).fmtDtm(context)

fun Throwable?.desc(): String = if (this == null) {
    ""
} else {
    val cn = this::class.java.simpleName
    if (this.message.isNullOrBlank()) {
        cn
    } else {
        "$cn : ${this.message}"
    }
}


@Composable
fun TrailingClose(visible: Boolean, onClick: () -> Unit) = AnimatedVisibility(
    visible = visible,
    enter = fadeIn(),
    exit = fadeOut(),
) {
    IconButton(onClick = onClick) {
        Icon(Icons.Outlined.Close, null)
    }
}

