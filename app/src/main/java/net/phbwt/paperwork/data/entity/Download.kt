package net.phbwt.paperwork.data.entity

import androidx.compose.runtime.Immutable
import androidx.room.Embedded

@Immutable
data class Download(
    val documentName: String,

    @Embedded val part: Part,
) {
    fun partPath() = part.path(documentName)
}