package net.phbwt.paperwork.data.dao

import android.util.Log
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.phbwt.paperwork.data.entity.db.*
import net.phbwt.paperwork.data.settings.isFullDownload

@Dao
interface DownloadDao {

    @RewriteQueriesToDropUnusedColumns
    @Query(
        """
select p.*, d.name as documentName, d.thumb as documentThumb
from Part p
join Document d
  on d.documentId = p.documentId
where p.downloadStatus in ($DNL_QUEUED) order by d.date desc limit 1
"""
    )
    suspend fun loadFirstDownloadableImpl(): Download?

    @Transaction
    suspend fun loadFirstDownloadable(): Download? {
        val dnl = loadFirstDownloadableImpl()
        if (dnl != null) {
            val count = updateDnlStatusIfImpl(dnl.part.partId, DNL_QUEUED, DNL_DOWNLOADING, null)
            if (count != 1) {
                // should not happen
                throw IllegalStateException("Part ${dnl.part.partId} is not in state QUEUED : $dnl")
            }
        }
        return dnl
    }

    @Query("select count(*) from Part where downloadStatus = $DNL_QUEUED")
    suspend fun countRemainingDownloads(): Int

    @Query("update Part set downloadStatus = $DNL_QUEUED, downloadError = null where downloadStatus = $DNL_DOWNLOADING")
    suspend fun retryStuckDownloads(): Int

    @Query("update Part set downloadStatus = $DNL_NONE, downloadError = null where downloadStatus != $DNL_NONE")
    suspend fun purgeDownloads(): Int

    @Query("update Part set downloadStatus = :status, downloadError = :error where partId = :partId")
    suspend fun updateDnlStatusImpl(partId: Int, status: Int, error: String?)

    @Query("update Part set downloadStatus = :newStatus, downloadError = :error where partId = :partId and downloadStatus = :oldStatus")
    suspend fun updateDnlStatusIfImpl(partId: Int, oldStatus: Int, newStatus: Int, error: String?): Int

    @Transaction
    suspend fun setPartFailed(partId: Int, error: String) {
        val count = updateDnlStatusIfImpl(partId, DNL_DOWNLOADING, DNL_ERROR, error)

        if (count != 1) {
            // not the expected state, reset
            Log.i(TAG, "setPartFailed : part $partId was not in state DOWNLOADING")
            updateDnlStatusImpl(partId, DNL_NONE, null)
        }
    }

    @Transaction
    suspend fun setPartDone(partId: Int): Boolean {
        val count = updateDnlStatusIfImpl(partId, DNL_DOWNLOADING, DNL_DONE, null)

        if (count != 1) {
            // not the expected state, probably because the processing took
            // too long and the user canceled it
            // anyway, reset the state, as we will delete the file
            Log.i(TAG, "setPartDone : part $partId was not in state DOWNLOADING")
            updateDnlStatusImpl(partId, DNL_NONE, null)
        }

        return count == 1
    }

    @Query(
        """
update Part
set downloadStatus = $DNL_DONE
where name = :partName
  and documentId = (select documentId from Document where name = :documentName)
"""
    )
    suspend fun setPartDone(documentName: String, partName: String): Int

    @Query("update Part set downloadStatus = $DNL_NONE, downloadError = null where documentId = :docId")
    suspend fun setDocumentCleared(docId: Int)

    @Query(
        """
update Part
set downloadStatus = $DNL_QUEUED
, downloadError = null
where partId = :partId
  and downloadStatus not in ($DNL_QUEUED, $DNL_DOWNLOADING, $DNL_DONE)
"""
    )
    suspend fun queueDownloadForPart(partId: Int)

    @Query(
        """
update Part
set downloadStatus = $DNL_QUEUED
, downloadError = null
where documentId = :docId
and downloadStatus not in ($DNL_QUEUED, $DNL_DOWNLOADING, $DNL_DONE)
"""
    )
    suspend fun queueDownloadForDocument(docId: Int)

    suspend fun queueAutoDownloads(labels: List<String>): Int = when {
        labels.isEmpty() -> 0
        labels.isFullDownload() -> queueAutoDownloadsAllImpl()
        else -> queueAutoDownloadsImpl(labels)
    }

    @Query(
        """
update Part
set downloadStatus = $DNL_QUEUED
, downloadError = null
where downloadStatus not in ($DNL_QUEUED, $DNL_DOWNLOADING, $DNL_DONE)
"""
    )
    suspend fun queueAutoDownloadsAllImpl(): Int

    @Query(
        """
update Part
set downloadStatus = $DNL_QUEUED
, downloadError = null
where documentId in (select distinct documentId from Label where name in (:labels))
and downloadStatus not in ($DNL_QUEUED, $DNL_DOWNLOADING, $DNL_DONE)
"""
    )
    suspend fun queueAutoDownloadsImpl(labels: List<String>): Int

    fun countAutoDownloads(labels: List<String>): Flow<AutoDownloadInfo> = when {
        labels.isEmpty() -> flowOf(AutoDownloadInfo(0, 0))
        labels.isFullDownload() -> countAutoDownloadsAllImpl()
        else -> countAutoDownloadsImpl(labels)
    }

    @Query(
        """
select count(distinct documentId) as total
, count(distinct case downloadStatus when $DNL_DONE then null else documentId end) as todo
from Part
where documentId in (select distinct documentId from Label where name in (:labels))
"""
    )
    fun countAutoDownloadsImpl(labels: List<String>): Flow<AutoDownloadInfo>

    @Query(
        """
select count(distinct documentId) as total
, count(distinct case downloadStatus when $DNL_DONE then null else documentId end) as todo
from Part
"""
    )
    fun countAutoDownloadsAllImpl(): Flow<AutoDownloadInfo>

    @Query(
        """
with doc_parts as (
    select p.documentId
    , d.size
    , count(*) as parts
    from Part p
    inner join Document d on d.documentId = p.documentId
    where p.downloadStatus = $DNL_DONE
    group by p.documentId
)
select count(*) as documents
, sum(parts) as parts
, sum(size) as size
from doc_parts
"""
    )
    fun stats(): Flow<DownloadStats>

}

data class DownloadStats(
    val documents: Int,
    val parts: Int,
    val size: Long,
)

data class AutoDownloadInfo(
    val total: Int,
    val todo: Int,
)


private const val TAG = "DownloadDao"
