package edu.tuk.satellitesarereal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun FilterScreen() {
    val satellites = List(1000) { "Satellite #$it" }
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Filter Screen")

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(satellites) { satellite ->
                Text(
                    text = satellite,
                    modifier = Modifier.padding(24.dp),
                )
                Divider(color = Color.Black)
            }
        }
    }
}

@Preview
@Composable
fun PreviewFilterScreen() {
    Surface(
        Modifier.background(Color.White)
    ) {
        FilterScreen()
    }
}
