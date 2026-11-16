package com.getkova.app

import android.content.Context
import android.os.Build
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors

/**
 * Le journal de ce qui se passe dans l'app : écrans vus, étapes de l'onboarding,
 * séances, paywall. Il sert à trouver où les gens décrochent.
 *
 * Chaque événement est compté sur le téléphone. Il n'est envoyé à PostHog
 * (hébergé dans l'Union européenne) que si une clé de projet est configurée
 * ET que l'utilisateur a dit oui. Sans réponse, les événements attendent sur le
 * téléphone. Après un refus, rien n'est gardé pour l'envoi.
 *
 * Règles : aucun nom d'app verrouillée, aucun texte saisi, jamais rien de la caméra.
 * Seulement des noms d'événements, des nombres et des choix faits dans Kova.
 * L'identifiant est tiré au hasard : il ne vient ni du téléphone ni d'un compte.
 */
object Mesures {
    private const val REGLAGES = "mesures_reglages"
    private const val COMPTEURS = "mesures"
    private const val JOURNAL = "mesures.jsonl"
    private const val A_ENVOYER = "mesures_envoi.jsonl"
    private const val CLE_CONSENTEMENT = "consentement"
    private const val CLE_IDENTIFIANT = "identifiant"

    /** Adresse de l'envoi par lots de PostHog, région Union européenne. */
    private const val ADRESSE = "https://eu.i.posthog.com/batch/"

    /** Au-delà de cette taille, la moitié la plus ancienne d'un fichier est effacée. */
    private const val TAILLE_MAXIMUM = 256 * 1024L
    private const val LOT_MAXIMUM = 500
    private const val EVENEMENTS_AVANT_ENVOI = 20
    private const val DELAI_ENVOI_MS = 60_000L
    private const val DELAI_RESEAU_MS = 10_000

    private val json = Json
    private val file = Executors.newSingleThreadExecutor()
    private var enAttente = 0
    private var dernierEnvoi = 0L

    /** Vrai si l'app a une clé de projet : sans elle, rien ne peut sortir du téléphone. */
    val disponible: Boolean get() = BuildConfig.CLE_MESURES.isNotEmpty()

    private fun reglages(contexte: Context) =
        contexte.applicationContext.getSharedPreferences(REGLAGES, Context.MODE_PRIVATE)

    /** Null tant que la question n'a pas été posée. */
    fun consentement(contexte: Context): Boolean? {
        val reglages = reglages(contexte)
        return if (reglages.contains(CLE_CONSENTEMENT)) reglages.getBoolean(CLE_CONSENTEMENT, false) else null
    }

    /** Enregistre le choix. Un refus efface l'identifiant et tout ce qui attendait d'être envoyé. */
    @Synchronized
    fun definirConsentement(contexte: Context, accepte: Boolean) {
        val edition = reglages(contexte).edit().putBoolean(CLE_CONSENTEMENT, accepte)
        if (accepte) {
            if (!reglages(contexte).contains(CLE_IDENTIFIANT)) edition.putString(CLE_IDENTIFIANT, UUID.randomUUID().toString())
        } else {
            edition.remove(CLE_IDENTIFIANT)
            File(contexte.applicationContext.filesDir, A_ENVOYER).delete()
        }
        edition.apply()
        if (accepte) vider(contexte)
    }

    @Synchronized
    fun suivre(contexte: Context, nom: String, vararg proprietes: Pair<String, Any?>) {
        // Une mesure ratée ne doit jamais gêner l'app.
        runCatching {
            val application = contexte.applicationContext
            val date = System.currentTimeMillis()
            val compteurs = application.getSharedPreferences(COMPTEURS, Context.MODE_PRIVATE)
            compteurs.edit().putInt(nom, compteurs.getInt(nom, 0) + 1).apply()

            val ligne = evenement(nom, date, proprietes.toMap(), contexteDeLApp()).toString() + "\n"
            ajouter(File(application.filesDir, JOURNAL), ligne)
            if (disponible && consentement(application) != false) {
                ajouter(File(application.filesDir, A_ENVOYER), ligne)
                enAttente += 1
                if (enAttente >= EVENEMENTS_AVANT_ENVOI || date - dernierEnvoi > DELAI_ENVOI_MS) vider(application)
            }
        }
    }

    private fun ajouter(fichier: File, ligne: String) {
        if (fichier.length() > TAILLE_MAXIMUM) {
            val lignes = fichier.readLines()
            fichier.writeText(lignes.drop(lignes.size / 2).joinToString("\n", postfix = "\n"))
        }
        fichier.appendText(ligne)
    }

