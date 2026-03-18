package com.hustlegate.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Affiche un avatar stick-figure animé montrant le mouvement d'un exercice.
 * Utilise Canvas Compose pour dessiner un personnage qui bouge en boucle.
 */
@Composable
fun ExerciseAvatar(
    exerciseId: String,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "avatar")
    // Phase d'animation 0→1→0 (aller-retour)
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )

    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(120.dp)) {
            when (exerciseId) {
                "pompes" -> drawPushUp(phase)
                "tractions" -> drawPullUp(phase)
                "dips" -> drawDip(phase)
                "squats" -> drawSquat(phase)
                "jumping_jacks" -> drawJumpingJack(phase)
                "situps" -> drawSitUp(phase)
                "burpees" -> drawBurpee(phase)
                "fentes" -> drawLunge(phase)
                "mountain_climbers" -> drawMountainClimber(phase)
                else -> drawPushUp(phase)
            }
        }
    }
}

private val bodyColor = Color.White
private val accentColor = Color(0xFF4CAF50)
private const val STROKE = 4f

private fun DrawScope.drawLimb(from: Offset, to: Offset, color: Color = bodyColor) {
    drawLine(color, from, to, strokeWidth = STROKE, cap = StrokeCap.Round)
}

private fun DrawScope.drawHead(center: Offset, radius: Float = 12f) {
    drawCircle(bodyColor, radius, center)
}

// === Position helpers ===
private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
private fun lerpOffset(a: Offset, b: Offset, t: Float) = Offset(lerp(a.x, b.x, t), lerp(a.y, b.y, t))
private fun offsetFromAngle(origin: Offset, angleDeg: Float, length: Float): Offset {
    val rad = angleDeg * PI.toFloat() / 180f
    return Offset(origin.x + cos(rad) * length, origin.y + sin(rad) * length)
}

// ─── Push-ups ──────────────────────────────────────────────
private fun DrawScope.drawPushUp(phase: Float) {
    val w = size.width; val h = size.height
    val floorY = h * 0.88f
    // Sol
    drawLimb(Offset(w * 0.05f, floorY), Offset(w * 0.95f, floorY), accentColor)

    // Pieds au sol à droite, mains au sol à gauche
    val feetPos = Offset(w * 0.82f, floorY)
    val handPos = Offset(w * 0.15f, floorY)

    // Épaule descend/monte — corps reste aligné (planche droite)
    val shoulderY = lerp(h * 0.55f, h * 0.72f, phase)
    val shoulderPos = Offset(w * 0.25f, shoulderY)
    // Hanche alignée entre épaule et pieds
    val hipAligned = Offset(
        (shoulderPos.x + feetPos.x) / 2f,
        (shoulderPos.y + feetPos.y) / 2f
    )
    val headPos = Offset(shoulderPos.x - 5f, shoulderPos.y - 16f)

    drawHead(headPos)
    drawLimb(headPos, shoulderPos) // cou
    drawLimb(shoulderPos, handPos) // bras
    drawLimb(shoulderPos, hipAligned) // tronc — DROIT
    drawLimb(hipAligned, feetPos) // jambes — ALIGNÉ avec le tronc
}

// ─── Pull-ups ──────────────────────────────────────────────
private fun DrawScope.drawPullUp(phase: Float) {
    val w = size.width; val h = size.height
    val barY = h * 0.1f
    // Barre
    drawLimb(Offset(w * 0.2f, barY), Offset(w * 0.8f, barY), accentColor)

    val bodyTop = lerp(h * 0.25f, h * 0.45f, phase)
    val headPos = Offset(w * 0.5f, bodyTop - 15f)
    val shoulder = Offset(w * 0.5f, bodyTop)
    val hip = Offset(w * 0.5f, bodyTop + h * 0.25f)
    val leftFoot = Offset(w * 0.42f, bodyTop + h * 0.45f)
    val rightFoot = Offset(w * 0.58f, bodyTop + h * 0.45f)
    val leftHand = Offset(w * 0.35f, barY)
    val rightHand = Offset(w * 0.65f, barY)

    drawHead(headPos)
    drawLimb(headPos, shoulder)
    drawLimb(shoulder, leftHand)
    drawLimb(shoulder, rightHand)
    drawLimb(shoulder, hip)
    drawLimb(hip, leftFoot)
    drawLimb(hip, rightFoot)
}

