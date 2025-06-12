package net.phbwt.paperwork.ui.settingscheck

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import net.phbwt.paperwork.R
import net.phbwt.paperwork.helper.desc
import net.phbwt.paperwork.ui.theme.caution
import net.phbwt.paperwork.ui.theme.success

/**
 * The common parts between the SettingsCheck and the Pairing screens.
 */

@Composable
fun ItemRow(
    item: Check,
    modifier: Modifier = Modifier,
    isFirst: Boolean = false,
) {
    Row(
        modifier = if (isFirst || item.level != Level.Title) modifier.padding(vertical = 8.dp)
        else modifier.padding(top = 24.dp, bottom = 8.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            val context = LocalContext.current
            val alpha = if (item.level == Level.None) .6f else 1f
            Text(
                text = item.desc.format(context),
                modifier = Modifier.alpha(alpha),
                style = if (item.level == Level.Title) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                color = if (item.level == Level.Title) MaterialTheme.colorScheme.primary else LocalContentColor.current,
            )
            if (item.msg != null) {
                Text(
                    text = item.msg.format(context),
                    modifier = Modifier.alpha(alpha),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (item.level !in listOf(Level.None, Level.Title)) {
            val color = when (item.level) {
                Level.OK -> MaterialTheme.colorScheme.success
                Level.Warn -> MaterialTheme.colorScheme.caution
                Level.Error -> MaterialTheme.colorScheme.error
                else -> LocalContentColor.current
            }

            Icon(
                imageVector = when (item.level) {
                    Level.OK -> Icons.Outlined.Check
                    Level.Warn -> Icons.Outlined.Warning
                    Level.Error -> Icons.Outlined.Error
                    else -> Icons.Outlined.Error
                },
                contentDescription = null,
                tint = color,
            )
        }
    }
}

@Immutable
data class Check(
    val desc: Msg,
    val level: Level,
    val msg: Msg?,
)

@Immutable
class Msg(
    @StringRes
    val res: Int,
    vararg val args: String,
) {
    constructor(ex: Throwable?) : this(R.string.check_exception_1, ex.desc())
    constructor(@StringRes res: Int, v: Int) : this(res, v.toString())

    fun format(ctxt: Context): String = try {
        ctxt.getString(res, *args)
    } catch (ex: Exception) {
        val msg = "Failed to format the message '${ctxt.getString(res)}' : ${ex.message}"
        Log.e(TAG, msg, ex)
        msg
    }
}

private const val TAG = "CommonChecks"

enum class Level { OK, Warn, Error, None, Title }

