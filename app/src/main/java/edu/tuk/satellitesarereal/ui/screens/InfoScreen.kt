package edu.tuk.satellitesarereal.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rtbishop.look4sat.domain.predict4kotlin.StationPosition
import edu.tuk.satellitesarereal.ui.viewmodels.InfoScreenViewModel
import java.util.*

@Composable
fun InfoScreen(viewModel: InfoScreenViewModel) {
    val selectedSatellites by viewModel.selectedSatellites.observeAsState()
    val lastLocation by viewModel.lastLocation.observeAsState()

    Column {
        lastLocation?.let {
            Text(
                "Location of the phone:",
                fontWeight = FontWeight.Bold,
            )
            Text("Latitude=${it.latitude}")
            Text("Longitude=${it.longitude}")
            Text("Altitude=${it.altitude}")
        }

        Spacer(Modifier.height(4.dp))

        Text(
            "Selected satellites:",
            fontWeight = FontWeight.Bold,
        )

        Divider(
            color = Color.Black,
            thickness = 1.dp,
        )

        Spacer(Modifier.height(1.dp))

        LazyColumn {
            selectedSatellites?.let { it ->
                items(it) { satellite ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.Gray),
                        elevation = 5.dp,
                    ) {
                        Row {
                            Text(
                                text = satellite.tle.name,
                                modifier = Modifier.padding(16.dp),
                                fontWeight = FontWeight.Bold,
                            )
                            Column(
                                modifier = Modifier.padding(16.dp),
                            ) {
                                lastLocation?.let { location ->
                                    val stationPosition = StationPosition(
                                        location.latitude,
                                        location.longitude,
                                        location.altitude,
                                    )

                                    val satPos = satellite.getPosition(stationPosition, Date())
                                    Text("Latitude=${satPos.latitude}")
                                    Text("Longitude=${satPos.longitude}")
                                    Text("Altitude=${satPos.altitude}")
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}