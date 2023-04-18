@file:OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)

package net.phbwt.paperwork.ui.doclist

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import net.phbwt.paperwork.BuildConfig
import net.phbwt.paperwork.R
import net.phbwt.paperwork.data.Repository
import net.phbwt.paperwork.data.background.DownloadWorker
import net.phbwt.paperwork.data.entity.Document
import net.phbwt.paperwork.data.entity.DocumentFull
import net.phbwt.paperwork.data.entity.Part
import net.phbwt.paperwork.data.settings.Settings
import net.phbwt.paperwork.helper.ComposeImmutableList
import net.phbwt.paperwork.helper.latestRelease
import java.io.*
import java.util.*
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject


@OptIn(SavedStateHandleSaveableApi::class)
@HiltViewModel
class DocListVM @Inject constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val repo: Repository,
    private val settings: Settings,
) : AndroidViewModel(application) {

    // we want a state, a StateFlow is not sync enough
    // cf https://medium.com/androiddevelopers/effective-state-management-for-textfield-in-compose-d6e5b070fbe5
    var search by savedStateHandle.saveable { mutableStateOf("") }
        private set

    val labelFilters = savedStateHandle.getStateFlow("labels", listOf<LabelFilter>())

    fun updateSearch(v: String) {
        // replace with easier to access characters
        search = v.map { chr ->
            when (chr) {
                '.' -> '*'

                // Other Punctuation, not mapped to *
                '"', '\'' -> '"'
                '\u2032', '\u2033', '\u2034',
                '\u2035', '\u2036', '\u2037',
                '\uff02', '\uff07',
                '\u3003' -> '"'

                else -> when (chr.category) {
                    CharCategory.INITIAL_QUOTE_PUNCTUATION,
                    CharCategory.FINAL_QUOTE_PUNCTUATION -> '"'

                    CharCategory.OTHER_PUNCTUATION -> '*'

                    else -> chr
                }
            }
        }.joinToString("")
    }

    private fun updateLabel(v: List<LabelFilter>) {
        savedStateHandle["labels"] = v
    }

    fun addLabel(newLabel: String) {
        val v = labelFilters.value
        if (!v.any { it.label == newLabel }) {
            updateLabel(v + LabelFilter(newLabel))
        }
    }

    fun removeLabel(oldLabel: LabelFilter) {
        val v = labelFilters.value
        if (v.contains(oldLabel)) {
            updateLabel(v - oldLabel)
        }
    }

    fun toggleLabel(oldFilter: LabelFilter) {
        val v = labelFilters.value
        if (v.contains(oldFilter)) {
            updateLabel(v.map { if (it == oldFilter) it.asToggled() else it })
        }
    }

    fun clearFilters() {
        updateSearch("")
        updateLabel(listOf())
    }

    fun makeDocumentShowIntent(doc: DocumentFull): Intent? {
        return when {
            doc.isPdfDoc -> Intent(Intent.ACTION_VIEW).apply {
                data = buildPdfUri(doc)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            else -> null
        }
    }

    suspend fun makeDocumentShareIntent(doc: DocumentFull, builder: ShareCompat.IntentBuilder): Intent = builder.apply {
        when {
            doc.isPdfDoc -> {
                setType("application/pdf")
                setStream(buildPdfUri(doc))
            }

            doc.isImagesDoc && doc.parts.size == 1 -> {
                setType("image/jpeg")
                setStream(buildJpegUri(doc.document, doc.parts.first()))
            }

            else -> {
                setType("application/zip")
                setStream(zipDocContent(doc))
            }
        }

        setSubject(doc.document.titleOrName)
        setChooserTitle(getApplication<Application>().getString(R.string.send_title, doc.document.titleOrName))
    }.createChooserIntent()

    val documentsWithHeaders: Flow<List<Any>> = snapshotFlow { search }
        .combine(labelFilters, ::Filters)
        .debounce(300)
        .flatMapLatest { filters ->
            val (include, exclude) = filters.labels.partition { it.include }
            repo.db.docDao().search(include.map { it.label }, exclude.map { it.label }, filters.search)
        }
        .map { docs ->
            // assume same TZ as the mtime
            val cal = Calendar.getInstance()

            var previousYear: Int? = null
            var previousMonth: Int? = null

            val rows = ArrayList<Any>(docs.size + 10)

            for (doc in docs) {
                cal.timeInMillis = doc.document.date

                val year = cal.get(Calendar.YEAR)
                val month = cal.get(Calendar.MONTH)

                val yearChange = year != previousYear
                val monthChange = yearChange || month != previousMonth
                if (monthChange) {
                    val monthName = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: ""
                    // headers have negative keys
                    rows.add(HeaderData(year, monthName, -year * 12 + month, yearChange))
                }
                rows.add(doc)
                previousYear = year
                previousMonth = month
            }
            ComposeImmutableList(rows)
        }
        .flowOn(Dispatchers.Default)
        .latestRelease(viewModelScope, listOf())

    val labelTypes = repo.db.labelDao().loadLabelTypes()

    suspend fun queueDownload(docId: Int) {
        repo.db.downloadDao().queueDownloadForDocument(docId)
        DownloadWorker.enqueueLoad(getApplication())
    }

    private fun buildPdfUri(doc: DocumentFull): Uri =
        buildUri(File(settings.localPartsDir, doc.partPath(0)), "${doc.document.titleOrName}.pdf")

    private fun buildJpegUri(doc: Document, part: Part): Uri =
        buildUri(File(settings.localPartsDir, part.path(doc.name)), "${doc.titleOrName}.jpg")

    private fun buildUri(file: File, name: String): Uri {
        // the filename should have an extension
        // some mail client ignore the mime type
        val ctxt = getApplication<Application>()
        return FileProvider.getUriForFile(
            ctxt,
            "${ctxt.packageName}.fileprovider",
            file,
            name,
        )
    }

    private suspend fun zipDocContent(doc: DocumentFull): Uri = withContext(Dispatchers.IO) {
        val zipFile = File(settings.localPartsDir, "${doc.docPath}/$EXPORTED_ZIP")

        if (doc.parts.any { !it.isLocal }) {
            val msg = "Not all parts were downloaded"
            Log.e(TAG, msg)
            if (BuildConfig.DEBUG) {
                throw IllegalStateException(msg)
            }
        }

        if (zipFile.exists()) {
            Log.d(TAG, "Zip already exists $zipFile")
        } else {
            var totalSize = 0L
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { out ->
                // no need to compress jpeg images
                // but setting the method to STORED requires to calc the CRC 32
                out.setLevel(Deflater.NO_COMPRESSION)
                for (part in doc.parts) {
                    val relativePath = part.path(doc.docPath)
                    FileInputStream(File(settings.localPartsDir, relativePath)).use { fi ->
                        BufferedInputStream(fi).use { from ->
                            val entry = ZipEntry(relativePath)
                            out.putNextEntry(entry)
                            totalSize += from.copyTo(out)
                        }
                    }
                }
            }
            if (totalSize != doc.document.size) {
                val msg = "Inconsistent size ${totalSize} != ${doc.document.size}"
                Log.e(TAG, msg)

                if (BuildConfig.DEBUG) {
                    throw IllegalStateException(msg)
                }
            }
        }

        buildUri(zipFile, doc.document.titleOrName)
    }

    @Immutable
    @Parcelize
    data class LabelFilter(
        val label: String,
        val include: Boolean = true,
    ) : Parcelable {
        fun asToggled() = LabelFilter(label, !include)
    }

    @Immutable
    data class Filters(val search: String = "", val labels: List<LabelFilter> = listOf())

    @Immutable
    data class HeaderData(val year: Int, val month: String, val key: Int, val yearChange: Boolean)

    companion object {
        private const val TAG = "DocListVM"
        private const val EXPORTED_ZIP = "exported_archive.zip"
    }
}


