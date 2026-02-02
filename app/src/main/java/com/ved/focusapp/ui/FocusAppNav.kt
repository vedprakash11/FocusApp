package com.ved.focusapp.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ved.focusapp.R
import com.ved.focusapp.data.PreferencesStorage
import com.ved.focusapp.timer.TimerStateHolder
import com.ved.focusapp.ui.home.HomeScreen
import com.ved.focusapp.ui.settings.SettingsScreen
import com.ved.focusapp.ui.stats.StatsScreen

const val ROUTE_HOME = "home"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_STATS = "stats"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusAppNav(
    viewModel: TimerStateHolder,
    storage: PreferencesStorage,
    navController: NavHostController = rememberNavController()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { navController.navigate(ROUTE_STATS) }) {
                        Icon(Icons.Default.BarChart, contentDescription = stringResource(R.string.stats_title))
                    }
                    IconButton(onClick = { navController.navigate(ROUTE_SETTINGS) }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                }
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_HOME,
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            composable(ROUTE_HOME) {
                HomeScreen(viewModel = viewModel)
            }
            composable(ROUTE_SETTINGS) {
                SettingsScreen(storage = storage)
            }
            composable(ROUTE_STATS) {
                StatsScreen(storage = storage)
            }
        }
    }
}
