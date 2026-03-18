package com.pompesblocker.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.pompesblocker.data.PreferencesManager
import java.time.LocalDate
import java.time.ZoneId

class HealthConnectManager(private val context: Context) {

    private val prefs = PreferencesManager(context)

    companion object {
        val STEPS_PERMISSION = HealthPermission.getReadPermission(StepsRecord::class)
        val PERMISSIONS = setOf(STEPS_PERMISSION)
    }

    /**
     * Vérifie si Health Connect est disponible sur l'appareil.
     */
    fun getAvailability(): Int {
        return HealthConnectClient.getSdkStatus(context)
    }

    fun isAvailable(): Boolean {
        return getAvailability() == HealthConnectClient.SDK_AVAILABLE
    }

    fun isInstallRequired(): Boolean {
        return getAvailability() == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
    }

    /**
     * Intent pour installer Health Connect depuis le Play Store.
     */
    fun getInstallIntent(): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("market://details?id=com.google.android.apps.healthdata")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Vérifie si les permissions de lecture des pas sont accordées.
     */
    suspend fun hasPermissions(): Boolean {
        if (!isAvailable()) return false
        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        return PERMISSIONS.all { it in granted }
    }

    /**
     * Récupère le nombre de pas du jour.
     */
    suspend fun getTodaySteps(): Int? {
        if (!isAvailable()) return null
        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val today = LocalDate.now()
            val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val now = java.time.Instant.now()

            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )
            )

            response.records.sumOf { it.count }.toInt()
        } catch (e: Exception) {
            android.util.Log.e("HealthConnect", "Erreur lecture pas", e)
            null
        }
    }

    /**
     * Vérifie les pas et attribue la récompense si l'objectif est atteint.
     */
    suspend fun checkAndRewardSteps(): Pair<Int?, Boolean> {
        val steps = getTodaySteps() ?: return Pair(null, false)

        val stepGoal = prefs.getStepGoal()
        val alreadyRewarded = prefs.hasStepRewardToday()
        val shouldReward = steps >= stepGoal && !alreadyRewarded

        if (shouldReward) {
            val rewardMinutes = prefs.getStepRewardMinutes()
            prefs.addTime(rewardMinutes * 60 * 1000L)
            prefs.markStepRewardToday()
        }

        return Pair(steps, shouldReward)
    }
}
