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
    private var accuracy: Int = SENSOR_STATUS_UNRELIABLE
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
        if (accuracy == SENSOR_STATUS_UNRELIABLE) {
            return
        }
        if (event == null) {
            return
        }
        listener?.let {
            if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                getRotationMatrixFromVector(rotationMatrix, event.values)

                // defaultDisplay was marked as deprecated in API level 30. Use it only if the API
                // level is lower and use context.display otherwise.
                val display =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        context.display
                    } else {
                            @Suppress("DEPRECATION")
                            (context.getSystemService(WINDOW_SERVICE) as WindowManager)
                                .defaultDisplay
                    }

                display?.let {
                    val rotation = it.rotation
                    val result = FloatArray(16)

                    when (rotation) {
                        Surface.ROTATION_90 -> {
                            remapCoordinateSystem(
                                rotationMatrix,
                                AXIS_Y,
                                AXIS_MINUS_X,
                                result
                            )
                            it(result)
                        }
                        Surface.ROTATION_270 -> {
                            remapCoordinateSystem(
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
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        this.accuracy = accuracy
    }
}