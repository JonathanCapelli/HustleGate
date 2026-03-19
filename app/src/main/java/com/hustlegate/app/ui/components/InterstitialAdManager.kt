package com.hustlegate.app.ui.components

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class InterstitialAdManager(private val context: Context) {

    private var interstitialAd: InterstitialAd? = null

    companion object {
        private const val AD_UNIT_ID = "ca-app-pub-2919600045223555/3024877566"
        private const val TAG = "InterstitialAd"
    }

    fun load() {
        InterstitialAd.load(
            context,
            AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d(TAG, "Interstitial failed to load: ${error.message}")
                    interstitialAd = null
                }
            }
        )
    }

    fun showIfReady(activity: Activity, onDismissed: () -> Unit) {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    load() // Preload next one
                    onDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    interstitialAd = null
                    load()
                    onDismissed()
                }
            }
            ad.show(activity)
        } else {
            // Ad not ready, don't block the user
            load()
            onDismissed()
        }
    }
}
