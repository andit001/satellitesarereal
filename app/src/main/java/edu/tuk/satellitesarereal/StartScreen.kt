package edu.tuk.satellitesarereal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rtbishop.look4sat.domain.predict4kotlin.DeepSpaceSat
import com.rtbishop.look4sat.domain.predict4kotlin.NearEarthSat
import com.rtbishop.look4sat.domain.predict4kotlin.Satellite
import com.rtbishop.look4sat.domain.predict4kotlin.TLE
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.tuk.satellitesarereal.model.SatelliteDatabase
import edu.tuk.satellitesarereal.model.TleEntry
import edu.tuk.satellitesarereal.repositories.TleFilesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.zip.ZipInputStream
import javax.inject.Inject

@HiltViewModel
class SomeViewModel @Inject constructor(
    val filesRepository: TleFilesRepository,
    val satelliteDatabase: SatelliteDatabase,
    val retrofitTleFilesRepository: TleFilesRepository,
) : ViewModel() {

    // Test-Code TODO

    private val _tleEntries: MutableStateFlow<List<TleEntry>> = MutableStateFlow(listOf())
    val tleEntries: StateFlow<List<TleEntry>> = _tleEntries


    init {
        getAllTleEntries()
    }

    private fun getAllTleEntries() {
        viewModelScope.launch {
            val dao = satelliteDatabase.tleEntryDao()

            dao.getAll().collect {
                _tleEntries.value = it
            }
        }
    }

    fun onToggleSelectSatellite(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val entry = _tleEntries.value.find { it.name == name }
            entry?.let {
                it.isSelected = !it.isSelected
                satelliteDatabase.tleEntryDao().updateTles(it)
            }
        }
    }

    fun onLoadTleData() {
        viewModelScope.launch(Dispatchers.IO) {
            val tleList: MutableList<TLE> = mutableListOf()
            filesRepository.listFiles().forEach {
                tleList.addAll(fileToTleList(it))
            }

            val satList: MutableList<Satellite> = mutableListOf()

            tleList.forEach {
                satList.add(
                    if (it.isDeepspace) {
                        DeepSpaceSat(it)
                    } else {
                        NearEarthSat(it)
                    }
                )
            }

            // Safe data to room db.
            val tleEntries: MutableList<TleEntry> = mutableListOf()

            tleList.forEach {
                tleEntries.add(TleEntry.fromTLE(it))
            }

            satelliteDatabase.tleEntryDao().insertTles(*tleEntries.toTypedArray())
        }
    }

    private fun fileToTleList(fileName: String): List<TLE> {
        // TODO handle exceptions smarter (i.e. keep them away from the view)
        filesRepository.openFile(fileName).use { file ->
            val inputStream =
                if (fileName.endsWith(".zip", true)) {
                    ZipInputStream(file).apply { nextEntry }
                } else {
                    file
                }

            return Satellite.importElements(inputStream)
        }

    }
}

@Composable
fun StartScreen(viewModel: SomeViewModel) {
    val tleEntries by viewModel.tleEntries.collectAsState()

    Column {
        Text("Room db experiments.")

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            viewModel.onLoadTleData()
        }) {
            Text("LOAD TLE DATA")
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn {
            items(tleEntries) { tle ->
                Row {
                    Checkbox(
                        checked = tle.isSelected,
                        onCheckedChange = {
                            viewModel.onToggleSelectSatellite(tle.name)
                        },
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