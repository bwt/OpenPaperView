package net.phbwt.paperwork.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import net.phbwt.paperwork.data.dao.DocumentDao
import net.phbwt.paperwork.data.dao.DownloadDao
import net.phbwt.paperwork.data.dao.LabelDao
import net.phbwt.paperwork.data.entity.db.Document
import net.phbwt.paperwork.data.entity.db.DocumentFts
import net.phbwt.paperwork.data.entity.db.DocumentText
import net.phbwt.paperwork.data.entity.db.Label
import net.phbwt.paperwork.data.entity.db.Part

@Database(
    version = 2,
    entities = [
        Document::class,
        Part::class,
        Label::class,
        DocumentText::class,
        DocumentFts::class,
    ],
    autoMigrations = [
        AutoMigration(1, 2),
    ],
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun docDao(): DocumentDao
    abstract fun downloadDao(): DownloadDao
    abstract fun labelDao(): LabelDao
}

