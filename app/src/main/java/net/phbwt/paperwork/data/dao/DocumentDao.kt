package net.phbwt.paperwork.data.dao

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import net.phbwt.paperwork.data.entity.db.DNL_NONE
import net.phbwt.paperwork.data.entity.db.DocumentFull
import net.phbwt.paperwork.data.entity.db.Part

@Dao
interface DocumentDao {

    @Transaction
    @RawQuery(observedEntities = [Part::class])
    fun searchImpl(query: SupportSQLiteQuery): Flow<List<DocumentFull>>

    fun search(
        includedLabels: List<String>,
        excludedLabels: List<String>,
        baseSearch: String,
    ): Flow<List<DocumentFull>> = searchImpl(
        DocumentQueryBuilder()
            .addIncludedLabels(includedLabels)
            .addExcludedLabels(excludedLabels)
            .addFts(baseSearch)
            .build()
    )

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Transaction
    @Query("select * from Document where documentId in (select documentId from Part where downloadStatus != ${DNL_NONE}) order by documentId limit 500")
    fun withDownloads(): Flow<List<DocumentFull>>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Transaction
    @Query("select * from Document where documentId = :id")
    fun loadDocument(id: Int): Flow<DocumentFull>
}

private const val TAG = "DocumentDao"





