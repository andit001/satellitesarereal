package edu.tuk.satellitesarereal.ui.viewmodels

import android.util.Log
import android.util.Patterns.WEB_URL
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.tuk.satellitesarereal.repositories.AppSettingsRepository
import edu.tuk.satellitesarereal.repositories.TleFilesRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.net.UnknownHostException
import javax.inject.Inject

const val TAG = "SatAr:SomeViewModel"

@HiltViewModel
class UpdateScreenViewModel @Inject constructor(
    val appSettingsRepository: AppSettingsRepository,
    val tleFilesRepository: TleFilesRepository,
) : ViewModel() {

    class MalformedUrlException(message: String) : Exception(message)

    private var _urls: MutableLiveData<List<String>> = MutableLiveData()
    val urls: LiveData<List<String>> get() = _urls

    private var _fileList: MutableLiveData<List<String>> = MutableLiveData()
    val fileList: LiveData<List<String>> get() = _fileList

    init {
        readUrls()
        getFileList()
    }


    private fun readUrls() {
        viewModelScope.launch {
            appSettingsRepository.tleUrls().collect { value ->
                _urls.postValue(value)
            }
        }
    }

    private fun getFileList() {
        viewModelScope.launch {
            _fileList.postValue(tleFilesRepository.listFiles())
        }
    }

    fun onLoadDefaultUrls() {
        viewModelScope.launch {
            appSettingsRepository.saveTLEUrls(
                listOf(
                    "https://celestrak.com/NORAD/elements/active.txt",
                    "https://www.prismnet.com/~mmccants/tles/classfd.zip",
                    "https://amsat.org/tle/current/nasabare.txt",
                    "https://www.prismnet.com/~mmccants/tles/inttles.zip"
                )
            )
        }
    }

    fun onAddTleUrl(url: String) {
        if (!WEB_URL.matcher(url).matches()) {
            throw MalformedUrlException(
                "UpdateScreenViewModel::onAddTleUrl(): the given URL '$url' is not a valid URL."
            )
        }

        Log.d(TAG, "onAddTleUrl(): Adding URL: $url, urls: ${_urls.value}")

        val newUrls = _urls.value
            ?.toMutableList()
            ?.apply { add(url) }
            ?.distinct()

        Log.d(TAG, "onAddTleUrl(): newUrls: $newUrls")
        viewModelScope.launch {
            newUrls?.let {
                appSettingsRepository.saveTLEUrls(newUrls)
            }
        }
    }

    fun onRemoveTleUrl(url: String) {
        viewModelScope.launch {
            val newUrls = _urls.value
                ?.filter { it != url && it.isNotEmpty() }
                ?.distinct()
            newUrls?.let {
                appSettingsRepository.saveTLEUrls(newUrls)
            }
        }
    }

    fun onDownloadFiles(it: List<String>) {
        it.forEach { downloadFile(it) }
    }

    private fun downloadFile(file: String) {
        viewModelScope.launch {
            try {
                tleFilesRepository.downloadTleFile(file)
            } catch (e: UnknownHostException) {
                Log.d(TAG, "downloadFile(): UnknownHostException message: ${e.message}")
            } catch (e: Exception) {
                Log.d(TAG, "Could not download file: $file")
                e.printStackTrace()
            } finally {
                getFileList()
            }
        }
    }

    fun onDeleteFile(file: String) {
        viewModelScope.launch {
            tleFilesRepository.deleteFile(file)
            getFileList()
        }
    }

    class Factory(
        private val appSettingsRepository: AppSettingsRepository,
        private val tleFilesRepository: TleFilesRepository,
    ) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return UpdateScreenViewModel(
                appSettingsRepository = appSettingsRepository,
                tleFilesRepository = tleFilesRepository
            ) as T
        }
    }

}
