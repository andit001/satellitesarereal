package edu.tuk.satellitesarereal.ui.screens

import android.annotation.SuppressLint
import android.location.Location
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.rtbishop.look4sat.domain.predict4kotlin.Satellite
import com.rtbishop.look4sat.domain.predict4kotlin.StationPosition
import edu.tuk.satellitesarereal.*
import edu.tuk.satellitesarereal.ui.viewmodels.ArViewModel
import java.util.*
import kotlin.math.abs

data class DrawableSat(val sat: Satellite, var coordinates: FloatArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DrawableSat

        if (sat != other.sat) return false
        if (!coordinates.contentEquals(other.coordinates)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sat.hashCode()
        result = 31 * result + coordinates.contentHashCode()
        return result
    }
}

@Composable
fun ArScreen(viewModel: ArViewModel) {
    var satelliteToShow: DrawableSat? by remember {
        mutableStateOf(null)
    }

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart()

        onDispose { viewModel.onStop() }
    }

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
            eciToPhoneTransformationM,
            satelliteToShow
        ) {
            satelliteToShow = it
        }

        Card {
            Column {
                satelliteToShow?.let { drawableSat ->
                    Text(drawableSat.sat.tle.name)
                    lastLocation?.let {
                        val stationPosition = StationPosition(
                            it.latitude,
                            it.longitude,
                            it.altitude,
                        )
                        val stationVector = drawableSat.sat.getObsPosVector(
                            stationPosition,
                            Date()
                        )

                        val satelliteVector = drawableSat.sat.getSatPosVector(
                            stationPosition,
                            Date()
                        )


                        val satPos = drawableSat.sat.getPosition(stationPosition, Date())
                        Text("Latitude=%.2f".format(satPos.latitude))
                        Text("Longitude=%.2f".format(satPos.longitude))
                        Text("Altitude=%.2f km".format(satPos.altitude))

                        val distance = (satelliteVector - stationVector).magnitude()
                        Text("Distance=%.2f km".format(distance))
                    }
                }
            }
        }
    }
}

private fun getSatellitesOnScreen(
    canvasDimensions: Offset,
    lastLocation: Location,
    eciToPhoneTransformationM: FloatArray,
    satellites: List<Satellite>
): List<DrawableSat> {
    val projectionMatrix = createProjectionMatrix(
        canvasDimensions.x,
        canvasDimensions.y,
        canvasDimensions.x / 2.0f,
        ((canvasDimensions.y / 2.0f) + 50000.0f)
    )

    val stationPosition = StationPosition(
        lastLocation.latitude,
        lastLocation.longitude,
        lastLocation.altitude,
    )


    // Model-View-Projection matrix (the real mvp).
    val mvpMatrix = multiplyMM(projectionMatrix, eciToPhoneTransformationM)

    return satellites
        .asSequence()
        .map {
            DrawableSat(
                it,
                it.getSatPosVector(stationPosition, Date()).asFloatArray()
            )
        }
        .map {
            it.coordinates = multiplyMV(mvpMatrix, it.coordinates)
            it
        }
        .filter {
            val vector = it.coordinates
            // Clip satellites which can't be seen.
            abs(vector[3]) >= abs(vector[0]) &&
                    abs(vector[3]) >= abs(vector[1]) &&
                    abs(vector[3]) >= abs(vector[2])
        }
        .toList()
}


@Composable
private fun RenderSatellites(
    modifier: Modifier,
    lastLocation: Location?,
    satellites: List<Satellite>?,
    rotationMatrix: FloatArray?,
    eciToPhoneTransformationM: FloatArray?,
    satelliteToShow: DrawableSat?,
    onSatToShow: (sat: DrawableSat) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        Canvas(modifier = modifier) {
            if (lastLocation != null &&
                satellites != null &&
                eciToPhoneTransformationM != null
            ) {
                val satsToShow = getSatellitesOnScreen(
                    Offset(size.width, size.height),
                    lastLocation,
                    eciToPhoneTransformationM,
                    satellites
                ).onEach {
                    drawSatellite(it)
                }

                val sat = satsToShow.minByOrNull {
                    val satVec = it.coordinates
                    val targetVec = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f)

                    floatArrayOf(
                        targetVec[0] - satVec[0],
                        targetVec[1] - satVec[1],
                        0.0f,
                        0.0f,
                    )
                        .asVector4()
                        .magnitude()
                }

                sat?.let { onSatToShow(it) }

                drawCursor()

//            drawGizmo(rotationMatrix, projectionMatrix, canvasWidth, canvasHeight)
            }
        }
    }
}

private fun DrawScope.drawCursor() {
    val pos = Offset(
        size.width / 2,
        size.height / 2
    )

    drawCircle(
        color = Color.Black,
        radius = 20.0f,
        center = pos,
        style = Stroke(
            width = 4f
        )
    )

    // Top
    drawLine(
        color = Color.Black,
        start = Offset(
            size.width / 2,
            size.height / 2 - 80,
        ),
        end = Offset(
            size.width / 2,
            size.height / 2 - 20,
        ),
        strokeWidth = 4f
    )

    // Bottom
    drawLine(
        color = Color.Black,
        start = Offset(
            size.width / 2,
            size.height / 2 + 80,
        ),
        end = Offset(
            size.width / 2,
            size.height / 2 + 20,
        ),
        strokeWidth = 4f
    )

    // Left
    drawLine(
        color = Color.Black,
        start = Offset(
            size.width / 2 - 80,
            size.height / 2,
        ),
        end = Offset(
            size.width / 2 - 20,
            size.height / 2,
        ),
        strokeWidth = 4f
    )

    // Right
    drawLine(
        color = Color.Black,
        start = Offset(
            size.width / 2 + 80,
            size.height / 2,
        ),
        end = Offset(
            size.width / 2 + 20,
            size.height / 2,
        ),
        strokeWidth = 4f
    )
}

private fun DrawScope.drawSatellite(it: DrawableSat) {
    val coordinates = it.coordinates

    val offset = calculateOffset(
        null,  // Vector is projected already.
        size.width,
        size.height,
        coordinates
    )

    drawCircle(
        color = Color.Magenta,
        radius = 10.0f,
        center = offset
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