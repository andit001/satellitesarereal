package edu.tuk.satellitesarereal

import android.opengl.Matrix
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.rtbishop.look4sat.domain.predict4kotlin.Satellite
import com.rtbishop.look4sat.domain.predict4kotlin.StationPosition
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.sqrt
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

fun FloatArray.asVector4(): Satellite.Vector4 {
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
    return lhs[0] * rhs[0] + lhs[1] * rhs[1] + lhs[2] * rhs[2]
}

fun crossProduct(lhs: FloatArray, rhs: FloatArray): FloatArray {
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

// Helper to apply the transformations to vectors.
fun rotateVector(
    rotationMatrix: FloatArray,
    vector: FloatArray
): FloatArray {
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
    val arrowDistance = 2.0f

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

    return Offset(
        centerX + vec[0] * centerX,
        centerY - vec[1] * centerY
    )
}
