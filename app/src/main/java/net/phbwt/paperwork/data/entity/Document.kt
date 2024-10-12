package net.phbwt.paperwork.data.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [
        Index("date", name = "Document_date"),
    ]
)
@Immutable
data class Document(
    @PrimaryKey
    val documentId: Int,

    val name: String,

    val title: String?,

    val thumb: String?,

    val pageCount: Int,

    val date: Long,

    val mtime: Long,

    val size: Long,
) {
    val titleOrName: String get() = title ?: name
}

fun makeDocumentThumbPathAndKey(documentName: String, thumb: String?) = "$documentName/$thumb"