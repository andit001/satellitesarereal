package edu.tuk.satellitesarereal.ui.screens

import android.annotation.SuppressLint
import android.location.Location
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
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

    return position
}

fun Satellite.getSatPosVector(pos: StationPosition, time: Date): Satellite.Vector4 {
    // Make sure getPosition was called first, so that the private field is set.
    getPosition(pos, time)
    return Satellite::class
        .declaredMemberProperties
        .firstOrNull { it.name == "position" }
        ?.apply { isAccessible = true }
        ?.get(this) as Satellite.Vector4
}

@HiltViewModel
class ArViewModel @Inject constructor(
    val satelliteDatabase: SatelliteDatabase,
    val locationRepository: LocationRepository,
) : ViewModel() {

    data class Sat3dPos(val x: Double, val y: Double, val z: Double, val satellite: Satellite)

    private var getSatellitesJob: Job = Job()

    private val _selectedSatellites: MutableLiveData<List<Satellite>> = MutableLiveData()
    val selectedSatellites: LiveData<List<Satellite>> = _selectedSatellites

    private val _lastLocation: MutableLiveData<Location?> = MutableLiveData()
    val lastLocation: LiveData<Location?> = _lastLocation

    init {
        getSelectedSatellites()
        locationRepository.getLastKnownLocation {
            _lastLocation.postValue(it)
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
}

fun SatTransformObsToOrigin(
    obsVector: Satellite.Vector4,
    satVector: Satellite.Vector4,
): Satellite.Vector4 {
    return satVector
        .apply {
            x -= obsVector.x
            y -= obsVector.y
            z -= obsVector.z
            w = sqrt(x * x + y * y + z * z)
        }
}

@Composable
fun ArScreen(viewModel: ArViewModel) {

    val satellites = viewModel.selectedSatellites.observeAsState()
    val lastLocation = viewModel.lastLocation.observeAsState()

    Box(
        Modifier.fillMaxSize()
    ) {
//        SimpleCameraPreview(
//            Modifier.matchParentSize()
//        )

        Column(
            Modifier.matchParentSize()
        ) {
            Text("Vectors:")
            lastLocation.value?.let { location ->
                val stationPosition = StationPosition(
                    location.latitude,
                    location.longitude,
                    location.altitude
                )

                satellites.value
                    ?.apply {
                        if (!isEmpty()) {
                            first()?.let { satellite ->
                                val obsPosVector =
                                    satellite.getObsPosVector(stationPosition, Date())
                                Text(
                                    "Obs Vector: x=%.${2}f".format(obsPosVector.x) +
                                            " y=%.${2}f".format(obsPosVector.y) +
                                            " z=%.${2}f".format(obsPosVector.z)
                                )
                            }
                        }
                    }
                    ?.forEach { satellite ->
                        val obsPosVector = satellite.getObsPosVector(stationPosition, Date())
                        val satPosVector = satellite.getSatPosVector(stationPosition, Date())
                        Text(
                            "Vector: x=%.${2}f".format(satPosVector.x) +
                                    " y=%.${2}f".format(satPosVector.y) +
                                    " z=%.${2}f".format(satPosVector.z)
                        )

                        val satTransVector = SatTransformObsToOrigin(obsPosVector, satPosVector)
                        Text(
                            "Trans Vector: x=%.${2}f".format(satTransVector.x) +
                                    " y=%.${2}f".format(satTransVector.y) +
                                    " z=%.${2}f".format(satTransVector.z)
                        )
                    }
            }
        }
    }

//        Canvas(modifier = Modifier.fillMaxSize()) {
//            val canvasWidth = size.width
//            val canvasHeight = size.height
//
//            drawLine(
//                start = Offset(
//                    x = canvasWidth,
//                    y = 0f
//                ),
//                end = Offset(
//                    x = 0f,
//                    y = canvasHeight
//                ),
//                color = Color.Blue,
//                strokeWidth = 5F
//            )

//        AugmentCamera(
//            Modifier.matchParentSize()
//        )
//        }
}


//@Composable
//fun AugmentCamera(modifier: Modifier) {
//    AndroidView(
//        factory = {
//            MyGLSurfaceView(it)
//        },
//        modifier = modifier
//    )
//}


@Composable
fun SimpleCameraPreview(modifier: Modifier) {
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
                    executor
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
    executor: Executor,
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