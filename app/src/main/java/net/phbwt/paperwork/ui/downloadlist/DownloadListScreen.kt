package net.phbwt.paperwork.ui.downloadlist

import android.text.format.Formatter
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Downloading
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import net.phbwt.paperwork.R
import net.phbwt.paperwork.data.dao.DownloadStats
import net.phbwt.paperwork.data.entity.DocumentFull
import net.phbwt.paperwork.data.entity.DownloadState
import net.phbwt.paperwork.data.entity.Part
import net.phbwt.paperwork.helper.fmtDtm
import net.phbwt.paperwork.ui.destinations.PageListScreenDestination
import net.phbwt.paperwork.ui.doclist.makeFakeDocuments
import net.phbwt.paperwork.ui.main.AppTransitions
import net.phbwt.paperwork.ui.main.Dest
import net.phbwt.paperwork.ui.main.WrappedScaffold
import net.phbwt.paperwork.ui.main.transitionDuration
import net.phbwt.paperwork.ui.theme.AppTheme
import kotlin.math.max


data class DownloadListScreenArgs(
    val documentId: Int? = null,
)

@Destination(
    navArgsDelegate = DownloadListScreenArgs::class,
    style = AppTransitions::class,
)
@Composable
fun DownloadListScreen(
    navigator: DestinationsNavigator,
    snackbarHostState: SnackbarHostState,
    onNavigationIcon: (Boolean) -> Unit,
    vm: DownloadListVM = hiltViewModel(),
) {
    val data by vm.screenData().collectAsStateWithLifecycle(DownloadListData())

    val scope = rememberCoroutineScope()

    DownloadListContent(
        vm.navArgs,
        data,
        onPartRestart = { scope.launch { vm.restart(it) } },
        onDocumentDelete = { scope.launch { vm.clear(it) } },
        onDocClicked = { doc ->
            if (doc.canBeViewed) {
                navigator.navigate(PageListScreenDestination(doc.document.documentId))
            }
        },
        onFlashFinished = { vm.setEnterFlashDone() },
        snackbarHostState,
        onNavigationIcon,
    )
}

