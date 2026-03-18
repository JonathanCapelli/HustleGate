package com.pompesblocker.ui.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pompesblocker.billing.BillingManager
import com.pompesblocker.data.PreferencesManager
import com.pompesblocker.model.defaultExercises

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
                title = { Text("⚙️ Paramètres") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
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
                "💪 Exercices",
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

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "${exercise.emoji} ${exercise.name}",
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
                                    newValue.toIntOrNull()?.let { value ->
                                        if (value > 0) prefs.setExerciseReps(exercise.id, value)
                                    }
                                },
                                label = { Text("Répétitions") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            OutlinedTextField(
                                value = rewardMin,
                                onValueChange = { newValue ->
                                    rewardMin = newValue
                                    newValue.toIntOrNull()?.let { value ->
                                        if (value > 0) prefs.setExerciseRewardMinutes(exercise.id, value)
                                    }
                                },
                                label = { Text("Minutes gagnées") },
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
                "🚶 Objectif pas",
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
                            label = { Text("Objectif pas") },
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
                            label = { Text("Minutes gagnées") },
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
                "📢 Publicité",
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
                            "✅ Publicités supprimées",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Merci pour ton soutien !",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Text(
                            "Supprime les publicités pour 3 €",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                (context as? Activity)?.let { activity ->
                                    billingManager.launchPurchase(activity) { success ->
                                        if (success) {
                                            adsRemoved = true
                                            Toast.makeText(context, "✅ Publicités supprimées !", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = billingReady
                        ) {
                            Text("💎 Supprimer les pubs — 3 €")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                billingManager.restorePurchases { restored ->
                                    adsRemoved = restored
                                    val msg = if (restored) "✅ Achat restauré !" else "Aucun achat trouvé"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = billingReady
                        ) {
                            Text("🔄 Restaurer un achat")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
