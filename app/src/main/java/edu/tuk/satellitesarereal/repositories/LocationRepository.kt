package edu.tuk.satellitesarereal.repositories

import android.location.Location

interface LocationRepository {
    fun getLastKnownLocation(callback: (Location?) -> Unit)
}