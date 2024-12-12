@file:OptIn(SavedStateHandleSaveableApi::class, ExperimentalCoroutinesApi::class)

package net.phbwt.paperwork.ui.settings

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.phbwt.paperwork.data.Repository
import net.phbwt.paperwork.data.background.DownloadWorker
import net.phbwt.paperwork.data.dao.AutoDownloadInfo
import net.phbwt.paperwork.data.entity.db.LabelType
import net.phbwt.paperwork.data.entity.pairing.QrCodeContent
import net.phbwt.paperwork.data.helper.PairingRunner
import net.phbwt.paperwork.data.settings.MAX_VALUE_SIZE
import net.phbwt.paperwork.data.settings.Settings
import net.phbwt.paperwork.helper.combine6
import net.phbwt.paperwork.helper.desc
import net.phbwt.paperwork.helper.firstThenDebounce
import net.phbwt.paperwork.helper.msg
import okio.buffer
import okio.source
import javax.inject.Inject
import kotlin.math.min

@HiltViewModel
class SettingsVM @Inject constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val repo: Repository,
    private val settings: Settings,
) : AndroidViewModel(application) {

    private var currentPairingJob: Job? = null
    val pairingRunner = PairingRunner(application)

    //region editable fields hoisting

    var baseUrl by savedStateHandle.saveable { mutableStateOf("") }
        private set

    var autoDownloadLabels by savedStateHandle.saveable(
        stateSaver = TextFieldValue.Saver,
    ) {
        mutableStateOf(TextFieldValue())
    }
        private set

    //endregion

    private val allLabels: Flow<List<LabelType>> = repo.db.labelDao().loadLabelTypes()

    private val labelInfo: Flow<LabelsInfo> = settings.autoDownloadLabels
        .firstThenDebounce(500)
        .flatMapLatest { result ->
            if (result.isFailure) {
                // this should not happen
                Log.e(TAG, "Failed to get the label list setting ???")
                flowOf(LabelsInfo(listOf("*bug*", result.exceptionOrNull()?.msg() ?: "")))
            } else {
                val labels = result.getOrThrow()
                repo.db.downloadDao().countAutoDownloads(labels)
                    .map { info -> LabelsInfo(labels, info) }
            }
        }

    // Editable text fields states are hoisted directly :
    // base URL and auto download labels
    // here we only use the URL validation error and the downloadable count
    val data = combine6(
        pairingRunner.pairingStatus,
        settings.baseUrl,
        allLabels,
        labelInfo,
        combine(settings.clientPemStr, settings.clientPem) { txt, certResult ->
            val certInfo = certResult.mapCatching { it?.certificate.toString() }
            SettingItem(txt, certInfo.getOrNull(), certInfo.exceptionOrNull()?.msg())
        },
        combine(settings.serverCaStr, settings.serverCa) { txt, certResult ->
            val certInfo = certResult.mapCatching { it?.toString() }
            SettingItem(txt, certInfo.getOrNull(), certInfo.exceptionOrNull()?.msg())
        },
    ) { pairing, url, all, info, client, server ->
        SettingsData(
            pairing,
            url.exceptionOrNull().msg(),
            all,
            info,
            client,
            server,
        )
    }.stateIn(
        viewModelScope,
        // restarting after the QR-Code capture is slow
        SharingStarted.Lazily,
        SettingsData(),
    )

    init {
        viewModelScope.launch {
            // init hoisted fields
            baseUrl = settings.baseUrlStr.first()
            autoDownloadLabels = TextFieldValue(settings.autoDownloadLabelsStr.first())
        }
    }

    fun updateBaseUrl(newVal: String) {
        baseUrl = newVal
        viewModelScope.launch {
            settings.updateBaseUrl(newVal)
        }
    }

    fun updateAutoDownloadLabels(newVal: TextFieldValue, wasCompleted: Boolean) {
        val newText = newVal.text.trimStart()

        autoDownloadLabels = if (!wasCompleted) {
            newVal
        } else {
            // after a completion, move the cursor to the end
            newVal.copy(
                text = newText,
                selection = TextRange(newText.length),
            )
        }

        viewModelScope.launch {
            settings.updateAutoDownloadLabels(newText)
        }
    }

    suspend fun startAutoDownloads(info: LabelsInfo): Int {
        val count = repo.db.downloadDao().queueAutoDownloads(info.labels)
        Log.i(TAG, "Requested $count new downloads for ${info.autoDownloads} documents")
        DownloadWorker.enqueueLoad(getApplication())
        return count
    }

    fun updateClientPem(newVal: String) = viewModelScope.launch {
        settings.updateClientPem(newVal)
    }

    fun updateClientPem(uri: Uri) = viewModelScope.launch {
        updateClientPem(loadFromUri(uri))
    }

    fun updateServerCa(newVal: String) = viewModelScope.launch {
        settings.updateServerCa(newVal)
    }

    fun updateServerCa(uri: Uri) = viewModelScope.launch {
        updateServerCa(loadFromUri(uri))
    }

    private suspend fun loadFromUri(newVal: Uri): String = withContext(Dispatchers.IO) {
        try {
            getApplication<Application>().contentResolver.openInputStream(newVal).use {
                // FIXME this seems quite complicated
                // There is probably a simpler way to read min(content_size, a_reasonable_value)

                val source = it?.source()?.buffer() ?: return@withContext "No content ???"
                val maxLen = MAX_VALUE_SIZE.toLong()
                source.request(maxLen + 1)
                val readLen = source.buffer.size

                if (readLen > maxLen) {
                    "Error : too long (max size : $maxLen)"
                } else {
                    source.readUtf8(min(readLen, maxLen))
                }
            }
        } catch (ex: Exception) {
            Log.w(TAG, "Failed to read the client certificate and key", ex)
            "Error: ${ex.desc()}"
        }
    }

    /** Try to connect to one of the addresses */
    fun startPairing(config: QrCodeContent) {
        Log.d(TAG, "Starting pairing")
        currentPairingJob?.cancel("Restarted")
        currentPairingJob = viewModelScope.launch {
            val result = pairingRunner.runPairing(config)

            if (result != null) {
                settings.updateServerCa(result.config.server.certificate)
                settings.updateClientPem(result.config.client.certificate)
                settings.updateBaseUrl(result.address)
                // also the hoisted field
                baseUrl = result.address
            }
        }
    }

    fun endPairing() {
        currentPairingJob?.cancel("Cancelled")
        currentPairingJob = null
        pairingRunner.pairingCanceled()
    }

    fun startSync(full: Boolean) {
        val labels = if (full) listOf("*") else listOf("")

        autoDownloadLabels = TextFieldValue(labels.joinToString())

        viewModelScope.launch {
            settings.updateAutoDownloadLabels(labels.joinToString())
//            repo.db.downloadDao().queueAutoDownloads(labels)
            repo.purgeCache()
            repo.purgeDownloaded()
            DownloadWorker.enqueueLoad(getApplication())
        }
    }


}

private const val TAG = "SettingsVM"


@Immutable
data class SettingItem(
    val inputValue: String = "",
    val value: String? = null,
    val error: String? = null,
)

@Immutable
data class LabelsInfo(
    val labels: List<String> = listOf(),
    val autoDownloads: AutoDownloadInfo = AutoDownloadInfo(0, 0),
)


@Immutable
data class SettingsData(
    val pairingStatus: PairingRunner.PairingStatus? = null,
    val baseUrlError: String = "",
    val allLabels: List<LabelType> = listOf(),
    val labelsInfo: LabelsInfo = LabelsInfo(),
    val clientPem: SettingItem = SettingItem(),
    val serverCa: SettingItem = SettingItem(),
)


