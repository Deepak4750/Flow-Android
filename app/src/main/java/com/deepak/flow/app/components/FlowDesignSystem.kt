package com.deepak.flow.app.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.deepak.flow.R
import com.deepak.flow.app.theme.FlowAccent
import com.deepak.flow.app.theme.FlowBlack
import com.deepak.flow.app.theme.FlowBorder
import com.deepak.flow.app.theme.FlowBorderStrong
import com.deepak.flow.app.theme.FlowMotion
import com.deepak.flow.app.theme.FlowPressed
import com.deepak.flow.app.theme.FlowSizes
import com.deepak.flow.app.theme.FlowSpacing
import com.deepak.flow.app.theme.FlowSurface
import com.deepak.flow.app.theme.FlowSurfaceRaised
import com.deepak.flow.app.theme.FlowTextDisabled
import com.deepak.flow.app.theme.FlowTextPrimary
import com.deepak.flow.app.theme.FlowTextSecondary
import com.deepak.flow.app.theme.FlowTextTertiary
import com.deepak.flow.app.theme.FlowWhite

/**
 * Section header for a block of content. Uppercase mono at 9.4:1 contrast —
 * bright enough to read as a real label, not decoration.
 *
 * No heading semantics: this is also the label inside selector and info rows,
 * where announcing a heading would mislead screen readers.
 */
@Composable
fun FlowSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = FlowTextTertiary,
        modifier = modifier,
    )
}

/**
 * Compact inline metadata — one step smaller than [FlowSectionLabel].
 * Use for tags and supporting detail inside a row, never to title a section.
 */
@Composable
fun FlowMetaText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = FlowTextTertiary,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier,
    )
}

@Composable
fun FlowHairlineDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = FlowSizes.hairline,
        color = FlowBorder,
    )
}

@Composable
fun FlowTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = FlowSizes.touchTarget),
            textStyle = MaterialTheme.typography.titleLarge.copy(color = FlowTextPrimary),
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = SolidColor(FlowWhite),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.titleLarge,
                            color = FlowTextDisabled,
                        )
                    }
                    inner()
                }
            },
        )
        FlowHairlineDivider()
    }
}

enum class FlowButtonVariant { Primary, Secondary }

/**
 * Primary is a filled white block for the one committing action on a screen.
 * Secondary is hairline-outlined for anything optional.
 * Set [fillWidth] to false for a compact action that sizes to its label.
 */
@Composable
fun FlowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: FlowButtonVariant = FlowButtonVariant.Primary,
    fillWidth: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val background by animateColorAsState(
        targetValue = when (variant) {
            FlowButtonVariant.Primary -> when {
                !enabled -> FlowBorder
                pressed -> FlowPressed
                else -> FlowWhite
            }
            FlowButtonVariant.Secondary -> if (pressed && enabled) FlowSurfaceRaised else Color.Transparent
        },
        animationSpec = tween(FlowMotion.FAST),
        label = "buttonBackground",
    )
    val foreground = when (variant) {
        FlowButtonVariant.Primary -> if (enabled) FlowBlack else FlowTextDisabled
        FlowButtonVariant.Secondary -> if (enabled) FlowTextPrimary else FlowTextDisabled
    }
    val border = when (variant) {
        FlowButtonVariant.Primary -> Color.Transparent
        FlowButtonVariant.Secondary -> if (enabled) FlowBorderStrong else FlowBorder
    }

    Box(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .defaultMinSize(minHeight = FlowSizes.touchTarget)
            .clip(MaterialTheme.shapes.small)
            .background(background)
            .then(
                if (border == Color.Transparent) {
                    Modifier
                } else {
                    Modifier.border(FlowSizes.hairline, border, MaterialTheme.shapes.small)
                },
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            )
            .padding(horizontal = FlowSpacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = foreground,
            maxLines = 1,
        )
    }
}

/** Lowest-emphasis action: a tracked uppercase label with no container. */
@Composable
fun FlowTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    val color = when {
        !enabled -> FlowTextDisabled
        destructive -> MaterialTheme.colorScheme.error
        else -> FlowTextPrimary
    }
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            )
            .defaultMinSize(minHeight = FlowSizes.touchTarget)
            .padding(vertical = FlowSpacing.xs),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
