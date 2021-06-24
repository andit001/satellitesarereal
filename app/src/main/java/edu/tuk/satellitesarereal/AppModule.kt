package edu.tuk.satellitesarereal

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import edu.tuk.satellitesarereal.model.SatelliteDatabase
import edu.tuk.satellitesarereal.repositories.AppSettingsRepository
import edu.tuk.satellitesarereal.repositories.LocationRepository
import edu.tuk.satellitesarereal.repositories.TleFilesRepository
import edu.tuk.satellitesarereal.services.DataStoreAppSettingsService
import edu.tuk.satellitesarereal.services.LocationService
import edu.tuk.satellitesarereal.services.RetrofitTleFilesService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideSatApplication(@ApplicationContext context: Context): SatApplication {
        return context as SatApplication
    }

    @Provides
    @Singleton
    fun provideAppSettingsRepository(@ApplicationContext context: Context): AppSettingsRepository {
        return DataStoreAppSettingsService(context)
    }

    @Provides
    @Singleton
    fun provideTleFilesRepository(@ApplicationContext context: Context): TleFilesRepository {
        return RetrofitTleFilesService(context)
    }

    @Provides
    @Singleton
    fun provideLocationRepository(@ApplicationContext context: Context): LocationRepository {
        return LocationService(context)
    }

    @Provides
    @Singleton
    fun provideSatellitesDatabase(@ApplicationContext context: Context) : SatelliteDatabase {
        return SatelliteDatabase.create(context)
    }
}