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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import kotlinx.coroutines.Job
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

    private var filterJob: Job = Job()

    private val _tleEntries: MutableLiveData<List<TleEntry>> = MutableLiveData()
    val tleEntries: LiveData<List<TleEntry>> = _tleEntries

    init {
        getFilteredEntries("")
    }

    private fun getFilteredEntries(subString: String) {
        // Make sure the flow gets cancelled.
        filterJob.cancel()

        // Start a new flow with the new filter properties.
        filterJob = viewModelScope.launch {
            if (subString.isNotEmpty()) {
                satelliteDatabase.tleEntryDao().getFilteredEntries("%$subString%")
            } else {
                satelliteDatabase.tleEntryDao().getAll()
            }
                .collect {
                    _tleEntries.postValue(it)
                }
        }
    }

    fun onFilterEntries(subString: String) {
        getFilteredEntries(subString)
    }

    fun onSelectAll() {
        viewModelScope.launch {
            tleEntries.value
                ?.map {
                    TleEntry
                        .deepCopy(it)
                        .apply { isSelected = true }
                }
                ?.toTypedArray()
                ?.also { satelliteDatabase.tleEntryDao().updateTles(*it) }
        }
    }

    fun onDeselectAll() {
        viewModelScope.launch {
            tleEntries.value
                ?.map {
                    TleEntry
                        .deepCopy(it)
                        .apply { isSelected = false }
                }
                ?.toTypedArray()
                ?.also { satelliteDatabase.tleEntryDao().updateTles(*it) }
        }
    }

    fun onToggleSelectSatellite(name: String) {
        viewModelScope.launch {
            // INFO: A deep copy of the element is created, such that when it is updated, it is
            //       considered a new entry in the list. Using the original element to call
            //       updateTles() seems not to recognize it as a change as the reference is the
            //       same. TODO: Further explore this and confirm/drop this idea.
            tleEntries.value
                ?.find { it.name == name }
                ?.let {
                    val newEntry = TleEntry.deepCopy(it)
                    newEntry.isSelected = !it.isSelected
                    satelliteDatabase.tleEntryDao().updateTles(newEntry)
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
            return Satellite.importElements(
                if (fileName.endsWith(".zip", true)) {
                    ZipInputStream(file).apply { nextEntry }
                } else {
                    file
                }
            )
        }
    }
}

@Composable
fun StartScreen(viewModel: SomeViewModel) {
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
}