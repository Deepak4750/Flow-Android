package com.deepak.flow.feature.gym.presentation



import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deepak.flow.FlowApplication
import com.deepak.flow.app.components.FlowButton
import com.deepak.flow.app.components.FlowButtonVariant
import com.deepak.flow.app.components.FlowChip
import com.deepak.flow.app.components.FlowMetaText
import com.deepak.flow.app.components.FlowScreenTitle
import com.deepak.flow.app.components.FlowSectionLabel
import com.deepak.flow.app.components.FlowSupportingText
import com.deepak.flow.app.components.FlowSwipeDeleteRow
import com.deepak.flow.app.components.FlowTextAction
import com.deepak.flow.app.components.FlowTextField
import com.deepak.flow.app.navigation.FlowDrawerDestination
import com.deepak.flow.app.navigation.FlowShell
import com.deepak.flow.app.theme.FlowBorder
import com.deepak.flow.app.theme.FlowSpacing
import com.deepak.flow.app.theme.FlowSizes
import com.deepak.flow.app.theme.FlowSurfaceRaised
import com.deepak.flow.app.theme.FlowTextPrimary
import com.deepak.flow.app.theme.FlowTextSecondary
import com.deepak.flow.core.gym.GymEquipment
import com.deepak.flow.core.gym.GymLibraryCatalogueDefaults
import com.deepak.flow.core.gym.GymLibraryExercise
import com.deepak.flow.core.gym.GymLibrarySourceFilter
import com.deepak.flow.core.gym.GymMuscleGroup



