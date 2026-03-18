package com.hustlegate.app.camera

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Gère les sons de feedback pendant les exercices.
 * - Bip court à chaque rep comptée
 * - Son de victoire quand l'exercice est terminé
 */
class SoundFeedback(context: Context) {

    private val toneGenerator = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    } catch (e: Exception) {
        null
    }

    /** Joue un bip court pour une rep comptée */
    fun playRepSound() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
    }

    /** Joue un son de succès pour les 3, 2, 1 du countdown */
    fun playCountdownTick() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 150)
    }

    /** Joue un son de victoire (exercice terminé) */
    fun playVictorySound() {
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
    }

    /** Joue le son "GO!" */
    fun playGoSound() {
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_CONFIRM, 300)
    }

    fun release() {
        toneGenerator?.release()
    }
}
