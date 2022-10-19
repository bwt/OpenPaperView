package net.phbwt.paperwork.data.dao

import android.util.Log
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import net.phbwt.paperwork.data.entity.*

@Dao
interface DownloadDao {

    @RewriteQueriesToDropUnusedColumns
    @Query(
        """
select p.*, d.name as documentName, d.title as documentTitle
from Part p
join Document d
  on d.documentId = p.documentId
where p.downloadStatus in ($DNL_QUEUED) order by p.partId limit 1
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

    @Query("update Part set downloadStatus = $DNL_ERROR, downloadError = 'canceled' where downloadStatus = $DNL_DOWNLOADING")
    suspend fun clearStuckDownloads(): Int

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
    suspend fun restartPart(partId: Int)

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

}

private const val TAG = "DownloadDao"