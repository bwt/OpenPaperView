@file:OptIn(ExperimentalFoundationApi::class)

package net.phbwt.paperwork.ui.settingscheck

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import net.phbwt.paperwork.R
import net.phbwt.paperwork.ui.main.AppTransitions
import net.phbwt.paperwork.ui.main.Dest
import net.phbwt.paperwork.ui.main.WrappedScaffold
import net.phbwt.paperwork.ui.theme.AppTheme

@Destination<RootGraph>(style = AppTransitions::class)
@Composable
fun SettingsCheckScreen(
    snackbarHostState: SnackbarHostState,
    onNavigationIcon: (Boolean) -> Unit,
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
        snackbarHostState,
        onNavigationIcon,
    )
}

@Composable
fun SettingsCheckContent(
    data: SettingsCheckState,
    onStart: () -> Unit = {},
    onStop: () -> Unit = {},
    onReset: suspend () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigationIcon: (Boolean) -> Unit = {},
) = WrappedScaffold(
    snackbarHostState,
    onNavigationIcon,
    Dest.SettingsCheck.labelRes,
    topLevel = false,
) { modifier ->
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .padding(8.dp),
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
                        .animateItem(),
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
                Text(stringResource(R.string.check_reset))
            }
            Button(
                onClick = { if (data.running) onStop() else onStart() },
                modifier = Modifier
                    .animateContentSize(),
            ) {
                val resId = if (data.running) R.string.check_stop else R.string.check_restart
                Text(stringResource(resId))
            }
        }

        // edge2edge : bottom
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
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

