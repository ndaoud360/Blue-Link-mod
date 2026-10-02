package com.bluedeck.ui.screens.deck

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bluedeck.ui.design.softSurface
import com.bluedeck.ui.screens.SettingsScreen
import com.bluedeck.ui.theme.DeckTheme
import com.bluedeck.viewmodel.VehicleViewModel

enum class DeckTab(val label: String, val icon: ImageVector) {
    EV("EV", Icons.Filled.DirectionsCar),
    CHARGE("Charge", Icons.Filled.BatteryChargingFull),
    CLIMATE("Climate", Icons.Filled.AcUnit),
    MAP("Map", Icons.Filled.Map),
    SETTINGS("Settings", Icons.Filled.Settings)
}

/**
 * Root of the signed-in app: five tabs over the shared [VehicleViewModel].
 * Detail screens (vehicle details, digital key, OBD) are pushed on the outer
 * NavHost in MainActivity, so system back returns here.
 */
@Composable
fun MainShell(
    vehicleViewModel: VehicleViewModel,
    onOpenDetails: () -> Unit,
    onOpenDigitalKey: () -> Unit,
    onOpenObd: () -> Unit,
    onLogout: () -> Unit
) {
    val c = DeckTheme.colors
    var tab by rememberSaveable { mutableStateOf(DeckTab.EV) }
    val stateHolder = rememberSaveableStateHolder()

    BackHandler(enabled = tab != DeckTab.EV) { tab = DeckTab.EV }

    Scaffold(
        containerColor = c.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { DeckTabBar(tab) { tab = it } }
    ) { inner ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .then(if (tab == DeckTab.SETTINGS) Modifier else Modifier.statusBarsPadding())
        ) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "tab"
            ) { current ->
                stateHolder.SaveableStateProvider(current.name) {
                    when (current) {
                        DeckTab.EV -> EvScreen(
                            vehicleViewModel = vehicleViewModel,
                            onOpenCharge = { tab = DeckTab.CHARGE },
                            onOpenClimate = { tab = DeckTab.CLIMATE },
                            onOpenDetails = onOpenDetails,
                            onOpenDigitalKey = onOpenDigitalKey
                        )
                        DeckTab.CHARGE -> ChargeScreen(vehicleViewModel)
                        DeckTab.CLIMATE -> ClimateScreen(vehicleViewModel)
                        DeckTab.MAP -> MapScreen(vehicleViewModel)
                        DeckTab.SETTINGS -> SettingsScreen(
                            onNavigateBack = null,
                            onLogout = onLogout,
                            onNavigateToObd = onOpenObd
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckTabBar(selected: DeckTab, onSelect: (DeckTab) -> Unit) {
    val c = DeckTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .background(c.background)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .softSurface(c, fill = c.card, elevation = 14.dp)
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            DeckTab.entries.forEach { t ->
                val isSel = t == selected
                val tint by animateColorAsState(if (isSel) c.accent else c.textFaint, tween(200), label = "tabTint")
                val pill by animateColorAsState(if (isSel) c.accentSoft else c.card.copy(alpha = 0f), tween(200), label = "tabPill")
                Column(
                    Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab
                        ) { onSelect(t) }
                        .semantics { this.selected = isSel }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(pill)
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(t.icon, null, tint = tint, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(t.label, style = MaterialTheme.typography.labelSmall, color = tint)
                }
            }
        }
    }
}
