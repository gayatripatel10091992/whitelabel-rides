package com.gayatripatel.rides

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gayatripatel.rides.core.BrandConfig
import com.gayatripatel.rides.core.DriverMode
import com.gayatripatel.rides.core.LatLng
import com.gayatripatel.rides.core.LocationThrottle
import com.gayatripatel.rides.core.TripOfferGuard
import com.gayatripatel.rides.ui.DeliveryScreen
import com.gayatripatel.rides.ui.DriverHomeScreen
import com.gayatripatel.rides.ui.deliveryState
import com.gayatripatel.rides.ui.driverHomeState
import com.gayatripatel.rides.ui.rememberSignaturePadState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DriverFlow(
    val online: Boolean = false,
    val offerSecondsLeft: Int? = null,
    val delivering: Boolean = false,
    val trips: Int = 7,
    val gpsFixes: Int = 0,
    val gpsSent: Int = 0,
)

/**
 * Driver app flow with simulated GPS. Every fix goes through [LocationThrottle], and trip offers through
 * [TripOfferGuard], so the counters on screen show how few updates actually leave the phone while idle.
 */
class DriverViewModel(private val brand: BrandConfig) : ViewModel() {
    private val _flow = MutableStateFlow(DriverFlow())
    val flow: StateFlow<DriverFlow> = _flow.asStateFlow()
    private val guard = TripOfferGuard()
    private var gps: Job? = null
    private var offers: Job? = null

    fun setOnline(online: Boolean) {
        _flow.update { it.copy(online = online, offerSecondsLeft = null) }
        gps?.cancel()
        offers?.cancel()
        if (!online) return
        gps = viewModelScope.launch { simulateGps() }
        offers = viewModelScope.launch {
            delay(3_000)
            val now = System.currentTimeMillis()
            if (guard.shouldShow("offer-$now", sentAtMillis = now, nowMillis = now, driverBusy = _flow.value.delivering)) {
                for (s in 15 downTo 1) {
                    _flow.update { it.copy(offerSecondsLeft = s) }
                    delay(1_000)
                }
                _flow.update { it.copy(offerSecondsLeft = null) }
            }
        }
    }

    private suspend fun kotlinx.coroutines.CoroutineScope.simulateGps() {
        var position = LatLng(37.7749, -122.4194)
        var lastSent: LatLng? = null
        var lastSentAt = 0L
        var tick = 0
        while (isActive) {
            delay(1_000)
            tick++
            // Parked most of the time between trips, with short moves.
            if (tick % 20 in 0..4) position = LatLng(position.lat + 0.0004, position.lng)
            val mode = if (_flow.value.delivering) DriverMode.ON_TRIP else DriverMode.IDLE
            val now = System.currentTimeMillis()
            val send = LocationThrottle.shouldSend(mode, lastSent, lastSentAt, position, now)
            if (send) {
                lastSent = position
                lastSentAt = now
            }
            _flow.update { it.copy(gpsFixes = it.gpsFixes + 1, gpsSent = it.gpsSent + if (send) 1 else 0) }
        }
    }

    fun accept() {
        offers?.cancel()
        _flow.update {
            if (brand.parcelDelivery) it.copy(offerSecondsLeft = null, delivering = true)
            else it.copy(offerSecondsLeft = null, trips = it.trips + 1)
        }
    }

    fun decline() {
        offers?.cancel()
        _flow.update { it.copy(offerSecondsLeft = null) }
    }

    fun delivered() = _flow.update { it.copy(delivering = false, trips = it.trips + 1) }
}

@Composable
fun RoleEntry(brand: BrandConfig, activity: ComponentActivity) {
    val vm: DriverViewModel = viewModel(viewModelStoreOwner = activity) { DriverViewModel(brand) }
    val f by vm.flow.collectAsStateWithLifecycle()

    if (f.delivering) {
        DeliveryScreen(deliveryState(brand), rememberSignaturePadState(), onConfirm = vm::delivered)
        return
    }
    val base = driverHomeState(brand, online = f.online, withRequest = f.offerSecondsLeft != null, delivery = brand.parcelDelivery)
    DriverHomeScreen(
        state = base.copy(
            tripsToday = f.trips,
            gpsFixes = f.gpsFixes,
            gpsSent = f.gpsSent,
            request = base.request?.copy(secondsLeft = f.offerSecondsLeft ?: 0),
        ),
        onOnlineChange = vm::setOnline,
        onAccept = vm::accept,
        onDecline = vm::decline,
    )
}
