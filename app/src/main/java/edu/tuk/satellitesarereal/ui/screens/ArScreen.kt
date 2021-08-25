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
import androidx.lifecycle.LifecycleOwner
import com.rtbishop.look4sat.domain.predict4kotlin.Satellite
import com.rtbishop.look4sat.domain.predict4kotlin.StationPosition
import edu.tuk.satellitesarereal.*
import edu.tuk.satellitesarereal.ui.viewmodels.ArViewModel
import java.util.*
import kotlin.math.abs


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

//            drawGizmo(rotationMatrix, projectionMatrix, canvasWidth, canvasHeight)
        }
    }
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