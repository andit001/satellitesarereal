package edu.tuk.satellitesarereal

import android.location.Location
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rtbishop.look4sat.domain.predict4kotlin.Satellite
import com.rtbishop.look4sat.domain.predict4kotlin.StationPosition
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.tuk.satellitesarereal.model.SatelliteDatabase
import edu.tuk.satellitesarereal.repositories.LocationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SomeViewModel @Inject constructor(
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
        locationRepository.getLastKnownLocation {
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

@Composable
fun StartScreen(viewModel: SomeViewModel) {
    val selectedSatellites by viewModel.selectedSatellites.observeAsState()
    val lastLocation by viewModel.lastLocation.observeAsState()

    Column {
        Text("TLE & GPS experiments.")

        lastLocation?.let {
            Text(
                text = "Last location: Alt: ${it.altitude} Long: ${it.longitude}"
            )
        }

        LazyColumn {
            selectedSatellites?.let { it ->
                items(it) { satellite ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.Gray),
                        elevation = 5.dp,
                    ) {
                        Row {
                            Text(
                                text = satellite.tle.name,
                                modifier = Modifier.padding(16.dp),
                            )
                            Column(
                                modifier = Modifier.padding(16.dp),
                            ) {
                                lastLocation?.let { location ->
                                    val stationPosition = StationPosition(
                                        location.latitude,
                                        location.longitude,
                                        location.altitude,
                                    )

                                    val satPos = satellite.getPosition(stationPosition, Date())
                                    Text("Latitude=${satPos.latitude}")
                                    Text("Longitude=${satPos.longitude}")
                                    Text("Altitude=${satPos.altitude}")
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}