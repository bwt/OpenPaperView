package net.phbwt.paperwork.data.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.phbwt.paperwork.data.entity.db.LabelType

@Dao
abstract class LabelDao {

    @Query("select distinct name from Label")
    abstract fun loadLabelTypes(): Flow<List<LabelType>>

}