package com.gayatripatel.rides.ui

import com.gayatripatel.rides.core.BrandConfig
import com.gayatripatel.rides.core.Brands
import com.gayatripatel.rides.core.DemoGateways
import com.gayatripatel.rides.core.FareBreakdown
import com.gayatripatel.rides.core.FareCalculator
import com.gayatripatel.rides.core.GatewayId
import com.gayatripatel.rides.core.GatewayRegistry
import com.gayatripatel.rides.core.Geo
import com.gayatripatel.rides.core.LatLng
import com.gayatripatel.rides.core.Promo
import com.gayatripatel.rides.core.VehicleType

data class Place(val label: String, val position: LatLng)

/** Demo places per brand city. Real builds get these from Places autocomplete. */
data class CityScenario(val pickup: Place, val dropoff: Place, val outside: Place, val driverName: String, val car: String, val plate: String)

object Scenarios {
    fun forBrand(brand: BrandConfig): CityScenario = when (brand.id) {
        Brands.CITYHOP.id -> CityScenario(
            Place("Westlands, Sarit Centre", LatLng(-1.2610, 36.8030)),
            Place("Upper Hill, Hospital Rd", LatLng(-1.2989, 36.8160)),
            Place("Thika Town", LatLng(-1.0333, 37.0693)),
            "Brian O.", "TVS HLX 125", "KMGA 214B",
        )
        Brands.RIHLA.id -> CityScenario(
            Place("دبي مول", LatLng(25.1972, 55.2796)),
            Place("مطار دبي الدولي · مبنى ٣", LatLng(25.2490, 55.3530)),
            Place("أبوظبي", LatLng(24.4539, 54.3773)),
            "يوسف", "Toyota Camry", "Dubai L 48213",
        )
        else -> CityScenario(
            Place("Ferry Building", LatLng(37.7955, -122.3937)),
            Place("Golden Gate Park, JFK Dr", LatLng(37.7694, -122.4580)),
            Place("Oakland Airport", LatLng(37.7126, -122.2197)),
            "Marcus R.", "Toyota Prius", "8XYZ 512",
        )
    }

    /** Road distance is longer than the straight line; 1.3 is a typical urban detour factor. */
    fun estimateKm(a: LatLng, b: LatLng): Double = Geo.distanceMeters(a, b) / 1000.0 * 1.3

    fun estimateMinutes(km: Double): Double = km / 24.0 * 60.0

    fun fare(brand: BrandConfig, vehicle: VehicleType, from: LatLng, to: LatLng, promo: Promo? = null): FareBreakdown {
        val km = estimateKm(from, to)
        return FareCalculator.calculate(vehicle.rules, brand.currency, km, estimateMinutes(km), promo = promo)
    }

    val registry = GatewayRegistry(DemoGateways.all)
}

data class VehicleOptionUi(val id: String, val name: String, val seats: Int, val etaMinutes: Int, val price: String, val selected: Boolean)

data class RiderHomeUiState(
    val brand: BrandConfig,
    val pickup: Place,
    val dropoff: Place,
    val vehicles: List<VehicleOptionUi>,
    /** Estimated road distance label and minutes, or null when the trip can't be booked. */
    val tripKm: String?,
    val tripMinutes: Int,
    val outsideArea: Boolean,
    val bookingForSomeoneElse: Boolean,
    val otherRiderName: String,
    val otherRiderPhone: String,
    val payments: List<GatewayId>,
    val selectedPayment: GatewayId?,
    val map: MapModel,
)

fun riderHomeState(
    brand: BrandConfig,
    dropoff: Place = Scenarios.forBrand(brand).dropoff,
    selectedVehicleId: String = brand.vehicles.first().id,
    bookingForSomeoneElse: Boolean = false,
    otherRiderName: String = "",
    otherRiderPhone: String = "",
    selectedPayment: GatewayId? = null,
): RiderHomeUiState {
    val s = Scenarios.forBrand(brand)
    val outside = dropoff.position !in brand.serviceArea || s.pickup.position !in brand.serviceArea
    val fares = brand.vehicles.associate { it.id to Scenarios.fare(brand, it, s.pickup.position, dropoff.position) }
    val selectedFare = fares.getValue(selectedVehicleId)
    val km = Scenarios.estimateKm(s.pickup.position, dropoff.position)
    val payments = Scenarios.registry.available(brand.payments, brand.currency, selectedFare.totalMinor)
    val mid = LatLng((s.pickup.position.lat + dropoff.position.lat) / 2, (s.pickup.position.lng + dropoff.position.lng) / 2)
    val dLat = kotlin.math.abs(s.pickup.position.lat - dropoff.position.lat)
    val dLng = kotlin.math.abs(s.pickup.position.lng - dropoff.position.lng)
    // The bottom sheet covers the lower half, so fit the trip into the top half of a portrait viewport.
    val span = maxOf(dLat * 2.6, dLng * 3.1).coerceIn(0.03, 1.5)
    return RiderHomeUiState(
        brand = brand,
        pickup = s.pickup,
        dropoff = dropoff,
        vehicles = brand.vehicles.map {
            VehicleOptionUi(
                it.id, it.name, it.seats, it.etaMinutes,
                if (outside) "—" else brand.currency.format(fares.getValue(it.id).totalMinor), it.id == selectedVehicleId,
            )
        },
        tripKm = if (outside) null else "%.1f km".format(java.util.Locale.US, km),
        tripMinutes = Scenarios.estimateMinutes(km).toInt(),
        outsideArea = outside,
        bookingForSomeoneElse = bookingForSomeoneElse,
        otherRiderName = otherRiderName,
        otherRiderPhone = otherRiderPhone,
        payments = payments,
        selectedPayment = selectedPayment ?: payments.firstOrNull(),
        map = MapModel(
            area = brand.serviceArea,
            pickup = s.pickup.position,
            dropoff = dropoff.position,
            route = if (outside) emptyList() else demoRoute(s.pickup.position, dropoff.position),
            cars = listOf(
                LatLng(s.pickup.position.lat + span * 0.08, s.pickup.position.lng - span * 0.05),
                LatLng(s.pickup.position.lat - span * 0.06, s.pickup.position.lng + span * 0.09),
                LatLng(mid.lat + span * 0.1, mid.lng - span * 0.12),
            ),
            center = LatLng(mid.lat - span * 0.27, mid.lng),
            spanLat = span,
            showArea = outside,
        ),
    )
}
