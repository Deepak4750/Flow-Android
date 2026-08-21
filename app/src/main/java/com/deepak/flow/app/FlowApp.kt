package com.deepak.flow.app

import android.content.Intent
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
import com.deepak.flow.app.navigation.FlowPlaceholderScreen
import com.deepak.flow.app.navigation.FlowRoute
import com.deepak.flow.app.navigation.navigateFromDrawer
import com.deepak.flow.app.theme.FlowTheme
import com.deepak.flow.core.update.AppUpdateViewModel
import com.deepak.flow.core.widget.WidgetLaunch
import com.deepak.flow.core.widget.widgetDestinationOrNull
import com.deepak.flow.feature.home.presentation.HomeScreen
import com.deepak.flow.feature.home.presentation.HomeViewModel
import com.deepak.flow.feature.onboarding.presentation.OnboardingScreen
import com.deepak.flow.feature.onboarding.presentation.OnboardingViewModel
import com.deepak.flow.feature.reminder.presentation.CreateReminderScreen
import com.deepak.flow.feature.reminder.presentation.CreateReminderViewModel
import com.deepak.flow.feature.reminder.presentation.RemindersScreen
import com.deepak.flow.feature.settings.presentation.AboutScreen
import com.deepak.flow.feature.settings.presentation.AppUpdatePrompt
import com.deepak.flow.feature.settings.presentation.SettingsScreen
import com.deepak.flow.feature.settings.presentation.SettingsViewModel
import com.deepak.flow.feature.water.presentation.WaterScreen

@Composable
fun FlowApp(
    modifier: Modifier = Modifier,
    launchIntent: Intent? = null,
    onLaunchIntentConsumed: () -> Unit = {},
) {
    val app = LocalContext.current.applicationContext as FlowApplication
    val factory = FlowViewModelFactory(app)
    val onboardingComplete by app.profileRepository.isOnboardingComplete()
        .collectAsStateWithLifecycle(initialValue = false)

    FlowTheme {
        if (!onboardingComplete) {
            val viewModel: OnboardingViewModel = viewModel(factory = factory)
            OnboardingScreen(
                viewModel = viewModel,
                modifier = modifier,
            )
        } else {
            val navController = rememberNavController()
            val updateViewModel: AppUpdateViewModel = viewModel(factory = factory)
            val featureViewModel: FeatureSettingsViewModel = viewModel(factory = factory)
            val featureState by featureViewModel.uiState.collectAsStateWithLifecycle()
            val onDrawerDestination = remember(navController) {
                { destination: FlowDrawerDestination ->
                    navController.navigateFromDrawer(destination)
                }
            }
            // v83 behavior: NavHost always starts at Home; widget/notification
            // extras navigate via LaunchedEffect. Do not gate on featureState here:
            // cold start uses waterEnabled=false as the StateFlow initial value, which
            // wrongly sent users to Home and cleared the intent before profile loaded.
            // Warm start already had real profile data, so it looked fine.
            LaunchedEffect(launchIntent) {
                val destination = when (launchIntent?.widgetDestinationOrNull()) {
                    WidgetLaunch.DEST_WATER -> FlowDrawerDestination.WATER
                    WidgetLaunch.DEST_REMINDERS -> FlowDrawerDestination.REMINDERS
                    else -> null
                }
                if (destination != null) {
                    navController.navigateFromDrawer(destination)
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
                    HomeScreen(
                        viewModel = viewModel,
                        remindersEnabled = featureState.remindersEnabled,
                        waterEnabled = featureState.waterEnabled,
                        onRemindersEnabledChange = featureViewModel::setRemindersEnabled,
                        onWaterEnabledChange = featureViewModel::setWaterEnabled,
                        onDestinationClick = onDrawerDestination,
                    )
                }
                composable<FlowRoute.Reminders> {
                    val viewModel: HomeViewModel = viewModel(factory = factory)
                    RemindersScreen(
                        viewModel = viewModel,
                        remindersEnabled = featureState.remindersEnabled,
                        waterEnabled = featureState.waterEnabled,
                        onRemindersEnabledChange = featureViewModel::setRemindersEnabled,
                        onWaterEnabledChange = featureViewModel::setWaterEnabled,
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
                        waterEnabled = featureState.waterEnabled,
                        waterGoalMl = featureState.waterGoalMl,
                        waterBottleStyleIndex = featureState.waterBottleStyleIndex,
                        waterIntakeMl = featureState.waterIntakeMl,
                        canUndoWater = featureState.canUndoWater,
                        waterCustomQuickAddsMl = featureState.waterCustomQuickAddsMl,
                        remindersEnabled = featureState.remindersEnabled,
                        waterRemindersEnabled = featureState.waterRemindersEnabled,
                        waterReminderIntervalMinutes = featureState.waterReminderIntervalMinutes,
                        waterActiveHoursStartMinutes = featureState.waterActiveHoursStartMinutes,
                        waterActiveHoursEndMinutes = featureState.waterActiveHoursEndMinutes,
                        onRemindersEnabledChange = featureViewModel::setRemindersEnabled,
                        onWaterEnabledChange = featureViewModel::setWaterEnabled,
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
                    FlowPlaceholderScreen(
                        selected = FlowDrawerDestination.GYM,
                        userName = featureState.profileName,
                        remindersEnabled = featureState.remindersEnabled,
                        waterEnabled = featureState.waterEnabled,
                        onRemindersEnabledChange = featureViewModel::setRemindersEnabled,
                        onWaterEnabledChange = featureViewModel::setWaterEnabled,
                        onDestinationClick = onDrawerDestination,
                    )
                }
                composable<FlowRoute.History> {
                    FlowPlaceholderScreen(
                        selected = FlowDrawerDestination.HISTORY,
                        userName = featureState.profileName,
                        remindersEnabled = featureState.remindersEnabled,
                        waterEnabled = featureState.waterEnabled,
                        onRemindersEnabledChange = featureViewModel::setRemindersEnabled,
                        onWaterEnabledChange = featureViewModel::setWaterEnabled,
                        onDestinationClick = onDrawerDestination,
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
