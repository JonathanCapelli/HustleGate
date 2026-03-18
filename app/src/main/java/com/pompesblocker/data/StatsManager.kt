package com.pompesblocker.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Gère l'historique des exercices et les statistiques.
 * Stocke chaque exercice complété avec date, type, reps.
 */
class StatsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("hustlegate_stats", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_HISTORY = "exercise_history"
        private const val KEY_TOTAL_REPS = "total_reps"
        private const val KEY_TOTAL_EXERCISES = "total_exercises"
        private const val KEY_TOTAL_MINUTES_EARNED = "total_minutes_earned"
        private const val KEY_CURRENT_STREAK = "current_streak"
        private const val KEY_BEST_STREAK = "best_streak"
        private const val KEY_LAST_EXERCISE_DATE = "last_exercise_date"
    }

    data class ExerciseRecord(
        val exerciseId: String,
        val exerciseName: String,
        val reps: Int,
        val minutesEarned: Int,
        val date: String, // yyyy-MM-dd
        val timestamp: Long
    )

    /** Enregistre un exercice complété */
    fun recordExercise(exerciseId: String, exerciseName: String, reps: Int, minutesEarned: Int) {
        val today = LocalDate.now().toString()
        val record = JSONObject().apply {
            put("exerciseId", exerciseId)
            put("exerciseName", exerciseName)
            put("reps", reps)
            put("minutesEarned", minutesEarned)
            put("date", today)
            put("timestamp", System.currentTimeMillis())
        }

        // Ajouter à l'historique
        val history = getHistoryJson()
        history.put(record)
        prefs.edit { putString(KEY_HISTORY, history.toString()) }

        // Mettre à jour les totaux
        prefs.edit {
            putInt(KEY_TOTAL_REPS, getTotalReps() + reps)
            putInt(KEY_TOTAL_EXERCISES, getTotalExercises() + 1)
            putInt(KEY_TOTAL_MINUTES_EARNED, getTotalMinutesEarned() + minutesEarned)
        }

        // Mettre à jour le streak
        updateStreak(today)
    }

    private fun updateStreak(today: String) {
        val lastDate = prefs.getString(KEY_LAST_EXERCISE_DATE, null)
        val yesterday = LocalDate.now().minusDays(1).toString()

        val currentStreak = when (lastDate) {
            today -> getCurrentStreak() // déjà exercé aujourd'hui, pas de changement
            yesterday -> getCurrentStreak() + 1 // jour consécutif
            else -> 1 // streak reset
        }

        val bestStreak = maxOf(currentStreak, getBestStreak())

        prefs.edit {
            putInt(KEY_CURRENT_STREAK, currentStreak)
            putInt(KEY_BEST_STREAK, bestStreak)
            putString(KEY_LAST_EXERCISE_DATE, today)
        }
    }

    fun getTotalReps(): Int = prefs.getInt(KEY_TOTAL_REPS, 0)
    fun getTotalExercises(): Int = prefs.getInt(KEY_TOTAL_EXERCISES, 0)
    fun getTotalMinutesEarned(): Int = prefs.getInt(KEY_TOTAL_MINUTES_EARNED, 0)
    fun getCurrentStreak(): Int = prefs.getInt(KEY_CURRENT_STREAK, 0)
    fun getBestStreak(): Int = prefs.getInt(KEY_BEST_STREAK, 0)

    /** Retourne l'historique des 7 derniers jours */
    fun getWeekHistory(): Map<String, List<ExerciseRecord>> {
        val history = getHistory()
        val sevenDaysAgo = LocalDate.now().minusDays(6)
        return history
            .filter { LocalDate.parse(it.date) >= sevenDaysAgo }
            .groupBy { it.date }
    }

    /** Retourne le nombre de reps par jour sur les 7 derniers jours */
    fun getWeeklyReps(): List<Pair<String, Int>> {
        val history = getWeekHistory()
        val result = mutableListOf<Pair<String, Int>>()
        val formatter = DateTimeFormatter.ofPattern("EEE")

        for (i in 6 downTo 0) {
            val date = LocalDate.now().minusDays(i.toLong())
            val dateStr = date.toString()
            val dayLabel = date.format(formatter)
            val totalReps = history[dateStr]?.sumOf { it.reps } ?: 0
            result.add(dayLabel to totalReps)
        }
        return result
    }

    /** Nombre d'exercices aujourd'hui */
    fun getTodayExerciseCount(): Int {
        val today = LocalDate.now().toString()
        return getHistory().count { it.date == today }
    }

    /** Reps d'aujourd'hui par type d'exercice */
    fun getTodayRepsByExercise(): Map<String, Int> {
        val today = LocalDate.now().toString()
        return getHistory()
            .filter { it.date == today }
            .groupBy { it.exerciseName }
            .mapValues { (_, records) -> records.sumOf { it.reps } }
    }

    private fun getHistory(): List<ExerciseRecord> {
        val json = getHistoryJson()
        val records = mutableListOf<ExerciseRecord>()
        for (i in 0 until json.length()) {
            val obj = json.getJSONObject(i)
            records.add(
                ExerciseRecord(
                    exerciseId = obj.getString("exerciseId"),
                    exerciseName = obj.getString("exerciseName"),
                    reps = obj.getInt("reps"),
                    minutesEarned = obj.getInt("minutesEarned"),
                    date = obj.getString("date"),
                    timestamp = obj.getLong("timestamp")
                )
            )
        }
        return records
    }

    private fun getHistoryJson(): JSONArray {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return JSONArray()
        return try { JSONArray(raw) } catch (e: Exception) { JSONArray() }
    }
}
