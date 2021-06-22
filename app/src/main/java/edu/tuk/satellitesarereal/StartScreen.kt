package edu.tuk.satellitesarereal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import edu.tuk.satellitesarereal.ui.viewmodels.FilterScreenViewModel

@Composable
fun StartScreen() {
    Column {
        Text("TLE experiments.")
    }
}