package edu.tuk.satellitesarereal.repositories

interface OrientationRepository {
    fun registerListener(listener: (rotationMatrix: FloatArray) -> Unit)
    fun unregisterListener()
}