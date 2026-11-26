package com.gayatripatel.rides.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.gayatripatel.rides.core.LatLng
import com.gayatripatel.rides.core.ServiceArea
import kotlin.random.Random

data class MapModel(
    val area: ServiceArea,
    val pickup: LatLng? = null,
    val dropoff: LatLng? = null,
    val route: List<LatLng> = emptyList(),
    val cars: List<LatLng> = emptyList(),
    /** Centre of the visible viewport and how many degrees of latitude it spans. */
    val center: LatLng,
    val spanLat: Double = 0.08,
    val showArea: Boolean = false,
)

/**
 * A stylised, offline city map drawn on a Canvas. Production builds use the Google Maps SDK with the brand's
 * map style; this keeps the demo key-free and lets screenshot tests render the map deterministically.
 */
@Composable
fun CityMap(model: MapModel, modifier: Modifier = Modifier, dark: Boolean = false) {
    val route = MaterialTheme.colorScheme.primary
    val accent = MaterialTheme.colorScheme.secondary
    Canvas(modifier.fillMaxSize()) {
        val land = if (dark) Color(0xFF1F2327) else Color(0xFFEFEDE7)
        val water = if (dark) Color(0xFF16263A) else Color(0xFFB9DCF2)
        val park = if (dark) Color(0xFF1F3326) else Color(0xFFCFE8C8)
        val minor = if (dark) Color(0xFF2C3136) else Color.White
        val major = if (dark) Color(0xFF3A4046) else Color(0xFFFFE8A8)
        drawRect(land)

        val spanLng = model.spanLat * size.width / size.height
        fun p(ll: LatLng) = Offset(
            ((ll.lng - (model.center.lng - spanLng / 2)) / spanLng * size.width).toFloat(),
            (((model.center.lat + model.spanLat / 2) - ll.lat) / model.spanLat * size.height).toFloat(),
        )

        // Deterministic decoration seeded by the area so each brand's city looks consistent.
        val rnd = Random(model.area.name.hashCode())
        drawRect(water, topLeft = Offset(size.width * 0.78f, 0f), size = Size(size.width * 0.22f, size.height))
        repeat(5) {
            drawRoundRect(
                park,
                topLeft = Offset(rnd.nextFloat() * size.width * 0.7f, rnd.nextFloat() * size.height * 0.9f),
                size = Size(size.width * (0.08f + rnd.nextFloat() * 0.12f), size.height * (0.04f + rnd.nextFloat() * 0.06f)),
                cornerRadius = CornerRadius(12f),
            )
        }
        val step = size.width / 9f
        var x = -step / 2
        while (x < size.width) {
            drawLine(minor, Offset(x, 0f), Offset(x + size.height * 0.08f, size.height), strokeWidth = 6f)
            x += step
        }
        var y = 0f
        while (y < size.height) {
            drawLine(minor, Offset(0f, y), Offset(size.width, y - size.width * 0.05f), strokeWidth = 6f)
            y += step
        }
        drawLine(major, Offset(0f, size.height * 0.62f), Offset(size.width, size.height * 0.38f), strokeWidth = 16f)
        drawLine(major, Offset(size.width * 0.32f, 0f), Offset(size.width * 0.44f, size.height), strokeWidth = 14f)

        if (model.showArea) {
            val path = Path().apply {
                model.area.polygon.forEachIndexed { i, ll -> if (i == 0) moveTo(p(ll).x, p(ll).y) else lineTo(p(ll).x, p(ll).y) }
                close()
            }
            drawPath(path, accent.copy(alpha = 0.06f))
            drawPath(path, accent, style = Stroke(width = 5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 16f))))
        }

        if (model.route.size >= 2) {
            val path = Path().apply {
                model.route.forEachIndexed { i, ll -> if (i == 0) moveTo(p(ll).x, p(ll).y) else lineTo(p(ll).x, p(ll).y) }
            }
            drawPath(path, Color.Black.copy(alpha = 0.15f), style = Stroke(width = 22f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(path, route, style = Stroke(width = 12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        model.cars.forEachIndexed { i, ll -> drawCar(p(ll), angle = (i * 47f) % 180f, color = if (dark) Color.White else Color(0xFF2B2F33)) }
        model.pickup?.let { drawPickup(p(it), route) }
        model.dropoff?.let { drawDropoff(p(it), route) }
    }
}

private fun DrawScope.drawPickup(at: Offset, color: Color) {
    drawCircle(Color.Black.copy(alpha = 0.18f), radius = 30f, center = at + Offset(0f, 4f))
    drawCircle(color, radius = 26f, center = at)
    drawCircle(Color.White, radius = 10f, center = at)
}

private fun DrawScope.drawDropoff(at: Offset, color: Color) {
    drawRect(Color.Black.copy(alpha = 0.18f), topLeft = at - Offset(24f, 20f), size = Size(48f, 48f))
    drawRect(color, topLeft = at - Offset(24f, 24f), size = Size(48f, 48f))
    drawRect(Color.White, topLeft = at - Offset(9f, 9f), size = Size(18f, 18f))
}

private fun DrawScope.drawCar(at: Offset, angle: Float, color: Color) {
    rotate(angle, pivot = at) {
        drawRoundRect(color, topLeft = at - Offset(14f, 26f), size = Size(28f, 52f), cornerRadius = CornerRadius(9f))
        drawRoundRect(Color(0xFF9FB4C7), topLeft = at - Offset(10f, 14f), size = Size(20f, 12f), cornerRadius = CornerRadius(3f))
    }
}

/** A simple two-leg street route between two points, standing in for a Directions API response. */
fun demoRoute(from: LatLng, to: LatLng): List<LatLng> {
    val mid1 = LatLng(from.lat + (to.lat - from.lat) * 0.15, from.lng + (to.lng - from.lng) * 0.55)
    val mid2 = LatLng(from.lat + (to.lat - from.lat) * 0.8, from.lng + (to.lng - from.lng) * 0.62)
    return listOf(from, mid1, mid2, to)
}
