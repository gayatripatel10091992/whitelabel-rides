package com.gayatripatel.rides.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoTest {
    private val sf = Brands.SWIFTRIDE.serviceArea

    @Test fun `haversine distance is about right`() {
        // Ferry Building to Golden Gate Park entrance ≈ 6.2 km
        val d = Geo.distanceMeters(LatLng(37.7955, -122.3937), LatLng(37.7694, -122.4580))
        assertTrue(d in 6_000.0..6_400.0)
    }

    @Test fun `service area polygon`() {
        assertTrue(LatLng(37.7749, -122.4194) in sf)
        assertFalse(LatLng(37.8044, -122.2712) in sf) // Oakland, across the bay
        assertTrue(LatLng(-1.286, 36.817) in Brands.CITYHOP.serviceArea)
    }

    @Test fun `trace length skips GPS spikes`() {
        val trace = listOf(LatLng(37.7700, -122.4200), LatLng(37.7710, -122.4200), LatLng(37.9000, -122.4200), LatLng(37.7720, -122.4200))
        val len = Geo.traceLengthMeters(trace)
        assertTrue("was $len", len in 100.0..130.0)
    }
}

class MoneyAndFareTest {
    private val usd = Currency.USD
    private val eco = Brands.SWIFTRIDE.vehicles.first().rules

    @Test fun `formats per currency`() {
        assertEquals("$1,234.50", usd.format(123_450))
        assertEquals("KSh 1,250", Currency.KES.format(1_250))
        assertEquals("32.50 AED", Currency.AED.format(3_250))
    }

    @Test fun `estimate adds up line by line`() {
        val fare = FareCalculator.calculate(eco, usd, distanceKm = 6.2, minutes = 18.0)
        // base 2.50 + 6.2 km * 1.15 = 7.13 + 18 min * 0.30 = 5.40 → 15.03, booking 2.49 → 17.52, tax 8.5% = 1.48
        assertEquals(713, fare.line(FareLineKind.DISTANCE)!!.amountMinor)
        assertEquals(148, fare.line(FareLineKind.TAX)!!.amountMinor)
        assertEquals(1_900, fare.totalMinor)
        assertEquals(fare.totalMinor, fare.lines.sumOf { it.amountMinor })
    }

    @Test fun `minimum fare, surge, waiting and promo`() {
        val short = FareCalculator.calculate(eco, usd, 0.5, 2.0)
        assertEquals(750 - (250 + 57 + 60), short.line(FareLineKind.MINIMUM_ADJUSTMENT)!!.amountMinor)

        val surged = FareCalculator.calculate(eco, usd, 6.2, 18.0, surge = 1.5, waitingMinutes = 7.0, promo = Promo("HELLO20", 20, 300))
        assertEquals(120, surged.line(FareLineKind.WAITING)!!.amountMinor) // 4 billable minutes × 0.30
        assertEquals(-300, surged.line(FareLineKind.PROMO)!!.amountMinor) // capped
        assertTrue(surged.line(FareLineKind.SURGE)!!.amountMinor > 0)
    }

    @Test fun `KES rounds to 10 shillings`() {
        val boda = Brands.CITYHOP.vehicles.first().rules
        val fare = FareCalculator.calculate(boda, Currency.KES, 4.3, 12.0)
        assertEquals(0, fare.totalMinor % 10)
        assertNull(fare.line(FareLineKind.TAX))
    }
}

class RideTest {
    private val rules = Brands.SWIFTRIDE.vehicles.first().rules
    private val sm = RideStateMachine(rules)

    @Test fun `free cancellation inside the grace period, fee after`() {
        val assigned = sm.reduce(RideState.Searching("r1", 0), RideEvent.Assigned("d1", 1_000))
        assertEquals(0, (sm.reduce(assigned, RideEvent.Cancel(CancelledBy.RIDER, 60_000)) as RideState.Cancelled).feeMinor)
        assertEquals(500, (sm.reduce(assigned, RideEvent.Cancel(CancelledBy.RIDER, 200_000)) as RideState.Cancelled).feeMinor)
        assertEquals(0, (sm.reduce(assigned, RideEvent.Cancel(CancelledBy.DRIVER, 200_000)) as RideState.Cancelled).feeMinor)
    }

