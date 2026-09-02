package com.deepak.flow.feature.water.presentation

import android.app.TimePickerDialog
import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt
import com.deepak.flow.app.components.AnimatedReveal
import com.deepak.flow.app.components.FeatureOffState
import com.deepak.flow.app.components.FlowButton
import com.deepak.flow.app.components.FlowButtonVariant
import com.deepak.flow.app.components.FlowChip
import com.deepak.flow.app.components.FlowFieldHeading
import com.deepak.flow.app.components.FlowHairlineDivider
import com.deepak.flow.app.components.FlowScreenTitle
import com.deepak.flow.app.components.FlowSectionBreak
import com.deepak.flow.app.components.FlowSectionLabel
import com.deepak.flow.app.components.FlowStepper
import com.deepak.flow.app.components.FlowSupportingText
import com.deepak.flow.app.components.FlowTextAction
import com.deepak.flow.app.components.FlowTextField
import com.deepak.flow.app.components.FlowToggleRow
import com.deepak.flow.app.navigation.FlowDrawerDestination
import com.deepak.flow.app.navigation.FlowShell
import com.deepak.flow.app.theme.FlowBorder
import com.deepak.flow.app.theme.FlowSizes
import com.deepak.flow.app.theme.FlowSpacing
import com.deepak.flow.app.theme.FlowSurfaceRaised
import com.deepak.flow.app.theme.FlowTextPrimary
import com.deepak.flow.app.theme.FlowTextSecondary
import com.deepak.flow.app.theme.FlowTextTertiary
import com.deepak.flow.core.model.UserProfile
import com.deepak.flow.core.model.WaterReminderSettings
import com.deepak.flow.core.model.filterCustomWaterInput
import com.deepak.flow.core.model.filterWaterGoalInput
import com.deepak.flow.core.model.formatWaterGoalInput
import com.deepak.flow.core.model.formatWaterLiters
import com.deepak.flow.core.model.isExistingWaterQuickAddMl
import com.deepak.flow.core.model.parseCustomWaterMl
import com.deepak.flow.core.model.parseWaterGoalMl
import com.deepak.flow.core.model.waterQuickAddAmountsMl
import com.deepak.flow.core.water.FlowBottleStyles
import com.deepak.flow.feature.reminder.presentation.flowTimeFormatter
import java.time.LocalTime

private enum class CustomWaterStep {
    Amount,
    Mode,
}

