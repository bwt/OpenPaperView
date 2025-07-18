@file:OptIn(ExperimentalMaterial3Api::class)

package net.phbwt.paperwork.ui.main

import android.util.Log
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.compose.rememberNavController
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.spec.DestinationSpec
import com.ramcosta.composedestinations.spec.DestinationStyle
import com.ramcosta.composedestinations.utils.currentDestinationAsState
import com.ramcosta.composedestinations.utils.destination
import com.ramcosta.composedestinations.utils.rememberDestinationsNavigator
import com.ramcosta.composedestinations.utils.route
import com.ramcosta.composedestinations.utils.startDestination
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.phbwt.paperwork.R
import net.phbwt.paperwork.data.DbUpdateStatus
import net.phbwt.paperwork.data.UpdateAvailable
import net.phbwt.paperwork.data.UpdateError
import net.phbwt.paperwork.ui.main.Dest.Companion.asDest

@Composable
fun MainScreen(
    vm: MainVM = hiltViewModel(),
) {
    val updateState by vm.dbUpdates.collectAsStateWithLifecycle()
    val isConfigured by vm.isConfigured.collectAsStateWithLifecycle()
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
    val destinationNav = navController.rememberDestinationsNavigator()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val currentDest = ((navController.currentDestinationAsState().value
        ?: NavGraphs.root.startDestination)).asDest()

    // 'New DB' snackbar
    when (updateState) {
        is UpdateError -> {
            val msg = stringResource(R.string.db_update_error, updateState.error.toString().take(50))

            LaunchedEffect(snackbarHostState) {
                snackbarHostState.showSnackbar(msg)
                dbUpdateAck()
            }
        }

        is UpdateAvailable -> {
            val msg = stringResource(R.string.db_update_available)
            val actionLabel = stringResource(R.string.db_update_restart)

            if (runningInTestLab) {
                Log.i(TAG, "TestLab : restarting application")
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
    var showDialog by rememberSaveable { mutableStateOf(true) }
    var startForTestLab by remember { mutableStateOf(false) }

    // avoid cancellation when isConfigured changes
    LaunchedEffect(startForTestLab) {
        if (startForTestLab) {
            delay(1000)
            Log.i(TAG, "TestLab : setting default server")
            setDemoServer()
        }
    }

    if (showDialog && !isConfigured) {

        if (runningInTestLab) {
            startForTestLab = true
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
            ModalDrawerSheet(drawerState) {
                Spacer(modifier = Modifier.height(16.dp))

                val tlds = if (runningInTestLab) TEST_TLDS else TLDS
                for (dest in tlds) {
                    NavigationDrawerItem(
                        icon = { Icon(dest.icon, contentDescription = null) },
                        label = { Text(stringResource(dest.labelRes)) },
                        selected = dest == currentDest,
                        onClick = {
                            scope.launch { drawerState.close() }

                            destinationNav.navigate(dest.topDirection) {

                                popUpTo(Dest.DocList.destination) {
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

            // prevent transitions fading to then from white when using dark theme
            // not sure why
            Surface {
                DestinationsNavHost(
                    navController = navController,
                    navGraph = NavGraphs.root,
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .imePadding(),
                    dependenciesContainerBuilder = {
                        dependency(snackbarHostState)
                        dependency { topLevel: Boolean ->
                            if (topLevel) {
                                scope.launch { drawerState.open() }
                            } else {
                                navController.popBackStack()
                            }
                        }
                    }
                )
            }
        },
    )
}


const val transitionDuration = 300

private val toStart = AnimatedContentTransitionScope.SlideDirection.Start
private val toEnd = AnimatedContentTransitionScope.SlideDirection.End

private fun <T> outNow() =
    tween<T>(transitionDuration * 8 / 20, easing = FastOutLinearInEasing)

private fun <T> inDelayed() =
    tween<T>(transitionDuration * 16 / 20, transitionDuration * 4 / 20, LinearOutSlowInEasing)

object AppTransitions : DestinationStyle.Animated() {

    override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition? = {
        when (val tt = transitionType()) {
            TransType.FADE -> fadeIn(inDelayed()) + scaleIn(inDelayed(), .92f)

            else -> slideIntoContainer(
                if (tt == TransType.OUT) toEnd else toStart,
                animationSpec = tween(transitionDuration, easing = FastOutSlowInEasing),
                initialOffset = { it / 10 },
            ) + fadeIn(inDelayed())
        }
    }

    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition? = {
        when (val tt = transitionType()) {
            TransType.FADE -> fadeOut(outNow())

            else -> slideOutOfContainer(
                if (tt == TransType.OUT) toEnd else toStart,
                animationSpec = tween(transitionDuration, easing = FastOutSlowInEasing),
                targetOffset = { it / 10 },
            ) + fadeOut(outNow())
        }
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.transitionType() =
        transitionType(initialState.destination(), targetState.destination())

    private fun transitionType(from: DestinationSpec, to: DestinationSpec): TransType {
        val f = from.asDest().transitionPosition
        val t = to.asDest().transitionPosition

        return when {
            f == t -> TransType.FADE
            t.startsWith(f) -> TransType.IN
            f.startsWith(t) -> TransType.OUT
            else -> TransType.FADE
        }
    }

    private enum class TransType { IN, OUT, FADE }
}


@Composable
fun EmptyScaffold(
    snackbarHostState: SnackbarHostState,
    content: @Composable (PaddingValues) -> Unit,
) = Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    content = content,
)

@Composable
fun WrappedScaffold(
    snackbarHostState: SnackbarHostState,
    onNavigationIcon: (Boolean) -> Unit,
    titleRes: Int,
    topLevel: Boolean,
    collapsingTop: Boolean = false,
    modifier: Modifier = Modifier,
    wrappedContent: @Composable (modifier: Modifier) -> Unit,
) {
    val scrollBehavior = if (collapsingTop) TopAppBarDefaults.enterAlwaysScrollBehavior() else TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = {
                    IconButton(
                        onClick = { onNavigationIcon(topLevel) },
                    ) {
                        Icon(if (topLevel) Icons.Filled.Menu else Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        content = { innerPadding ->
            wrappedContent(
                // edge2edge :
                // The bottom padding (behind the navigationbar)
                // is handled by each screen
                // typically by adding as Spacer directly (in a column),
                // as an additional item (lazycolumn)
                // or by some insets padding
                Modifier.padding(
                    top = innerPadding.calculateTopPadding(),
                    start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                )
            )
        },
    )
}

private const val TAG = "MainScreen"