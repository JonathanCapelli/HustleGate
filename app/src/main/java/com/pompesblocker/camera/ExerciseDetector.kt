package com.pompesblocker.camera

import android.content.Context
import com.pompesblocker.R
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Détecte et compte les répétitions d'exercices en analysant les poses ML Kit.
 *
 * Logique par exercice :
 * - Pompes (push-ups)       : angle coude < 100° = bas, > 155° = haut
 * - Tractions (pull-ups)    : angle coude < 90° = haut, > 155° = bas
 * - Dips                    : angle coude < 100° = bas, > 150° = haut
 * - Squats                  : angle genou < 100° = bas, > 160° = haut
 * - Jumping Jacks           : bras levés + jambes écartées → bras baissés + jambes serrées
 * - Sit-ups                 : angle hanche < 70° = haut, > 130° = couché
 * - Burpees                 : debout (hanche haute) → au sol (hanche basse) → debout
 * - Fentes (lunges)         : angle genou avant < 100° = bas, > 155° = haut
 * - Mountain Climbers       : genou vers poitrine alterné (angle hanche < 90°)
 */
class ExerciseDetector(
    private val context: Context,
    private val exerciseId: String,
    private val targetReps: Int,
    private val onRepCounted: (currentCount: Int) -> Unit,
    private val onExerciseComplete: () -> Unit
) {
    private var repCount = 0
    private var isInDownPosition = false
    private var lastFeedback: String = context.getString(R.string.detector_get_in_position)
    private var confidence: Float = 0f

    // Pour mountain climbers : alternance gauche/droite
    private var lastMountainClimberSide: String? = null

    val currentReps: Int get() = repCount
    val feedback: String get() = lastFeedback
    val poseConfidence: Float get() = confidence

    fun reset() {
        repCount = 0
        isInDownPosition = false
        lastFeedback = context.getString(R.string.detector_get_in_position)
        confidence = 0f
        lastMountainClimberSide = null
    }

    /**
     * Analyse une pose et met à jour le compteur de reps.
     * Retourne true si une nouvelle rep a été comptée.
     */
    fun analyzePose(pose: Pose): Boolean {
        val landmarks = pose.allPoseLandmarks
        if (landmarks.isEmpty()) {
            lastFeedback = context.getString(R.string.detector_no_person)
            confidence = 0f
            return false
        }

        return when (exerciseId) {
            "pompes" -> analyzePushUp(pose)
            "tractions" -> analyzePullUp(pose)
            "dips" -> analyzeDip(pose)
            "squats" -> analyzeSquat(pose)
            "jumping_jacks" -> analyzeJumpingJack(pose)
            "situps" -> analyzeSitUp(pose)
            "burpees" -> analyzeBurpee(pose)
            "fentes" -> analyzeLunge(pose)
            "mountain_climbers" -> analyzeMountainClimber(pose)
            else -> analyzePushUp(pose) // fallback
        }
    }

    // ─── Push-ups ──────────────────────────────────────────────

    private fun analyzePushUp(pose: Pose): Boolean {
        // On utilise les deux bras et on fait la moyenne
        val leftElbowAngle = getAngle(
            pose, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST
        )
        val rightElbowAngle = getAngle(
            pose, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST
        )

        if (leftElbowAngle == null && rightElbowAngle == null) {
            lastFeedback = context.getString(R.string.detector_elbows_not_visible)
            confidence = 0f
            return false
        }

        val elbowAngle = averageAngles(leftElbowAngle, rightElbowAngle)
        confidence = getMinConfidence(pose,
            PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST,
            PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST
        )

        return checkRep(
            angle = elbowAngle,
            downThreshold = 100f,
            upThreshold = 155f,
            downFeedback = context.getString(R.string.detector_pushup_down),
            upFeedback = context.getString(R.string.detector_pushup_up),
            holdFeedback = context.getString(R.string.detector_pushup_hold)
        )
    }

    // ─── Pull-ups ──────────────────────────────────────────────

    private fun analyzePullUp(pose: Pose): Boolean {
        val leftElbowAngle = getAngle(
            pose, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST
        )
        val rightElbowAngle = getAngle(
            pose, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST
        )

        if (leftElbowAngle == null && rightElbowAngle == null) {
            lastFeedback = context.getString(R.string.detector_arms_not_visible)
            confidence = 0f
            return false
        }

        val elbowAngle = averageAngles(leftElbowAngle, rightElbowAngle)
        confidence = getMinConfidence(pose,
            PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST,
            PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST
        )

        // Pour les tractions, "down" = bras tendus (angle grand), "up" = bras pliés (angle petit)
        return checkRep(
            angle = elbowAngle,
            downThreshold = 90f,   // position haute (bras pliés) = angle < 90
            upThreshold = 155f,    // position basse (bras tendus) = angle > 155
            downFeedback = context.getString(R.string.detector_pullup_down),
            upFeedback = context.getString(R.string.detector_pullup_up),
            holdFeedback = context.getString(R.string.detector_pullup_hold)
        )
    }

    // ─── Dips ──────────────────────────────────────────────────

    private fun analyzeDip(pose: Pose): Boolean {
        val leftElbowAngle = getAngle(
            pose, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST
        )
        val rightElbowAngle = getAngle(
            pose, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST
        )

        if (leftElbowAngle == null && rightElbowAngle == null) {
            lastFeedback = context.getString(R.string.detector_arms_not_visible)
            confidence = 0f
            return false
        }

        val elbowAngle = averageAngles(leftElbowAngle, rightElbowAngle)
        confidence = getMinConfidence(pose,
            PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST,
            PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST
        )

        return checkRep(
            angle = elbowAngle,
            downThreshold = 100f,
            upThreshold = 150f,
            downFeedback = context.getString(R.string.detector_dip_down),
            upFeedback = context.getString(R.string.detector_dip_up),
            holdFeedback = context.getString(R.string.detector_dip_hold)
        )
    }

    // ─── Squats ────────────────────────────────────────────────

    private fun analyzeSquat(pose: Pose): Boolean {
        val leftKneeAngle = getAngle(
            pose, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE
        )
        val rightKneeAngle = getAngle(
            pose, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE
        )

        if (leftKneeAngle == null && rightKneeAngle == null) {
            lastFeedback = context.getString(R.string.detector_legs_not_visible)
            confidence = 0f
            return false
        }

        val kneeAngle = averageAngles(leftKneeAngle, rightKneeAngle)
        confidence = getMinConfidence(pose,
            PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE,
            PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE
        )

        return checkRep(
            angle = kneeAngle,
            downThreshold = 100f,
            upThreshold = 160f,
            downFeedback = context.getString(R.string.detector_squat_down),
            upFeedback = context.getString(R.string.detector_squat_up),
            holdFeedback = context.getString(R.string.detector_squat_hold)
        )
    }

    // ─── Jumping Jacks ────────────────────────────────────────

    private fun analyzeJumpingJack(pose: Pose): Boolean {
        // Bras : angle épaule (hanche-épaule-poignet)
        val leftArmAngle = getAngle(
            pose, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_WRIST
        )
        val rightArmAngle = getAngle(
            pose, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_WRIST
        )

        if (leftArmAngle == null && rightArmAngle == null) {
            lastFeedback = context.getString(R.string.detector_body_not_visible)
            confidence = 0f
            return false
        }

        val armAngle = averageAngles(leftArmAngle, rightArmAngle)
        confidence = getMinConfidence(pose,
            PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_WRIST,
            PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_WRIST
        )

        // Bras levés > 140° (position ouverte), bras baissés < 60° (position fermée)
        return checkRep(
            angle = armAngle,
            downThreshold = 60f,   // bras le long du corps = position fermée
            upThreshold = 140f,    // bras levés = position ouverte
            downFeedback = context.getString(R.string.detector_jj_down),
            upFeedback = context.getString(R.string.detector_jj_up),
            holdFeedback = context.getString(R.string.detector_jj_hold)
        )
    }

    // ─── Sit-ups ──────────────────────────────────────────────

    private fun analyzeSitUp(pose: Pose): Boolean {
        // Angle de la hanche : épaule-hanche-genou
        val leftHipAngle = getAngle(
            pose, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE
        )
        val rightHipAngle = getAngle(
            pose, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE
        )

        if (leftHipAngle == null && rightHipAngle == null) {
            lastFeedback = context.getString(R.string.detector_torso_not_visible)
            confidence = 0f
            return false
        }

        val hipAngle = averageAngles(leftHipAngle, rightHipAngle)
        confidence = getMinConfidence(pose,
            PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE,
            PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE
        )

        // Couché = angle hanche grand (~150-180°), assis = angle hanche petit (<70°)
        return checkRep(
            angle = hipAngle,
            downThreshold = 70f,   // position haute (assis)
            upThreshold = 130f,    // position basse (couché)
            downFeedback = context.getString(R.string.detector_situp_down),
            upFeedback = context.getString(R.string.detector_situp_up),
            holdFeedback = context.getString(R.string.detector_situp_hold)
        )
    }

    // ─── Burpees ──────────────────────────────────────────────

    private fun analyzeBurpee(pose: Pose): Boolean {
        // On mesure la distance verticale relative entre les hanches et les chevilles
        // Debout = hanches bien au-dessus des chevilles → angle hanche épaule-hanche-cheville grand
        // Au sol = hanches proches du sol → angle petit
        val leftHipAngle = getAngle(
            pose, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_ANKLE
        )
        val rightHipAngle = getAngle(
            pose, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_ANKLE
        )

        if (leftHipAngle == null && rightHipAngle == null) {
            lastFeedback = context.getString(R.string.detector_body_step_back)
            confidence = 0f
            return false
        }

        val hipAngle = averageAngles(leftHipAngle, rightHipAngle)
        confidence = getMinConfidence(pose,
            PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_ANKLE,
            PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_ANKLE
        )

        // Debout = angle ~170-180°, au sol / planche = angle < 90°
        return checkRep(
            angle = hipAngle,
            downThreshold = 90f,   // position au sol
            upThreshold = 155f,    // position debout
            downFeedback = context.getString(R.string.detector_burpee_down),
            upFeedback = context.getString(R.string.detector_burpee_up),
            holdFeedback = context.getString(R.string.detector_burpee_hold)
        )
    }

    // ─── Fentes (Lunges) ──────────────────────────────────────

    private fun analyzeLunge(pose: Pose): Boolean {
        // On prend l'angle du genou le plus plié (le genou avant dans la fente)
        val leftKneeAngle = getAngle(
            pose, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE
        )
        val rightKneeAngle = getAngle(
            pose, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE
        )

        if (leftKneeAngle == null && rightKneeAngle == null) {
            lastFeedback = context.getString(R.string.detector_legs_not_visible)
            confidence = 0f
            return false
        }

        // Pour les fentes, on prend le genou le plus plié (angle minimum)
        val kneeAngle = when {
            leftKneeAngle != null && rightKneeAngle != null -> minOf(leftKneeAngle, rightKneeAngle)
            leftKneeAngle != null -> leftKneeAngle
            else -> rightKneeAngle!!
        }
        confidence = getMinConfidence(pose,
            PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE,
            PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE
        )

        return checkRep(
            angle = kneeAngle,
            downThreshold = 100f,
            upThreshold = 155f,
            downFeedback = context.getString(R.string.detector_lunge_down),
            upFeedback = context.getString(R.string.detector_lunge_up),
            holdFeedback = context.getString(R.string.detector_lunge_hold)
        )
    }

    // ─── Mountain Climbers ────────────────────────────────────

    private fun analyzeMountainClimber(pose: Pose): Boolean {
        // Angle hanche (épaule-hanche-genou) pour détecter le genou qui monte vers la poitrine
        val leftHipAngle = getAngle(
            pose, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE
        )
        val rightHipAngle = getAngle(
            pose, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE
        )

        if (leftHipAngle == null && rightHipAngle == null) {
            lastFeedback = context.getString(R.string.detector_side_view)
            confidence = 0f
            return false
        }

        confidence = getMinConfidence(pose,
            PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE,
            PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE
        )

        // Genou monté vers poitrine = angle hanche < 90°
        val leftUp = leftHipAngle != null && leftHipAngle < 90f
        val rightUp = rightHipAngle != null && rightHipAngle < 90f

        if (leftUp && lastMountainClimberSide != "left") {
            lastMountainClimberSide = "left"
            repCount++
            lastFeedback = context.getString(R.string.detector_rep_count, repCount, targetReps)
            onRepCounted(repCount)
            if (repCount >= targetReps) onExerciseComplete()
            return true
        }
        if (rightUp && lastMountainClimberSide != "right") {
            lastMountainClimberSide = "right"
            repCount++
            lastFeedback = context.getString(R.string.detector_rep_count, repCount, targetReps)
            onRepCounted(repCount)
            if (repCount >= targetReps) onExerciseComplete()
            return true
        }

        if (!leftUp && !rightUp) {
            lastMountainClimberSide = null
            lastFeedback = context.getString(R.string.detector_mc_knee_up)
        } else {
            lastFeedback = context.getString(R.string.detector_mc_alternate)
        }

        return false
    }

    // ─── Helpers ───────────────────────────────────────────────

    /**
     * Logique commune de comptage : descend sous [downThreshold], remonte au-dessus de [upThreshold] = 1 rep.
     */
    private fun checkRep(
        angle: Float,
        downThreshold: Float,
        upThreshold: Float,
        downFeedback: String,
        upFeedback: String,
        holdFeedback: String
    ): Boolean {
        if (angle < downThreshold) {
            if (!isInDownPosition) {
                isInDownPosition = true
                lastFeedback = holdFeedback
            }
        }

        if (angle > upThreshold && isInDownPosition) {
            isInDownPosition = false
            repCount++
            lastFeedback = context.getString(R.string.detector_rep_count, repCount, targetReps)
            onRepCounted(repCount)
            if (repCount >= targetReps) {
                onExerciseComplete()
            }
            return true
        }

        if (!isInDownPosition) {
            lastFeedback = downFeedback
        }

        return false
    }

    /**
     * Calcule l'angle entre 3 points (au point du milieu).
     */
    private fun getAngle(pose: Pose, firstType: Int, midType: Int, lastType: Int): Float? {
        val first = pose.getPoseLandmark(firstType) ?: return null
        val mid = pose.getPoseLandmark(midType) ?: return null
        val last = pose.getPoseLandmark(lastType) ?: return null

        // Vérifier la confiance minimale
        if (first.inFrameLikelihood < 0.5f || mid.inFrameLikelihood < 0.5f || last.inFrameLikelihood < 0.5f) {
            return null
        }

        val angle = Math.toDegrees(
            atan2(
                (last.position.y - mid.position.y).toDouble(),
                (last.position.x - mid.position.x).toDouble()
            ) - atan2(
                (first.position.y - mid.position.y).toDouble(),
                (first.position.x - mid.position.x).toDouble()
            )
        ).toFloat()

        return abs(angle).let { if (it > 180f) 360f - it else it }
    }

    private fun averageAngles(a: Float?, b: Float?): Float {
        return when {
            a != null && b != null -> (a + b) / 2f
            a != null -> a
            b != null -> b
            else -> 180f // fallback, shouldn't happen
        }
    }

    private fun getMinConfidence(pose: Pose, vararg landmarkTypes: Int): Float {
        var minConf = 1f
        for (type in landmarkTypes) {
            val landmark = pose.getPoseLandmark(type)
            if (landmark != null) {
                minConf = minOf(minConf, landmark.inFrameLikelihood)
            }
        }
        return minConf
    }
}
