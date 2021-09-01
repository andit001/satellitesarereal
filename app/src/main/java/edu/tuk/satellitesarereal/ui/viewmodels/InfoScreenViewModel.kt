package edu.tuk.satellitesarereal.ui.viewmodels

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rtbishop.look4sat.domain.predict4kotlin.Satellite
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.tuk.satellitesarereal.model.SatelliteDatabase
import edu.tuk.satellitesarereal.repositories.LocationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InfoScreenViewModel @Inject constructor(
    val satelliteDatabase: SatelliteDatabase,
    val locationRepository: LocationRepository,
) : ViewModel() {

    private var getSatellitesJob: Job = Job()

    private val _selectedTles: MutableLiveData<List<Satellite>> = MutableLiveData()
    val selectedSatellites: LiveData<List<Satellite>> = _selectedTles

    private val _lastLocation: MutableLiveData<Location?> = MutableLiveData()
    val lastLocation: LiveData<Location?> = _lastLocation

    init {
        getSelectedSatellites()
        locationRepository.registerLocationListener {
            _lastLocation.postValue(it)
        }
    }

    private fun getSelectedSatellites() {
        getSatellitesJob.cancel()
        getSatellitesJob = viewModelScope.launch {
            satelliteDatabase
                .tleEntryDao()
                .getSelectedEntries()
                .collect { tleEntries ->
                    tleEntries
                        .map { it.toTLE() }
                        .mapNotNull {
                            Satellite.createSat(it)
                        }
                        .also { _selectedTles.postValue(it) }
                }
        }
    }
}
