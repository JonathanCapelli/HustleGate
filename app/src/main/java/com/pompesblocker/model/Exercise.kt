package com.pompesblocker.model

import com.pompesblocker.data.PreferencesManager

data class Exercise(
    val id: String,
    val name: String,
    val defaultReps: Int,
    val emoji: String,
    val defaultRewardMinutes: Int = 15
)

val defaultExercises = listOf(
    Exercise(id = "pompes", name = "Pompes", defaultReps = 10, emoji = "💪"),
    Exercise(id = "tractions", name = "Tractions", defaultReps = 5, emoji = "🏋️"),
    Exercise(id = "dips", name = "Dips", defaultReps = 5, emoji = "💪"),
    Exercise(id = "squats", name = "Squats", defaultReps = 10, emoji = "🦵"),
    Exercise(id = "jumping_jacks", name = "Jumping Jacks", defaultReps = 15, emoji = "⭐"),
    Exercise(id = "situps", name = "Sit-ups", defaultReps = 10, emoji = "🔄"),
    Exercise(id = "burpees", name = "Burpees", defaultReps = 5, emoji = "🔥"),
    Exercise(id = "fentes", name = "Fentes", defaultReps = 10, emoji = "🦿"),
    Exercise(id = "mountain_climbers", name = "Mountain Climbers", defaultReps = 15, emoji = "⛰️")
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
