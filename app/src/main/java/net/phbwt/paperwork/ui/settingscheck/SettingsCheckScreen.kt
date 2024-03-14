@file:OptIn(ExperimentalFoundationApi::class)

package net.phbwt.paperwork.ui.settingscheck

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import net.phbwt.paperwork.R
import net.phbwt.paperwork.ui.main.AppTransitions
import net.phbwt.paperwork.ui.theme.AppTheme

@Destination(style = AppTransitions::class)
@Composable
fun SettingsCheckScreen(
    vm: SettingsCheckVM = hiltViewModel(),
) {
    val data by vm.data.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.startChecks()
    }

    SettingsCheckContent(
        data,
        onStart = vm::startChecks,
        onStop = vm::stopChecks,
        onReset = vm::clearDataAndReloadDb,
    )
}

@Composable
fun SettingsCheckContent(
    data: SettingsCheckState,
    onStart: () -> Unit = {},
    onStop: () -> Unit = {},
    onReset: suspend () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    Surface(
        color = colors.background,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
        ) {

            val items = data.items
            LazyColumn(
                modifier = Modifier.weight(1f),
            ) {
                items(items.size, { idx -> idx }) { idx ->
                    ItemRow(
                        items[idx],
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItemPlacement(),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                Button(
                    onClick = { scope.launch { onReset() } },
                    enabled = !data.running && data.paramsOk,
                ) {
                    Text(stringResource(R.string.settingsCheck_reset))
                }
                Button(
                    onClick = { if (data.running) onStop() else onStart() },
                    modifier = Modifier
                        .animateContentSize(),
                ) {
                    val resId = if (data.running) R.string.settingsCheck_stop else R.string.settingsCheck_restart
                    Text(stringResource(resId))
                }
            }

            // handle the navigationbar
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
fun ItemRow(
    item: Check,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            val context = LocalContext.current
            val alpha = if (item.level == Level.None) .6f else 1f
            Text(
                text = item.desc.format(context),
                modifier = Modifier.alpha(alpha),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (item.msg != null) {
                Text(
                    text = item.msg.format(context),
                    modifier = Modifier.alpha(alpha),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (item.level != Level.None) {
            val color = if (item.level == Level.Error) MaterialTheme.colorScheme.error else LocalContentColor.current
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


//region preview

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AppTheme {
        SettingsCheckContent(
            data = SettingsCheckState(
                true,
                true,
                persistentListOf(
                    Check(Msg(R.string.check_base_url), Level.OK, Msg(R.string.check_no_network_response_2, "aa", "bb")),
                    Check(Msg(R.string.check_failure), Level.Error, Msg(R.string.check_no_network_response)),
                )
            )
        )
    }
}

//endregion

