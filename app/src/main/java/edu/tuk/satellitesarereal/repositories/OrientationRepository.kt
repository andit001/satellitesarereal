package edu.tuk.satellitesarereal.repositories

import com.rtbishop.look4sat.domain.predict4kotlin.Satellite

interface OrientationRepository {
    fun addListener(listener: (rotationMatrix: FloatArray) -> Unit)
    fun removeListener()
}