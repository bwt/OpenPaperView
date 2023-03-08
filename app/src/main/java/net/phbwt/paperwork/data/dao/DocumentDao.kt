package net.phbwt.paperwork.data.dao

import android.util.Log
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import net.phbwt.paperwork.data.entity.DNL_NONE
import net.phbwt.paperwork.data.entity.DocumentFull
import net.phbwt.paperwork.data.entity.Part

@Dao
interface DocumentDao {

    @Transaction
    @RawQuery(observedEntities = [Part::class])
    fun searchImpl(query: SupportSQLiteQuery): Flow<List<DocumentFull>>

    fun search(labels: List<String>, baseSearch: String): Flow<List<DocumentFull>> {
        return searchImpl(DocumentQueryBuilder().addLabels(labels).addFts(baseSearch).build())
    }

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





