package edu.tuk.satellitesarereal.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

const val databaseName = "satellite-database"

@Database(
    version = 1,
    entities = [TleEntry::class],
)
abstract class SatelliteDatabase : RoomDatabase() {
    abstract fun tleEntryDao(): TleEntryDao

    companion object {
        fun create(context: Context): SatelliteDatabase {
            return Room.databaseBuilder(
                context,
                SatelliteDatabase::class.java,
                databaseName
            ).build()
        }
    }
}