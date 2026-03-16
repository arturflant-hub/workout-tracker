package com.workouttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.workouttracker.ui.navigation.WorkoutNavGraph
import com.workouttracker.ui.theme.WorkoutTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WorkoutTrackerTheme {
                val navController = rememberNavController()
                WorkoutNavGraph(navController = navController)
            }
        }
    }
}
