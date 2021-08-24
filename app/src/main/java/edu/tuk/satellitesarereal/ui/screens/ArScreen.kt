package edu.tuk.satellitesarereal.ui.screens

import android.annotation.SuppressLint
import android.location.Location
import android.opengl.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.rtbishop.look4sat.domain.predict4kotlin.Satellite
import com.rtbishop.look4sat.domain.predict4kotlin.StationPosition
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.tuk.satellitesarereal.model.SatelliteDatabase
import edu.tuk.satellitesarereal.repositories.LocationRepository
import edu.tuk.satellitesarereal.repositories.OrientationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import kotlin.math.abs
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

private val transformAxesMatrix = floatArrayOf(
    0.0f, 0.0f, 1.0f, 0.0f,
    1.0f, 0.0f, 0.0f, 0.0f,
    0.0f, 1.0f, 0.0f, 0.0f,
    0.0f, 0.0f, 0.0f, 1.0f,
)

fun createProjectionMatrix(w: Float, h: Float, n: Float, f: Float): FloatArray {
    return floatArrayOf(
        2.0f * n / w, 0.0f, 0.0f, 0.0f,
        0.0f, 2.0f * n / h, 0.0f, 0.0f,
        0.0f, 0.0f, -(f + n) / (f - n), -1.0f,
        0.0f, 0.0f, -2.0f * n * f / (f - n), 0.0f
    )
}