@Composable
fun DownloadListContent(
    navArgs: DownloadListScreenArgs,
    data: DownloadListData,
    onPartRestart: (Part) -> Unit = {},
    onDocumentDelete: (DocumentFull) -> Unit = { },
    onDocClicked: (DocumentFull) -> Unit = {},
    onFlashFinished: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigationIcon: (Boolean) -> Unit = {},
) = WrappedScaffold(
    snackbarHostState,
    onNavigationIcon,
    Dest.DownloadList.labelRes,
    topLevel = navArgs.documentId == null,
) { modifier ->
    Column(
        modifier = modifier,
    ) {
        DownloadRows(
            data.downloads,
            navArgs.documentId,
            data.enterFlashDone,
            onPartRestart,
            onDocumentDelete,
            onDocClicked,
            onFlashFinished,
            modifier = Modifier.weight(1f),
        )
        DownloadStatsRow(
            data.stats,
            modifier = Modifier
                .padding(top = 12.dp, bottom = 4.dp)
        )
        // edge2edge : bottom
        // we cant't use Modifier.navigationBarsPadding()
        // because we only want the bottom (other parts are already accounted for when wrapping the scaffold)
        // and the navigation bar may be on the left or right (e.g. landscape on older Android)
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}


@Composable
fun DownloadStatsRow(
    stats: DownloadStats?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val d = stats?.documents ?: 0
    val p = stats?.parts ?: 0
    val s = stats?.size ?: 0
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = pluralStringResource(id = R.plurals.dowloads_documents, count = d, d)
                    + (if (d == p) "" else pluralStringResource(id = R.plurals.dowloads_parts, count = p, p))
                    + stringResource(id = R.string.dowloads_size, Formatter.formatFileSize(context, s)),
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DownloadRows(
    docs: List<DocumentFull>,
    documentId: Int?,
    enterFlashDone: Boolean,
    onPartRestart: (Part) -> Unit,
    onDocumentDelete: (DocumentFull) -> Unit,
    onDocClicked: (DocumentFull) -> Unit,
    onFlashFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    var documentIndex by remember { mutableIntStateOf(-1) }
    var scrollDone by remember { mutableStateOf(false) }

    LaunchedEffect(documentIndex) {
        if (!enterFlashDone && documentIndex >= 0 && !scrollDone) {
            Log.d(TAG, "Scrolling to $documentIndex")
            listState.scrollToItem(max(0, documentIndex - 1))
            scrollDone = true
        }
    }

    // flash animation on the selected item when entering
    val normal = MaterialTheme.colorScheme.background
    val highlight = MaterialTheme.colorScheme.secondaryContainer
    val duration = AnimationConstants.DefaultDurationMillis * 3 / 2

    val flashingBackground by animateColorAsState(
        // When using transparent : animation with an intermediate gray color
        if (!scrollDone) highlight else normal,
        animationSpec = keyframes {
            durationMillis = duration
            delayMillis = transitionDuration / 4
            // override the start color
            normal at 0 using LinearOutSlowInEasing
            highlight at duration / 2 using FastOutLinearInEasing
            normal at duration
        },
        label = "color_flash"
    ) {
        onFlashFinished()
    }

    val flashingModifier = Modifier.drawBehind { drawRect(flashingBackground) }

    // When not setting background on not animated items : glitch with the top divider when the animation ends
    val notFlashingModifier = Modifier.background(MaterialTheme.colorScheme.background)

    LazyColumn(
        state = listState,
        modifier = modifier,
    ) {
        var currentIndex = 0

        docs.forEach { doc ->

            val isSelected = doc.document.documentId == documentId
            val isFlashing = isSelected && !enterFlashDone

            if (isSelected) {
                documentIndex = currentIndex
            }

            // will add the scoped part later
            val baseModifier = if (isFlashing) flashingModifier else notFlashingModifier

            // main item for the document infos
            item(key = -doc.document.documentId, contentType = "header") {
                HorizontalDivider(thickness = Dp.Hairline, color = MaterialTheme.colorScheme.onBackground)
                DownloadHeader(doc, baseModifier.animateItemPlacement(), onDocumentDelete, onDocClicked)
            }
            currentIndex++

            // optional items for each part not yet downloaded
            doc.parts.filter { it.isIn }.forEach { part ->
                item(key = part.partId, contentType = "part") {

                    DownloadRow(
                        part,
                        onPartRestart,
                        baseModifier.animateItemPlacement(),
                    )
                }
                currentIndex++
            }
        }
    }
}

@Composable
fun DownloadHeader(
    doc: DocumentFull,
    modifier: Modifier = Modifier,
    onDocumentDelete: (DocumentFull) -> Unit,
    onDocClicked: (DocumentFull) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val icons = Icons.Outlined

    Row(
        modifier = modifier
            .clickable(enabled = doc.downloadStatus == DownloadState.LOCAL) { onDocClicked(doc) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = doc.document.titleOrName,
                color = colors.primary,
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
            )
            Row {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.bodyMedium,
//                    LocalContentAlpha provides 0.5f,
                ) {
                    if (doc.parts.size > 1) {
                        // ok / total
                        Text(doc.parts.count { it.isLocal }.toString(), fontWeight = FontWeight.Bold)
                        Text(" / ")
                        Text(doc.parts.size.toString(), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(doc.document.date.fmtDtm(LocalContext.current))
                }
            }
        }

        // button delete
        IconButton(onClick = { onDocumentDelete(doc) }) {
            Icon(
                icons.Delete,
                contentDescription = null,
            )
        }
    }
}


@Composable
fun DownloadRow(
    part: Part,
    onPartRestart: (Part) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val icons = Icons.Outlined

    Row(
        modifier = modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        // status
        Icon(
            when {
                part.isQueued -> icons.Timer
                part.isInProgress -> icons.Downloading
                part.isLocal -> icons.Done
                part.isFailed -> icons.Error
                else -> icons.Warning
            },
            contentDescription = null,
        )

        // name + error
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f),
        ) {

            // name
            Text(
                text = part.name,
                modifier = modifier.alpha(.7f),
                style = MaterialTheme.typography.bodyMedium,
            )

            // error
            AnimatedVisibility(visible = !part.downloadError.isNullOrBlank()) {
                Text(
                    text = part.downloadError ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start,
                    color = colors.error,
                )
            }

//            // size
//            Text(
//                text = Formatter.formatFileSize(LocalContext.current, dnl.part.size),
//                Modifier.widthIn(min = 64.dp),
//                style = MaterialTheme.typography.body2,
//                textAlign = TextAlign.End,
//            )
        }

        // button restart
        IconButton(
            onClick = { onPartRestart(part) },
            modifier = Modifier.alpha(if (part.isFailed) 1f else 0f),
        ) {
            Icon(
                icons.RestartAlt,
                contentDescription = null,
                tint = colors.error,
            )
        }
    }
}

private const val TAG = "DownloadListScreen"

//region preview

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AppTheme {
        DownloadListContent(DownloadListScreenArgs(12), DownloadListData(makeFakeDocuments(13, "document"), DownloadStats(32, 434, 534535)))
//        DocListContent("zz", listOf("label1", "label2"), makeFakeDocuments(5, "none"))
    }
}


//endregion preview

