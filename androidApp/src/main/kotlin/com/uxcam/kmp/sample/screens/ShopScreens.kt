package com.uxcam.kmp.sample.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.uxcam.kmp.KMPUXCamBlur
import com.uxcam.kmp.UXCamKMP
import com.uxcam.kmp.sample.SampleLog
import com.uxcam.kmp.sample.nav.Destination
import com.uxcam.kmp.sample.nav.productRoute
import com.uxcam.kmp.sample.ui.Action
import com.uxcam.kmp.sample.ui.ScreenScaffold
import com.uxcam.kmp.sample.ui.Section
import com.uxcam.kmp.sample.ui.ToggleRow
import com.uxcam.kmp.uxcamOcclude

private data class Product(val id: Int, val name: String, val price: String)

private val CATALOG = listOf(
    Product(1, "Noise-cancelling headphones", "$249"),
    Product(2, "Mechanical keyboard", "$139"),
    Product(3, "27\" 4K monitor", "$429"),
)

/** Catalog: a realistic list-to-detail flow plus an explicit event playground. */
@Composable
fun ShopScreen(navController: NavHostController) {
    var eventName by remember { mutableStateOf("checkout_started") }

    ScreenScaffold {
        Section("Catalog", "Opening a product navigates and tags a new screen name.") {
            CATALOG.forEach { product ->
                Action("${product.name} — ${product.price}") {
                    UXCamKMP.logEvent(
                        "product_opened",
                        mapOf("product_id" to product.id, "price" to product.price),
                    )
                    SampleLog.add("logEvent(\"product_opened\", id=${product.id})")
                    navController.navigate(productRoute(product.id))
                }
            }
        }

        Section("Event playground", "The three logEvent overloads on the shared API.") {
            OutlinedTextField(
                value = eventName,
                onValueChange = { eventName = it },
                label = { Text("Event name") },
                modifier = Modifier.fillMaxWidth(),
            )
            Action("logEvent(name)") {
                UXCamKMP.logEvent(eventName)
                SampleLog.add("logEvent(\"$eventName\")")
            }
            Action("logEvent(name, properties)") {
                UXCamKMP.logEvent(
                    eventName,
                    mapOf("source" to "sample", "cart_size" to 3, "is_returning" to true),
                )
                SampleLog.add("logEvent(\"$eventName\", 3 properties)")
            }
            Action("logEventWithJson(name, json)") {
                UXCamKMP.logEventWithJson(eventName, """{"source":"sample","cart_size":3}""")
                SampleLog.add("logEventWithJson(\"$eventName\")")
            }
        }
    }
}

@Composable
fun ProductScreen(navController: NavHostController, id: String) {
    val product = CATALOG.find { it.id.toString() == id } ?: CATALOG.first()

    ScreenScaffold {
        Text(product.name, style = MaterialTheme.typography.headlineSmall)
        Text(product.price, style = MaterialTheme.typography.titleLarge)
        Text(
            "This screen reports as “Product Detail” regardless of which product is open, so " +
                "per-screen occlusion rules stay stable across product ids.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Section("Actions") {
            Action("Add to cart") {
                UXCamKMP.logEvent(
                    "add_to_cart",
                    mapOf("product_id" to product.id, "name" to product.name),
                )
                SampleLog.add("logEvent(\"add_to_cart\", id=${product.id})")
            }
            Action("Go to checkout") {
                navController.navigate(Destination.Checkout.route)
            }
        }
    }
}

/** The sensitive screen: field-level occlusion plus a rule scoped to this screen name only. */
@Composable
fun CheckoutScreen() {
    var cardNumber by remember { mutableStateOf("4242 4242 4242 4242") }
    var cvv by remember { mutableStateOf("123") }
    var occludeFields by remember { mutableStateOf(false) }

    ScreenScaffold {
        Section("Payment details", "Both fields below are occluded per-composable.") {
            OutlinedTextField(
                value = cardNumber,
                onValueChange = { cardNumber = it },
                label = { Text("Card number") },
                modifier = Modifier
                    .fillMaxWidth()
                    .uxcamOcclude("checkout-card-number"),
            )
            OutlinedTextField(
                value = cvv,
                onValueChange = { cvv = it },
                label = { Text("CVV") },
                modifier = Modifier
                    .fillMaxWidth()
                    .uxcamOcclude("checkout-cvv"),
            )
            ToggleRow("occludeAllTextFields", occludeFields) {
                occludeFields = it
                UXCamKMP.occludeAllTextFields(it)
                SampleLog.add("occludeAllTextFields($it)")
            }
        }

        Section(
            "Screen-scoped occlusion",
            "Applies only while the “Checkout” screen name is active.",
        ) {
            Action("Blur this screen only") {
                UXCamKMP.applyBlurOcclusion(
                    KMPUXCamBlur(
                        blurRadius = 20,
                        screens = listOf(Destination.Checkout.screenName),
                    ),
                )
                SampleLog.add("applyBlurOcclusion(screens = [Checkout])")
            }
            Action("removeOcclusion()") {
                UXCamKMP.removeOcclusion()
                SampleLog.add("removeOcclusion()")
            }
        }

        Section("Complete") {
            Action("Pay now") {
                UXCamKMP.logEvent("purchase_completed", mapOf("total" to 249, "currency" to "USD"))
                UXCamKMP.setSessionProperty("last_purchase", "headphones")
                SampleLog.add("logEvent(\"purchase_completed\")")
            }
        }
    }
}