fun FlowIconAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconSize: Dp = FlowSizes.iconMd,
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .size(FlowSizes.touchTarget)
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) FlowTextSecondary else FlowTextDisabled,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
fun FlowFab(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val background by animateColorAsState(
        targetValue = if (pressed) FlowPressed else FlowWhite,
        animationSpec = tween(FlowMotion.FAST),
        label = "fabBackground",
    )

    Box(
        modifier = modifier
            .size(FlowSizes.fab)
            .clip(CircleShape)
            .background(background)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = FlowBlack,
            modifier = Modifier.size(FlowSizes.iconLg),
        )
    }
}

/** Monochrome toggle. Hairline track, solid thumb — no Material pill. */
@Composable
fun FlowSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    val inset = FlowSpacing.xxs
    val thumbTravel = FlowSizes.switchTrackWidth - FlowSizes.switchThumb - inset

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) thumbTravel else inset,
        animationSpec = tween(FlowMotion.STANDARD),
        label = "switchThumbOffset",
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked && enabled) FlowWhite else Color.Transparent,
        animationSpec = tween(FlowMotion.STANDARD),
        label = "switchTrack",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            !enabled -> FlowBorder
            checked -> FlowWhite
            else -> FlowBorderStrong
        },
        animationSpec = tween(FlowMotion.STANDARD),
        label = "switchBorder",
    )
    val thumbColor by animateColorAsState(
        targetValue = when {
            !enabled -> FlowTextDisabled
            checked -> FlowBlack
            else -> FlowTextSecondary
        },
        animationSpec = tween(FlowMotion.STANDARD),
        label = "switchThumb",
    )

    Box(
        modifier = modifier
            .defaultMinSize(
                minWidth = FlowSizes.touchTarget,
                minHeight = FlowSizes.touchTarget,
            )
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCheckedChange(it)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = FlowSizes.switchTrackWidth,
                    height = FlowSizes.switchTrackHeight,
                )
                .clip(CircleShape)
                .background(trackColor)
                .border(FlowSizes.hairline, borderColor, CircleShape),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(FlowSizes.switchThumb)
                    .clip(CircleShape)
                    .background(thumbColor),
            )
        }
    }
}

@Composable
fun FlowChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val background by animateColorAsState(
        targetValue = if (selected) FlowWhite else Color.Transparent,
        animationSpec = tween(FlowMotion.STANDARD),
        label = "chipBackground",
    )
    val foreground by animateColorAsState(
        targetValue = if (selected) FlowBlack else FlowTextSecondary,
        animationSpec = tween(FlowMotion.STANDARD),
        label = "chipForeground",
    )
    // Unselected chips are still interactive, so the outline clears 3:1 rather
    // than fading into the background like a divider.
    val border by animateColorAsState(
        targetValue = if (selected) FlowWhite else FlowBorderStrong,
        animationSpec = tween(FlowMotion.STANDARD),
        label = "chipBorder",
    )

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 36.dp)
            .clip(MaterialTheme.shapes.small)
            .border(FlowSizes.hairline, border, MaterialTheme.shapes.small)
            .background(background)
            .clickable(
                role = Role.RadioButton,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            )
            .padding(horizontal = FlowSpacing.sm, vertical = FlowSpacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun FlowStepper(
    label: String,
    value: Int,
    unitLabel: String,
    valueDescription: String,
    onValueChange: (String) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    min: Int,
    max: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FlowSectionLabel(label)
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm),
        ) {
            FlowIconAction(
                icon = Icons.Default.Remove,
                contentDescription = "Decrease $valueDescription",
                onClick = onDecrement,
                enabled = value > min,
                iconSize = FlowSizes.iconSm,
            )
            BasicTextField(
                value = value.toString(),
                onValueChange = onValueChange,
                modifier = Modifier
                    .width(56.dp)
                    .defaultMinSize(minHeight = FlowSizes.touchTarget),
                textStyle = MaterialTheme.typography.headlineMedium.copy(color = FlowTextPrimary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = SolidColor(FlowWhite),
            )
            Text(
                text = unitLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = FlowTextSecondary,
                modifier = Modifier.weight(1f),
            )
            FlowIconAction(
                icon = Icons.Default.Add,
                contentDescription = "Increase $valueDescription",
                onClick = onIncrement,
                enabled = value < max,
                iconSize = FlowSizes.iconSm,
            )
        }
    }
}

