package com.gayatripatel.rides.core

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLng(val lat: Double, val lng: Double)

object Geo {
    private const val EARTH_RADIUS_M = 6_371_000.0

    /** Great-circle distance in metres. */
    fun distanceMeters(a: LatLng, b: LatLng): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * EARTH_RADIUS_M * atan2(sqrt(h), sqrt(1 - h))
    }

    /** Length of a GPS trace, ignoring jumps that are physically impossible between two fixes (GPS spikes). */
    fun traceLengthMeters(points: List<LatLng>, maxJumpMeters: Double = 500.0): Double =
        points.zipWithNext().sumOf { (a, b) -> distanceMeters(a, b).takeIf { it <= maxJumpMeters } ?: 0.0 }
}

/** A brand's operating area. Bookings with a pickup or drop-off outside it are refused before reaching the backend. */
data class ServiceArea(val name: String, val polygon: List<LatLng>) {

    /** Ray casting; fine for city-sized polygons that don't cross the antimeridian. */
    operator fun contains(p: LatLng): Boolean {
        var inside = false
        var j = polygon.lastIndex
        for (i in polygon.indices) {
            val a = polygon[i]
            val b = polygon[j]
            if ((a.lat > p.lat) != (b.lat > p.lat) &&
                p.lng < (b.lng - a.lng) * (p.lat - a.lat) / (b.lat - a.lat) + a.lng
            ) {
                inside = !inside
            }
            j = i
        }
        return inside
    }

    val bounds: Pair<LatLng, LatLng>
        get() = LatLng(polygon.minOf { it.lat }, polygon.minOf { it.lng }) to LatLng(polygon.maxOf { it.lat }, polygon.maxOf { it.lng })
}
