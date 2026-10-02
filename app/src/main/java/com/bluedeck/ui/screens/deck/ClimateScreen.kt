package com.bluedeck.ui.screens.deck

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AirlineSeatReclineNormal
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bluedeck.data.models.ClimatePreset
import com.bluedeck.data.models.ClimatePresetsStore
import com.bluedeck.data.models.MAX_CLIMATE_DURATION_MINUTES
import com.bluedeck.data.models.MIN_CLIMATE_DURATION_MINUTES
import com.bluedeck.data.models.SeatClimateCapabilities
import com.bluedeck.data.models.resolveCapabilities
import com.bluedeck.ui.design.ActionTile
import com.bluedeck.ui.design.ArcDial
import com.bluedeck.ui.design.CarTopView
import com.bluedeck.ui.design.CircleIconButton
import com.bluedeck.ui.design.CommandFeedback
import com.bluedeck.ui.design.DeckCard
import com.bluedeck.ui.design.DeckConfirmDialog
import com.bluedeck.ui.design.DeckHeader
import com.bluedeck.ui.design.DeckSegmented
import com.bluedeck.ui.design.PrimaryAction
import com.bluedeck.ui.design.ScreenPadding
import com.bluedeck.ui.design.SectionTitle
import com.bluedeck.ui.design.TileShape
import com.bluedeck.ui.design.TileState
import com.bluedeck.ui.design.softSurface
import com.bluedeck.ui.theme.DeckTheme
import com.bluedeck.ui.theme.apiTemperatureToPreferredLabel
import com.bluedeck.ui.theme.climateDisplayValueFromF
import com.bluedeck.ui.theme.climateFahrenheitFromDisplay
import com.bluedeck.ui.theme.climateSliderRange
import com.bluedeck.ui.theme.climateTemperatureLabelFromF
import com.bluedeck.viewmodel.CommandStatus
import com.bluedeck.viewmodel.RemoteStartSettings
import com.bluedeck.viewmodel.VehicleViewModel

/** Hyundai seat codes: 2 = off, 6/7/8 = heat low/med/high, 3/4/5 = vent low/med/high. */
private const val SEAT_OFF = 2

private fun seatLabel(level: Int): String = when (level) {
    6 -> "Heat low"; 7 -> "Heat med"; 8 -> "Heat high"
    3 -> "Vent low"; 4 -> "Vent med"; 5 -> "Vent high"
    else -> "Off"
}

