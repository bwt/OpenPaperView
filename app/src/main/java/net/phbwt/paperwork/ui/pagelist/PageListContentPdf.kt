@file:OptIn(ExperimentalFoundationApi::class, ExperimentalFoundationApi::class, ExperimentalFoundationApi::class)

package net.phbwt.paperwork.ui.pagelist

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.memory.MemoryCache
import coil.request.ImageRequest
import com.google.accompanist.pager.pagerTabIndicatorOffset
import kotlinx.coroutines.launch
import net.phbwt.paperwork.R
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

    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val pageCount = renderer.state?.pageCount ?: 0

        if (pageCount > 1) {
            ScrollableTabRow(
                // Our selected tab is our current page
                selectedTabIndex = pagerState.currentPage,
                // Override the indicator, using the provided pagerTabIndicatorOffset modifier
                indicator = { tabPositions: List<TabPosition> ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.pagerTabIndicatorOffset(pagerState, tabPositions),
                    )
                }
            ) {
                for (index in 0 until pageCount) {
                    Tab(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = { Text("Page ${index + 1}") },
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.scrollToPage(index) } },
                    )
                }
            }
        }

        val imageCache = LocalContext.current.imageLoader.memoryCache ?: throw IllegalStateException("Coil image loader should have a cache")

        BoxWithConstraints {
            val width = with(LocalDensity.current) { maxWidth.toPx() }.toInt()
            val height = with(LocalDensity.current) { maxHeight.toPx() }.toInt()

            HorizontalPager(
                modifier = Modifier
//                    .weight(1f)
                    .fillMaxSize(),
                state = pagerState,
                pageCount = pageCount,
                key = { it },
            ) { index ->

                var scale by remember { mutableStateOf(1f) }
                var rotation by remember { mutableStateOf(0f) }
                var offset by remember { mutableStateOf(Offset.Zero) }

                // separate box : clip + gesture not impacted by scale (especially touchSlop)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RectangleShape)
                        .pointerInput(Unit) {
                            appDetectTransformGestures(true) { centroid, pan, baseZoom, _ ->
                                val newScale = minOf(99f, maxOf(1f, scale * baseZoom))
                                val zoom = newScale / scale
                                scale = newScale

                                val o = centroid - ((centroid - offset) * zoom)

                                val mx = minOf(0f, maxOf(size.width * (1 - scale), pan.x + o.x))
                                val my = minOf(0f, maxOf(size.height * (1 - scale), pan.y + o.y))

                                offset = Offset(mx, my)
                                // FIXME gesture end rebound
                                scale > 1.05f
                            }
                        },
                ) {
                    val cacheKey = MemoryCache.Key("$pdfFile-$index}")
                    var bitmap by remember { mutableStateOf(imageCache[cacheKey]?.bitmap) }
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
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y,
                                    transformOrigin = TransformOrigin(0f, 0f),
                                )
                                .fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            error = painterResource(R.drawable.ic_error_outline_24),
                        )
                    }
                }
            }
        }
    }
}

private const val TAG = "PageListContentPdf"


