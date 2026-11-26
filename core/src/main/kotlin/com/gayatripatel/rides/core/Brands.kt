package com.gayatripatel.rides.core

data class VehicleType(
    val id: String,
    val name: String,
    val seats: Int,
    val etaMinutes: Int,
    val rules: FareRules,
)

/**
 * Everything that differs between white-label clients. One `BrandConfig` per Gradle `brand` flavor;
 * the shared codebase never checks a brand id directly, it only reads these values.
 */
data class BrandConfig(
    val id: String,
    val name: String,
    val tagline: String,
    val primaryArgb: Long,
    val accentArgb: Long,
    val currency: Currency,
    /** BCP-47 language the app starts in; `ar` switches the layout to right-to-left. */
    val language: String,
    val serviceArea: ServiceArea,
    val vehicles: List<VehicleType>,
    val payments: AdminPaymentConfig,
    val supportPhone: String,
    val parcelDelivery: Boolean,
) {
    val isRtl: Boolean get() = language in setOf("ar", "he", "fa", "ur")
}

object Brands {
    val SWIFTRIDE = BrandConfig(
        id = "swiftride",
        name = "SwiftRide",
        tagline = "Rides in minutes across the Bay",
        primaryArgb = 0xFF111827,
        accentArgb = 0xFF22C55E,
        currency = Currency.USD,
        language = "en",
        serviceArea = ServiceArea(
            "San Francisco",
            listOf(LatLng(37.810, -122.515), LatLng(37.810, -122.355), LatLng(37.705, -122.355), LatLng(37.705, -122.515)),
        ),
        vehicles = listOf(
            VehicleType("eco", "Swift", 4, 3, FareRules(250, 115, 30, 750, 249, 30, 3, 8.5, 500)),
            VehicleType("comfort", "Comfort", 4, 6, FareRules(400, 160, 40, 1_100, 249, 40, 3, 8.5, 500)),
            VehicleType("xl", "Swift XL", 6, 9, FareRules(500, 210, 50, 1_400, 249, 50, 3, 8.5, 700)),
        ),
        payments = AdminPaymentConfig(listOf(GatewayId.STRIPE), cashEnabled = false),
        supportPhone = "+14155550100",
        parcelDelivery = false,
    )

    val CITYHOP = BrandConfig(
        id = "cityhop",
        name = "CityHop",
        tagline = "Boda, car or parcel. Pay with M-Pesa.",
        primaryArgb = 0xFF0F766E,
        accentArgb = 0xFFF59E0B,
        currency = Currency.KES,
        language = "en",
        serviceArea = ServiceArea(
            "Nairobi",
            listOf(LatLng(-1.200, 36.700), LatLng(-1.200, 36.950), LatLng(-1.360, 36.950), LatLng(-1.360, 36.700)),
        ),
        vehicles = listOf(
            VehicleType("boda", "Boda", 1, 2, FareRules(80, 35, 3, 150, 0, 3, 3, 0.0, 50)),
            VehicleType("car", "Hop Car", 4, 5, FareRules(150, 55, 4, 250, 20, 4, 3, 0.0, 100)),
            VehicleType("parcel", "Parcel", 0, 7, FareRules(120, 40, 2, 200, 0, 2, 5, 0.0, 80)),
        ),
        payments = AdminPaymentConfig(listOf(GatewayId.FLUTTERWAVE_MPESA), cashEnabled = true, maxCashMinor = 5_000),
        supportPhone = "+254205550100",
        parcelDelivery = true,
    )

    val RIHLA = BrandConfig(
        id = "rihla",
        name = "رحلة Rihla",
        tagline = "Rides and airport transfers in Dubai",
        primaryArgb = 0xFF7C2D12,
        accentArgb = 0xFFD4A017,
        currency = Currency.AED,
        language = "ar",
        serviceArea = ServiceArea(
            "Dubai",
            listOf(LatLng(25.290, 55.130), LatLng(25.290, 55.420), LatLng(25.050, 55.420), LatLng(25.050, 55.130)),
        ),
        vehicles = listOf(
            VehicleType("economy", "Economy", 4, 4, FareRules(1_200, 197, 50, 1_200, 0, 50, 5, 5.0, 1_000)),
            VehicleType("business", "Business", 4, 8, FareRules(2_500, 300, 80, 3_000, 0, 80, 5, 5.0, 2_000)),
        ),
        payments = AdminPaymentConfig(listOf(GatewayId.STRIPE), cashEnabled = true),
        supportPhone = "+97145550100",
        parcelDelivery = false,
    )

    val all = listOf(SWIFTRIDE, CITYHOP, RIHLA)

    fun byId(id: String): BrandConfig = all.firstOrNull { it.id == id } ?: error("Unknown brand '$id'")
}
