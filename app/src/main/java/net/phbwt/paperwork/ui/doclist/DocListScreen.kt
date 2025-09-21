@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class,
    ExperimentalLayoutApi::class,
)

package net.phbwt.paperwork.ui.doclist

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.Downloading
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.DownloadListScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PageListScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import net.phbwt.paperwork.R
import net.phbwt.paperwork.data.dao.SNIPPET_RESULT
import net.phbwt.paperwork.data.dao.SNIPPET_SPLIT
import net.phbwt.paperwork.data.entity.db.Document
import net.phbwt.paperwork.data.entity.db.DocumentFull
import net.phbwt.paperwork.data.entity.db.DownloadState
import net.phbwt.paperwork.data.entity.db.LabelType
import net.phbwt.paperwork.data.entity.db.Part
import net.phbwt.paperwork.data.entity.db.asFilter
import net.phbwt.paperwork.data.entity.db.makeDocumentThumbPathAndKey
import net.phbwt.paperwork.helper.TrailingClose
import net.phbwt.paperwork.helper.fmtDtm
import net.phbwt.paperwork.helper.startActivitySafely
import net.phbwt.paperwork.ui.main.AppTransitions
import net.phbwt.paperwork.ui.main.Dest
import net.phbwt.paperwork.ui.main.WrappedScaffold
import net.phbwt.paperwork.ui.theme.AppTheme
import kotlin.random.Random

@Destination<RootGraph>(start = true, style = AppTransitions::class)
@Composable
fun DocListScreen(
    navigator: DestinationsNavigator,
    snackbarHostState: SnackbarHostState,
    onNavigationIcon: (Boolean) -> Unit,
    vm: DocListVM = hiltViewModel(),
) {
    val search = vm.search
    val labels by vm.labelFilters.collectAsStateWithLifecycle()
    val labelTypes by vm.labelTypes.collectAsStateWithLifecycle()
    val fullMode by vm.fullMode.collectAsStateWithLifecycle(false)
    val rows by vm.documentsWithHeaders.collectAsStateWithLifecycle()

    val hasFilter = search.isNotBlank() || labels.isNotEmpty()

    BackHandler(enabled = hasFilter && !WindowInsets.isImeVisible) {
        vm.clearFilters()
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val pdfToast = stringResource(id = R.string.doclist_local_pdf_toast)
    val pdfAction = stringResource(id = R.string.doclist_local_pdf_action)
    val unknownDocType = stringResource(id = R.string.doclist_unknown_document_type)

    DocListContent(
        search,
        labels,
        rows = rows,
        labelTypes,
        fullMode,
        onSearchChange = { vm.updateSearch(it) },
        onDocClicked = { doc ->
            when {
                doc.canBeViewed -> {
                    navigator.navigate(PageListScreenDestination(doc.document.documentId))
                }

                doc.isPdfDoc -> {
                    // only local PDF can be shown
                    scope.launch {
                        val r = snackbarHostState.showSnackbar(
                            pdfToast, pdfAction,
                            withDismissAction = true,
                            duration = SnackbarDuration.Short,
                        )
                        if (r == SnackbarResult.ActionPerformed) {
                            vm.queueDownload(doc.document.documentId)
                        }
                    }
                }

                else -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            unknownDocType,
                            withDismissAction = true,
                            duration = SnackbarDuration.Short,
                        )
                    }
                }
            }
        },
        onLabelAdded = { vm.addLabel(it) },
        onLabelRemoved = { vm.removeLabel(it) },
        onLabelToggled = { vm.toggleLabel(it) },
        onDownloadClicked = { doc ->
            if (doc.downloadStatus == DownloadState.DOWNLOADABLE) {
                scope.launch { vm.queueDownload(doc.document.documentId) }
            } else {
                navigator.navigate(DownloadListScreenDestination(doc.document.documentId))
            }
        },
        onShowClicked = { doc -> vm.makeDocumentShowIntent(doc).startActivitySafely(context) },
        onShareClicked = { doc ->
            scope.launch {
                vm.makeDocumentShareIntent(doc, ShareCompat.IntentBuilder(context))
                    .startActivitySafely(context)
            }
        },
        snackbarHostState,
        onNavigationIcon,
    )
}

