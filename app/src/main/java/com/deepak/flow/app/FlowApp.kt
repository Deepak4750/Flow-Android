package com.deepak.flow.app

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.deepak.flow.CreateReminderViewModelFactory
import com.deepak.flow.FlowApplication
import com.deepak.flow.FlowViewModelFactory
import com.deepak.flow.app.navigation.FeatureSettingsViewModel
import com.deepak.flow.app.navigation.FlowDrawerDestination
import com.deepak.flow.app.navigation.FlowRoute
import com.deepak.flow.app.navigation.navigateFromDrawer
import com.deepak.flow.app.navigation.navigateToActiveWorkout
import com.deepak.flow.app.theme.FlowTheme
import com.deepak.flow.core.gym.GymWorkoutType
import com.deepak.flow.core.model.OnboardingGate
import com.deepak.flow.core.model.UserProfile
import com.deepak.flow.core.model.onboardingGate
import com.deepak.flow.core.update.AppUpdateViewModel
import com.deepak.flow.core.widget.WidgetLaunch
import com.deepak.flow.core.widget.widgetDestinationOrNull
import com.deepak.flow.feature.gym.presentation.FreeWorkoutScreen
import com.deepak.flow.feature.gym.presentation.FreeWorkoutViewModel
import com.deepak.flow.feature.gym.presentation.FreeWorkoutViewModelFactory
import com.deepak.flow.feature.gym.presentation.GymChoiceScreen
import com.deepak.flow.feature.gym.presentation.GymHomeViewModel
import com.deepak.flow.feature.gym.presentation.GymHomeViewModelFactory
import com.deepak.flow.feature.gym.presentation.GymRoutineScreen
import com.deepak.flow.feature.gym.presentation.RoutineBuilderScreen
import com.deepak.flow.feature.gym.presentation.RoutineBuilderViewModel
import com.deepak.flow.feature.gym.presentation.RoutineBuilderViewModelFactory
import com.deepak.flow.feature.gym.presentation.RoutineCatalogScreen
import com.deepak.flow.feature.gym.presentation.RoutineCatalogViewModel
import com.deepak.flow.feature.gym.presentation.RoutineCatalogViewModelFactory
import com.deepak.flow.feature.home.presentation.HomeScreen
import com.deepak.flow.feature.home.presentation.HomeViewModel
import com.deepak.flow.feature.history.presentation.HistoryDayScreen
import com.deepak.flow.feature.history.presentation.HistoryDayViewModel
import com.deepak.flow.feature.history.presentation.HistoryDayViewModelFactory
import com.deepak.flow.feature.history.presentation.HistoryGymDayScreen
import com.deepak.flow.feature.history.presentation.HistoryGymDayViewModel
import com.deepak.flow.feature.history.presentation.HistoryGymEditExerciseScreen
import com.deepak.flow.feature.history.presentation.HistoryGymEditExerciseViewModel
import com.deepak.flow.feature.history.presentation.HistoryGymViewModelFactory
import com.deepak.flow.feature.history.presentation.HistoryGymWorkoutScreen
import com.deepak.flow.feature.history.presentation.HistoryGymWorkoutViewModel
import com.deepak.flow.feature.history.presentation.HistoryScreen
import com.deepak.flow.feature.history.presentation.HistoryTasksDetailScreen
import com.deepak.flow.feature.history.presentation.HistoryTasksViewModel
import com.deepak.flow.feature.history.presentation.HistoryViewModel
import com.deepak.flow.feature.history.presentation.HistoryWaterDetailScreen
import com.deepak.flow.feature.history.presentation.HistoryWaterViewModel
import com.deepak.flow.feature.onboarding.presentation.OnboardingScreen
import com.deepak.flow.feature.onboarding.presentation.OnboardingViewModel
import com.deepak.flow.feature.reminder.presentation.CreateReminderScreen
import com.deepak.flow.feature.reminder.presentation.CreateReminderViewModel
import com.deepak.flow.feature.reminder.presentation.RemindersScreen
import com.deepak.flow.feature.settings.presentation.AboutScreen
import com.deepak.flow.feature.settings.presentation.AppUpdatePrompt
import com.deepak.flow.feature.settings.presentation.SettingsScreen
import com.deepak.flow.feature.settings.presentation.SettingsViewModel
import kotlinx.coroutines.flow.map
import com.deepak.flow.feature.water.presentation.WaterScreen

