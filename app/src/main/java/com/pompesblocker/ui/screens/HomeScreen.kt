package com.pompesblocker.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.pompesblocker.R
import com.pompesblocker.data.PreferencesManager
import com.pompesblocker.health.HealthConnectManager
import com.pompesblocker.model.Exercise
import com.pompesblocker.model.defaultExercises
import com.pompesblocker.model.getReps
import com.pompesblocker.model.getRewardMillis
import com.pompesblocker.model.getRewardMinutes
import com.pompesblocker.ui.components.ExerciseAvatar
import com.pompesblocker.ui.components.AdBanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAppSelection: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCamera: (exerciseId: String) -> Unit = {},
    onNavigateToStats: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val healthManager = remember { HealthConnectManager(context) }
    val coroutineScope = rememberCoroutineScope()
    var remainingTimeMillis by remember { mutableLongStateOf(prefs.getRemainingTimeMillis()) }
    var isServiceEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var blockedAppsCount by remember { mutableStateOf(prefs.getBlockedApps().size) }
    var steps by remember { mutableStateOf<Int?>(null) }
    var healthAvailable by remember { mutableStateOf(healthManager.isAvailable()) }
    var healthConnected by remember { mutableStateOf(false) }
    var stepsLoading by remember { mutableStateOf(false) }
    var stepsRewarded by remember { mutableStateOf(prefs.hasStepRewardToday()) }

    // Launcher pour demander les permissions Health Connect
    val healthPermissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        healthConnected = granted.containsAll(HealthConnectManager.PERMISSIONS)
        if (healthConnected) {
            coroutineScope.launch {
                stepsLoading = true
                val (todaySteps, rewarded) = healthManager.checkAndRewardSteps()
                steps = todaySteps
                if (rewarded) {
                    stepsRewarded = true
                    remainingTimeMillis = prefs.getRemainingTimeMillis()
                }
                stepsLoading = false
            }
        }
    }

    // Rafraîchir le timer et l'état toutes les secondes
    LaunchedEffect(Unit) {
        while (true) {
            remainingTimeMillis = prefs.getRemainingTimeMillis()
            isServiceEnabled = isAccessibilityServiceEnabled(context)
            blockedAppsCount = prefs.getBlockedApps().size
            healthAvailable = healthManager.isAvailable()
            delay(1000)
        }
    }

    // Vérifier les permissions Health Connect et charger les pas
    LaunchedEffect(healthAvailable) {
        if (healthAvailable) {
            healthConnected = healthManager.hasPermissions()
            if (healthConnected) {
                stepsLoading = true
                val (todaySteps, rewarded) = healthManager.checkAndRewardSteps()
                steps = todaySteps
                if (rewarded) {
                    stepsRewarded = true
                    remainingTimeMillis = prefs.getRemainingTimeMillis()
                }
                stepsLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Alerte si service désactivé
            if (!isServiceEnabled) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            stringResource(R.string.service_disabled),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.service_disabled_message),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            )
                        }) {
                            Text(stringResource(R.string.enable_service))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Affichage du temps restant
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (remainingTimeMillis > 0)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.time_remaining),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        formatTime(remainingTimeMillis),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (remainingTimeMillis > 0)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (remainingTimeMillis <= 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.do_exercise_for_time),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Titre section exercices
            Text(
                stringResource(R.string.earn_time),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Cartes d'exercices
            defaultExercises.forEach { exercise ->
                ExerciseCard(
                    exercise = exercise,
                    prefs = prefs,
                    onClick = { onNavigateToCamera(exercise.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Section Pas (Health Connect) ---
            Text(
                stringResource(R.string.step_goal_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (stepsRewarded)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!healthAvailable) {
                        Text(
                            stringResource(R.string.health_connect_unavailable),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        if (healthManager.isInstallRequired()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = {
                                context.startActivity(healthManager.getInstallIntent())
                            }) {
                                Text(stringResource(R.string.install_health_connect))
                            }
                        }
                    } else if (!healthConnected) {
                        Text(
                            stringResource(R.string.authorize_steps_message),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = {
                            healthPermissionLauncher.launch(HealthConnectManager.PERMISSIONS)
                        }) {
                            Text(stringResource(R.string.authorize_steps_button))
                        }
                    } else if (stepsLoading) {
                        Text(stringResource(R.string.loading_steps))
                    } else {
                        val currentSteps = steps ?: 0
                        val stepGoal = prefs.getStepGoal()
                        val stepRewardMin = prefs.getStepRewardMinutes()
                        val progress = (currentSteps.toFloat() / stepGoal).coerceIn(0f, 1f)

                        Text(
                            stringResource(R.string.steps_progress, currentSteps, stepGoal),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (stepsRewarded)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (stepsRewarded) {
                            Text(
                                stringResource(R.string.steps_rewarded, stepRewardMin),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                stringResource(R.string.steps_remaining, stepGoal - currentSteps, stepRewardMin),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    stepsLoading = true
                                    val (newSteps, rewarded) = healthManager.checkAndRewardSteps()
                                    steps = newSteps
                                    if (rewarded) {
                                        stepsRewarded = true
                                        remainingTimeMillis = prefs.getRemainingTimeMillis()
                                    }
                                    stepsLoading = false
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (stepsRewarded)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (stepsRewarded)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(stringResource(R.string.refresh))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bouton gestion des apps
            OutlinedButton(
                onClick = onNavigateToAppSelection,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.manage_blocked_apps, blockedAppsCount)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bouton statistiques
            OutlinedButton(
                onClick = onNavigateToStats,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.statistics))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bouton paramètres
            OutlinedButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings))
            }

            if (remainingTimeMillis > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        prefs.setRemainingTimeMillis(0)
                        remainingTimeMillis = 0
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.reset_timer))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status du service
            Text(
                if (isServiceEnabled) stringResource(R.string.service_active) else stringResource(R.string.service_inactive),
                style = MaterialTheme.typography.bodySmall,
                color = if (isServiceEnabled)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
        }
    }

    // Bannière pub en bas si pas acheté
    if (!prefs.hasRemovedAds()) {
        AdBanner()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseCard(exercise: Exercise, prefs: PreferencesManager, onClick: () -> Unit) {
    val reps = exercise.getReps(prefs)
    val rewardMinutes = exercise.getRewardMinutes(prefs)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExerciseAvatar(
                exerciseId = exercise.id,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.exercise_card_label, exercise.emoji, reps, stringResource(exercise.nameResId)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.plus_minutes, rewardMinutes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text("▶", fontSize = 24.sp)
        }
    }
}

private fun formatTime(millis: Long): String {
    if (millis <= 0) return "00:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val service = "${context.packageName}/com.pompesblocker.service.AppBlockerService"
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )
    return enabledServices?.contains(service) == true
}
