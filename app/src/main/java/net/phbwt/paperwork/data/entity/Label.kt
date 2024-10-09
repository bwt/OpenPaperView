package net.phbwt.paperwork.data.entity

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
        Index("documentId", name = "Label_documentId"),
        Index("name", name = "Label_name"),
    ]
)
@Immutable
data class Label(
    @PrimaryKey
    val labelId: Int,

    val documentId: Int,

    val name: String,

    val color: String?,
)

