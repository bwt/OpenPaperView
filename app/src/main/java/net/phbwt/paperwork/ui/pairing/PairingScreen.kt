@file:OptIn(ExperimentalFoundationApi::class)

package net.phbwt.paperwork.ui.pairing

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.persistentListOf
import net.phbwt.paperwork.R
import net.phbwt.paperwork.data.entity.pairing.ConfigClient
import net.phbwt.paperwork.data.entity.pairing.ConfigServer
import net.phbwt.paperwork.data.entity.pairing.PairingConfig
import net.phbwt.paperwork.data.entity.pairing.QrCodeContent
import net.phbwt.paperwork.data.helper.PairingRunner
import net.phbwt.paperwork.ui.main.AppTransitions
import net.phbwt.paperwork.ui.main.Dest
import net.phbwt.paperwork.ui.main.WrappedScaffold
import net.phbwt.paperwork.ui.settingscheck.Check
import net.phbwt.paperwork.ui.settingscheck.ItemRow
import net.phbwt.paperwork.ui.settingscheck.Level
import net.phbwt.paperwork.ui.settingscheck.Msg
import net.phbwt.paperwork.ui.theme.AppTheme

data class PairingScreenArgs(
    val qrCode: QrCodeContent?,
)

@Destination(
    navArgsDelegate = PairingScreenArgs::class,
    style = AppTransitions::class,
)
@Composable
fun PairingScreen(
    navigator: DestinationsNavigator,
    snackbarHostState: SnackbarHostState,
    onNavigationIcon: (Boolean) -> Unit,
    vm: PairingVM = hiltViewModel(),
) {
    val data by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.startPairing()
    }

    PairingContent(
        data,
        onStart = { vm.startPairing(true) },
        onStop = { vm.stopPairing() },
        onStartSync = vm::startSync,
        snackbarHostState,
        onNavigationIcon,
    )
}

@Composable
fun PairingContent(
    uiState: PairingUiState,
    onStart: () -> Unit = {},
    onStop: () -> Unit = {},
    onStartSync: (Boolean, PairingRunner.PairingResult) -> Unit = { _, _ -> },
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigationIcon: (Boolean) -> Unit = {},
) = WrappedScaffold(
    snackbarHostState,
    onNavigationIcon,
    Dest.Pairing.labelRes,
    topLevel = false,
) { modifier ->

    Column(
        modifier = modifier
            .padding(8.dp),
    ) {

        val items = uiState.log
        LazyColumn(
            modifier = Modifier.weight(1f),
        ) {
            items(items.size, { idx -> idx }) { idx ->
                ItemRow(
                    items[idx],
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                    idx == 0,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            val pairingStatus = uiState.status

            Button(
                onClick = {
                    when (pairingStatus) {
                        Failed -> onStart()
                        Ongoing -> onStop()
                        is Succeeded -> onStartSync(false, pairingStatus.result)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                val resId = when (uiState.status) {
                    Failed -> R.string.pairing_restart
                    Ongoing -> R.string.pairing_stop
                    is Succeeded -> R.string.pairing_download_db
                }
                Text(stringResource(resId))
            }
            if (pairingStatus is Succeeded) {
                Button(
                    onClick = { onStartSync(true, pairingStatus.result) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.pairing_download_db_and_docs))
                }
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
        PairingContent(
            uiState = PairingUiState(
                Ongoing,
                persistentListOf(
                    Check(Msg(R.string.check_base_url), Level.OK, Msg(R.string.check_no_network_response_2, "aa", "bb")),
                    Check(Msg(R.string.check_failure), Level.Error, Msg(R.string.check_no_network_response)),
                )
            )
        )
    }
}

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Composable
fun SuccessPreview() {
    AppTheme {
        PairingContent(
            uiState = PairingUiState(
                Succeeded(
                    PairingRunner.PairingResult(
                        "the adresse",
                        PairingConfig(ConfigClient("a certificate", "the key"), ConfigServer("the server cert"))
                    )
                ),
                persistentListOf(
                    Check(Msg(R.string.check_base_url), Level.OK, Msg(R.string.check_no_network_response_2, "aa", "bb")),
                    Check(Msg(R.string.check_failure), Level.Error, Msg(R.string.check_no_network_response)),
                )
            )
        )
    }
}

//endregion

