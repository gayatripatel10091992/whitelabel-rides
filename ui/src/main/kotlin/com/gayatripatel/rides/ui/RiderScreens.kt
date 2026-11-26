package com.gayatripatel.rides.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayatripatel.rides.core.BrandConfig
import com.gayatripatel.rides.core.FareBreakdown
import com.gayatripatel.rides.core.FareLineKind
import com.gayatripatel.rides.core.GatewayId
import com.gayatripatel.rides.core.LatLng

@Composable
fun BrandBadge(brand: BrandConfig, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(12.dp).background(MaterialTheme.colorScheme.secondary, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(brand.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun BottomSheetCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 12.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.navigationBarsPadding().padding(20.dp), content = content)
    }
}

@Composable
private fun PlaceRow(label: String, value: String, square: Boolean, onClick: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(12.dp).background(MaterialTheme.colorScheme.primary, if (square) RoundedCornerShape(2.dp) else CircleShape),
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun RiderHomeScreen(
    state: RiderHomeUiState,
    onSelectVehicle: (String) -> Unit,
    onBookingForSomeoneElse: (Boolean) -> Unit,
    onOtherName: (String) -> Unit,
    onOtherPhone: (String) -> Unit,
    onSelectPayment: (GatewayId) -> Unit,
    onEditDropoff: () -> Unit,
    onBook: () -> Unit,
    dark: Boolean = false,
) {
    Box(Modifier.fillMaxSize()) {
        CityMap(state.map, dark = dark)
        BrandBadge(state.brand, Modifier.statusBarsPadding().padding(16.dp))
        BottomSheetCard(Modifier.align(Alignment.BottomCenter)) {
            Text(stringResource(R.string.where_to), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            PlaceRow(stringResource(R.string.pickup), state.pickup.label, square = false)
            PlaceRow(stringResource(R.string.dropoff), state.dropoff.label, square = true, onClick = onEditDropoff)
            Spacer(Modifier.height(8.dp))

            if (state.outsideArea) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.errorContainer).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.WarningAmber, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            stringResource(R.string.outside_area, state.dropoff.label, state.brand.serviceArea.name),
                            color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.SemiBold,
                        )
                        Text(stringResource(R.string.outside_area_hint), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                    }
                }
                return@BottomSheetCard
            }

            state.tripKm?.let {
                Text(
                    stringResource(R.string.approx_trip, it, state.tripMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.vehicles.forEach { v -> VehicleCard(v, Modifier.weight(1f)) { onSelectVehicle(v.id) } }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.booking_for), style = MaterialTheme.typography.labelLarge)
                FilterChip(!state.bookingForSomeoneElse, { onBookingForSomeoneElse(false) }, label = { Text(stringResource(R.string.me)) })
                FilterChip(state.bookingForSomeoneElse, { onBookingForSomeoneElse(true) }, label = { Text(stringResource(R.string.someone_else)) })
            }
            if (state.bookingForSomeoneElse) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(state.otherRiderName, onOtherName, Modifier.weight(1f), label = { Text(stringResource(R.string.rider_name)) }, singleLine = true)
                    OutlinedTextField(state.otherRiderPhone, onOtherPhone, Modifier.weight(1f), label = { Text(stringResource(R.string.rider_phone)) }, singleLine = true)
                }
                Text(stringResource(R.string.someone_else_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.pay_with), style = MaterialTheme.typography.labelLarge)
                state.payments.forEach { g ->
                    FilterChip(g == state.selectedPayment, { onSelectPayment(g) }, label = { Text(g.label) })
                }
            }
            Spacer(Modifier.height(12.dp))
            val selected = state.vehicles.firstOrNull { it.selected }
            Button(onClick = onBook, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Text(
                    stringResource(R.string.book_vehicle, selected?.name.orEmpty()) + "  ·  " + selected?.price.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun VehicleCard(v: VehicleOptionUi, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (v.selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (v.selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(v.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(
                if (v.seats == 0) stringResource(R.string.parcel_only) else stringResource(R.string.seats, v.seats),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(stringResource(R.string.min_away, v.etaMinutes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(v.price, fontWeight = FontWeight.Bold)
        }
    }
}

data class RiderTripUiState(
    val brand: BrandConfig,
    val driverName: String,
    val car: String,
    val plate: String,
    val rating: Double,
    val etaMinutes: Int,
    val bookedFor: String?,
    val cancellationFee: String,
    val map: MapModel,
    val parcel: Boolean = false,
)

fun riderTripState(brand: BrandConfig, bookedFor: String? = null): RiderTripUiState {
    val s = Scenarios.forBrand(brand)
    val home = riderHomeState(brand)
    val car = LatLng(s.pickup.position.lat + home.map.spanLat * 0.07, s.pickup.position.lng - home.map.spanLat * 0.04)
    return RiderTripUiState(
        brand, s.driverName, s.car, s.plate, 4.9, 3, bookedFor,
        brand.currency.format(brand.vehicles.first().rules.cancellationFeeMinor),
        home.map.copy(route = demoRoute(car, s.pickup.position), dropoff = null, cars = listOf(car)),
    )
}

@Composable
fun RiderTripScreen(state: RiderTripUiState, onCall: () -> Unit, onMessage: () -> Unit, onCancel: () -> Unit, dark: Boolean = false) {
    Box(Modifier.fillMaxSize()) {
        CityMap(state.map, dark = dark)
        BrandBadge(state.brand, Modifier.statusBarsPadding().padding(16.dp))
        BottomSheetCard(Modifier.align(Alignment.BottomCenter)) {
            Text(stringResource(R.string.driver_on_way), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.arriving_in, state.etaMinutes), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
            state.bookedFor?.let {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.booked_for, it), style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.driverName, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.Star, null, tint = Color(0xFFF5B301), modifier = Modifier.size(16.dp))
                        Text("${state.rating}", style = MaterialTheme.typography.labelLarge)
                    }
                    Text(state.car, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    state.plate,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(horizontal = 10.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionTile(Icons.Filled.Phone, stringResource(R.string.call), Modifier.weight(1f), onCall)
                ActionTile(Icons.AutoMirrored.Filled.Chat, stringResource(R.string.message), Modifier.weight(1f), onMessage)
                ActionTile(Icons.Filled.Close, stringResource(R.string.cancel), Modifier.weight(1f), onCancel)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.free_cancel, state.cancellationFee),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActionTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

data class ReceiptUiState(
    val brand: BrandConfig,
    val driverName: String,
    val fare: FareBreakdown,
    val payment: GatewayId,
    val rating: Int,
    val parcel: Boolean = false,
)

@Composable
fun fareLineLabel(kind: FareLineKind): String = stringResource(
    when (kind) {
        FareLineKind.BASE -> R.string.fare_base
        FareLineKind.DISTANCE -> R.string.fare_distance
        FareLineKind.TIME -> R.string.fare_time
        FareLineKind.WAITING -> R.string.fare_waiting
        FareLineKind.SURGE -> R.string.fare_surge
        FareLineKind.MINIMUM_ADJUSTMENT -> R.string.fare_minimum
        FareLineKind.BOOKING_FEE -> R.string.fare_booking
        FareLineKind.TOLLS -> R.string.fare_tolls
        FareLineKind.PROMO -> R.string.fare_promo
        FareLineKind.TAX -> R.string.fare_tax
    },
)

@Composable
fun ReceiptScreen(state: ReceiptUiState, onRate: (Int) -> Unit, onDone: () -> Unit) {
    val currency = state.fare.currency
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding().padding(24.dp),
    ) {
        BrandBadge(state.brand)
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.trip_receipt), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(currency.format(state.fare.totalMinor), fontSize = 44.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.paid_with, state.payment.label), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.padding(18.dp)) {
                state.fare.lines.forEach { line ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        Text(
                            fareLineLabel(line.kind) + if (line.detail.isNotEmpty()) "  ·  ${line.detail}" else "",
                            modifier = Modifier.weight(1f),
                            color = if (line.amountMinor < 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            (if (line.amountMinor < 0) "−" else "") + currency.format(kotlin.math.abs(line.amountMinor)),
                            color = if (line.amountMinor < 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.total), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text(currency.format(state.fare.totalMinor), fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (state.parcel) Icon(Icons.Filled.Inventory2, null, modifier = Modifier.padding(end = 8.dp))
            Text(stringResource(R.string.rate_trip, state.driverName), style = MaterialTheme.typography.titleMedium)
        }
        Row {
            (1..5).forEach { i ->
                Icon(
                    if (i <= state.rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "$i",
                    tint = Color(0xFFF5B301),
                    modifier = Modifier.size(44.dp).clickable { onRate(i) }.padding(4.dp),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text(stringResource(R.string.done)) }
        TextButton(onClick = {}, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(state.brand.supportPhone) }
    }
}
