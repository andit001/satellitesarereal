package edu.tuk.satellitesarereal

import androidx.compose.foundation.clickable
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
fun FilterScreen(viewModel: FilterScreenViewModel) {
    val tleEntries by viewModel.tleEntries.observeAsState()
    var inputText by remember { mutableStateOf("") }

    Column {
        Text("Room db experiments.")

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            viewModel.onLoadTleData()
        }) {
            Text("LOAD TLE DATA")
        }

        OutlinedTextField(
            value = inputText,
            onValueChange = {
                inputText = it
                viewModel.onFilterEntries(it)
            }
        )

        Row {
            Button(onClick = {
                viewModel.onSelectAll()
            }) {
                Text("SELECT ALL")
            }
            Button(onClick = {
                viewModel.onDeselectAll()
            }) {
                Text("DESELECT ALL")
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn {
            tleEntries?.let {
                items(it) { tle ->
                    Row(
                        modifier = Modifier.clickable {
                            viewModel.onToggleSelectSatellite(tle.name)
                        }
                    ) {
                        Checkbox(
                            checked = tle.isSelected,
                            onCheckedChange = {
                                viewModel.onToggleSelectSatellite(tle.name)
                            },
                            modifier = Modifier.padding(
                                top = 24.dp,
                                bottom = 24.dp,
                                start = 16.dp,
                            ),
                        )
                        Text(
                            text = tle.name,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                    Divider(color = Color.Black)
                }
            }
        }
    }
}