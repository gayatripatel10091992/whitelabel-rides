package com.gayatripatel.rides

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.gayatripatel.rides.core.BrandConfig
import com.gayatripatel.rides.ui.KioskScreen
import com.gayatripatel.rides.ui.KioskTicket
import com.gayatripatel.rides.ui.kioskState
import kotlinx.coroutines.delay

/**
 * Kiosk app. On a device-owner provisioned tablet `startLockTask()` pins the app silently
 * (android:lockTaskMode="if_whitelisted" in src/kiosk/AndroidManifest.xml); elsewhere Android asks the user
 * to pin the screen. The ticket resets after 20 seconds for the next customer.
 */
@Composable
fun RoleEntry(brand: BrandConfig, activity: ComponentActivity) {
    LaunchedEffect(Unit) {
        runCatching { activity.startLockTask() }.onFailure { Log.w("Kiosk", "Lock task unavailable", it) }
    }
    var counter by rememberSaveable { mutableStateOf(16) }
    var ticket by rememberSaveable { mutableStateOf<String?>(null) }
    var destination by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(ticket) {
        if (ticket != null) {
            delay(20_000)
            ticket = null
        }
    }
    KioskScreen(
        state = kioskState(brand, ticket?.let { KioskTicket(it, bay = 1 + counter % 3, minutes = 4, destination = destination) }),
        onPick = { d ->
            counter++
            destination = d.name
            ticket = "A-$counter"
        },
        onOther = {},
    )
}
