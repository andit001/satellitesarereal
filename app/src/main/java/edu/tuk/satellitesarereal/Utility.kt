package edu.tuk.satellitesarereal

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.opengl.Matrix
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.rtbishop.look4sat.domain.predict4kotlin.SatPos
import com.rtbishop.look4sat.domain.predict4kotlin.Satellite
import com.rtbishop.look4sat.domain.predict4kotlin.StationPosition
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

// Extension functions for Satellite.
// Here an evil trick is used to access private variables and methods of Satellite by the use of
// reflection. This way it is not necessary to change the original class to get the data. It is
// important to keep in mind that things might break when the original library is updated.

fun Satellite.getObsPosVector(pos: StationPosition, time: Date): Satellite.Vector4 {
    // Using calculations from Satellite.kt
    val currentDaynum = Satellite::class
        .declaredMemberFunctions
        .firstOrNull { it.name == "calcCurrentDaynum" }
        ?.apply { isAccessible = true }
        ?.call(this, time) as Double

    val julUtc = currentDaynum + 2444238.5
    val position = Satellite.Vector4()
    val gsPosTheta = AtomicReference<Double>()

    Satellite::class
        .declaredMemberFunctions
        .firstOrNull { it.name == "calculateUserPosVel" }
        ?.apply { isAccessible = true }
        ?.call(this, julUtc, pos, gsPosTheta, position, Satellite.Vector4())

    return position.apply { w = 1.0 }
}


/**
 * Returns the satelites position in ECI koordinates as kilometers.
 *
 * @param pos The position of the station (the observer).
 * @param time Point in time to get the satellites position.
 *
 * @return Satellite.Vector4 containing the coordinates in ECI coordinates.
 */
fun Satellite.getSatPosVector(pos: StationPosition, time: Date): Satellite.Vector4 {
    // Make sure getPosition was called first, so that the private field is set.
    getPosition(pos, time)
    val position = Satellite::class
        .declaredMemberProperties
        .firstOrNull { it.name == "position" }
        ?.apply { isAccessible = true }
        ?.get(this) as Satellite.Vector4

    return position.apply { w = 1.0 }
}

// Helper functions for vector math.
fun Satellite.Vector4.magnitude(): Double {
    return sqrt(x * x + y * y + z * z)
}

fun Satellite.Vector4.asFloatArray(): FloatArray {
    return FloatArray(4).apply {
        set(0, x.toFloat())
        set(1, y.toFloat())
        set(2, z.toFloat())
        set(3, w.toFloat())
    }
}

operator fun Satellite.Vector4.minus(rhs: Satellite.Vector4): Satellite.Vector4 {
    val lhs = this
    return Satellite.Vector4().apply {
        x = lhs.x - rhs.x
        y = lhs.y - rhs.y
        z = lhs.z - rhs.z
    }
}

fun Satellite.Vector4?.asString(): String {
    if (this == null) {
        return "null"
    }

    return "x=${this.x} y=${this.y} z=${this.z} w=${this.w} mag=${this.magnitude()}"
}

fun SatPos.latDeg(): Double {
    return Math.toDegrees(latitude)
}

fun SatPos.lonDeg(): Double {
    var ret = Math.toDegrees(longitude)

    while (ret < -180) {
        ret += 360
    }

    while (ret > 180) {
        ret -= 360
    }

    return ret
}

fun FloatArray.asVector4(): Satellite.Vector4 {
    assert(this.size == 4)
    return Satellite.Vector4().apply {
        x = get(0).toDouble()
        y = get(1).toDouble()
        z = get(2).toDouble()
        w = get(3).toDouble()
    }
}

fun FloatArray.normalize(): FloatArray {
    val magnitude = asVector4().magnitude().toFloat()
    this[0] /= magnitude
    this[1] /= magnitude
    this[2] /= magnitude

    return this
}

