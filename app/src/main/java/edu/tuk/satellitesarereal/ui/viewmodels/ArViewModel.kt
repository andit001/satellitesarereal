package edu.tuk.satellitesarereal.ui.viewmodels

import android.location.Location
import android.opengl.Matrix
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rtbishop.look4sat.domain.predict4kotlin.Satellite
import com.rtbishop.look4sat.domain.predict4kotlin.StationPosition
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.tuk.satellitesarereal.*
import edu.tuk.satellitesarereal.model.SatelliteDatabase
import edu.tuk.satellitesarereal.repositories.LocationRepository
import edu.tuk.satellitesarereal.repositories.OrientationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

private const val TAG = "SatAr::ArViewModel"

@HiltViewModel
class ArViewModel @Inject constructor(
    val satelliteDatabase: SatelliteDatabase,
    val locationRepository: LocationRepository,
    val orientationRepository: OrientationRepository,
) : ViewModel() {

    private var getSatellitesJob: Job = Job()

    private val _selectedSatellites: MutableLiveData<List<Satellite>> = MutableLiveData()
    val selectedSatellites: LiveData<List<Satellite>> = _selectedSatellites

    private val _lastLocation: MutableLiveData<Location?> = MutableLiveData()
     val lastLocation: LiveData<Location?> = _lastLocation

    private val _rotationMatrix: MutableLiveData<FloatArray?> = MutableLiveData()
    val rotationMatrix: LiveData<FloatArray?> = _rotationMatrix

    private val _eciToPhoneTransformationM: MutableLiveData<FloatArray?> = MutableLiveData(
        floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f,
        )
    )
    val eciToPhoneTransformationM: LiveData<FloatArray?> = _eciToPhoneTransformationM

    // Start/Stop called by DisposableEffect to make sure the listeners are unregistered as the
    // user navigates away from the ArScreen.
    fun onStart() {
        Log.d(TAG, "onStart() called.")
        getSelectedSatellites()
        locationRepository.registerLocationListener {
            _lastLocation.postValue(it)
            calculateEciToPhoneTransformationM()
        }
        orientationRepository.addListener {
            onReceiveRotationMatrix(it)
            calculateEciToPhoneTransformationM()
        }
    }

    fun onStop() {
        orientationRepository.removeListener()
        locationRepository.unregister()
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
                        .also {
                            _selectedSatellites.postValue(it)
                        }
                }
        }
    }

    private fun onReceiveRotationMatrix(rotationMatrix: FloatArray) {
//        Log.d("SatAr:ArViewModel", "Received $rotationMatrix")
        // TODO: composable doesn't get recomposed if we don't have a new
        //       reference to the matrix. Wait for update on this or make it
        //       more efficient by switching between two matrices.
        _rotationMatrix.postValue(rotationMatrix.copyOf())
    }

    private fun calculateEciToPhoneTransformationM() {
        viewModelScope.launch {

            // Calculate the rotation matrix for the observer coordinate system first.
            val satellites = selectedSatellites.value
            satellites?.let {
                val location = lastLocation.value
                location?.let {
                    val stationPosition = StationPosition(
                        location.latitude,
                        location.longitude,
                        location.altitude,
                    )

                    satellites.first().also { satellite ->
                        val obsPosVector = satellite.getObsPosVector(stationPosition, Date())

                        // Rename the axes to phone coordinate-naming. Use the fact that the
                        // direction of the obsPosVector is the same as the Z-Vector in the phone's
                        // coordinate system.
                        val obsZVector = FloatArray(4)
                        Matrix.multiplyMV(
                            obsZVector,
                            0,
                            transformAxesMatrix,
                            0,
                            obsPosVector.asFloatArray(),
                            0
                        )

                        // Normalize the vector.
                        obsZVector.normalize()

                        // Calculate the Y-Vector
                        // The upVector is the Z-Vector in ECI which is the same as the Y-Vector
                        // in phone coordinates.
                        val upVector = floatArrayOf(
                            0.0f,
                            1.0f,
                            0.0f,
                            1.0f
                        )

                        val prod = dotProduct(upVector, obsZVector)
                        val obsYVector = floatArrayOf(
                            (upVector[0] - obsZVector[0] * prod),
                            (upVector[1] - obsZVector[1] * prod),
                            (upVector[2] - obsZVector[2] * prod),
                            1f
                        ).normalize()

                        val obsXVector = crossProduct(obsYVector, obsZVector).normalize()

                        // Calculate the translation for the transformationMatrix.
                        // The origin of the ECI is 'moved' to the origin of the phone's
                        // coordinate system.

                        // Make sure the axes are in the right order.
                        val translation = FloatArray(4)
                        Matrix.multiplyMV(
                            translation,
                            0,
                            transformAxesMatrix,
                            0,
                            floatArrayOf(
                                -satellite.getObsPosVector(stationPosition, Date()).x.toFloat(),
                                -satellite.getObsPosVector(stationPosition, Date()).y.toFloat(),
                                -satellite.getObsPosVector(stationPosition, Date()).z.toFloat(),
                                1.0f,
                            ),
                            0
                        )

                        // Create a transformation matrix to map ECI coordinates to phone
                        // coordinates.

                        // (obsZVector will be parallel to the Z-Vector on the phone which is
                        // pointing to the sky. So we can fill the 3rd column of the transformation
                        // matrix with its values.)

                        val transformationMatrix = FloatArray(16).also {
                            it[0] = obsXVector[0]
                            it[4] = obsXVector[1]
                            it[8] = obsXVector[2]

                            it[1] = obsYVector[0]
                            it[5] = obsYVector[1]
                            it[9] = obsYVector[2]

                            it[2] = obsZVector[0]
                            it[6] = obsZVector[1]
                            it[10] = obsZVector[2]

                            it[12] = translation[0]
                            it[13] = translation[1]
                            it[14] = translation[2]

                            it[15] = 1.0f
                        }

                        // Apply the renaming of the axes.
                        var eciToPhoneTransformMatrix = FloatArray(16)
                        Matrix.multiplyMM(
                            eciToPhoneTransformMatrix,
                            0,
                            transformationMatrix,
                            0,
                            transformAxesMatrix,
                            0,
                        )

                        val scratch = FloatArray(16)
                        Matrix.multiplyMM(
                            scratch,
                            0,
                            rotationMatrix.value,
                            0,
                            eciToPhoneTransformMatrix,
                            0
                        )
                        eciToPhoneTransformMatrix = scratch.copyOf()

                        _eciToPhoneTransformationM.postValue(eciToPhoneTransformMatrix)
                    }
                }
            }
        }
    }
}
