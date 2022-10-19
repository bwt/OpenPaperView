package net.phbwt.paperwork.data.background

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_DEFERRED
import androidx.hilt.work.HiltWorker
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.*
import coil.Coil
import coil.annotation.ExperimentalCoilApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import net.phbwt.paperwork.BuildConfig
import net.phbwt.paperwork.R
import net.phbwt.paperwork.data.Repository
import net.phbwt.paperwork.data.entity.Download
import net.phbwt.paperwork.data.newDbName
import net.phbwt.paperwork.data.settings.Settings
import net.phbwt.paperwork.helper.desc
import okhttp3.Request
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import okio.buffer
import okio.sink
import ru.gildor.coroutines.okhttp.await
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random


@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val settings: Settings,
    private val repo: Repository,
) : CoroutineWorker(context, workerParameters) {

    override suspend fun getForegroundInfo(): ForegroundInfo = ForegroundInfo(
        NOTIFICATION_ID,
        NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_download_24)
            .setContentTitle(applicationContext.getString(R.string.download_title))
            .setContentText(applicationContext.getString(R.string.download_body))
            .setForegroundServiceBehavior(FOREGROUND_SERVICE_DEFERRED)
            .setPriority(NotificationCompat.PRIORITY_LOW)
//            .setOngoing(true)
            .build()
    )

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "Working")