@Composable
fun ExerciseLibraryScreen(
    userName: String?,
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
    gymEnabled: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
    onGymEnabledChange: (Boolean) -> Unit,
    onDestinationClick: (FlowDrawerDestination) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseLibraryViewModel = viewModel(
        factory = ExerciseLibraryViewModelFactory(
            application = LocalContext.current.applicationContext as FlowApplication,
        ),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()



    FlowShell(
        selected = FlowDrawerDestination.GYM,
        userName = userName,
        remindersEnabled = remindersEnabled,
        waterEnabled = waterEnabled,
        gymEnabled = gymEnabled,
        onRemindersEnabledChange = onRemindersEnabledChange,
        onWaterEnabledChange = onWaterEnabledChange,
        onGymEnabledChange = onGymEnabledChange,
        onDestinationClick = onDestinationClick,
        onBack = onBack,
        modifier = modifier,
    ) {
        FlowScreenTitle("Exercise Library")
        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        FlowTextField(
            value = uiState.query,
            onValueChange = viewModel::onQueryChange,
            placeholder = "Search exercises...",
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(FlowSpacing.md))
        LibraryFilterRow(
            label = "Show",
            selected = if (uiState.sourceFilter == GymLibrarySourceFilter.ALL) {
                null
            } else {
                uiState.sourceFilter.label
            },
            options = GymLibrarySourceFilter.entries
                .filter { it != GymLibrarySourceFilter.ALL }
                .map { it.label },
            onSelected = { label ->
                val filter = GymLibrarySourceFilter.entries.firstOrNull { it.label == label }
                    ?: GymLibrarySourceFilter.ALL
                viewModel.onSourceFilterSelected(filter)
            },
            onClear = { viewModel.onSourceFilterSelected(GymLibrarySourceFilter.ALL) },
            clearLabel = "All",
        )
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        LibraryFilterRow(
            label = "Muscle",
            selected = uiState.muscleFilter?.displayName,
            options = GymMuscleGroup.entries.map { it.displayName },
            onSelected = { label ->
                val muscle = GymMuscleGroup.entries.firstOrNull { it.displayName == label }
                viewModel.onMuscleFilterSelected(
                    if (uiState.muscleFilter?.displayName == label) null else muscle,
                )
            },
            onClear = { viewModel.onMuscleFilterSelected(null) },
        )
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        LibraryFilterRow(
            label = "Equipment",
            selected = uiState.equipmentFilter?.displayName,
            options = GymEquipment.entries.map { it.displayName },
            onSelected = { label ->
                val equipment = GymEquipment.entries.firstOrNull { it.displayName == label }
                viewModel.onEquipmentFilterSelected(
                    if (uiState.equipmentFilter?.displayName == label) null else equipment,
                )
            },
            onClear = { viewModel.onEquipmentFilterSelected(null) },
        )
        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.heightIn(min = FlowSizes.touchTarget),
                contentAlignment = Alignment.CenterStart,
            ) {
                FlowMetaText(
                    if (uiState.loading) "Loading..." else "${uiState.exercises.size} exercises",
                )
            }
            FlowTextAction(
                text = "+ Add custom",
                onClick = { viewModel.openCreate() },
            )
        }
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        if (!uiState.loading && uiState.exercises.isEmpty()) {
            FlowSupportingText("No exercises match your filters.")
        } else {
            var swipeResetKey by remember { mutableIntStateOf(0) }
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(FlowSpacing.xxs),
            ) {
                items(uiState.exercises, key = { it.exerciseId }) { exercise ->
                    LibraryExerciseRow(
                        exercise = exercise,
                        onClick = { viewModel.openExercise(exercise.exerciseId) },
                        onDelete = {
                            viewModel.deleteCustomExercise(exercise.exerciseId)
                            swipeResetKey++
                        },
                        swipeResetKey = swipeResetKey,
                    )
                }
            }
        }
    }



    if (uiState.showDetail && uiState.selectedExercise != null) {
        ExerciseDetailSheet(
            exercise = uiState.selectedExercise!!,
            onDismiss = viewModel::dismissDetail,
            onEdit = viewModel::openEdit,
            onDelete = viewModel::deleteCustomExercise,
        )
    }

    if (uiState.showEdit && uiState.selectedExercise != null) {
        key(uiState.selectedExercise!!.exerciseId) {
            CustomExerciseFormDialog(
                title = "Edit exercise",
                displayName = uiState.editDisplayName,
                primaryMuscle = uiState.editPrimaryMuscle,
                secondaryMuscles = uiState.editSecondaryMuscles,
                equipment = uiState.editEquipment,
                message = uiState.message,
                onDisplayNameChange = viewModel::onEditDisplayNameChange,
                onPrimaryMuscleSelected = viewModel::onEditPrimaryMuscleSelected,
                onSecondaryMuscleToggled = viewModel::onEditSecondaryMuscleToggled,
                onEquipmentSelected = viewModel::onEditEquipmentSelected,
                onSave = viewModel::saveEdit,
                onDismiss = viewModel::dismissEdit,
            )
        }
    }

    if (uiState.showCreate) {
        key("create-custom-exercise") {
            CustomExerciseFormDialog(
                title = "Add custom exercise",
                displayName = uiState.createName,
                primaryMuscle = uiState.createPrimaryMuscle,
                secondaryMuscles = uiState.createSecondaryMuscles,
                equipment = uiState.createEquipment,
                message = uiState.message,
                onDisplayNameChange = viewModel::onCreateNameChange,
                onPrimaryMuscleSelected = viewModel::onCreatePrimaryMuscleSelected,
                onSecondaryMuscleToggled = viewModel::onCreateSecondaryMuscleToggled,
                onEquipmentSelected = viewModel::onCreateEquipmentSelected,
                onSave = viewModel::saveCreate,
                onDismiss = viewModel::dismissCreate,
                saveLabel = "Create",
            )
        }
    }
}



@Composable
private fun LibraryFilterRow(
    label: String,
    selected: String?,
    options: List<String>,
    onSelected: (String) -> Unit,
    onClear: () -> Unit,
    clearLabel: String = "All",
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        FlowSectionLabel(label)
        Spacer(modifier = Modifier.height(FlowSpacing.xs))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(FlowSpacing.xs),
        ) {
            FlowChip(
                label = clearLabel,
                selected = selected == null,
                onClick = onClear,
            )
            options.forEach { option ->
                FlowChip(
                    label = option,
                    selected = selected == option,
                    onClick = { onSelected(option) },
                )
            }
        }
    }
}



