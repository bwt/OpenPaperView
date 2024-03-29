@file:OptIn(ExperimentalMaterial3Api::class)

package net.phbwt.paperwork.ui.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.navigation.compose.rememberNavController
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.spec.DestinationStyle
import kotlinx.coroutines.launch
import net.phbwt.paperwork.R
import net.phbwt.paperwork.data.DbUpdateStatus
import net.phbwt.paperwork.data.UpdateAvailable
import net.phbwt.paperwork.data.UpdateError
import net.phbwt.paperwork.ui.NavGraphs
import net.phbwt.paperwork.ui.appCurrentDestinationAsState
import net.phbwt.paperwork.ui.appDestination
import net.phbwt.paperwork.ui.destinations.Destination
import net.phbwt.paperwork.ui.destinations.DocListScreenDestination
import net.phbwt.paperwork.ui.destinations.DownloadListScreenDestination
import net.phbwt.paperwork.ui.destinations.PageListScreenDestination
import net.phbwt.paperwork.ui.destinations.SettingsCheckScreenDestination
import net.phbwt.paperwork.ui.destinations.SettingsScreenDestination
import net.phbwt.paperwork.ui.main.Dest.Companion.asDest
import net.phbwt.paperwork.ui.startAppDestination

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

    val currentDest = ((navController.appCurrentDestinationAsState().value
        ?: NavGraphs.root.startAppDestination)).asDest()

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

                            navController.navigate(dest.topDirection) {

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
                    Box(
                        modifier = Modifier
                            .statusBarsPadding()
                            .animateContentSize(tween(transitionDuration, transitionDelayIn)),
                    ) {
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
                    }
                },
            ) { innerPadding ->
                DestinationsNavHost(
                    navController = navController,
                    navGraph = NavGraphs.root,
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
                    dependenciesContainerBuilder = {
                        dependency(snackbarHostState)
                    }
                )
            }
        },
    )
}


const val transitionDuration = 400
const val transitionDelayIn = 0
const val transitionDelayOut = 120
val moveIn = AnimatedContentTransitionScope.SlideDirection.Left
val moveOut = AnimatedContentTransitionScope.SlideDirection.Right

object AppTransitions : DestinationStyle.Animated {
    override fun AnimatedContentTransitionScope<NavBackStackEntry>.enterTransition() =
        slideIntoContainer(
            if (goingBack()) moveOut else moveIn,
            animationSpec = tween(transitionDuration - transitionDelayIn, transitionDelayIn),
        )

    override fun AnimatedContentTransitionScope<NavBackStackEntry>.exitTransition() =
        slideOutOfContainer(
            if (goingBack()) moveOut else moveIn,
            animationSpec = tween(transitionDuration - (transitionDelayOut * 2), transitionDelayOut),
        )

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.goingBack() =
        goingBack(initialState.appDestination(), targetState.appDestination())

    private fun goingBack(from: Destination, to: Destination) = when {
        to is DocListScreenDestination -> true
        from is PageListScreenDestination && to is DownloadListScreenDestination -> true
        from is SettingsCheckScreenDestination && to is SettingsScreenDestination -> true
        else -> false
    }

}
