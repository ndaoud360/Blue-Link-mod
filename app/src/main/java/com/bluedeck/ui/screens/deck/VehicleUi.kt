package com.bluedeck.ui.screens.deck

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.TireRepair
import androidx.compose.material.icons.filled.Window
import androidx.compose.ui.graphics.vector.ImageVector
import com.bluedeck.data.models.Vehicle
import com.bluedeck.data.models.VehicleStatusData
import com.bluedeck.data.models.totalRangeMilesFor
import com.bluedeck.ui.design.CarOpenings
import com.bluedeck.ui.design.PillTone

/** Everything the EV and Charge tabs need, derived once from the raw Bluelink status. */
data class EvSnapshot(
    val percent: Int?,
    val charging: Boolean,
    val plugged: Boolean,
    val plugLabel: String,
    val methodLabel: String,
    val rangeMiles: Double?,
    val acTarget: Int?,
    val dcTarget: Int?,
    val remainingMinutes: Int?,
    val powerKw: Double?,
    val chargePortOpen: Boolean?
) {
    /** Limit that applies to the current session: DC when on a fast charger, otherwise AC. */
    val activeTarget: Int?
        get() = if (methodLabel.startsWith("DC")) dcTarget ?: acTarget else acTarget ?: dcTarget

    /**
     * Linear estimate of range at [targetPercent] from current range per percent. It is an
     * estimate and is always labelled as such in the UI.
     */
    fun estimatedRangeAt(targetPercent: Int): Double? {
        val p = percent ?: return null
        val r = rangeMiles ?: return null
        if (p < 5) return null
        return r / p * targetPercent
    }

    /** Wall-clock finish time, when Bluelink reports time remaining. */
    fun finishAtMillis(now: Long = System.currentTimeMillis()): Long? =
        remainingMinutes?.takeIf { charging && it > 0 }?.let { now + it * 60_000L }
}

fun VehicleStatusData?.evSnapshot(vehicle: Vehicle?): EvSnapshot {
    val ev = this?.evStatus
    val remaining: Int? = ev?.let { e ->
        e.remainChargeTime.firstOrNull()?.time?.takeIf { it.value > 0 }?.let { t ->
            if (t.unit == 1) t.value else t.value * 60
        } ?: e.remainTime2?.currentCharge?.takeIf { it.value > 0 && it.unit == 1 }?.value
    }
    return EvSnapshot(
        percent = ev?.batteryStatus?.takeIf { it in 1..100 },
        charging = ev?.batteryCharge == true,
        plugged = ev != null && ev.batteryPlugin != 0,
        plugLabel = ev?.plugStatusLabel ?: "Unknown",
        methodLabel = ev?.chargingMethodLabel ?: "Not charging",
        rangeMiles = this?.totalRangeMilesFor(vehicle)?.takeIf { it > 0.0 },
        acTarget = ev?.acChargeTarget,
        dcTarget = ev?.dcChargeTarget,
        remainingMinutes = remaining,
        powerKw = ev?.chargingPowerKw?.takeIf { it > 0.0 },
        chargePortOpen = ev?.chargePortDoorOpen?.takeIf { it == 0 || it == 1 }?.let { it == 1 }
    )
}

fun VehicleStatusData?.openings(): CarOpenings {
    val d = this?.doorOpenStatus
    return CarOpenings(
        frontLeft = d?.frontLeft == 1,
        frontRight = d?.frontRight == 1,
        rearLeft = d?.backLeft == 1,
        rearRight = d?.backRight == 1,
        trunk = this?.trunkOpenStatus == true,
        hood = this?.hoodOpenStatus == true
    )
}

data class VehicleWarning(
    val icon: ImageVector,
    val title: String,
    val tone: PillTone = PillTone.CAUTION
)

/** Only conditions the vehicle actually reported; nothing is inferred. */
fun VehicleStatusData?.warnings(): List<VehicleWarning> {
    val s = this ?: return emptyList()
    return buildList {
        val open = s.openings()
        if (open.frontLeft || open.frontRight || open.rearLeft || open.rearRight) {
            add(VehicleWarning(Icons.Filled.DoorFront, "A door is open"))
        }
        if (open.trunk) add(VehicleWarning(Icons.Filled.DoorFront, "Trunk is open"))
        if (open.hood) add(VehicleWarning(Icons.Filled.DoorFront, "Hood is open"))
        if (s.windowOpenStatus?.anyOpen == true) add(VehicleWarning(Icons.Filled.Window, "A window is open"))
        if (s.tirePressureLamp?.anyLow == true) add(VehicleWarning(Icons.Filled.TireRepair, "Tire pressure low", PillTone.DANGER))
        val aux = s.battery?.batteryLevel ?: 0
        val auxWarn = s.battery?.batSignalReferenceValue?.warningThreshold?.takeIf { it > 0 }
        if (aux in 1..100 && (auxWarn?.let { aux <= it } ?: (aux < 50))) {
            add(VehicleWarning(Icons.Filled.BatteryAlert, "12V battery low ($aux%)", PillTone.DANGER))
        }
        if (s.smartKeyBatteryWarning) add(VehicleWarning(Icons.Filled.Key, "Key fob battery low"))
        if (s.washerFluidStatus) add(VehicleWarning(Icons.Filled.Opacity, "Washer fluid low"))
    }
}

fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "${m}m"
        m == 0 -> "${h}h"
        else -> "${h}h ${m}m"
    }
}
