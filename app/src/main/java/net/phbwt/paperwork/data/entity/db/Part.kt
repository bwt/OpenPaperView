package net.phbwt.paperwork.data.entity.db

import androidx.compose.runtime.Immutable
import androidx.room.*

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Document::class,
            parentColumns = arrayOf("documentId"),
            childColumns = arrayOf("documentId"),
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("documentId", name = "Part_documentId"),
        Index("downloadStatus", name = "Part_downloadStatus"),
    ]
)
@Immutable
data class Part(
    @PrimaryKey
    val partId: Int,

    val documentId: Int,

    val name: String,

    val downloadStatus: Int = 0,
    val downloadError: String? = null,
) {
    fun path(documentName: String) = "${documentName}/${name}"

    val isQueued get() = downloadStatus == DNL_QUEUED

    val isInProgress get() = downloadStatus == DNL_DOWNLOADING

    val isLocal get() = downloadStatus == DNL_DONE

    val isFailed get() = downloadStatus == DNL_ERROR

    val isIn get() = downloadStatus != DNL_NONE && downloadStatus != DNL_DONE

    val isPdfPart: Boolean get() = name.endsWith(".pdf")

    val isImagePart: Boolean get() = IMAGE_EXTENSIONS.contains(name.substringAfterLast('.'))
}

const val DNL_NONE = 100
const val DNL_QUEUED = 1
const val DNL_DOWNLOADING = 2
const val DNL_DONE = 3
const val DNL_ERROR = 4

private val IMAGE_EXTENSIONS = listOf("png", "jpg")
