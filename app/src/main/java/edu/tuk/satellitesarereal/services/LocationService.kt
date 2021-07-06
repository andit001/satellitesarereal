package edu.tuk.satellitesarereal.services

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import dagger.hilt.android.qualifiers.ApplicationContext
import edu.tuk.satellitesarereal.repositories.LocationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationService @Inject constructor(
    @ApplicationContext val context: Context,
) : LocationRepository {

    class NoPermissionException : Exception()

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private lateinit var locationCallback: LocationCallback

    private var callback: (Location?) -> Unit = {}

    private fun checkPermission(): Boolean {
        // TODO: Does not work as intended.
        return ActivityCompat.checkSelfPermission(context,
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    override fun getLastKnownLocation(callback: (Location?) -> Unit) {
//        if (checkPermission()) {
//            throw NoPermissionException()
//        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                Log.d("SatAr: LocationService", "Callback called.")
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

        fusedLocationClient.setMockMode(true)

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper(),
        )

        this.callback = callback
    }
}