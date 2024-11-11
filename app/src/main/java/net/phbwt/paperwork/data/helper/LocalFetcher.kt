package net.phbwt.paperwork.data.helper

import android.util.Log
import android.webkit.MimeTypeMap
import coil3.ImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.io.File

/**
 * Coil fetcher looking in the downloaded documents
 */
class LocalFetcher(private val data: File) : Fetcher {

    override suspend fun fetch(): FetchResult {
        return SourceFetchResult(
            source = ImageSource(file = data.toOkioPath(), FileSystem.SYSTEM),
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

                // 0 is the file does not exists
                val lastModified = p.lastModified()

                if (0 < lastModified && lastModified < System.currentTimeMillis() - CACHE_DELAY_MILLIS) {
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