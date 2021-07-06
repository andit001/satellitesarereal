package edu.tuk.satellitesarereal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import edu.tuk.satellitesarereal.ui.screens.SomeViewModel
import edu.tuk.satellitesarereal.ui.screens.StartScreen
import edu.tuk.satellitesarereal.ui.theme.SatellitesAreRealTheme
import edu.tuk.satellitesarereal.ui.viewmodels.FilterScreenViewModel
import edu.tuk.satellitesarereal.ui.viewmodels.UpdateScreenViewModel

@AndroidEntryPoint
class SatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        var hasPermission: Boolean = false
        setContent {
            SatellitesAreRealTheme {
                var hasPermission by rememberSaveable { mutableStateOf(false) }

                hasPermission = checkPermission(applicationContext)

                if (hasPermission) {
                    SatArApp()
                } else {
                    PermissionScreen(applicationContext, hasPermission) {
                        hasPermission = it
                        Log.d("SatAr", "lambda hasPermission=$hasPermission")
                    }
                }
            }
        }
    }
}


fun checkPermission(context: Context): Boolean {
    return when (PackageManager.PERMISSION_GRANTED) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ),
        -> true
        else -> false
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
                            Icon(Icons.Filled.Favorite, contentDescription = null)
                            Text("Start")
                        }
                    },
                    selected = selectedItem == 0,
                    onClick = {
                        navController.navigate("StartScreen")
                    },
                )
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
                    selected = selectedItem == 1,
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
                            Icon(Icons.Filled.ThumbUp, contentDescription = null)
                            Text("Update")
                        }
                    },
                    selected = selectedItem == 2,
                    onClick = {
                        navController.navigate("UpdateScreen")
                    },
                )
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = "UpdateScreen",
            Modifier.padding(it)
        ) {
            composable(route = "StartScreen") {
                selectedItem = 0


                val viewModel: SomeViewModel = hiltViewModel()
                StartScreen(viewModel)
            }
            composable(route = "FilterScreen") {
                selectedItem = 1
                val viewModel: FilterScreenViewModel = hiltViewModel()
                FilterScreen(viewModel)
            }
            composable(route = "UpdateScreen") {
                selectedItem = 2
                val viewModel: UpdateScreenViewModel = hiltViewModel()
                UpdateScreen(viewModel)
            }
        }
    }
}

@Composable
fun PermissionScreen(
    context: Context,
    hasPermission: Boolean,
    onUpdatePermission: (Boolean) -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission Accepted: Do something
            onUpdatePermission(true)
            Log.d("SatAr:PermissionScreen", "PERMISSION GRANTED")

        } else {
            // Permission Denied: Do something
            onUpdatePermission(false)
            Log.d("SatAr:PermissionScreen", "PERMISSION DENIED")

        }
    }

    if (!hasPermission) {
        // Check permission
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = {
                    if (!checkPermission(context)) {
                        launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }
            ) {
                Text("Get permission")
            }
        }
    }
}