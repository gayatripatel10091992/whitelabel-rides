package com.gayatripatel.rides.core

import kotlin.math.roundToLong

data class Currency(
    val code: String,
    val symbol: String,
    val decimals: Int,
    /** Smallest amount riders actually pay, in minor units, e.g. KES fares round to 10 shillings. */
    val roundingStepMinor: Long = 1,
    val symbolAfter: Boolean = false,
) {
    val minorPerMajor: Long get() = pow10(decimals)

    fun format(minor: Long): String {
        val major = minor / minorPerMajor
        val rest = kotlin.math.abs(minor % minorPerMajor)
        val number = if (decimals == 0) groupThousands(major) else "${groupThousands(major)}.${rest.toString().padStart(decimals, '0')}"
        return if (symbolAfter) "$number $symbol" else "$symbol$number"
    }

    fun round(minor: Double): Long {
        val step = roundingStepMinor.coerceAtLeast(1)
        return (minor / step).roundToLong() * step
    }

    private fun groupThousands(n: Long): String = n.toString().reversed().chunked(3).joinToString(",").reversed()

    companion object {
        val USD = Currency("USD", "$", 2)
        val KES = Currency("KES", "KSh ", 0, roundingStepMinor = 10)
        val AED = Currency("AED", "AED", 2, roundingStepMinor = 50, symbolAfter = true)

        private fun pow10(n: Int): Long = (1..n).fold(1L) { acc, _ -> acc * 10 }
    }
}