@Composable
private fun LibraryExerciseRow(
    exercise: GymLibraryExercise,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    swipeResetKey: Int,
) {
    FlowSwipeDeleteRow(
        enabled = exercise.isCustom,
        onDelete = onDelete,
        resetKey = swipeResetKey,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = FlowSpacing.sm),
        ) {
        Text(
            text = exercise.displayName,
            style = MaterialTheme.typography.bodyLarge,
            color = FlowTextPrimary,
        )
        val meta = buildList {
            exercise.primaryMuscle?.displayName?.let { add(it) }
            exercise.equipment?.displayName?.let { add(it) }
            if (exercise.isCustom) add("Custom")
            if (exercise.hasUserOverride) add("Customized")
        }
        if (meta.isNotEmpty()) {
            Spacer(modifier = Modifier.height(FlowSpacing.xxs))
            Text(
                text = meta.joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = FlowTextSecondary,
            )
        }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseDetailSheet(
    exercise: GymLibraryExercise,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FlowSpacing.lg)
                .padding(bottom = FlowSpacing.xl),
        ) {
            Text(
                text = exercise.displayName,
                style = MaterialTheme.typography.titleLarge,
                color = FlowTextPrimary,
            )
            if (exercise.isCustom) {
                Spacer(modifier = Modifier.height(FlowSpacing.xxs))
                FlowMetaText("Custom exercise")
            }
            Spacer(modifier = Modifier.height(FlowSpacing.lg))



            if (!exercise.isCustom && exercise.catalogueDefaults != null) {
                CatalogueSection(exercise.catalogueDefaults)
                if (exercise.hasUserOverride) {
                    Spacer(modifier = Modifier.height(FlowSpacing.lg))
                    FlowSectionLabel("Your customization")
                    Spacer(modifier = Modifier.height(FlowSpacing.sm))
                    CustomizationLine("Name", exercise.displayName, exercise.catalogueDefaults.displayName)
                    CustomizationLine(
                        "Primary muscle",
                        exercise.primaryMuscle?.displayName,
                        exercise.catalogueDefaults.primaryMuscle?.displayName,
                    )
                    CustomizationLine(
                        "Equipment",
                        exercise.equipment?.displayName,
                        exercise.catalogueDefaults.equipment?.displayName,
                    )
                    val catalogueSecondary = exercise.catalogueDefaults.secondaryMuscles
                        .joinToString(", ") { it.displayName }
                        .ifBlank { null }
                    val resolvedSecondary = exercise.secondaryMuscles
                        .joinToString(", ") { it.displayName }
                        .ifBlank { null }
                    if (resolvedSecondary != catalogueSecondary) {
                        CustomizationLine("Secondary muscles", resolvedSecondary, catalogueSecondary)
                    }
                }
            } else if (exercise.isCustom) {
                DetailLine("Primary muscle", exercise.primaryMuscle?.displayName)
                DetailLine("Equipment", exercise.equipment?.displayName)
                if (exercise.secondaryMuscles.isNotEmpty()) {
                    DetailLine(
                        "Secondary muscles",
                        exercise.secondaryMuscles.joinToString(", ") { it.displayName },
                    )
                }
            }



            if (exercise.aliases.isNotEmpty()) {
                Spacer(modifier = Modifier.height(FlowSpacing.md))
                FlowSectionLabel("Aliases")
                Spacer(modifier = Modifier.height(FlowSpacing.xxs))
                FlowSupportingText(exercise.aliases.joinToString(" · "))
            }
            Spacer(modifier = Modifier.height(FlowSpacing.lg))
            if (exercise.isCustom) {
                FlowButton(text = "Edit", onClick = onEdit, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(FlowSpacing.sm))
                FlowButton(
                    text = "Delete",
                    onClick = onDelete,
                    variant = FlowButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}



@Composable
private fun CatalogueSection(catalogue: GymLibraryCatalogueDefaults) {
    FlowSectionLabel("Catalogue")
    Spacer(modifier = Modifier.height(FlowSpacing.sm))
    DetailLine("Name", catalogue.displayName)
    DetailLine("Primary muscle", catalogue.primaryMuscle?.displayName)
    DetailLine("Equipment", catalogue.equipment?.displayName)
    if (catalogue.secondaryMuscles.isNotEmpty()) {
        DetailLine(
            "Secondary muscles",
            catalogue.secondaryMuscles.joinToString(", ") { it.displayName },
        )
    }
}



@Composable
private fun CustomizationLine(label: String, resolved: String?, catalogue: String?) {
    if (resolved == catalogue) return
    FlowSectionLabel(label)
    Spacer(modifier = Modifier.height(FlowSpacing.xxs))
    Text(text = resolved ?: "—", style = MaterialTheme.typography.bodyMedium, color = FlowTextPrimary)
    Spacer(modifier = Modifier.height(FlowSpacing.md))
}



@Composable
private fun DetailLine(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    FlowSectionLabel(label)
    Spacer(modifier = Modifier.height(FlowSpacing.xxs))
    Text(text = value, style = MaterialTheme.typography.bodyMedium, color = FlowTextPrimary)
    Spacer(modifier = Modifier.height(FlowSpacing.md))
}



@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomExerciseFormDialog(
    title: String,
    displayName: String,
    primaryMuscle: GymMuscleGroup?,
    secondaryMuscles: List<GymMuscleGroup>,
    equipment: GymEquipment?,
    message: String?,
    onDisplayNameChange: (String) -> Unit,
    onPrimaryMuscleSelected: (GymMuscleGroup?) -> Unit,
    onSecondaryMuscleToggled: (GymMuscleGroup) -> Unit,
    onEquipmentSelected: (GymEquipment?) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    saveLabel: String = "Save",
) {
    val configuration = LocalConfiguration.current
    val dialogShape = MaterialTheme.shapes.large
    val dialogWidth = configuration.screenWidthDp.dp * 0.94f
    val dialogHeight = configuration.screenHeightDp.dp * 0.92f
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .width(dialogWidth)
                .height(dialogHeight)
                .clip(dialogShape)
                .background(FlowSurfaceRaised)
                .border(FlowSizes.hairline, FlowBorder, dialogShape)
                .padding(FlowSpacing.xl)
                .imePadding(),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = FlowTextPrimary,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(modifier = Modifier.height(FlowSpacing.lg))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                ExerciseEditFormContent(
                    displayName = displayName,
                    primaryMuscle = primaryMuscle,
                    secondaryMuscles = secondaryMuscles,
                    equipment = equipment,
                    message = message,
                    onDisplayNameChange = onDisplayNameChange,
                    onPrimaryMuscleSelected = onPrimaryMuscleSelected,
                    onSecondaryMuscleToggled = onSecondaryMuscleToggled,
                    onEquipmentSelected = onEquipmentSelected,
                )
            }
            Spacer(modifier = Modifier.height(FlowSpacing.lg))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FlowButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    variant = FlowButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                )
                FlowButton(
                    text = saveLabel,
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseEditFormContent(
    displayName: String,
    primaryMuscle: GymMuscleGroup?,
    secondaryMuscles: List<GymMuscleGroup>,
    equipment: GymEquipment?,
    message: String?,
    onDisplayNameChange: (String) -> Unit,
    onPrimaryMuscleSelected: (GymMuscleGroup?) -> Unit,
    onSecondaryMuscleToggled: (GymMuscleGroup) -> Unit,
    onEquipmentSelected: (GymEquipment?) -> Unit,
) {
    FlowTextField(
        value = displayName,
        onValueChange = onDisplayNameChange,
        placeholder = "Exercise name",
        singleLine = true,
    )
    if (message != null) {
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        FlowSupportingText(message)
    }
    Spacer(modifier = Modifier.height(FlowSpacing.lg))
    FlowSectionLabel("Primary muscle")
    Spacer(modifier = Modifier.height(FlowSpacing.xs))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(FlowSpacing.xs)) {
        GymMuscleGroup.entries.forEach { muscle ->
            FlowChip(
                label = muscle.displayName,
                selected = primaryMuscle == muscle,
                onClick = {
                    onPrimaryMuscleSelected(if (primaryMuscle == muscle) null else muscle)
                },
            )
        }
    }
    Spacer(modifier = Modifier.height(FlowSpacing.lg))
    FlowSectionLabel("Secondary muscles")
    Spacer(modifier = Modifier.height(FlowSpacing.xs))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(FlowSpacing.xs)) {
        GymMuscleGroup.entries.forEach { muscle ->
            if (muscle == primaryMuscle) return@forEach
            FlowChip(
                label = muscle.displayName,
                selected = muscle in secondaryMuscles,
                onClick = { onSecondaryMuscleToggled(muscle) },
            )
        }
    }
    Spacer(modifier = Modifier.height(FlowSpacing.lg))
    FlowSectionLabel("Equipment")
    Spacer(modifier = Modifier.height(FlowSpacing.xs))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(FlowSpacing.xs)) {
        GymEquipment.entries.forEach { item ->
            FlowChip(
                label = item.displayName,
                selected = equipment == item,
                onClick = {
                    onEquipmentSelected(if (equipment == item) null else item)
                },
            )
        }
    }
}
