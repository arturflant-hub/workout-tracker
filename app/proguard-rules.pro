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

# Google API Client
-keep class com.google.api.** { *; }
-keep class com.google.http.** { *; }
-dontwarn com.google.api.client.**
-dontwarn com.google.http.client.**
-dontwarn org.apache.http.**
-dontwarn com.google.common.**
-dontwarn javax.naming.**

# Gson used by Google HTTP Client
-keep class com.google.gson.** { *; }
