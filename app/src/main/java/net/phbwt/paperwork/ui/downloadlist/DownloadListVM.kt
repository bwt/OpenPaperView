package net.phbwt.paperwork.ui.downloadlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.phbwt.paperwork.data.Repository
import net.phbwt.paperwork.data.background.DownloadWorker
import net.phbwt.paperwork.data.entity.DocumentFull
import net.phbwt.paperwork.data.entity.Part
import net.phbwt.paperwork.data.settings.Settings
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DownloadListVM @Inject constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val settings: Settings,
    private val repo: Repository,
) : AndroidViewModel(application) {

    fun downloads() = repo.db.docDao().withDownloads()

    suspend fun restart(part: Part) {
        repo.db.downloadDao().restartPart(part.partId)
        DownloadWorker.enqueueLoad(getApplication())
    }

    suspend fun clear(doc: DocumentFull) = withContext(Dispatchers.IO) {
        repo.db.downloadDao().setDocumentCleared(doc.document.documentId)
        File(settings.localPartsDir, doc.docPath).deleteRecursively()
    }

    companion object {
        private const val TAG = "DownloadListVM"
    }

}
