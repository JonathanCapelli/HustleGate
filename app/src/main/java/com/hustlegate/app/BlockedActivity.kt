package com.hustlegate.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.hustlegate.app.data.PreferencesManager
import com.hustlegate.app.model.Exercise
import com.hustlegate.app.model.defaultExercises
import com.hustlegate.app.model.getReps
import com.hustlegate.app.model.getRewardMinutes
import com.hustlegate.app.ui.components.ExerciseAvatar
import com.hustlegate.app.ui.screens.CameraExerciseScreen
import com.hustlegate.app.ui.theme.HustleGateTheme
import com.hustlegate.app.ui.components.InterstitialAdManager

class BlockedActivity : ComponentActivity() {

    private var pendingExercise: Exercise? = null
    private lateinit var interstitialAdManager: InterstitialAdManager

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingExercise?.let { recreate() }
        } else {
            Toast.makeText(this, getString(R.string.camera_required_toast), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        interstitialAdManager = InterstitialAdManager(this)
        interstitialAdManager.load()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goHome()
            }
        })

        setContent {
            HustleGateTheme {
                val prefs = remember { PreferencesManager(this@BlockedActivity) }
                var selectedExercise by remember { mutableStateOf<Exercise?>(null) }

                if (selectedExercise != null) {
                    // Écran caméra pour l'exercice sélectionné
                    CameraExerciseScreen(
                        exercise = selectedExercise!!,
                        prefs = prefs,
                        onExerciseComplete = {
                            selectedExercise = null
                            finish()
                        },
                        onCancel = {
                            selectedExercise = null
                        }
                    )
                } else {
                    // Écran de blocage
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("🚫", fontSize = 64.sp)

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                stringResource(R.string.app_blocked),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                stringResource(R.string.do_exercise_to_unlock),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            defaultExercises.forEach { exercise ->
                                val reps = exercise.getReps(prefs)
                                val rewardMin = exercise.getRewardMinutes(prefs)
                                Button(
                                    onClick = {
                                        if (hasCameraPermission()) {
                                            val p = PreferencesManager(this@BlockedActivity)
                                            if (p.shouldShowInterstitial()) {
                                                val ex = exercise
                                                interstitialAdManager.showIfReady(this@BlockedActivity) {
                                                    selectedExercise = ex
                                                }
                                            } else {
                                                selectedExercise = exercise
                                            }
                                            p.incrementExerciseCompletedCount()
                                        } else {
                                            pendingExercise = exercise
                                            requestCameraPermission()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ExerciseAvatar(
                                            exerciseId = exercise.id,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            stringResource(R.string.exercise_button_label, reps, stringResource(exercise.nameResId), rewardMin),
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedButton(onClick = { goHome() }) {
                                Text(stringResource(R.string.go_home))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}
