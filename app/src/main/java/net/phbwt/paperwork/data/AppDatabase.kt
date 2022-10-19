package net.phbwt.paperwork.data

import androidx.room.Database
import androidx.room.RoomDatabase
import net.phbwt.paperwork.data.dao.DocumentDao
import net.phbwt.paperwork.data.dao.DownloadDao
import net.phbwt.paperwork.data.dao.LabelDao
import net.phbwt.paperwork.data.entity.*

@Database(
    version = 1,
    entities = [
        Document::class,
        Part::class,
        Label::class,
        DocumentText::class,
        DocumentFts::class,
    ],
    autoMigrations = [
    ],
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun docDao(): DocumentDao
    abstract fun downloadDao(): DownloadDao
    abstract fun labelDao(): LabelDao
}

