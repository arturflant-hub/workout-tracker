package com.workouttracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.workouttracker.ui.screens.*

sealed class Screen(val route: String) {
    object Programs : Screen("programs")
    object ExerciseEdit : Screen("exercise_edit/{programId}/{exerciseId}") {
        fun createRoute(programId: Long, exerciseId: Long = -1L) =
            "exercise_edit/$programId/$exerciseId"
    }
    object ScheduleSettings : Screen("schedule_settings")
    object Calendar : Screen("calendar")
    object WorkoutSession : Screen("workout_session/{sessionId}") {
        fun createRoute(sessionId: Long) = "workout_session/$sessionId"
    }
    object History : Screen("history")
}

@Composable
fun WorkoutNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Programs.route) {
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
        composable(
            route = Screen.WorkoutSession.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStack ->
            val sessionId = backStack.arguments?.getLong("sessionId") ?: return@composable
            WorkoutSessionScreen(sessionId = sessionId, navController = navController)
        }
        composable(Screen.History.route) {
            HistoryScreen(navController = navController)
        }
    }
}
