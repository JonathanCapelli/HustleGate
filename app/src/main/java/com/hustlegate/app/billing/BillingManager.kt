package com.hustlegate.app.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.hustlegate.app.data.PreferencesManager

class BillingManager(private val context: Context) {

    companion object {
        const val PRODUCT_REMOVE_ADS = "remove_ads"
        private const val TAG = "BillingManager"
    }

    private val prefs = PreferencesManager(context)

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        }
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            com.android.billingclient.api.PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    private var onPurchaseComplete: ((Boolean) -> Unit)? = null

    fun startConnection(onConnected: () -> Unit = {}) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing connected")
                    onConnected()
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "Billing disconnected, retrying...")
                billingClient.startConnection(this)
            }
        })
    }

    fun endConnection() {
        onPurchaseComplete = null
        billingClient.endConnection()
    }

    fun launchPurchase(activity: Activity, onComplete: (Boolean) -> Unit) {
        onPurchaseComplete = onComplete

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_REMOVE_ADS)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]
                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .build()
                        )
                    )
                    .build()
                billingClient.launchBillingFlow(activity, flowParams)
            } else {
                Log.e(TAG, "Product not found or error: ${billingResult.debugMessage}")
                onComplete(false)
            }
        }
    }

    fun restorePurchases(onResult: (Boolean) -> Unit) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasRemoveAds = purchases.any {
                    it.products.contains(PRODUCT_REMOVE_ADS) &&
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                if (hasRemoveAds) {
                    prefs.setAdsRemoved(true)
                }
                onResult(hasRemoveAds)
            } else {
                onResult(false)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (purchase.products.contains(PRODUCT_REMOVE_ADS)) {
                prefs.setAdsRemoved(true)
                onPurchaseComplete?.invoke(true)

                // Acknowledge the purchase
                if (!purchase.isAcknowledged) {
                    val acknowledgeParams = com.android.billingclient.api.AcknowledgePurchaseParams
                        .newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(acknowledgeParams) { result ->
                        Log.d(TAG, "Acknowledge result: ${result.responseCode}")
                    }
                }
            }
        }
    }
}