// ─── Dips ──────────────────────────────────────────────────
private fun DrawScope.drawDip(phase: Float) {
    val w = size.width; val h = size.height
    val barY = h * 0.35f
    // Barres parallèles
    drawLimb(Offset(w * 0.15f, barY), Offset(w * 0.4f, barY), accentColor)
    drawLimb(Offset(w * 0.6f, barY), Offset(w * 0.85f, barY), accentColor)

    val shoulderY = lerp(barY - h * 0.08f, barY + h * 0.08f, phase)
    val shoulder = Offset(w * 0.5f, shoulderY)
    val head = Offset(w * 0.5f, shoulderY - 18f)
    val hip = Offset(w * 0.5f, shoulderY + h * 0.2f)
    val leftFoot = Offset(w * 0.42f, shoulderY + h * 0.42f)
    val rightFoot = Offset(w * 0.58f, shoulderY + h * 0.42f)
    val leftHand = Offset(w * 0.35f, barY)
    val rightHand = Offset(w * 0.65f, barY)

    drawHead(head)
    drawLimb(head, shoulder)
    drawLimb(shoulder, leftHand)
    drawLimb(shoulder, rightHand)
    drawLimb(shoulder, hip)
    drawLimb(hip, leftFoot)
    drawLimb(hip, rightFoot)
}

// ─── Squats ────────────────────────────────────────────────
private fun DrawScope.drawSquat(phase: Float) {
    val w = size.width; val h = size.height
    val floorY = h * 0.88f
    // Sol
    drawLimb(Offset(w * 0.05f, floorY), Offset(w * 0.95f, floorY), accentColor)

    val hipY = lerp(h * 0.4f, h * 0.55f, phase)
    val kneeY = lerp(h * 0.6f, h * 0.7f, phase)

    val head = Offset(w * 0.5f, hipY - h * 0.18f)
    val shoulder = Offset(w * 0.5f, hipY - h * 0.08f)
    val hip = Offset(w * 0.5f, hipY)
    val leftKnee = Offset(w * 0.38f, kneeY)
    val rightKnee = Offset(w * 0.62f, kneeY)
    val leftFoot = Offset(w * 0.35f, floorY)
    val rightFoot = Offset(w * 0.65f, floorY)
    val leftHand = Offset(w * 0.3f, hipY - h * 0.02f)
    val rightHand = Offset(w * 0.7f, hipY - h * 0.02f)

    drawHead(head)
    drawLimb(head, shoulder)
    drawLimb(shoulder, hip)
    drawLimb(shoulder, leftHand)
    drawLimb(shoulder, rightHand)
    drawLimb(hip, leftKnee)
    drawLimb(hip, rightKnee)
    drawLimb(leftKnee, leftFoot)
    drawLimb(rightKnee, rightFoot)
}

// ─── Jumping Jacks ─────────────────────────────────────────
private fun DrawScope.drawJumpingJack(phase: Float) {
    val w = size.width; val h = size.height
    // Phase 0 = position fermée (bras le long du corps, pieds joints, au sol)
    // Phase 1 = position ouverte (bras levés, jambes écartées, en l'air)
    val jumpHeight = lerp(0f, h * 0.08f, phase) // le corps entier monte
    val armAngle = lerp(-15f, -75f, phase) // bras montent
    val legSpread = lerp(0.03f, 0.25f, phase) // jambes s'écartent

    val baseY = h * 0.22f - jumpHeight
    val head = Offset(w * 0.5f, baseY)
    val shoulder = Offset(w * 0.5f, baseY + h * 0.12f)
    val hip = Offset(w * 0.5f, baseY + h * 0.35f)

    val leftHand = offsetFromAngle(shoulder, 180f + armAngle, w * 0.28f)
    val rightHand = offsetFromAngle(shoulder, -armAngle, w * 0.28f)

    val footY = baseY + h * 0.65f
    val leftKnee = Offset(w * (0.5f - legSpread * 0.5f), baseY + h * 0.5f)
    val rightKnee = Offset(w * (0.5f + legSpread * 0.5f), baseY + h * 0.5f)
    val leftFoot = Offset(w * (0.5f - legSpread), footY)
    val rightFoot = Offset(w * (0.5f + legSpread), footY)

    // Sol (fixe)
    drawLimb(Offset(w * 0.05f, h * 0.88f), Offset(w * 0.95f, h * 0.88f), accentColor)

    drawHead(head)
    drawLimb(head, shoulder)
    drawLimb(shoulder, hip)
    drawLimb(shoulder, leftHand)
    drawLimb(shoulder, rightHand)
    drawLimb(hip, leftKnee)
    drawLimb(hip, rightKnee)
    drawLimb(leftKnee, leftFoot)
    drawLimb(rightKnee, rightFoot)
}