@Composable
fun FlowScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = FlowSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            FlowIconAction(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.content_description_back),
                onClick = onBack,
            )
        } else {
            Spacer(modifier = Modifier.width(FlowSizes.touchTarget))
        }
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = FlowTextTertiary,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = FlowSpacing.xs)
                .semantics { heading() },
        )
        if (trailing != null) {
            trailing()
        } else {
            Spacer(modifier = Modifier.width(FlowSizes.touchTarget))
        }
    }
}

/** Selection list for a bottom sheet. The accent dot is the only colour in the app. */
@Composable
fun FlowOptionSheet(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FlowSurface)
            .padding(horizontal = FlowSpacing.screenHorizontal)
            .padding(bottom = FlowSpacing.xl),
    ) {
        FlowSectionLabel(title)
        Spacer(modifier = Modifier.height(FlowSpacing.md))
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        role = Role.RadioButton,
                        onClick = {
                            onSelect(index)
                            onDismiss()
                        },
                    )
                    .defaultMinSize(minHeight = FlowSizes.touchTarget)
                    .padding(vertical = FlowSpacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selected) FlowTextPrimary else FlowTextSecondary,
                )
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(FlowAccent),
                    )
                }
            }
            if (index < options.lastIndex) {
                FlowHairlineDivider()
            }
        }
    }
}

/** Flow's own dialog. Used for destructive confirmation, never for information. */
@Composable
fun FlowDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(FlowSurfaceRaised)
                .border(FlowSizes.hairline, FlowBorder, MaterialTheme.shapes.large)
                .padding(FlowSpacing.lg),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = FlowTextPrimary,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(modifier = Modifier.height(FlowSpacing.xs))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = FlowTextSecondary,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.lg))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FlowTextAction(text = dismissText, onClick = onDismiss)
                Spacer(modifier = Modifier.width(FlowSpacing.lg))
                FlowTextAction(
                    text = confirmText,
                    onClick = onConfirm,
                    destructive = destructive,
                )
            }
        }
    }
}

/**
 * Navigation row for the drawer. Selection is shown with a hairline rule and a
 * white label rather than a filled pill, so the drawer stays flat and quiet.
 */
@Composable
fun FlowDrawerItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val labelColor by animateColorAsState(
        targetValue = if (selected) FlowTextPrimary else FlowTextSecondary,
        animationSpec = tween(FlowMotion.STANDARD),
        label = "drawerItemLabel",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Tab,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            )
            .defaultMinSize(minHeight = FlowSizes.touchTarget)
            .padding(vertical = FlowSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(FlowSpacing.sm)
                .height(FlowSizes.hairline)
                .background(if (selected) FlowWhite else Color.Transparent),
        )
        Spacer(modifier = Modifier.width(FlowSpacing.sm))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = labelColor,
        )
    }
}

/** The single reveal transition used for every progressive-disclosure section. */
@Composable
fun AnimatedReveal(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(FlowMotion.REVEAL)) + expandVertically(tween(FlowMotion.REVEAL)),
        exit = fadeOut(tween(FlowMotion.FAST)) + shrinkVertically(tween(FlowMotion.FAST)),
        modifier = modifier,
    ) {
        content()
    }
}

/** A full-width content row separated by hairlines rather than wrapped in a card. */
@Composable
fun FlowListRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = FlowSpacing.md),
    ) {
        content()
    }
}

/** Read-only counterpart to [FlowSelectorRow] for facts the user cannot change. */
@Composable
fun FlowInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = FlowSizes.touchTarget)
            .padding(vertical = FlowSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FlowSectionLabel(label)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = FlowTextPrimary,
        )
    }
}

@Composable
fun FlowSelectorRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = FlowSizes.touchTarget)
            .padding(vertical = FlowSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FlowSectionLabel(label)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = FlowTextPrimary,
        )
    }
}