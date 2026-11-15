package com.getkova.app.comptage

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.getkova.app.jeu.Exercice
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.Executors

/**
 * La caméra d'une séance : elle filme, détecte la posture sur le téléphone
 * (ML Kit, modèle embarqué) et compte les répétitions.
 *
 * Confidentialité (brief, règle 10) : chaque image est analysée en mémoire puis
 * oubliée. Cette classe n'a aucune sortie photo ou vidéo, n'écrit rien sur le
 * disque et n'envoie rien sur le réseau. Seuls un nombre de répétitions et un
 * "corps visible ou non" en sortent.
 */
class CameraSeance(contexte: Context) {
    enum class Etat { ARRETEE, ACTIVE, REFUSEE, INDISPONIBLE }

    private val contexte = contexte.applicationContext
    private val principal = Handler(Looper.getMainLooper())
    private val executeur = Executors.newSingleThreadExecutor()
    private val detecteur = PoseDetection.getClient(
        PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.STREAM_MODE).build(),
    )

    var etat by mutableStateOf(Etat.ARRETEE)
        private set
    var repetitions by mutableIntStateOf(0)
        private set
    var corpsVisible by mutableStateOf(false)
        private set

    /** Le squelette détecté sur la dernière image, pour le dessiner par dessus l'aperçu. */
    var squelette by mutableStateOf<Pose>(emptyMap())
        private set

    /** Largeur de l'image divisée par sa hauteur, une fois redressée. */
    var rapportImage by mutableStateOf(0.75f)
        private set

    /** Dernier angle mesuré (coude ou genou). Affiché dans la version de test. */
    var angle by mutableStateOf<Double?>(null)
        private set

    /** Ce qui est arrivé à la dernière répétition. Affiché dans la version de test. */
    var diagnostic by mutableStateOf<Diagnostic?>(null)
        private set

    /** Appelé à chaque répétition comptée, avec le total. */
    var surRepetition: ((Int) -> Unit)? = null

    private var compteur = CompteurRepetitions(Exercice.POMPES)
    private var objectif = 0

    /** Numéro du comptage en cours, pour écarter les images d'un comptage précédent. */
    private var serie = 0
    private var fournisseur: ProcessCameraProvider? = null
    private var proprietaire: LifecycleOwner? = null
    private var vueApercu: PreviewView? = null
    private val simulation = object : Runnable {
        override fun run() {
            publier(repetitions + 1, visible = true, comptee = true, numero = serie)
            if (etat == Etat.ACTIVE) principal.postDelayed(this, 1000)
        }
    }

    val autorisee: Boolean
        get() = ContextCompat.checkSelfPermission(contexte, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    /**
     * Allume la caméra et remet le compteur à zéro. La caméra s'éteint seule dès
     * que l'objectif est atteint (objectif à zéro : elle observe sans s'arrêter).
     * `simule` compte une répétition par seconde sans caméra (réglage de chantier).
     */
    fun demarrer(proprietaire: LifecycleOwner, exercice: Exercice, objectif: Int, simule: Boolean = false) {
        serie += 1
        principal.removeCallbacks(simulation)
        this.objectif = objectif
        this.proprietaire = proprietaire
        compteur = CompteurRepetitions(exercice)
        repetitions = 0
        corpsVisible = false
        squelette = emptyMap()
        angle = null
        diagnostic = null
        if (simule) {
            etat = Etat.ACTIVE
            corpsVisible = true
            if (objectif > 0) principal.postDelayed(simulation, 1000)
            return
        }
        if (!autorisee) {
            etat = Etat.REFUSEE
            return
        }
        etat = Etat.ACTIVE
        val futur = ProcessCameraProvider.getInstance(contexte)
        futur.addListener({
            fournisseur = runCatching { futur.get() }.getOrNull()
            lier()
        }, ContextCompat.getMainExecutor(contexte))
    }

    /** Éteint la caméra. */
    fun arreter() {
        serie += 1
        principal.removeCallbacks(simulation)
        etat = Etat.ARRETEE
        corpsVisible = false
        squelette = emptyMap()
        fournisseur?.unbindAll()
    }

    /** Branche ou débranche l'aperçu à l'écran. L'analyse tourne même sans aperçu. */
    fun attacherApercu(vue: PreviewView?) {
        if (vueApercu === vue) return
        vueApercu = vue
        if (etat == Etat.ACTIVE) lier()
    }

    /** Caméra avant, petite définition (suffisante pour la posture). Aucune sortie d'enregistrement. */
    private fun lier() {
        val fournisseur = fournisseur ?: return
        val proprietaire = proprietaire ?: return
        if (etat != Etat.ACTIVE) return
        val numero = serie
        val analyse = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        analyse.setAnalyzer(executeur) { image -> analyser(image, numero) }
        try {
            fournisseur.unbindAll()
            val apercu = vueApercu?.let { vue -> Preview.Builder().build().also { it.surfaceProvider = vue.surfaceProvider } }
            if (apercu != null) {
                fournisseur.bindToLifecycle(proprietaire, CameraSelector.DEFAULT_FRONT_CAMERA, apercu, analyse)
            } else {
                fournisseur.bindToLifecycle(proprietaire, CameraSelector.DEFAULT_FRONT_CAMERA, analyse)
            }
        } catch (erreur: Exception) {
            etat = Etat.INDISPONIBLE
        }
    }

    /** Détecte la posture sur une image. L'image n'est ni copiée ni gardée. */
    @OptIn(ExperimentalGetImage::class)
    private fun analyser(image: ImageProxy, numero: Int) {
        val media = image.image
        if (media == null) {
            image.close()
            return
        }
        val rotation = image.imageInfo.rotationDegrees
        // Hauteur de l'image une fois redressée : elle sert d'unité pour les positions.
        val hauteur = (if (rotation % 180 == 0) image.height else image.width).coerceAtLeast(1).toDouble()
        val largeur = (if (rotation % 180 == 0) image.width else image.height).toDouble()
        detecteur.process(InputImage.fromMediaImage(media, rotation))
            .addOnSuccessListener { resultat ->
                val points = mutableMapOf<Articulation, Point>()
                for ((repere, articulation) in correspondance) {
                    val trouve = resultat.getPoseLandmark(repere) ?: continue
                    if (trouve.inFrameLikelihood >= ReglagesComptage.CONFIANCE_MINIMUM) {
                        points[articulation] = Point(trouve.position.x / hauteur, trouve.position.y / hauteur)
                    }
                }
                if (numero == serie && etat == Etat.ACTIVE) {
                    squelette = points
                    rapportImage = (largeur / hauteur).toFloat()
                    val comptee = compteur.traiter(points.ifEmpty { null })
                    angle = compteur.angle
                    diagnostic = compteur.diagnostic
                    publier(compteur.compte, compteur.corpsVisible, comptee, numero)
                }
            }
            .addOnCompleteListener { image.close() }
    }

    /** Met à jour ce que l'écran affiche. Toujours sur le fil principal. */
    private fun publier(compte: Int, visible: Boolean, comptee: Boolean, numero: Int) {
        if (etat != Etat.ACTIVE || numero != serie) return
        corpsVisible = visible
        repetitions = compte
        if (comptee) surRepetition?.invoke(compte)
        if (objectif > 0 && compte >= objectif) arreter()
    }

    private companion object {
        val correspondance = listOf(
            PoseLandmark.LEFT_SHOULDER to Articulation.EPAULE_GAUCHE,
            PoseLandmark.LEFT_ELBOW to Articulation.COUDE_GAUCHE,
            PoseLandmark.LEFT_WRIST to Articulation.POIGNET_GAUCHE,
            PoseLandmark.LEFT_HIP to Articulation.HANCHE_GAUCHE,
            PoseLandmark.LEFT_KNEE to Articulation.GENOU_GAUCHE,
            PoseLandmark.LEFT_ANKLE to Articulation.CHEVILLE_GAUCHE,
            PoseLandmark.RIGHT_SHOULDER to Articulation.EPAULE_DROITE,
            PoseLandmark.RIGHT_ELBOW to Articulation.COUDE_DROIT,
            PoseLandmark.RIGHT_WRIST to Articulation.POIGNET_DROIT,
            PoseLandmark.RIGHT_HIP to Articulation.HANCHE_DROITE,
            PoseLandmark.RIGHT_KNEE to Articulation.GENOU_DROIT,
            PoseLandmark.RIGHT_ANKLE to Articulation.CHEVILLE_DROITE,
        )
    }
}
