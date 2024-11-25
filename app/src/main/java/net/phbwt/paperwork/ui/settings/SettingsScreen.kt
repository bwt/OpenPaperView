@file:OptIn(ExperimentalMaterial3Api::class)

package net.phbwt.paperwork.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import net.phbwt.paperwork.R
import net.phbwt.paperwork.data.entity.db.LabelType
import net.phbwt.paperwork.data.entity.db.asFilter
import net.phbwt.paperwork.data.settings.LABELS_SEPARATOR
import net.phbwt.paperwork.ui.destinations.SettingsCheckScreenDestination
import net.phbwt.paperwork.ui.main.AppTransitions
import net.phbwt.paperwork.ui.main.Dest
import net.phbwt.paperwork.ui.main.WrappedScaffold
import net.phbwt.paperwork.ui.theme.AppTheme

@Destination(style = AppTransitions::class)
@Composable
fun SettingsScreen(
    navigator: DestinationsNavigator,
    snackbarHostState: SnackbarHostState,
    onNavigationIcon: (Boolean) -> Unit,
    vm: SettingsVM = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val baseUrl = vm.baseUrl
    val autoDowloadLabels = vm.autoDownloadLabels
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
        baseUrl,
        autoDowloadLabels,
        data,
        onBaseUrlChanged = {
            vm.updateBaseUrl(it)
        },
        onClientPemChanged = {
            vm.updateClientPem(it)
        },
        onServerCaChanged = {
            vm.updateServerCa(it)
        },
        onAutoDownloadLabelsChanged = { value, completed ->
            vm.updateAutoDownloadLabels(value, completed)
        },
        onImportClientPEM = {
            // application/x-pem-file does not work
            // application/x-x509-ca-cert does ??
            launcherC.launch("application/*")
        },
        onImportServerCA = {
            launcherS.launch("application/*")
        },
        onCheck = {
            navigator.navigate(SettingsCheckScreenDestination())
        },
        onAutoDownload = {
            scope.launch {
                val count = vm.startAutoDownloads(data.labelsInfo)
                val toastMessage = context.resources.getQuantityString(
                    R.plurals.settings_auto_dowload_started,
                    count,
                    count,
                )
                snackbarHostState.showSnackbar(toastMessage)
            }
        },
        snackbarHostState,
        onNavigationIcon,
    )
}