private fun rotateVector(
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

private fun projectVector(
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

//                    Log.d("SatAr:ArScreen", "projected vec len: ${Matrix.length(projectedVector[0], projectedVector[1], projectedVector[2])}")
//                    Log.d("SatAr:ArScreen", "projected vec: ${projectedVector[0]} ${projectedVector[1]} ${projectedVector[2]} ${projectedVector[3]}")
    return projectedVector
}

// Helper
//private fun printMatrix(matrix: FloatArray) {
//    for ((i, v)  in matrix.withIndex()) {
//        if (i != 0 && i % 4 == 0) {
//            print('\n')
//        }
//        print("$v ")
//    }
//}

@HiltViewModel
class ArViewModel @Inject constructor(
    val satelliteDatabase: SatelliteDatabase,
    locationRepository: LocationRepository,
    orientationRepository: OrientationRepository,
) : ViewModel() {

    private var getSatellitesJob: Job = Job()

    private val _selectedSatellites: MutableLiveData<List<Satellite>> = MutableLiveData()
    val selectedSatellites: LiveData<List<Satellite>> = _selectedSatellites

    private val _lastLocation: MutableLiveData<Location?> = MutableLiveData()
    val lastLocation: LiveData<Location?> = _lastLocation

    private val _rotationMatrix: MutableLiveData<FloatArray?> = MutableLiveData()
    val rotationMatrix: LiveData<FloatArray?> = _rotationMatrix

    private val _eciToPhoneTransformationM: MutableLiveData<FloatArray?> = MutableLiveData(
        floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f,
        )
    )
    val eciToPhoneTransformationM: LiveData<FloatArray?> = _eciToPhoneTransformationM

    init {
        getSelectedSatellites()
        locationRepository.getLastKnownLocation {
            _lastLocation.postValue(it)
            calculateEciToPhoneTransformationM()
        }
        orientationRepository.addListener {
            onReceiveRotationMatrix(it)
            calculateEciToPhoneTransformationM()
        }
    }

    private fun getSelectedSatellites() {
        getSatellitesJob.cancel()
        getSatellitesJob = viewModelScope.launch {
            satelliteDatabase
                .tleEntryDao()
                .getSelectedEntries()
                .collect { tleEntries ->
                    tleEntries
                        .map { it.toTLE() }
                        .mapNotNull {
                            Satellite.createSat(it)
                        }
                        .also {
                            _selectedSatellites.postValue(it)
                        }
                }
        }
    }

    private fun onReceiveRotationMatrix(rotationMatrix: FloatArray) {
//        Log.d("SatAr:ArViewModel", "Received $rotationMatrix")
        // TODO: composable doesn't get recomposed if we don't have a new
        //       reference to the matrix. Wait for update on this or make it
        //       more efficient by switching between two matrices.
        _rotationMatrix.postValue(rotationMatrix.copyOf())
    }

    private fun calculateEciToPhoneTransformationM() {
        viewModelScope.launch {

            // Calculate the rotation matrix for the observer coordinate system first.
            val satellites = selectedSatellites.value
            satellites?.let {
                val location = lastLocation.value
                location?.let {
                    val stationPosition = StationPosition(
                        location.latitude,
                        location.longitude,
                        location.altitude,
                    )

                    satellites.first().also { satellite ->
                        val obsPosVector = satellite.getObsPosVector(stationPosition, Date())

                        // Rename the axes to phone coordinate-naming. Use the fact that the
                        // direction of the obsPosVector is the same as the Z-Vector in the phone's
                        // coordinate system.
                        val obsZVector = FloatArray(4)
                        Matrix.multiplyMV(
                            obsZVector,
                            0,
                            transformAxesMatrix,
                            0,
                            obsPosVector.asFloatArray(),
                            0
                        )

                        // Normalize the vector.
                        obsZVector.normalize()

                        // Calculate the Y-Vector
                        // The upVector is the Z-Vector in ECI which is the same as the Y-Vector
                        // in phone coordinates.
                        val upVector = floatArrayOf(
                            0.0f,
                            1.0f,
                            0.0f,
                            1.0f
                        )

                        val prod = dotProduct(upVector, obsZVector)
                        val obsYVector = floatArrayOf(
                            (upVector[0] - obsZVector[0] * prod),
                            (upVector[1] - obsZVector[1] * prod),
                            (upVector[2] - obsZVector[2] * prod),
                            1f
                        ).normalize()

//                        fun crossProduct(lhs: FloatArray, rhs: FloatArray): FloatArray {
//                            return floatArrayOf(
//                                lhs[1] * rhs[2] - lhs[2] * rhs[1],
//                                lhs[2] * rhs[0] - lhs[0] * rhs[2],
//                                lhs[0] * rhs[1] - lhs[1] * rhs[0],
//                                1.0f
//                            )
//                        }

                        val obsXVector = crossProduct(obsYVector, obsZVector).normalize()

//                        val obsXVector = floatArrayOf(
//                            obsYVector[1] * obsZVector[2] - obsYVector[2] * obsZVector[1],
//                            obsYVector[2] * obsZVector[0] - obsYVector[0] * obsZVector[2],
//                            obsYVector[0] * obsZVector[1] - obsYVector[1] * obsZVector[0],
//                            1f
//                        ).normalize()

                        // Calculate the translation for the transformationMatrix.
                        // The origin of the ECI is 'moved' to the origin of the phone's
                        // coordinate system.

                        // Make sure the axes are in the right order.
                        val translation = FloatArray(4)
                        Matrix.multiplyMV(
                            translation,
                            0,
                            transformAxesMatrix,
                            0,
                            floatArrayOf(
                                -satellite.getObsPosVector(stationPosition, Date()).x.toFloat(),
                                -satellite.getObsPosVector(stationPosition, Date()).y.toFloat(),
                                -satellite.getObsPosVector(stationPosition, Date()).z.toFloat(),
                                1.0f,
                            ),
                            0
                        )

                        // Create a transformation matrix to map ECI coordinates to phone
                        // coordinates.

                        // (obsZVector will be parallel to the Z-Vector on the phone which is
                        // pointing to the sky. So we can fill the 3rd column of the transformation
                        // matrix with its values.)

                        val transformationMatrix = FloatArray(16).also {
                            it[0] = obsXVector[0]
                            it[4] = obsXVector[1]
                            it[8] = obsXVector[2]

                            it[1] = obsYVector[0]
                            it[5] = obsYVector[1]
                            it[9] = obsYVector[2]

                            it[2] = obsZVector[0]
                            it[6] = obsZVector[1]
                            it[10] = obsZVector[2]

                            it[12] = translation[0]
                            it[13] = translation[1]
                            it[14] = translation[2]

                            it[15] = 1.0f
                        }

                        // Apply the renaming of the axes.
                        var eciToPhoneTransformMatrix = FloatArray(16)
                        Matrix.multiplyMM(
                            eciToPhoneTransformMatrix,
                            0,
                            transformationMatrix,
                            0,
                            transformAxesMatrix,
                            0,
                        )

                        val scratch = FloatArray(16)
                        Matrix.multiplyMM(
                            scratch,
                            0,
                            rotationMatrix.value,
                            0,
                            eciToPhoneTransformMatrix,
                            0
                        )
                        eciToPhoneTransformMatrix = scratch.copyOf()

                        _eciToPhoneTransformationM.postValue(eciToPhoneTransformMatrix)
                    }
                }
            }
        }
    }
}

