package edu.tuk.satellitesarereal.services

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import dagger.hilt.android.qualifiers.ApplicationContext
import edu.tuk.satellitesarereal.repositories.LocationRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SatAr:LocationService"

@Singleton
class LocationService @Inject constructor(
    @ApplicationContext val context: Context,
) : LocationRepository {

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
        Log.d("SatAr: LocationService", "callFunc()")
        callback(location)
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val locationRequest = LocationRequest
            .create()
            .setInterval(2000)

        Log.d("SatAr: LocationService", "getInterval()=${locationRequest.interval}")

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null,
        )
    }

    fun stopLocationUpdates() {
        Log.d("SatAr: LocationService", "stopLocationUpdates()")
        fusedLocationClient.removeLocationUpdates(locationCallback)
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
}