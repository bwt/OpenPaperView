package net.phbwt.paperwork.ui.settings

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import net.phbwt.paperwork.data.Repository
import net.phbwt.paperwork.data.settings.Settings
import net.phbwt.paperwork.helper.desc
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

    // TextField state should not be hold in a StateFlow
    // see https://medium.com/androiddevelopers/effective-state-management-for-textfield-in-compose-d6e5b070fbe5
    // The initial value comes from a flow.
    // Instead of doing it properly (hoisting the state here, like the DocList's search)
    // we use an ugly hack : trying to update the flow very fast

    //    private val baseUrlBypass1 = MutableStateFlow<String?>(null)
    private val baseUrlBypass2 = Channel<String>()
//    private val baseUrlBypass3 = MutableSharedFlow<String>()

    //    private val baseUrl = merge(baseUrlBypass1.filterNotNull(), settings.baseUrlStr)
    private val baseUrl = merge(baseUrlBypass2.receiveAsFlow(), settings.baseUrlStr)
//    private val baseUrl = merge(baseUrlBypass3, settings.baseUrlStr)

    val data = combine(
        combineWithError(baseUrl, settings.baseUrl),
        combineWithError(settings.clientPemStr, settings.clientPem),
        combineWithError(settings.serverCaStr, settings.serverCa),
        ::SettingsData
    )

    fun fastUpdate(newVal: String) {
//        baseUrlBypass1.value = newVal
        baseUrlBypass2.trySend(newVal)
//        baseUrlBypass3.tryEmit(newVal)
    }

    suspend fun updateBaseUrl(newVal: String) {
//        baseUrlBypass1.value = newVal
//        baseUrlBypass2.send(newVal)
//        baseUrlBypass3.emit(newVal)

        settings.updateBaseUrl(newVal)
    }

    suspend fun updateClientPem(newVal: String) = settings.updateClientPem(newVal)

    suspend fun updateClientPem(uri: Uri) = updateClientPem(loadFromUri(uri))

    suspend fun updateServerCa(newVal: String) = settings.updateServerCa(newVal)

    suspend fun updateServerCa(uri: Uri) = updateServerCa(loadFromUri(uri))

    private suspend fun loadFromUri(newVal: Uri): String = withContext(Dispatchers.IO) {
        try {
            getApplication<Application>().contentResolver.openInputStream(newVal).use {
                it?.source()?.buffer()?.readUtf8() ?: "No content ???"
            }
        } catch (ex: Exception) {
            Log.w(TAG, "Failed to read the client cetificate and key", ex)
            "Error: ${ex.desc()}"
        }
    }

    private fun combineWithError(
        valueFlow: Flow<String>,
        errorFlow: Flow<Result<Any?>>,
    ): Flow<SettingItem> = combine(valueFlow, errorFlow) { v, e ->
        SettingItem(v, e.exceptionOrNull()?.message ?: "")
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