@Composable
fun WaterScreen(
    waterEnabled: Boolean,
    waterGoalMl: Int?,
    waterBottleStyleIndex: Int?,
    waterIntakeMl: Int,
    canUndoWater: Boolean,
    waterCustomQuickAddsMl: List<Int> = emptyList(),
    remindersEnabled: Boolean,
    gymEnabled: Boolean,
    waterRemindersEnabled: Boolean,
    waterReminderIntervalMinutes: Int,
    waterActiveHoursStartMinutes: Int,
    waterActiveHoursEndMinutes: Int,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
    onGymEnabledChange: (Boolean) -> Unit,
    onGoalSet: (Int) -> Unit,
    onBottleStyleSet: (Int) -> Unit,
    onSaveSettings: (goalMl: Int, bottleStyleIndex: Int) -> Unit,
    onAddWater: (Int) -> Unit,
    onAddCustomWater: (amountMl: Int, saveAsButton: Boolean) -> Unit,
    onRemoveCustomWater: (Int) -> Unit,
    onUndoWater: () -> Unit,
    onWaterRemindersEnabledChange: (Boolean) -> Unit,
    onWaterReminderIntervalInput: (String) -> Unit,
    onIncrementWaterReminderInterval: () -> Unit,
    onDecrementWaterReminderInterval: () -> Unit,
    onWaterActiveHoursStartChange: (Int) -> Unit,
    onWaterActiveHoursEndChange: (Int) -> Unit,
    onDestinationClick: (FlowDrawerDestination) -> Unit,
    userName: String? = null,
    modifier: Modifier = Modifier,
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var revisingSetupGoal by rememberSaveable { mutableStateOf(false) }
    val goalMl = waterGoalMl
    val styleIndex = waterBottleStyleIndex
    val showingEdit = editing && goalMl != null && styleIndex != null
    val showingBottleSetup = waterEnabled && goalMl != null && styleIndex == null && !revisingSetupGoal
    val showingGoalRevision = waterEnabled && styleIndex == null && revisingSetupGoal
    val onInnerBack: (() -> Unit)? = when {
        showingEdit -> ({ editing = false })
        showingBottleSetup -> ({ revisingSetupGoal = true })
        showingGoalRevision -> ({ revisingSetupGoal = false })
        else -> null
    }

    FlowShell(
        selected = FlowDrawerDestination.WATER,
        userName = userName,
        remindersEnabled = remindersEnabled,
        waterEnabled = waterEnabled,
        gymEnabled = gymEnabled,
        onRemindersEnabledChange = onRemindersEnabledChange,
        onWaterEnabledChange = onWaterEnabledChange,
        onGymEnabledChange = onGymEnabledChange,
        onDestinationClick = onDestinationClick,
        onBack = onInnerBack,
        modifier = modifier,
    ) {
        FlowScreenTitle("H₂O")
        when {
            !waterEnabled -> {
                Spacer(modifier = Modifier.height(FlowSpacing.lg))
                FeatureOffState(
                    title = "H₂O is off.",
                    message = "Turn it on when you want it back.",
                    actionLabel = "Turn H₂O back on",
                    onTurnBackOn = { onWaterEnabledChange(true) },
                    modifier = Modifier.weight(1f),
                )
            }
            goalMl == null -> {
                Spacer(modifier = Modifier.height(FlowSpacing.lg))
                WaterGoalPrompt(
                    onGoalSet = {
                        revisingSetupGoal = false
                        onGoalSet(it)
                    },
                )
            }
            styleIndex == null && revisingSetupGoal -> {
                Spacer(modifier = Modifier.height(FlowSpacing.lg))
                WaterGoalPrompt(
                    onGoalSet = {
                        revisingSetupGoal = false
                        onGoalSet(it)
                    },
                )
            }
            styleIndex == null -> {
                Spacer(modifier = Modifier.height(FlowSpacing.lg))
                WaterBottlePrompt(onBottleStyleSet = onBottleStyleSet)
            }
            editing -> {
                Spacer(modifier = Modifier.height(FlowSpacing.lg))
                WaterEditPrompt(
                    initialGoalMl = goalMl,
                    initialBottleIndex = styleIndex.coerceIn(0, FlowBottleStyles.lastIndex),
                    customQuickAddsMl = waterCustomQuickAddsMl,
                    onRemoveCustom = onRemoveCustomWater,
                    onGoalChange = { onSaveSettings(it, styleIndex) },
                    onBottleChange = { onSaveSettings(goalMl, it) },
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                ) {
                    WaterTracker(
                        millilitres = waterIntakeMl,
                        goalMl = goalMl,
                        styleIndex = styleIndex.coerceIn(0, FlowBottleStyles.lastIndex),
                        canUndo = canUndoWater,
                        customQuickAddsMl = waterCustomQuickAddsMl,
                        waterRemindersEnabled = waterRemindersEnabled,
                        waterReminderIntervalMinutes = waterReminderIntervalMinutes,
                        waterActiveHoursStartMinutes = waterActiveHoursStartMinutes,
                        waterActiveHoursEndMinutes = waterActiveHoursEndMinutes,
                        onAdd = onAddWater,
                        onAddCustom = onAddCustomWater,
                        onUndo = onUndoWater,
                        onEdit = { editing = true },
                        onWaterRemindersEnabledChange = onWaterRemindersEnabledChange,
                        onWaterReminderIntervalInput = onWaterReminderIntervalInput,
                        onIncrementWaterReminderInterval = onIncrementWaterReminderInterval,
                        onDecrementWaterReminderInterval = onDecrementWaterReminderInterval,
                        onWaterActiveHoursStartChange = onWaterActiveHoursStartChange,
                        onWaterActiveHoursEndChange = onWaterActiveHoursEndChange,
                    )
                }
            }
        }
    }
}

