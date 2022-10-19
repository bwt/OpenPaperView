package net.phbwt.paperwork.data.entity

import androidx.compose.runtime.Immutable
import androidx.room.Ignore
import java.text.Normalizer
import java.util.*


@Immutable
data class LabelType(
    val name: String,
) {
    @Ignore
    val normalizedName: String = name.asFilter()
}

private val REMOVE_DIACRITICS = "\\p{Mn}+".toRegex()

fun String.asFilter() = Normalizer
    .normalize(this, Normalizer.Form.NFKD)
    .replace(REMOVE_DIACRITICS, "")
    .lowercase(Locale.getDefault())
