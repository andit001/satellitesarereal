package edu.tuk.satellitesarereal.ui.viewmodels

import android.util.Log
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.tuk.satellitesarereal.repositories.AppSettingsRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateScreenViewModel @Inject constructor(
    val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    companion object {
        private const val TAG = "SatAr:SomeViewModel"
    }

    private var _urls: MutableLiveData<List<String>> = MutableLiveData()
    val urls: LiveData<List<String>> get() = _urls

    init {
        viewModelScope.launch {
            readUrls()
        }
    }


    private suspend fun readUrls() {
        appSettingsRepository.tleUrls().collect { value ->
            if (value.isEmpty()) {
                _urls.postValue(defaultSources())
                Log.d(TAG, "readUrls: postValue(${defaultSources()}) executed")
            } else {
                Log.d(TAG, "readUrls: postValue($value) executed")
                _urls.postValue(value)
            }
        }
    }

    private fun defaultSources(): List<String> {
        return listOf(
            "https://celestrak.com/NORAD/elements/active.txt",
            "https://www.prismnet.com/~mmccants/tles/classfd.zip",
            "https://amsat.org/tle/current/nasabare.txt",
            "https://www.prismnet.com/~mmccants/tles/inttles.zip"
        )
    }

    fun onAddTleUrl(url: String) {
        viewModelScope.launch {
            val newUrls = _urls.value
                ?.toMutableList()
                ?.apply { add(url) }
                ?.distinct()
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

    class Factory(private val appSettingsRepository: AppSettingsRepository) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return UpdateScreenViewModel(
                appSettingsRepository = appSettingsRepository
            ) as T
        }
    }

}
