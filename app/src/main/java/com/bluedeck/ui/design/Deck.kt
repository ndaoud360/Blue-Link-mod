package com.bluedeck.ui.design

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bluedeck.ui.theme.DeckColors
import com.bluedeck.ui.theme.DeckTheme
import com.bluedeck.viewmodel.CommandState
import com.bluedeck.viewmodel.CommandStatus
import java.text.DateFormat
import java.util.Date

val CardShape = RoundedCornerShape(22.dp)
val TileShape = RoundedCornerShape(16.dp)
val ScreenPadding = 18.dp

/** Soft raised surface: light mode gets a diffuse shadow, dark mode a top-lit hairline. */
fun Modifier.softSurface(
    colors: DeckColors,
    shape: Shape = CardShape,
    fill: Color = colors.card,
    elevation: Dp = 10.dp
): Modifier {
    val base = if (colors.isDark) this else this.shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = colors.shadow,
        spotColor = colors.shadow
    )
    return base
        .clip(shape)
        .background(fill)
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(listOf(colors.highlight, colors.hairline.copy(alpha = 0.35f))),
            shape = shape
        )
}

@Composable
fun DeckCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val c = DeckTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, tween(120), label = "cardPress")
    Column(
        modifier = modifier
            .scale(scale)
            .softSurface(c)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClickLabel = onClickLabel,
                    onClick = onClick
                ) else Modifier
            )
            .padding(contentPadding),
        content = content
    )
}

