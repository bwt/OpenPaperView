package net.phbwt.paperwork.ui.pairing

import android.app.Application
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.phbwt.paperwork.BuildConfig
import net.phbwt.paperwork.data.Repository
import net.phbwt.paperwork.data.background.DownloadWorker
import net.phbwt.paperwork.data.helper.PairingRunner
import net.phbwt.paperwork.data.helper.PairingRunner.PairingResult
import net.phbwt.paperwork.data.settings.Settings
import net.phbwt.paperwork.helper.latestRelease
import net.phbwt.paperwork.ui.destinations.PairingScreenDestination
import net.phbwt.paperwork.ui.settingscheck.Check
import javax.inject.Inject


@HiltViewModel
class PairingVM @Inject constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val repo: Repository,
    private val settings: Settings,
) : AndroidViewModel(application) {

    val navArgs = PairingScreenDestination.argsFrom(savedStateHandle)

    private val pairingRunner = PairingRunner(application)

    val status = MutableStateFlow<PairingStatus>(Ongoing)

    val uiState = combine(status, pairingRunner.logFlow) { status, log ->
        PairingUiState(status, log)
    }.latestRelease(viewModelScope, PairingUiState())

    private var currentJob: Job? = null

    fun startPairing(force: Boolean = false) {

        if (!force && (currentJob?.isActive == true || status.value == Failed)) {
            Log.e(TAG, "startPairing: ")
            return
        }

        val config = navArgs.qrCode

        if (config == null) {
            // should never happen

            status.value = Failed

            if (BuildConfig.DEBUG) {
                throw IllegalStateException("No QR Code")
            }

            return
        }

        currentJob?.cancel("Restarted")
        currentJob = viewModelScope.launch {
            status.value = Ongoing

            val result = pairingRunner.runPairing(config)

            status.value = if (result != null) Succeeded(result) else Failed
        }
    }

    fun stopPairing() {
        if (currentJob?.isActive == true) {
            currentJob?.cancel("Stopped")
            pairingRunner.jobWasCancelled()
            status.value = Failed
        }
    }

    fun startSync(full: Boolean, result: PairingResult) {
        val labels = if (full) listOf("*") else listOf("")

        viewModelScope.launch {
            settings.updateServerCa(result.config.server.certificate)
            settings.updateClientPem(result.config.client.certificate)
            settings.updateBaseUrl(result.address)
            settings.updateAutoDownloadLabels(labels.joinToString())

//            repo.db.downloadDao().queueAutoDownloads(labels)
            repo.purgeCache()
            repo.purgeDownloaded()
            DownloadWorker.enqueueLoad(getApplication())
        }
    }

}

private const val TAG = "PairingVM"


sealed interface PairingStatus

object Ongoing : PairingStatus
data class Succeeded(val result: PairingResult) : PairingStatus
object Failed : PairingStatus


@Immutable
data class PairingUiState(
    val status: PairingStatus = Ongoing,
    val log: ImmutableList<Check> = persistentListOf(),
)


