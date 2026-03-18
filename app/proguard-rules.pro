# ──────────────────────────────────────────────────────
# ProGuard / R8 rules for HustleGate
# ──────────────────────────────────────────────────────

# Google Play Billing
-keep class com.android.vending.billing.**

# ML Kit Pose Detection
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Health Connect
-keep class androidx.health.connect.** { *; }
-dontwarn androidx.health.connect.**

# AdMob
-keep class com.google.android.gms.ads.** { *; }

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**

# Keep data classes used with JSON (StatsManager)
-keep class com.hustlegate.app.data.StatsManager$ExerciseRecord { *; }

# Keep R8 from stripping Compose
-dontwarn androidx.compose.**
