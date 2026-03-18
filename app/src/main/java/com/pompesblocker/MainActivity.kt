package com.pompesblocker

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.pompesblocker.ui.screens.AppSelectionScreen
import com.pompesblocker.ui.screens.CameraExerciseScreen
import com.pompesblocker.ui.screens.HomeScreen
import com.pompesblocker.ui.screens.SettingsScreen
import com.pompesblocker.ui.screens.StatsScreen
import com.pompesblocker.ui.theme.PompesBlockerTheme
import com.pompesblocker.data.PreferencesManager
import com.pompesblocker.model.defaultExercises
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not, we just needed to ask */ }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialiser AdMob
        MobileAds.initialize(this) {}

        // Demander la permission de notification sur Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            PompesBlockerTheme {
                val navController = rememberNavController()
                val prefs = PreferencesManager(this)

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            onNavigateToAppSelection = {
                                navController.navigate("app_selection")
                            },
                            onNavigateToSettings = {
                                navController.navigate("settings")
                            },
                            onNavigateToCamera = { exerciseId ->
                                navController.navigate("camera/$exerciseId")
                            },
                            onNavigateToStats = {
                                navController.navigate("stats")
                            }
                        )
                    }
                    composable("app_selection") {
                        AppSelectionScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("stats") {
                        StatsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("camera/{exerciseId}") { backStackEntry ->
                        val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: "pompes"
                        val exercise = defaultExercises.find { it.id == exerciseId } ?: defaultExercises[0]

                        @OptIn(ExperimentalPermissionsApi::class)
                        val cameraPermissionState = rememberPermissionState(
                            android.Manifest.permission.CAMERA
                        )

                        if (cameraPermissionState.status.isGranted) {
                            CameraExerciseScreen(
                                exercise = exercise,
                                prefs = prefs,
                                onExerciseComplete = {
                                    navController.popBackStack()
                                },
                                onCancel = {
                                    navController.popBackStack()
                                }
                            )
                        } else {
                            // Demander la permission
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("📷", fontSize = 64.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    stringResource(R.string.camera_needed),
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                @OptIn(ExperimentalPermissionsApi::class)
                                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                                    Text(stringResource(R.string.allow_camera))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { navController.popBackStack() }) {
                                    Text(stringResource(R.string.cancel_simple))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
