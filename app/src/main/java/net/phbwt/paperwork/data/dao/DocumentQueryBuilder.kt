package net.phbwt.paperwork.data.dao

import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import net.phbwt.paperwork.BuildConfig

class DocumentQueryBuilder {

    private val ctes = mutableListOf<String>()
    private val cteArgs = mutableListOf<Any>()
    private var selects = mutableListOf("d.*")
    private val joins = mutableListOf<String>()
    private var wheres = mutableListOf<String>()
    private val whereArgs = mutableListOf<Any>()

    fun addIncludedLabels(labels: List<String>) = apply {

        if (labels.isNotEmpty()) {
            val c = """
l(documentId) as (
  select documentId
  from Label
  where name in (${labels.joinToString(", ") { "?" }})
  group by documentId
  having count(*) = ?
)"""
            ctes.add(c)
            joins.add("join l on d.documentId = l.documentId")
            cteArgs.addAll(labels)
            cteArgs.add(labels.size)
        }

    }

    fun addExcludedLabels(labels: List<String>) = apply {
        if (labels.isNotEmpty()) {
            wheres.add("(select count(*) from Label l where l.documentId = d.documentId and l.name in (${labels.joinToString(", ") { "?" }})) = 0")
            whereArgs.addAll(labels)
        }
    }

    fun addFts(words: String) = apply {
        val ftsQuery = prepareFtsQuery(words)

        if (BuildConfig.DEBUG) {
            Log.i(TAG, "'$words' --> '$ftsQuery'")
        }

        if (ftsQuery.isNotEmpty()) {
            // match on main and additional (i.e. title),
            // extract snippet only from main
            ctes.add(
                """
t(documentId, snippet) as (
  select rowid, snippet(DocumentFts, '$S$R', '$S', '$S...$S', 0, 15)
  from DocumentFts
  where DocumentFts match ?
)"""
            )
            selects.add("snippet")
            joins.add("join t on d.documentId = t.documentId")
            cteArgs.add(ftsQuery)
        }
    }

    fun build(): SupportSQLiteQuery {
        val query = """
${if (ctes.isNotEmpty()) ctes.joinToString(", ", " with ") else ""}
select ${selects.joinToString(", ")}
from Document d
${joins.joinToString("\n")}
${if (wheres.isNotEmpty()) wheres.joinToString(" and ", " where ") else ""}
order by d.documentId
limit 150
"""

        val args = cteArgs + whereArgs

        if (BuildConfig.DEBUG) {
            Log.i(TAG, "QUERY=${query}\nARGS=$args")
        }

        return SimpleSQLiteQuery(query, args.toTypedArray())
    }

    // public for tests
    fun prepareFtsQuery(baseWords: String): String {
        val words = baseWords.replace(CLEANER_RE, " ")
        var pos = 0

        fun next(block: (Boolean, String) -> Unit): Boolean {

            if (pos > words.lastIndex) {
                block(false, "")
                return false
            }

            val p = words.indexOf('"', pos)
            val found = p != -1

            if (found) {
                block(true, words.substring(pos, p))
                pos = p + 1
            } else {
                block(false, words.substring(pos))
                pos = words.length
            }

            return found
        }

        val res = StringBuilder(words.length + 16)

        do {
            val hasMore = next { found, str ->
                // out of quoted part
                for (v in maybeAsPrefix(found, str).split(" ").filter { it.isNotBlank() }) {
                    res.append(" \"").append(v).append("\"")
                }
            } && next { found, str ->
                // in quoted part
                if (str.isNotBlank()) {
                    res.append(" \"").append(maybeAsPrefix(found, str)).append("\"")
                }
            }
        } while (hasMore)

        return res.trim().toString()
    }

    private fun maybeAsPrefix(hasMore: Boolean, str: String) = when {
        hasMore -> str
        str.endsWith(' ') -> str
        str.endsWith('*') -> str
        str.isBlank() -> str
        else -> "$str*"
    }

}

private const val TAG = "DocumentQueryHelper"


val CLEANER_RE = Regex("\\s+")
const val SNIPPET_SPLIT = "|"
private const val S = SNIPPET_SPLIT
const val SNIPPET_RESULT = "§"
private const val R = SNIPPET_RESULT
