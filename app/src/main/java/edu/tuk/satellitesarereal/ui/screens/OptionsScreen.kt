package edu.tuk.satellitesarereal.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.RadioButton
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.tuk.satellitesarereal.appSettingsRepository
import edu.tuk.satellitesarereal.repositories.AppSettingsRepository
import edu.tuk.satellitesarereal.repositories.LocationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OptionsScreenViewModel @Inject constructor(
    val appSettingsRepository: AppSettingsRepository,
    val locationRepository: LocationRepository,
): ViewModel() {

    private val _locationServiceInterval: MutableLiveData<Long> = MutableLiveData()
    val locationServiceInterval: LiveData<Long> = _locationServiceInterval

    private var getLocationServiceIntervalJob: Job = Job()

    private val _fieldOfView: MutableLiveData<Float> = MutableLiveData()
    val fieldOfView: LiveData<Float> = _fieldOfView

    private var getFieldOfViewJob: Job = Job()

    init {
        getLocationServiceInterval()
        getFieldOfView()
    }

    private fun getLocationServiceInterval() {
        getLocationServiceIntervalJob.cancel()
        getLocationServiceIntervalJob = viewModelScope.launch {
            appSettingsRepository
                .locationServiceInterval()
                .collect {
                    _locationServiceInterval.postValue(it)
                }
        }
    }

    fun onChangeLocationServiceInterval(interval: Long) {
        locationRepository.setUpdateInterval(interval)
        viewModelScope.launch {
            appSettingsRepository
                .saveLocationServiceInterval(interval)
        }
    }

    private fun getFieldOfView() {
        getFieldOfViewJob.cancel()
        getFieldOfViewJob = viewModelScope.launch {
            appSettingsRepository
                .fieldOfView()
                .collect {
                    _fieldOfView.postValue(it)
                }
        }
    }

    fun onChangeFoV(fieldOfView: Float) {
        _fieldOfView.value = fieldOfView
        viewModelScope.launch {
            appSettingsRepository.saveFieldOfView(fieldOfView)
        }
    }
}

@Composable
fun OptionsScreen(viewModel: OptionsScreenViewModel) {
    val locationServiceInterval by viewModel.locationServiceInterval.observeAsState()
    val fieldOfView by viewModel.fieldOfView.observeAsState()

    Column() {
        Text("Location Update Interval: $locationServiceInterval")

        Row(Modifier.selectableGroup()) {
            Text("Slow")
            RadioButton(
                selected = locationServiceInterval == 4000L,
                onClick = {
                    viewModel.onChangeLocationServiceInterval(4000L)
                }
            )
            Text("Normal")
            RadioButton(
                selected = locationServiceInterval == 2000L,
                onClick = {
                    viewModel.onChangeLocationServiceInterval(2000L)
                }
            )
            Text("Fast")
            RadioButton(
                selected = locationServiceInterval == 1000L,
                onClick = {
                    viewModel.onChangeLocationServiceInterval(1000L)
                }
            )
        }

        Text("Field of View: $fieldOfView")
            Slider(
                value = fieldOfView ?: 45f,
                onValueChange = {
                    viewModel.onChangeFoV(it)
                },
                valueRange = 10f..180f,
            )
    }
}