package net.phbwt.paperwork.ui.main

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import net.phbwt.paperwork.R


sealed class Dest(
    val route: String,
    val topRoute: String,
    val icon: ImageVector,
    @StringRes val labelRes: Int,
) {

    object DocList : Dest(
        route = "docList",
        topRoute = "docList",
        icon = Icons.Filled.FormatListBulleted,
        labelRes = R.string.screen_docList,
    )

    object DownloadsList : Dest(
        route = "downloadList/{documentId}",
        topRoute = "downloadList/0",
        icon = Icons.Filled.Download,
        labelRes = R.string.screen_downloadList,
    )

    object Settings : Dest(
        route = "settings",
        topRoute = "settings",
        icon = Icons.Filled.Settings,
        labelRes = R.string.screen_settings,
    )

    object SettingsCheck : Dest(
        route = "settingsCheck",
        topRoute = "settingsCheck",
        icon = Icons.Filled.Settings,
        labelRes = R.string.screen_settingsCheck,
    )

    object PageList : Dest(
        route = "pageList/{documentId}",
        topRoute = "pageList/0",
        icon = Icons.Filled.Image,
        labelRes = R.string.screen_pageList,
    )

    object About : Dest(
        route = "about",
        topRoute = "about",
        icon = Icons.Filled.Info,
        labelRes = R.string.screen_about,
    )

    companion object {

        fun find(route: String) = ALL_DESTS.first { it.route == route || it.topRoute == route }
    }
}

val TLDS = listOf(Dest.DocList, Dest.DownloadsList, Dest.Settings, Dest.About)
val TEST_TLDS = listOf(Dest.DocList, Dest.DownloadsList)
val ALL_DESTS = listOf(Dest.DocList, Dest.PageList, Dest.DownloadsList, Dest.Settings, Dest.SettingsCheck, Dest.About)


