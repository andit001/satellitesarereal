package edu.tuk.satellitesarereal.repositories

interface TleFilesRepository {

    fun listFiles(): List<String>

    fun deleteFile(fileName: String)

    suspend fun downloadTleFile(fileUrl: String)

}