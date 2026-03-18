package com.pompesblocker.model

import com.pompesblocker.R
import com.pompesblocker.data.PreferencesManager

data class Exercise(
    val id: String,
    val nameResId: Int,
    val defaultReps: Int,
    val emoji: String,
    val defaultRewardMinutes: Int = 15
)

val defaultExercises = listOf(
    Exercise(id = "pompes", nameResId = R.string.exercise_pushups, defaultReps = 10, emoji = "💪"),
    Exercise(id = "tractions", nameResId = R.string.exercise_pullups, defaultReps = 5, emoji = "🏋️"),
    Exercise(id = "dips", nameResId = R.string.exercise_dips, defaultReps = 5, emoji = "💪"),
    Exercise(id = "squats", nameResId = R.string.exercise_squats, defaultReps = 10, emoji = "🦵"),
    Exercise(id = "jumping_jacks", nameResId = R.string.exercise_jumping_jacks, defaultReps = 15, emoji = "⭐"),
    Exercise(id = "situps", nameResId = R.string.exercise_situps, defaultReps = 10, emoji = "🔄"),
    Exercise(id = "burpees", nameResId = R.string.exercise_burpees, defaultReps = 5, emoji = "🔥"),
    Exercise(id = "fentes", nameResId = R.string.exercise_lunges, defaultReps = 10, emoji = "🦿"),
    Exercise(id = "mountain_climbers", nameResId = R.string.exercise_mountain_climbers, defaultReps = 15, emoji = "⛰️")
)

/** Récupère le nombre de reps configuré pour cet exercice */
fun Exercise.getReps(prefs: PreferencesManager): Int {
    return prefs.getExerciseReps(id, defaultReps)
}

/** Récupère le temps de récompense configuré pour cet exercice */
fun Exercise.getRewardMinutes(prefs: PreferencesManager): Int {
    return prefs.getExerciseRewardMinutes(id, defaultRewardMinutes)
}

fun Exercise.getRewardMillis(prefs: PreferencesManager): Long {
    return getRewardMinutes(prefs) * 60 * 1000L
}
