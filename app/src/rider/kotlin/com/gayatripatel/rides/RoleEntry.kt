package com.gayatripatel.rides

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gayatripatel.rides.core.BrandConfig
import com.gayatripatel.rides.core.CancelledBy
import com.gayatripatel.rides.core.FareCalculator
import com.gayatripatel.rides.core.GatewayId
import com.gayatripatel.rides.core.RideEvent
import com.gayatripatel.rides.core.RideState
import com.gayatripatel.rides.core.RideStateMachine
import com.gayatripatel.rides.ui.ReceiptScreen
import com.gayatripatel.rides.ui.ReceiptUiState
import com.gayatripatel.rides.ui.RiderHomeScreen
import com.gayatripatel.rides.ui.RiderTripScreen
import com.gayatripatel.rides.ui.Scenarios
import com.gayatripatel.rides.ui.demoRoute
import com.gayatripatel.rides.ui.riderHomeState
import com.gayatripatel.rides.ui.riderTripState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RiderFlow(
    val vehicleId: String,
    val someoneElse: Boolean = false,
    val otherName: String = "",
    val otherPhone: String = "",
    val payment: GatewayId? = null,
    val dropoffOutside: Boolean = false,
    val ride: RideState? = null,
    val receipt: ReceiptUiState? = null,
    val rating: Int = 0,
)

/** Rider app flow: book → driver on the way → trip → receipt. Driver and backend events are simulated. */
class RiderViewModel(private val brand: BrandConfig) : ViewModel() {
    private val _flow = MutableStateFlow(RiderFlow(brand.vehicles.first().id))
    val flow: StateFlow<RiderFlow> = _flow.asStateFlow()
    private var trip: Job? = null

    fun update(transform: (RiderFlow) -> RiderFlow) = _flow.update(transform)

    fun book() {
        val vehicle = brand.vehicles.first { it.id == _flow.value.vehicleId }
        val machine = RideStateMachine(vehicle.rules)
        trip = viewModelScope.launch {
            var ride: RideState = RideState.Searching("ride-${System.currentTimeMillis()}", System.currentTimeMillis())
            fun step(e: RideEvent) {
                ride = machine.reduce(ride, e)
                _flow.update { it.copy(ride = ride) }
            }
            step(RideEvent.Assigned("driver-1", System.currentTimeMillis()))
            delay(6_000)
            step(RideEvent.Arrived(System.currentTimeMillis()))
            delay(3_000)
            step(RideEvent.Started(System.currentTimeMillis()))
            delay(4_000)
            step(RideEvent.Ended(System.currentTimeMillis()))

            // End-of-ride fare from the actual GPS trace and waiting time, not the up-front estimate.
            val s = Scenarios.forBrand(brand)
            val fare = FareCalculator.finalFare(
                vehicle.rules, brand.currency, demoRoute(s.pickup.position, s.dropoff.position),
                tripMinutes = 21.0, waitingMinutes = 5.0, tollsMinor = 0, surge = 1.0, promo = null,
            )
            val payment = _flow.value.payment ?: Scenarios.registry.available(brand.payments, brand.currency, fare.totalMinor).first()
            _flow.update { it.copy(receipt = ReceiptUiState(brand, s.driverName, fare, payment, 0)) }
        }
    }

    fun cancel() {
        trip?.cancel()
        val ride = _flow.value.ride ?: return
        val vehicle = brand.vehicles.first { it.id == _flow.value.vehicleId }
        val cancelled = RideStateMachine(vehicle.rules).reduce(ride, RideEvent.Cancel(CancelledBy.RIDER, System.currentTimeMillis()))
        _flow.update { it.copy(ride = if (cancelled is RideState.Cancelled) null else ride) }
    }

    fun reset() = _flow.update { RiderFlow(brand.vehicles.first().id) }
}

@Composable
fun RoleEntry(brand: BrandConfig, activity: ComponentActivity) {
    val vm: RiderViewModel = viewModel(viewModelStoreOwner = activity) { RiderViewModel(brand) }
    val f by vm.flow.collectAsStateWithLifecycle()
    val s = Scenarios.forBrand(brand)

    val receipt = f.receipt
    when {
        receipt != null -> ReceiptScreen(receipt.copy(rating = f.rating), { r -> vm.update { it.copy(rating = r) } }, vm::reset)
        f.ride != null -> RiderTripScreen(
            riderTripState(brand, bookedFor = f.otherName.takeIf { f.someoneElse && it.isNotBlank() }),
            onCall = {}, onMessage = {}, onCancel = vm::cancel,
        )
        else -> RiderHomeScreen(
            state = riderHomeState(
                brand,
                dropoff = if (f.dropoffOutside) s.outside else s.dropoff,
                selectedVehicleId = f.vehicleId,
                bookingForSomeoneElse = f.someoneElse,
                otherRiderName = f.otherName,
                otherRiderPhone = f.otherPhone,
                selectedPayment = f.payment,
            ),
            onSelectVehicle = { id -> vm.update { it.copy(vehicleId = id, payment = null) } },
            onBookingForSomeoneElse = { v -> vm.update { it.copy(someoneElse = v) } },
            onOtherName = { v -> vm.update { it.copy(otherName = v) } },
            onOtherPhone = { v -> vm.update { it.copy(otherPhone = v) } },
            onSelectPayment = { g -> vm.update { it.copy(payment = g) } },
            // Demo: tapping the drop-off toggles a destination outside the service area.
            onEditDropoff = { vm.update { it.copy(dropoffOutside = !it.dropoffOutside) } },
            onBook = vm::book,
        )
    }
}
