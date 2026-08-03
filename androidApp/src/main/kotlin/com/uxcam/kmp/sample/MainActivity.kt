package com.uxcam.kmp.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.uxcam.kmp.UXCamKMP
import com.uxcam.kmp.sample.nav.Destination
import com.uxcam.kmp.sample.nav.TABS
import com.uxcam.kmp.sample.screens.CheckoutScreen
import com.uxcam.kmp.sample.screens.ConsoleScreen
import com.uxcam.kmp.sample.screens.DebugScreen
import com.uxcam.kmp.sample.screens.EditProfileScreen
import com.uxcam.kmp.sample.screens.HomeScreen
import com.uxcam.kmp.sample.screens.PrivacyScreen
import com.uxcam.kmp.sample.screens.ProductScreen
import com.uxcam.kmp.sample.screens.ProfileScreen
import com.uxcam.kmp.sample.screens.SensitiveFormScreen
import com.uxcam.kmp.sample.screens.ShopScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme { SampleApp() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SampleApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = Destination.forRoute(backStackEntry?.destination?.route)
    val isTab = TABS.any { it.destination == current }

    // The canonical Navigation Compose integration: one hook tags every destination change.
    // Automatic tagging can only see the single host Activity in a Compose app, so screen names
    // come from the nav graph instead.
    TagScreenOnNavigation(navController)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(current?.title ?: "UXCam KMP") },
                navigationIcon = {
                    if (!isTab) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        SampleLog.add("isRecording() = ${UXCamKMP.isRecording()}")
                    }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Recording state")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                TABS.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab.destination,
                        onClick = { navController.navigateToTab(tab.destination.route) },
                        icon = { Icon(tab.icon, contentDescription = tab.destination.title) },
                        label = { Text(tab.destination.title) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Destination.Home.route) { HomeScreen(navController) }

            composable(Destination.Shop.route) { ShopScreen(navController) }
            composable(Destination.Product.route) { entry ->
                ProductScreen(navController, entry.arguments?.getString("id").orEmpty())
            }
            composable(Destination.Checkout.route) { CheckoutScreen() }

            composable(Destination.Privacy.route) { PrivacyScreen(navController) }
            composable(Destination.SensitiveForm.route) { SensitiveFormScreen() }

            composable(Destination.Profile.route) { ProfileScreen(navController) }
            composable(Destination.EditProfile.route) { EditProfileScreen() }

            composable(Destination.Debug.route) { DebugScreen(navController) }
            composable(Destination.Console.route) { ConsoleScreen() }
        }
    }
}

/**
 * Tags a UXCam screen name on every navigation event. Collecting `currentBackStackEntryFlow`
 * catches tab switches, drill-downs and back presses alike — one hook for the whole graph.
 */
@Composable
private fun TagScreenOnNavigation(navController: NavHostController) {
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            val destination = Destination.forRoute(entry.destination.route) ?: return@collect
            UXCamKMP.tagScreenName(destination.screenName)
            SampleLog.add("tagScreenName(\"${destination.screenName}\")")
        }
    }
}

/** Standard bottom-bar behaviour: single top, state preserved per tab. */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
