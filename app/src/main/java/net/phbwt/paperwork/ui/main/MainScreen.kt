@file:OptIn(ExperimentalMaterial3Api::class)

package net.phbwt.paperwork.ui.main

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import net.phbwt.paperwork.R
import net.phbwt.paperwork.data.DbUpdateStatus
import net.phbwt.paperwork.data.UpdateAvailable
import net.phbwt.paperwork.data.UpdateError
import net.phbwt.paperwork.ui.about.AboutScreen
import net.phbwt.paperwork.ui.doclist.DocListScreen
import net.phbwt.paperwork.ui.downloadlist.DownloadListScreen
import net.phbwt.paperwork.ui.pagelist.PageListScreen
import net.phbwt.paperwork.ui.settings.SettingsScreen
import net.phbwt.paperwork.ui.settingscheck.SettingsCheckScreen

@Composable
fun MainScreen(
    vm: MainVM = hiltViewModel(),
) {
    val updateState by vm.dbUpdates.collectAsStateWithLifecycle()
    val isConfigured by vm.isConfigured.collectAsStateWithLifecycle(true)
    val runningInTestLab = remember { vm.isRunningInTestLab() }
    MainContent(
        updateState,
        isConfigured,
        runningInTestLab,
        dbUpdateAck = { vm.clearDbUpdate() },
        restartApplication = { vm.restartApplication() },
        setDemoServer = { vm.setDemoServer() }
    )
}

@Composable
fun MainContent(
    updateState: DbUpdateStatus,
    isConfigured: Boolean,
    runningInTestLab: Boolean,
    dbUpdateAck: () -> Unit,
    restartApplication: () -> Unit,
    setDemoServer: suspend () -> Unit,
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = Dest.find(navBackStackEntry?.destination?.route ?: Dest.DocList.route)

    // 'New DB' snackbar
    when (updateState) {
        is UpdateError -> {
            val msg = stringResource(R.string.db_update_error, updateState.error.toString().take(50))

            LaunchedEffect(snackbarHostState) {
                snackbarHostState.showSnackbar(msg)
                dbUpdateAck()
            }
        }

        UpdateAvailable -> {
            val msg = stringResource(R.string.db_update_available)
            val actionLabel = stringResource(R.string.db_update_restart)

            if (runningInTestLab) {
                restartApplication()
            }

            LaunchedEffect(snackbarHostState) {
                val action = snackbarHostState.showSnackbar(
                    message = msg,
                    actionLabel = actionLabel,
                    withDismissAction = true,
                )

                if (action == SnackbarResult.ActionPerformed) {
                    restartApplication()
                } else {
                    dbUpdateAck()
                }
            }
        }

        else -> {
            // nothing to do
        }
    }


    // 'check the demo' dialog
    var showDialog by remember { mutableStateOf(true) }

    if (showDialog && !isConfigured) {

        if (runningInTestLab) {
            LaunchedEffect(Unit) {
                setDemoServer()
            }
        }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(id = R.string.main_dialog_title)) },
            text = { Text(stringResource(id = R.string.main_dialog_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        scope.launch { setDemoServer() }
                    }
                ) { Text(stringResource(id = R.string.main_dialog_yes)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                    }
                ) { Text(stringResource(id = R.string.main_dialog_no)) }
            },
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))

                val tlds = if (runningInTestLab) TEST_TLDS else TLDS
                for (dest in tlds) {
                    NavigationDrawerItem(
                        icon = { Icon(dest.icon, contentDescription = null) },
                        label = { Text(stringResource(dest.labelRes)) },
                        selected = dest == currentDest,
                        onClick = {
                            scope.launch { drawerState.close() }

                            navController.navigate(dest.topRoute) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    // XXX : if true and restoreState is false, the VM are kept
                                    // but not reused (possibly with coroutines in the viewModelScope)
                                    saveState = false
                                }
                                launchSingleTop = true
                                // XXX : true breaks navigate to doclist when starting
                                // from the downloadList for a specific documentId
                                restoreState = false
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        },

        content = {
            Scaffold(
                // FIXME : there is an additional space when the IME is open (systembar)
                snackbarHost = { SnackbarHost(snackbarHostState, Modifier.imePadding()) },
                topBar = {
                    if (currentDest != Dest.PageList) {
                        TopAppBar(
                            title = { Text(stringResource(currentDest.labelRes)) },
                            navigationIcon = {
                                IconToggleButton(
                                    checked = false,
                                    onCheckedChange = {
                                        scope.launch { if (it) drawerState.open() else drawerState.close() }
                                    }) {
                                    Icon(Icons.Filled.Menu, contentDescription = null)
                                }
                            }
                        )
                    }
                },
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Dest.DocList.route,
                    modifier = Modifier
                        .fillMaxSize()
                        // The bottom padding (behind the navigationbar)
                        // is handled by each screen
                        // typically by adding as Spacer directly (in a column)
                        // or as an additional item (lazycolumn)
                        .padding(
                            top = innerPadding.calculateTopPadding(),
                            start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                            end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                        )
                        .imePadding(),
                    // avoid default transition
                    // TODO : define transitions
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                ) {
                    composable(
                        Dest.DocList.route,
                    ) { DocListScreen(navController, snackbarHostState) }
                    composable(
                        Dest.DownloadsList.route,
                        arguments = listOf(navArgument(PARAM_DOCUMENT_ID) { type = NavType.IntType }),
                    ) { bse ->
                        DownloadListScreen(navController)
                    }
                    composable(
                        Dest.Settings.route,
                    ) { SettingsScreen(navController) }
                    composable(
                        Dest.SettingsCheck.route,
                    ) { SettingsCheckScreen(navController) }
                    composable(
                        Dest.PageList.route,
                        arguments = listOf(navArgument(PARAM_DOCUMENT_ID) { type = NavType.IntType }),
                    ) { bse ->
                        PageListScreen(navController)
                    }
                    composable(
                        Dest.About.route,
                    ) { AboutScreen(navController) }
                }
            }
        },
    )
}

private fun NavBackStackEntry.getIntArg(argName: String) = arguments?.getInt(argName) ?: throw IllegalArgumentException("Missing '$argName' argument")
