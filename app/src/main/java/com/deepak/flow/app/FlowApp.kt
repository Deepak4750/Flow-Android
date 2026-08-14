package com.deepak.flow.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.deepak.flow.CreateReminderViewModelFactory
import com.deepak.flow.FlowApplication
import com.deepak.flow.FlowViewModelFactory
import com.deepak.flow.app.navigation.FlowRoute
import com.deepak.flow.app.theme.FlowTheme
import com.deepak.flow.feature.home.presentation.HomeScreen
import com.deepak.flow.feature.home.presentation.HomeViewModel
import com.deepak.flow.feature.onboarding.presentation.OnboardingScreen
import com.deepak.flow.feature.onboarding.presentation.OnboardingViewModel
import com.deepak.flow.feature.reminder.presentation.CreateReminderScreen
import com.deepak.flow.feature.reminder.presentation.CreateReminderViewModel
import com.deepak.flow.feature.settings.presentation.AboutScreen
import com.deepak.flow.feature.settings.presentation.SettingsScreen
import com.deepak.flow.feature.settings.presentation.SettingsViewModel

@Composable
fun FlowApp(modifier: Modifier = Modifier) {
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
            NavHost(
                navController = navController,
                startDestination = FlowRoute.Home,
                modifier = modifier,
            ) {
                composable<FlowRoute.Home> {
                    val viewModel: HomeViewModel = viewModel(factory = factory)
                    HomeScreen(
                        viewModel = viewModel,
                        onCreateReminder = { navController.navigate(FlowRoute.CreateReminder) },
                        onEditReminder = { id ->
                            navController.navigate(FlowRoute.EditReminder(reminderId = id))
                        },
                        onOpenSettings = { navController.navigate(FlowRoute.Settings) },
                        onOpenAbout = { navController.navigate(FlowRoute.About) },
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
                        onBack = { navController.popBackStack() },
                    )
                }
                composable<FlowRoute.About> {
                    AboutScreen(onBack = { navController.popBackStack() })
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
