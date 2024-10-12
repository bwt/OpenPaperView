package net.phbwt.paperwork.data.entity

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import java.io.File

@Immutable
data class Download(
    val documentName: String,
    val documentThumb: String,

    @Embedded val part: Part,
) {
    fun partPathAndKey() = part.path(documentName)
}

fun File.isThumb() = this.isFile && this.name.contains(".thumb.", ignoreCase = true)