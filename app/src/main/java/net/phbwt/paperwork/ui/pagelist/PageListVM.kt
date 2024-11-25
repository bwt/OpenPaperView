@file:OptIn(ExperimentalCoroutinesApi::class)

package net.phbwt.paperwork.ui.pagelist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import net.phbwt.paperwork.data.Repository
import net.phbwt.paperwork.data.entity.db.DocumentFull
import net.phbwt.paperwork.data.settings.Settings
import net.phbwt.paperwork.helper.latestRelease
import net.phbwt.paperwork.ui.destinations.PageListScreenDestination
import java.io.File
import javax.inject.Inject


@HiltViewModel
class PageListVM @Inject constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val repo: Repository,
    private val settings: Settings,
) : AndroidViewModel(application) {

    val navArgs = PageListScreenDestination.argsFrom(savedStateHandle)

    val document = repo.db.docDao()
        .loadDocument(navArgs.documentId)
        .latestRelease(viewModelScope, null)

    fun getPdfLocalPath(doc: DocumentFull?) = if (doc != null) File(settings.localPartsDir, doc.partPath(0)) else null

}
