package com.workouttracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.*
import androidx.navigation.NavType
import androidx.navigation.compose.*
import com.workouttracker.ui.screens.*
import com.workouttracker.ui.theme.*

sealed class BottomNavScreen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Dashboard : BottomNavScreen("dashboard", "Главная", Icons.Default.Home)
    object Workout : BottomNavScreen("workout_tab", "Тренировка", Icons.Default.FitnessCenter)
    object Body : BottomNavScreen("body", "Тело", Icons.Default.MonitorWeight)
    object Statistics : BottomNavScreen("statistics", "Статистика", Icons.Default.BarChart)
    object Settings : BottomNavScreen("settings", "Настройки", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    BottomNavScreen.Dashboard,
    BottomNavScreen.Workout,
    BottomNavScreen.Body,
    BottomNavScreen.Statistics,
    BottomNavScreen.Settings
)

// Legacy screen routes (used inside settings/navigation)
sealed class Screen(val route: String) {
    object Programs : Screen("programs")
    object ExerciseEdit : Screen("exercise_edit/{programId}/{exerciseId}") {
        fun createRoute(programId: Long, exerciseId: Long = -1L) =
            "exercise_edit/$programId/$exerciseId"
    }
    object ScheduleSettings : Screen("schedule_settings")
    object WorkoutSession : Screen("workout_session/{sessionId}") {
        fun createRoute(sessionId: Long) = "workout_session/$sessionId"
    }
    object History : Screen("history")
    object Calendar : Screen("calendar")
    object PlannedWorkout : Screen("planned_workout/{sessionId}") {
        fun createRoute(sessionId: Long) = "planned_workout/$sessionId"
    }
    object ActiveWorkout : Screen("active_workout/{sessionId}") {
        fun createRoute(sessionId: Long) = "active_workout/$sessionId"
    }
    object WorkoutDetail : Screen("workout_detail/{sessionId}") {
        fun createRoute(sessionId: Long) = "workout_detail/$sessionId"
    }
}

@Composable
fun WorkoutNavGraph(navController: NavHostController) {
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination

    val topLevelRoutes = bottomNavItems.map { it.route }
    val showBottomBar = currentDestination?.route?.let { route ->
        topLevelRoutes.any { route == it } || route == "workout_tab"
    } ?: true

    Scaffold(
        containerColor = ColorBackground,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = ColorSurface,
                    contentColor = ColorOnSurface
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.route == item.route
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    item.icon,
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(item.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(BottomNavScreen.Dashboard.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ColorPrimary,
                                selectedTextColor = ColorPrimary,
                                unselectedIconColor = ColorOnSurface,
                                unselectedTextColor = ColorOnSurface,
                                indicatorColor = ColorPrimary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavScreen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Bottom nav destinations
            composable(BottomNavScreen.Dashboard.route) {
                DashboardScreen(navController = navController)
            }

            composable(BottomNavScreen.Workout.route) {
                WorkoutTabScreen(navController = navController)
            }

            composable(BottomNavScreen.Body.route) {
                BodyTrackerScreen()
            }

            composable(BottomNavScreen.Statistics.route) {
                StatisticsScreen()
            }

            composable(BottomNavScreen.Settings.route) {
                SettingsScreen(navController = navController)
            }

            // Deeper screens
            composable(Screen.Programs.route) {
                ProgramsScreen(navController = navController)
            }

            composable(
                route = Screen.ExerciseEdit.route,
                arguments = listOf(
                    navArgument("programId") { type = NavType.LongType },
                    navArgument("exerciseId") { type = NavType.LongType; defaultValue = -1L }
                )
            ) { backStack ->
                val programId = backStack.arguments?.getLong("programId") ?: return@composable
                val exerciseId = backStack.arguments?.getLong("exerciseId") ?: -1L
                ProgramExerciseEditScreen(
                    programId = programId,
                    exerciseId = if (exerciseId == -1L) null else exerciseId,
                    navController = navController
                )
            }

            composable(Screen.ScheduleSettings.route) {
                ScheduleSettingsScreen(navController = navController)
            }

            composable(Screen.Calendar.route) {
                CalendarScreen(navController = navController)
            }

            composable(Screen.History.route) {
                HistoryScreen(navController = navController)
            }

            composable(
                route = Screen.PlannedWorkout.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { backStack ->
                val sessionId = backStack.arguments?.getLong("sessionId") ?: return@composable
                PlannedWorkoutScreen(sessionId = sessionId, navController = navController)
            }

            composable(
                route = Screen.ActiveWorkout.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { backStack ->
                val sessionId = backStack.arguments?.getLong("sessionId") ?: return@composable
                ActiveWorkoutScreen(sessionId = sessionId, navController = navController)
            }

            composable(
                route = Screen.WorkoutSession.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { backStack ->
                val sessionId = backStack.arguments?.getLong("sessionId") ?: return@composable
                WorkoutSessionScreen(sessionId = sessionId, navController = navController)
            }

            composable(
                route = Screen.WorkoutDetail.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { backStack ->
                val sessionId = backStack.arguments?.getLong("sessionId") ?: return@composable
                WorkoutDetailScreen(sessionId = sessionId, navController = navController)
            }
        }
    }
}
