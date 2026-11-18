package com.getkova.app

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

/**
 * Les abonnements Google Play (brief, section 4).
 *
 * À créer dans la Play Console (action humaine) :
 *   - un abonnement dont l'identifiant est [PRODUIT] ;
 *   - un forfait de base [FORFAIT_ANNUEL] (1 an), avec une offre d'essai gratuit de 3 jours ;
 *   - un forfait de base [FORFAIT_HEBDO] (1 semaine).
 * Les prix affichés viennent de Google Play. Tant qu'ils ne sont pas arrivés, le
 * paywall montre les prix écrits dans les textes de l'app.
 *
 * Tout passe par l'app Google Play du téléphone : Kova ne se connecte pas à
 * Internet et ne reçoit ni nom ni moyen de paiement.
 */
class Achats(
    contexte: Context,
    /** Appelé quand on sait si un abonnement est actif. */
    private val surEtat: (actif: Boolean) -> Unit,
    /** Appelé quand un achat vient d'aboutir. `essai` : il commence par l'essai gratuit. */
    private val surAchat: (essai: Boolean) -> Unit,
    /** Appelé quand un achat ou une restauration n'aboutit pas, avec le texte à montrer. */
    private val surProbleme: (message: Int) -> Unit,
) : PurchasesUpdatedListener {

    /** Prix lus dans Google Play. Nuls tant qu'ils ne sont pas arrivés. */
    var prixAnnuel by mutableStateOf<String?>(null)
        private set
    var prixHebdo by mutableStateOf<String?>(null)
        private set

    /** Vrai si l'essai gratuit de l'annuel est encore proposé à ce compte. */
    var essaiDisponible by mutableStateOf(true)
        private set

    private var details: ProductDetails? = null
    private var offreAnnuelle: ProductDetails.SubscriptionOfferDetails? = null
    private var offreHebdo: ProductDetails.SubscriptionOfferDetails? = null
    /** Vrai si l'achat en cours commence par l'essai gratuit. */
    private var achatAvecEssai = false

    private val client: BillingClient = BillingClient.newBuilder(contexte.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    /** Se connecte à Google Play, puis lit les offres et les abonnements en cours. */
    fun demarrer() {
        if (client.isReady) {
            rafraichir()
            return
        }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(resultat: BillingResult) {
                if (resultat.responseCode == BillingClient.BillingResponseCode.OK) {
                    chargerLesOffres()
                    rafraichir()
                }
            }

            override fun onBillingServiceDisconnected() {
                // La reconnexion est automatique : rien à faire ici.
            }
        })
    }

    private fun chargerLesOffres() {
        val produit = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUIT)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val demande = QueryProductDetailsParams.newBuilder().setProductList(listOf(produit)).build()
        client.queryProductDetailsAsync(demande) { resultat, reponse ->
            if (resultat.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            val trouve = reponse.productDetailsList.firstOrNull() ?: return@queryProductDetailsAsync
            val offres = trouve.subscriptionOfferDetails.orEmpty()
            val annuelles = offres.filter { it.basePlanId == FORFAIT_ANNUEL }
            // L'offre avec essai gratuit d'abord, si Google la propose encore à ce compte.
            val avecEssai = annuelles.firstOrNull { commenceParUnEssai(it) }
            details = trouve
            offreAnnuelle = avecEssai ?: annuelles.firstOrNull { it.offerId == null } ?: annuelles.firstOrNull()
            offreHebdo = offres.firstOrNull { it.basePlanId == FORFAIT_HEBDO && it.offerId == null }
                ?: offres.firstOrNull { it.basePlanId == FORFAIT_HEBDO }
            essaiDisponible = avecEssai != null
            prixAnnuel = offreAnnuelle?.let(::prixPlein)
            prixHebdo = offreHebdo?.let(::prixPlein)
        }
    }

    private fun commenceParUnEssai(offre: ProductDetails.SubscriptionOfferDetails): Boolean =
        offre.pricingPhases.pricingPhaseList.firstOrNull()?.priceAmountMicros == 0L

    /** Le prix payé à chaque période, une fois l'essai passé. */
    private fun prixPlein(offre: ProductDetails.SubscriptionOfferDetails): String? =
        offre.pricingPhases.pricingPhaseList.lastOrNull()?.formattedPrice

    /** Relit les abonnements en cours. À appeler à chaque retour dans l'app. */
    fun rafraichir(restauration: Boolean = false) {
        if (!client.isReady) {
            if (restauration) surProbleme(R.string.achat_indisponible)
            demarrer()
            return
        }
        val demande = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        client.queryPurchasesAsync(demande) { resultat, achats ->
            if (resultat.responseCode != BillingClient.BillingResponseCode.OK) {
                // Google Play ne répond pas : on garde le dernier état connu.
                if (restauration) surProbleme(R.string.achat_indisponible)
                return@queryPurchasesAsync
            }
            val actif = traiter(achats)
            if (restauration && !actif) surProbleme(R.string.achat_rien_a_restaurer)
        }
    }

    /** Ouvre la fenêtre d'achat de Google Play. Si les offres manquent, le dit. */
    fun acheter(activite: Activity, annuel: Boolean) {
        val produit = details
        val offre = if (annuel) offreAnnuelle else offreHebdo
        if (!client.isReady || produit == null || offre == null) {
            surProbleme(R.string.achat_indisponible)
            demarrer()
            return
        }
        achatAvecEssai = annuel && commenceParUnEssai(offre)
        val choix = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(produit)
            .setOfferToken(offre.offerToken)
            .build()
        val demande = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(choix)).build()
        val resultat = client.launchBillingFlow(activite, demande)
        if (resultat.responseCode != BillingClient.BillingResponseCode.OK) {
            surProbleme(R.string.achat_indisponible)
        }
    }

    override fun onPurchasesUpdated(resultat: BillingResult, achats: MutableList<Purchase>?) {
        when (resultat.responseCode) {
            BillingClient.BillingResponseCode.OK -> if (traiter(achats.orEmpty())) surAchat(achatAvecEssai)
            // L'utilisateur a fermé la fenêtre : rien à dire.
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            // Déjà abonné sur ce compte : on relit, l'accès revient.
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> rafraichir()
            else -> surProbleme(R.string.achat_echec)
        }
    }

    /** Confirme les achats payés auprès de Google et dit si un abonnement est actif. */
    private fun traiter(achats: List<Purchase>): Boolean {
        val payes = achats.filter {
            PRODUIT in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        for (achat in payes) {
            // Un achat non confirmé sous trois jours est remboursé par Google.
            if (!achat.isAcknowledged) {
                val confirmation = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(achat.purchaseToken).build()
                client.acknowledgePurchase(confirmation) { }
            }
        }
        val actif = payes.isNotEmpty()
        surEtat(actif)
        return actif
    }

    companion object {
        const val PRODUIT = "kova_premium"
        const val FORFAIT_ANNUEL = "annuel"
        const val FORFAIT_HEBDO = "hebdo"

        /**
         * Faux pour la première version, comme sur iPhone : la remise montrée à la
         * fermeture du paywall demande une offre de plus dans la Play Console.
         * Écart voulu avec le brief (section 4).
         */
        const val OFFRE_DE_RATTRAPAGE_ACTIVE = false
    }
}
