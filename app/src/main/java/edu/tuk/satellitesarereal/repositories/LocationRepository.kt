package edu.tuk.satellitesarereal.repositories

import android.location.Location

interface LocationRepository {
    fun registerLocationListener(callback: (Location?) -> Unit)
    fun unregister()
    fun setUpdateInterval(interval: Long)
}