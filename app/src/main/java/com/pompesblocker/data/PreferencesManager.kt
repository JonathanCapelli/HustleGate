package com.pompesblocker.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.time.LocalDate

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("pompes_blocker", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BLOCKED_APPS = "blocked_apps"
        private const val KEY_REMAINING_TIME = "remaining_time_millis"
        private const val KEY_STEP_REWARD_DATE = "fitbit_step_reward_date"
        private const val KEY_STEP_GOAL = "fitbit_step_goal"
        private const val KEY_STEP_REWARD_MINUTES = "fitbit_step_reward_minutes"
    }

    fun getBlockedApps(): Set<String> {
        return prefs.getStringSet(KEY_BLOCKED_APPS, emptySet()) ?: emptySet()
    }

    fun setBlockedApps(apps: Set<String>) {
        prefs.edit { putStringSet(KEY_BLOCKED_APPS, apps) }
    }

    fun addBlockedApp(packageName: String) {
        val apps = getBlockedApps().toMutableSet()
        apps.add(packageName)
        setBlockedApps(apps)
    }

    fun removeBlockedApp(packageName: String) {
        val apps = getBlockedApps().toMutableSet()
        apps.remove(packageName)
        setBlockedApps(apps)
    }

    fun isAppBlocked(packageName: String): Boolean {
        return getBlockedApps().contains(packageName)
    }

    fun getRemainingTimeMillis(): Long {
        return prefs.getLong(KEY_REMAINING_TIME, 0L)
    }

    fun setRemainingTimeMillis(millis: Long) {
        prefs.edit {
            putLong(KEY_REMAINING_TIME, maxOf(0L, millis))
        }
    }

    fun addTime(millis: Long) {
        setRemainingTimeMillis(getRemainingTimeMillis() + millis)
    }

    fun hasTimeRemaining(): Boolean {
        return getRemainingTimeMillis() > 0
    }

    // --- Pas (Health Connect) ---

    fun hasStepRewardToday(): Boolean {
        val lastDate = prefs.getString(KEY_STEP_REWARD_DATE, null) ?: return false
        return lastDate == LocalDate.now().toString()
    }

    fun markStepRewardToday() {
        prefs.edit { putString(KEY_STEP_REWARD_DATE, LocalDate.now().toString()) }
    }

    // --- Réglages exercices ---

    fun getExerciseRewardMinutes(exerciseId: String, default: Int = 15): Int {
        return prefs.getInt("exercise_reward_$exerciseId", default)
    }

    fun setExerciseRewardMinutes(exerciseId: String, minutes: Int) {
        prefs.edit { putInt("exercise_reward_$exerciseId", minutes) }
    }

    fun getExerciseReps(exerciseId: String, default: Int): Int {
        return prefs.getInt("exercise_reps_$exerciseId", default)
    }

    fun setExerciseReps(exerciseId: String, reps: Int) {
        prefs.edit { putInt("exercise_reps_$exerciseId", reps) }
    }

    // --- Réglages pas ---

    fun getStepGoal(): Int {
        return prefs.getInt(KEY_STEP_GOAL, 15000)
    }

    fun setStepGoal(goal: Int) {
        prefs.edit { putInt(KEY_STEP_GOAL, goal) }
    }

    fun getStepRewardMinutes(): Int {
        return prefs.getInt(KEY_STEP_REWARD_MINUTES, 60)
    }

    fun setStepRewardMinutes(minutes: Int) {
        prefs.edit { putInt(KEY_STEP_REWARD_MINUTES, minutes) }
    }

    // --- Publicité ---

    fun hasRemovedAds(): Boolean {
        return prefs.getBoolean("ads_removed", false)
    }

    fun setAdsRemoved(removed: Boolean) {
        prefs.edit { putBoolean("ads_removed", removed) }
    }
}
