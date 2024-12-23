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
)
@Immutable
data class DocumentText(
    @PrimaryKey
    val documentId: Int,

    val main: String,

    val additional: String?,
)