@Composable
fun DocListContent(
    search: String,
    labels: List<DocListVM.LabelFilter>,
    rows: List<Any>,
    labelTypes: List<LabelType>,
    fullMode: Boolean,
    onSearchChange: (String) -> Unit = {},
    onDocClicked: (DocumentFull) -> Unit = {},
    onLabelAdded: (String) -> Unit = {},
    onLabelRemoved: (DocListVM.LabelFilter) -> Unit = {},
    onLabelToggled: (DocListVM.LabelFilter) -> Unit = {},
    onDownloadClicked: (DocumentFull) -> Unit = {},
    onShowClicked: (DocumentFull) -> Unit = {},
    onShareClicked: (DocumentFull) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigationIcon: (Boolean) -> Unit = {},
) = WrappedScaffold(
    snackbarHostState,
    onNavigationIcon,
    Dest.DocList.labelRes,
    topLevel = true,
    collapsingTop = true,
) { modifier ->
    Column(
        modifier = modifier,
    ) {
        Filters(search, labels, labelTypes, onLabelRemoved, onLabelAdded, onLabelToggled, onSearchChange)
        // workaround position lost on reconfiguration / navigation
        // because the list is temporarily empty
        // https://issuetracker.google.com/issues/179397301
        if (rows.isNotEmpty()) {
            DocRows(rows, fullMode, onDocClicked, onLabelAdded, onDownloadClicked, onShowClicked, onShareClicked)
        }
    }

}


