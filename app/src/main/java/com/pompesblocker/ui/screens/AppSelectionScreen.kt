package com.pompesblocker.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.pompesblocker.data.PreferencesManager

private data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    var blockedApps by remember { mutableStateOf(prefs.getBlockedApps()) }
    var searchQuery by remember { mutableStateOf("") }

    // Charger les apps installées (une seule fois)
    val installedApps = remember {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
            .mapNotNull { resolveInfo ->
                val appPackage = resolveInfo.activityInfo.packageName
                // Ne pas afficher notre propre app
                if (appPackage == context.packageName) return@mapNotNull null
                try {
                    AppInfo(
                        name = resolveInfo.loadLabel(pm).toString(),
                        packageName = appPackage,
                        icon = resolveInfo.loadIcon(pm)
                    )
                } catch (e: Exception) {
                    null
                }
            }
            .distinctBy { it.packageName }
            .sortedWith(compareBy { it.name.lowercase() })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Apps bloquées") },
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
        ) {
            // Barre de recherche
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Rechercher une app...") },
                singleLine = true
            )

            // Compteur
            Text(
                "${blockedApps.size} app(s) bloquée(s)",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Liste des apps
            val filteredApps = if (searchQuery.isBlank()) {
                installedApps
            } else {
                installedApps.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                            it.packageName.contains(searchQuery, ignoreCase = true)
                }
            }

            LazyColumn {
                items(filteredApps, key = { it.packageName }) { app ->
                    val isBlocked = blockedApps.contains(app.packageName)
                    ListItem(
                        headlineContent = { Text(app.name) },
                        supportingContent = {
                            Text(
                                app.packageName,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = {
                            Image(
                                bitmap = app.icon.toBitmap(48, 48).asImageBitmap(),
                                contentDescription = app.name,
                                modifier = Modifier.size(40.dp)
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = isBlocked,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        prefs.addBlockedApp(app.packageName)
                                    } else {
                                        prefs.removeBlockedApp(app.packageName)
                                    }
                                    blockedApps = prefs.getBlockedApps()
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}
