package com.pompesblocker.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.pompesblocker.BlockedActivity
import com.pompesblocker.data.PreferencesManager
import com.pompesblocker.widget.TimerWidgetProvider

class AppBlockerService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var currentBlockedPackage: String? = null
    private lateinit var prefs: PreferencesManager
    private lateinit var notificationHelper: NotificationHelper
    private var notified2Min = false
    private var notified1Min = false
    private var isScreenOn = true

    // Suivi du temps réel avec SystemClock
    private var timerStartRealtime: Long = 0L
    private var timerStartRemainingMillis: Long = 0L

    // Package bloqué mémorisé pendant l'écran éteint pour reprendre au réveil
    private var pausedBlockedPackage: String? = null

    companion object {
        var isRunning = false
            private set
    }

    // Receiver pour écran ON/OFF
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    if (currentBlockedPackage != null) {
                        pausedBlockedPackage = currentBlockedPackage
                        stopTimer()
                    }
                }
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    // Le timer reprendra naturellement au prochain événement d'accessibilité
                    // quand l'utilisateur déverrouille et retourne sur l'app bloquée
                }
            }
        }
    }

    // Packages système à ne jamais bloquer — NE PAS stopper le timer pour ceux-ci
    private val systemPackages = setOf(
        "com.android.systemui",
        "com.android.launcher",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.android.settings",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.permissioncontroller"
    )

    // Packages qui signifient que l'utilisateur a quitté l'appli bloquée
    private val launcherPackages = setOf(
        "com.android.launcher",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher"
    )

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (currentBlockedPackage != null) {
                val elapsed = SystemClock.elapsedRealtime() - timerStartRealtime
                val remaining = timerStartRemainingMillis - elapsed

                if (remaining <= 0) {
                    prefs.setRemainingTimeMillis(0)
                    currentBlockedPackage = null
                    notificationHelper.notifyTimeUp()
                    notified2Min = false
                    notified1Min = false
                    launchBlockedScreen()
                } else {
                    prefs.setRemainingTimeMillis(remaining)

                    // Notifications de temps restant
                    val remainingSec = remaining / 1000
                    if (remainingSec <= 120 && remainingSec > 60 && !notified2Min) {
                        notified2Min = true
                        notificationHelper.notifyLowTime(2)
                    } else if (remainingSec <= 60 && !notified1Min) {
                        notified1Min = true
                        notificationHelper.notifyLowTime(1)
                    }

                    TimerWidgetProvider.refreshAll(this@AppBlockerService)
                    handler.postDelayed(this, 1000)
                }
            }
        }
    }

    private fun startTimer(packageName: String) {
        handler.removeCallbacks(timerRunnable)
        currentBlockedPackage = packageName
        notified2Min = false
        notified1Min = false
        timerStartRemainingMillis = prefs.getRemainingTimeMillis()
        timerStartRealtime = SystemClock.elapsedRealtime()
        handler.postDelayed(timerRunnable, 1000)
    }

    private fun stopTimer() {
        if (currentBlockedPackage != null) {
            // Sauvegarder le temps réel restant avant d'arrêter
            val elapsed = SystemClock.elapsedRealtime() - timerStartRealtime
            val remaining = (timerStartRemainingMillis - elapsed).coerceAtLeast(0)
            prefs.setRemainingTimeMillis(remaining)
            TimerWidgetProvider.refreshAll(this)
        }
        currentBlockedPackage = null
        handler.removeCallbacks(timerRunnable)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = PreferencesManager(this)
        notificationHelper = NotificationHelper(this)
        isRunning = true

        // Écouter les événements écran ON/OFF
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (!isScreenOn) return // Ignorer les événements quand l'écran est éteint
        val packageName = event.packageName?.toString() ?: return

        // Ignorer notre propre app
        if (packageName == this.packageName) {
            stopTimer()
            return
        }

        // Si c'est un launcher → l'utilisateur a quitté l'app bloquée
        if (launcherPackages.contains(packageName) || packageName.contains("launcher")) {
            stopTimer()
            return
        }

        // SystemUI, Settings, etc. → ignorer complètement (ne PAS toucher au timer)
        // L'utilisateur est toujours sur l'app bloquée, c'est juste une notification/overlay
        if (systemPackages.contains(packageName)) {
            return
        }

        if (prefs.isAppBlocked(packageName)) {
            if (!prefs.hasTimeRemaining()) {
                // Pas de temps → bloquer immédiatement
                stopTimer()
                launchBlockedScreen()
            } else if (currentBlockedPackage != packageName) {
                // Nouvelle app bloquée avec du temps → démarrer le décompte
                startTimer(packageName)
            }
            // Si même app bloquée, le timer tourne déjà
        } else {
            // App non bloquée, non système → l'utilisateur a réellement quitté
            stopTimer()
        }
    }

    private fun launchBlockedScreen() {
        val intent = Intent(this, BlockedActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        handler.removeCallbacks(timerRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
        try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
        isRunning = false
    }
}
