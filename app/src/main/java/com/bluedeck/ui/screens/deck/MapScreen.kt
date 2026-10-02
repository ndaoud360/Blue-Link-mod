package com.bluedeck.ui.screens.deck

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.location.Geocoder
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bluedeck.data.chargers.ChargerFilter
import com.bluedeck.data.chargers.ChargingStation
import com.bluedeck.ui.design.CircleIconButton
import com.bluedeck.ui.design.DeckCard
import com.bluedeck.ui.design.DeckHeader
import com.bluedeck.ui.design.DeckSegmented
import com.bluedeck.ui.design.PillTone
import com.bluedeck.ui.design.ScreenPadding
import com.bluedeck.ui.design.StatusPill
import com.bluedeck.ui.design.friendlyError
import com.bluedeck.ui.design.relativeTimeLabel
import com.bluedeck.ui.theme.DeckColors
import com.bluedeck.ui.theme.DeckTheme
import com.bluedeck.viewmodel.MapViewModel
import com.bluedeck.viewmodel.VehicleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    vehicleViewModel: VehicleViewModel,
    mapViewModel: MapViewModel = hiltViewModel()
) {
    val c = DeckTheme.colors
    val context = LocalContext.current
    val vehicle by vehicleViewModel.selectedVehicle.collectAsStateWithLifecycle()
    val cached by vehicleViewModel.lastKnownLocation.collectAsStateWithLifecycle()
    val locating by vehicleViewModel.isLocationLoading.collectAsStateWithLifecycle()
    val locationError by vehicleViewModel.locationError.collectAsStateWithLifecycle()
    val distanceUnit by vehicleViewModel.distanceUnit.collectAsStateWithLifecycle()
    val chargers by mapViewModel.state.collectAsStateWithLifecycle()

    val car = cached?.takeIf { vehicle == null || it.vin.isBlank() || it.vin == vehicle?.vin }
    var selected by remember { mutableStateOf<ChargingStation?>(null) }
    var showList by remember { mutableStateOf(false) }
    var recenterTick by remember { mutableIntStateOf(0) }

    // Locate on open only when the stored position is stale (never polled in a loop).
    LaunchedEffect(vehicle?.vin) { if (vehicle != null) vehicleViewModel.refreshLocationIfStale() }
    LaunchedEffect(car?.latitude, car?.longitude) {
        car?.let { mapViewModel.loadAround(it.latitude, it.longitude) }
    }

    val address by produceState<String?>(null, car?.latitude, car?.longitude) {
        value = car?.let { reverseGeocode(context, it.latitude, it.longitude) }
    }

    if (showList) {
        ModalBottomSheet(
            onDismissRequest = { showList = false },
            containerColor = c.cardRaised
        ) {
            Text(
                "Nearby chargers",
                style = MaterialTheme.typography.titleMedium,
                color = c.text,
                modifier = Modifier.padding(horizontal = ScreenPadding)
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.fillMaxWidth().height(460.dp)) {
                items(chargers.stations, key = { it.id }) { s ->
                    StationRow(s, distanceUnit) {
                        selected = s
                        showList = false
                    }
                    HorizontalDivider(color = c.hairline, modifier = Modifier.padding(horizontal = ScreenPadding))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    Box(Modifier.fillMaxSize().background(c.background)) {
        if (car != null) {
            OsmMap(
                colors = c,
                car = GeoPoint(car.latitude, car.longitude),
                stations = chargers.stations,
                selected = selected,
                recenterTick = recenterTick,
                onStationClick = { selected = it },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.DirectionsCar, null, tint = c.textFaint, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(12.dp))
                Text(
                    if (locating) "Finding your car…" else "No location yet. Tap locate to ask your car where it is.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textMuted
                )
            }
        }

        // Floating header over the map, as in the "Charging Map" concept
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(84.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(c.background, c.background.copy(alpha = 0.7f), c.background.copy(alpha = 0f))
                    )
                )
        )
        DeckHeader(
            title = "Map",
            subtitle = car?.let { relativeTimeLabel(it.updatedAtMillis).replace("Updated", "Located") },
            modifier = Modifier.align(Alignment.TopCenter),
            leading = {
                CircleIconButton(Icons.Filled.MyLocation, "Center on car", onClick = { recenterTick++ }, enabled = car != null)
            },
            trailing = {
                CircleIconButton(Icons.Filled.Refresh, "Locate car now", onClick = { vehicleViewModel.refreshLocation() }, busy = locating)
            }
        )

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = ScreenPadding, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            locationError?.let {
                DeckCard(Modifier.fillMaxWidth(), onClick = { vehicleViewModel.clearLocationError() }) {
                    Text(
                        "Couldn't get a new location. " + friendlyError(it) +
                            (if (car != null) " Showing the last known position." else ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = c.caution
                    )
                }
            }

            if (car != null) {
                DeckCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MarkerDot(c.accent) { Icon(Icons.Filled.DirectionsCar, null, tint = c.onAccent, modifier = Modifier.size(16.dp)) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Your car", style = MaterialTheme.typography.labelMedium, color = c.textMuted)
                            Text(
                                address ?: "%.5f, %.5f".format(Locale.US, car.latitude, car.longitude),
                                style = MaterialTheme.typography.bodyMedium,
                                color = c.text,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        CircleIconButton(Icons.Filled.Route, "Directions to car", {
                            openDirections(context, car.latitude, car.longitude, "My car")
                        })
                    }
                }
            }

            // Chargers
            if (!chargers.configured) {
                DeckCard(Modifier.fillMaxWidth()) {
                    Text(
                        "Nearby chargers need a free NREL API key in local.properties (NREL_API_KEY).",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textMuted
                    )
                }
            } else if (car != null) {
                DeckSegmented(
                    options = listOf(ChargerFilter.ALL to "All", ChargerFilter.DC_FAST to "DC fast", ChargerFilter.LEVEL_2 to "Level 2"),
                    selected = chargers.filter,
                    onSelect = {
                        selected = null
                        mapViewModel.setFilter(it, car.latitude, car.longitude)
                    }
                )
                val shown = selected ?: chargers.stations.firstOrNull()
                DeckCard(
                    Modifier.fillMaxWidth(),
                    onClick = if (chargers.stations.isNotEmpty()) ({ showList = true }) else null,
                    onClickLabel = "Show all chargers",
                    contentPadding = PaddingValues(14.dp)
                ) {
                    when {
                        chargers.loading -> Text("Finding chargers…", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                        chargers.error != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(chargers.error ?: "", style = MaterialTheme.typography.bodySmall, color = c.caution, modifier = Modifier.weight(1f))
                            TextButton(onClick = { mapViewModel.loadAround(car.latitude, car.longitude, force = true) }) {
                                Text("Retry", color = c.accent)
                            }
                        }
                        shown == null -> Text("No public chargers within 25 miles.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                        else -> StationSummary(
                            station = shown,
                            heading = if (selected != null) "Selected station" else "Nearest station",
                            distanceUnit = distanceUnit,
                            count = chargers.stations.size,
                            onNavigate = { openDirections(context, shown.latitude, shown.longitude, shown.name) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkerDot(color: androidx.compose.ui.graphics.Color, content: @Composable () -> Unit) {
    Box(
        Modifier.size(32.dp).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center
    ) { content() }
}

private fun formatMiles(miles: Double?, distanceUnit: String): String? {
    miles ?: return null
    return if (distanceUnit.equals("KM", true)) "%.1f km".format(miles * 1.609344) else "%.1f mi".format(miles)
}

@Composable
private fun StationSummary(
    station: ChargingStation,
    heading: String,
    distanceUnit: String,
    count: Int,
    onNavigate: () -> Unit
) {
    val c = DeckTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        MarkerDot(if (station.isDcFast) c.accent else c.cardPressed) {
            Icon(if (station.isDcFast) Icons.Filled.Bolt else Icons.Filled.EvStation, null,
                tint = if (station.isDcFast) c.onAccent else c.text, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(heading, style = MaterialTheme.typography.labelMedium, color = c.textMuted)
            Text(station.name, style = MaterialTheme.typography.bodyMedium, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(
                    formatMiles(station.distanceMiles, distanceUnit),
                    if (station.isDcFast) "${station.dcFastPorts} DC fast" else "${station.level2Ports} Level 2",
                    station.network
                ).joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = c.textFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        CircleIconButton(Icons.Filled.Route, "Directions to ${station.name}", onNavigate)
    }
    if (count > 1) {
        Spacer(Modifier.height(8.dp))
        Text("Tap to see all $count chargers", style = MaterialTheme.typography.labelSmall, color = c.accent)
    }
}

@Composable
private fun StationRow(s: ChargingStation, distanceUnit: String, onClick: () -> Unit) {
    val c = DeckTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = ScreenPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(s.name, style = MaterialTheme.typography.bodyMedium, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(s.address, style = MaterialTheme.typography.bodySmall, color = c.textFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (s.dcFastPorts > 0) StatusPill("${s.dcFastPorts} DC", PillTone.GOOD)
                if (s.level2Ports > 0) StatusPill("${s.level2Ports} L2")
                s.connectorLabels.take(2).forEach { StatusPill(it) }
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(formatMiles(s.distanceMiles, distanceUnit) ?: "", style = MaterialTheme.typography.labelMedium, color = c.textMuted)
    }
}

private suspend fun reverseGeocode(context: Context, lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
    if (!Geocoder.isPresent()) return@withContext null
    runCatching {
        @Suppress("DEPRECATION")
        Geocoder(context, Locale.getDefault()).getFromLocation(lat, lon, 1)?.firstOrNull()?.let { a ->
            listOfNotNull(
                listOfNotNull(a.subThoroughfare, a.thoroughfare).joinToString(" ").ifBlank { null },
                a.locality
            ).joinToString(", ").ifBlank { null }
        }
    }.getOrNull()
}

private fun openDirections(context: Context, lat: Double, lon: Double, label: String) {
    val uri = Uri.parse("geo:0,0?q=$lat,$lon(${Uri.encode(label)})")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.openstreetmap.org/?mlat=$lat&mlon=$lon#map=17/$lat/$lon"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

// ── osmdroid bridge ─────────────────────────────────────────────────────────────

private fun dotDrawable(context: Context, fill: Int, ring: Int, sizeDp: Int, glyph: Char?): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val px = (sizeDp * density).toInt()
    val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = ring
    canvas.drawCircle(px / 2f, px / 2f, px / 2f, paint)
    paint.color = fill
    canvas.drawCircle(px / 2f, px / 2f, px / 2f - 2.5f * density, paint)
    if (glyph != null) {
        paint.color = ring
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = px * 0.5f
        paint.isFakeBoldText = true
        val y = px / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(glyph.toString(), px / 2f, y, paint)
    }
    return BitmapDrawable(context.resources, bmp)
}

/** Muted tiles that sit quietly under the UI: desaturated in light, inverted graphite in dark. */
private fun tileFilter(dark: Boolean): ColorMatrixColorFilter {
    val m = ColorMatrix().apply { setSaturation(if (dark) 0.1f else 0.35f) }
    if (dark) {
        val invert = ColorMatrix(
            floatArrayOf(
                -0.85f, 0f, 0f, 0f, 235f,
                0f, -0.85f, 0f, 0f, 240f,
                0f, 0f, -0.85f, 0f, 240f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        m.postConcat(invert)
    }
    return ColorMatrixColorFilter(m)
}

@Composable
private fun OsmMap(
    colors: DeckColors,
    car: GeoPoint,
    stations: List<ChargingStation>,
    selected: ChargingStation?,
    recenterTick: Int,
    onStationClick: (ChargingStation) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember {
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            userAgentValue = context.packageName
            // Keep the tile cache in app-private storage (no storage permission needed).
            osmdroidBasePath = java.io.File(context.cacheDir, "osmdroid")
            osmdroidTileCache = java.io.File(osmdroidBasePath, "tiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            controller.setZoom(14.5)
            controller.setCenter(car)
        }
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        mapView.onResume()
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    LaunchedEffect(recenterTick, car.latitude, car.longitude) {
        mapView.controller.animateTo(car)
    }
    LaunchedEffect(selected) {
        selected?.let { mapView.controller.animateTo(GeoPoint(it.latitude, it.longitude)) }
    }

    val accent = colors.accent.toArgb()
    val ring = colors.cardRaised.toArgb()
    val muted = colors.textMuted.toArgb()
    val dark = colors.isDark

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { map ->
            map.overlayManager.tilesOverlay.setColorFilter(tileFilter(dark))
            map.overlays.clear()
            stations.forEach { s ->
                val isSel = s.id == selected?.id
                map.overlays.add(Marker(map).apply {
                    position = GeoPoint(s.latitude, s.longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = dotDrawable(
                        context,
                        fill = if (s.isDcFast || isSel) accent else muted,
                        ring = ring,
                        sizeDp = if (isSel) 30 else 22,
                        glyph = if (s.isDcFast) 'D' else null
                    )
                    title = s.name
                    setOnMarkerClickListener { _, _ -> onStationClick(s); true }
                })
            }
            map.overlays.add(Marker(map).apply {
                position = car
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = dotDrawable(context, fill = accent, ring = ring, sizeDp = 34, glyph = null)
                title = "Your car"
                setOnMarkerClickListener { _, _ -> true }
            })
            map.invalidate()
        }
    )
}
