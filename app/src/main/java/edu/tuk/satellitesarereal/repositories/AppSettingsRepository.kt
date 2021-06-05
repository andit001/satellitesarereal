package edu.tuk.satellitesarereal.repositories

import kotlinx.coroutines.flow.Flow


interface AppSettingsRepository {
    suspend fun tleUrls(): Flow<List<String>>
    suspend fun saveTLEUrls(urls: List<String>)
}