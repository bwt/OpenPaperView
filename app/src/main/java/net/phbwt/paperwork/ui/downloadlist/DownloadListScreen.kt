package net.phbwt.paperwork.ui.downloadlist

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import net.phbwt.paperwork.data.entity.DocumentFull
import net.phbwt.paperwork.data.entity.DownloadState
import net.phbwt.paperwork.data.entity.Part
import net.phbwt.paperwork.helper.fmtDtm
import net.phbwt.paperwork.ui.doclist.makeFakeDocuments
import net.phbwt.paperwork.ui.theme.AppTheme

@Composable
fun DownloadListScreen(
    navController: NavController,
    documentId: Int,
    vm: DownloadListVM = hiltViewModel(),
) {
    val docs by vm.downloads().collectAsStateWithLifecycle(listOf())

    val scope = rememberCoroutineScope()

    DownloadListContent(
        docs = docs,
        documentId,
        onPartRestart = { scope.launch { vm.restart(it) } },
        onDocumentDelete = { scope.launch { vm.clear(it) } },
        onDocClicked = { doc ->
            if (doc.downloadStatus == DownloadState.LOCAL || doc.isImagesDoc) {
                navController.navigate("pageList/${doc.document.documentId}")
            }
        },
    )
}

@Composable
fun DownloadListContent(
    docs: List<DocumentFull>,
    documentId: Int = 12,
    onPartRestart: (Part) -> Unit = {},
    onDocumentDelete: (DocumentFull) -> Unit = { },
    onDocClicked: (DocumentFull) -> Unit = {},
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column {
            DownloadRows(docs, documentId, onPartRestart, onDocumentDelete, onDocClicked)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DownloadRows(
    docs: List<DocumentFull>,
    documentId: Int,
    onPartRestart: (Part) -> Unit,
    onDocumentDelete: (DocumentFull) -> Unit,
    onDocClicked: (DocumentFull) -> Unit,
) {
    val listState = rememberLazyListState()

    var documentIndex by remember { mutableIntStateOf(-1) }
    var scrollDone by remember { mutableStateOf(false) }

    LaunchedEffect(documentIndex) {
        if (documentIndex >= 0 && !scrollDone) {
            Log.d(TAG, "Scrolling to $documentIndex")
            listState.scrollToItem(documentIndex)
            scrollDone = true
        }
    }

    LazyColumn(
        state = listState,
    ) {
        var currentIndex = 0

        docs.forEach { doc ->

            item(key = -doc.document.documentId, contentType = "header") {
                Divider(color = MaterialTheme.colorScheme.onBackground, thickness = Dp.Hairline)
                DownloadHeader(doc, Modifier.animateItemPlacement(), onDocumentDelete, onDocClicked)
            }

            if (doc.document.documentId == documentId) {
                documentIndex = currentIndex
            }
            currentIndex++

            doc.parts.filter { it.isIn }.forEach { part ->
                item(key = part.partId, contentType = "part") {
                    DownloadRow(
                        part,
                        onPartRestart,
                        Modifier.animateItemPlacement(),
                    )
                }
                currentIndex++
            }
        }
        // if we draw behind the navigation bar (IME closed)
        // we add a spacer so that the last item
        // can been scrolled into the visible area
        item {
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
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
        DownloadListContent(makeFakeDocuments(13, "document"))
//        DocListContent("zz", listOf("label1", "label2"), makeFakeDocuments(5, "none"))
    }
}


//endregion preview

