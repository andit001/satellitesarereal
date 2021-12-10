package edu.tuk.satellitesarereal

import android.content.Context
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationCallback
import dagger.hilt.android.AndroidEntryPoint
import edu.tuk.satellitesarereal.repositories.AppSettingsRepository
import edu.tuk.satellitesarereal.repositories.LocationRepository
import edu.tuk.satellitesarereal.services.DataStoreAppSettingsService
import edu.tuk.satellitesarereal.services.LocationService
import edu.tuk.satellitesarereal.ui.screens.ArScreen
import edu.tuk.satellitesarereal.ui.screens.InfoScreen
import edu.tuk.satellitesarereal.ui.screens.OptionsScreen
import edu.tuk.satellitesarereal.ui.screens.OptionsScreenViewModel
import edu.tuk.satellitesarereal.ui.theme.SatellitesAreRealTheme
import edu.tuk.satellitesarereal.ui.viewmodels.ArViewModel
import edu.tuk.satellitesarereal.ui.viewmodels.FilterScreenViewModel
import edu.tuk.satellitesarereal.ui.viewmodels.InfoScreenViewModel
import edu.tuk.satellitesarereal.ui.viewmodels.UpdateScreenViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import javax.inject.Inject


@AndroidEntryPoint
class SatActivity : ComponentActivity() {

    lateinit var locationService: LocationService

    override fun onResume() {
        super.onResume()

        // Start location service.
        locationService.startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()

        // Stop location service.
        locationService.stopLocationUpdates()
    }

    @ExperimentalPermissionsApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationService = LocationService(applicationContext)
        appSettingsRepository = DataStoreAppSettingsService(applicationContext)

        var interval = 2000L

        runBlocking {
            appSettingsRepository?.locationServiceInterval()?.take(1)?.collect {
                interval = it
            }
        }

        locationService.setUpdateInterval(interval)

        setContent {
            SatellitesAreRealTheme {
                val permissionsState = rememberMultiplePermissionsState(
                    listOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.CAMERA
                    )
                )

                PermissionScreen(permissionsState) {
                    SatArApp(locationService)
                }
            }
        }
    }
}

@ExperimentalPermissionsApi
@Composable
private fun PermissionScreen(
    permissionsState: MultiplePermissionsState,
    content: @Composable () -> Unit,
) {
    when {
        permissionsState.allPermissionsGranted -> {
            content()
        }
        else -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = {
                        permissionsState.launchMultiplePermissionRequest()
                    }
                ) {
                    Text("Get permissions")
                }
            }
        }
    }
}

@Composable
fun SatArApp(locationRepository: LocationRepository) {
    var selectedItem by rememberSaveable { mutableStateOf(0) }
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavigation {
                BottomNavigationItem(
                    icon = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = null)
                            Text("Filter")
                        }
                    },
                    selected = selectedItem == 0,
                    onClick = {
                        navController.navigate("FilterScreen")
                    },
                )
                BottomNavigationItem(
                    icon = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.Star, contentDescription = null)
                            Text("AR")
                        }
                    },
                    selected = selectedItem == 1,
                    onClick = {
                        navController.navigate("ArScreen")
                    },
                )
                BottomNavigationItem(
                    icon = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.Favorite, contentDescription = null)
                            Text("Info")
                        }
                    },
                    selected = selectedItem == 2,
                    onClick = {
                        navController.navigate("InfoScreen")
                    },
                )
                BottomNavigationItem(
                    icon = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.ThumbUp, contentDescription = null)
                            Text("Update")
                        }
                    },
                    selected = selectedItem == 3,
                    onClick = {
                        navController.navigate("UpdateScreen")
                    },
                )
                BottomNavigationItem(
                    icon = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.Settings, contentDescription = null)
                            Text("Options")
                        }
                    },
                    selected = selectedItem == 4,
                    onClick = {
                        navController.navigate("OptionsScreen")
                    },
                )
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = "FilterScreen",
            Modifier.padding(it)
        ) {
            composable(route = "FilterScreen") {
                selectedItem = 0
                val viewModel: FilterScreenViewModel = hiltViewModel()
                FilterScreen(viewModel)
            }
            composable(route = "ArScreen") {
                selectedItem = 1
                val viewModel: ArViewModel = hiltViewModel()
                viewModel.setLocationRepository(locationRepository)
                ArScreen(viewModel)
            }
            composable(route = "InfoScreen") {
                selectedItem = 2
                val viewModel: InfoScreenViewModel = hiltViewModel()
                viewModel.setLocationRepository(locationRepository)
                InfoScreen(viewModel)
            }
            composable(route = "UpdateScreen") {
                selectedItem = 3
                val viewModel: UpdateScreenViewModel = hiltViewModel()
                UpdateScreen(viewModel)
            }
            composable(route = "OptionsScreen") {
                selectedItem = 4
                val viewModel: OptionsScreenViewModel = hiltViewModel()
                OptionsScreen(viewModel)
            }
        }
    }
}