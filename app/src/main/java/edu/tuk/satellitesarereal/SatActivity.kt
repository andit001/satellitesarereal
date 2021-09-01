package edu.tuk.satellitesarereal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.AndroidEntryPoint
import edu.tuk.satellitesarereal.ui.screens.ArScreen
import edu.tuk.satellitesarereal.ui.screens.InfoScreen
import edu.tuk.satellitesarereal.ui.theme.SatellitesAreRealTheme
import edu.tuk.satellitesarereal.ui.viewmodels.ArViewModel
import edu.tuk.satellitesarereal.ui.viewmodels.FilterScreenViewModel
import edu.tuk.satellitesarereal.ui.viewmodels.InfoScreenViewModel
import edu.tuk.satellitesarereal.ui.viewmodels.UpdateScreenViewModel

@AndroidEntryPoint
class SatActivity : ComponentActivity() {

    @ExperimentalPermissionsApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SatellitesAreRealTheme {
                val permissionsState = rememberMultiplePermissionsState(
                    listOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.CAMERA
                    )
                )

                PermissionScreen(permissionsState) {
                    SatArApp()
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
fun SatArApp() {
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
                            Icon(Icons.Filled.Star, contentDescription = null)
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
                ArScreen(viewModel)
            }
            composable(route = "InfoScreen") {
                selectedItem = 2
                val viewModel: InfoScreenViewModel = hiltViewModel()
                InfoScreen(viewModel)
            }
            composable(route = "UpdateScreen") {
                selectedItem = 3
                val viewModel: UpdateScreenViewModel = hiltViewModel()
                UpdateScreen(viewModel)
            }
        }
    }
}