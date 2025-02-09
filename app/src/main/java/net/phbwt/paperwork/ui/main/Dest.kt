package net.phbwt.paperwork.ui.main

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.ramcosta.composedestinations.spec.Direction
import net.phbwt.paperwork.R
import net.phbwt.paperwork.ui.destinations.AboutScreenDestination
import net.phbwt.paperwork.ui.destinations.Destination
import net.phbwt.paperwork.ui.destinations.DocListScreenDestination
import net.phbwt.paperwork.ui.destinations.DownloadListScreenDestination
import net.phbwt.paperwork.ui.destinations.PageListScreenDestination
import net.phbwt.paperwork.ui.destinations.PairingScreenDestination
import net.phbwt.paperwork.ui.destinations.SettingsCheckScreenDestination
import net.phbwt.paperwork.ui.destinations.SettingsScreenDestination


sealed class Dest(
    val destination: Destination,
    val topDirection: Direction,
    val icon: ImageVector,
    @StringRes val labelRes: Int,
    // Used to know which transition to use
    val transitionPosition: String,
) {

    data object DocList : Dest(
        DocListScreenDestination,
        DocListScreenDestination,
        icon = Icons.AutoMirrored.Filled.FormatListBulleted,
        labelRes = R.string.screen_docList,
        transitionPosition = "a",
    )

    data object DownloadList : Dest(
        DownloadListScreenDestination,
        DownloadListScreenDestination(),
        icon = Icons.Filled.Download,
        labelRes = R.string.screen_downloadList,
        transitionPosition = "aa",
    )

    data object Settings : Dest(
        SettingsScreenDestination,
        SettingsScreenDestination,
        icon = Icons.Filled.Settings,
        labelRes = R.string.screen_settings,
        transitionPosition = "b",
    )

    data object SettingsCheck : Dest(
        SettingsCheckScreenDestination,
        SettingsCheckScreenDestination,
        icon = Icons.Filled.Settings,
        labelRes = R.string.screen_settingsCheck,
        transitionPosition = "ba",
    )

    data object Pairing : Dest(
        PairingScreenDestination,
        PairingScreenDestination(null),
        icon = Icons.Filled.Settings,
        labelRes = R.string.screen_pairing,
        transitionPosition = "bb",
    )

    data object PageList : Dest(
        PageListScreenDestination,
        PageListScreenDestination(0),
        icon = Icons.Filled.Image,
        labelRes = R.string.screen_pageList,
        transitionPosition = "aaa",
    )

    data object About : Dest(
        AboutScreenDestination,
        AboutScreenDestination,
        icon = Icons.Filled.Info,
        labelRes = R.string.screen_about,
        transitionPosition = "c",
    )

    companion object {
        fun Destination.asDest() = ALL_DESTS.first {
            it.destination == this
        }
    }
}

val TLDS = listOf(Dest.DocList, Dest.DownloadList, Dest.Settings, Dest.About)
val TEST_TLDS = listOf(Dest.DocList, Dest.DownloadList)
val ALL_DESTS = listOf(Dest.DocList, Dest.PageList, Dest.DownloadList, Dest.Settings, Dest.SettingsCheck, Dest.Pairing, Dest.About)


