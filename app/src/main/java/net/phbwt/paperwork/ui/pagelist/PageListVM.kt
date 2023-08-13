package net.phbwt.paperwork.ui.pagelist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import net.phbwt.paperwork.data.Repository
import net.phbwt.paperwork.data.entity.DocumentFull
import net.phbwt.paperwork.data.settings.Settings
import java.io.File
import javax.inject.Inject


@HiltViewModel
class PageListVM @Inject constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val repo: Repository,
    private val settings: Settings,
) : AndroidViewModel(application) {

    fun loadDocument(id: Int) = repo.db.docDao().loadDocument(id)

    fun getPdfLocalPath(doc: DocumentFull?) = if (doc != null) File(settings.localPartsDir, doc.partPath(0)) else null

}
