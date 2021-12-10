package edu.tuk.satellitesarereal.services

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import edu.tuk.satellitesarereal.repositories.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

@Singleton
class DataStoreAppSettingsService @Inject constructor(@ApplicationContext val context: Context) :
    AppSettingsRepository {

    companion object {
        val TLE_URLS_KEY = stringPreferencesKey("TLE_URLS_KEY")
        val ORIENTATION_SENSOR_RATE = intPreferencesKey("ORIENTATION_SENSOR_RATE")
        val LOCATION_SERVICE_RATE = longPreferencesKey("LOCATION_SERVICE_RATE")
        val FOV = floatPreferencesKey("FOV")
    }

    override suspend fun tleUrls(): Flow<List<String>> {
        return context.dataStore.data
            .catch {
                if (it is IOException) {
                    it.printStackTrace()
                } else {
                    throw it
                }
            }
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

    override suspend fun orientationSensorRate(): Flow<Int> {
        return context.dataStore.data
            .catch {
                if (it is IOException) {
                    it.printStackTrace()
                } else {
                    throw it
                }
            }
            .map {
                it[ORIENTATION_SENSOR_RATE] ?: 2000
            }
    }

    override suspend fun saveOrientationSensorRate(rate: Int) {
        context.dataStore.edit { settings ->
            settings[ORIENTATION_SENSOR_RATE] = rate
        }
    }

    override suspend fun locationServiceInterval(): Flow<Long> {
        return context.dataStore.data
            .catch {
                if (it is IOException) {
                    it.printStackTrace()
                } else {
                    throw it
                }
            }
            .map {
                it[LOCATION_SERVICE_RATE] ?: 2000L
            }
    }

    override suspend fun saveLocationServiceInterval(rate: Long) {
        context.dataStore.edit { settings ->
            settings[LOCATION_SERVICE_RATE] = rate
        }
    }

    override suspend fun fieldOfView(): Flow<Float> {
        return context.dataStore.data
            .catch {
                if (it is IOException) {
                    it.printStackTrace()
                } else {
                    throw it
                }
            }
            .map {
                it[FOV] ?: 45.0f
            }
    }

    override suspend fun saveFieldOfView(fov: Float) {
        context.dataStore.edit { settings ->
            settings[FOV] = fov
        }
    }
}