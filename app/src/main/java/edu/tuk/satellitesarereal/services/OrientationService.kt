package edu.tuk.satellitesarereal.services

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.rtbishop.look4sat.domain.predict4kotlin.Satellite.Vector4
import edu.tuk.satellitesarereal.repositories.OrientationRepository
import edu.tuk.satellitesarereal.ui.screens.magnitude
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG: String = "SatAr:OrientationService"

@Singleton
class OrientationService @Inject constructor(
    private val sensorManager: SensorManager
) : OrientationRepository, SensorEventListener {

    private var listener: ((rotationMatrix: FloatArray) -> Unit)? = null
    private var accuracy: Int = SensorManager.SENSOR_STATUS_UNRELIABLE
    private val rotationMatrix = FloatArray(16)
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)

    override fun addListener(listener: (rotationMatrix: FloatArray) -> Unit) {
        if (listener != this.listener) {
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
                sensorManager.registerListener(
                    this,
                    it,
                    11000,
                )
            }
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also {
                sensorManager.registerListener(
                    this,
                    it,
                    11000,
                )
            }
            this.listener = listener
        } else {
            Log.d(TAG, "WARNING: Tried to add the same listener twice.")
        }
    }

    override fun removeListener() {
        sensorManager.unregisterListener(this)
        listener = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return
        }
        if (event == null) {
            return
        }
        if (listener != null) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(
                    event.values,
                    0,
                    accelerometerReading,
                    0,
                    accelerometerReading.size
                )
            }
            if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(
                    event.values,
                    0,
                    magnetometerReading,
                    0,
                    magnetometerReading.size
                )
            }

            SensorManager.getRotationMatrix(
                rotationMatrix,
                null,
                accelerometerReading,
                magnetometerReading,
            )

            listener?.let { it(rotationMatrix) }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        this.accuracy = accuracy
    }
}