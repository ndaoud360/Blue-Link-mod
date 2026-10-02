package com.bluedeck.ui.screens.deck

import android.content.Intent
import android.net.Uri
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DoorBack
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bluedeck.data.models.CommandHistoryEntry
import com.bluedeck.data.models.resolveCapabilities
import com.bluedeck.ui.design.ActionTile
import com.bluedeck.ui.design.CarTopView
import com.bluedeck.ui.design.ChargeRing
import com.bluedeck.ui.design.CircleIconButton
import com.bluedeck.ui.design.CommandFeedback
import com.bluedeck.ui.design.DeckCard
import com.bluedeck.ui.design.DeckConfirmDialog
import com.bluedeck.ui.design.DeckHeader
import com.bluedeck.ui.design.PillTone
import com.bluedeck.ui.design.PrimaryAction
import com.bluedeck.ui.design.ScreenPadding
import com.bluedeck.ui.design.SectionTitle
import com.bluedeck.ui.design.StatCard
import com.bluedeck.ui.design.StatusPill
import com.bluedeck.ui.design.TileShape
import com.bluedeck.ui.design.TileState
import com.bluedeck.ui.design.clockLabel
import com.bluedeck.ui.design.friendlyError
import com.bluedeck.ui.design.relativeTimeLabel
import com.bluedeck.ui.design.softSurface
import com.bluedeck.ui.theme.DeckTheme
import com.bluedeck.ui.theme.formatOdometerFromMiles
import com.bluedeck.ui.theme.milesToDisplayDistance
import com.bluedeck.viewmodel.CommandStatus
import com.bluedeck.viewmodel.VehicleViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvScreen(
    vehicleViewModel: VehicleViewModel,
    onOpenCharge: () -> Unit,
    onOpenClimate: () -> Unit,
    onOpenDetails: () -> Unit,
    onOpenDigitalKey: () -> Unit
) {
    val c = DeckTheme.colors
    val context = LocalContext.current
    val vehicle by vehicleViewModel.selectedVehicle.collectAsStateWithLifecycle()
    val vehicles by vehicleViewModel.vehicles.collectAsStateWithLifecycle()
    val status by vehicleViewModel.vehicleStatus.collectAsStateWithLifecycle()
    val loading by vehicleViewModel.isStatusLoading.collectAsStateWithLifecycle()
    val statusError by vehicleViewModel.statusError.collectAsStateWithLifecycle()
    val lastRefresh by vehicleViewModel.lastStatusRefresh.collectAsStateWithLifecycle()
    val command by vehicleViewModel.commandState.collectAsStateWithLifecycle()
    val history by vehicleViewModel.commandHistory.collectAsStateWithLifecycle()
    val showRecent by vehicleViewModel.showRecentCommands.collectAsStateWithLifecycle()
    val distanceUnit by vehicleViewModel.distanceUnit.collectAsStateWithLifecycle()
    val region by vehicleViewModel.region.collectAsStateWithLifecycle()
    val customImage by vehicleViewModel.customDashboardImageUri.collectAsStateWithLifecycle()
    val climatePending by vehicleViewModel.climateAwaitingConfirmation.collectAsStateWithLifecycle()

    val ev = status.evSnapshot(vehicle)
    val openings = status.openings()
    val warnings = status.warnings()
    val caps = vehicle?.resolveCapabilities(region)
    val locked = status?.doorsLocked
    val busy = command.status == CommandStatus.LOADING || command.status == CommandStatus.ACCEPTED ||
        command.status == CommandStatus.REFRESHING
    val distanceSuffix = if (distanceUnit.equals("KM", true)) "km" else "mi"

    var confirm by remember { mutableStateOf<String?>(null) }
    var lastAction by remember { mutableStateOf<String?>(null) }
    var showVehiclePicker by remember { mutableStateOf(false) }
    var showImageOptions by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
                // Provider does not offer persistable grants; URI still works for now.
            }
            vehicleViewModel.setCustomDashboardImage(uri.toString())
        }
    }

    when (confirm) {
        "unlock" -> DeckConfirmDialog(
            title = "Unlock your car?",
            message = "All doors will unlock remotely.",
            confirmLabel = "Unlock",
            onConfirm = { lastAction = "lock"; vehicleViewModel.unlockDoors() },
            onDismiss = { confirm = null }
        )
        "horn" -> DeckConfirmDialog(
            title = "Sound horn and flash lights?",
            message = "Use this to find your car. It will be heard nearby.",
            confirmLabel = "Sound horn",
            onConfirm = { lastAction = "horn"; vehicleViewModel.hornAndLights() },
            onDismiss = { confirm = null }
        )
    }

    if (showVehiclePicker) {
        AlertDialog(
            onDismissRequest = { showVehiclePicker = false },
            containerColor = c.cardRaised,
            title = { Text("Choose vehicle", color = c.text) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    vehicles.forEach { v ->
                        val selected = v.vin == vehicle?.vin
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(TileShape)
                                .background(if (selected) c.accentSoft else c.card)
                                .clickable {
                                    vehicleViewModel.selectVehicle(v)
                                    showVehiclePicker = false
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.DirectionsCar, null, tint = if (selected) c.accent else c.textMuted)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(v.displayName, color = c.text, style = MaterialTheme.typography.titleSmall)
                                Text("VIN ••••${v.vin.takeLast(6)}", color = c.textFaint, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showVehiclePicker = false }) { Text("Close", color = c.accent) } }
        )
    }

    if (showImageOptions) {
        AlertDialog(
            onDismissRequest = { showImageOptions = false },
            containerColor = c.cardRaised,
            title = { Text("Car picture", color = c.text) },
            text = { Text("Use a photo of your own car, or the built-in drawing that shows open doors and trunk.", color = c.textMuted) },
            confirmButton = {
                TextButton(onClick = {
                    showImageOptions = false
                    imagePicker.launch(arrayOf("image/*"))
                }) { Text("Choose photo", color = c.accent) }
            },
            dismissButton = {
                if (!customImage.isNullOrBlank()) {
                    TextButton(onClick = {
                        showImageOptions = false
                        vehicleViewModel.setCustomDashboardImage(null)
                    }) { Text("Use drawing", color = c.textMuted) }
                }
            }
        )
    }

    Box(Modifier.fillMaxSize().background(c.background)) {
        PullToRefreshBox(
            isRefreshing = loading,
            onRefresh = { vehicleViewModel.refreshStatus(forceFromServer = true) },
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
            ) {
                DeckHeader(
                    title = vehicle?.displayName?.ifBlank { null } ?: "IONIQ 6",
                    subtitle = if (loading) "Updating…" else relativeTimeLabel(lastRefresh),
                    leading = {
                        CircleIconButton(
                            Icons.Filled.Refresh, "Refresh from car",
                            onClick = { vehicleViewModel.refreshStatus(forceFromServer = true) },
                            busy = loading
                        )
                    },
                    trailing = {
                        if (vehicles.size > 1) {
                            CircleIconButton(Icons.Filled.SwapHoriz, "Switch vehicle", onClick = { showVehiclePicker = true })
                        } else {
                            CircleIconButton(Icons.Filled.PhotoCamera, "Car picture", onClick = { showImageOptions = true })
                        }
                    }
                )

                statusError?.let { err ->
                    if (status == null) {
                        ErrorCard(
                            message = friendlyError(err).let { if (it == "Please try again.") "Vehicle status unavailable. Please try again." else it },
                            onRetry = { vehicleViewModel.clearError(); vehicleViewModel.loadVehicles() },
                            modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 8.dp)
                        )
                    }
                }

                // ── Hero: car with opening tiles either side ───────────────────────
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(horizontal = ScreenPadding, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.width(64.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
                        OpeningTile("Driver", Icons.Filled.DoorFront, openings.frontLeft, status != null)
                        OpeningTile("Rear L", Icons.Filled.DoorBack, openings.rearLeft, status != null)
                        OpeningTile("Hood", Icons.Filled.DirectionsCar, openings.hood, status != null)
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(onClickLabel = "Change car picture") { showImageOptions = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!customImage.isNullOrBlank()) {
                            AndroidView(
                                factory = { ctx ->
                                    ImageView(ctx).apply {
                                        adjustViewBounds = true
                                        scaleType = ImageView.ScaleType.FIT_CENTER
                                    }
                                },
                                update = { it.setImageURI(Uri.parse(customImage)) },
                                modifier = Modifier.fillMaxSize().padding(8.dp)
                            )
                        } else {
                            CarTopView(
                                modifier = Modifier.fillMaxSize(),
                                openings = openings,
                                airflowOn = status?.airCtrlOn == true && !climatePending,
                                contentDescription = buildString {
                                    append("Your car")
                                    if (openings.any) append(", something is open") else if (status != null) append(", all closed")
                                }
                            )
                        }
                    }
                    Column(Modifier.width(64.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
                        OpeningTile("Passenger", Icons.Filled.DoorFront, openings.frontRight, status != null)
                        OpeningTile("Rear R", Icons.Filled.DoorBack, openings.rearRight, status != null)
                        OpeningTile("Trunk", Icons.Filled.DirectionsCar, openings.trunk, status != null)
                    }
                }

                // ── Range + battery ───────────────────────────────────────────────
                Row(
                    Modifier.padding(horizontal = ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val rangeValue = ev.rangeMiles?.let { milesToDisplayDistance(it, distanceUnit).roundToInt().toString() } ?: "—"
                    StatCard(
                        label = "Range",
                        value = rangeValue,
                        unit = distanceSuffix,
                        progress = ev.percent?.let { it / 100f },
                        modifier = Modifier.weight(1f),
                        onClick = onOpenCharge
                    )
                    StatCard(
                        label = if (ev.charging) "Charging" else "Battery",
                        value = ev.percent?.toString() ?: "—",
                        unit = "%",
                        progress = ev.percent?.let { it / 100f },
                        modifier = Modifier.weight(1f),
                        onClick = onOpenCharge,
                        trailing = { ChargeRing(ev.percent ?: 0, ev.charging, diameter = 36.dp) }
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ── Lock ──────────────────────────────────────────────────────────
                LockCard(
                    locked = locked,
                    busy = busy && lastAction == "lock",
                    onLock = { lastAction = "lock"; vehicleViewModel.lockDoors() },
                    onUnlock = { confirm = "unlock" },
                    modifier = Modifier.padding(horizontal = ScreenPadding)
                )

                Spacer(Modifier.height(12.dp))

                // ── Quick actions ─────────────────────────────────────────────────
                Row(
                    Modifier.padding(horizontal = ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionTile(
                        Icons.Filled.Highlight, "Lights",
                        onClick = { lastAction = "lights"; vehicleViewModel.flashLights() },
                        modifier = Modifier.weight(1f),
                        state = if (busy && lastAction == "lights") TileState.BUSY else TileState.IDLE,
                        enabled = !busy || lastAction == "lights"
                    )
                    ActionTile(
                        Icons.Filled.Campaign, "Horn",
                        onClick = { confirm = "horn" },
                        modifier = Modifier.weight(1f),
                        state = if (busy && lastAction == "horn") TileState.BUSY else TileState.IDLE,
                        enabled = !busy || lastAction == "horn"
                    )
                    if (caps?.showDigitalKey != false) {
                        ActionTile(
                            Icons.Filled.Key, "Digital Key",
                            onClick = onOpenDigitalKey,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    ActionTile(
                        Icons.Filled.Info, "Details",
                        onClick = onOpenDetails,
                        modifier = Modifier.weight(1f)
                    )
                }

                // ── Climate running ───────────────────────────────────────────────
                AnimatedVisibility(visible = status?.airCtrlOn == true || climatePending) {
                    DeckCard(
                        modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 12.dp).fillMaxWidth(),
                        onClick = onOpenClimate,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Thermostat, null, tint = c.accent)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                if (climatePending) "Climate starting…" else "Climate is on",
                                style = MaterialTheme.typography.titleSmall,
                                color = c.text,
                                modifier = Modifier.weight(1f)
                            )
                            StatusPill(if (climatePending) "Waiting for car" else "Running", if (climatePending) PillTone.NEUTRAL else PillTone.GOOD)
                        }
                    }
                }

                // ── Warnings ──────────────────────────────────────────────────────
                if (warnings.isNotEmpty()) {
                    SectionTitle("Needs attention", Modifier.padding(horizontal = ScreenPadding).padding(top = 12.dp))
                    DeckCard(Modifier.padding(horizontal = ScreenPadding).fillMaxWidth()) {
                        warnings.forEachIndexed { i, w ->
                            if (i > 0) Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(w.icon, null, tint = if (w.tone == PillTone.DANGER) c.danger else c.caution, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(w.title, style = MaterialTheme.typography.bodyMedium, color = c.text)
                            }
                        }
                    }
                }

                // ── At a glance ───────────────────────────────────────────────────
                if (status != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.padding(horizontal = ScreenPadding),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val odo = status?.totalMileage?.takeIf { it > 0 } ?: vehicle?.odometer ?: 0
                        StatCard(
                            label = "Odometer",
                            value = formatOdometerFromMiles(odo, distanceUnit),
                            modifier = Modifier.weight(1f)
                        )
                        val aux = status?.battery?.batteryLevel?.takeIf { it in 1..100 }
                        StatCard(
                            label = "12V battery",
                            value = aux?.toString() ?: "—",
                            unit = if (aux != null) "%" else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ── Recent activity ───────────────────────────────────────────────
                if (showRecent && history.isNotEmpty()) {
                    SectionTitle(
                        "Recent activity",
                        Modifier.padding(horizontal = ScreenPadding).padding(top = 12.dp),
                        trailing = {
                            TextButton(onClick = { vehicleViewModel.clearCommandHistory() }) {
                                Text("Clear", color = c.textMuted, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    )
                    DeckCard(Modifier.padding(horizontal = ScreenPadding).fillMaxWidth()) {
                        history.take(5).forEachIndexed { i, entry ->
                            if (i > 0) Spacer(Modifier.height(12.dp))
                            HistoryRow(entry)
                        }
                    }
                }
            }
        }
        CommandFeedback(command, Modifier.align(Alignment.TopCenter).padding(top = 56.dp))
    }
}

@Composable
private fun OpeningTile(label: String, icon: ImageVector, open: Boolean, known: Boolean) {
    val c = DeckTheme.colors
    val fill by animateColorAsState(if (open) c.cautionSoft else c.cardRaised, label = "opening")
    Column(
        Modifier
            .fillMaxWidth()
            .softSurface(c, TileShape, fill, elevation = 4.dp)
            .padding(vertical = 10.dp)
            .semantics { contentDescription = "$label " + when { !known -> "unknown"; open -> "open"; else -> "closed" } },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = if (open) c.caution else c.textFaint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            if (open) "Open" else label,
            style = MaterialTheme.typography.labelSmall,
            color = if (open) c.caution else c.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LockCard(
    locked: Boolean?,
    busy: Boolean,
    onLock: () -> Unit,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = DeckTheme.colors
    DeckCard(modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val tint by animateColorAsState(
                when (locked) { true -> c.accent; false -> c.caution; null -> c.textFaint },
                label = "lockTint"
            )
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(locked, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "lockIcon") { l ->
                    Icon(if (l == false) Icons.Filled.LockOpen else Icons.Filled.Lock, null, tint = tint)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when (locked) { true -> "Locked"; false -> "Unlocked"; null -> "Lock status unknown" },
                    style = MaterialTheme.typography.titleLarge,
                    color = c.text
                )
                Text(
                    when (locked) { true -> "All doors secured"; false -> "Doors are unlocked"; null -> "Refresh to check" },
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textMuted
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        if (locked == true) {
            PrimaryAction("Unlock", onUnlock, icon = Icons.Filled.LockOpen, busy = busy, destructive = true)
        } else {
            PrimaryAction("Lock", onLock, icon = Icons.Filled.Lock, busy = busy)
        }
    }
}

@Composable
internal fun HistoryRow(entry: CommandHistoryEntry) {
    val c = DeckTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.History, null,
            tint = if (entry.successful) c.textFaint else c.danger,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.title, style = MaterialTheme.typography.bodyMedium, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (entry.successful) entry.detail else friendlyError(entry.detail),
                style = MaterialTheme.typography.bodySmall,
                color = c.textFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(clockLabel(entry.timestampMillis), style = MaterialTheme.typography.labelSmall, color = c.textFaint)
    }
}

@Composable
internal fun ErrorCard(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val c = DeckTheme.colors
    DeckCard(modifier.fillMaxWidth()) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = c.text)
        Spacer(Modifier.height(12.dp))
        PrimaryAction("Try again", onRetry, icon = Icons.Filled.Refresh)
    }
}