/** Header modelled on the concept: round icon buttons either side of a centred title. */
@Composable
fun DeckHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val c = DeckTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = ScreenPadding, vertical = 6.dp)
    ) {
        Box(Modifier.align(Alignment.CenterStart)) {
            when {
                onBack != null -> CircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
                leading != null -> leading()
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Box(Modifier.align(Alignment.CenterEnd)) { trailing?.invoke() }
    }
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    busy: Boolean = false,
    enabled: Boolean = true
) {
    val c = DeckTheme.colors
    Box(
        modifier = modifier
            .size(40.dp)
            .softSurface(c, CircleShape, c.cardRaised, elevation = 6.dp)
            .clickable(enabled = enabled && !busy, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(16.dp), color = c.accent, strokeWidth = 2.dp)
        } else {
            Icon(icon, null, tint = c.text.copy(alpha = if (enabled) 0.85f else 0.35f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = DeckTheme.colors.textMuted,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

/** Small label + large value, with an optional slim progress track — the concept's Range / Battery cards. */
@Composable
fun StatCard(
    label: String,
    value: String,
    unit: String? = null,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    progressColor: Color = DeckTheme.colors.accent,
    footnote: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val c = DeckTheme.colors
    DeckCard(modifier = modifier, onClick = onClick, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = c.textMuted)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(value, style = MaterialTheme.typography.headlineMedium, color = c.text)
                    if (unit != null) {
                        Spacer(Modifier.width(3.dp))
                        Text(
                            unit,
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.textMuted,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
            trailing?.invoke()
        }
        if (progress != null) {
            Spacer(Modifier.height(10.dp))
            SlimProgress(progress, progressColor)
        }
        if (footnote != null) {
            Spacer(Modifier.height(8.dp))
            Text(footnote, style = MaterialTheme.typography.bodySmall, color = c.textFaint)
        }
    }
}

@Composable
fun SlimProgress(progress: Float, color: Color = DeckTheme.colors.accent, modifier: Modifier = Modifier) {
    val c = DeckTheme.colors
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700), label = "slimProgress")
    Box(
        modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape)
            .background(c.hairline)
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(4.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

enum class TileState { IDLE, ACTIVE, BUSY, ALERT }

/**
 * Square-ish action tile (icon over label). ACTIVE renders "pressed in" with the accent,
 * as in the concept's selected "Auto" button.
 */
@Composable
fun ActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: TileState = TileState.IDLE,
    enabled: Boolean = true,
    stateDescription: String? = null
) {
    val c = DeckTheme.colors
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, tween(110), label = "tilePress")
    val fill by animateColorAsState(
        when (state) {
            TileState.ACTIVE -> c.accentSoft
            TileState.ALERT -> c.cautionSoft
            else -> c.cardRaised
        }, tween(250), label = "tileFill"
    )
    val tint = when {
        !enabled -> c.textFaint
        state == TileState.ACTIVE -> c.accent
        state == TileState.ALERT -> c.caution
        else -> c.text
    }
    Column(
        modifier = modifier
            .scale(scale)
            .softSurface(c, TileShape, fill, elevation = if (state == TileState.ACTIVE) 0.dp else 6.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled && state != TileState.BUSY,
                role = Role.Button
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .semantics { if (stateDescription != null) this.stateDescription = stateDescription }
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            if (state == TileState.BUSY) {
                CircularProgressIndicator(Modifier.size(18.dp), color = c.accent, strokeWidth = 2.dp)
            } else {
                Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) c.textMuted else c.textFaint,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

enum class PillTone { NEUTRAL, GOOD, CAUTION, DANGER }

@Composable
fun StatusPill(text: String, tone: PillTone = PillTone.NEUTRAL, icon: ImageVector? = null, modifier: Modifier = Modifier) {
    val c = DeckTheme.colors
    val (bg, fg) = when (tone) {
        PillTone.GOOD -> c.accentSoft to c.accent
        PillTone.CAUTION -> c.cautionSoft to c.caution
        PillTone.DANGER -> c.dangerSoft to c.danger
        PillTone.NEUTRAL -> c.cardRaised to c.textMuted
    }
    Row(
        modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1)
    }
}

@Composable
fun <T> DeckSegmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = DeckTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(TileShape)
            .background(c.cardPressed)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            val fill by animateColorAsState(if (isSelected) c.cardRaised else Color.Transparent, label = "seg")
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(fill)
                    .clickable(role = Role.RadioButton) { onSelect(value) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) c.text else c.textMuted,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun PrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    busy: Boolean = false,
    enabled: Boolean = true,
    destructive: Boolean = false
) {
    val c = DeckTheme.colors
    val bg = when {
        !enabled -> c.cardPressed
        destructive -> c.cardRaised
        else -> c.accent
    }
    val fg = when {
        !enabled -> c.textFaint
        destructive -> c.danger
        else -> c.onAccent
    }
    val haptics = LocalHapticFeedback.current
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .softSurface(c, TileShape, bg, elevation = 4.dp)
            .clickable(enabled = enabled && !busy, role = Role.Button) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(18.dp), color = fg, strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
        } else if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

/** Floating command progress strip bound to VehicleViewModel.commandState. */
@Composable
fun CommandFeedback(state: CommandState, modifier: Modifier = Modifier) {
    val c = DeckTheme.colors
    AnimatedVisibility(
        visible = state.status != CommandStatus.IDLE,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier
    ) {
        val tone = when (state.status) {
            CommandStatus.ERROR -> c.danger
            CommandStatus.SUCCESS -> c.accent
            else -> c.text
        }
        Row(
            Modifier
                .padding(horizontal = ScreenPadding, vertical = 6.dp)
                .fillMaxWidth()
                .softSurface(c, TileShape, c.cardRaised, elevation = 12.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (state.status) {
                CommandStatus.SUCCESS -> Icon(Icons.Filled.CheckCircle, null, tint = c.accent, modifier = Modifier.size(18.dp))
                CommandStatus.ERROR -> Icon(Icons.Filled.ErrorOutline, null, tint = c.danger, modifier = Modifier.size(18.dp))
                else -> CircularProgressIndicator(Modifier.size(16.dp), color = c.accent, strokeWidth = 2.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(state.message, style = MaterialTheme.typography.labelLarge, color = tone, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val detail = friendlyCommandDetail(state)
                if (detail.isNotBlank()) {
                    Text(detail, style = MaterialTheme.typography.bodySmall, color = c.textMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** Keep raw transport errors (stack-ish text, HTTP codes) out of the primary UI. */
fun friendlyCommandDetail(state: CommandState): String {
    if (state.status != CommandStatus.ERROR) return state.detail
    return friendlyError(state.detail)
}

fun friendlyError(raw: String?): String {
    val text = raw.orEmpty()
    return when {
        text.isBlank() -> "Please try again."
        text.contains("Unable to resolve host", true) || text.contains("timeout", true) ||
            text.contains("failed to connect", true) || text.contains("Network error", true) ->
            "Unable to connect to Bluelink. Check your connection and try again."
        text.contains("401") || text.contains("Unauthorized", true) -> "Your Bluelink session expired. Sign in again."
        text.contains("429") || text.contains("Too many", true) -> "Bluelink is limiting requests. Wait a few minutes and try again."
        text.contains("PIN", true) -> "Bluelink PIN was rejected. Check it in Settings."
        text.contains("Exception", true) || text.contains("java.", true) -> "Command could not be completed. Please try again."
        text.length > 160 -> text.take(157) + "…"
        else -> text
    }
}

@Composable
fun DeckConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val c = DeckTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.cardRaised,
        titleContentColor = c.text,
        textContentColor = c.textMuted,
        shape = CardShape,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirmLabel, color = c.accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = c.textMuted) }
        }
    )
}

fun relativeTimeLabel(timestampMillis: Long, now: Long = System.currentTimeMillis()): String {
    if (timestampMillis <= 0L) return "Not updated yet"
    val diff = (now - timestampMillis).coerceAtLeast(0L)
    val minutes = diff / 60_000L
    return when {
        minutes < 1 -> "Updated just now"
        minutes < 60 -> "Updated $minutes min ago"
        minutes < 24 * 60 -> "Updated ${minutes / 60} h ago"
        else -> "Updated " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestampMillis))
    }
}

fun clockLabel(timestampMillis: Long): String =
    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(timestampMillis))
