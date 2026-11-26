package com.gayatripatel.rides.core

enum class CancelledBy { RIDER, DRIVER, SYSTEM }

sealed interface RideState {
    val rideId: String

    data class Searching(override val rideId: String, val requestedAtMillis: Long) : RideState
    data class DriverAssigned(override val rideId: String, val driverId: String, val assignedAtMillis: Long) : RideState
    data class DriverArrived(override val rideId: String, val driverId: String, val arrivedAtMillis: Long) : RideState
    data class InProgress(override val rideId: String, val driverId: String, val startedAtMillis: Long, val waitedMinutes: Double) : RideState
    data class Completed(override val rideId: String, val endedAtMillis: Long) : RideState
    data class Cancelled(override val rideId: String, val by: CancelledBy, val feeMinor: Long) : RideState
}

sealed interface RideEvent {
    data class Assigned(val driverId: String, val atMillis: Long) : RideEvent
    data class Arrived(val atMillis: Long) : RideEvent
    data class Started(val atMillis: Long) : RideEvent
    data class Ended(val atMillis: Long) : RideEvent
    data class Cancel(val by: CancelledBy, val atMillis: Long) : RideEvent
    data object NoDriverFound : RideEvent
}

/**
 * Ride lifecycle shared by the rider, driver and kiosk apps.
 * Cancellation is free for the rider within the grace period after assignment; after the driver arrives
 * (or the grace period ends) the brand's cancellation fee applies.
 */
class RideStateMachine(private val rules: FareRules, private val graceMillis: Long = 2 * 60_000) {

    fun reduce(state: RideState, event: RideEvent): RideState = when (state) {
        is RideState.Searching -> when (event) {
            is RideEvent.Assigned -> RideState.DriverAssigned(state.rideId, event.driverId, event.atMillis)
            is RideEvent.Cancel -> RideState.Cancelled(state.rideId, event.by, 0)
            RideEvent.NoDriverFound -> RideState.Cancelled(state.rideId, CancelledBy.SYSTEM, 0)
            else -> state
        }
        is RideState.DriverAssigned -> when (event) {
            is RideEvent.Arrived -> RideState.DriverArrived(state.rideId, state.driverId, event.atMillis)
            is RideEvent.Cancel -> RideState.Cancelled(
                state.rideId, event.by,
                if (event.by == CancelledBy.RIDER && event.atMillis - state.assignedAtMillis > graceMillis) rules.cancellationFeeMinor else 0,
            )
            else -> state
        }
        is RideState.DriverArrived -> when (event) {
            is RideEvent.Started -> RideState.InProgress(
                state.rideId, state.driverId, event.atMillis, (event.atMillis - state.arrivedAtMillis) / 60_000.0,
            )
            is RideEvent.Cancel -> RideState.Cancelled(
                state.rideId, event.by, if (event.by == CancelledBy.RIDER) rules.cancellationFeeMinor else 0,
            )
            else -> state
        }
        is RideState.InProgress -> when (event) {
            is RideEvent.Ended -> RideState.Completed(state.rideId, event.atMillis)
            else -> state
        }
        is RideState.Completed, is RideState.Cancelled -> state
    }
}

/**
 * Driver-side guard against "ghost trips": stale or duplicate trip offers that arrive late over push or
 * real-time messaging after the ride was already taken, cancelled or completed.
 */
class TripOfferGuard(private val offerTtlMillis: Long = 30_000) {
    private val seen = LinkedHashSet<String>()

    fun shouldShow(rideId: String, sentAtMillis: Long, nowMillis: Long, driverBusy: Boolean): Boolean {
        if (driverBusy) return false
        if (nowMillis - sentAtMillis > offerTtlMillis) return false
        if (!seen.add(rideId)) return false
        if (seen.size > 200) seen.remove(seen.first())
        return true
    }
}
