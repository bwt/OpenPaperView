@file:OptIn(ExperimentalFoundationApi::class)

package net.phbwt.paperwork.ui.pagelist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.pager.pagerTabIndicatorOffset
import kotlinx.coroutines.launch
import net.phbwt.paperwork.R
import net.phbwt.paperwork.data.entity.DocumentFull
import net.phbwt.paperwork.helper.animateBy
import net.phbwt.paperwork.helper.appDetectTransformGestures


@Composable
fun PageListContentImages(
    document: DocumentFull?,
    selectedIndex: Int,
) {
    if (document != null && document.parts.isNotEmpty()) {

        val pages = document.parts

        val pagerState = rememberPagerState(initialPage = selectedIndex)
        val scope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (document.parts.size > 1) {
                ScrollableTabRow(
                    // Our selected tab is our current page
                    selectedTabIndex = pagerState.currentPage,
                    // Override the indicator, using the provided pagerTabIndicatorOffset modifier
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.pagerTabIndicatorOffset(pagerState, tabPositions),
                        )
                    }
                ) {
                    pages.forEachIndexed { index, _ ->
                        Tab(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            text = { Text("Page ${index + 1}") },
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.scrollToPage(index) } },
                        )
                    }
                }
            }

            HorizontalPager(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                //   .disabledHorizontalPointerInputScroll()
                state = pagerState,
                pageCount = pages.size,
                key = { pages[it].partId },
//                contentPadding = PaddingValues(horizontal = 25.dp),
            ) { index ->

                var scale by remember { mutableStateOf(1f) }
                var rotation by remember { mutableStateOf(0f) }
                var offset by remember { mutableStateOf(Offset.Zero) }

                val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
                    scale *= zoomChange
                    offset += offsetChange
                }

                // separate box : clip + gesture not impacted by scale (especially touchSlop)
                Box(
                    modifier = Modifier
                        .clip(RectangleShape)
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
                    val dataAndCacheKey = document.partPath(index)
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(dataAndCacheKey)
                            .diskCacheKey(dataAndCacheKey)
                            .build(),
                        contentDescription = null,
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
                        placeholder = painterResource(R.drawable.ic_cloud_queue_24),
                        error = painterResource(R.drawable.ic_error_outline_24),
                    )
                }
            }
        }
    }

}

private val HorizontalScrollConsumer = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource) = available.copy(y = 0f)
    override suspend fun onPreFling(available: Velocity) = available.copy(y = 0f)
}

fun Modifier.disabledHorizontalPointerInputScroll(disabled: Boolean = true) =
    if (disabled) this.nestedScroll(HorizontalScrollConsumer) else this

private const val TAG = "PageListContentImages"