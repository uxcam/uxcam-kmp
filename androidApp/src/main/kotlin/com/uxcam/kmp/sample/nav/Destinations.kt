package com.uxcam.kmp.sample.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Every destination in the sample, with the UXCam screen name it reports. Keeping the name next
 * to the route means the tagging hook in `MainActivity` never has to guess, and per-screen
 * occlusion rules can reference the same constants the tagging uses.
 */
enum class Destination(
    val route: String,
    val title: String,
    val screenName: String,
) {
    Home("home", "Home", "Home"),

    Shop("shop", "Shop", "Catalog"),
    Product("shop/product/{id}", "Product", "Product Detail"),
    Checkout("shop/checkout", "Checkout", "Checkout"),

    Privacy("privacy", "Privacy", "Privacy"),
    SensitiveForm("privacy/form", "Sensitive form", "Sensitive Form"),

    Profile("profile", "Profile", "Profile"),
    EditProfile("profile/edit", "Edit profile", "Edit Profile"),

    Debug("debug", "Debug", "Debug"),
    Console("debug/console", "Console", "Console");

    companion object {
        fun forRoute(route: String?): Destination? = entries.find { it.route == route }
    }
}

/** Bottom bar tabs, in display order. */
val TABS: List<Tab> = listOf(
    Tab(Destination.Home, Icons.Filled.Home),
    Tab(Destination.Shop, Icons.Filled.ShoppingCart),
    Tab(Destination.Privacy, Icons.Filled.Lock),
    Tab(Destination.Profile, Icons.Filled.Person),
    Tab(Destination.Debug, Icons.Filled.Build),
)

data class Tab(val destination: Destination, val icon: ImageVector)

fun productRoute(id: Int): String = "shop/product/$id"
