package edu.tuk.satellitesarereal.repositories

interface OrientationRepository {
    fun addListener(listener: (rotationMatrix: FloatArray) -> Unit)
    fun removeListener()
}