package net.phbwt.paperwork.data.background

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.collection.mutableIntSetOf
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.hilt.work.HiltWorker
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.*
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import net.phbwt.paperwork.BuildConfig
import net.phbwt.paperwork.R
import net.phbwt.paperwork.data.AppDatabase
import net.phbwt.paperwork.data.Repository
import net.phbwt.paperwork.data.entity.db.isThumb
import net.phbwt.paperwork.data.entity.db.makeDocumentThumbPathAndKey
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

    val notificationBuilder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_download)
        .setContentTitle(applicationContext.getString(R.string.download_title))
        .setContentText(applicationContext.getString(R.string.download_body))
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .setAutoCancel(false)
        .addAction(
            R.drawable.ic_cancel,
            applicationContext.getString(R.string.download_cancel),
            WorkManager.getInstance(applicationContext).createCancelPendingIntent(id),
        )
        .setOnlyAlertOnce(true)
        .setLocalOnly(true)

    // A long running worker (> 10mn) requires a ForegroundService,
    // which in turn requires a notification
    // but it will be visible on modern devices (> 33)
    // only if we have the POST_NOTIFICATIONS permission
    override suspend fun getForegroundInfo(): ForegroundInfo = ForegroundInfo(
        NOTIFICATION_ID,
        notificationBuilder.build(),
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
    )

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "Working")

        setForeground(getForegroundInfo())

        if (BuildConfig.DEBUG && DEBUG_NETWORK) {
            Log.e(TAG, "Adding work delay")
            delay(500 + (Random.Default.nextLong() % 500))
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
                        val newDb = downloadDb(body.source().inputStream(), newDbName)

                        newDb.withTransaction {
                            cleanupLocalFiles(newDb)
                        }

                        prepareAutoDownloads(newDb)

                        newDb.close()
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

    private suspend fun downloadDb(from: InputStream, newDbName: String): AppDatabase = withContext(Dispatchers.IO) {

        Log.i(TAG, "Download and check the new DB")

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

        newDb
    }

    private suspend fun cleanupLocalFiles(newDb: AppDatabase) = withContext(Dispatchers.IO) {

        Log.i(TAG, "Sync local state and DB")

        val ts = SystemClock.elapsedRealtime()
        var thumbs = 0
        var parts = 0
        var errors = 0
        var deleted = 0

        // update the status of the already downloaded parts
        var depth = 0
        for (f in settings.localPartsDir.walkTopDown()
            .onEnter { depth += 1; true }
            .onLeave { depth -= 1 }
        ) {
            if (!f.isFile) {
                continue
            }

            if (depth != 2) {
                Log.e(TAG, "Structure problem : $f, $depth")
                continue
            }

            if (f.length() == 0L) {
                Log.e(TAG, "Empty file : $f")

                // TODO : if this is a thumbnail, also delete the document (in order to trigger a new download)
                // and / or update the DB accordingly
                f.delete()

                errors++
                continue
            }

            if (f.isThumb()) {
                // OK, keep it
                thumbs++
                continue
            }

            val count = newDb.downloadDao().setPartDone(f.parentFile!!.name, f.name)
            when (count) {
                1 -> {
                    // OK
                    parts++
                }

                0 -> {
                    Log.i(TAG, "Deleting disappeared part $f")
                    f.delete()
                    deleted++
                }

                else -> {
                    Log.e(TAG, "Db inconsistency for file $f : $count")
                }
            }

        }

        Log.i(TAG, "Files cleaned : $parts parts, $thumbs thumbs, $errors errors, $deleted deleted in ${SystemClock.elapsedRealtime() - ts} ms")
    }

    private suspend fun prepareAutoDownloads(newDb: AppDatabase) = withContext(Dispatchers.IO) {
        // the actual downloads will be triggered
        // after switching to the new DB

        val labels = settings.autoDownloadLabels.first().getOrNull()
        if (labels != null) {
            val count = newDb.downloadDao().queueAutoDownloads(labels)
            Log.i(TAG, "Will auto download $count parts for labels ${labels.joinToString()}")
        }
    }

    private suspend fun downloadParts() {
        val dao = repo.db.downloadDao()
        val notificationManager = applicationContext.getSystemService<NotificationManager>() ?: throw IllegalStateException()
        val canNotify = Build.VERSION.SDK_INT < 33
                || applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        val clearedCount = dao.retryStuckDownloads()

        if (clearedCount > 0) {
            Log.w(TAG, "Retrying $clearedCount stuck downloads")
        }

        val doneDocuments = mutableIntSetOf()
        val initialCount = dao.countRemainingDownloads()
        var downloadPartCount = 0
        var downloadThumbCount = 0
        var errorCount = 0

        while (true) {

            if (canNotify && initialCount > 5 && (downloadPartCount + errorCount > 1)) {
                val remaining = dao.countRemainingDownloads()

                notificationManager.notify(
                    NOTIFICATION_ID,
                    notificationBuilder
                        .setProgress(downloadPartCount + remaining, downloadPartCount, false)
                        .build(),
                )
            }

            val dnl = dao.loadFirstDownloadable()
                ?: break

            val partId = dnl.part.partId

            if (BuildConfig.DEBUG && DEBUG_NETWORK) {
                Log.e(TAG, "Adding pre download delay")
                delay(510 + (Random.Default.nextLong() % 490))
            }

            // the documents thumb
            val thumbPathAndKey = makeDocumentThumbPathAndKey(dnl.documentName, dnl.documentThumb)
            val thumbDest = File(settings.localPartsDir, thumbPathAndKey)
            val docId = dnl.part.documentId
            var thumbAvailable = true
            if (!doneDocuments.add(docId)) {
                // retry only once per document per run
                Log.d(TAG, "Document $docId already processed for thumb : $thumbPathAndKey")
            } else if (thumbDest.exists()) {
                Log.d(TAG, "Thumb already exists : $thumbPathAndKey")
            } else {
                try {
                    downloadPartOrThumbnail(thumbPathAndKey, thumbDest)
                    downloadThumbCount++
                } catch (ex: Exception) {
                    Log.e(TAG, "Failed to download thumb $thumbPathAndKey", ex)

                    // fails the part, so that both will be retried
                    dao.setPartFailed(partId, "Thumb failed : ${ex.desc()}")

                    thumbAvailable = false
                    errorCount++
                }
            }

            // the pdf / image
            if (thumbAvailable) {
                val partPathAndKey = dnl.partPathAndKey()
                val partDest = File(settings.localPartsDir, partPathAndKey)
                try {
                    downloadPartOrThumbnail(partPathAndKey, partDest)
                    val setAsDone = dao.setPartDone(partId)

                    if (!setAsDone) {
                        // e.g. canceled by the user while in progress
                        partDest.delete()
                    }

                    downloadPartCount++
                } catch (ex: Exception) {
                    Log.e(TAG, "Failed to download part $partPathAndKey", ex)
                    dao.setPartFailed(partId, ex.desc())
                    partDest.delete()
                    errorCount++
                }
            }
        }

        Log.i(TAG, "Parts downloads : $downloadPartCount parts, $downloadThumbCount thumbs, $errorCount errors")
    }

    @OptIn(ExperimentalCoilApi::class)
    private suspend fun downloadPartOrThumbnail(pathAndKey: String, dest: File) {
        dest.parentFile?.mkdirs()

        // Try first to find the image in Coil's cache

        SingletonImageLoader.get(applicationContext).diskCache?.openSnapshot(pathAndKey)?.use {
            Log.d(TAG, "Copying ${pathAndKey} from cache")
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

        Log.d(TAG, "Downloading ${pathAndKey}")

        var url = "$baseUrl/${pathAndKey}"

        if (BuildConfig.DEBUG && DEBUG_NETWORK) {
            // error injection
            if (Random.Default.nextInt() % 3 == 0) {
                Log.e(TAG, "Injecting an error")
                url += "_injected_error"
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

                    if (BuildConfig.DEBUG && DEBUG_NETWORK) {
                        Log.e(TAG, "Adding copy delay")
                        delay(520 + (Random.Default.nextLong() % 480))
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

        // add delay and inject errors
        private const val DEBUG_NETWORK = false

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
                    ).build()
            )
        }
    }
}