fun multiplyMM(lhs: FloatArray, rhs: FloatArray): FloatArray {
    assert(lhs.size == 16 && rhs.size == 16)
    val scratch = FloatArray(16)
    Matrix.multiplyMM(
        scratch,
        0,
        lhs,
        0,
        rhs,
        0
    )

    return scratch
}

fun multiplyMV(lhs: FloatArray, rhs: FloatArray): FloatArray {
    assert(lhs.size == 16 && rhs.size == 4)
    val scratch = FloatArray(4)
    Matrix.multiplyMV(
        scratch,
        0,
        lhs,
        0,
        rhs,
        0,
    )

    return scratch
}

fun dotProduct(lhs: FloatArray, rhs: FloatArray): Float {
    assert(lhs.size == 4 && rhs.size == 4)
    return lhs[0] * rhs[0] + lhs[1] * rhs[1] + lhs[2] * rhs[2]
}

fun crossProduct(lhs: FloatArray, rhs: FloatArray): FloatArray {
    assert(lhs.size == 4 && rhs.size == 4)
    return floatArrayOf(
        lhs[1] * rhs[2] - lhs[2] * rhs[1],
        lhs[2] * rhs[0] - lhs[0] * rhs[2],
        lhs[0] * rhs[1] - lhs[1] * rhs[0],
        1.0f
    )
}

// Note: android.opengl.Matrix is an OpenGl function and as such it expects matrices
// to be in column-major. That is M[offset+0], M[offset+1], M[offset+2], M[offset+4]
// represent the column offset. This is why in the notation the matrices
// are transposed.

// Permutates the Axes from ECI to OpenGL naming. Only the naming of the axes in the
// two coordinate systems are different.
val transformAxesMatrix = floatArrayOf(
    0.0f, 0.0f, 1.0f, 0.0f,
    1.0f, 0.0f, 0.0f, 0.0f,
    0.0f, 1.0f, 0.0f, 0.0f,
    0.0f, 0.0f, 0.0f, 1.0f,
)

// The projection matrix to project from eye coordinates to clip coordinates.
fun createProjectionMatrix(w: Float, h: Float, n: Float, f: Float): FloatArray {
    return floatArrayOf(
        2.0f * n / w, 0.0f, 0.0f, 0.0f,
        0.0f, 2.0f * n / h, 0.0f, 0.0f,
        0.0f, 0.0f, -(f + n) / (f - n), -1.0f,
        0.0f, 0.0f, -2.0f * n * f / (f - n), 0.0f
    )
}

/**
 * Creates a projection matrix considering the given perspective.
 *
 * @param FoVx the field of view in degrees. Can't be 180° and should be smaller than 270°.
 * @param aspectRatio the aspect ration of the render surface (width/height)
 * @param near distance of the near plane to the camera
 * @param far distance of the far plane to the camera
 * @return a FloatArray(16) representing the projection matrix
 */
fun createPerspective(FoVx: Float, aspectRatio: Float, near: Float, far: Float): FloatArray {
    // Make sure we do not have wrong values.
    assert(FoVx < 270.0f && FoVx != 180.0f)

    val fov = Math.toRadians(FoVx.toDouble()).toFloat()

    // Divide by 2 so that we can use an angle of 90 degrees for the FoV.
    val width = 2.0f * tan(fov / 2.0f) * near
    val height = width * (1.0f / aspectRatio)

    return createProjectionMatrix(width, height, near, far)
}

// Helper to apply the transformations to vectors.
fun rotateVector(
    rotationMatrix: FloatArray,
    vector: FloatArray
): FloatArray {
    assert(rotationMatrix.size == 16 && vector.size == 4)
    val newVector = FloatArray(4)
    Matrix.multiplyMV(
        newVector,
        0,
        rotationMatrix,
        0,
        vector,
        0,
    )
    return newVector
}

fun projectVector(
    projectionMatrix: FloatArray,
    satVector: FloatArray
): FloatArray {
    val projectedVector = FloatArray(4)
    Matrix.multiplyMV(
        projectedVector,
        0,
        projectionMatrix,
        0,
        satVector,
        0,
    )
    return projectedVector
}

