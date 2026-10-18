package com.linqi.triplog.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.linqi.triplog.ui.detail.TripDetailScreen
import com.linqi.triplog.ui.edit.TripEditScreen
import com.linqi.triplog.ui.list.TripListScreen
import com.linqi.triplog.ui.me.AiSettingsScreen
import com.linqi.triplog.ui.me.BackupScreen
import com.linqi.triplog.ui.me.MeScreen
import com.linqi.triplog.ui.stats.StatsScreen

@Composable
fun AppNav(openTripId: Long? = null) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(openTripId) {
        if (openTripId != null && openTripId > 0) {
            navController.navigate(Routes.detail(openTripId))
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentRoute == Routes.LIST || currentRoute == Routes.ME) {
                BottomBar(navController, currentRoute)
            }
        },
        floatingActionButton = {
            // 多选模式下 TripListScreen 自己会不显示 FAB，这里也用路由判断兜一层
            if (currentRoute == Routes.LIST) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Routes.add()) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("添加") }
                )
            }
        }
    ) { inner ->
        Box(modifier = Modifier.fillMaxSize().padding(inner)) {
            NavHost(navController = navController, startDestination = Routes.LIST) {

                composable(Routes.LIST) {
                    TripListScreen(
                        onOpenDetail = { id -> navController.navigate(Routes.detail(id)) },
                        onAdd = { navController.navigate(Routes.add()) }
                    )
                }

                composable(Routes.ME) {
                    MeScreen(
                        onOpenAiSettings = { navController.navigate(Routes.AI_SETTINGS) },
                        onOpenBackup = { navController.navigate(Routes.BACKUP) },
                        onOpenStats = { navController.navigate(Routes.STATS) }
                    )
                }

                composable(Routes.AI_SETTINGS) {
                    AiSettingsScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.BACKUP) {
                    BackupScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.STATS) {
                    StatsScreen(onBack = { navController.popBackStack() })
                }

                composable(
                    route = Routes.DETAIL,
                    arguments = listOf(navArgument("tripId") { type = NavType.LongType })
                ) { entry ->
                    val id = entry.arguments?.getLong("tripId") ?: 0L
                    TripDetailScreen(
                        tripId = id,
                        onBack = { navController.popBackStack() },
                        onEdit = { tripId -> navController.navigate(Routes.add(tripId)) }
                    )
                }

                composable(
                    route = Routes.ADD,
                    arguments = listOf(
                        navArgument("tripId") {
                            type = NavType.LongType
                            defaultValue = 0L
                        }
                    )
                ) { entry ->
                    val tripId = entry.arguments?.getLong("tripId") ?: 0L
                    TripEditScreen(
                        tripId = tripId.takeIf { it > 0 },
                        onDone = { navController.popBackStack() },
                        onCancel = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Routes.LIST,
            onClick = { navController.navigateTab(Routes.LIST) },
            icon = {
                Icon(
                    if (currentRoute == Routes.LIST) Icons.Filled.DateRange else Icons.Outlined.DateRange,
                    contentDescription = "行程"
                )
            },
            label = { Text("行程") }
        )
        NavigationBarItem(
            selected = currentRoute == Routes.ME,
            onClick = { navController.navigateTab(Routes.ME) },
            icon = {
                Icon(
                    if (currentRoute == Routes.ME) Icons.Filled.Person else Icons.Outlined.Person,
                    contentDescription = "我的"
                )
            },
            label = { Text("我的") }
        )
    }
}

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
