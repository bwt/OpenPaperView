@file:OptIn(DelicateCoilApi::class)

package net.phbwt.paperwork.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.room.Room
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.Uri
import coil3.annotation.DelicateCoilApi
import coil3.disk.DiskCache
import coil3.map.Mapper
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.toUri
import coil3.util.DebugLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.phbwt.paperwork.BuildConfig
import net.phbwt.paperwork.data.helper.LocalFetcher
import net.phbwt.paperwork.data.settings.Settings
import net.phbwt.paperwork.helper.combineResultFlows
import net.phbwt.paperwork.helper.firstThenDebounce
import net.phbwt.paperwork.helper.mapResultFlow
import okhttp3.Cache
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import okio.Path.Companion.toOkioPath
import java.io.File
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "Repository"

@Singleton
class Repository @Inject constructor(
    @param:ApplicationContext private val applicationContext: Context,
    private val settings: Settings,
    private val externalScope: CoroutineScope,
) {
    fun dbBuilder(name: String) = Room.databaseBuilder(
        applicationContext,
        AppDatabase::class.java,
        name,
    )

    val db = dbBuilder(applicationContext.currentDbName(true))
        .build()

    // HTTP client used to download content (thumbnails, images, PDF)
    // no cache (will be handled by Coil)
    val contentHttpClient: Flow<Result<OkHttpClient>> by lazy {
        combineResultFlows(settings.clientPem, settings.serverCa, ::buildOkHttpClientWithoutCache)
            .stateIn(externalScope, SharingStarted.Eagerly, null)
            .filterNotNull()
    }

    // HTTP client used only to download the DB
    // build upon the content client + cache
    val dbHttpClient: Flow<Result<OkHttpClient>> by lazy {
        // we only add a cache to let OkHttp handle Etag, Last-Modified, ... HTTP headers
        contentHttpClient
            .mapResultFlow { it.withHttpCache(settings.dbCacheDir) }
            .stateIn(externalScope, SharingStarted.Eagerly, null)
            .filterNotNull()
    }

    val dbUpdateStatus = MutableStateFlow<DbUpdateStatus>(NoUpdate)

    fun dbUpdateReady() {
        dbUpdateStatus.value = UpdateAvailable
    }

    fun dbUpdateFailed(ex: Throwable) {
        dbUpdateStatus.value = UpdateError(ex)
    }

    fun dbUpdateAcknowledged() {
        dbUpdateStatus.value = NoUpdate
    }

    init {
        externalScope.launch {
            combineResultFlows(
                contentHttpClient,
                // we need the first value quickly
                // but we don't want to rebuild on each keystroke in the settings screen
                settings.contentBaseUrl.firstThenDebounce(1000),
            ) { client, baseUrl ->
                buildImageLoader(client, baseUrl)
            }.collect { ilr ->
                ilr.onSuccess {
                    SingletonImageLoader.setUnsafe(it)
                    Log.d(TAG, "Coil init done")
                }.onFailure { ex ->
                    Log.w(TAG, "Cannot init Coil", ex)
                }
            }
        }
    }

    // reuse the same cache when rebuilding
    private val coilDiskCache: DiskCache by lazy {
        DiskCache.Builder()
            .directory(settings.imageCacheDir.toOkioPath())
            .build()
    }

    private fun buildImageLoader(
        okhttp: OkHttpClient,
        baseUrl: HttpUrl
    ): ImageLoader {
        Log.d(TAG, "Build ImageLoader")
        val b = ImageLoader.Builder(applicationContext)
            .diskCache(coilDiskCache)
            .components {
                // data is a relative path, to which we add the base URL
                add(Mapper<String, Uri> { data, _ -> baseUrl.newBuilder().addEncodedPathSegments(data).build().toString().toUri() })
                // try to load from the local documents
                add(LocalFetcher.Factory(settings.localPartsDir))
                add(OkHttpNetworkFetcherFactory(
                    callFactory = { okhttp }
                ))
            }
//            .crossfade(true)

        if (BuildConfig.DEBUG) {
            b.logger(DebugLogger())
        }

        return b.build()
    }

    suspend fun purgeCache() = withContext(Dispatchers.IO) {

        // image caches
        SingletonImageLoader.get(applicationContext).run {
            memoryCache?.clear()
            diskCache?.clear()
        }

        // db cache
        dbHttpClient.first().getOrNull()?.cache?.evictAll()

        // not necessary
        // triggers 2 db downloads (no sure why)
//        settings.dbCache.deleteRecursively()
//        settings.imageCache.deleteRecursively()
    }

    suspend fun purgeDownloaded() = withContext(Dispatchers.IO) {
        settings.localPartsDir.deleteRecursively()
        db.downloadDao().purgeDownloads()
    }
}

fun buildOkHttpClientWithoutCache(
    clientPem: HeldCertificate?,
    serverCa: X509Certificate?,
): OkHttpClient {
    Log.d(TAG, "Build ClientCertificates")
    val clientCertificates = HandshakeCertificates.Builder().apply {
        if (clientPem != null) {
            heldCertificate(clientPem)
        } else {
            Log.w(TAG, "No client certificate")
        }
        if (serverCa != null) {
            Log.i(TAG, "Custom CA")
            addTrustedCertificate(serverCa)
        } else {
            addPlatformTrustedCertificates()
        }
    }.build()

    Log.d(TAG, "Build OkHttpClient WITHOUT cache")
    return OkHttpClient.Builder().apply {
        sslSocketFactory(clientCertificates.sslSocketFactory(), clientCertificates.trustManager)
        // cache handled by Coil
        cache(null)
        if (BuildConfig.DEBUG) {
            addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS))
        }
    }.build()
}

fun OkHttpClient.withHttpCache(
    cacheDir: File,
): OkHttpClient {
    Log.d(TAG, "Adding cache to OkHttpClient")
    cacheDir.mkdirs()
    return newBuilder()
        .cache(Cache(cacheDir, 256 * 1024 * 1024L))
        .build()
}

private const val DB_PREFIX = "documents_"
private const val DB_SUFFIX = ".sqlite"

fun Context.currentDbName(purgeOlder: Boolean = false): String {
    val names = databaseList().filter {
        it.startsWith(DB_PREFIX) && it.endsWith(DB_SUFFIX)
    }.sorted()

    if (purgeOlder) {
        for (n in names.dropLast(1)) {
            Log.d(TAG, "Deleting old DB : '$n'")
            deleteDatabase(n)
        }
    }
    return names.lastOrNull() ?: getDbName(0)
}

private fun Context.currentDbNumber() = currentDbName()
    .removePrefix(DB_PREFIX)
    .removeSuffix(DB_SUFFIX)
    .toInt()

private fun getDbName(count: Int) = "%1\$s%2\$05d%3\$s".format(DB_PREFIX, count, DB_SUFFIX)
fun Context.newDbName() = getDbName(currentDbNumber() + 1)


@Immutable
sealed interface DbUpdateStatus
data object NoUpdate : DbUpdateStatus
data object UpdateAvailable : DbUpdateStatus
data class UpdateError(val error: Throwable) : DbUpdateStatus


