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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
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

                Log.d("SatAr", "hasPermission=$hasPermission")

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


@Composable
fun SatArApp() {
    var selectedItem by rememberSaveable { mutableStateOf(0) }
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavigation {
                BottomNavigationItem(
                    icon = {
                        Icon(Icons.Filled.Favorite, contentDescription = null)
                    },
                    selected = selectedItem == 0,
                    onClick = {
                        navController.navigate("StartScreen")
                    },
                )
                BottomNavigationItem(
                    icon = {
                        Icon(Icons.Filled.Star, contentDescription = null)
                    },
                    selected = selectedItem == 1,
                    onClick = {
                        navController.navigate("FilterScreen")
                    },
                )
                BottomNavigationItem(
                    icon = {
                        Icon(Icons.Filled.ThumbUp, contentDescription = null)
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
            startDestination = "StartScreen",
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
fun PermissionScreen(context: Context, hasPermission: Boolean, onUpdatePermission: (Boolean) -> Unit) {
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

//    if ( ContextCompat.checkSelfPermission(
//            context,
//            Manifest.permission.ACCESS_FINE_LOCATION
//        ) == PackageManager.PERMISSION_GRANTED) {
//        onUpdatePermission(true)
//    }

    if (!hasPermission) {
        // Check permission
        Button(
            onClick = {
//                    launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                when (PackageManager.PERMISSION_GRANTED) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ),
                    -> {
                        // Some works that require permission
                        onUpdatePermission(true)
                        Log.d("SatAr: PermissionScreen", "Code requires permission")
                    }
                    else -> {
                        // Asking for permission
                        launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }
            }
        ) {
            Text("Get permission")
        }
    }
}

//@Composable
//fun PermissionScreen1(updatePermission: (Boolean) -> Unit) {
//    val launcher = rememberLauncherForActivityResult(
//        ActivityResultContracts.RequestPermission()
//    ) { isGranted: Boolean ->
//        if (isGranted) {
//            // Permission Accepted: Do something
//            updatePermission(true)
//            Log.d("SatAr:PermissionScreen", "PERMISSION GRANTED")
//
//        } else {
//            // Permission Denied: Do something
//            updatePermission(false)
//            Log.d("SatAr:PermissionScreen", "PERMISSION DENIED")
//        }
//    }
//    val context = LocalContext.current
//
//    // Check permission
//    when (PackageManager.PERMISSION_GRANTED) {
//        ContextCompat.checkSelfPermission(
//            context,
//            Manifest.permission.ACCESS_FINE_LOCATION
//        ),
//        -> {
//            // Some works that require permission
//            updatePermission(true)
//            Log.d("SatAr: PermissionScreen", "Code requires permission")
//        }
//        else -> {
//            // Asking for permission
//            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
//        }
//    }
//}
