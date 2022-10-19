package net.phbwt.paperwork.data.entity

import androidx.compose.runtime.Immutable
import androidx.room.*

@Fts4(
    tokenizer = FtsOptions.TOKENIZER_UNICODE61,
    contentEntity = DocumentText::class,
)
@Entity
@Immutable
data class DocumentFts(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Int,

    // content, used to match, and in snippet
    val main: String,

    // title, used to match, but not in snippet
    val additional: String?,
)
