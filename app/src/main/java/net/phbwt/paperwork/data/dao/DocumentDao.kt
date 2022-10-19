package net.phbwt.paperwork.data.dao

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import net.phbwt.paperwork.BuildConfig
import net.phbwt.paperwork.data.entity.DNL_NONE
import net.phbwt.paperwork.data.entity.DocumentFull

@Dao
interface DocumentDao {

    @Transaction
    @RawQuery
    fun searchImpl(query: SupportSQLiteQuery): Flow<List<DocumentFull>>

    fun search(labels: List<String>, baseSearch: String): Flow<List<DocumentFull>> {
        val args = mutableListOf<Any>()
        val cte = mutableListOf<String>()
        var selected = "d.*"
        val join = mutableListOf<String>()

        if (labels.isNotEmpty()) {
            val c = """
l(documentId) as (
  select documentId
  from Label
  where name in (${labels.joinToString(", ") { "?" }})
  group by documentId
  having count(*) = ?
)"""
            cte.add(c)
            join.add("join l on d.documentId = l.documentId")
            args.addAll(labels)
            args.add(labels.size)
        }

        if (baseSearch.isNotBlank()) {
            val search = baseSearch
                .trim()
                .replace('"', '_')
                .split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { "\"${it}*\"" }

            // match on main and additional (i.e. title),
            // extract snippet only from main
            val c = """
t(documentId, snippet) as (
  select rowid, snippet(DocumentFts, '$S$R', '$S', '$S...$S', 0, 15)
  from DocumentFts
  where DocumentFts match ?
)"""
            cte.add(c)
            selected += ", snippet"
            join.add("join t on d.documentId = t.documentId")
            args.add(search)
        }

        val query = """
${if (cte.isNotEmpty()) cte.joinToString(", ", "with") else ""}
select $selected
from Document d
${join.joinToString("\n")}
order by d.documentId
limit 150
"""
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "QUERY=${query}\nARGS=$args")
        }

        return searchImpl(SimpleSQLiteQuery(query, args.toTypedArray()))
    }


    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Transaction
    @Query("select * from Document order by documentId limit 500")
    fun allDocuments(): Flow<List<DocumentFull>>

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

const val SNIPPET_SPLIT = "|"
private const val S = SNIPPET_SPLIT
const val SNIPPET_RESULT = "§"
private const val R = SNIPPET_RESULT




