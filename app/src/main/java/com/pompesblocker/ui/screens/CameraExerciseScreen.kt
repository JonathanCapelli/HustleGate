package com.pompesblocker.ui.screens

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.pompesblocker.camera.ExerciseDetector
import com.pompesblocker.camera.PoseAnalyzer
import com.pompesblocker.camera.SoundFeedback
import com.pompesblocker.data.PreferencesManager
import com.pompesblocker.data.StatsManager
import com.pompesblocker.model.Exercise
import com.pompesblocker.model.getReps
import com.pompesblocker.model.getRewardMillis
import com.pompesblocker.model.getRewardMinutes
import com.pompesblocker.ui.components.ExerciseAvatar
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@Composable
fun CameraExerciseScreen(
    exercise: Exercise,
    prefs: PreferencesManager,
    onExerciseComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val targetReps = exercise.getReps(prefs)
    val rewardMinutes = exercise.getRewardMinutes(prefs)

    var currentReps by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf("Positionne-toi devant la caméra") }
    var isComplete by remember { mutableStateOf(false) }
    var showCompletionScreen by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(5) }
    var countdownFinished by remember { mutableStateOf(false) }

    val soundFeedback = remember { SoundFeedback(context) }

    // Compte à rebours 5 → 1 → GO!
    LaunchedEffect(Unit) {
        for (i in 5 downTo 1) {
            countdown = i
            soundFeedback.playCountdownTick()
            delay(1000)
        }
        countdownFinished = true
        soundFeedback.playGoSound()
        feedback = "C'est parti ! 💪"
    }

    val exerciseDetector = remember {
        ExerciseDetector(
            exerciseId = exercise.id,
            targetReps = targetReps,
            onRepCounted = { count ->
                currentReps = count
                soundFeedback.playRepSound()
            },
            onExerciseComplete = {
                isComplete = true
                soundFeedback.playVictorySound()
            }
        )
    }

    val poseAnalyzer = remember {
        PoseAnalyzer { pose ->
            if (countdownFinished) {
                exerciseDetector.analyzePose(pose)
                feedback = exerciseDetector.feedback
            }
        }
    }

    // Quand l'exercice est terminé, afficher l'écran de complétion
    LaunchedEffect(isComplete) {
        if (isComplete) {
            delay(500) // Petite pause pour voir le dernier rep
            showCompletionScreen = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            poseAnalyzer.close()
            soundFeedback.release()
        }
    }

    if (showCompletionScreen) {
        // Écran de victoire
        CompletionScreen(
            exercise = exercise,
            reps = targetReps,
            rewardMinutes = rewardMinutes,
            onConfirm = {
                // Enregistrer dans les stats
                val statsManager = StatsManager(context)
                statsManager.recordExercise(
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    reps = targetReps,
                    minutesEarned = rewardMinutes
                )
                // Créditer le temps
                val currentTime = prefs.getRemainingTimeMillis()
                prefs.setRemainingTimeMillis(currentTime + exercise.getRewardMillis(prefs))
                onExerciseComplete()
            }
        )
    } else {
        // Écran caméra avec détection
        Box(modifier = Modifier.fillMaxSize()) {
            // Preview caméra
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    val executor = Executors.newSingleThreadExecutor()

                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(executor, poseAnalyzer)
                                }

                            // Utiliser la caméra frontale pour que l'utilisateur se voie
                            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            Log.e("CameraExercise", "Camera init failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Overlay UI par-dessus la caméra
            if (!countdownFinished) {
                // Overlay countdown
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Installe-toi !",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Avatar animé montrant le mouvement attendu
                        ExerciseAvatar(
                            exerciseId = exercise.id,
                            modifier = Modifier.size(120.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "$countdown",
                            fontSize = 96.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "${exercise.emoji} ${exercise.name}",
                            fontSize = 20.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        OutlinedButton(
                            onClick = onCancel,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            )
                        ) {
                            Text("✕ Annuler")
                        }
                    }
                }
            } else {
            // UI normale de détection
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "${exercise.emoji} ${exercise.name}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        feedback,
                        fontSize = 18.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }

                // Compteur central
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val counterColor by animateColorAsState(
                        targetValue = if (currentReps >= targetReps)
                            Color(0xFF4CAF50)
                        else
                            Color.White,
                        label = "counterColor"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .size(140.dp)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "$currentReps",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = counterColor
                        )
                        Text(
                            "/ $targetReps",
                            fontSize = 18.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                // Footer avec barre de progression + bouton annuler
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = { (currentReps.toFloat() / targetReps).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "+${rewardMinutes}min si tu complètes",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("✕ Annuler")
                    }
                }
            }
            } // fin else countdownFinished
        }
    }
}

@Composable
private fun CompletionScreen(
    exercise: Exercise,
    reps: Int,
    rewardMinutes: Int,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "🎉",
            fontSize = 72.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Bravo !",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "$reps ${exercise.name} validées par la caméra !",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "+${rewardMinutes} minutes",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text(
                "✅ Récupérer mon temps",
                fontSize = 18.sp,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