@Composable
private fun WaterTracker(
    millilitres: Int,
    goalMl: Int,
    styleIndex: Int,
    canUndo: Boolean,
    customQuickAddsMl: List<Int>,
    waterRemindersEnabled: Boolean,
    waterReminderIntervalMinutes: Int,
    waterActiveHoursStartMinutes: Int,
    waterActiveHoursEndMinutes: Int,
    onAdd: (Int) -> Unit,
    onAddCustom: (amountMl: Int, saveAsButton: Boolean) -> Unit,
    onUndo: () -> Unit,
    onEdit: () -> Unit,
    onWaterRemindersEnabledChange: (Boolean) -> Unit,
    onWaterReminderIntervalInput: (String) -> Unit,
    onIncrementWaterReminderInterval: () -> Unit,
    onDecrementWaterReminderInterval: () -> Unit,
    onWaterActiveHoursStartChange: (Int) -> Unit,
    onWaterActiveHoursEndChange: (Int) -> Unit,
) {
    val progress = if (goalMl <= 0) 0f else (millilitres / goalMl.toFloat()).coerceAtMost(1f)
    val percentLabel = "${(progress * 100f).roundToInt()}%"
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val bottleHeight = (screenHeight * 0.28f).coerceIn(168.dp, 220.dp)
    val quickAdds = remember(customQuickAddsMl) { waterQuickAddAmountsMl(customQuickAddsMl) }
    val progressDescription =
        "${formatWaterLiters(millilitres)} of ${formatWaterLiters(goalMl)}, $percentLabel"
    val canAdd = millilitres < UserProfile.MAX_WATER_INTAKE_ML
    val canSaveMoreCustoms = customQuickAddsMl.size < UserProfile.MAX_CUSTOM_WATER_QUICK_ADDS
    var customStep by rememberSaveable { mutableStateOf<String?>(null) }
    var customAmountText by rememberSaveable { mutableStateOf("") }
    val customParsed = parseCustomWaterMl(customAmountText)
    val customAlreadyExists = customParsed != null &&
        isExistingWaterQuickAddMl(customParsed, customQuickAddsMl)
    val canSaveCustomButton = customParsed != null &&
        canSaveMoreCustoms &&
        !customAlreadyExists

    if (customStep == CustomWaterStep.Amount.name) {
        CustomWaterAmountDialog(
            amountText = customAmountText,
            parsed = customParsed,
            alreadyExists = customAlreadyExists,
            onAmountChange = { customAmountText = filterCustomWaterInput(it) },
            onContinue = { customStep = CustomWaterStep.Mode.name },
            onDismiss = {
                customStep = null
                customAmountText = ""
            },
        )
    } else if (customStep == CustomWaterStep.Mode.name && customParsed != null && !customAlreadyExists) {
        CustomWaterModeDialog(
            amountMl = customParsed,
            canSaveAsButton = canSaveCustomButton,
            onOneTime = {
                onAddCustom(customParsed, false)
                customStep = null
                customAmountText = ""
            },
            onSaveAsButton = {
                onAddCustom(customParsed, true)
                customStep = null
                customAmountText = ""
            },
            onDismiss = {
                customStep = null
                customAmountText = ""
            },
        )
    }

    // 1. Progress hero
    Box(modifier = Modifier.fillMaxWidth()) {
        FlowTextAction(
            text = "Edit",
            onClick = onEdit,
            modifier = Modifier.align(Alignment.TopEnd),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = FlowSpacing.xxs)
                .semantics { contentDescription = progressDescription },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = formatWaterLiters(millilitres),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium),
                color = FlowTextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clickable(role = Role.Button, onClick = onEdit)
                    .padding(horizontal = FlowSpacing.sm),
            )
            Text(
                text = "/ ${formatWaterLiters(goalMl)}",
                style = MaterialTheme.typography.bodyLarge,
                color = FlowTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clickable(role = Role.Button, onClick = onEdit)
                    .padding(horizontal = FlowSpacing.sm, vertical = FlowSpacing.xxs),
            )
            Spacer(modifier = Modifier.height(FlowSpacing.md))
            BottleFill(
                bottleRes = FlowBottleStyles.drawableRes(styleIndex),
                progress = progress,
                modifier = Modifier
                    .height(bottleHeight)
                    .fillMaxWidth(0.42f),
            )
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            Text(
                text = percentLabel,
                style = MaterialTheme.typography.labelLarge,
                color = FlowTextTertiary,
                textAlign = TextAlign.Center,
            )
        }
    }

    Spacer(modifier = Modifier.height(FlowSpacing.xl))

    // 2. Add water
    FlowSectionLabel("Add water")
    Spacer(modifier = Modifier.height(FlowSpacing.sm))
    val addActions = quickAdds.map { amount ->
        amount to quickAddLabel(amount)
    } + listOf(null to "Custom")
    addActions.chunked(2).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm),
        ) {
            row.forEach { (amount, label) ->
                FlowButton(
                    text = label,
                    onClick = {
                        if (amount == null) {
                            customAmountText = ""
                            customStep = CustomWaterStep.Amount.name
                        } else {
                            onAdd(amount)
                        }
                    },
                    enabled = canAdd,
                    variant = FlowButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                )
            }
            if (row.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
    }
    FlowTextAction(
        text = "Undo",
        onClick = onUndo,
        enabled = canUndo,
    )

    FlowSectionBreak()

    // 3. Reminders
    WaterRemindersSection(
        remindersEnabled = waterRemindersEnabled,
        intervalMinutes = waterReminderIntervalMinutes,
        activeHoursStartMinutes = waterActiveHoursStartMinutes,
        activeHoursEndMinutes = waterActiveHoursEndMinutes,
        onRemindersEnabledChange = onWaterRemindersEnabledChange,
        onIntervalInput = onWaterReminderIntervalInput,
        onIncrementInterval = onIncrementWaterReminderInterval,
        onDecrementInterval = onDecrementWaterReminderInterval,
        onActiveHoursStartChange = onWaterActiveHoursStartChange,
        onActiveHoursEndChange = onWaterActiveHoursEndChange,
    )
    Spacer(modifier = Modifier.height(FlowSpacing.lg))
}

