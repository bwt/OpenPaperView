@file:OptIn(SavedStateHandleSaveableApi::class)

package net.phbwt.paperwork.ui.settings

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
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

    //endregion


    // Editable text fields states are hoisted directly :
    // base URL and auto download labels
    // here we only use the URL validation error and the downloadable count
    val data = combine(
        combineWithError(baseUrl, settings.baseUrl),
        combineWithError(settings.clientPemStr, settings.clientPem),
        combineWithError(settings.serverCaStr, settings.serverCa),
        ::SettingsData
        )
    }

    init {
        viewModelScope.launch {
            baseUrl = settings.baseUrlStr.first()
        }
    }

    fun updateBaseUrl(newVal: String) {
        baseUrl = newVal
        viewModelScope.launch {
            settings.updateBaseUrl(newVal)
        }
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
data class SettingsData(
    val baseUrl: SettingItem = SettingItem(),
    val clientPem: SettingItem = SettingItem(),
    val serverCa: SettingItem = SettingItem(),
)
