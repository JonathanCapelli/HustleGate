package com.hustlegate.app.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.hustlegate.app.R
import com.hustlegate.app.billing.BillingManager
import com.hustlegate.app.data.PreferencesManager
import com.hustlegate.app.model.defaultExercises

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val billingManager = remember { BillingManager(context) }
    var adsRemoved by remember { mutableStateOf(prefs.hasRemovedAds()) }
    var billingReady by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        billingManager.startConnection { billingReady = true }
        onDispose { billingManager.endConnection() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // --- Section Exercices ---
            Text(
                stringResource(R.string.exercises_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            defaultExercises.forEach { exercise ->
                var reps by remember {
                    mutableStateOf(
                        prefs.getExerciseReps(exercise.id, exercise.defaultReps).toString()
                    )
                }
                var rewardMin by remember {
                    mutableStateOf(
                        prefs.getExerciseRewardMinutes(exercise.id, exercise.defaultRewardMinutes).toString()
                    )
                }

                // Debounce save for reps
                LaunchedEffect(reps) {
                    delay(500)
                    reps.toIntOrNull()?.let { value ->
                        if (value > 0) prefs.setExerciseReps(exercise.id, value)
                    }
                }

                // Debounce save for reward minutes
                LaunchedEffect(rewardMin) {
                    delay(500)
                    rewardMin.toIntOrNull()?.let { value ->
                        if (value > 0) prefs.setExerciseRewardMinutes(exercise.id, value)
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "${exercise.emoji} ${stringResource(exercise.nameResId)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = reps,
                                onValueChange = { newValue ->
                                    reps = newValue
                                },
                                label = { Text(stringResource(R.string.repetitions)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            OutlinedTextField(
                                value = rewardMin,
                                onValueChange = { newValue ->
                                    rewardMin = newValue
                                },
                                label = { Text(stringResource(R.string.minutes_earned)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(24.dp))

            // --- Section Pas ---
            Text(
                stringResource(R.string.step_goal_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    var stepGoal by remember {
                        mutableStateOf(prefs.getStepGoal().toString())
                    }
                    var stepReward by remember {
                        mutableStateOf(prefs.getStepRewardMinutes().toString())
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = stepGoal,
                            onValueChange = { newValue ->
                                stepGoal = newValue
                                newValue.toIntOrNull()?.let { value ->
                                    if (value > 0) prefs.setStepGoal(value)
                                }
                            },
                            label = { Text(stringResource(R.string.step_goal_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        OutlinedTextField(
                            value = stepReward,
                            onValueChange = { newValue ->
                                stepReward = newValue
                                newValue.toIntOrNull()?.let { value ->
                                    if (value > 0) prefs.setStepRewardMinutes(value)
                                }
                            },
                            label = { Text(stringResource(R.string.minutes_earned)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(24.dp))

            // --- Section Publicité ---
            Text(
                stringResource(R.string.ads_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (adsRemoved)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (adsRemoved) {
                        Text(
                            stringResource(R.string.ads_removed),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.thanks_support),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Text(
                            stringResource(R.string.remove_ads_price),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                (context as? Activity)?.let { activity ->
                                    billingManager.launchPurchase(activity) { success ->
                                        if (success) {
                                            adsRemoved = true
                                            Toast.makeText(context, context.getString(R.string.ads_removed_toast), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = billingReady
                        ) {
                            Text(stringResource(R.string.buy_remove_ads))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                billingManager.restorePurchases { restored ->
                                    adsRemoved = restored
                                    val msg = if (restored) context.getString(R.string.purchase_restored) else context.getString(R.string.no_purchase_found)
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = billingReady
                        ) {
                            Text(stringResource(R.string.restore_purchase))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