// ─── Sit-ups ───────────────────────────────────────────────
private fun DrawScope.drawSitUp(phase: Float) {
    val w = size.width; val h = size.height
    val floorY = h * 0.82f
    // Sol
    drawLimb(Offset(w * 0.05f, floorY), Offset(w * 0.95f, floorY), accentColor)

    // Hanche fixe au sol, jambes pliées fixes (genoux en haut, pieds au sol)
    val hipPos = Offset(w * 0.5f, floorY - 4f)
    val knees = Offset(w * 0.72f, h * 0.58f)
    val feet = Offset(w * 0.82f, floorY - 4f)

    // Buste : phase 0 = allongé à gauche, phase 1 = assis vers les genoux
    // Angle 0° = à plat vers la gauche, 80° = assis (arrêt AVANT les genoux)
    val trunkAngleDeg = lerp(5f, 75f, phase)
    val trunkRad = trunkAngleDeg * PI.toFloat() / 180f
    val trunkLen = w * 0.28f
    val shoulder = Offset(
        hipPos.x - cos(trunkRad) * trunkLen,
        hipPos.y - sin(trunkRad) * trunkLen
    )
    val headOffset = 16f
    val head = Offset(
        shoulder.x - cos(trunkRad) * headOffset,
        shoulder.y - sin(trunkRad) * headOffset
    )

    // Bras suivent le buste
    val handTarget = lerpOffset(
        Offset(shoulder.x - 15f, shoulder.y - 5f), // allongé : bras au-dessus de la tête
        Offset(knees.x - 15f, knees.y + 8f), // assis : mains vers genoux (pas à travers)
        phase
    )

    drawHead(head)
    drawLimb(head, shoulder)
    drawLimb(shoulder, hipPos)
    drawLimb(shoulder, handTarget) // bras
    drawLimb(hipPos, knees)
    drawLimb(knees, feet)
}

// ─── Burpees ───────────────────────────────────────────────
private fun DrawScope.drawBurpee(phase: Float) {
    val w = size.width; val h = size.height
    // Sol
    drawLimb(Offset(w * 0.05f, h * 0.88f), Offset(w * 0.95f, h * 0.88f), accentColor)

    // Phase 0 = debout, Phase 1 = en planche au sol
    // Personnage vu de profil
    val feetX = lerp(w * 0.5f, w * 0.8f, phase)
    val feetY = h * 0.85f
    val hipX = lerp(w * 0.5f, w * 0.55f, phase)
    val hipY = lerp(h * 0.5f, h * 0.72f, phase)
    val shoulderX = lerp(w * 0.5f, w * 0.3f, phase)
    val shoulderY = lerp(h * 0.3f, h * 0.65f, phase)
    val headX = lerp(w * 0.5f, w * 0.22f, phase)
    val headY = lerp(h * 0.18f, h * 0.52f, phase)
    val handX = lerp(w * 0.35f, w * 0.18f, phase)
    val handY = lerp(h * 0.45f, h * 0.85f, phase)

    val hipPos = Offset(hipX, hipY)
    val shoulderPos = Offset(shoulderX, shoulderY)
    val headPos = Offset(headX, headY)
    val handPos = Offset(handX, handY)
    val feetPos = Offset(feetX, feetY)
    val kneePos = Offset((hipX + feetX) / 2f, lerp(h * 0.7f, h * 0.78f, phase))

    drawHead(headPos)
    drawLimb(headPos, shoulderPos)
    drawLimb(shoulderPos, hipPos) // tronc
    drawLimb(shoulderPos, handPos) // bras
    drawLimb(hipPos, kneePos) // cuisse
    drawLimb(kneePos, feetPos) // tibia
}

