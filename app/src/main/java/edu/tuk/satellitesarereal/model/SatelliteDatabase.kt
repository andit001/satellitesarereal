package edu.tuk.satellitesarereal.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

const val databaseName = "satellite-database"

@Database(entities = arrayOf(TleEntry::class), version = 1)
abstract class SatelliteDatabase : RoomDatabase() {
    abstract fun tleEntryDao(): TleEntryDao

    companion object {
        fun create(context: Context): SatelliteDatabase {
            return Room.databaseBuilder(
                context,
                SatelliteDatabase::class.java, databaseName
            ).build()
        }
    }
}