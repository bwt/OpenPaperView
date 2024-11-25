package net.phbwt.paperwork.ui.downloadlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import net.phbwt.paperwork.data.Repository
import net.phbwt.paperwork.data.background.DownloadWorker
import net.phbwt.paperwork.data.dao.DownloadStats
import net.phbwt.paperwork.data.entity.db.DocumentFull
import net.phbwt.paperwork.data.entity.db.Part
import net.phbwt.paperwork.data.settings.Settings
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

    fun screenData() = combine(
        repo.db.docDao().withDownloads(),
        repo.db.downloadDao().stats(),
        enterAnimDone,
    ) { d, s, e -> DownloadListData(d.toComposeImmutable(), s, e) }

    suspend fun restart(part: Part) {
        repo.db.downloadDao().restartPart(part.partId)
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

data class DownloadListData(
    val downloads: List<DocumentFull> = listOf(),
    val stats: DownloadStats? = null,
    val enterFlashDone: Boolean = false,
)