@Composable
fun ArScreen(viewModel: ArViewModel) {

    val satellites by viewModel.selectedSatellites.observeAsState()
    val lastLocation by viewModel.lastLocation.observeAsState()
    val rotationMatrix by viewModel.rotationMatrix.observeAsState()
    val eciToPhoneTransformationM by viewModel.eciToPhoneTransformationM.observeAsState()

    Box(
        Modifier.fillMaxSize()
    ) {
        CameraPreview(
            Modifier.matchParentSize()
        )
        RenderSatellites(
            Modifier.matchParentSize(),
            lastLocation,
            satellites,
            rotationMatrix,
            eciToPhoneTransformationM
        )
    }
}

@Composable
private fun RenderSatellites(
    modifier: Modifier,
    lastLocation: Location?,
    satellites: List<Satellite>?,
    rotationMatrix: FloatArray?,
    eciToPhoneTransformationM: FloatArray?
) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
//        Log.d("SatAr", "canvas: $canvasWidth, $canvasHeight")
        // Create projection matrix with a fov of 90 degrees.
        val projectionMatrix = createProjectionMatrix(
            canvasWidth,
            canvasHeight,
            canvasWidth / 2.0f,
            ((canvasWidth / 2.0f) + 50000.0f)
        )

        if (lastLocation != null &&
            satellites != null &&
            rotationMatrix != null &&
            eciToPhoneTransformationM != null
        ) {
            val stationPosition = StationPosition(
                lastLocation.latitude,
                lastLocation.longitude,
                lastLocation.altitude,
            )

            satellites
                .asSequence()
                .map { it.getSatPosVector(stationPosition, Date()) }
                .map { satVector ->
                    eciToPhoneTransformationM.let {
                        val transformedSatVector = FloatArray(4)
                        Matrix.multiplyMV(
                            transformedSatVector,
                            0,
                            it,
                            0,
                            satVector.asFloatArray(),
                            0,
                        )
                        transformedSatVector
                    }
                }
                .map { satVector -> projectVector(projectionMatrix, satVector) }
                .filter {
                    // Clip satellites which can't be seen.
                    abs(it[3]) >= abs(it[0]) &&
                            abs(it[3]) >= abs(it[1]) &&
                            abs(it[3]) >= abs(it[2])
                }
                .toList()
                .onEach { coordinates ->
                    val offset = calculateOffset(
                        null,
                        canvasWidth,
                        canvasHeight,
                        coordinates
                    )

                    drawCircle(
                        color = Color.Magenta,
                        radius = 10.0f,
                        center = offset
                    )
                }

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
    }
}

private fun DrawScope.drawArrow(
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

private fun calculateOffset(
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

@Composable
fun CameraPreview(modifier: Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val preview = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                bindPreview(
                    lifecycleOwner,
                    preview,
                    cameraProvider,
                )
            }, executor)
            preview
        },
        modifier = modifier,
    )
}

@SuppressLint("UnsafeExperimentalUsageError")
private fun bindPreview(
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    cameraProvider: ProcessCameraProvider,
//    analyzer: ImageAnalysis.Analyzer,
) {
    val preview = Preview.Builder().build().also {
        it.setSurfaceProvider(previewView.surfaceProvider)
    }

    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
        .build()

    cameraProvider.unbindAll()
    cameraProvider.bindToLifecycle(
        lifecycleOwner,
        cameraSelector,
        preview
    )
}

//class MyGLRenderer : GLSurfaceView.Renderer {
//
//    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
//        // Set the background frame color
//        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 0.5f)
//    }
//
//    override fun onDrawFrame(unused: GL10) {
//        // Redraw background color
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
//    }
//
//    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
//        GLES20.glViewport(0, 0, width, height)
//    }
//}
//
//class MyGLSurfaceView(context: Context) : GLSurfaceView(context) {
//
//    private val renderer: MyGLRenderer
//
//    init {
//
//        // Create an OpenGL ES 2.0 context
//        setEGLContextClientVersion(2)
//
//        renderer = MyGLRenderer()
//
//        // Set the Renderer for drawing on the GLSurfaceView
//        setRenderer(renderer)
//    }
//}