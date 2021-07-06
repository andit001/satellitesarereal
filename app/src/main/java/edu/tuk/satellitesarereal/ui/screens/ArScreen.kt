package edu.tuk.satellitesarereal.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.ar.sceneform.ArSceneView

@Composable
fun ArScreen() {
    Text("AR Screen")

    val arSceneView = AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            ArSceneView(context).apply{
                // TODO: Do stuff on the view.
            }
        }
    ) {
        // View's been inflated or state read in this block has been updated
        // Add logic here if necessary

        // As selectedItem is read here, AndroidView will recompose
        // whenever the state changes
        // Example of Compose -> View communication
    }

}