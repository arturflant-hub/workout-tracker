# Room entities
-keep class com.workouttracker.data.db.entities.** { *; }

# Room DAOs
-keep class com.workouttracker.data.db.dao.** { *; }

# Hilt ViewModels
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Domain models
-keep class com.workouttracker.domain.model.** { *; }

# Room enum converters
-keepclassmembers enum com.workouttracker.data.db.entities.** { <fields>; }

# Coroutines
-dontwarn kotlinx.coroutines.**
