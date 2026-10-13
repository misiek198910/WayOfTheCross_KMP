package droga_krzyzowa.droga_krzyzowa.billing

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.app.Activity
import androidx.core.content.edit
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import droga_krzyzowa.droga_krzyzowa.platform.AdFreePlan
import droga_krzyzowa.droga_krzyzowa.platform.AdFreeState

/**
 * Google Play Billing for the "no ads" subscription, one client for the whole app (activities are recreated,
 * e.g. on a language change). The last known state is cached so a subscriber never sees ads before Play answers.
 */
class AdFreeBilling private constructor(private val context: Context) : PurchasesUpdatedListener {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(AdFreeState(active = prefs.getBoolean(KEY_ACTIVE, false)))
    val state: StateFlow<AdFreeState> = _state.asStateFlow()

    private var productDetails: ProductDetails? = null

    private val client = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private var connecting = false

    init {
        connect()
    }

    private fun connect() {
        if (connecting || client.isReady) return
        connecting = true
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) refresh()
                else Log.w(TAG, "Billing setup: ${result.responseCode} ${result.debugMessage}")
            }

            override fun onBillingServiceDisconnected() {
                connecting = false
            }
        })
    }

    /** Re-reads the prices and the owned subscriptions, e.g. after a cancellation in the Play Store. */
    fun refresh() {
        if (!client.isReady) return connect()
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()) { _, result ->
            productDetails = result.productDetailsList.firstOrNull()
            if (productDetails == null) Log.w(TAG, "Product $PRODUCT_ID not returned by Play: ${result.unfetchedProductList.map { it.statusCode }}")
            val plans = productDetails?.offersByBasePlan().orEmpty().map { (basePlanId, offer) ->
                // The last pricing phase is the regular recurring price; a free phase before it is the trial.
                val phases = offer.pricingPhases.pricingPhaseList
                val regular = phases.last()
                AdFreePlan(
                    id = basePlanId,
                    price = regular.formattedPrice,
                    period = regular.billingPeriod,
                    freeTrial = phases.firstOrNull { it.priceAmountMicros == 0L }?.billingPeriod,
                )
            }
            _state.update { it.copy(plans = plans) }
        }
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) handlePurchases(purchases)
        }
    }

    fun purchase(activity: Activity, basePlanId: String) {
        val details = productDetails ?: return refresh()
        val offerToken = details.offersByBasePlan()[basePlanId]?.offerToken ?: return
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()
        client.launchBillingFlow(activity, params)
    }

    fun manage() {
        val url = "https://play.google.com/store/account/subscriptions?sku=$PRODUCT_ID&package=${context.packageName}"
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            // Neither the Play Store nor a browser is available.
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> handlePurchases(purchases.orEmpty())
            // Already subscribed (e.g. on another device): re-read what the account owns.
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> refresh()
            else -> Log.d(TAG, "Purchase flow ended: ${result.responseCode} ${result.debugMessage}")
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        val owned = purchases.filter { PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED }
        // Unacknowledged purchases are refunded by Play after three days.
        owned.filterNot { it.isAcknowledged }.forEach { purchase ->
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()) { ack ->
                if (ack.responseCode != BillingClient.BillingResponseCode.OK) Log.w(TAG, "Acknowledge: ${ack.debugMessage}")
            }
        }
        val active = owned.isNotEmpty()
        prefs.edit { putBoolean(KEY_ACTIVE, active) }
        _state.update { it.copy(active = active) }
    }

    /**
     * One offer per base plan, monthly first: a free trial offer when Play lists one (it only lists offers
     * the user is eligible for), otherwise the plain base plan.
     */
    private fun ProductDetails.offersByBasePlan(): Map<String, ProductDetails.SubscriptionOfferDetails> =
        subscriptionOfferDetails.orEmpty()
            .groupBy { it.basePlanId }
            .mapValues { (_, offers) ->
                offers.firstOrNull { offer -> offer.offerId != null && offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L } }
                    ?: offers.firstOrNull { it.offerId == null }
                    ?: offers.first()
            }
            .toList()
            .sortedBy { (_, offer) -> periodDays(offer.pricingPhases.pricingPhaseList.last().billingPeriod) }
            .toMap()

    private fun periodDays(period: String): Int {
        val match = Regex("""P(\d+)([DWMY])""").matchEntire(period) ?: return Int.MAX_VALUE
        val n = match.groupValues[1].toInt()
        return n * when (match.groupValues[2]) { "D" -> 1; "W" -> 7; "M" -> 30; else -> 365 }
    }

    companion object {
        @Volatile private var instance: AdFreeBilling? = null

        fun get(context: Context): AdFreeBilling =
            instance ?: synchronized(this) { instance ?: AdFreeBilling(context.applicationContext).also { instance = it } }

        /** Play Console product of the previous app, with the base plans premium-monthly and premium-yearly (7 day trial). */
        const val PRODUCT_ID = "remove_ads_year"

        private const val TAG = "AdFreeBilling"
        const val PREFS = "billing"
        const val KEY_ACTIVE = "ad_free_active"
    }
}
