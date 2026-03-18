package com.pompesblocker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pompesblocker.MainActivity

/**
 * Gère les notifications pour HustleGate.
 * - Alerte quand il reste peu de temps (2 min, 1 min, 30 sec)
 * - Notification quand le temps est écoulé
 */
class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "hustlegate_timer"
        const val NOTIFICATION_ID_LOW_TIME = 1001
        const val NOTIFICATION_ID_TIME_UP = 1002
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Timer HustleGate",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications de temps restant"
            enableVibration(true)
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    fun notifyLowTime(minutesLeft: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = when {
            minutesLeft <= 0 -> "⏰ Temps écoulé ! Fais un exercice pour continuer"
            minutesLeft == 1 -> "⚠️ Plus qu'1 minute !"
            else -> "⚠️ Plus que $minutesLeft minutes !"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("💪 HustleGate")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID_LOW_TIME, notification)
    }

    fun notifyTimeUp() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🚫 Temps écoulé !")
            .setContentText("Fais un exercice pour débloquer du temps")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID_TIME_UP, notification)
    }

    fun cancel(notificationId: Int) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancel(notificationId)
    }
}
