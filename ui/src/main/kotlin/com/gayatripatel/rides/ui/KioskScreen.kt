package com.gayatripatel.rides.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayatripatel.rides.core.BrandConfig
import com.gayatripatel.rides.core.FareCalculator

data class KioskDestination(val id: String, val name: String, val icon: ImageVector, val fare: String, val minutes: Int)

data class KioskTicket(val number: String, val bay: Int, val minutes: Int, val destination: String)

data class KioskUiState(val brand: BrandConfig, val destinations: List<KioskDestination>, val ticket: KioskTicket?)

fun kioskState(brand: BrandConfig, ticket: KioskTicket? = null): KioskUiState {
    val rules = brand.vehicles.first().rules
    fun fare(km: Double) = brand.currency.format(FareCalculator.calculate(rules, brand.currency, km, Scenarios.estimateMinutes(km)).totalMinor)
    return KioskUiState(
        brand,
        listOf(
            KioskDestination("airport", "Airport", Icons.Filled.Flight, fare(21.0), 28),
            KioskDestination("downtown", "Downtown", Icons.Filled.LocationCity, fare(5.5), 14),
            KioskDestination("station", "Central Station", Icons.Filled.Train, fare(3.2), 9),
            KioskDestination("stadium", "Stadium", Icons.Filled.SportsSoccer, fare(8.4), 19),
        ),
        ticket,
    )
}

/**
 * Self-service taxi kiosk (hotel lobby / mall). The host activity pins itself with Lock Task Mode,
 * so riders can't leave the app; every booking is cash or card-to-driver with a printed-style ticket number.
 */
@Composable
fun KioskScreen(state: KioskUiState, onPick: (KioskDestination) -> Unit, onOther: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandBadge(state.brand)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.Lock, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(" " + stringResource(R.string.kiosk_mode), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.kiosk_title), fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.kiosk_subtitle), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        state.ticket?.let { ticket ->
            Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.kiosk_ticket), color = MaterialTheme.colorScheme.onPrimary)
                    Text(ticket.number, color = MaterialTheme.colorScheme.onPrimary, fontSize = 72.sp, fontWeight = FontWeight.Black)
                    Text(ticket.destination, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.kiosk_ticket_hint, ticket.bay, ticket.minutes),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(state.destinations, key = { it.id }) { d ->
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.height(170.dp).clickable { onPick(d) },
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Box(
                            Modifier.size(52.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(d.icon, null, modifier = Modifier.size(28.dp)) }
                        Spacer(Modifier.weight(1f))
                        Text(d.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Row {
                            Text(d.fare, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("· ${d.minutes} min", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        OutlinedButton(onClick = onOther, modifier = Modifier.fillMaxWidth().height(64.dp)) {
            Icon(Icons.Filled.Place, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.kiosk_other), style = MaterialTheme.typography.titleMedium)
        }
    }
}
