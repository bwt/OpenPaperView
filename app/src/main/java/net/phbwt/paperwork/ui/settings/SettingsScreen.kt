@file:OptIn(ExperimentalMaterial3Api::class)

package net.phbwt.paperwork.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import net.phbwt.paperwork.R
import net.phbwt.paperwork.ui.main.Dest
import net.phbwt.paperwork.ui.theme.AppTheme

@Composable
fun SettingsScreen(
    navController: NavController,
    vm: SettingsVM = hiltViewModel(),
) {
    val data by vm.data.collectAsStateWithLifecycle(SettingsData())

    // start activity for client certificate
    val launcherC = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        it?.let {
            vm.updateClientPem(it)
        }
    }

    // start activity for server CA
    val launcherS = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        it?.let {
            vm.updateServerCa(it)
        }
    }

    SettingsContent(
        data = data,
        onBaseUrlChanged = { vm.updateBaseUrl(it) },
        onClientPemChanged = { vm.updateClientPem(it) },
        onServerCaChanged = { vm.updateServerCa(it) },
        onImportClientPEM = {
            // application/x-pem-file does not work
            // application/x-x509-ca-cert does ??
            launcherC.launch("application/*")
        },
        onImportServerCA = { launcherS.launch("application/*") },
        onCheck = { navController.navigate(Dest.SettingsCheck.topRoute) },
    )
}

@Composable
fun SettingsContent(
    data: SettingsData,
    onBaseUrlChanged: (String) -> Unit = {},
    onClientPemChanged: (String) -> Unit = {},
    onServerCaChanged: (String) -> Unit = {},
    onImportClientPEM: () -> Unit = {},
    onImportServerCA: () -> Unit = {},
    onCheck: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme

    Surface(color = colors.background) {
        Column(
            modifier = Modifier
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {

                PrefItem(
                    data.baseUrl,
                    onBaseUrlChanged,
                    KeyboardType.Uri,
                    false,
                    true,
                    R.string.settings_baseUrl_label,
                    R.string.settings_baseUrl_hint,
                )
                Spacer(modifier = Modifier.height(24.dp))
                PrefTextItem(
                    data.clientPem,
                    onClientPemChanged,
                    onImportClientPEM,
                    R.string.settings_clientPem_label,
                    R.string.settings_clientPem_hint,
                )
                Spacer(modifier = Modifier.height(24.dp))
                PrefTextItem(
                    data.serverCa,
                    onServerCaChanged,
                    onImportServerCA,
                    R.string.settings_serverCa_label,
                    R.string.settings_serverCa_hint,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = onCheck,
                ) {
                    Text(stringResource(R.string.settings_check))
                }
            }

            // handle the navigationbar
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
fun PrefItem(
    data: SettingItem,
    onValueChanged: (String) -> Unit = {},
    type: KeyboardType,
    readOnly: Boolean,
    singleLine: Boolean,
    @StringRes labelRes: Int,
    @StringRes hintRes: Int,
) {
    val txt = when {
        data.value.isNotBlank() -> data.value
        !readOnly -> ""
        else -> stringResource(hintRes)
    }

    OutlinedTextField(
        value = txt,
        onValueChange = onValueChanged,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = type,
            autoCorrect = false,
            imeAction = ImeAction.Next,
        ),
        readOnly = readOnly,
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 6,
        placeholder = { Text(stringResource(hintRes)) },
        label = { Text(stringResource(labelRes)) },
        isError = data.hasError,
    )
    if (data.hasError) {
        Text(
            text = data.error,
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
fun PrefTextItem(
    data: SettingItem,
    onValueChanged: (String) -> Unit = {},
    onImport: () -> Unit = {},
    @StringRes labelRes: Int,
    @StringRes hintRes: Int,
) {
    val clipboardManager = LocalClipboardManager.current

    PrefItem(data, onValueChanged, KeyboardType.Ascii, true, false, labelRes, hintRes)
    Row {
        IconButton(onClick = onImport) {
            Icon(Icons.Outlined.Download, null)
        }
        IconButton(
            onClick = {
                val t = clipboardManager.getText()?.text
                if (t != null) {
                    onValueChanged(t)
                }
            },
        ) {
            Icon(Icons.Outlined.ContentPaste, null)
        }
        IconButton(
            onClick = { onValueChanged("") },
        ) {
            Icon(Icons.Outlined.Clear, null)
        }

    }
}


//region preview

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AppTheme {
        SettingsContent(
            SettingsData(
                SettingItem("value 1", "An error"),
                SettingItem(""),
                SettingItem("Another value"),
            )
        )
    }
}

//endregion

