package net.phbwt.paperwork.ui.main

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import net.phbwt.paperwork.data.Repository
import net.phbwt.paperwork.data.background.DownloadWorker
import net.phbwt.paperwork.data.settings.Settings
import javax.inject.Inject


@HiltViewModel
class MainVM @Inject constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val repo: Repository,
    private val settings: Settings,
) : AndroidViewModel(application) {

    val dbUpdates = repo.dbUpdateStatus

    val isConfigured = settings.baseUrlStr.map { it.isNotBlank() }

    fun clearDbUpdate() = repo.dbUpdateAcknowledged()

    suspend fun setDemoServer() {
        settings.updateBaseUrl("https://bwtdev.eu/OpenPaperViewDemo")
        settings.updateAutoDownloadLabels("label 2, some_other_label")
        DownloadWorker.enqueueLoad(getApplication())
    }

    // https://stackoverflow.com/questions/6609414/how-do-i-programmatically-restart-an-android-app
    fun restartApplication() {
        val context = getApplication<Application>()
        val packageManager: PackageManager = context.packageManager
        val componentName = packageManager.getLaunchIntentForPackage(context.packageName)!!.component
        val restartIntent = Intent.makeRestartActivityTask(componentName)
        context.startActivity(restartIntent)
        Runtime.getRuntime().exit(0)
    }

    // so that we can make the Play Store's Pre-launch report a bit more useful
    fun isRunningInTestLab() = android.provider.Settings.System.getString(
        getApplication<Application>().contentResolver,
        "firebase.test.lab",
    ).toBoolean()

}
