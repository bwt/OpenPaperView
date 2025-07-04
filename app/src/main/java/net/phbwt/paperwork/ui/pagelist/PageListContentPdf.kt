@file:OptIn(ExperimentalFoundationApi::class)

package net.phbwt.paperwork.ui.pagelist

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import coil3.toBitmap
import kotlinx.coroutines.launch
import net.phbwt.paperwork.R
import net.phbwt.paperwork.helper.animateBy
import net.phbwt.paperwork.helper.appDetectTransformGestures
import java.io.File


@Composable
fun PageListContentPdf(
    pdfFile: File,
) {
    val renderer = remember(Unit) { PdfRendererWrapper() }

    LaunchedEffect(pdfFile) {
        renderer.open(pdfFile)
    }

    val pageCount = renderer.state?.pageCount ?: 0
    val pagerState = rememberPagerState(initialPage = 0) { pageCount }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {

        if (pageCount > 1) {
            ScrollableTabRow(
                // Our selected tab is our current page
                selectedTabIndex = pagerState.currentPage,
            ) {
                for (index in 0 until pageCount) {
                    Tab(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = { Text(stringResource(R.string.pages_page_1, index + 1)) },
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.scrollToPage(index) } },
                    )
                }
            }
        }

        val imageCache = LocalContext.current.imageLoader.memoryCache
            ?: throw IllegalStateException("Coil image loader should have a cache")

        BoxWithConstraints {
            val width = with(LocalDensity.current) { maxWidth.toPx() }.toInt()
            val height = with(LocalDensity.current) { maxHeight.toPx() }.toInt()

            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
                scale *= zoomChange
                offset += offsetChange
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { it },
            ) { index ->

                // another box : clipped zone includes navigation bar padding
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RectangleShape),
                ) {
                    // image wrapped in a box : gesture not impacted by scale (especially touchSlop)
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            // edge2edge : bottom
                            .navigationBarsPadding()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = { tapOffset ->
                                        if (scale > 1.00001f) {
                                            scope.launch {
                                                transformableState.animateBy(1 / scale, -offset)
                                            }
                                        } else {
                                            val zoomBy = 3f
                                            val o = tapOffset - ((tapOffset - offset) * zoomBy)
                                            scope.launch {
                                                transformableState.animateBy(zoomBy, o)
                                            }
                                        }
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                appDetectTransformGestures(true) { centroid, pan, baseZoom, _ ->
                                    val newScale = minOf(99f, maxOf(1f, scale * baseZoom))
                                    val zoomBy = newScale / scale
                                    scale = newScale

                                    val o = centroid - ((centroid - offset) * zoomBy)

                                    val mx = minOf(0f, maxOf(size.width * (1 - scale), pan.x + o.x))
                                    val my = minOf(0f, maxOf(size.height * (1 - scale), pan.y + o.y))

                                    offset = Offset(mx, my)
                                    // FIXME gesture end rebound
                                    scale > 1.05f
                                }
                            },
                    ) {
                        val cacheKey = MemoryCache.Key("$pdfFile-$index}")
                        var bitmap by remember { mutableStateOf(imageCache[cacheKey]?.image?.toBitmap()) }
                        if (bitmap == null) {
                            LaunchedEffect(pdfFile, index) {
                                Log.w(TAG, "> $index")
                                bitmap = renderer.renderPage(
                                    index,
                                    width, height,
                                    null,
                                    null,
                                    imageCache,
                                    cacheKey,
                                )
                                Log.w(TAG, "< $index")
                            }
                            Icon(
                                Icons.Outlined.Timer,
                                null,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .fillMaxSize(.3f),
                                tint = MaterialTheme.colorScheme.outline,
                            )
                        } else {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
//                                .size(width, height)
                                    .memoryCacheKey(cacheKey)
                                    .data(bitmap)
                                    .build(),
                                contentDescription = "Page ${index + 1} of ${pageCount}",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        translationX = offset.x
                                        translationY = offset.y
                                        transformOrigin = TransformOrigin(0f, 0f)
                                    },
                                contentScale = ContentScale.Fit,
                                error = painterResource(R.drawable.ic_error_outline),
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val TAG = "PageListContentPdf"