// ─── Fentes (Lunges) ───────────────────────────────────────
private fun DrawScope.drawLunge(phase: Float) {
    val w = size.width; val h = size.height
    // Sol
    drawLimb(Offset(w * 0.05f, h * 0.88f), Offset(w * 0.95f, h * 0.88f), accentColor)

    // Phase 0 = debout, Phase 1 = fente profonde
    // Le buste descend verticalement, jambe avant pliée, jambe arrière étirée
    val hipY = lerp(h * 0.42f, h * 0.55f, phase)
    val hipPos = Offset(w * 0.45f, hipY)
    val shoulderPos = Offset(w * 0.45f, hipY - h * 0.15f)
    val headPos = Offset(w * 0.45f, hipY - h * 0.27f)

    // Bras le long du corps
    val leftHand = Offset(w * 0.35f, hipY + h * 0.02f)
    val rightHand = Offset(w * 0.55f, hipY + h * 0.02f)

    // Jambe avant (gauche) : genou avancé, pied au sol devant
    val frontKneeX = lerp(w * 0.4f, w * 0.25f, phase)
    val frontKneeY = lerp(h * 0.65f, h * 0.72f, phase)
    val frontFoot = Offset(lerp(w * 0.42f, w * 0.2f, phase), h * 0.85f)

    // Jambe arrière : genou descend, pied au sol derrière
    val backKneeX = lerp(w * 0.5f, w * 0.65f, phase)
    val backKneeY = lerp(h * 0.65f, h * 0.8f, phase)
    val backFoot = Offset(lerp(w * 0.48f, w * 0.78f, phase), h * 0.85f)

    drawHead(headPos)
    drawLimb(headPos, shoulderPos)
    drawLimb(shoulderPos, hipPos)
    drawLimb(shoulderPos, leftHand)
    drawLimb(shoulderPos, rightHand)
    drawLimb(hipPos, Offset(frontKneeX, frontKneeY))
    drawLimb(Offset(frontKneeX, frontKneeY), frontFoot)
    drawLimb(hipPos, Offset(backKneeX, backKneeY))
    drawLimb(Offset(backKneeX, backKneeY), backFoot)
}

// ─── Mountain Climbers ─────────────────────────────────────
private fun DrawScope.drawMountainClimber(phase: Float) {
    val w = size.width; val h = size.height
    val floorY = h * 0.88f
    // Sol
    drawLimb(Offset(w * 0.05f, floorY), Offset(w * 0.95f, floorY), accentColor)

    // Position planche fixe — seules les jambes alternent en continu
    val hands = Offset(w * 0.18f, floorY)
    val shoulder = Offset(w * 0.25f, h * 0.52f)
    val hip = Offset(w * 0.6f, h * 0.55f)
    val head = Offset(w * 0.2f, h * 0.4f)

    // Animation continue sans coupure :
    // Phase va de 0→1→0 (RepeatMode.Reverse)
    // On utilise sin pour un mouvement fluide et symétrique
    // Quand t>0 : genou gauche monte, droit étendu
    // Quand t<0 : genou droit monte, gauche étendu
    val t = sin(phase * PI.toFloat()) * 2f - 1f // -1 → 1 → -1 fluide

    val restKnee = Offset(w * 0.72f, h * 0.7f)
    val restFoot = Offset(w * 0.82f, h * 0.82f)
    val bentKnee = Offset(w * 0.42f, h * 0.55f)
    val bentFoot = Offset(w * 0.48f, h * 0.65f)

    val leftT = (t.coerceIn(0f, 1f))  // 0→1 : gauche monte
    val rightT = ((-t).coerceIn(0f, 1f)) // 0→1 : droit monte

    val leftKnee = lerpOffset(restKnee, bentKnee, leftT)
    val leftFoot = lerpOffset(restFoot, bentFoot, leftT)
    val rightKnee = lerpOffset(restKnee, bentKnee, rightT)
    val rightFoot = lerpOffset(restFoot, bentFoot, rightT)

    drawHead(head)
    drawLimb(head, shoulder)
    drawLimb(shoulder, hands) // bras
    drawLimb(shoulder, hip) // tronc
    drawLimb(hip, leftKnee)
    drawLimb(leftKnee, leftFoot)
    drawLimb(hip, rightKnee)
    drawLimb(rightKnee, rightFoot)
}
