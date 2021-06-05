package edu.tuk.satellitesarereal.repositories

interface TleFilesRepository {

    suspend fun listFiles(): List<String>

    suspend fun deleteFile(fileName: String)

    suspend fun downloadTleFile(fileUrl: String)

}