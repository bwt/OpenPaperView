@file:OptIn(ExperimentalCoroutinesApi::class)

package net.phbwt.paperwork.ui.pagelist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import net.phbwt.paperwork.data.Repository
import net.phbwt.paperwork.data.entity.DocumentFull
import net.phbwt.paperwork.data.settings.Settings
import net.phbwt.paperwork.helper.latestRelease
import net.phbwt.paperwork.ui.main.PARAM_DOCUMENT_ID
import java.io.File
import javax.inject.Inject


@HiltViewModel
class PageListVM @Inject constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val repo: Repository,
    private val settings: Settings,
) : AndroidViewModel(application) {

    val document = savedStateHandle.getStateFlow(PARAM_DOCUMENT_ID, -1)
        .flatMapLatest {
            require(it >= 0) { "missing documentId" }
            repo.db.docDao().loadDocument(it)
        }.latestRelease(viewModelScope, null)

    fun getPdfLocalPath(doc: DocumentFull?) = if (doc != null) File(settings.localPartsDir, doc.partPath(0)) else null

}
