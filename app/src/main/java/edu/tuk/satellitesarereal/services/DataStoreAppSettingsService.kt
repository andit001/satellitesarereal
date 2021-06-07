package edu.tuk.satellitesarereal.services

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import edu.tuk.satellitesarereal.repositories.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

@Singleton
class DataStoreAppSettingsService @Inject constructor(@ApplicationContext val context: Context) :
    AppSettingsRepository {

    companion object {
        val TLE_URLS_KEY = stringPreferencesKey("TLE_URLS_KEY")
    }

    override suspend fun tleUrls(): Flow<List<String>> {
        return context.dataStore.data
            .map {
                (it[TLE_URLS_KEY] ?: "")
                    .split(";")
                    .filter { str -> str.isNotEmpty() }
            }
    }

    override suspend fun saveTLEUrls(urls: List<String>) {
        context.dataStore.edit { settings ->
            settings[TLE_URLS_KEY] = urls.joinToString(";")
        }
    }
}