private fun quickAddLabel(amountMl: Int): String =
    if (amountMl >= 1000) "1 L" else "$amountMl ml"

@Composable
private fun CustomWaterAmountDialog(
    amountText: String,
    parsed: Int?,
    alreadyExists: Boolean,
    onAmountChange: (String) -> Unit,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(FlowSurfaceRaised)
                .border(FlowSizes.hairline, FlowBorder, MaterialTheme.shapes.large)
                .padding(FlowSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "How much?",
                style = MaterialTheme.typography.titleLarge,
                color = FlowTextPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { heading() },
            )
            Spacer(modifier = Modifier.height(FlowSpacing.xxs))
            Text(
                text = if (alreadyExists) {
                    "That amount already has a button."
                } else {
                    "${UserProfile.MIN_CUSTOM_WATER_ML}-${UserProfile.MAX_CUSTOM_WATER_ML} ml"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = FlowTextSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(FlowSpacing.lg))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                FlowTextField(
                    value = amountText,
                    onValueChange = onAmountChange,
                    placeholder = "330",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.width(120.dp),
                )
                Spacer(modifier = Modifier.width(FlowSpacing.sm))
                Text(
                    text = "ml",
                    style = MaterialTheme.typography.titleLarge,
                    color = FlowTextPrimary,
                )
            }
            Spacer(modifier = Modifier.height(FlowSpacing.xl))
            FlowButton(
                text = "Continue",
                onClick = onContinue,
                enabled = parsed != null && !alreadyExists,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            FlowTextAction(
                text = "Cancel",
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun CustomWaterModeDialog(
    amountMl: Int,
    canSaveAsButton: Boolean,
    onOneTime: () -> Unit,
    onSaveAsButton: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(FlowSurfaceRaised)
                .border(FlowSizes.hairline, FlowBorder, MaterialTheme.shapes.large)
                .padding(FlowSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "$amountMl ml",
                style = MaterialTheme.typography.titleLarge,
                color = FlowTextPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { heading() },
            )
            Spacer(modifier = Modifier.height(FlowSpacing.xxs))
            Text(
                text = if (canSaveAsButton) {
                    "Add once, or keep a button for next time."
                } else {
                    "You already have ${UserProfile.MAX_CUSTOM_WATER_QUICK_ADDS} custom buttons. Add this once, or remove one in Edit."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = FlowTextSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(FlowSpacing.lg))
            FlowButton(
                text = "One time",
                onClick = onOneTime,
            )
            if (canSaveAsButton) {
                Spacer(modifier = Modifier.height(FlowSpacing.sm))
                FlowButton(
                    text = "Save as button",
                    onClick = onSaveAsButton,
                    variant = FlowButtonVariant.Secondary,
                )
            }
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            FlowTextAction(
                text = "Cancel",
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun WaterRemindersSection(
    remindersEnabled: Boolean,
    intervalMinutes: Int,
    activeHoursStartMinutes: Int,
    activeHoursEndMinutes: Int,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onIntervalInput: (String) -> Unit,
    onIncrementInterval: () -> Unit,
    onDecrementInterval: () -> Unit,
    onActiveHoursStartChange: (Int) -> Unit,
    onActiveHoursEndChange: (Int) -> Unit,
) {
    val context = LocalContext.current
    val uses24Hour = remember(context) { DateFormat.is24HourFormat(context) }
    val timeFormatter = remember(uses24Hour) { flowTimeFormatter(uses24Hour) }
    val startTime = remember(activeHoursStartMinutes) {
        WaterReminderSettings.localTimeFromMinutes(activeHoursStartMinutes)
    }
    val endTime = remember(activeHoursEndMinutes) {
        WaterReminderSettings.localTimeFromMinutes(activeHoursEndMinutes)
    }
    val startLabel = startTime.format(timeFormatter)
    val endLabel = endTime.format(timeFormatter)

    FlowToggleRow(
        label = "Remind me to drink",
        supporting = if (remindersEnabled) null else "Off until you turn it on.",
        checked = remindersEnabled,
        onCheckedChange = onRemindersEnabledChange,
    )
    AnimatedReveal(visible = remindersEnabled) {
        Column {
            Spacer(modifier = Modifier.height(FlowSpacing.md))
            FlowStepper(
                label = "Remind every",
                value = intervalMinutes,
                unitLabel = "min",
                valueDescription = "water reminder interval",
                onValueChange = onIntervalInput,
                onIncrement = onIncrementInterval,
                onDecrement = onDecrementInterval,
                min = WaterReminderSettings.MIN_INTERVAL_MINUTES,
                max = WaterReminderSettings.MAX_INTERVAL_MINUTES,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.lg))
            FlowSectionLabel("Active hours")
            Spacer(modifier = Modifier.height(FlowSpacing.xxs))
            FlowSupportingText("Only remind me while I'm awake.")
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = startLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = FlowTextPrimary,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.small)
                        .clickable(role = Role.Button) {
                            showTimePicker(
                                context = context,
                                time = startTime,
                                uses24Hour = uses24Hour,
                                onTime = {
                                    onActiveHoursStartChange(WaterReminderSettings.minutesOfDay(it))
                                },
                            )
                        }
                        .defaultMinSize(minHeight = FlowSizes.touchTarget)
                        .padding(vertical = FlowSpacing.xs),
                )
                Text(
                    text = "→",
                    style = MaterialTheme.typography.bodyLarge,
                    color = FlowTextTertiary,
                    modifier = Modifier.padding(horizontal = FlowSpacing.sm),
                )
                Text(
                    text = endLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = FlowTextPrimary,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.small)
                        .clickable(role = Role.Button) {
                            showTimePicker(
                                context = context,
                                time = endTime,
                                uses24Hour = uses24Hour,
                                onTime = {
                                    onActiveHoursEndChange(WaterReminderSettings.minutesOfDay(it))
                                },
                            )
                        }
                        .defaultMinSize(minHeight = FlowSizes.touchTarget)
                        .padding(vertical = FlowSpacing.xs),
                )
            }
        }
    }
}

@Composable
private fun WaterGoalPrompt(onGoalSet: (Int) -> Unit) {
    var goalText by rememberSaveable { mutableStateOf("") }
    val parsed = parseWaterGoalMl(goalText)
    FlowFieldHeading(
        label = "Daily goal",
        supporting = "How much water do you want to drink each day?",
    )
    FlowTextField(
        value = goalText,
        onValueChange = { goalText = filterWaterGoalInput(it) },
        placeholder = "2.5",
        suffix = "L",
        keyboardType = KeyboardType.Decimal,
    )
    Spacer(modifier = Modifier.height(FlowSpacing.xl))
    FlowButton(
        text = "Continue",
        onClick = { parsed?.let(onGoalSet) },
        enabled = parsed != null,
    )
}

@Composable
private fun ColumnScope.WaterBottlePrompt(onBottleStyleSet: (Int) -> Unit) {
    var pendingIndex by rememberSaveable { mutableIntStateOf(-1) }
    BottlePickerBlock(
        selectedIndex = pendingIndex.takeIf { it >= 0 },
        onSelect = { pendingIndex = it },
        label = "Bottle",
        supporting = "Pick one. Flow will remember it.",
    )
    Spacer(modifier = Modifier.height(FlowSpacing.xl))
    FlowButton(
        text = "Continue",
        onClick = { if (pendingIndex >= 0) onBottleStyleSet(pendingIndex) },
        enabled = pendingIndex >= 0,
    )
    BottlePreview(selectedIndex = pendingIndex.takeIf { it >= 0 })
}

@Composable
private fun ColumnScope.WaterEditPrompt(
    initialGoalMl: Int,
    initialBottleIndex: Int,
    customQuickAddsMl: List<Int>,
    onRemoveCustom: (Int) -> Unit,
    onGoalChange: (Int) -> Unit,
    onBottleChange: (Int) -> Unit,
) {
    var goalText by rememberSaveable(initialGoalMl) {
        mutableStateOf(formatWaterGoalInput(initialGoalMl))
    }
    var pendingIndex by rememberSaveable(initialBottleIndex) {
        mutableIntStateOf(initialBottleIndex)
    }
    FlowFieldHeading(
        label = "Daily goal",
        supporting = "How much water do you want to drink each day?",
    )
    FlowTextField(
        value = goalText,
        onValueChange = { raw ->
            val nextText = filterWaterGoalInput(raw)
            goalText = nextText
            val parsed = parseWaterGoalMl(nextText) ?: return@FlowTextField
            if (parsed != initialGoalMl) onGoalChange(parsed)
        },
        placeholder = "2.5",
        suffix = "L",
        keyboardType = KeyboardType.Decimal,
    )
    Spacer(modifier = Modifier.height(FlowSpacing.lg))
    BottlePickerBlock(
        selectedIndex = pendingIndex.takeIf { it >= 0 },
        onSelect = { index ->
            pendingIndex = index
            if (index != initialBottleIndex) onBottleChange(index)
        },
        label = "Change bottle",
    )
    if (customQuickAddsMl.isNotEmpty()) {
        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        FlowFieldHeading(
            label = "Custom buttons",
            supporting = "Remove ones you do not want anymore.",
        )
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        customQuickAddsMl.forEach { amount ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = quickAddLabel(amount),
                    style = MaterialTheme.typography.bodyLarge,
                    color = FlowTextPrimary,
                )
                FlowTextAction(
                    text = "Remove",
                    onClick = { onRemoveCustom(amount) },
                    destructive = true,
                )
            }
            FlowHairlineDivider()
        }
    }
    BottlePreview(selectedIndex = pendingIndex.takeIf { it >= 0 })
}

@Composable
private fun BottlePickerBlock(
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    label: String,
    supporting: String? = null,
) {
    FlowFieldHeading(
        label = label,
        supporting = supporting,
    )
    Spacer(modifier = Modifier.height(FlowSpacing.md))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(FlowSpacing.xxs),
    ) {
        repeat(UserProfile.BOTTLE_STYLE_COUNT) { index ->
            FlowChip(
                label = "${index + 1}",
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
            )
        }
    }
}

@Composable
private fun ColumnScope.BottlePreview(selectedIndex: Int?) {
    if (selectedIndex == null) {
        Spacer(modifier = Modifier.weight(1f))
        return
    }
    Spacer(modifier = Modifier.height(FlowSpacing.lg))
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(vertical = FlowSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        BottleFill(
            bottleRes = FlowBottleStyles.drawableRes(selectedIndex),
            progress = 0.5f,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun showTimePicker(
    context: Context,
    time: LocalTime,
    uses24Hour: Boolean,
    onTime: (LocalTime) -> Unit,
) {
    TimePickerDialog(
        context,
        { _, hour, minute -> onTime(LocalTime.of(hour, minute)) },
        time.hour,
        time.minute,
        uses24Hour,
    ).show()
}
