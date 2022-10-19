package net.phbwt.paperwork.ui.settingscheck

import android.app.Application
import android.net.ConnectivityManager
import android.text.format.Formatter
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.core.content.getSystemService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import net.phbwt.paperwork.data.Repository
import net.phbwt.paperwork.data.background.DownloadWorker
import net.phbwt.paperwork.data.buildOkHttpClientWithoutCache
import net.phbwt.paperwork.data.settings.Settings
import net.phbwt.paperwork.data.withHttpCache
import net.phbwt.paperwork.helper.desc
import okhttp3.Request
import okhttp3.tls.HeldCertificate
import okio.Buffer
import ru.gildor.coroutines.okhttp.await
import java.security.cert.X509Certificate
import javax.inject.Inject


@HiltViewModel
class SettingsCheckVM @Inject constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val repo: Repository,
    private val settings: Settings,
) : AndroidViewModel(application) {

    val data = MutableStateFlow(SettingsCheckState())

    private var checks = persistentListOf<Check>()

    private var currentJob: Job? = null

    fun startChecks() {
        currentJob?.cancel("Restarted")
        checks = checks.clear()
        currentJob = viewModelScope.launch(Dispatchers.IO) {
            setRunning(true)
            try {
                runChecks()
            } catch (ex: Exception) {
                // e.g. network error
                addItem(Check("Failure", Level.Error, ex.desc()))
            }
            setRunning(false)
        }
    }

    fun stopChecks() {
        if (currentJob?.isActive == true) {
            currentJob?.cancel("Stopped")
            addItem(Check("Stopped", Level.Error, null))
            setRunning(false)
        }
    }

    private suspend fun runChecks() {

        val p = checkParameters()

        if (p == null) {
            setParamsOk(false)
            return
        }

        setParamsOk(true)

        checkConnectivity()
        checkDbDownloadWithoutCertificate(p)
        checkDbDownloadWithCertificate(p)
        checkDbDownloadCache(p)

        // we don't need the cache anymore
        settings.checksCache.deleteRecursively()

        setParamsOk(true)
    }

    suspend fun clearDataAndReloadDb() {
        repo.purgeCache()
        repo.purgeDownloaded()
        DownloadWorker.enqueueLoad(getApplication())
    }

    data class P(val clientPem: HeldCertificate?, val serverCa: X509Certificate?, val dbRequest: Request)

    private suspend fun checkParameters(): P? {
        addItem(Check("Checking parameters", Level.None, null))

        delay(300)

        val baseUrl = settings.baseUrl.first()
        if (baseUrl.isFailure) {
            addItem(Check("Base URL", Level.Error, baseUrl.exceptionOrNull().desc()))
            return null
        }

        val dbRequest = settings.dbRequest.first().getOrThrow()
        if (!dbRequest.isHttps) {
            addItem(Check("Base URL is not HTTPS", Level.Warn, "This is not secure"))
        } else {
            addItem(Check("Base URL", Level.OK, "Looks good"))
        }

        val clientPem = settings.clientPem.first()
        when {
            clientPem.isFailure -> {
                addItem(Check("Client certificate", Level.Error, clientPem.exceptionOrNull().desc()))
                return null
            }
            clientPem.getOrThrow() == null -> {
                addItem(Check("No client certificate", Level.Warn, "This is not secure"))
            }
            !dbRequest.isHttps -> {
                addItem(Check("Client certificate over HTTP", Level.Error, "This is inconsistent"))
            }
            else -> {
                addItem(Check("Client certificate", Level.OK, "Looks good"))
            }
        }

        val serverCa = settings.serverCa.first()
        when {
            serverCa.isFailure -> {
                addItem(Check("Server root CA", Level.Error, serverCa.exceptionOrNull().desc()))
                return null
            }
            serverCa.getOrThrow() == null -> {
                addItem(Check("No Server root CA", Level.OK, "The system's CA will be trusted"))
            }
            !dbRequest.isHttps -> {
                addItem(Check("Server CA over HTTP", Level.Error, "This is inconsistent"))
            }
            else -> {
                addItem(Check("Server root CA", Level.OK, "Looks good"))
            }
        }

        return P(clientPem.getOrThrow(), serverCa.getOrThrow(), dbRequest)
    }

    private fun checkConnectivity() {
        val connected = getApplication<Application>().getSystemService<ConnectivityManager>()
            ?.activeNetworkInfo
            ?.isConnected ?: false
        if (!connected) {
            addItem(Check("No network connectivity", Level.Error, null))
        }
    }

    /**
     * Check that the server doesn't allow access if we don't provide the certificate.
     */
    private suspend fun checkDbDownloadWithoutCertificate(p: P) {
        if (p.clientPem != null) {
            addItem(Check("Trying to download the DB without certificate", Level.None, "The server should deny the request"))

            delay(300)

            settings.checksCache.deleteRecursively()
            val httpClient = buildOkHttpClientWithoutCache(null, p.serverCa).withHttpCache(settings.checksCache)

            httpClient.newCall(p.dbRequest).await().use { response ->
                when {
                    response.code in listOf(401, 403) -> {
                        addItem(Check("Response code ${response.code}", Level.OK, "Denied, as expected"))
                    }
                    response.isSuccessful -> {
                        addItem(Check("Response code ${response.code} (expected 401 ot 403)", Level.Error, "Access not denied, the server is NOT properly protected"))
                    }
                    else -> {
                        addItem(Check("Response code ${response.code}", Level.Warn, "Unexpected error (expected 401 ot 403)"))
                    }
                }
            }
        }
    }

    /**
     * Download the DB.
     * TODO : check the SQLite db
     */
    private suspend fun checkDbDownloadWithCertificate(p: P) {
        addItem(Check("Trying to download the DB", Level.None, null))

        delay(300)

        settings.checksCache.deleteRecursively()
        val httpClient = buildOkHttpClientWithoutCache(p.clientPem, p.serverCa).withHttpCache(settings.checksCache)

        httpClient.newCall(p.dbRequest).await().use { response ->
            if (response.isSuccessful) {

                val src = response.body?.source()

                if (src == null) {
                    addItem(Check("Empty response", Level.Error, "Response code ${response.code} without body ???"))
                } else {
                    var size = 0L
                    val buffer = Buffer()
                    while (src.read(buffer, 8192) != -1L) {
                        size += buffer.size
                        buffer.clear()
                    }
                    addItem(Check("Received ${Formatter.formatFileSize(getApplication(), size)} (${size} bytes)", Level.OK, null))
                }
            } else {
                addItem(Check("HTTP error", Level.Error, "Response code ${response.code}"))
            }
        }
    }


    /**
     * Download a second time the DB.
     */
    private suspend fun checkDbDownloadCache(p: P) {
        addItem(Check("Trying to download again the DB", Level.None, "The server should respond that it is not necessary"))

        val httpClient = buildOkHttpClientWithoutCache(p.clientPem, p.serverCa).withHttpCache(settings.checksCache)

        // the Okhttp cache must be populated (by downloading the db)

        httpClient.newCall(p.dbRequest).await().use { response ->
            when {
                !response.isSuccessful -> {
                    addItem(Check("HTTP error", Level.Error, "Response code ${response.code}"))
                }

                response.networkResponse == null -> {
                    // should not happen
                    addItem(Check("No network request", Level.Error, "No network request ?? ${response.code}, '${response.message}'"))
                }

                response.networkResponse?.code == 304 -> {
                    addItem(Check("Response code 304", Level.OK, "'Not modified', as expected"))
                }

                else -> {
                    addItem(Check("Db received again", Level.Error, "The cache control does not work properly"))
                }
            }
        }
    }

    private fun addItem(item: Check) {
        checks += item
        data.value = data.value.copy(items = checks)
    }

    private fun setRunning(v: Boolean) {
        data.value = data.value.copy(running = v)
    }

    private fun setParamsOk(v: Boolean) {
        data.value = data.value.copy(paramsOk = v)
    }
}

@Immutable
data class SettingsCheckState(
    val running: Boolean = false,
    val paramsOk: Boolean = false,
    val items: ImmutableList<Check> = persistentListOf(),
)

enum class Level { OK, Warn, Error, None }

@Immutable
data class Check(
    val desc: String,
    val level: Level,
    val msg: String?,
)