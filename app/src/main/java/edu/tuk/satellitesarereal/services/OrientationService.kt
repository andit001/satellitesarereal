package edu.tuk.satellitesarereal.services

import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.*
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import dagger.hilt.android.qualifiers.ApplicationContext
import edu.tuk.satellitesarereal.repositories.OrientationRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG: String = "SatAr:OrientationService"

@Singleton
class OrientationService @Inject constructor(
    @ApplicationContext val context: Context,
    private val sensorManager: SensorManager
) : OrientationRepository, SensorEventListener {

    private var listener: ((rotationMatrix: FloatArray) -> Unit)? = null
    private var accuracy: Int = SensorManager.SENSOR_STATUS_UNRELIABLE
    private val rotationMatrix = FloatArray(16)

    override fun registerListener(listener: (rotationMatrix: FloatArray) -> Unit) {
        if (listener != this.listener) {

            if (this.listener != null) {
                sensorManager.unregisterListener(this)
            }

            sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.also {
                sensorManager.registerListener(
                    this,
                    it,
                    33000,
                )
            }

            this.listener = listener
        } else {
            Log.d(TAG, "WARNING: Tried to add the same listener twice.")
        }
    }

    override fun unregisterListener() {
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
        listener?.let {
            if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                val rotation = (context.getSystemService(WINDOW_SERVICE) as WindowManager)
                    .defaultDisplay
                    .rotation

                val result = FloatArray(16)
                when (rotation) {
                    Surface.ROTATION_90 -> {
                        SensorManager.remapCoordinateSystem(
                            rotationMatrix,
                            AXIS_Y,
                            AXIS_MINUS_X,
                            result
                        )
                        it(result)
                    }
                    Surface.ROTATION_270 -> {
                        SensorManager.remapCoordinateSystem(
                            rotationMatrix,
                            AXIS_MINUS_Y,
                            AXIS_X,
                            result
                        )
                        it(result)
                    }
                    else -> it(rotationMatrix)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        this.accuracy = accuracy
    }
}