@Composable
fun FlowApp(
    modifier: Modifier = Modifier,
    launchIntent: Intent? = null,
    onLaunchIntentConsumed: () -> Unit = {},
) {
    val app = LocalContext.current.applicationContext as FlowApplication
    val factory = FlowViewModelFactory(app)
    val profileLoad by app.profileRepository.observeProfile()
        .map<UserProfile?, ProfileLoad> { ProfileLoad.Loaded(it) }
        .collectAsStateWithLifecycle(initialValue = ProfileLoad.Loading)
    val gate = when (val load = profileLoad) {
        ProfileLoad.Loading -> onboardingGate(profileLoaded = false, profile = null)
        is ProfileLoad.Loaded -> onboardingGate(profileLoaded = true, profile = load.profile)
    }

    FlowTheme {
        when (gate) {
            OnboardingGate.Loading -> {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                )
            }
            OnboardingGate.ShowTutorial -> {
                val viewModel: OnboardingViewModel = viewModel(factory = factory)
                OnboardingScreen(
                    viewModel = viewModel,
                    modifier = modifier,
                )
            }
            OnboardingGate.Ready -> {
                val profile = (profileLoad as ProfileLoad.Loaded).profile
                    ?: return@FlowTheme
                val remindersEnabled = profile.remindersEnabled
                val waterEnabled = profile.waterEnabled
                val gymEnabled = profile.gymEnabled
                val navController = rememberNavController()
            val updateViewModel: AppUpdateViewModel = viewModel(factory = factory)
            val featureViewModel: FeatureSettingsViewModel = viewModel(factory = factory)
            val featureState by featureViewModel.uiState.collectAsStateWithLifecycle()
            val onDrawerDestination = remember(navController) {
                { destination: FlowDrawerDestination ->
                    navController.navigateFromDrawer(destination)
                }
            }
            // NavHost always starts at Home. Profile is already loaded here, so
            // feature flags are the stored values, not new-user defaults.
            LaunchedEffect(launchIntent) {
                val destinationKey = launchIntent?.widgetDestinationOrNull()
                when (destinationKey) {
                    WidgetLaunch.DEST_WATER -> navController.navigateFromDrawer(FlowDrawerDestination.WATER)
                    WidgetLaunch.DEST_REMINDERS -> navController.navigateFromDrawer(FlowDrawerDestination.REMINDERS)
                    WidgetLaunch.DEST_GYM_FREE_WORKOUT -> navController.navigateToActiveWorkout(GymWorkoutType.FREE)
                    WidgetLaunch.DEST_GYM_ROUTINE_WORKOUT -> navController.navigateToActiveWorkout(GymWorkoutType.ROUTINE)
                }
                if (launchIntent != null) {
                    onLaunchIntentConsumed()
                }
            }
            LifecycleResumeEffect(Unit) {
                updateViewModel.onAppOpened()
                onPauseOrDispose { }
            }
            AppUpdatePrompt(updateViewModel)
            NavHost(
                navController = navController,
                startDestination = FlowRoute.Home,
                modifier = modifier,
            ) {
                composable<FlowRoute.Home> {
                    val viewModel: HomeViewModel = viewModel(factory = factory)
                    val gymHomeFactory = remember { GymHomeViewModelFactory(app) }
                    val gymHomeViewModel: GymHomeViewModel = viewModel(factory = gymHomeFactory)
                    HomeScreen(
                        viewModel = viewModel,
                        gymViewModel = gymHomeViewModel,
                        remindersEnabled = remindersEnabled,
                        waterEnabled = waterEnabled,
                        gymEnabled = gymEnabled,
                        waterGoalMl = featureState.waterGoalMl,
                        waterIntakeMl = featureState.waterIntakeMl,
                        waterCustomQuickAddsMl = featureState.waterCustomQuickAddsMl,
                        onRemindersEnabledChange = featureViewModel::setRemindersEnabled,
                        onWaterEnabledChange = featureViewModel::setWaterEnabled,
                        onGymEnabledChange = featureViewModel::setGymEnabled,
                        onDestinationClick = onDrawerDestination,
                        onEditReminder = { id ->
                            navController.navigate(FlowRoute.EditReminder(reminderId = id))
                        },
                        onAddWaterMl = featureViewModel::addWaterMl,
                        onOpenWater = { onDrawerDestination(FlowDrawerDestination.WATER) },
                        onOpenGymRoutine = { navController.navigate(FlowRoute.GymRoutine) },
                    )
                }
                composable<FlowRoute.Reminders> {
                    val viewModel: HomeViewModel = viewModel(factory = factory)
                    RemindersScreen(
                        viewModel = viewModel,
                        remindersEnabled = remindersEnabled,
                        waterEnabled = waterEnabled,
                        gymEnabled = gymEnabled,
                        onRemindersEnabledChange = featureViewModel::setRemindersEnabled,
                        onWaterEnabledChange = featureViewModel::setWaterEnabled,
                        onGymEnabledChange = featureViewModel::setGymEnabled,
                        onDestinationClick = onDrawerDestination,
                        onCreateReminder = { navController.navigate(FlowRoute.CreateReminder) },
                        onEditReminder = { id ->
                            navController.navigate(FlowRoute.EditReminder(reminderId = id))
                        },
                    )
                }
                composable<FlowRoute.Water> {
                    WaterScreen(
                        userName = featureState.profileName,
                        waterEnabled = waterEnabled,
                        gymEnabled = gymEnabled,
                        waterGoalMl = featureState.waterGoalMl,
                        waterBottleStyleIndex = featureState.waterBottleStyleIndex,
                        waterIntakeMl = featureState.waterIntakeMl,
                        canUndoWater = featureState.canUndoWater,
                        waterCustomQuickAddsMl = featureState.waterCustomQuickAddsMl,
                        remindersEnabled = remindersEnabled,
                        waterRemindersEnabled = featureState.waterRemindersEnabled,
                        waterReminderIntervalMinutes = featureState.waterReminderIntervalMinutes,
                        waterActiveHoursStartMinutes = featureState.waterActiveHoursStartMinutes,
                        waterActiveHoursEndMinutes = featureState.waterActiveHoursEndMinutes,
                        onRemindersEnabledChange = featureViewModel::setRemindersEnabled,
                        onWaterEnabledChange = featureViewModel::setWaterEnabled,
                        onGymEnabledChange = featureViewModel::setGymEnabled,
                        onGoalSet = featureViewModel::setWaterGoalMl,
                        onBottleStyleSet = featureViewModel::setWaterBottleStyle,
                        onSaveSettings = featureViewModel::saveWaterSettings,
                        onAddWater = featureViewModel::addWaterMl,
                        onAddCustomWater = featureViewModel::addCustomWaterQuickAdd,
                        onRemoveCustomWater = featureViewModel::removeWaterCustomQuickAdd,
                        onUndoWater = featureViewModel::undoWater,
                        onWaterRemindersEnabledChange = featureViewModel::setWaterRemindersEnabled,
                        onWaterReminderIntervalInput = featureViewModel::onWaterReminderIntervalInput,
                        onIncrementWaterReminderInterval = featureViewModel::incrementWaterReminderInterval,
                        onDecrementWaterReminderInterval = featureViewModel::decrementWaterReminderInterval,
                        onWaterActiveHoursStartChange = featureViewModel::setWaterActiveHoursStart,
                        onWaterActiveHoursEndChange = featureViewModel::setWaterActiveHoursEnd,
                        onDestinationClick = onDrawerDestination,
                    )
                }
                composable<FlowRoute.Gym> {
                    val gymHomeFactory = remember { GymHomeViewModelFactory(app) }
                    val gymHomeViewModel: GymHomeViewModel = viewModel(factory = gymHomeFactory)
                    GymChoiceScreen(
                        viewModel = gymHomeViewModel,
                        userName = featureState.profileName,
                        remindersEnabled = remindersEnabled,
                        waterEnabled = waterEnabled,
                        gymEnabled = gymEnabled,
                        onRemindersEnabledChange = featureViewModel::setRemindersEnabled,
                        onWaterEnabledChange = featureViewModel::setWaterEnabled,
                        onGymEnabledChange = featureViewModel::setGymEnabled,
                        onDestinationClick = onDrawerDestination,
                        onRoutine = { navController.navigate(FlowRoute.GymRoutine) },
                        onFreeWorkout = { navController.navigate(FlowRoute.GymFreeWorkout) },
                        onContinueWorkout = { type ->
                            when (type) {
                                GymWorkoutType.ROUTINE ->
                                    navController.navigate(FlowRoute.GymRoutineWorkout)
                                GymWorkoutType.FREE ->
                                    navController.navigate(FlowRoute.GymFreeWorkout)
                            }
                        },
                    )
                }
                composable<FlowRoute.GymRoutine> {
                    val gymHomeFactory = remember { GymHomeViewModelFactory(app) }
                    val gymHomeViewModel: GymHomeViewModel = viewModel(factory = gymHomeFactory)
                    GymRoutineScreen(
                        viewModel = gymHomeViewModel,
                        userName = featureState.profileName,
                        remindersEnabled = remindersEnabled,
                        waterEnabled = waterEnabled,
                        gymEnabled = gymEnabled,
                        onRemindersEnabledChange = featureViewModel::setRemindersEnabled,
                        onWaterEnabledChange = featureViewModel::setWaterEnabled,
                        onGymEnabledChange = featureViewModel::setGymEnabled,
                        onDestinationClick = onDrawerDestination,
                        onBack = { navController.popBackStack() },
                        onOpenRoutines = { navController.navigate(FlowRoute.GymRoutineCatalog) },
                        onEditRoutine = { routineId ->
                            navController.navigate(FlowRoute.GymRoutineBuilder(routineId = routineId))
                        },
                        onStartRoutine = { navController.navigate(FlowRoute.GymRoutineWorkout) },
                    )
                }
                composable<FlowRoute.GymRoutineCatalog> {
                    val catalogFactory = remember { RoutineCatalogViewModelFactory(app) }
                    val catalogViewModel: RoutineCatalogViewModel = viewModel(factory = catalogFactory)
                    RoutineCatalogScreen(
                        viewModel = catalogViewModel,
                        userName = featureState.profileName,
                        remindersEnabled = remindersEnabled,
                        waterEnabled = waterEnabled,
                        gymEnabled = gymEnabled,
                        onRemindersEnabledChange = featureViewModel::setRemindersEnabled,
                        onWaterEnabledChange = featureViewModel::setWaterEnabled,
                        onGymEnabledChange = featureViewModel::setGymEnabled,
                        onDestinationClick = onDrawerDestination,
                        onBack = { navController.popBackStack() },
                        onOpenRoutine = { routineId ->
                            navController.navigate(FlowRoute.GymRoutineBuilder(routineId = routineId))
                        },
                        onNewRoutine = {
                            navController.navigate(FlowRoute.GymRoutineBuilder(routineId = 0L))
                        },
                    )
                }
                composable<FlowRoute.GymRoutineBuilder> { entry ->
                    val args = entry.toRoute<FlowRoute.GymRoutineBuilder>()
                    val editingRoutineId = args.routineId.takeIf { it > 0L }
                    val builderFactory = remember(editingRoutineId) {
                        RoutineBuilderViewModelFactory(app, editingRoutineId)
                    }
                    val builderViewModel: RoutineBuilderViewModel = viewModel(
                        key = "routineBuilder-${args.routineId}",
                        factory = builderFactory,
                    )
                    RoutineBuilderScreen(
                        viewModel = builderViewModel,
                        userName = featureState.profileName,
                        remindersEnabled = remindersEnabled,
                        waterEnabled = waterEnabled,
                        gymEnabled = gymEnabled,
                        onRemindersEnabledChange = featureViewModel::setRemindersEnabled,
                        onWaterEnabledChange = featureViewModel::setWaterEnabled,
                        onGymEnabledChange = featureViewModel::setGymEnabled,
                        onDestinationClick = onDrawerDestination,
                        onLeave = { navController.popBackStack() },
                    )
                }
                composable<FlowRoute.GymFreeWorkout> {
                    val freeFactory = remember { FreeWorkoutViewModelFactory(app, GymWorkoutType.FREE) }
                    val viewModel: FreeWorkoutViewModel = viewModel(
                        key = "freeWorkout",
                        factory = freeFactory,
                    )
                    FreeWorkoutScreen(
                        viewModel = viewModel,
                        userName = featureState.profileName,
                        remindersEnabled = remindersEnabled,
                        waterEnabled = waterEnabled,
                        gymEnabled = gymEnabled,
                        onRemindersEnabledChange = featureViewModel::setRemindersEnabled,
                        onWaterEnabledChange = featureViewModel::setWaterEnabled,
                        onGymEnabledChange = featureViewModel::setGymEnabled,
                        onDestinationClick = onDrawerDestination,
                        onLeave = {
                            navController.popBackStack(FlowRoute.Gym, inclusive = false)
                        },
                    )
                }
                composable<FlowRoute.GymRoutineWorkout> {
                    val routineFactory = remember {
                        FreeWorkoutViewModelFactory(app, GymWorkoutType.ROUTINE)
                    }
                    val viewModel: FreeWorkoutViewModel = viewModel(
                        key = "routineWorkout",
                        factory = routineFactory,
                    )
                    FreeWorkoutScreen(
                        viewModel = viewModel,
                        userName = featureState.profileName,
                        remindersEnabled = remindersEnabled,
                        waterEnabled = waterEnabled,
                        gymEnabled = gymEnabled,
                        onRemindersEnabledChange = featureViewModel::setRemindersEnabled,
                        onWaterEnabledChange = featureViewModel::setWaterEnabled,
                        onGymEnabledChange = featureViewModel::setGymEnabled,
                        onDestinationClick = onDrawerDestination,
                        onLeave = {
                            navController.popBackStack(FlowRoute.Gym, inclusive = false)
                        },
                    )
                }
                composable<FlowRoute.History> {
                    val viewModel: HistoryViewModel = viewModel(factory = factory)
                    HistoryScreen(
                        viewModel = viewModel,
                        userName = featureState.profileName,
                        remindersEnabled = remindersEnabled,
                        waterEnabled = waterEnabled,
                        gymEnabled = gymEnabled,
                        onRemindersEnabledChange = featureViewModel::setRemindersEnabled,
                        onWaterEnabledChange = featureViewModel::setWaterEnabled,
                        onGymEnabledChange = featureViewModel::setGymEnabled,
                        onDestinationClick = onDrawerDestination,
                        onDayClick = { dateEpochDay ->
                            navController.navigate(FlowRoute.HistoryDay(dateEpochDay))
                        },
                    )
                }
                composable<FlowRoute.HistoryDay> { backStackEntry ->
                    val route = backStackEntry.toRoute<FlowRoute.HistoryDay>()
                    val dayFactory = remember(route.dateEpochDay) {
                        HistoryDayViewModelFactory(app, route.dateEpochDay)
                    }
                    val viewModel: HistoryDayViewModel = viewModel(
                        factory = dayFactory,
                        key = "history-day-${route.dateEpochDay}",
                    )
                    HistoryDayScreen(
                        viewModel = viewModel,
                        userName = featureState.profileName,
                        remindersEnabled = remindersEnabled,
                        waterEnabled = waterEnabled,
                        gymEnabled = gymEnabled,
                        onRemindersEnabledChange = featureViewModel::setRemindersEnabled,
                        onWaterEnabledChange = featureViewModel::setWaterEnabled,
                        onGymEnabledChange = featureViewModel::setGymEnabled,
                        onDestinationClick = onDrawerDestination,
                        onBack = { navController.popBackStack() },
                        onTasksClick = {
                            navController.navigate(FlowRoute.HistoryTasks(route.dateEpochDay))
                        },
                        onWaterClick = {
                            navController.navigate(FlowRoute.HistoryWater(route.dateEpochDay))
                        },
                        onGymClick = {
                            navController.navigate(FlowRoute.HistoryGym(route.dateEpochDay))
                        },
                    )
                }
                composable<FlowRoute.HistoryTasks> { backStackEntry ->
                    val route = backStackEntry.toRoute<FlowRoute.HistoryTasks>()
                    val dayFactory = remember(route.dateEpochDay) {
                        HistoryDayViewModelFactory(app, route.dateEpochDay)
                    }
                    val viewModel: HistoryTasksViewModel = viewModel(
                        factory = dayFactory,
                        key = "history-tasks-${route.dateEpochDay}",
                    )
                    HistoryTasksDetailScreen(
                        viewModel = viewModel,
                        userName = featureState.profileName,
                        remindersEnabled = remindersEnabled,
                        waterEnabled = waterEnabled,
                        gymEnabled = gymEnabled,
                        onRemindersEnabledChange = featureViewModel::setRemindersEnabled,
                        onWaterEnabledChange = featureViewModel::setWaterEnabled,
                        onGymEnabledChange = featureViewModel::setGymEnabled,
                        onDestinationClick = onDrawerDestination,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable<FlowRoute.HistoryWater> { backStackEntry ->
                    val route = backStackEntry.toRoute<FlowRoute.HistoryWater>()
                    val dayFactory = remember(route.dateEpochDay) {
                        HistoryDayViewModelFactory(app, route.dateEpochDay)
                    }
                    val viewModel: HistoryWaterViewModel = viewModel(
                        factory = dayFactory,
                        key = "history-water-${route.dateEpochDay}",
                    )
                    HistoryWaterDetailScreen(
                        viewModel = viewModel,
                        userName = featureState.profileName,
                        remindersEnabled = remindersEnabled,
                        waterEnabled = waterEnabled,
                        gymEnabled = gymEnabled,
                        onRemindersEnabledChange = featureViewModel::setRemindersEnabled,
                        onWaterEnabledChange = featureViewModel::setWaterEnabled,
                        onGymEnabledChange = featureViewModel::setGymEnabled,
                        onDestinationClick = onDrawerDestination,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable<FlowRoute.HistoryGym> { backStackEntry ->
                    val route = backStackEntry.toRoute<FlowRoute.HistoryGym>()
                    val gymFactory = remember(route.dateEpochDay) {
                        HistoryGymViewModelFactory(app, dateEpochDay = route.dateEpochDay)
                    }
                    val viewModel: HistoryGymDayViewModel = viewModel(
                        factory = gymFactory,
                        key = "history-gym-${route.dateEpochDay}",
                    )
                    HistoryGymDayScreen(
                        viewModel = viewModel,
                        userName = featureState.profileName,
                        remindersEnabled = remindersEnabled,
                        waterEnabled = waterEnabled,
                        gymEnabled = gymEnabled,
                        onRemindersEnabledChange = featureViewModel::setRemindersEnabled,
                        onWaterEnabledChange = featureViewModel::setWaterEnabled,
                        onGymEnabledChange = featureViewModel::setGymEnabled,
                        onDestinationClick = onDrawerDestination,
                        onBack = { navController.popBackStack() },
                        onWorkoutClick = { workoutId ->
                            navController.navigate(FlowRoute.HistoryGymWorkout(workoutId))
                        },
                    )
                }
                composable<FlowRoute.HistoryGymWorkout> { backStackEntry ->
                    val route = backStackEntry.toRoute<FlowRoute.HistoryGymWorkout>()
                    val gymFactory = remember(route.workoutId) {
                        HistoryGymViewModelFactory(app, workoutId = route.workoutId)
                    }
                    val viewModel: HistoryGymWorkoutViewModel = viewModel(
                        factory = gymFactory,
                        key = "history-gym-workout-${route.workoutId}",
                    )
                    HistoryGymWorkoutScreen(
                        viewModel = viewModel,
                        userName = featureState.profileName,
                        remindersEnabled = remindersEnabled,
                        waterEnabled = waterEnabled,
                        gymEnabled = gymEnabled,
                        onRemindersEnabledChange = featureViewModel::setRemindersEnabled,
                        onWaterEnabledChange = featureViewModel::setWaterEnabled,
                        onGymEnabledChange = featureViewModel::setGymEnabled,
                        onDestinationClick = onDrawerDestination,
                        onBack = { navController.popBackStack() },
                        onEditExercise = { exerciseId ->
                            navController.navigate(
                                FlowRoute.HistoryGymEditExercise(
                                    workoutId = route.workoutId,
                                    exerciseId = exerciseId,
                                ),
                            )
                        },
                    )
                }
                composable<FlowRoute.HistoryGymEditExercise> { backStackEntry ->
                    val route = backStackEntry.toRoute<FlowRoute.HistoryGymEditExercise>()
                    val gymFactory = remember(route.workoutId, route.exerciseId) {
                        HistoryGymViewModelFactory(
                            app,
                            workoutId = route.workoutId,
                            exerciseId = route.exerciseId,
                        )
                    }
                    val viewModel: HistoryGymEditExerciseViewModel = viewModel(
                        factory = gymFactory,
                        key = "history-gym-edit-${route.workoutId}-${route.exerciseId}",
                    )
                    HistoryGymEditExerciseScreen(
                        viewModel = viewModel,
                        userName = featureState.profileName,
                        remindersEnabled = remindersEnabled,
                        waterEnabled = waterEnabled,
                        gymEnabled = gymEnabled,
                        onRemindersEnabledChange = featureViewModel::setRemindersEnabled,
                        onWaterEnabledChange = featureViewModel::setWaterEnabled,
                        onGymEnabledChange = featureViewModel::setGymEnabled,
                        onDestinationClick = onDrawerDestination,
                        onLeave = { navController.popBackStack() },
                    )
                }
                composable<FlowRoute.CreateReminder> {
                    val viewModel: CreateReminderViewModel = viewModel(
                        factory = CreateReminderViewModelFactory(app),
                    )
                    CreateReminderScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() },
                    )
                }
                composable<FlowRoute.Settings> {
                    val viewModel: SettingsViewModel = viewModel(factory = factory)
                    SettingsScreen(
                        viewModel = viewModel,
                        updateViewModel = updateViewModel,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable<FlowRoute.About> {
                    AboutScreen(
                        updateViewModel = updateViewModel,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable<FlowRoute.EditReminder> { backStackEntry ->
                    val route = backStackEntry.toRoute<FlowRoute.EditReminder>()
                    val viewModel: CreateReminderViewModel = viewModel(
                        factory = CreateReminderViewModelFactory(app, route.reminderId),
                        key = "edit-${route.reminderId}",
                    )
                    CreateReminderScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() },
                    )
                }
            }
            }
        }
    }
}

private sealed interface ProfileLoad {
    data object Loading : ProfileLoad
    data class Loaded(val profile: UserProfile?) : ProfileLoad
}
