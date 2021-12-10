package edu.tuk.satellitesarereal.services

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import dagger.hilt.android.qualifiers.ApplicationContext
import edu.tuk.satellitesarereal.repositories.AppSettingsRepository
import edu.tuk.satellitesarereal.repositories.LocationRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SatAr:LocationService"

@Singleton
class LocationService @Inject constructor(
    @ApplicationContext val context: Context,
) : LocationRepository {

    private var isStarted = false

    private var updateInterval = 2000L

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult ?: return
            for (location in locationResult.locations) {
                callFunc(location)
            }
        }
    }

    private var callback: (Location?) -> Unit = {}

    private fun callFunc(location: Location) {
//        Log.d("SatAr: LocationService", "callFunc()")
        callback(location)
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val locationRequest = LocationRequest
            .create()
            .setInterval(updateInterval.toLong())

//        Log.d("SatAr: LocationService", "getInterval()=${locationRequest.interval}")

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper(),
        )

        isStarted = true
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)

        isStarted = false
    }

    @SuppressLint("MissingPermission")
    override fun registerLocationListener(callback: (Location?) -> Unit) {
        this.callback = callback

        // Hand over the last location if available.
        fusedLocationClient.lastLocation.addOnSuccessListener {
            it?.let {
                callFunc(it)
            }
        }
    }

    override fun unregister() {
        callback = {}
//        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun setUpdateInterval(interval: Long) {
        updateInterval = interval

        if (callback != {} && isStarted) {
            stopLocationUpdates()
            startLocationUpdates()
        }
    }
}