//        setForeground(getForegroundInfo())

        if (BuildConfig.DEBUG) {
            Log.e(TAG, "Adding work delay")
            delay(2000 + (Random.Default.nextLong() % 1000))
        }

        downloadParts()
        checkAndDownloadDb()

        Result.success()
    }

    private suspend fun checkAndDownloadDb() {
        val httpClient = repo.dbHttpClient.first().getOrNull() ?: return
        val dbRequest = settings.dbRequest.first().getOrNull() ?: return

        httpClient.newCall(dbRequest).await().use { response ->

            when {
                !response.isSuccessful -> {
                    Log.w(TAG, "Could not download DB : HTTP code ${response.code}, '${response.message}'")
                }

                response.networkResponse == null -> {
                    // should not happen
                    Log.e(TAG, "No network request ?? ${response.code}, '${response.message}'")
                }

                response.networkResponse?.code == 304 -> {
                    Log.i(TAG, "No new DB")
                }

                else -> {
                    Log.i(TAG, "New DB available")
                    val body = response.body ?: throw IOException("No body")
                    val newDbName = applicationContext.newDbName()
                    try {
                        downloadAndPrepareDb(body.source().inputStream(), newDbName)
                        repo.dbUpdateReady()
                    } catch (ex: Exception) {
                        Log.e(TAG, "New DB failed", ex)
                        applicationContext.deleteDatabase(newDbName)
                        repo.dbUpdateFailed(ex)
                    }
                }
            }
        }
    }

    private suspend fun downloadAndPrepareDb(from: InputStream, newDbName: String) = withContext(Dispatchers.IO) {

        // It seems that a corrupted db file may be silently deleted
        // and a DB created from scratch
        // specially if there is no PrepackagedDatabaseCallback
        val downloadedDbWasOpened = AtomicBoolean(false)
        val newDb = repo.dbBuilder(newDbName)
            .createFromInputStream({ from }, object : RoomDatabase.PrepackagedDatabaseCallback() {
                override fun onOpenPrepackagedDatabase(db: SupportSQLiteDatabase) {
                    downloadedDbWasOpened.set(true)
                }
            })
            .build()

        // Triggers the actual download, verification and migration
        newDb.downloadDao().loadFirstDownloadable()

        if (!downloadedDbWasOpened.get()) {
            throw IllegalStateException("Db not processed")
        }

        // update the status of the already downloaded parts
        var depth = 0
        for (f in settings.localPartsDir.walkTopDown()
            .onEnter { depth += 1; true }
            .onLeave { depth -= 1 }
        ) {
            if (depth != 2 && f.isFile) {
                Log.e(TAG, "Structure problem : $f, $depth")
            }
            if (f.isFile) {
                val count = newDb.downloadDao().setPartDone(f.parentFile!!.name, f.name)
                when (count) {
                    1 -> {
                        // OK
                    }
                    0 -> {
                        Log.i(TAG, "Deleting disappeared part $f")
                        f.delete()
                    }
                    else -> {
                        Log.e(TAG, "Db inconsistency for file $f : $count")
                    }
                }
            }
        }

        newDb.close()
    }

    private suspend fun downloadParts() {
        val dao = repo.db.downloadDao()

        val clearedCount = dao.clearStuckDownloads()

        if (clearedCount > 0) {
            Log.w(TAG, "Cleared $clearedCount stuck downloads")
        }

        var downloadCount = 0
        var errorCount = 0

        while (true) {
            val dnl = repo.db.downloadDao().loadFirstDownloadable()
                ?: break

            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Adding pre download delay")
                delay(2000 + (Random.Default.nextLong() % 1000))
            }

            val dest = File(settings.localPartsDir, dnl.partPath())
            try {
                downloadPart(dnl, dest)
                val setAsDone = dao.setPartDone(dnl.part.partId)

                if (!setAsDone) {
                    // e.g. canceled by the user while in progress
                    dest.delete()
                }

                downloadCount++
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to download ${dnl.partPath()}", ex)
                dao.setPartFailed(dnl.part.partId, ex.desc())
                dest.delete()
                errorCount++
            }
        }

        Log.i(TAG, "Parts downloads : $downloadCount ok, $errorCount errors")
    }

    @OptIn(ExperimentalCoilApi::class)
    private suspend fun downloadPart(dnl: Download, dest: File) {
        dest.parentFile?.mkdirs()

        // Try first to find the image in Coil's cache

        val cacheKey = dnl.partPath()
        Coil.imageLoader(applicationContext).diskCache?.get(cacheKey)?.use {
            Log.d(TAG, "Copying ${dnl.partPath()} from cache")
            withContext(Dispatchers.IO) {
                FileSystem.SYSTEM.copy(it.data, dest.toOkioPath())
//                it.data.toNioPath().source().use { a ->
//                    dest.sink().buffer().use { b -> b.writeAll(a) }
//                }
            }
            return
        }

        // Not found, download it

        val httpClient = repo.contentHttpClient.first().getOrNull() ?: return
        val baseUrl = settings.contentBaseUrl.first().getOrNull() ?: return

        Log.d(TAG, "Downloading ${dnl.partPath()}")

        var url = "$baseUrl/${dnl.partPath()}"
        if (BuildConfig.DEBUG) {
            // error injection
            if (Random.Default.nextInt() % 3 == 0) {
                Log.e(TAG, "Injecting an error")
                url += "error"
            }
        }

        val request = Request.Builder()
            .url(url)
            .build()

        httpClient.newCall(request).await().use { response ->

            if (!response.isSuccessful) {
                var msg = "HTTP ${response.code}"

                if (response.message.isNotBlank()) {
                    msg += " : ${response.message}"
                }
                throw IOException(msg)
            }

            val body = response.body ?: throw IOException("No body")

            withContext(Dispatchers.IO) {
                // see https://github.com/square/okio/issues/501
                dest.sink().buffer().use { sink ->

                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Adding copy delay")
                        delay(2000 + (Random.Default.nextLong() % 1000))
                    }

                    sink.writeAll(body.source())
                }
            }
        }
    }

    companion object {
        private const val TAG = "DownloadWorker"
        private const val CHANNEL_ID = TAG
        private const val NOTIFICATION_ID = 352
        private const val WORKER_NAME = "downloadPartsWorker"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= 26) {
                val name = context.getString(R.string.download_channel_name)
                val descriptionText = context.getString(R.string.download_channel_desc)
                val importance = NotificationManager.IMPORTANCE_LOW
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }
                val notificationManager: NotificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

        internal fun enqueueLoad(context: Context) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Enqueuing download job")
            }
            val workManager = WorkManager.getInstance(context)

            workManager.enqueueUniqueWork(
                WORKER_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                OneTimeWorkRequestBuilder<DownloadWorker>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .build()
            )
        }
    }
}

