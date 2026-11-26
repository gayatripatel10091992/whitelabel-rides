package com.gayatripatel.rides.core

enum class DriverMode { OFFLINE, IDLE, EN_ROUTE, ON_TRIP }

/** How often the driver app asks the fused location provider for fixes, per mode. */
data class LocationRequestSpec(val intervalMillis: Long, val minDistanceMeters: Float, val highAccuracy: Boolean)

/**
 * Driver location policy.
 *
 * Between trips the driver app used to request high-accuracy fixes every second and send every one,
 * which allocated constantly and kept the radio awake. The fix: ask for fewer, cheaper fixes while idle,
 * and only *send* a fix when it moved far enough or enough time passed.
 */
object LocationThrottle {

    fun requestFor(mode: DriverMode): LocationRequestSpec? = when (mode) {
        DriverMode.OFFLINE -> null
        DriverMode.IDLE -> LocationRequestSpec(15_000, 50f, highAccuracy = false)
        DriverMode.EN_ROUTE -> LocationRequestSpec(4_000, 10f, highAccuracy = true)
        DriverMode.ON_TRIP -> LocationRequestSpec(2_000, 5f, highAccuracy = true)
    }

    fun shouldSend(
        mode: DriverMode,
        last: LatLng?,
        lastSentAtMillis: Long,
        current: LatLng,
        nowMillis: Long,
    ): Boolean {
        val spec = requestFor(mode) ?: return false
        if (last == null) return true
        val moved = Geo.distanceMeters(last, current)
        val elapsed = nowMillis - lastSentAtMillis
        // Heartbeat so the backend doesn't mark a parked driver as offline.
        val heartbeat = if (mode == DriverMode.IDLE) 60_000L else spec.intervalMillis * 3
        return (moved >= spec.minDistanceMeters && elapsed >= spec.intervalMillis) || elapsed >= heartbeat
    }
}
