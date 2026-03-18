# 💪 HustleGate

Application Android qui bloque l'accès à certaines apps (Instagram, YouTube, etc.) tant que tu n'as pas fait des exercices physiques **vérifiés par caméra** pour gagner du temps d'utilisation. Intègre aussi le suivi de pas via Fitbit.

## Principe

1. Tu sélectionnes les apps que tu veux bloquer
2. Quand tu ouvres une app bloquée, un écran de blocage apparaît
3. Tu fais un exercice **devant la caméra** (ML Kit Pose Detection) pour débloquer du temps
4. Le timer décompte uniquement quand une app bloquée est au premier plan
5. Quand le temps est écoulé, l'écran de blocage réapparaît
6. Des notifications t'alertent quand il reste peu de temps (2 min, 1 min)

## Fonctionnalités

### 📷 Détection d'exercices par caméra
- Utilise **ML Kit Pose Detection** pour analyser ta pose en temps réel
- Compte les répétitions automatiquement via les angles articulaires
- Countdown de 5 secondes pour te mettre en position
- Feedback sonore : bip à chaque rep + son de victoire

### 🏋️ Exercices disponibles

| Exercice   | Répétitions | Temps gagné | Détection         |
|------------|-------------|-------------|-------------------|
| Pompes     | 10          | 15 min      | Angle coudes      |
| Tractions  | 5           | 15 min      | Angle coudes      |
| Dips       | 5           | 15 min      | Angle coudes      |
| Squats     | 10          | 15 min      | Angle genoux      |

> Tous les paramètres (reps, temps gagné) sont configurables dans les Paramètres.

### 🚶 Intégration Fitbit
- Connexion OAuth 2.0 avec PKCE
- Objectif de pas quotidien configurable (défaut : 15 000 pas)
- Récompense de temps quand l'objectif est atteint (défaut : 60 min)
- Une récompense par jour maximum

### 📊 Statistiques & Historique
- Streak de jours consécutifs (record personnel)
- Détail des exercices du jour
- Graphique des reps sur 7 jours
- Totaux : exercices, reps, minutes gagnées

### 🔔 Notifications
- Alerte à 2 minutes restantes
- Alerte à 1 minute restante
- Notification quand le temps est écoulé

### 📱 Widget home screen
- Affiche le temps restant directement sur l'écran d'accueil
- Se met à jour en temps réel quand le timer tourne
- Tap pour ouvrir l'app

### ⚙️ Paramètres configurables
- Nombre de répétitions par exercice
- Minutes gagnées par exercice
- Objectif de pas Fitbit
- Minutes gagnées pour l'objectif Fitbit

## Structure du projet

```
app/src/main/
├── java/com/pompesblocker/
│   ├── MainActivity.kt              # Écran principal + navigation
│   ├── BlockedActivity.kt           # Écran affiché quand une app est bloquée
│   ├── camera/
│   │   ├── ExerciseDetector.kt      # Logique de détection (angles articulaires)
│   │   ├── PoseAnalyzer.kt          # Analyseur d'images CameraX → ML Kit
│   │   └── SoundFeedback.kt         # Feedback sonore (bips, victoire)
│   ├── data/
│   │   ├── PreferencesManager.kt    # Stockage local (apps, timer, tokens, réglages)
│   │   └── StatsManager.kt          # Historique et statistiques des exercices
│   ├── fitbit/
│   │   └── FitbitManager.kt         # OAuth 2.0 PKCE + API Fitbit
│   ├── model/
│   │   └── Exercise.kt              # Modèle des exercices
│   ├── service/
│   │   ├── AppBlockerService.kt     # Service d'accessibilité (détection + timer)
│   │   └── NotificationHelper.kt    # Gestion des notifications
│   ├── widget/
│   │   └── TimerWidgetProvider.kt   # Widget Android (temps restant)
│   └── ui/
│       ├── theme/                    # Thème Material 3
│       └── screens/
│           ├── HomeScreen.kt        # Accueil (timer + exercices + Fitbit)
│           ├── CameraExerciseScreen.kt # Caméra avec détection de pose
│           ├── AppSelectionScreen.kt # Sélection des apps à bloquer
│           ├── SettingsScreen.kt    # Paramètres configurables
│           └── StatsScreen.kt      # Statistiques et historique
└── res/
    ├── drawable/                    # Icône + widget background
    ├── layout/                      # Layout du widget
    ├── xml/                         # Config accessibilité + widget
    └── values/                      # Strings, thèmes, couleurs
```

## Prérequis

- **Android Studio** (Hedgehog ou plus récent) : https://developer.android.com/studio
- **JDK 17** (inclus avec Android Studio)
- Un téléphone Android (version 8.0 / API 26 minimum)

## Build et installation

### 1. Ouvrir le projet

- Lance Android Studio
- **File → Open** → sélectionne le dossier `PompesBlocker`
- Attends que la synchronisation Gradle se termine (barre de progression en bas)

### 2. Préparer le téléphone

1. Va dans **Paramètres → À propos du téléphone**
2. Tape **7 fois** sur **"Numéro de build"** pour activer le mode développeur
3. Va dans **Paramètres → Options pour les développeurs**
4. Active le **Débogage USB**
5. Branche ton téléphone en USB à ton PC

### 3. Lancer l'app

1. Ton téléphone doit apparaître dans la barre d'outils en haut d'Android Studio
2. Clique sur le bouton **▶ Run** (triangle vert)
3. L'app se compile et s'installe automatiquement sur le téléphone

### 4. Build APK (sans câble)

Si tu veux générer un fichier APK pour l'installer manuellement :

1. Dans Android Studio : **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. L'APK se trouve dans `app/build/outputs/apk/debug/app-debug.apk`
3. Transfère ce fichier sur ton téléphone et installe-le (il faudra autoriser les "Sources inconnues" dans les paramètres)

## Configuration après installation

1. **Activer le service d'accessibilité** : l'app t'y invite au lancement. Va dans Paramètres → Accessibilité → PompesBlocker → Activer
2. **Sélectionner les apps à bloquer** : dans l'app, clique sur "Gérer les apps bloquées" et active le switch pour chaque app souhaitée
3. C'est prêt ! Essaie d'ouvrir une app bloquée pour vérifier que ça fonctionne

## Technologies

- **Kotlin 2.0** + **Jetpack Compose** (UI déclarative)
- **Material 3** (design moderne avec thème dynamique)
- **CameraX** + **ML Kit Pose Detection** (détection d'exercices par caméra)
- **AccessibilityService** (détection des apps au premier plan)
- **Fitbit OAuth 2.0 PKCE** (intégration pas quotidiens)
- **OkHttp** (requêtes HTTP)
- **SharedPreferences** (stockage local des données)
- **App Widget** (widget Android pour l'écran d'accueil)
- **NotificationCompat** (alertes temps restant)
