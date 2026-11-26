package com.gayatripatel.rides.core

/** Per-brand, per-vehicle pricing, all in minor currency units. Configured from the admin panel. */
data class FareRules(
    val baseMinor: Long,
    val perKmMinor: Long,
    val perMinuteMinor: Long,
    val minimumMinor: Long,
    val bookingFeeMinor: Long,
    val waitingPerMinuteMinor: Long,
    val freeWaitingMinutes: Int,
    val taxPercent: Double,
    val cancellationFeeMinor: Long,
)

enum class FareLineKind { BASE, DISTANCE, TIME, WAITING, SURGE, MINIMUM_ADJUSTMENT, BOOKING_FEE, TOLLS, PROMO, TAX }

data class FareLine(val kind: FareLineKind, val amountMinor: Long, val detail: String = "")

data class FareBreakdown(val lines: List<FareLine>, val totalMinor: Long, val currency: Currency) {
    fun line(kind: FareLineKind): FareLine? = lines.firstOrNull { it.kind == kind }
}

data class Promo(val code: String, val percentOff: Int, val maxOffMinor: Long)

/**
 * Same calculator for the up-front estimate and the end-of-ride fare, so the receipt always
 * explains any difference line by line (extra distance, waiting time, tolls).
 */
object FareCalculator {

    fun calculate(
        rules: FareRules,
        currency: Currency,
        distanceKm: Double,
        minutes: Double,
        surge: Double = 1.0,
        waitingMinutes: Double = 0.0,
        tollsMinor: Long = 0,
        promo: Promo? = null,
    ): FareBreakdown {
        require(surge >= 1.0) { "surge can't discount a fare" }
        val lines = mutableListOf<FareLine>()
        val distance = (rules.perKmMinor * distanceKm).toLong()
        val time = (rules.perMinuteMinor * minutes).toLong()
        lines += FareLine(FareLineKind.BASE, rules.baseMinor)
        lines += FareLine(FareLineKind.DISTANCE, distance, "%.1f km".format(java.util.Locale.US, distanceKm))
        lines += FareLine(FareLineKind.TIME, time, "${minutes.toInt()} min")

        val billableWait = (waitingMinutes - rules.freeWaitingMinutes).coerceAtLeast(0.0)
        if (billableWait > 0) {
            lines += FareLine(FareLineKind.WAITING, (rules.waitingPerMinuteMinor * billableWait).toLong(), "${billableWait.toInt()} min")
        }

        var ride = lines.sumOf { it.amountMinor }
        if (surge > 1.0) {
            val extra = (ride * (surge - 1.0)).toLong()
            lines += FareLine(FareLineKind.SURGE, extra, "×%.1f".format(java.util.Locale.US, surge))
            ride += extra
        }
        if (ride < rules.minimumMinor) {
            lines += FareLine(FareLineKind.MINIMUM_ADJUSTMENT, rules.minimumMinor - ride)
            ride = rules.minimumMinor
        }
        if (promo != null) {
            val off = minOf(ride * promo.percentOff / 100, promo.maxOffMinor)
            lines += FareLine(FareLineKind.PROMO, -off, promo.code)
            ride -= off
        }
        if (rules.bookingFeeMinor > 0) lines += FareLine(FareLineKind.BOOKING_FEE, rules.bookingFeeMinor)
        if (tollsMinor > 0) lines += FareLine(FareLineKind.TOLLS, tollsMinor)

        val beforeTax = ride + rules.bookingFeeMinor + tollsMinor
        val tax = (beforeTax * rules.taxPercent / 100.0).toLong()
        if (tax > 0) lines += FareLine(FareLineKind.TAX, tax, "${rules.taxPercent}%")

        return FareBreakdown(lines, currency.round((beforeTax + tax).toDouble()), currency)
    }

    /** Final fare from what actually happened on the trip: the GPS trace, not the estimate's route. */
    fun finalFare(
        rules: FareRules,
        currency: Currency,
        trace: List<LatLng>,
        tripMinutes: Double,
        waitingMinutes: Double,
        tollsMinor: Long,
        surge: Double,
        promo: Promo?,
    ): FareBreakdown = calculate(
        rules, currency, Geo.traceLengthMeters(trace) / 1000.0, tripMinutes, surge, waitingMinutes, tollsMinor, promo,
    )
}
