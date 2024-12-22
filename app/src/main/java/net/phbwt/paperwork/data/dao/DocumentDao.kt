package net.phbwt.paperwork.data.dao

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import net.phbwt.paperwork.data.entity.db.DNL_DONE
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

    fun withDownloads(): Flow<List<DocumentFull>> = havingAPartNotInState(DNL_NONE)

    fun notFullyDownloaded(): Flow<List<DocumentFull>> = havingAPartNotInState(DNL_DONE)

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("""
select *
from Document
where documentId in (
  select documentId
  from Part
  where downloadStatus != :excludedState
)
order by date desc limit 500
""")
    fun havingAPartNotInState(excludedState: Int ): Flow<List<DocumentFull>>



    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("select * from Document where documentId = :id")
    fun loadDocument(id: Int): Flow<DocumentFull>
}

private const val TAG = "DocumentDao"





