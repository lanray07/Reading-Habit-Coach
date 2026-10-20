package com.readinghabitcoach.app

import android.app.Application
import androidx.room.Room
import com.readinghabitcoach.app.data.CoachDatabase
import com.readinghabitcoach.app.data.Preferences

class CoachApplication : Application() {
    val database by lazy { Room.databaseBuilder(this, CoachDatabase::class.java, "reading-coach.db").build() }
    val preferences by lazy { Preferences(this) }
}
