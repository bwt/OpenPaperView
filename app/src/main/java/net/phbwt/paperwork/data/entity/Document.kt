package net.phbwt.paperwork.data.entity

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
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
    val thumbPath: String get() = "$name/$thumb"
    val titleOrName: String get() = title ?: name
}