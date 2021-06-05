package edu.tuk.satellitesarereal

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import edu.tuk.satellitesarereal.repositories.AppSettingsRepository
import edu.tuk.satellitesarereal.services.DataStoreAppSettingsService
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
}