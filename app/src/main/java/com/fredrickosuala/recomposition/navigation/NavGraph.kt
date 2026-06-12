package com.fredrickosuala.recomposition.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fredrickosuala.recomposition.core.LabScaffold
import com.fredrickosuala.recomposition.home.HomeScreen
import com.fredrickosuala.recomposition.model.allLabs
import com.fredrickosuala.recomposition.tooling.ToolingScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val topLevelRoutes = setOf(Screen.Home.route, Screen.Tooling.route)

    Scaffold(
        topBar = {
            if (currentRoute in topLevelRoutes) {
                TopAppBar(title = { Text("Recompose Lab") })
            }
        },
        bottomBar = {
            if (currentRoute in topLevelRoutes) {
                AppBottomBar(navController = navController, currentRoute = currentRoute)
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onLabClick = { labId ->
                        navController.navigate(Screen.LabDetail(labId).route)
                    },
                    contentPadding = paddingValues,
                )
            }
            composable(Screen.Tooling.route) {
                ToolingScreen(contentPadding = paddingValues)
            }
            composable(
                route = Screen.LabDetail.ROUTE,
                arguments = listOf(navArgument("labId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val labId = backStackEntry.arguments?.getString("labId") ?: return@composable
                val lab = allLabs.find { it.id == labId } ?: return@composable
                LabScaffold(
                    lab = lab,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun AppBottomBar(navController: NavController, currentRoute: String?) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Screen.Home.route,
            onClick = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text("Labs") },
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Tooling.route,
            onClick = {
                navController.navigate(Screen.Tooling.route) {
                    popUpTo(Screen.Home.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Filled.Build, contentDescription = null) },
            label = { Text("Tooling") },
        )
    }
}
