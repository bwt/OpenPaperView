package net.phbwt.paperwork.ui.settingscheck

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.text.format.Formatter
import androidx.annotation.StringRes
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
import net.phbwt.paperwork.R
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
                addItem(Check(Msg(R.string.check_failure), Level.Error, Msg(ex)))
            }
            setRunning(false)
        }
    }

    fun stopChecks() {
        if (currentJob?.isActive == true) {
            currentJob?.cancel("Stopped")
            addItem(Check(Msg(R.string.check_stopped), Level.Error, null))
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
        addItem(Check(Msg(R.string.check_checking_parameters), Level.None, null))

        delay(300)

        val baseUrl = settings.baseUrl.first()
        if (baseUrl.isFailure) {
            addItem(Check(Msg(R.string.check_base_url), Level.Error, Msg(baseUrl.exceptionOrNull())))
            return null
        }

        val dbRequest = settings.dbRequest.first().getOrThrow()
        if (!dbRequest.isHttps) {
            addItem(Check(Msg(R.string.check_base_url_is_not_https), Level.Warn, Msg(R.string.check_this_is_not_secure)))
        } else {
            addItem(Check(Msg(R.string.check_base_url), Level.OK, Msg(R.string.check_looks_good)))
        }

        val clientPem = settings.clientPem.first()
        when {
            clientPem.isFailure -> {
                addItem(Check(Msg(R.string.check_client_certificate), Level.Error, Msg(clientPem.exceptionOrNull())))
                return null
            }
            clientPem.getOrThrow() == null -> {
                addItem(Check(Msg(R.string.check_no_client_certificate), Level.Warn, Msg(R.string.check_this_is_not_secure)))
            }
            !dbRequest.isHttps -> {
                addItem(Check(Msg(R.string.check_client_certificate_over_http), Level.Error, Msg(R.string.check_this_is_inconsistent)))
            }
            else -> {
                addItem(Check(Msg(R.string.check_client_certificate), Level.OK, Msg(R.string.check_looks_good)))
            }
        }

        val serverCa = settings.serverCa.first()
        when {
            serverCa.isFailure -> {
                addItem(Check(Msg(R.string.check_server_root_ca), Level.Error, Msg(serverCa.exceptionOrNull())))
                return null
            }
            serverCa.getOrThrow() == null -> {
                addItem(Check(Msg(R.string.check_no_server_root_ca), Level.OK, Msg(R.string.check_the_systems_ca_will_be_trusted)))
            }
            !dbRequest.isHttps -> {
                addItem(Check(Msg(R.string.check_server_ca_over_http), Level.Error, Msg(R.string.check_this_is_inconsistent)))
            }
            else -> {
                addItem(Check(Msg(R.string.check_server_root_ca), Level.OK, Msg(R.string.check_looks_good)))
            }
        }

        return P(clientPem.getOrThrow(), serverCa.getOrThrow(), dbRequest)
    }

    private fun checkConnectivity() {
        val connected = getApplication<Application>().getSystemService<ConnectivityManager>()
            ?.activeNetworkInfo
            ?.isConnected ?: false
        if (!connected) {
            addItem(Check(Msg(R.string.check_no_network_connectivity), Level.Error, null))
        }
    }

    /**
     * Check that the server doesn't allow access if we don't provide the certificate.
     */
    private suspend fun checkDbDownloadWithoutCertificate(p: P) {
        if (p.clientPem != null) {
            addItem(Check(Msg(R.string.check_dnl_db_without_certificate), Level.None, Msg(R.string.check_the_server_should_deny_the_request)))

            delay(300)

            settings.checksCache.deleteRecursively()
            val httpClient = buildOkHttpClientWithoutCache(null, p.serverCa).withHttpCache(settings.checksCache)

            httpClient.newCall(p.dbRequest).await().use { response ->
                when {
                    response.code in listOf(401, 403) -> {
                        addItem(Check(Msg(R.string.check_response_code_1, response.code), Level.OK, Msg(R.string.check_denied_as_expected)))
                    }
                    response.isSuccessful -> {
                        addItem(Check(Msg(R.string.check_response_code_expected_401_or_403_1, response.code), Level.Error, Msg(R.string.check_access_not_denied_server_not_secure)))
                    }
                    else -> {
                        addItem(Check(Msg(R.string.check_response_code_1, response.code), Level.Warn, Msg(R.string.check_unexpected_error_expected_401_or_403)))
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
        addItem(Check(Msg(R.string.check_trying_to_download_the_db), Level.None, null))

        delay(300)

        settings.checksCache.deleteRecursively()
        val httpClient = buildOkHttpClientWithoutCache(p.clientPem, p.serverCa).withHttpCache(settings.checksCache)

        httpClient.newCall(p.dbRequest).await().use { response ->
            if (response.isSuccessful) {

                val src = response.body?.source()

                if (src == null) {
                    addItem(Check(Msg(R.string.check_empty_response), Level.Error, Msg(R.string.check_response_code_without_body_1, response.code)))
                } else {
                    var size = 0L
                    val buffer = Buffer()
                    while (src.read(buffer, 8192) != -1L) {
                        size += buffer.size
                        buffer.clear()
                    }
                    addItem(Check(Msg(R.string.check_received_size_bytes_2, Formatter.formatFileSize(getApplication(), size), size.toString()), Level.OK, null))
                }
            } else {
                addItem(Check(Msg(R.string.check_http_error), Level.Error, Msg(R.string.check_response_code_1, response.code)))
            }
        }
    }


    /**
     * Download a second time the DB.
     */
    private suspend fun checkDbDownloadCache(p: P) {
        addItem(Check(Msg(R.string.check_trying_to_download_again_the_db), Level.None, Msg(R.string.check_the_server_should_repond_no_necessary)))

        val httpClient = buildOkHttpClientWithoutCache(p.clientPem, p.serverCa).withHttpCache(settings.checksCache)

        // the Okhttp cache must be populated (by downloading the db)

        httpClient.newCall(p.dbRequest).await().use { response ->
            when {
                !response.isSuccessful -> {
                    addItem(Check(Msg(R.string.check_http_error), Level.Error, Msg(R.string.check_response_code_1, response.code)))
                }

                response.networkResponse == null -> {
                    // should not happen
                    addItem(Check(Msg(R.string.check_no_network_response), Level.Error, Msg(R.string.check_no_network_response_2, response.code.toString(), response.message)))
                }

                response.networkResponse?.code == 304 -> {
                    addItem(Check(Msg(R.string.check_response_code_1, 304), Level.OK, Msg(R.string.check_not_modified_as_expected)))
                }

                else -> {
                    addItem(Check(Msg(R.string.check_db_received_again), Level.Error, Msg(R.string.check_the_cache_control_does_not_work_properly)))
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
    val desc: Msg,
    val level: Level,
    val msg: Msg?,
)

@Immutable
class Msg(
    @StringRes
    val res: Int,
    vararg val args: String,
) {
    constructor(ex: Throwable?) : this(R.string.check_exception_1, ex.desc())
    constructor(@StringRes res: Int, v: Int) : this(res, v.toString())

    fun format(ctxt: Context) = ctxt.getString(res, *args)
}