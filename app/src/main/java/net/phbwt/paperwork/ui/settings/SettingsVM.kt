@file:OptIn(SavedStateHandleSaveableApi::class)

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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.phbwt.paperwork.data.Repository
import net.phbwt.paperwork.data.background.DownloadWorker
import net.phbwt.paperwork.data.entity.LabelType
import net.phbwt.paperwork.data.settings.Settings
import net.phbwt.paperwork.helper.desc
import net.phbwt.paperwork.helper.firstThenDebounce
import net.phbwt.paperwork.helper.msg
import okio.buffer
import okio.source
import javax.inject.Inject

@HiltViewModel
class SettingsVM @Inject constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val repo: Repository,
    private val settings: Settings,
) : AndroidViewModel(application) {

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

    private val labelInfo = settings.autoDownloadLabels
        .firstThenDebounce(500)
        .map { result ->
            result.mapCatching {
                LabelsInfo(it, repo.db.downloadDao().countAutoDownloadsImpl(it))
            }
        }

    // Editable text fields states are hoisted directly :
    // base URL and auto download labels
    // here we only use the URL validation error and the downloadable count
    val data = combine(
        settings.baseUrl,
        allLabels,
        labelInfo,
        combineWithError(settings.clientPemStr, settings.clientPem),
        combineWithError(settings.serverCaStr, settings.serverCa),
    ) { url, all, info, client, server ->
        SettingsData(
            url.exceptionOrNull().msg(),
            all,
            info.getOrThrow(),
            client,
            server,
        )
    }

    init {
        viewModelScope.launch {
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

    suspend fun startAutoDownloads(info: LabelsInfo) : Int {
        val count = repo.db.downloadDao().queueAutoDownloads(info.counted)
        Log.i(TAG, "Requested $count new downloads for ${info.documentCount} documents")
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
                it?.source()?.buffer()?.readUtf8() ?: "No content ???"
            }
        } catch (ex: Exception) {
            Log.w(TAG, "Failed to read the client certificate and key", ex)
            "Error: ${ex.desc()}"
        }
    }

    private fun combineWithError(
        valueFlow: Flow<String>,
        errorFlow: Flow<Result<Any?>>,
    ): Flow<SettingItem> = combine(valueFlow, errorFlow) { v, e ->
        SettingItem(v, e.exceptionOrNull().msg())
    }

    companion object {
        private const val TAG = "SettingsVM"
    }
}


@Immutable
data class SettingItem(
    val value: String = "",
    val error: String = "",
) {
    val hasError get() = error.isNotEmpty()
}

@Immutable
data class LabelsInfo(
    val counted: List<String> = listOf(),
    val documentCount: Int = 0,
)

@Immutable
data class SettingsData(
    val baseUrlError: String = "",
    val allLabels: List<LabelType> = listOf(),
    val labelsInfo: LabelsInfo = LabelsInfo(),
    val clientPem: SettingItem = SettingItem(),
    val serverCa: SettingItem = SettingItem(),
)
