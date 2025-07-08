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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.phbwt.paperwork.R
import net.phbwt.paperwork.data.Repository
import net.phbwt.paperwork.data.background.DownloadWorker
import net.phbwt.paperwork.data.buildOkHttpClientWithoutCache
import net.phbwt.paperwork.data.settings.Settings
import net.phbwt.paperwork.data.withHttpCache
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import okhttp3.tls.HeldCertificate
import okio.Buffer
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
                ensureActive()
                // e.g. network error
                addItem(Msg(R.string.check_failure), Level.Error, Msg(ex))
            } finally {
                setRunning(false)
            }
        }
    }

    fun stopChecks() {
        if (currentJob?.isActive == true) {
            currentJob?.cancel("Stopped")
            addItem(Msg(R.string.check_stopped), Level.Error, null)
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

        delay(900)

        checkConnectivity()
        checkDbDownloadWithoutCertificate(p)
        checkDbDownloadWithCertificate(p)
        checkDbDownloadCache(p)

        // we don't need the cache anymore
        settings.checksCacheDir.deleteRecursively()

        setParamsOk(true)
    }

    suspend fun clearDataAndReloadDb() {
        repo.purgeCache()
        repo.purgeDownloaded()
        DownloadWorker.enqueueLoad(getApplication())
    }

    data class P(val clientPem: HeldCertificate?, val serverCa: X509Certificate?, val dbRequest: Request)

    private suspend fun checkParameters(): P? {
        addItem(Msg(R.string.check_checking_parameters), Level.None, null)

        delay(100)

        val baseUrl = settings.baseUrl.first()
        if (baseUrl.isFailure) {
            addItem(Msg(R.string.check_base_url), Level.Error, Msg(baseUrl.exceptionOrNull()))
            return null
        }

        val dbRequest = settings.dbRequest.first().getOrThrow()
        if (!dbRequest.isHttps) {
            addItem(Msg(R.string.check_base_url_is_not_https), Level.Warn, Msg(R.string.check_this_is_not_secure))
        } else {
            addItem(Msg(R.string.check_base_url), Level.OK, Msg(R.string.check_looks_good))
        }

        val clientPem = settings.clientPem.first()
        when {
            clientPem.isFailure -> {
                addItem(Msg(R.string.check_client_certificate), Level.Error, Msg(clientPem.exceptionOrNull()))
                return null
            }

            clientPem.getOrThrow() == null -> {
                addItem(Msg(R.string.check_no_client_certificate), Level.Warn, Msg(R.string.check_this_is_not_secure))
            }

            !dbRequest.isHttps -> {
                addItem(Msg(R.string.check_client_certificate_over_http), Level.Error, Msg(R.string.check_this_is_inconsistent))
            }

            else -> {
                addItem(Msg(R.string.check_client_certificate), Level.OK, Msg(R.string.check_looks_good))
            }
        }

        val serverCa = settings.serverCa.first()
        when {
            serverCa.isFailure -> {
                addItem(Msg(R.string.check_server_root_ca), Level.Error, Msg(serverCa.exceptionOrNull()))
                return null
            }

            serverCa.getOrThrow() == null -> {
                addItem(Msg(R.string.check_no_server_root_ca), Level.OK, Msg(R.string.check_the_systems_ca_will_be_trusted))
            }

            !dbRequest.isHttps -> {
                addItem(Msg(R.string.check_server_ca_over_http), Level.Error, Msg(R.string.check_this_is_inconsistent))
            }

            else -> {
                addItem(Msg(R.string.check_server_root_ca), Level.OK, Msg(R.string.check_looks_good))
            }
        }

        return P(clientPem.getOrThrow(), serverCa.getOrThrow(), dbRequest)
    }

    private fun checkConnectivity() {
        val connected = getApplication<Application>().getSystemService<ConnectivityManager>()
            ?.activeNetworkInfo
            ?.isConnected ?: false
        if (!connected) {
            addItem(Msg(R.string.check_no_network_connectivity), Level.Error, null)
        }
    }

    /**
     * Check that the server doesn't allow access if we don't provide the certificate.
     */
    private suspend fun checkDbDownloadWithoutCertificate(p: P) {
        if (p.clientPem != null) {
            addItem(Msg(R.string.check_dnl_db_without_certificate), Level.None, Msg(R.string.check_the_server_should_deny_the_request))

            delay(300)

            settings.checksCacheDir.deleteRecursively()
            val httpClient = buildOkHttpClientWithoutCache(null, p.serverCa).withHttpCache(settings.checksCacheDir)

            httpClient.newCall(p.dbRequest).executeAsync().use { response ->
                withContext(Dispatchers.IO) {
                    when {
                        response.code in listOf(401, 403) -> {
                            addItem(Msg(R.string.check_response_code_1, response.code), Level.OK, Msg(R.string.check_denied_as_expected))
                        }

                        response.isSuccessful -> {
                            addItem(
                                Msg(R.string.check_response_code_expected_401_or_403_1, response.code),
                                Level.Error,
                                Msg(R.string.check_access_not_denied_server_not_secure)
                            )
                        }

                        else -> {
                            addItem(
                                Msg(R.string.check_response_code_1, response.code),
                                Level.Warn,
                                Msg(R.string.check_unexpected_error_expected_401_or_403)
                            )
                        }
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
        addItem(Msg(R.string.check_trying_to_download_the_db), Level.None, null)

        delay(300)

        settings.checksCacheDir.deleteRecursively()

        // size of the actual content of the network response (without transparent decompression)
        var compressedSizeFromEvent = 0L

        val httpClient = buildOkHttpClientWithoutCache(p.clientPem, p.serverCa).withHttpCache(settings.checksCacheDir)
            .newBuilder()
            .eventListener(object : EventListener() {
                override fun responseBodyEnd(call: Call, byteCount: Long) {
                    // this should always work
                    // (as opposed to using the Content-Length header from the network response)
                    compressedSizeFromEvent = byteCount
                }
            })
            .build()

        httpClient.newCall(p.dbRequest).executeAsync().use { response ->
            withContext(Dispatchers.IO) {

                if (response.isSuccessful) {
                    // HTTP OK

                    val src = response.body.source()

                    var size = 0L
                    val buffer = Buffer()

                    while (src.read(buffer, 8192) != -1L) {
                        size += buffer.size
                        buffer.clear()
                    }
                    addItem(
                        Msg(R.string.check_received_size_bytes_2, Formatter.formatFileSize(getApplication(), size), size.toString()),
                        Level.OK,
                        null
                    )

                    // AFTER the response body has been read, so that the responseBodyEnd event has been triggered

                    val networkResponse = response.networkResponse

                    if (networkResponse == null || networkResponse.code == 304) {
                        // should not happen, we cleared the cache
                        addItem(
                            Msg(R.string.check_response_was_cached_desc),
                            Level.Warn,
                            Msg(R.string.check_response_was_cached_msg),
                        )
                    } else if (networkResponse.header("Content-Encoding") != "gzip") {
                        // not compressed
                        val contentType = networkResponse.header("Content-Type").toString()
                        addItem(
                            Msg(R.string.check_response_was_not_compressed_desc),
                            Level.Warn,
                            Msg(R.string.check_response_was_not_compressed_msg, contentType),
                        )
                    } else {
                        // compressed

                        val compressedSizeFromHeader = networkResponse.header("Content-Length")?.toLongOrNull() ?: -1

                        if (compressedSizeFromHeader > 0 && compressedSizeFromHeader != compressedSizeFromEvent) {
                            Log.e(TAG, "Network size mismatch : Content-Length $compressedSizeFromHeader, Event $compressedSizeFromEvent")
                        }

                        addItem(
                            Msg(
                                R.string.check_compressed_response,
                                Formatter.formatFileSize(getApplication(), compressedSizeFromEvent),
                                compressedSizeFromEvent.toString(),
                            ),
                            Level.OK,
                            null
                        )
                    }
                } else {
                    // HTTP error
                    addItem(Msg(R.string.check_http_error), Level.Error, Msg(R.string.check_response_code_1, response.code))
                }
            }
        }
    }


    /**
     * Download a second time the DB.
     */
    private suspend fun checkDbDownloadCache(p: P) {
        addItem(Msg(R.string.check_trying_to_download_again_the_db), Level.None, Msg(R.string.check_the_server_should_repond_no_necessary))

        val httpClient = buildOkHttpClientWithoutCache(p.clientPem, p.serverCa).withHttpCache(settings.checksCacheDir)

        // the Okhttp cache must be populated (by downloading the db)

        httpClient.newCall(p.dbRequest).executeAsync().use { response ->
            withContext(Dispatchers.IO) {

                when {
                    !response.isSuccessful -> {
                        addItem(Msg(R.string.check_http_error), Level.Error, Msg(R.string.check_response_code_1, response.code))
                    }

                    response.networkResponse == null -> {
                        // should not happen
                        addItem(
                            Msg(R.string.check_no_network_response),
                            Level.Error,
                            Msg(R.string.check_no_network_response_2, response.code.toString(), response.message)
                        )
                    }

                    response.networkResponse?.code == 304 -> {
                        addItem(Msg(R.string.check_response_code_1, 304), Level.OK, Msg(R.string.check_not_modified_as_expected))
                    }

                    else -> {
                        addItem(Msg(R.string.check_db_received_again), Level.Error, Msg(R.string.check_the_cache_control_does_not_work_properly))
                    }
                }
            }
        }
    }

    private fun addItem(item: Check) {
        checks += item
        data.value = data.value.copy(items = checks)
    }

    private fun addItem(
        desc: Msg,
        level: Level,
        msg: Msg?,
    ) = addItem(Check(desc, level, msg))

    private fun setRunning(v: Boolean) {
        data.value = data.value.copy(running = v)
    }

    private fun setParamsOk(v: Boolean) {
        data.value = data.value.copy(paramsOk = v)
    }
}

private const val TAG = "SettingsCheckVM"

@Immutable
data class SettingsCheckState(
    val running: Boolean = false,
    val paramsOk: Boolean = false,
    val items: ImmutableList<Check> = persistentListOf(),
)


