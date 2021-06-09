package edu.tuk.satellitesarereal.services

import android.content.Context
import android.util.Log
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
import retrofit2.http.Url
import javax.inject.Inject
import javax.inject.Singleton

// https://stackoverflow.com/a/61093017
// https://developer.android.com/training/data-storage/app-specific.

const val TAG = "SatAr:RetrofitTleFilesService"

interface TleFilesDownloader {
    @GET
    suspend fun downloadFile(@Url fileUrl: String): Response<ResponseBody>
}

@Singleton
class RetrofitTleFilesService @Inject constructor(@ApplicationContext val context: Context) :
    TleFilesRepository {

    private val service: TleFilesDownloader = Retrofit.Builder()
        .baseUrl("https://google.com/")
        .build()
        .create(TleFilesDownloader::class.java)

    override fun listFiles(): List<String> {
        return context.fileList().toList().drop(1)
    }

    override fun deleteFile(fileName: String) {
        context.deleteFile(fileName)
    }

    override suspend fun downloadTleFile(fileUrl: String) {
        // https://github.com/Kotlin/kotlinx-datetime
        val timeStamp = Clock.System
            .now()
            .toLocalDateTime(TimeZone.UTC)
            .toJavaLocalDateTime()
            .toString()

        Log.d(TAG, "fileUrl: $fileUrl")

        val fileName = fileUrl
            .substring(fileUrl.lastIndexOf("/") + 1)

        val response = service.downloadFile(fileUrl)

        val responseBody = if (response != null && response.isSuccessful) {
            response.body()
        } else {
            Log.d(TAG, "Could not fetch file: $fileUrl")
            return
        }

        responseBody?.let {
            saveFile(
                fileName = "$timeStamp $fileName",
                responseBody = responseBody
            )
        }

    }

    private fun saveFile(fileName: String, responseBody: ResponseBody?) {
        responseBody?.let {
            responseBody.byteStream().use { input ->
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
}