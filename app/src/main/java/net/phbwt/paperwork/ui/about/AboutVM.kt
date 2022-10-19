package net.phbwt.paperwork.ui.about

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import net.phbwt.paperwork.data.Repository
import javax.inject.Inject


@HiltViewModel
class AboutVM @Inject constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val repo: Repository,
) : AndroidViewModel(application) {

    fun getDbVersion() = repo.db.openHelper.readableDatabase.version
}


