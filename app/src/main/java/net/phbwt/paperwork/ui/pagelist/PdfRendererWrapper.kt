package net.phbwt.paperwork.ui.pagelist

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import coil.memory.MemoryCache
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

@Stable
class PdfRendererWrapper {

    // PdfRenderer are not threadsafe
    // A Mutex does not guaranty that the thread is always the same
    // FIXME make it global ?
    private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    private var renderer: PdfRenderer? = null

    var state: State? by mutableStateOf(null)

    suspend fun open(newFile: File) = withContext(dispatcher) {
        Log.d(TAG, "Opening $newFile")
        if (newFile != state?.file) {
            Log.d(TAG, "Really opening $newFile")
            closeImp()
            renderer = PdfRenderer(ParcelFileDescriptor.open(newFile, ParcelFileDescriptor.MODE_READ_ONLY))
            state = State(newFile, renderer!!.pageCount)
        }
    }

    suspend fun renderPage(
        pageIndex: Int,
        width: Int,
        height: Int,
        destClip: Rect?,
        transform: Matrix?,
        imageCache: MemoryCache,
        cacheKey: MemoryCache.Key,
    ): Bitmap = withContext(dispatcher) {
        Log.e(TAG, ">> $pageIndex")

        val r = renderer ?: throw IllegalStateException("Missing open() ?")

        val ts = System.currentTimeMillis()

        val bm: Bitmap

        r.openPage(pageIndex).use { page ->

            val screenRatio = height.toFloat() / width
            val pageRatio = page.height.toFloat() / page.width

            val bmWidth: Int
            val bmHeight: Int

            if (pageRatio > screenRatio) {
                bmWidth = (width / pageRatio * screenRatio).toInt()
                bmHeight = height
            } else {
                bmWidth = width
                bmHeight = (height * pageRatio / screenRatio).toInt()
            }

            bm = Bitmap.createBitmap(bmWidth, bmHeight, Bitmap.Config.ARGB_8888)
            bm.eraseColor(Color.White.toArgb())

            page.render(
                bm,
                destClip,
                transform,
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
            )
        }

        // If the page was cancelled during the rendering, the bitmap will not be used
        // so we put it in cache ourself
        imageCache[cacheKey] = MemoryCache.Value(bm)

        Log.e(TAG, "<< $pageIndex in ${System.currentTimeMillis() - ts} ms")
        bm
    }

    // TODO : explicitly release the renderer when we are done, instead of relying on finalize()
    // we want it to run when the scope is cancelled but also to be a suspend fun because we want to run it on the single thread dispatcher
    suspend fun close() = withContext(dispatcher) {
        closeImp()
    }

    private fun closeImp() {
        if (renderer != null) {
            Log.d(TAG, "Closing $state")
            renderer?.close()
            renderer = null
        }
        state = null
    }
}

@Immutable
data class State(val file: File, val pageCount: Int)

private const val TAG = "PdfRendererWrapper"