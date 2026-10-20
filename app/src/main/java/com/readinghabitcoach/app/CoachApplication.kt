package com.readinghabitcoach.app

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.readinghabitcoach.app.data.CoachDatabase
import com.readinghabitcoach.app.data.Preferences

class CoachApplication : Application() {
    val database by lazy { Room.databaseBuilder(this, CoachDatabase::class.java, "reading-coach.db")
        .addMigrations(object : Migration(1,2) { override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE sessions ADD COLUMN manual INTEGER NOT NULL DEFAULT 0")
        } }).build() }
    val preferences by lazy { Preferences(this) }
}