    @Test fun `waiting time is measured from arrival to start`() {
        var s: RideState = RideState.Searching("r1", 0)
        s = sm.reduce(s, RideEvent.Assigned("d1", 0))
        s = sm.reduce(s, RideEvent.Arrived(60_000))
        s = sm.reduce(s, RideEvent.Started(60_000 + 5 * 60_000))
        assertEquals(5.0, (s as RideState.InProgress).waitedMinutes, 0.001)
        assertEquals(s, sm.reduce(s, RideEvent.Cancel(CancelledBy.RIDER, 0))) // can't cancel mid-trip
        assertTrue(sm.reduce(s, RideEvent.Ended(9_999_999)) is RideState.Completed)
    }

    @Test fun `ghost trip offers are dropped`() {
        val guard = TripOfferGuard(offerTtlMillis = 30_000)
        assertTrue(guard.shouldShow("r1", sentAtMillis = 0, nowMillis = 5_000, driverBusy = false))
        assertFalse(guard.shouldShow("r1", 0, 6_000, false)) // duplicate delivery
        assertFalse(guard.shouldShow("r2", 0, 45_000, false)) // stale
        assertFalse(guard.shouldShow("r3", 40_000, 41_000, driverBusy = true))
    }
}

class LocationAndPaymentsTest {
    private val here = LatLng(37.7749, -122.4194)
    private val near = LatLng(37.77492, -122.4194) // ~2 m
    private val far = LatLng(37.7760, -122.4194) // ~120 m

    @Test fun `idle drivers send far fewer updates`() {
        assertNull(LocationThrottle.requestFor(DriverMode.OFFLINE))
        assertFalse(LocationThrottle.shouldSend(DriverMode.IDLE, here, 0, far, 10_000)) // too soon
        assertTrue(LocationThrottle.shouldSend(DriverMode.IDLE, here, 0, far, 16_000))
        assertFalse(LocationThrottle.shouldSend(DriverMode.IDLE, here, 0, near, 30_000)) // parked
        assertTrue(LocationThrottle.shouldSend(DriverMode.IDLE, here, 0, near, 61_000)) // heartbeat
        assertTrue(LocationThrottle.shouldSend(DriverMode.ON_TRIP, here, 0, far, 2_000))
    }

    @Test fun `gateways follow admin config, currency and cash cap`() {
        val registry = GatewayRegistry(DemoGateways.all)
        val kes = Brands.CITYHOP
        assertEquals(listOf(GatewayId.FLUTTERWAVE_MPESA, GatewayId.CASH), registry.available(kes.payments, kes.currency, 900))
        assertEquals(listOf(GatewayId.FLUTTERWAVE_MPESA), registry.available(kes.payments, kes.currency, 9_000))
        // Admin enables Stripe for a KES brand: Stripe can't take KES, so it isn't offered.
        assertEquals(
            listOf(GatewayId.FLUTTERWAVE_MPESA, GatewayId.CASH),
            registry.available(kes.payments.copy(enabled = listOf(GatewayId.STRIPE, GatewayId.FLUTTERWAVE_MPESA)), kes.currency, 900),
        )
        assertTrue(registry.charge(GatewayId.FLUTTERWAVE_MPESA, 900, kes.currency, "u1") is PaymentResult.RequiresAction)
        assertTrue(registry.charge(GatewayId.RAZORPAY, 900, kes.currency, "u1") is PaymentResult.Failed)
    }

    @Test fun `brands are complete`() {
        Brands.all.forEach { b ->
            assertTrue(b.vehicles.isNotEmpty())
            assertTrue(b.serviceArea.polygon.size >= 3)
        }
        assertTrue(Brands.RIHLA.isRtl)
        assertEquals(Brands.CITYHOP, Brands.byId("cityhop"))
    }
}
