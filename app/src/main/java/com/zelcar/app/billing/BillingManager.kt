package com.zelcar.app.billing

import android.app.Activity
import android.content.Context
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
import com.zelcar.app.BuildConfig
import com.zelcar.app.data.Plan
import com.zelcar.app.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Assinatura Zelcar Pro pelo Google Play Billing (RF25).
 * Crie no Play Console duas assinaturas com os ids abaixo.
 */
class BillingManager(
    context: Context,
    private val scope: CoroutineScope,
    private val prefs: Prefs,
) : PurchasesUpdatedListener {

    sealed interface Event {
        data object Purchased : Event
        data object Cancelled : Event
        data class Failed(val message: String) : Event
    }

    companion object {
        const val MONTHLY_ID = "zelcar_pro_monthly"
        const val ANNUAL_ID = "zelcar_pro_annual"
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 4)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    private val _prices = MutableStateFlow<Map<String, String>>(emptyMap())
    /** productId -> preço formatado pela Play Store (ex.: "R$ 14,90"). */
    val prices: StateFlow<Map<String, String>> = _prices.asStateFlow()

    private var details: Map<String, ProductDetails> = emptyMap()

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    fun start() {
        if (client.isReady) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    loadProducts()
                    refreshEntitlement()
                }
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    val isAvailable: Boolean get() = client.isReady && details.isNotEmpty()

    private fun loadProducts() {
        val products = listOf(MONTHLY_ID, ANNUAL_ID).map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(products).build()) { result, list ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            details = list.associateBy { it.productId }
            _prices.value = list.mapNotNull { d ->
                val price = d.subscriptionOfferDetails?.firstOrNull()
                    ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                price?.let { d.productId to it }
            }.toMap()
        }
    }

    /** Lê as assinaturas ativas e alinha o plano local. Em debug nunca rebaixa (permite Pro manual). */
    fun refreshEntitlement() {
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
        ) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
            val active = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            purchases.forEach { acknowledgeIfNeeded(it) }
            scope.launch {
                if (active) prefs.setPlan(Plan.Pro) else if (!BuildConfig.DEBUG) prefs.setPlan(Plan.Free)
            }
        }
    }

    fun launch(activity: Activity, annual: Boolean) {
        val d = details[if (annual) ANNUAL_ID else MONTHLY_ID]
        val offer = d?.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (d == null || offer == null) {
            _events.tryEmit(Event.Failed("Play Store indisponível no momento."))
            return
        }
        val p = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).setOfferToken(offer).build()
        client.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(p)).build())
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val ok = purchases.orEmpty().filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                ok.forEach { acknowledgeIfNeeded(it) }
                if (ok.isNotEmpty()) {
                    scope.launch { prefs.setPlan(Plan.Pro) }
                    _events.tryEmit(Event.Purchased)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> _events.tryEmit(Event.Cancelled)
            else -> _events.tryEmit(Event.Failed(result.debugMessage.ifBlank { "Não foi possível concluir a assinatura." }))
        }
    }

    private fun acknowledgeIfNeeded(p: Purchase) {
        if (p.purchaseState != Purchase.PurchaseState.PURCHASED || p.isAcknowledged) return
        client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) { }
    }
}
