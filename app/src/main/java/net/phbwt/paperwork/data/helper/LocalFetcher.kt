package net.phbwt.paperwork.data.helper

import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import okio.Path.Companion.toOkioPath
import java.io.File

/**
 * Coil fetcher looking in the downloaded documents
 */
class LocalFetcher(private val data: File) : Fetcher {

    override suspend fun fetch(): FetchResult {
        return SourceResult(
            source = ImageSource(file = data.toOkioPath()),
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(data.extension),
            dataSource = DataSource.DISK
        )
    }

    class Factory(private val baseDir: File) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val k = options.diskCacheKey
            return if (k == null) {
                null
            } else {
                // FIXME what if the document is currently being downloaded ?
                // currently we only check if the file has not modified recently
                val p = File(baseDir, k)
                if (p.exists() && p.lastModified() < System.currentTimeMillis() - CACHE_DELAY_MILLIS) {
                    Log.d(TAG, "Found '$k' in local data")
                    LocalFetcher(p)
                } else {
                    null
                }
            }
        }
    }

    companion object {
        private const val TAG = "LocalFetcher"
        private const val CACHE_DELAY_MILLIS = 60 * 1000L
    }
}