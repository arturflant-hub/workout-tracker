package com.workouttracker.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.*
import androidx.navigation.NavType
import androidx.navigation.compose.*
import android.content.Context
import com.workouttracker.data.db.dao.UserDao
import com.workouttracker.ui.components.LocalTopToastState
import com.workouttracker.ui.components.TopToastHost
import com.workouttracker.ui.components.rememberTopToastState
import com.workouttracker.ui.screens.*
import com.workouttracker.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp

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
    object Backup : Screen("backup")
    object Onboarding : Screen("onboarding")
    object FeatureOnboarding : Screen("feature_onboarding")
}

@Composable
fun WorkoutNavGraph(navController: NavHostController, userDao: UserDao) {
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination

    // Check if user exists for conditional start
    var startRoute by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val user = userDao.getUserOnce()
        startRoute = if (user == null) Screen.Onboarding.route else BottomNavScreen.Dashboard.route
    }

    val topToastState = rememberTopToastState()

    if (startRoute == null) {
        // Loading state while checking user
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ColorPrimary)
        }
        return
    }

    val topLevelRoutes = bottomNavItems.map { it.route }
    val showBottomBar = currentDestination?.route?.let { route ->
        topLevelRoutes.any { route == it } || route == "workout_tab"
    } ?: (startRoute != Screen.Onboarding.route)

    CompositionLocalProvider(LocalTopToastState provides topToastState) {
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
                            label = {
                                Text(
                                    item.title,
                                    maxLines = 1,
                                    softWrap = false,
                                    fontSize = 11.sp
                                )
                            },
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
        Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startRoute!!,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Onboarding
            composable(Screen.Onboarding.route) {
                val context = LocalContext.current
                OnboardingScreen(
                    onComplete = {
                        val prefs = context.getSharedPreferences("workout_prefs", Context.MODE_PRIVATE)
                        val seen = prefs.getBoolean("feature_onboarding_seen", false)
                        val target = if (!seen) Screen.FeatureOnboarding.route else BottomNavScreen.Dashboard.route
                        navController.navigate(target) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

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
                StatisticsScreen(navController = navController)
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

            composable(Screen.Backup.route) {
                BackupScreen(navController = navController)
            }

            composable(Screen.FeatureOnboarding.route) {
                val context = LocalContext.current
                FeatureOnboardingScreen(
                    onFinish = {
                        context.getSharedPreferences("workout_prefs", Context.MODE_PRIVATE)
                            .edit().putBoolean("feature_onboarding_seen", true).apply()
                        val popped = navController.popBackStack(BottomNavScreen.Dashboard.route, inclusive = false)
                        if (!popped) {
                            navController.navigate(BottomNavScreen.Dashboard.route) {
                                popUpTo(Screen.FeatureOnboarding.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(
                route = Screen.WorkoutDetail.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { backStack ->
                val sessionId = backStack.arguments?.getLong("sessionId") ?: return@composable
                WorkoutDetailScreen(sessionId = sessionId, navController = navController)
            }
        }
        TopToastHost(state = topToastState)
        }
    }
    } // CompositionLocalProvider
}