@Composable
fun SettingsContent(
    baseUrl: String,
    autoDowloadLabels: TextFieldValue,
    data: SettingsData,
    onBaseUrlChanged: (String) -> Unit = {},
    onClientPemChanged: (String) -> Unit = {},
    onServerCaChanged: (String) -> Unit = {},
    onAutoDownloadLabelsChanged: (TextFieldValue, Boolean) -> Unit = { _, _ -> },
    onImportClientPEM: () -> Unit = {},
    onImportServerCA: () -> Unit = {},
    onCheck: () -> Unit = {},
    onAutoDownload: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigationIcon: (Boolean) -> Unit = {},
) = WrappedScaffold(
    snackbarHostState,
    onNavigationIcon,
    Dest.Settings.labelRes,
    topLevel = true,
) { modifier ->
    Column(
        modifier = modifier
            .padding(horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            // otherwise the label may be truncated vertically
            // when wrapped
            Spacer(modifier = Modifier.height(8.dp))

            PrefLabelsItem(
                value = autoDowloadLabels,
                allValues = data.allLabels,
                info = data.labelsInfo,
                onValueChanged = onAutoDownloadLabelsChanged,
                onAutoDownload = onAutoDownload,
            )
            Spacer(modifier = Modifier.height(24.dp))

            PrefSimpleItem(
                value = baseUrl,
                error = data.baseUrlError,
                onBaseUrlChanged,
                KeyboardType.Uri,
                R.string.settings_baseUrl_label,
                R.string.settings_baseUrl_hint,
            )
            Spacer(modifier = Modifier.height(24.dp))

            PrefLoadableTextItem(
                data.clientPem,
                onClientPemChanged,
                onImportClientPEM,
                R.string.settings_clientPem_label,
                R.string.settings_clientPem_hint,
            )
            Spacer(modifier = Modifier.height(24.dp))

            PrefLoadableTextItem(
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
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            Button(
                onClick = onCheck,
            ) {
                Text(stringResource(R.string.settings_check))
            }
        }

        // edge2edge : bottom
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}


@Composable
fun PrefSimpleItem(
    value: String,
    error: String?,
    onValueChanged: (String) -> Unit,
    type: KeyboardType,
    @StringRes labelRes: Int,
    @StringRes hintRes: Int,
    modifier: Modifier = Modifier,
) {
    val isError = !error.isNullOrEmpty()
    OutlinedTextField(
        value = value,
        onValueChange = onValueChanged,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = type,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Next,
        ),
        singleLine = true,
        placeholder = { Text(stringResource(hintRes)) },
        label = { Text(stringResource(labelRes)) },
        isError = isError,
        supportingText = {
            if (isError) {
                Text(error)
            }
        }
    )
}

@Composable
fun PrefLabelsItem(
    value: TextFieldValue,
    allValues: List<LabelType>,
    info: LabelsInfo,
    onValueChanged: (TextFieldValue, Boolean) -> Unit,
    onAutoDownload: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    val lastLabel = value.text.takeLastWhile { it != LABELS_SEPARATOR }.trim()

    val isExact = remember(lastLabel) {
        val filter = lastLabel.asFilter()
        allValues.any { it.normalizedName == filter }
    }

    val filtered = remember(lastLabel) {
        if (lastLabel.length >= 2) {
            val filter = lastLabel.asFilter()
            allValues.filter { it.normalizedName.contains(filter) }
        } else {
            listOf()
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.padding(horizontal = 5.dp),
    ) {

        OutlinedTextField(
            value = value,
            onValueChange = { onValueChanged(it, false) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable)
                .fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Next,
            ),
            singleLine = true,
            maxLines = 1,
            placeholder = { Text(stringResource(R.string.settings_autoDownloadLabels_hint)) },
            label = { Text(stringResource(R.string.settings_autoDownloadLabels_label)) },
            supportingText = {
                Text(
                    stringResource(
                        id = R.string.settings_autoDownloadLabels_info,
                        info.autoDownloads.total,
                        info.autoDownloads.todo,
                    )
                )
            },
            trailingIcon = {
                AnimatedVisibility(
                    visible = info.autoDownloads.todo > 0,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    IconButton(
                        onClick = onAutoDownload,
                    ) {
                        Icon(Icons.Outlined.Download, null)
                    }
                }
            }
        )

        if (!isExact && filtered.isNotEmpty() && filtered.size < 15) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    // see ExposedDropdownMenuBox looses focus and disappears on keyboard input
                    // https://issuetracker.google.com/issues/238331998
                    expanded = false
                },
            ) {
                filtered.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption.name) },
                        onClick = {
                            val before = value.text.dropLastWhile { it != LABELS_SEPARATOR }
                            onValueChanged(value.copy("$before ${selectionOption.name}, "), true)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PrefLoadableTextItem(
    data: SettingItem,
    onValueChanged: (String) -> Unit,
    onImport: () -> Unit = {},
    @StringRes labelRes: Int,
    @StringRes hintRes: Int,
) {
    val clipboardManager = LocalClipboardManager.current
    var showDialog by remember { mutableStateOf(false) }

    val isError = !data.error.isNullOrEmpty()
    val isEmpty = data.inputValue.isEmpty()
    val mayShowHelp = isEmpty || isError

    val txt = stringResource(
        when {
            isError -> R.string.settings_value_error
            isEmpty -> R.string.settings_value_empty
            else -> R.string.settings_value_ok
        }
    )

    var colors = OutlinedTextFieldDefaults.colors()

    val txtColor = when {
        isEmpty -> colors.disabledTextColor
        else -> colors.focusedTextColor
    }

    colors = colors.copy(
        errorTextColor = colors.errorLabelColor,
        errorTrailingIconColor = colors.focusedTrailingIconColor,
        focusedTextColor = txtColor,
        unfocusedTextColor = txtColor,
    )

    OutlinedTextField(
        value = txt,
        onValueChange = onValueChanged,
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        singleLine = true,
        maxLines = 1,
        label = {
            Text(stringResource(labelRes))
        },
        isError = isError,
        supportingText = {
            if (isError) {
                Text(data.error)
            }
        },
        trailingIcon = {
            Row {
                AnimatedVisibility(
                    isEmpty || isError,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
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
                    }
                }

                AnimatedVisibility(
                    !isEmpty,
                    enter = fadeIn() + expandHorizontally(),
                    exit = shrinkHorizontally() + fadeOut(),
                ) {
                    IconButton(onClick = { onValueChanged("") }) {
                        Icon(Icons.Outlined.Clear, null)
                    }
                }

                IconButton(
                    onClick = { showDialog = true },
                ) {
                    Icon(if (mayShowHelp) Icons.AutoMirrored.Outlined.Help else Icons.Outlined.Visibility, null)
                }
            }

        },
        colors = colors,
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            text = {
                if (mayShowHelp) {
                    Text(
                        stringResource(hintRes),
                        style = MaterialTheme.typography.bodyMedium.copy(),
                    )
                } else {
                    Text(
                        data.value ?: "",
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState()),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(id = R.string.settings_dialog_ok))
                }
            },
        )
    }
}

//region preview

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AppTheme {
        SettingsContent(
            "http://",
            TextFieldValue("aaa, dddd"),
            SettingsData(
                "URL error",
                listOf(),
                LabelsInfo(),
                SettingItem("value 1", "An error"),
                SettingItem(""),
            ),
        )
    }
}

//endregion

