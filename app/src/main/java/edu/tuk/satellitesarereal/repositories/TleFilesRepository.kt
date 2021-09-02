package edu.tuk.satellitesarereal.repositories

import java.io.InputStream

interface TleFilesRepository {

    fun listFiles(): List<String>

    fun deleteFile(fileName: String)

    suspend fun downloadTleFile(fileUrl: String)

    fun openFile(fileName: String): InputStream
}