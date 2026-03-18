# 💪 HustleGate

Application Android de bien-être numérique qui bloque l'accès à certaines apps tant que tu n'as pas fait des exercices physiques **vérifiés par caméra** ou atteint ton objectif de pas quotidien.

## Principe

1. Tu sélectionnes les apps que tu veux bloquer
2. Quand tu ouvres une app bloquée, un écran de blocage apparaît
3. Tu fais un exercice **devant la caméra** (IA Pose Detection) pour débloquer du temps
4. Ou tu marches ! Tes pas comptent aussi grâce à **Santé Connect**
5. Le timer décompte uniquement quand une app bloquée est au premier plan
6. Quand le temps est écoulé, l'écran de blocage réapparaît
7. Des notifications t'alertent quand il reste peu de temps

## Fonctionnalités

### 📷 Détection d'exercices par caméra
- **ML Kit Pose Detection** analyse ta pose en temps réel
- Compte les répétitions automatiquement via les angles articulaires
- Countdown de 5 secondes pour te mettre en position
- Feedback sonore : bip à chaque rep + son de victoire
- Avatar animé pour chaque exercice

### 🏋️ 9 exercices disponibles

| Exercice           | Emoji | Détection            |
|--------------------|-------|----------------------|
| Pompes             | 💪    | Angle coudes         |
| Tractions          | 🏋️   | Angle coudes         |
| Dips               | 💺    | Angle coudes         |
| Squats             | 🦵    | Angle genoux         |
| Jumping Jacks      | ⭐    | Bras + jambes écartés |
| Abdos (Sit-ups)    | 🔥    | Angle tronc          |
| Burpees            | 🏃    | Séquence complète    |
| Fentes (Lunges)    | 🦿    | Angle genoux         |
| Mountain Climbers  | ⛰️    | Genoux alternés      |

> Tous les paramètres (reps, temps gagné) sont configurables dans les Paramètres.

### 🚶 Objectif de pas (Santé Connect / Health Connect)
- Lecture des pas du jour via **Health Connect**
- Objectif quotidien configurable (défaut : 15 000 pas)
- Récompense de temps quand l'objectif est atteint (défaut : 60 min)
- Une récompense par jour maximum
- Aucune donnée envoyée à l'extérieur

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
- Affiche le temps restant sur l'écran d'accueil
- Mise à jour en temps réel
- Tap pour ouvrir l'app

### 💎 Modèle freemium
- Application **gratuite** avec bannière publicitaire
- Achat unique (3 €) pour supprimer les pubs
- Restauration d'achat disponible dans les paramètres

### ⚙️ Paramètres configurables
- Nombre de répétitions par exercice
- Minutes gagnées par exercice
- Objectif de pas quotidien
- Minutes gagnées pour l'objectif de pas

## Structure du projet

```
app/src/main/
├── java/com/hustlegate/app/
│   ├── MainActivity.kt              # Écran principal + navigation
│   ├── BlockedActivity.kt           # Écran affiché quand une app est bloquée
│   ├── billing/
│   │   └── BillingManager.kt        # Achat in-app Google Play Billing
│   ├── camera/
│   │   ├── ExerciseDetector.kt      # Logique de détection (angles articulaires)
│   │   ├── PoseAnalyzer.kt          # Analyseur d'images CameraX → ML Kit
│   │   └── SoundFeedback.kt         # Feedback sonore (bips, victoire)
│   ├── data/
│   │   ├── PreferencesManager.kt    # Stockage local (apps, timer, réglages)
│   │   └── StatsManager.kt          # Historique et statistiques
│   ├── health/
│   │   └── HealthConnectManager.kt  # Lecture des pas via Health Connect
│   ├── model/
│   │   └── Exercise.kt              # Modèle des exercices
│   ├── service/
│   │   ├── AppBlockerService.kt     # Service d'accessibilité (détection + timer)
│   │   └── NotificationHelper.kt    # Gestion des notifications
│   ├── widget/
│   │   └── TimerWidgetProvider.kt   # Widget Android (temps restant)
│   └── ui/
│       ├── components/
│       │   ├── AdBanner.kt          # Bannière publicitaire AdMob
│       │   └── ExerciseAvatar.kt    # Avatars animés des exercices
│       ├── theme/                    # Thème Material 3
│       └── screens/
│           ├── HomeScreen.kt        # Accueil (timer + exercices + pas)
│           ├── CameraExerciseScreen.kt # Caméra avec détection de pose
│           ├── AppSelectionScreen.kt # Sélection des apps à bloquer
│           ├── SettingsScreen.kt    # Paramètres + achat in-app
│           └── StatsScreen.kt      # Statistiques et historique
├── res/
│   ├── drawable/                    # Icône + widget background
│   ├── layout/                      # Layout du widget
│   ├── xml/                         # Config accessibilité + widget
│   └── values/                      # Strings, thèmes, couleurs
└── docs/
    └── privacy-policy.html          # Politique de confidentialité
```

## Prérequis

- **Android Studio** (Hedgehog ou plus récent) : https://developer.android.com/studio
- **JDK 17** (inclus avec Android Studio)
- Un téléphone Android (version 8.0 / API 26 minimum)

## Build et installation

### 1. Ouvrir le projet

- Lance Android Studio
- **File → Open** → sélectionne le dossier `HustleGate`
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

1. **Activer le service d'accessibilité** : l'app t'y invite au lancement. Va dans Paramètres → Accessibilité → HustleGate → Activer
2. **Sélectionner les apps à bloquer** : dans l'app, clique sur "Gérer les apps bloquées" et active le switch pour chaque app souhaitée
3. C'est prêt ! Essaie d'ouvrir une app bloquée pour vérifier que ça fonctionne

## Technologies

- **Kotlin 2.0** + **Jetpack Compose** (UI déclarative)
- **Material 3** (design moderne avec thème dynamique)
- **CameraX** + **ML Kit Pose Detection** (détection d'exercices par caméra)
- **AccessibilityService** (détection des apps au premier plan)
- **Health Connect** (lecture des pas quotidiens)
- **Google AdMob** (bannière publicitaire)
- **Google Play Billing** (achat in-app)
- **SharedPreferences** (stockage local)
- **App Widget** (widget Android)
- **NotificationCompat** (alertes temps restant)
