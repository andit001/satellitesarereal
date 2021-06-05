package edu.tuk.satellitesarereal.services

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import edu.tuk.satellitesarereal.repositories.TleFilesRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url
import javax.inject.Inject
import javax.inject.Singleton

// https://stackoverflow.com/a/61093017
// https://developer.android.com/training/data-storage/app-specific.

interface TleFilesDownloader {
    @Streaming
    @GET
    suspend fun downloadFile(@Url fileUrl: String): Response<ResponseBody>
}

@Singleton
class RetrofitTleFilesService @Inject constructor(@ApplicationContext val context: Context)
    : TleFilesRepository {
    override suspend fun listFiles(): List<String> {
        return context.fileList().toList()
    }

    override suspend fun deleteFile(fileName: String) {
        context.deleteFile(fileName)
    }

    override suspend fun downloadTleFile(fileUrl: String) {
        // https://github.com/Kotlin/kotlinx-datetime
        val timeStamp = Clock.System
            .now()
            .toLocalDateTime(TimeZone.UTC)
            .toJavaLocalDateTime()
            .toString()

        val fileName = fileUrl
            .substring(fileUrl.lastIndexOf("/") + 1)

        val responseBody = Retrofit.Builder()
            .baseUrl(fileUrl.substring(0..fileUrl.lastIndexOf("/")))
            .build()
            .create(TleFilesDownloader::class.java)
            .downloadFile(fileUrl)
            .body()

        saveFile(
            fileName = fileName + "_" + timeStamp,
            responseBody = responseBody
        )

    }

    private fun saveFile(fileName: String, responseBody: ResponseBody?) {
        responseBody?.let {
            val input = responseBody.byteStream()
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { output ->
                val buffer = ByteArray(4 * 1024)
                var read: Int

                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                }
            }
        }
    }
}