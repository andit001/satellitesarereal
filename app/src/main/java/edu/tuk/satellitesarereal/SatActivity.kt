package edu.tuk.satellitesarereal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import edu.tuk.satellitesarereal.ui.theme.SatellitesAreRealTheme
import edu.tuk.satellitesarereal.ui.viewmodels.UpdateScreenViewModel

@AndroidEntryPoint
class SatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SatellitesAreRealTheme {
                SatArApp()
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
                        selectedItem = 0
                        navController.navigate("StartScreen")
                    },
                )
                BottomNavigationItem(
                    icon = {
                        Icon(Icons.Filled.Star, contentDescription = null)
                    },
                    selected = selectedItem == 1,
                    onClick = {
                        selectedItem = 1
                        navController.navigate("FilterScreen")
                    },
                )
                BottomNavigationItem(
                    icon = {
                        Icon(Icons.Filled.ThumbUp, contentDescription = null)
                    },
                    selected = selectedItem == 2,
                    onClick = {
                        selectedItem = 2
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
                StartScreen()
            }
            composable(route = "FilterScreen") {
                FilterScreen()
            }
            composable(route = "UpdateScreen") {
                val updateScreenViewModel: UpdateScreenViewModel = hiltViewModel()
                UpdateScreen(updateScreenViewModel)
            }
        }
    }
}
