package edu.tuk.satellitesarereal.services

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import dagger.hilt.android.qualifiers.ApplicationContext
import edu.tuk.satellitesarereal.repositories.LocationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationService @Inject constructor(
    @ApplicationContext val context: Context,
) : LocationRepository {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private lateinit var locationCallback: LocationCallback

    private var callback: (Location?) -> Unit = {}

    @SuppressLint("MissingPermission")
    override fun registerLocationListener(callback: (Location?) -> Unit) {

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    callback(location)
                }
            }
        }

        val locationRequest = LocationRequest
            .create()
            .setInterval(15000)

        Log.d("SatAr: LocationService", "getInterval()=${locationRequest.interval}")

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper(),
        )

        this.callback = callback
    }

    override fun unregister() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}