package edu.tuk.satellitesarereal.repositories

import kotlinx.coroutines.flow.Flow


interface AppSettingsRepository {
    suspend fun tleUrls(): Flow<List<String>>
    suspend fun saveTLEUrls(urls: List<String>)
    suspend fun orientationSensorRate(): Flow<Int>
    suspend fun saveOrientationSensorRate(rate: Int)
    suspend fun locationServiceInterval(): Flow<Long>
    suspend fun saveLocationServiceInterval(rate: Long)
    suspend fun fieldOfView(): Flow<Float>
    suspend fun saveFieldOfView(fov: Float)
}