    /** Ce qui accompagne chaque événement : la version, la langue, la version d'Android. Rien de personnel. */
    private fun contexteDeLApp(): Map<String, Any?> = mapOf(
        "version" to BuildConfig.VERSION_NAME,
        "langue" to Locale.getDefault().language,
        "android" to Build.VERSION.SDK_INT,
        // Les essais faits avec la version de test se filtrent dans PostHog.
        "test" to BuildConfig.DEBUG,
    )

    /** Envoie en arrière-plan ce qui attend, si l'utilisateur a dit oui. À appeler quand l'app passe au second plan. */
    fun vider(contexte: Context) {
        val application = contexte.applicationContext
        if (!disponible || consentement(application) != true) return
        enAttente = 0
        dernierEnvoi = System.currentTimeMillis()
        file.execute { runCatching { envoyer(application) } }
    }

    private fun envoyer(application: Context) {
        val fichier = File(application.filesDir, A_ENVOYER)
        val identifiant = reglages(application).getString(CLE_IDENTIFIANT, null) ?: return
        val lignes = synchronized(this) { if (fichier.exists()) fichier.readLines().filter { it.isNotBlank() } else emptyList() }
        val lot = lignes.take(LOT_MAXIMUM)
        if (lot.isEmpty()) return
        val connexion = URL(ADRESSE).openConnection() as HttpURLConnection
        try {
            connexion.requestMethod = "POST"
            connexion.connectTimeout = DELAI_RESEAU_MS
            connexion.readTimeout = DELAI_RESEAU_MS
            connexion.doOutput = true
            connexion.setRequestProperty("Content-Type", "application/json")
            connexion.outputStream.use { it.write(corps(BuildConfig.CLE_MESURES, identifiant, lot).toByteArray()) }
            if (connexion.responseCode != HttpURLConnection.HTTP_OK) return
        } finally {
            connexion.disconnect()
        }
        // Envoi réussi : on retire du fichier ce qui est parti, pas ce qui est arrivé entre-temps.
        synchronized(this) {
            if (consentement(application) != true) return
            val restantes = fichier.readLines().filter { it.isNotBlank() }.drop(lot.size)
            if (restantes.isEmpty()) fichier.delete() else fichier.writeText(restantes.joinToString("\n", postfix = "\n"))
        }
    }

    // --- Mise en forme, sans Android : testée sur l'ordinateur ---

    private fun valeur(v: Any?): JsonElement = when (v) {
        null -> JsonNull
        is Boolean -> JsonPrimitive(v)
        is Number -> JsonPrimitive(v)
        else -> JsonPrimitive(v.toString())
    }

    /**
     * Un événement au format de PostHog. `$process_person_profile` à faux : pas de
     * fiche de personne. `$geoip_disable` : pas de localisation déduite de l'adresse IP.
     */
    fun evenement(nom: String, date: Long, proprietes: Map<String, Any?>, contexte: Map<String, Any?> = emptyMap()): JsonObject =
        buildJsonObject {
            put("event", nom)
            put("timestamp", Instant.ofEpochMilli(date).toString())
            put("properties", buildJsonObject {
                (contexte + proprietes).forEach { (cle, v) -> put(cle, valeur(v)) }
                put("\$process_person_profile", false)
                put("\$geoip_disable", true)
                put("\$lib", "kova-android")
            })
        }

    /** Le corps de la requête d'envoi : la clé du projet et le lot, chaque événement avec l'identifiant au hasard. */
    fun corps(cle: String, identifiant: String, lignes: List<String>): String = buildJsonObject {
        put("api_key", cle)
        put("batch", buildJsonArray {
            lignes.forEach { ligne ->
                val evenement = runCatching { json.parseToJsonElement(ligne).jsonObject }.getOrNull() ?: return@forEach
                add(JsonObject(evenement + ("distinct_id" to JsonPrimitive(identifiant))))
            }
        })
    }.toString()

    /** Nombre de fois où chaque événement s'est produit, par ordre alphabétique. */
    fun compteurs(contexte: Context): List<Pair<String, Int>> =
        contexte.applicationContext.getSharedPreferences(COMPTEURS, Context.MODE_PRIVATE).all
            .mapNotNull { (nom, valeur) -> (valeur as? Int)?.let { nom to it } }
            .sortedBy { it.first }
}
