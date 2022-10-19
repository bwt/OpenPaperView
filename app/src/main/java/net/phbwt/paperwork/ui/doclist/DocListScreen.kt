@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalAnimationApi::class)

package net.phbwt.paperwork.ui.doclist

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.accompanist.flowlayout.FlowRow
import kotlinx.coroutines.launch
import net.phbwt.paperwork.R
import net.phbwt.paperwork.data.dao.SNIPPET_RESULT
import net.phbwt.paperwork.data.dao.SNIPPET_SPLIT
import net.phbwt.paperwork.data.entity.*
import net.phbwt.paperwork.helper.TrailingClose
import net.phbwt.paperwork.helper.fmtDtm
import net.phbwt.paperwork.helper.startActivitySafely
import net.phbwt.paperwork.ui.theme.AppTheme
import kotlin.random.Random

@Composable
fun DocListScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    vm: DocListVM = hiltViewModel(),
) {
    val search = vm.search
    val labels by vm.labels.collectAsState(listOf())
    val labelTypes by vm.labelTypes.collectAsState(listOf())
    val rows by vm.documentsWithHeaders.collectAsState(listOf())

    BackHandler(enabled = !search.isNullOrBlank() || labels.isNotEmpty()) {
        vm.clearFilters()
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val pdfToast = stringResource(id = R.string.doclist_local_pdf_toast)
    val pdfAction = stringResource(id = R.string.doclist_local_pdf_action)

    DocListContent(
        search,
        labels,
        rows = rows,
        labelTypes,
        onSearchChange = { vm.updateSearch(it) },
        onDocClicked = { doc ->
            if (doc.downloadStatus == DownloadState.LOCAL || doc.isImagesDoc) {
                navController.navigate("pageList/${doc.document.documentId}")
            } else {
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
        },
        onLabelAdded = { vm.addLabel(it) },
        onLabelRemoved = { vm.removeLabel(it) },
        onDownloadClicked = { doc ->
            if (doc.downloadStatus == DownloadState.DOWNLOADABLE) {
                scope.launch { vm.queueDownload(doc.document.documentId) }
            } else {
                navController.navigate("downloadList/${doc.document.documentId}")
            }
        },
        onShowClicked = { doc -> vm.makeDocumentShowIntent(doc).startActivitySafely(context) },
        onShareClicked = { doc ->
            scope.launch {
                vm.makeDocumentShareIntent(doc, ShareCompat.IntentBuilder(context))
                    .startActivitySafely(context)
            }
        },
    )
}

@Composable
fun DocListContent(
    search: String,
    labels: List<String>,
    rows: List<Any>,
    labelTypes: List<LabelType>,
    onSearchChange: (String) -> Unit = {},
    onDocClicked: (DocumentFull) -> Unit = {},
    onLabelAdded: (String) -> Unit = {},
    onLabelRemoved: (String) -> Unit = {},
    onDownloadClicked: (DocumentFull) -> Unit = {},
    onShowClicked: (DocumentFull) -> Unit = {},
    onShareClicked: (DocumentFull) -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme

    Surface(color = colors.background) {
        Column {
            Filters(search, labels, labelTypes, onLabelRemoved, onLabelAdded, onSearchChange)
            DocRows(rows, onDocClicked, onLabelAdded, onDownloadClicked, onShowClicked, onShareClicked)
        }
    }
}


@Composable
fun Filters(
    search: String,
    labels: List<String>,
    labelTypes: List<LabelType>,
    onLabelRemoved: (String) -> Unit = {},
    onLabelAdded: (String) -> Unit = {},
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
        modifier = Modifier.padding(horizontal = 5.dp),
    ) {

        // filter textfield
        OutlinedTextField(
            value = search,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
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
                    DropdownMenuItem(
                        text = { Text(selectionOption.name) },
                        onClick = {
                            onLabelAdded(selectionOption.name)
                            // AFTER adding label
                            onSearchChange("")
                        }
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
                mainAxisSpacing = 5.dp,
                crossAxisSpacing = 5.dp
            ) {
                for (label in labels) {
                    Chip(label, { onLabelRemoved(label) }, closable = true)
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocRows(
    rows: List<Any>,
    onDocClicked: (DocumentFull) -> Unit,
    onLabelClicked: (String) -> Unit,
    onDownloadClicked: (DocumentFull) -> Unit,
    onShowClicked: (DocumentFull) -> Unit,
    onShareClicked: (DocumentFull) -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier.padding(top = 5.dp),
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
                                .fillParentMaxWidth()
                                .animateItemPlacement(),
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

                        Divider(color = colors.primary, thickness = Dp.Hairline)
                        DocRow(
                            row,
                            onDocClicked, onLabelClicked, onDownloadClicked, onShowClicked, onShareClicked,
                            Modifier.animateItemPlacement(),
                        )
                    }
                }
            }
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
        AsyncImage(
            model = doc.document.thumbPath,
            contentDescription = null,
            modifier = Modifier
                // A4 ratio
                .requiredSize(100.dp, 141.dp)
                .align(Alignment.CenterVertically)
                .clickable { onDocClicked(doc) },
            // makes the image too small at first
//            placeholder = painterResource(R.drawable.ic_cloud_queue_24),
            error = painterResource(R.drawable.ic_error_outline_24),
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
            FlowRow(mainAxisSpacing = 5.dp, crossAxisSpacing = 5.dp) {
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
                        else -> icons.QuestionAnswer
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

                // download button
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
                            else -> colors.secondary
                        },
                    )
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
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    closable: Boolean = false,
) = FilterChip(
    selected = true,
    onClick = { onClick(txt) },
    label = { Text(txt) },
    // just to remove the paddings
    modifier = modifier.height(FilterChipDefaults.Height),
    trailingIcon = { if (closable) Icon(Icons.Outlined.Close, null) else null },
)


//region preview

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AppTheme {
        DocListContent(
            "zz", listOf("label1", "label2"),
            makeFakeDocuments(5, "none"),
            listOf(LabelType("label1")),
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
    else listOf("like", "A pretty tag", "like"),
)

fun makeFakePart() = Part(
    0,
    0,
    "page2.jpg",
    downloadStatus = Random.Default.nextInt(3),
    downloadError = "A nasty error",
)

//endregion preview