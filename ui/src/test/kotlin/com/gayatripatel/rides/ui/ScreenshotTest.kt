package com.gayatripatel.rides.ui

import androidx.compose.runtime.Composable
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gayatripatel.rides.core.BrandConfig
import com.gayatripatel.rides.core.Brands
import com.gayatripatel.rides.core.FareCalculator
import com.gayatripatel.rides.core.GatewayId
import com.gayatripatel.rides.core.Promo
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The same screens rendered for three white-label brands: colours, currency, payment providers, language and
 * layout direction all come from `BrandConfig`.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class ScreenshotTest {

    private fun shot(name: String, brand: BrandConfig, dark: Boolean = false, content: @Composable () -> Unit) {
        captureRoboImage("build/outputs/roborazzi/$name.png") { BrandTheme(brand, darkTheme = dark, content = content) }
    }

    @Composable
    private fun Rider(state: RiderHomeUiState, dark: Boolean = false) =
        RiderHomeScreen(state, {}, {}, {}, {}, {}, {}, {}, dark = dark)

    @Test fun rider_swiftride() = shot("01_rider_swiftride", Brands.SWIFTRIDE) { Rider(riderHomeState(Brands.SWIFTRIDE)) }

    @Test fun rider_cityhop() = shot("02_rider_cityhop_mpesa", Brands.CITYHOP) { Rider(riderHomeState(Brands.CITYHOP)) }

    @Test fun rider_rihla_rtl() = shot("03_rider_rihla_arabic_rtl", Brands.RIHLA) {
        Rider(riderHomeState(Brands.RIHLA, selectedPayment = GatewayId.CASH))
    }

    @Test fun rider_outside_area() = shot("04_outside_service_area", Brands.SWIFTRIDE) {
        Rider(riderHomeState(Brands.SWIFTRIDE, dropoff = Scenarios.forBrand(Brands.SWIFTRIDE).outside))
    }

    @Test fun rider_for_someone_else() = shot("05_book_for_someone_else", Brands.SWIFTRIDE, dark = true) {
        Rider(
            riderHomeState(Brands.SWIFTRIDE, selectedVehicleId = "comfort", bookingForSomeoneElse = true, otherRiderName = "Grandma Rosa", otherRiderPhone = "(415) 555-0172"),
            dark = true,
        )
    }

    @Test fun rider_trip() = shot("06_driver_on_the_way", Brands.SWIFTRIDE) {
        RiderTripScreen(riderTripState(Brands.SWIFTRIDE, bookedFor = "Grandma Rosa"), {}, {}, {})
    }

    private fun receipt(brand: BrandConfig) = ReceiptUiState(
        brand = brand,
        driverName = Scenarios.forBrand(brand).driverName,
        fare = FareCalculator.calculate(
            brand.vehicles.first().rules, brand.currency, distanceKm = 6.8, minutes = 21.0,
            surge = 1.2, waitingMinutes = 6.0, promo = Promo("WELCOME20", 20, brand.currency.minorPerMajor * 5),
        ),
        payment = brand.payments.enabled.first(),
        rating = 4,
    )

    @Test fun receipt_swiftride() = shot("07_end_of_ride_fare", Brands.SWIFTRIDE) { ReceiptScreen(receipt(Brands.SWIFTRIDE), {}, {}) }

    @Test fun receipt_rihla() = shot("08_end_of_ride_fare_arabic", Brands.RIHLA) { ReceiptScreen(receipt(Brands.RIHLA), {}, {}) }

    @Test fun driver_request() = shot("09_driver_trip_request", Brands.SWIFTRIDE) {
        DriverHomeScreen(driverHomeState(Brands.SWIFTRIDE), {}, {}, {})
    }

    @Test fun driver_delivery_request_dark() = shot("10_driver_delivery_request_dark", Brands.CITYHOP, dark = true) {
        DriverHomeScreen(driverHomeState(Brands.CITYHOP, delivery = true), {}, {}, {}, dark = true)
    }

    @Test fun delivery_signature() = shot("11_proof_of_delivery_signature", Brands.CITYHOP) {
        DeliveryScreen(deliveryState(Brands.CITYHOP), rememberSignaturePadState(), {}, demoSignature = true)
    }

    @Test fun kiosk() = shot("12_kiosk_booked", Brands.SWIFTRIDE) {
        KioskScreen(kioskState(Brands.SWIFTRIDE, KioskTicket("A-17", bay = 2, minutes = 4, destination = "Airport")), {}, {})
    }
}
