package net.phbwt.paperwork.data.entity.db

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Relation

@Immutable
data class DocumentFull(
    @Embedded
    val document: Document,

    // only present when searching via a FTS query
    val snippet: String?,

    @Relation(
        parentColumn = "documentId",
        entityColumn = "documentId",
    )
    val parts: List<Part>,

    @Relation(
        entity = Label::class,
        parentColumn = "documentId",
        entityColumn = "documentId",
        projection = ["name"]
    )
    val labelNames: List<String>,
) {
    val docPath get() = document.name

    fun partPath(idx: Int) = partPath(parts[idx])

    fun partPath(part: Part) = part.path(docPath)

    val isPdfDoc get() = parts.size == 1 && parts.first().isPdfPart

    val isImagesDoc get() = parts.isNotEmpty() && parts.first().isImagePart

    val canBeViewed
        get() = when {
            isImagesDoc -> true
            isPdfDoc && downloadStatus == DownloadState.LOCAL -> true
            else -> false
        }

    val downloadStatus
        get() = when {
            parts.all { it.isLocal } -> DownloadState.LOCAL
            parts.any { it.isFailed } -> DownloadState.FAILED
            parts.any { it.isInProgress } -> DownloadState.IN_PROGRESS
            parts.any { it.isQueued } -> DownloadState.QUEUED
            else -> DownloadState.DOWNLOADABLE
        }
}

enum class DownloadState { LOCAL, FAILED, QUEUED, IN_PROGRESS, DOWNLOADABLE }

