@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package net.phbwt.paperwork.ui.downloadlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.withContext
import net.phbwt.paperwork.data.Repository
import net.phbwt.paperwork.data.background.DownloadWorker
import net.phbwt.paperwork.data.dao.DownloadStats
import net.phbwt.paperwork.data.entity.db.DocumentFull
import net.phbwt.paperwork.data.entity.db.Part
import net.phbwt.paperwork.data.settings.Settings
import net.phbwt.paperwork.helper.latestRelease
import net.phbwt.paperwork.helper.toComposeImmutable
import net.phbwt.paperwork.ui.destinations.DownloadListScreenDestination
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DownloadListVM @Inject constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val settings: Settings,
    private val repo: Repository,
) : AndroidViewModel(application) {

    val navArgs = DownloadListScreenDestination.argsFrom(savedStateHandle)

    private val enterAnimDone = savedStateHandle.getStateFlow(ENTER_ANIM_DONE, false)

    private val downloads: Flow<Downloads> = settings.isFullDownload.flatMapLatest { fullMode ->
        when {
            // should not happen
            fullMode.isFailure -> throw IllegalStateException("Could not get the full download setting")
            // full download mode, show the missing documents
            fullMode.getOrThrow() -> repo.db.docDao().notFullyDownloaded()
                // throttling, otherwise the UI is jerky
                .sample(300)
            // the downloaded documents
            else -> repo.db.docDao().withDownloads()
        }.map {
            Downloads(fullMode.getOrThrow(), it.toComposeImmutable())
        }
    }

    val screenData = combine(
        downloads,
        repo.db.downloadDao().stats(),
        enterAnimDone,
    ) { d, s, e -> DownloadListData(d, s, e) }
        .latestRelease(viewModelScope, DownloadListData())

    suspend fun restart(part: Part) {
        repo.db.downloadDao().queueDownloadForPart(part.partId)
        DownloadWorker.enqueueLoad(getApplication())
    }

    suspend fun restart(doc: DocumentFull) {
        repo.db.downloadDao().queueDownloadForDocument(doc.document.documentId)
        DownloadWorker.enqueueLoad(getApplication())
    }

    suspend fun clear(doc: DocumentFull) = withContext(Dispatchers.IO) {
        repo.db.downloadDao().setDocumentCleared(doc.document.documentId)
        File(settings.localPartsDir, doc.docPath).deleteRecursively()
    }

    fun setEnterFlashDone() {
        savedStateHandle[ENTER_ANIM_DONE] = true
    }
}

private const val ENTER_ANIM_DONE = "enter_anim_done"

data class Downloads(
    val fullMode: Boolean = false,
    val documents: List<DocumentFull> = listOf(),
)

data class DownloadListData(
    val downloads: Downloads = Downloads(),
    val stats: DownloadStats? = null,
    val enterFlashDone: Boolean = false,
)
