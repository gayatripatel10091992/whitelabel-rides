package com.gayatripatel.rides.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gayatripatel.rides.core.BrandConfig
import com.gayatripatel.rides.core.LatLng

data class TripRequestUi(
    val riderName: String,
    val riderRating: Double,
    val pickup: String,
    val dropoff: String,
    val km: String,
    val fare: String,
    val secondsLeft: Int,
    val totalSeconds: Int,
    val isDelivery: Boolean,
)

data class DriverHomeUiState(
    val brand: BrandConfig,
    val online: Boolean,
    val earningsToday: String,
    val tripsToday: Int,
    val gpsSent: Int,
    val gpsFixes: Int,
    val request: TripRequestUi?,
    val map: MapModel,
)

fun driverHomeState(brand: BrandConfig, online: Boolean = true, withRequest: Boolean = true, delivery: Boolean = false): DriverHomeUiState {
    val s = Scenarios.forBrand(brand)
    val home = riderHomeState(brand)
    val vehicle = if (delivery) brand.vehicles.last() else brand.vehicles.first()
    val fare = Scenarios.fare(brand, vehicle, s.pickup.position, s.dropoff.position)
    val me = LatLng(s.pickup.position.lat + home.map.spanLat * 0.09, s.pickup.position.lng + home.map.spanLat * 0.05)
    return DriverHomeUiState(
        brand = brand,
        online = online,
        earningsToday = brand.currency.format(fare.totalMinor * 7),
        tripsToday = 7,
        gpsSent = 74,
        gpsFixes = 240,
        request = if (!withRequest) null else TripRequestUi(
            riderName = if (delivery) "Amina W." else "Priya S.",
            riderRating = 4.8,
            pickup = s.pickup.label,
            dropoff = s.dropoff.label,
            km = "%.1f".format(java.util.Locale.US, Scenarios.estimateKm(s.pickup.position, s.dropoff.position)),
            fare = brand.currency.format(fare.totalMinor),
            secondsLeft = 11,
            totalSeconds = 15,
            isDelivery = delivery,
        ),
        map = home.map.copy(route = if (withRequest) demoRoute(me, s.pickup.position) else emptyList(), dropoff = null, cars = listOf(me)),
    )
}

@Composable
fun DriverHomeScreen(state: DriverHomeUiState, onOnlineChange: (Boolean) -> Unit, onAccept: () -> Unit, onDecline: () -> Unit, dark: Boolean = false) {
    Box(Modifier.fillMaxSize()) {
        CityMap(state.map, dark = dark)
        Surface(
            Modifier.statusBarsPadding().padding(12.dp).fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 6.dp,
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(12.dp).background(if (state.online) Color(0xFF22C55E) else MaterialTheme.colorScheme.outline, CircleShape),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(if (state.online) R.string.you_are_online else R.string.you_are_offline),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = state.online, onCheckedChange = onOnlineChange)
                }
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.today), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(state.earningsToday, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(stringResource(R.string.trips_count, state.tripsToday), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.GpsFixed, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.gps_stats, state.gpsSent, state.gpsFixes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        state.request?.let { request ->
            BottomSheetCard(Modifier.align(Alignment.BottomCenter)) { RequestCard(request, onAccept, onDecline) }
        }
    }
}

@Composable
private fun RequestCard(request: TripRequestUi, onAccept: () -> Unit, onDecline: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(if (request.isDelivery) R.string.new_delivery else R.string.new_request),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(request.riderName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Filled.Star, null, tint = Color(0xFFF5B301), modifier = Modifier.size(14.dp))
                Text("${request.riderRating}", style = MaterialTheme.typography.labelMedium)
            }
        }
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { request.secondsLeft / request.totalSeconds.toFloat() },
                modifier = Modifier.size(56.dp),
                strokeWidth = 5.dp,
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            Text("${request.secondsLeft}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
    Spacer(Modifier.height(14.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("● ${request.pickup}", maxLines = 1)
            Text("■ ${request.dropoff}", maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(request.fare, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.trip_distance, request.km), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onDecline, modifier = Modifier.weight(1f).height(54.dp)) { Text(stringResource(R.string.decline)) }
        Button(onClick = onAccept, modifier = Modifier.weight(2f).height(54.dp)) { Text(stringResource(R.string.accept)) }
    }
}

data class DeliveryUiState(
    val brand: BrandConfig,
    val recipient: String,
    val address: String,
    val parcelNote: String,
    val fare: String,
)

fun deliveryState(brand: BrandConfig): DeliveryUiState {
    val s = Scenarios.forBrand(brand)
    val parcel = brand.vehicles.last()
    return DeliveryUiState(
        brand, "Amina Wanjiru", s.dropoff.label, "Small box · documents · handle with care",
        brand.currency.format(Scenarios.fare(brand, parcel, s.pickup.position, s.dropoff.position).totalMinor),
    )
}

@Composable
fun DeliveryScreen(
    state: DeliveryUiState,
    signature: SignaturePadState,
    onConfirm: () -> Unit,
    demoSignature: Boolean = false,
) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding().padding(20.dp),
    ) {
        BrandBadge(state.brand)
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Inventory2, null) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.deliver_to, state.recipient), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(state.address, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(state.fare, fontWeight = FontWeight.Bold)
        }
        Text(state.parcelNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.collect_signature), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier.fillMaxWidth().height(260.dp)
                .background(Color.White, RoundedCornerShape(20.dp))
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)),
        ) {
            if (!signature.isSigned) {
                Text(
                    stringResource(R.string.sign_here),
                    color = Color(0xFF9AA0A6),
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            SignaturePad(signature, inkColor = Color(0xFF1A1C1E).toArgb(), modifier = Modifier.fillMaxSize(), demo = demoSignature)
            TextButton(onClick = { signature.clear() }, modifier = Modifier.align(Alignment.TopEnd)) { Text(stringResource(R.string.clear)) }
        }
        Text(stringResource(R.string.proof_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
        Spacer(Modifier.weight(1f))
        Button(onClick = onConfirm, enabled = signature.isSigned, modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Text(stringResource(R.string.confirm_delivery))
        }
    }
}