@Composable
fun Filters(
    search: String,
    labels: List<DocListVM.LabelFilter>,
    labelTypes: List<LabelType>,
    onLabelRemoved: (DocListVM.LabelFilter) -> Unit = {},
    onLabelAdded: (String) -> Unit = {},
    onLabelToggled: (DocListVM.LabelFilter) -> Unit = {},
    onSearchChange: (String) -> Unit = {},
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var expanded by remember { mutableStateOf(false) }

    val filtered = remember(search) {
        if (search.length >= 2) {
            val filter = search.asFilter()
            labelTypes.filter { it.normalizedName.contains(filter) }
        } else {
            listOf()
        }
    }

    // filter : search
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.padding(start = 5.dp, end = 5.dp, top = 5.dp),
    ) {

        // filter textfield
        OutlinedTextField(
            value = search,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
            trailingIcon = {
                TrailingClose(visible = search.isNotEmpty()) {
                    onSearchChange("")
                    expanded = true
                }
            },
            placeholder = { Text(stringResource(R.string.main_search)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions {
                keyboardController?.hide()
                expanded = false
            },
        )

        // labels dropdown
        if (filtered.isNotEmpty() && filtered.size < 50) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    // see ExposedDropdownMenuBox looses focus and disappears on keyboard input
                    // https://issuetracker.google.com/issues/238331998
                    expanded = false
                },
            ) {
                filtered.forEach { selectionOption ->
                    val cb = {
                        onLabelAdded(selectionOption.name)
                        // AFTER adding label
                        onSearchChange("")
                    }
                    DropdownMenuItem(
                        text = { Text(selectionOption.name) },
                        onClick = cb,
                    )
                }
            }
        }
    }

    // filter : labels
    Box(modifier = Modifier.animateContentSize()) {
        if (labels.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .padding(top = 5.dp, start = 5.dp, end = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                for (label in labels) {
                    Chip(
                        label.label,
                        onAction = { },
                        onLeadingClick = { onLabelToggled(label) },
                        onTrailingClick = { onLabelRemoved(label) },
                        selected = label.include,
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocRows(
    rows: List<Any>,
    fullMode: Boolean,
    onDocClicked: (DocumentFull) -> Unit,
    onLabelClicked: (String) -> Unit,
    onDownloadClicked: (DocumentFull) -> Unit,
    onShowClicked: (DocumentFull) -> Unit,
    onShareClicked: (DocumentFull) -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier
            .padding(top = 5.dp)
            .clipToBounds(),
    ) {

        rows.forEach { row ->
            when (row) {
                is DocListVM.HeaderData -> {
                    stickyHeader(
                        key = row.key,
                        contentType = "header",
                    ) {
                        Surface(
                            modifier = Modifier
                                // doesn't work since accompanist 0.27, not sure why
                                //.animateItemPlacement()
                                .fillParentMaxWidth(),
                            color = colors.secondaryContainer,
                            contentColor = colors.onSecondaryContainer
                        ) {
                            DateHeader(row)
                        }
                    }
                }

                is DocumentFull -> {
                    item(
                        key = row.document.documentId,
                        contentType = "doc",
                    ) {

                        HorizontalDivider(thickness = Dp.Hairline, color = colors.primary)
                        DocRow(
                            row,
                            fullMode,
                            onDocClicked, onLabelClicked, onDownloadClicked, onShowClicked, onShareClicked,
                            Modifier.animateItem(),
                        )
                    }
                }
            }
        }
        // edge2edge : bottom
        // if we draw behind the navigation bar (IME is closed)
        // we add a spacer so that the last item
        // can been scrolled into the visible area
        item {
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
fun DateHeader(header: DocListVM.HeaderData) {
    Row {
        Text(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .alignByBaseline(),
            text = header.year.toString(),
            style = MaterialTheme.typography.titleLarge,
        )

        Text(
            modifier = Modifier.alignByBaseline(),
            text = header.month,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
fun DocRow(
    doc: DocumentFull,
    fullMode: Boolean,
    onDocClicked: (DocumentFull) -> Unit,
    onLabelClicked: (String) -> Unit,
    onDownloadClicked: (DocumentFull) -> Unit,
    onShowClicked: (DocumentFull) -> Unit,
    onShareClicked: (DocumentFull) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val icons = Icons.Outlined

    Row(
        modifier = modifier.padding(8.dp), //.height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val thumbPathAndKey = makeDocumentThumbPathAndKey(doc.document.name, doc.document.thumb)
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(thumbPathAndKey)
                .diskCacheKey(thumbPathAndKey)
                .build(),
            contentDescription = null,
            modifier = Modifier
                // A4 ratio
                .requiredSize(100.dp, 141.dp)
                .align(Alignment.CenterVertically)
                .clickable { onDocClicked(doc) },
            // makes the image too small at first
//            placeholder = painterResource(R.drawable.ic_cloud_queue_24),
            error = painterResource(R.drawable.ic_error_outline),
            contentScale = ContentScale.Fit
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            // date
            Text(
                text = doc.document.date.fmtDtm(LocalContext.current),
                style = MaterialTheme.typography.bodyMedium,
            )

            // title
            if (doc.document.title != null) {
                Text(
                    color = colors.primary,
                    text = doc.document.title,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            // labels
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                for (tag in doc.labelNames) {
                    Chip(tag, { onLabelClicked(tag) })
                }
            }

            // snippet
            if (doc.snippet != null) {
                val formatted = buildAnnotatedString {
                    for (s in doc.snippet
                        .replace('\n', ' ')
                        .split(SNIPPET_SPLIT)) {
                        when {
                            s == "..." -> append('\u2026')

                            s.startsWith(SNIPPET_RESULT) -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(s.substring(1))
                            }

                            else -> append(s)
                        }
                    }
                }

//                var isExpanded: Boolean by remember { mutableStateOf(false) }
                Text(
                    text = formatted,
                    style = MaterialTheme.typography.bodyMedium,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 3, //if (isExpanded) 5 else 1,
//                    modifier = Modifier
//                        .animateContentSize()
//                        .clickable { isExpanded = !isExpanded }
                )
            }

            // infos line
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                // image / PDF
                Icon(
                    when {
                        doc.isPdfDoc -> icons.PictureAsPdf
                        doc.isImagesDoc -> icons.Image
                        else -> icons.Error
                    },
                    contentDescription = null,
                )

                // https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/package-summary#BoxWithConstraints(androidx.compose.ui.Modifier,androidx.compose.ui.Alignment,kotlin.Boolean,kotlin.Function1)

                // page count
                if (doc.document.pageCount > 0) {
                    Text(
                        text = doc.document.pageCount.toString(),
                        modifier = Modifier.widthIn(min = 24.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.End,
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                val dnlStatus = doc.downloadStatus

                // external view button
                AnimatedVisibility(visible = dnlStatus == DownloadState.LOCAL && doc.isPdfDoc) {
                    IconButton(
                        onClick = { onShowClicked(doc) },
                        modifier = Modifier.size(40.dp, 32.dp),
                    ) {
                        Icon(
                            icons.Visibility,
                            contentDescription = null,
                            tint = colors.tertiary,
                        )
                    }
                }

                // share (send) button
                AnimatedVisibility(visible = dnlStatus == DownloadState.LOCAL) {
                    IconButton(
                        onClick = { onShareClicked(doc) },
                        modifier = Modifier.size(40.dp, 32.dp),
                    ) {
                        Icon(
                            icons.Share,
                            contentDescription = null,
                            tint = colors.tertiary,
                        )
                    }
                }

                // download / "show in downloads" button
                // in fullmode, LOCAL is the normal state, not shown in the downloads screen
                // there is nothing to do
                if (!fullMode || dnlStatus != DownloadState.LOCAL) {
                    IconButton(
                        onClick = { onDownloadClicked(doc) },
                        modifier = Modifier.size(40.dp, 32.dp),
                    ) {
                        Icon(
                            when (dnlStatus) {
                                DownloadState.DOWNLOADABLE -> icons.DownloadForOffline
                                DownloadState.LOCAL -> icons.DownloadDone
                                DownloadState.FAILED -> icons.Error
                                DownloadState.IN_PROGRESS -> icons.Downloading
                                DownloadState.QUEUED -> icons.Timer
                            },
                            contentDescription = null,
                            tint = when (dnlStatus) {
                                DownloadState.FAILED -> colors.error
                                DownloadState.LOCAL -> colors.primary
                                else -> colors.secondary
                            },
                        )
                    }
                }

                // size
                Text(
                    text = Formatter.formatFileSize(LocalContext.current, doc.document.size),
                    Modifier.widthIn(min = 64.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
fun Chip(
    txt: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    onLeadingClick: (() -> Unit)? = null,
    onTrailingClick: (() -> Unit)? = null,
    selected: Boolean = true,
) = FilterChip(
    selected = selected,
    onClick = onAction,
    label = { Text(txt) },
    // just to remove the paddings
    modifier = modifier.height(FilterChipDefaults.Height),
    leadingIcon = {
        if (onLeadingClick != null) IconButton(
            onClick = onLeadingClick,
            modifier = Modifier.width(24.dp),
        ) {
            Icon(
                Icons.Outlined.Flip,
                null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    },
    trailingIcon = {
        if (onTrailingClick != null) IconButton(
            onClick = onTrailingClick,
            modifier = Modifier.width(24.dp),
        ) {
            Icon(
                Icons.Outlined.Close,
                null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    },
)


//region preview

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AppTheme {
        DocListContent(
            "zz",
            listOf(DocListVM.LabelFilter("label1"), DocListVM.LabelFilter("label2", false)),
            makeFakeDocuments(5, "none"),
            listOf(LabelType("label1")),
            false,
        )
    }
}

fun makeFakeDocuments(count: Int, nameStart: String): List<DocumentFull> = (0..count).map { makeFakeDocument(it, nameStart) }

fun makeFakeDocument(id: Int, nameStart: String) = DocumentFull(
    Document(
        id,
        "A very nice nice $nameStart $id",
        "The title of the document $id",
        "no_thumb",
        (id * 127) % 99,
        System.currentTimeMillis() + id * 86400,
        System.currentTimeMillis() - id * 86400,
        145,
    ),
    "This is a |§wonderful| snippet",
    listOf(makeFakePart(), makeFakePart(), makeFakePart()),
    if (id % 2 == 0) listOf("A beautiful tag", "like")
    else listOf("like", "A pretty tag", "like", "no other", "tag", "at least", "it", "seems so"),
)

fun makeFakePart() = Part(
    0,
    0,
    "page2.jpg",
    downloadStatus = Random.Default.nextInt(3),
    downloadError = "A nasty error",
)

//endregion preview