// Can be handy sometimes.
fun printMatrix(matrix: FloatArray) {
    for ((i, v) in matrix.withIndex()) {
        if (i != 0 && i % 4 == 0) {
            print('\n')
        }
        print("$v ")
    }
}

// The gizmo shows the three axes in "phone coordinates".
// X: Green (East)
// Y: Blue (North)
// Z: Red (Up)
fun DrawScope.drawGizmo(
    rotationMatrix: FloatArray,
    projectionMatrix: FloatArray,
    canvasWidth: Float,
    canvasHeight: Float
) {
    val arrowDistance = 4.0f

    // X-Arrow (east)
    var arrowStart = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)
    var arrowEnd = floatArrayOf(.5f, 0.0f, 0.0f, 1.0f)

    arrowStart = rotateVector(rotationMatrix, arrowStart)
    arrowEnd = rotateVector(rotationMatrix, arrowEnd)

    arrowStart[2] -= arrowDistance
    arrowEnd[2] -= arrowDistance

    drawArrow(
        projectionMatrix,
        Color.Green,
        arrowStart,
        arrowEnd,
        canvasWidth,
        canvasHeight
    )

    // Y-Arrow (north)
    arrowStart = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)
    arrowEnd = floatArrayOf(0.0f, .5f, 0.0f, 1.0f)

    arrowStart = rotateVector(rotationMatrix, arrowStart)
    arrowEnd = rotateVector(rotationMatrix, arrowEnd)

    arrowStart[2] -= arrowDistance
    arrowEnd[2] -= arrowDistance

    drawArrow(
        projectionMatrix,
        Color.Blue,
        arrowStart,
        arrowEnd,
        canvasWidth,
        canvasHeight
    )

    // Z-Arrow (up)
    arrowStart = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)
    arrowEnd = floatArrayOf(0.0f, 0.0f, 0.5f, 1.0f)

    arrowStart = rotateVector(rotationMatrix, arrowStart)
    arrowEnd = rotateVector(rotationMatrix, arrowEnd)

    arrowStart[2] -= arrowDistance
    arrowEnd[2] -= arrowDistance

    drawArrow(
        projectionMatrix,
        Color.Red,
        arrowStart,
        arrowEnd,
        canvasWidth,
        canvasHeight
    )
}

fun DrawScope.drawArrow(
    projectionMatrix: FloatArray?,
    color: Color,
    arrowStart: FloatArray,
    arrowEnd: FloatArray,
    canvasWidth: Float,
    canvasHeight: Float,
    strokeWidth: Float = 2f
) {

    val offsetStart = calculateOffset(
        projectionMatrix,
        canvasWidth,
        canvasHeight,
        arrowStart
    )

    val offsetEnd = calculateOffset(
        projectionMatrix,
        canvasWidth,
        canvasHeight,
        arrowEnd
    )

    drawLine(
        color,
        offsetStart,
        offsetEnd,
        strokeWidth
    )

    drawCircle(
        color = color,
        radius = 10.0f,
        center = offsetStart
    )
}

fun calculateOffset(
    projectionMatrix: FloatArray?,
    canvasWidth: Float,
    canvasHeight: Float,
    vector: FloatArray
): Offset {
    val centerX = canvasWidth / 2.0f
    val centerY = canvasHeight / 2.0f

    var vec = vector
    projectionMatrix?.let {
        vec = projectVector(projectionMatrix, vec)
    }
    vec[0] /= vec[3]
    vec[1] /= vec[3]

    if (vec[0].isNaN()) {
        vec[0] = 0.0f
    }
    if (vec[1].isNaN()) {
        vec[1] = 0.0f
    }

    return Offset(
        centerX + vec[0] * centerX,
        centerY - vec[1] * centerY
    )
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }

        context = context.baseContext
    }

    return null
}