@Composable
fun ClimateScreen(vehicleViewModel: VehicleViewModel) {
    val c = DeckTheme.colors
    val context = LocalContext.current
    val vehicle by vehicleViewModel.selectedVehicle.collectAsStateWithLifecycle()
    val status by vehicleViewModel.vehicleStatus.collectAsStateWithLifecycle()
    val command by vehicleViewModel.commandState.collectAsStateWithLifecycle()
    val region by vehicleViewModel.region.collectAsStateWithLifecycle()
    val tempUnit by vehicleViewModel.temperatureUnit.collectAsStateWithLifecycle()
    val saved by vehicleViewModel.remoteStartSettings.collectAsStateWithLifecycle()
    val pending by vehicleViewModel.climateAwaitingConfirmation.collectAsStateWithLifecycle()
    val loading by vehicleViewModel.isStatusLoading.collectAsStateWithLifecycle()

    val caps = remember(vehicle, region) { vehicle?.resolveCapabilities(region) }
    val running = status?.airCtrlOn == true && !pending
    val busy = command.status == CommandStatus.LOADING || command.status == CommandStatus.ACCEPTED ||
        command.status == CommandStatus.REFRESHING

    // Working copy of the climate request; committed to the ViewModel when started.
    var settings by remember(saved.tempF, saved.durationMinutes) { mutableStateOf(saved) }
    var presets by remember(tempUnit) { mutableStateOf(ClimatePresetsStore.load(context, tempUnit)) }
    var confirm by remember { mutableStateOf<String?>(null) }
    var savingSlot by remember { mutableStateOf<Int?>(null) }
    var showSeats by remember { mutableStateOf(false) }

    val unitSymbol = if (tempUnit.equals("C", true)) "°C" else "°F"
    val tempRange = climateSliderRange(tempUnit)
    val displayTemp = climateDisplayValueFromF(settings.tempF, tempUnit)

    fun setDisplayTemp(v: Int) {
        val clamped = v.coerceIn(tempRange.start.toInt(), tempRange.endInclusive.toInt())
        settings = settings.copy(tempF = climateFahrenheitFromDisplay(clamped, tempUnit).toString())
    }

    fun applyPreset(p: ClimatePreset) {
        settings = settings.copy(
            tempF = p.tempF,
            defrost = p.defrost,
            heatedSteering = p.heatedSteering,
            durationMinutes = p.durationMinutes,
            driverSeatHeat = p.driverSeat,
            passengerSeatHeat = p.passengerSeat,
            rearLeftSeatHeat = p.rearLeftSeat,
            rearRightSeatHeat = p.rearRightSeat
        )
    }

    fun start() {
        vehicleViewModel.updateRemoteStartSettings(settings)
        val s = settings
        if (status?.doorsLocked == false) {
            vehicleViewModel.lockThenStartClimate(
                tempF = s.tempF, defrost = s.defrost, heatedSteering = s.heatedSteering,
                driverSeat = s.driverSeatHeat, passengerSeat = s.passengerSeatHeat,
                rearLeftSeat = s.rearLeftSeatHeat, rearRightSeat = s.rearRightSeatHeat,
                durationMinutes = s.durationMinutes
            )
        } else {
            vehicleViewModel.startClimate(
                tempF = s.tempF, defrost = s.defrost, heatedSteering = s.heatedSteering,
                driverSeat = s.driverSeatHeat, passengerSeat = s.passengerSeatHeat,
                rearLeftSeat = s.rearLeftSeatHeat, rearRightSeat = s.rearRightSeatHeat,
                durationMinutes = s.durationMinutes
            )
        }
    }

    when (confirm) {
        "start" -> DeckConfirmDialog(
            title = if (status?.doorsLocked == false) "Lock and start climate?" else "Start climate?",
            message = buildString {
                if (status?.doorsLocked == false) append("Your car is unlocked. Bluelink needs it locked, so it will lock first.\n\n")
                append("${climateTemperatureLabelFromF(settings.tempF, tempUnit)} for ${settings.durationMinutes} min")
                if (settings.defrost) append(", defrost")
                if (settings.heatedSteering) append(", heated wheel")
                val seats = listOf(
                    "Driver" to settings.driverSeatHeat, "Passenger" to settings.passengerSeatHeat,
                    "Rear left" to settings.rearLeftSeatHeat, "Rear right" to settings.rearRightSeatHeat
                ).filter { it.second != SEAT_OFF && it.second != 0 }
                if (seats.isNotEmpty()) append("\nSeats: " + seats.joinToString { "${it.first} ${seatLabel(it.second).lowercase()}" })
            },
            confirmLabel = if (status?.doorsLocked == false) "Lock & start" else "Start",
            onConfirm = { start() },
            onDismiss = { confirm = null }
        )
        "stop" -> DeckConfirmDialog(
            "Stop climate?", "The cabin will stop heating or cooling.", "Stop",
            onConfirm = { vehicleViewModel.stopClimate() },
            onDismiss = { confirm = null }
        )
    }

    savingSlot?.let { slot ->
        var name by remember(slot) { mutableStateOf(presets[slot].name) }
        AlertDialog(
            onDismissRequest = { savingSlot = null },
            containerColor = c.cardRaised,
            title = { Text("Save as preset", color = c.text) },
            text = {
                Column {
                    Text("Replaces \"${presets[slot].name}\" with the current settings.", color = c.textMuted, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = name, onValueChange = { name = it.take(20) },
                        singleLine = true, label = { Text("Name") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = c.accent, focusedLabelColor = c.accent, cursorColor = c.accent,
                            focusedTextColor = c.text, unfocusedTextColor = c.text
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val updated = presets.toMutableList()
                    updated[slot] = ClimatePreset(
                        name = name.trim().ifBlank { "Preset ${slot + 1}" },
                        tempF = settings.tempF,
                        defrost = settings.defrost,
                        heatedSteering = settings.heatedSteering,
                        durationMinutes = settings.durationMinutes,
                        driverSeat = settings.driverSeatHeat,
                        passengerSeat = settings.passengerSeatHeat,
                        rearLeftSeat = settings.rearLeftSeatHeat,
                        rearRightSeat = settings.rearRightSeatHeat
                    )
                    ClimatePresetsStore.save(context, updated)
                    presets = updated
                    savingSlot = null
                }) { Text("Save", color = c.accent) }
            },
            dismissButton = { TextButton(onClick = { savingSlot = null }) { Text("Cancel", color = c.textMuted) } }
        )
    }

    Box(Modifier.fillMaxSize().background(c.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            DeckHeader(
                title = "Climate Control",
                subtitle = when {
                    pending -> "Starting…"
                    running -> "On" + (status?.airTemp?.let { " · " + apiTemperatureToPreferredLabel(it.value, it.unit, tempUnit) + unitSymbol } ?: "")
                    loading -> "Updating…"
                    else -> "Off"
                }
            )

            // ── Car with airflow ─────────────────────────────────────────────────
            CarTopView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .padding(horizontal = ScreenPadding),
                horizontal = true,
                airflowOn = running,
                contentDescription = if (running) "Climate running" else "Climate off"
            )

            // ── Dials (concept's driver / passenger pair → temperature / duration) ─
            DeckCard(Modifier.padding(horizontal = ScreenPadding, vertical = 8.dp).fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DialColumn(
                        title = "Temperature",
                        valueText = "$displayTemp$unitSymbol",
                        value = displayTemp.toFloat(),
                        range = tempRange,
                        active = running || pending,
                        onChange = { setDisplayTemp(it.toInt()) },
                        onMinus = { setDisplayTemp(displayTemp - 1) },
                        onPlus = { setDisplayTemp(displayTemp + 1) },
                        modifier = Modifier.weight(1f)
                    )
                    DialColumn(
                        title = "Run time",
                        valueText = "${settings.durationMinutes} min",
                        value = settings.durationMinutes.toFloat(),
                        range = MIN_CLIMATE_DURATION_MINUTES.toFloat()..MAX_CLIMATE_DURATION_MINUTES.toFloat(),
                        active = running || pending,
                        onChange = { settings = settings.copy(durationMinutes = it.toInt()) },
                        onMinus = { settings = settings.copy(durationMinutes = (settings.durationMinutes - 1).coerceAtLeast(MIN_CLIMATE_DURATION_MINUTES)) },
                        onPlus = { settings = settings.copy(durationMinutes = (settings.durationMinutes + 1).coerceAtMost(MAX_CLIMATE_DURATION_MINUTES)) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(16.dp))
                // Toggle row, as in the concept (Auto / Defroster / Seat Heat)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionTile(
                        Icons.Filled.AcUnit, "Defrost",
                        onClick = { settings = settings.copy(defrost = !settings.defrost) },
                        state = if (settings.defrost) TileState.ACTIVE else TileState.IDLE,
                        stateDescription = if (settings.defrost) "On" else "Off",
                        modifier = Modifier.weight(1f)
                    )
                    if (caps?.showHeatedSteering != false) {
                        ActionTile(
                            Icons.Filled.Whatshot, "Wheel",
                            onClick = { settings = settings.copy(heatedSteering = !settings.heatedSteering) },
                            state = if (settings.heatedSteering) TileState.ACTIVE else TileState.IDLE,
                            stateDescription = if (settings.heatedSteering) "On" else "Off",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (caps?.showSeatClimateControls != false) {
                        val anySeat = listOf(settings.driverSeatHeat, settings.passengerSeatHeat, settings.rearLeftSeatHeat, settings.rearRightSeatHeat)
                            .any { it != SEAT_OFF && it != 0 }
                        ActionTile(
                            Icons.Filled.AirlineSeatReclineNormal, "Seats",
                            onClick = { showSeats = !showSeats },
                            state = if (anySeat) TileState.ACTIVE else TileState.IDLE,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                AnimatedVisibility(showSeats && caps != null) {
                    if (caps != null) {
                        Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            SeatSelector("Driver", caps.driver, settings.driverSeatHeat) { settings = settings.copy(driverSeatHeat = it) }
                            SeatSelector("Passenger", caps.passenger, settings.passengerSeatHeat) { settings = settings.copy(passengerSeatHeat = it) }
                            SeatSelector("Rear left", caps.rearLeft, settings.rearLeftSeatHeat) { settings = settings.copy(rearLeftSeatHeat = it) }
                            SeatSelector("Rear right", caps.rearRight, settings.rearRightSeatHeat) { settings = settings.copy(rearRightSeatHeat = it) }
                        }
                    }
                }
            }

            // ── Start / stop ─────────────────────────────────────────────────────
            Column(Modifier.padding(horizontal = ScreenPadding, vertical = 6.dp)) {
                if (running || pending) {
                    PrimaryAction(
                        if (pending) "Waiting for car…" else "Stop climate",
                        { confirm = "stop" },
                        icon = Icons.Filled.PowerSettingsNew,
                        busy = busy || pending,
                        destructive = true
                    )
                    if (running) {
                        Spacer(Modifier.height(10.dp))
                        PrimaryAction("Send new settings", { confirm = "start" }, icon = Icons.Filled.Thermostat, enabled = !busy)
                    }
                } else {
                    PrimaryAction("Start climate", { confirm = "start" }, icon = Icons.Filled.PowerSettingsNew, busy = busy, enabled = vehicle != null)
                }
            }

            // ── Presets ──────────────────────────────────────────────────────────
            SectionTitle("Presets", Modifier.padding(horizontal = ScreenPadding).padding(top = 12.dp))
            Text(
                "Tap to load. Long-press a preset to save the settings above into it.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textFaint,
                modifier = Modifier.padding(horizontal = ScreenPadding + 4.dp)
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.padding(horizontal = ScreenPadding), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                presets.forEachIndexed { index, preset ->
                    PresetTile(
                        preset = preset,
                        tempUnit = tempUnit,
                        onClick = {
                            if (preset.stopsClimate) {
                                if (running || pending) confirm = "stop"
                            } else applyPreset(preset)
                        },
                        onLongClick = if (preset.stopsClimate) null else ({ savingSlot = index }),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (caps?.showSeatClimateControls == false && caps.showHeatedSteering.not()) {
                Text(
                    "Your car didn't report heated or ventilated seats, so seat controls are hidden.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textFaint,
                    modifier = Modifier.padding(ScreenPadding)
                )
            }
        }
        CommandFeedback(command, Modifier.align(Alignment.TopCenter).padding(top = 56.dp))
    }
}

@Composable
private fun DialColumn(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    active: Boolean,
    onChange: (Float) -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = DeckTheme.colors
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = c.textMuted)
        Spacer(Modifier.height(6.dp))
        ArcDial(
            value = value, range = range, step = 1f, onChange = onChange, active = active,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .semantics { contentDescription = "$title $valueText" }
        ) {
            Text(valueText, style = MaterialTheme.typography.headlineSmall, color = c.text, textAlign = TextAlign.Center)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CircleIconButton(Icons.Filled.Remove, "Decrease $title", onMinus)
            CircleIconButton(Icons.Filled.Add, "Increase $title", onPlus)
        }
    }
}

@Composable
private fun SeatSelector(label: String, caps: SeatClimateCapabilities, value: Int, onChange: (Int) -> Unit) {
    if (!caps.showHeat && !caps.showVent) return
    val c = DeckTheme.colors
    val options = buildList {
        add(SEAT_OFF to "Off")
        if (caps.showHeat) { add(6 to "Heat 1"); add(7 to "Heat 2"); add(8 to "Heat 3") }
        if (caps.ventCapableForSelector) { add(3 to "Vent 1"); add(4 to "Vent 2"); add(5 to "Vent 3") }
    }
    val selected = options.firstOrNull { it.first == value }?.first ?: SEAT_OFF
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.textMuted)
        Spacer(Modifier.height(6.dp))
        if (options.size <= 4) {
            DeckSegmented(options, selected, onChange)
        } else {
            DeckSegmented(options.take(4), if (selected in listOf(SEAT_OFF, 6, 7, 8)) selected else -1, onChange)
            Spacer(Modifier.height(6.dp))
            DeckSegmented(options.drop(4), if (selected in listOf(3, 4, 5)) selected else -1, onChange)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetTile(
    preset: ClimatePreset,
    tempUnit: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val c = DeckTheme.colors
    Column(
        modifier
            .softSurface(c, TileShape, c.cardRaised, elevation = 6.dp)
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick, onLongClickLabel = "Save current settings here")
            .padding(PaddingValues(horizontal = 12.dp, vertical = 12.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            when {
                preset.stopsClimate -> Icons.Filled.PowerSettingsNew
                onLongClick != null && preset.name.isBlank() -> Icons.Filled.Save
                else -> Icons.Filled.Thermostat
            },
            null, tint = if (preset.stopsClimate) c.textMuted else c.accent, modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(preset.name, style = MaterialTheme.typography.labelLarge, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            if (preset.stopsClimate) "Stop" else climateTemperatureLabelFromF(preset.tempF, tempUnit),
            style = MaterialTheme.typography.bodySmall, color = c.textFaint
        )
    }
}

