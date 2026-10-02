package com.bluedeck.ui.screens.deck

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bluedeck.data.models.VehicleStatusData
import com.bluedeck.ui.design.BatteryCell
import com.bluedeck.ui.design.CircleIconButton
import com.bluedeck.ui.design.CommandFeedback
import com.bluedeck.ui.design.DeckCard
import com.bluedeck.ui.design.DeckConfirmDialog
import com.bluedeck.ui.design.DeckHeader
import com.bluedeck.ui.design.EnergyFlowLine
import com.bluedeck.ui.design.PillTone
import com.bluedeck.ui.design.PrimaryAction
import com.bluedeck.ui.design.ScreenPadding
import com.bluedeck.ui.design.SectionTitle
import com.bluedeck.ui.design.StatCard
import com.bluedeck.ui.design.StatusPill
import com.bluedeck.ui.design.TileShape
import com.bluedeck.ui.design.clockLabel
import com.bluedeck.ui.design.relativeTimeLabel
import com.bluedeck.ui.theme.DeckTheme
import com.bluedeck.ui.theme.milesToDisplayDistance
import com.bluedeck.viewmodel.CommandStatus
import com.bluedeck.viewmodel.ObdViewModel
import com.bluedeck.viewmodel.VehicleViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargeScreen(
    vehicleViewModel: VehicleViewModel,
    obdViewModel: ObdViewModel = hiltViewModel()
) {
    val c = DeckTheme.colors
    val vehicle by vehicleViewModel.selectedVehicle.collectAsStateWithLifecycle()
    val status by vehicleViewModel.vehicleStatus.collectAsStateWithLifecycle()
    val loading by vehicleViewModel.isStatusLoading.collectAsStateWithLifecycle()
    val lastRefresh by vehicleViewModel.lastStatusRefresh.collectAsStateWithLifecycle()
    val command by vehicleViewModel.commandState.collectAsStateWithLifecycle()
    val history by vehicleViewModel.commandHistory.collectAsStateWithLifecycle()
    val distanceUnit by vehicleViewModel.distanceUnit.collectAsStateWithLifecycle()
    val obdSnapshot by obdViewModel.latestSnapshot.collectAsStateWithLifecycle()

    val ev = status.evSnapshot(vehicle)
    val busy = command.status == CommandStatus.LOADING || command.status == CommandStatus.ACCEPTED ||
        command.status == CommandStatus.REFRESHING
    val unit = if (distanceUnit.equals("KM", true)) "km" else "mi"
    fun dist(miles: Double) = milesToDisplayDistance(miles, distanceUnit).roundToInt().toString()

    var confirm by remember { mutableStateOf<String?>(null) }
    var pendingAction by remember { mutableStateOf<String?>(null) }

    // Limits: start from what the car reports, let the user stage changes, then apply.
    var acTarget by remember(ev.acTarget) { mutableIntStateOf(ev.acTarget ?: 80) }
    var dcTarget by remember(ev.dcTarget) { mutableIntStateOf(ev.dcTarget ?: 80) }
    val targetsChanged = (ev.acTarget != null && acTarget != ev.acTarget) || (ev.dcTarget != null && dcTarget != ev.dcTarget) ||
        (ev.acTarget == null && ev.dcTarget == null)

    when (confirm) {
        "start" -> DeckConfirmDialog(
            "Start charging?", "Send the start charging command to your car.", "Start",
            onConfirm = { pendingAction = "charge"; vehicleViewModel.startCharging() },
            onDismiss = { confirm = null }
        )
        "stop" -> DeckConfirmDialog(
            "Stop charging?", "Charging will stop until you start it again or the schedule resumes.", "Stop",
            onConfirm = { pendingAction = "charge"; vehicleViewModel.stopCharging() },
            onDismiss = { confirm = null }
        )
        "targets" -> DeckConfirmDialog(
            "Set charge limits?", "Level 2 / home charging to $acTarget%, DC fast charging to $dcTarget%.", "Set limits",
            onConfirm = { pendingAction = "targets"; vehicleViewModel.setChargeTarget(acTarget, dcTarget) },
            onDismiss = { confirm = null }
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
                    title = "Charge",
                    subtitle = if (loading) "Updating…" else relativeTimeLabel(lastRefresh),
                    trailing = {
                        CircleIconButton(Icons.Filled.Refresh, "Refresh from car", { vehicleViewModel.refreshStatus(true) }, busy = loading)
                    }
                )

                if (status?.evStatus == null) {
                    DeckCard(Modifier.padding(ScreenPadding).fillMaxWidth()) {
                        Text(
                            if (loading) "Getting battery status…" else "Battery status unavailable. Pull down to refresh.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.textMuted
                        )
                    }
                    return@Column
                }

                // ── Battery cell ──────────────────────────────────────────────────
                Column(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BatteryCell(
                        percent = ev.percent ?: 0,
                        charging = ev.charging,
                        targetPercent = ev.activeTarget,
                        modifier = Modifier.width(132.dp).height(250.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    when {
                        ev.charging -> StatusPill(
                            buildString {
                                append(ev.methodLabel)
                                ev.powerKw?.let { append("  ${"%.1f".format(it)} kW") }
                            },
                            PillTone.GOOD, Icons.Filled.Bolt
                        )
                        ev.plugged -> StatusPill(ev.plugLabel, PillTone.NEUTRAL, Icons.Filled.Power)
                        else -> StatusPill("Not plugged in", PillTone.NEUTRAL)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Time + finish / plug ─────────────────────────────────────────
                Row(Modifier.padding(horizontal = ScreenPadding), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val target = ev.activeTarget
                    if (ev.charging && ev.remainingMinutes != null) {
                        StatCard(
                            label = "Time to ${target ?: 100}%",
                            value = formatDuration(ev.remainingMinutes),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Finishes",
                            value = ev.finishAtMillis()?.let { clockLabel(it) } ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        StatCard(
                            label = "Charge limit",
                            value = target?.toString() ?: "—",
                            unit = if (target != null) "%" else null,
                            modifier = Modifier.weight(1f)
                        )
                        val soh = obdSnapshot.tractionSohPercent
                        if (soh != null) {
                            StatCard(
                                label = "Battery health",
                                value = "%.0f".format(soh),
                                unit = "%",
                                footnote = "From OBD scanner",
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            StatCard(
                                label = "Charge port",
                                value = when (ev.chargePortOpen) { true -> "Open"; false -> "Closed"; null -> "—" },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Range now / at limit ─────────────────────────────────────────
                DeckCard(Modifier.padding(horizontal = ScreenPadding).fillMaxWidth()) {
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text("Range now", style = MaterialTheme.typography.labelMedium, color = c.textMuted)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(ev.rangeMiles?.let { dist(it) } ?: "—", style = MaterialTheme.typography.headlineMedium, color = c.text)
                                Spacer(Modifier.width(3.dp))
                                Text(unit, style = MaterialTheme.typography.bodyMedium, color = c.textMuted, modifier = Modifier.padding(bottom = 4.dp))
                            }
                        }
                        val limit = ev.activeTarget ?: 100
                        val est = ev.estimatedRangeAt(limit)
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text("At $limit% (est.)", style = MaterialTheme.typography.labelMedium, color = c.textMuted)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(est?.let { dist(it) } ?: "—", style = MaterialTheme.typography.headlineMedium, color = c.accent)
                                Spacer(Modifier.width(3.dp))
                                Text(unit, style = MaterialTheme.typography.bodyMedium, color = c.textMuted, modifier = Modifier.padding(bottom = 4.dp))
                            }
                        }
                    }
                }

                // ── Energy flow + start/stop ─────────────────────────────────────
                SectionTitle("Energy flow", Modifier.padding(horizontal = ScreenPadding).padding(top = 14.dp))
                DeckCard(Modifier.padding(horizontal = ScreenPadding).fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.EvStation, "Charger", tint = if (ev.plugged) c.accent else c.textFaint, modifier = Modifier.size(28.dp))
                        EnergyFlowLine(ev.charging, Modifier.weight(1f).height(24.dp).padding(horizontal = 12.dp))
                        Icon(Icons.Filled.DirectionsCar, "Car", tint = if (ev.charging) c.accent else c.textMuted, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        when {
                            ev.charging -> ev.methodLabel + (ev.powerKw?.let { " at ${"%.1f".format(it)} kW" } ?: "")
                            ev.plugged -> "Plugged in, not charging"
                            else -> "Plug in to charge"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textMuted
                    )
                    Spacer(Modifier.height(14.dp))
                    if (ev.charging) {
                        PrimaryAction("Stop charging", { confirm = "stop" }, icon = Icons.Filled.Stop,
                            busy = busy && pendingAction == "charge", enabled = !busy, destructive = true)
                    } else {
                        PrimaryAction("Start charging", { confirm = "start" }, icon = Icons.Filled.PlayArrow,
                            busy = busy && pendingAction == "charge", enabled = !busy && ev.plugged)
                    }
                }

                // ── Charge limits ────────────────────────────────────────────────
                SectionTitle("Charge limits", Modifier.padding(horizontal = ScreenPadding).padding(top = 14.dp))
                DeckCard(Modifier.padding(horizontal = ScreenPadding).fillMaxWidth()) {
                    LimitSlider("Level 2 / home (AC)", acTarget) { acTarget = it }
                    Spacer(Modifier.height(14.dp))
                    LimitSlider("DC fast", dcTarget) { dcTarget = it }
                    Spacer(Modifier.height(14.dp))
                    PrimaryAction(
                        "Set limits", { confirm = "targets" },
                        busy = busy && pendingAction == "targets",
                        enabled = !busy && targetsChanged
                    )
                }

                // ── Schedule ─────────────────────────────────────────────────────
                ChargeScheduleCard(
                    status = status,
                    busy = busy && pendingAction == "schedule",
                    enabled = !busy,
                    onSave = { start, end, opStart, opEnd, opOnly ->
                        pendingAction = "schedule"
                        vehicleViewModel.setChargingSchedule(start, end, opStart, opEnd, opOnly)
                    }
                )

                // ── Recent charging commands ─────────────────────────────────────
                val chargeHistory = history.filter { it.title.contains("charg", true) }.take(4)
                if (chargeHistory.isNotEmpty()) {
                    SectionTitle("Recent", Modifier.padding(horizontal = ScreenPadding).padding(top = 14.dp))
                    DeckCard(Modifier.padding(horizontal = ScreenPadding).fillMaxWidth()) {
                        chargeHistory.forEachIndexed { i, e ->
                            if (i > 0) Spacer(Modifier.height(12.dp))
                            HistoryRow(e)
                        }
                    }
                }
            }
        }
        CommandFeedback(command, Modifier.align(Alignment.TopCenter).padding(top = 56.dp))
    }
}

@Composable
private fun LimitSlider(label: String, value: Int, onChange: (Int) -> Unit) {
    val c = DeckTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = c.text, modifier = Modifier.weight(1f))
        Text("$value%", style = MaterialTheme.typography.titleMedium, color = c.accent)
    }
    // Bluelink accepts 50–100 % in 10 % steps.
    Slider(
        value = value.toFloat(),
        onValueChange = { onChange(((it / 10f).roundToInt() * 10).coerceIn(50, 100)) },
        valueRange = 50f..100f,
        steps = 4,
        colors = SliderDefaults.colors(
            thumbColor = c.accent,
            activeTrackColor = c.accent,
            inactiveTrackColor = c.hairline,
            activeTickColor = c.onAccent.copy(alpha = 0.5f),
            inactiveTickColor = c.textFaint
        )
    )
}

private fun parseHhmm(raw: String?): Pair<Int, Int>? {
    val digits = raw.orEmpty().filter { it.isDigit() }
    if (digits.isEmpty()) return null
    val padded = if (digits.length >= 4) digits.takeLast(4) else digits.padStart(4, '0')
    val h = padded.take(2).toIntOrNull() ?: return null
    val m = padded.takeLast(2).toIntOrNull() ?: return null
    return if (h in 0..23 && m in 0..59) h to m else null
}

private fun hhmm(t: Pair<Int, Int>) = "%02d%02d".format(t.first, t.second)

@Composable
private fun timeText(t: Pair<Int, Int>): String {
    val cal = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, t.first)
        set(java.util.Calendar.MINUTE, t.second)
    }
    val fmt = android.text.format.DateFormat.getTimeFormat(LocalContext.current)
    return fmt.format(cal.time)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChargeScheduleCard(
    status: VehicleStatusData?,
    busy: Boolean,
    enabled: Boolean,
    onSave: (String, String, String, String, Boolean) -> Unit
) {
    val c = DeckTheme.colors
    val reserv = status?.evStatus?.reservChargeInfos
    val reportedStart = parseHhmm(reserv?.chargeWindow?.start?.time?.time ?: reserv?.reservChargeInfo?.reservInfo?.time?.time)
    val reportedEnd = parseHhmm(reserv?.chargeWindow?.end?.time?.time ?: reserv?.reserveChargeInfo2?.reservInfo?.time?.time)
    val reportedOpStart = parseHhmm(reserv?.offPeakPowerInfo?.offPeakPowerTime1?.startTime?.time)
    val reportedOpEnd = parseHhmm(reserv?.offPeakPowerInfo?.offPeakPowerTime1?.endTime?.time)
    val reportedOpOnly = reserv?.offPeakPowerInfo?.offPeakPowerFlag == 1

    var start by remember(reportedStart) { mutableStateOf(reportedStart ?: (23 to 0)) }
    var end by remember(reportedEnd) { mutableStateOf(reportedEnd ?: (6 to 0)) }
    var opStart by remember(reportedOpStart) { mutableStateOf(reportedOpStart ?: (23 to 0)) }
    var opEnd by remember(reportedOpEnd) { mutableStateOf(reportedOpEnd ?: (7 to 0)) }
    var opOnly by remember(reportedOpOnly) { mutableStateOf(reportedOpOnly) }
    var editing by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }

    editing?.let { which ->
        val initial = when (which) { "start" -> start; "end" -> end; "opStart" -> opStart; else -> opEnd }
        val is24 = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
        val state = rememberTimePickerState(initial.first, initial.second, is24Hour = is24)
        AlertDialog(
            onDismissRequest = { editing = null },
            containerColor = c.cardRaised,
            title = {
                Text(
                    when (which) { "start" -> "Charge start"; "end" -> "Charge end"; "opStart" -> "Off-peak starts"; else -> "Off-peak ends" },
                    color = c.text
                )
            },
            text = {
                TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = c.card,
                        selectorColor = c.accent,
                        timeSelectorSelectedContainerColor = c.accentSoft,
                        timeSelectorSelectedContentColor = c.accent,
                        timeSelectorUnselectedContainerColor = c.card,
                        timeSelectorUnselectedContentColor = c.text,
                        periodSelectorSelectedContainerColor = c.accentSoft,
                        periodSelectorSelectedContentColor = c.accent
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = state.hour to state.minute
                    when (which) { "start" -> start = v; "end" -> end = v; "opStart" -> opStart = v; else -> opEnd = v }
                    editing = null
                }) { Text("Done", color = c.accent) }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel", color = c.textMuted) } }
        )
    }

    SectionTitle("Schedule", Modifier.padding(horizontal = ScreenPadding).padding(top = 14.dp))
    DeckCard(Modifier.padding(horizontal = ScreenPadding).fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Schedule, null, tint = c.textMuted, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (reportedStart != null) "Charge from ${timeText(reportedStart)}" else "No schedule reported",
                    style = MaterialTheme.typography.bodyMedium, color = c.text
                )
                if (reportedOpOnly && reportedOpStart != null && reportedOpEnd != null) {
                    Text("Off-peak only, ${timeText(reportedOpStart)}–${timeText(reportedOpEnd)}", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                }
            }
            Text(if (expanded) "Close" else "Edit", style = MaterialTheme.typography.labelLarge, color = c.accent)
        }
        AnimatedVisibility(expanded) {
            Column {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TimeChip("Start", timeText(start), Modifier.weight(1f)) { editing = "start" }
                    TimeChip("End", timeText(end), Modifier.weight(1f)) { editing = "end" }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Off-peak only", style = MaterialTheme.typography.bodyMedium, color = c.text)
                        Text("Charge only during your utility's cheaper hours", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                    }
                    Switch(
                        checked = opOnly, onCheckedChange = { opOnly = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = c.accent, checkedThumbColor = c.onAccent)
                    )
                }
                AnimatedVisibility(opOnly) {
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TimeChip("Off-peak from", timeText(opStart), Modifier.weight(1f)) { editing = "opStart" }
                        TimeChip("Until", timeText(opEnd), Modifier.weight(1f)) { editing = "opEnd" }
                    }
                }
                Spacer(Modifier.height(14.dp))
                PrimaryAction(
                    "Save schedule",
                    { onSave(hhmm(start), hhmm(end), hhmm(opStart), hhmm(opEnd), opOnly) },
                    busy = busy, enabled = enabled
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "The schedule is confirmed only after the car reports it back.",
                    style = MaterialTheme.typography.bodySmall, color = c.textFaint
                )
            }
        }
    }
}

@Composable
private fun TimeChip(label: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = DeckTheme.colors
    Column(
        modifier
            .background(c.cardPressed, TileShape)
            .clickable(onClick = onClick)
            .padding(PaddingValues(horizontal = 14.dp, vertical = 10.dp))
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = c.textMuted)
        Text(value, style = MaterialTheme.typography.titleMedium, color = c.text)
    }
}
