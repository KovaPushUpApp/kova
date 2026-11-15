package com.getkova.app.ecrans

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.ImageBitmap
import com.getkova.app.jeu.plageEnCours
import java.time.ZoneId
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import com.getkova.app.BuildConfig
import com.getkova.app.comptage.Articulation
import com.getkova.app.comptage.Diagnostic
import com.getkova.app.comptage.Point
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.getkova.app.EtatApp
import com.getkova.app.R
import com.getkova.app.comptage.CameraSeance
import com.getkova.app.design.Anneau
import com.getkova.app.design.BarreSegments
import com.getkova.app.design.BoutonFermer
import com.getkova.app.design.BoutonPrincipal
import com.getkova.app.design.BoutonRetour
import com.getkova.app.design.BoutonSecondaire
import com.getkova.app.design.CarteOption
import com.getkova.app.design.Centre
import com.getkova.app.design.Chiffre
import com.getkova.app.design.Corps
import com.getkova.app.design.EcranKova
import com.getkova.app.design.FigurePompe
import com.getkova.app.design.IllustrationPlacement
import com.getkova.app.design.PLACEMENT_PART_DISTANCE
import com.getkova.app.design.Graisse
import com.getkova.app.design.Icone
import com.getkova.app.design.IconeAnneau
import com.getkova.app.design.Legende
import com.getkova.app.design.LigneCoche
import com.getkova.app.design.Paragraphe
import com.getkova.app.design.Pictogramme
import com.getkova.app.design.StyleBouton
import com.getkova.app.design.Titre
import com.getkova.app.design.Tokens
import com.getkova.app.design.carte
import com.getkova.app.design.entree
import com.getkova.app.design.f
import com.getkova.app.design.n
import com.getkova.app.design.pointilles
import com.getkova.app.design.pop
import com.getkova.app.design.positionDuScan
import com.getkova.app.design.reduireLesAnimations
import com.getkova.app.design.t
import com.getkova.app.jeu.EtatJeu
import com.getkova.app.jeu.Exercice
import com.getkova.app.jeu.finDOuverture
import com.getkova.app.jeu.ouvrirFenetre
import com.getkova.app.jeu.plageActive
import com.getkova.app.jeu.repetitionsDuJour
import com.getkova.app.jeu.serie
import com.getkova.app.jeu.urgencesRestantes
import com.getkova.app.nom
import kotlinx.coroutines.delay

/** Les segments du squelette : bras, tronc et jambes. */
private val segments = listOf(
    Articulation.EPAULE_GAUCHE to Articulation.EPAULE_DROITE,
    Articulation.HANCHE_GAUCHE to Articulation.HANCHE_DROITE,
    Articulation.EPAULE_GAUCHE to Articulation.COUDE_GAUCHE,
    Articulation.COUDE_GAUCHE to Articulation.POIGNET_GAUCHE,
    Articulation.EPAULE_DROITE to Articulation.COUDE_DROIT,
    Articulation.COUDE_DROIT to Articulation.POIGNET_DROIT,
    Articulation.EPAULE_GAUCHE to Articulation.HANCHE_GAUCHE,
    Articulation.EPAULE_DROITE to Articulation.HANCHE_DROITE,
    Articulation.HANCHE_GAUCHE to Articulation.GENOU_GAUCHE,
    Articulation.GENOU_GAUCHE to Articulation.CHEVILLE_GAUCHE,
    Articulation.HANCHE_DROITE to Articulation.GENOU_DROIT,
    Articulation.GENOU_DROIT to Articulation.CHEVILLE_DROITE,
)

/**
 * Le squelette détecté, dessiné par dessus l'aperçu de la caméra. Il montre ce
 * que Kova voit : si le squelette te suit, le comptage peut marcher.
 * L'aperçu remplit le cadre en rognant l'image et la caméra avant est en miroir :
 * les positions sont converties de la même façon.
 */
@Composable
private fun Squelette(camera: CameraSeance, modifier: Modifier = Modifier) {
    val pose = camera.squelette
    val rapport = camera.rapportImage
    Canvas(modifier.clearAndSetSemantics { }) {
        if (pose.isEmpty() || rapport <= 0f) return@Canvas
        // Une unité de la pose vaut la hauteur de l'image. L'image remplit le cadre.
        val echelle = maxOf(size.width / rapport, size.height)
        val decalageX = (size.width - rapport * echelle) / 2
        val decalageY = (size.height - echelle) / 2
        fun place(point: Point) = Offset(
            decalageX + (rapport - point.x.toFloat()) * echelle,
            decalageY + point.y.toFloat() * echelle,
        )
        val trait = Tokens.Traits.illustration.dp.toPx()
        for ((debut, fin) in segments) {
            val a = pose[debut] ?: continue
            val b = pose[fin] ?: continue
            drawLine(Tokens.Couleurs.accent, place(a), place(b), strokeWidth = trait, cap = StrokeCap.Round)
        }
        for (point in pose.values) {
            drawCircle(Tokens.Couleurs.texte, trait, place(point))
        }
    }
}

/**
 * Le retour caméra : aperçu en direct, squelette par dessus et ligne de scan.
 * Sans caméra (refus, comptage simulé), il montre la silhouette de la maquette.
 */
@Composable
fun VueCamera(camera: CameraSeance, simule: Boolean, modifier: Modifier = Modifier) {
    val enDirect = camera.etat == CameraSeance.Etat.ACTIVE && !simule
    val scan = positionDuScan()
    Box(modifier.background(Tokens.Couleurs.carte)) {
        if (enDirect) {
            AndroidView(
                factory = { contexte ->
                    PreviewView(contexte).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                },
                modifier = Modifier.fillMaxSize().clearAndSetSemantics { },
                update = { vue -> camera.attacherApercu(vue) },
            )
            DisposableEffect(Unit) { onDispose { camera.attacherApercu(null) } }
            Squelette(camera, Modifier.fillMaxSize())
        } else {
            val message = when (camera.etat) {
                CameraSeance.Etat.REFUSEE -> t(R.string.camera_refusee)
                CameraSeance.Etat.INDISPONIBLE -> t(R.string.camera_indisponible)
                else -> if (simule) t(R.string.camera_fictive) else ""
            }
            Column(
                Modifier.fillMaxSize().padding(horizontal = Tokens.Espacements.e21.dp),
                verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e13.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FigurePompe()
                Legende(message, centre = true)
            }
        }
        if (scan != null) {
            // La ligne de scan suit le bas d'une boîte dont la hauteur varie.
            Box(Modifier.fillMaxWidth().fillMaxHeight(scan), contentAlignment = Alignment.BottomCenter) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(Tokens.Traits.normal.dp)
                        .alpha(Tokens.Opacites.scan)
                        .background(Tokens.Couleurs.accent),
                )
            }
        }
    }
}

/**
 * Gabarit des écrans de comptage : le retour caméra remplit tout l'écran, pour
 * bien se voir. L'en-tête est en haut, le compteur et le bouton en bas, sur un
 * voile sombre qui les garde lisibles. Écart voulu avec la maquette, où la
 * caméra n'occupe que 38 % de la hauteur.
 */
@Composable
private fun EcranCamera(
    camera: CameraSeance,
    simule: Boolean,
    haut: @Composable RowScope.() -> Unit,
    bas: @Composable ColumnScope.() -> Unit,
) {
    val enDirect = camera.etat == CameraSeance.Etat.ACTIVE && !simule
    val cotes = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
    Box(Modifier.fillMaxSize().background(Tokens.Couleurs.fond)) {
        VueCamera(camera, simule, Modifier.fillMaxSize())
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(horizontal = Tokens.Ecran.margeCotes.dp, vertical = Tokens.Espacements.e8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Espacements.e13.dp),
            content = haut,
        )
        if (enDirect && !camera.corpsVisible) {
            Etiquette(t(R.string.camera_hors_cadre), Modifier.align(Alignment.Center), accent = true)
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.3f to Tokens.Couleurs.fond.copy(alpha = Tokens.Opacites.voileCamera),
                        1f to Tokens.Couleurs.fond,
                    ),
                )
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).union(cotes))
                .padding(horizontal = Tokens.Ecran.margeCotes.dp)
                .padding(top = Tokens.Espacements.e55.dp, bottom = Tokens.Espacements.e8.dp),
            verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e13.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = bas,
        )
    }
}

/** Petite étiquette lisible par dessus l'image de la caméra. */
@Composable
private fun Etiquette(texte: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    Corps(
        texte,
        modifier
            .clip(RoundedCornerShape(Tokens.Arrondis.moyen.dp))
            .background(if (accent) Tokens.Couleurs.accent else Tokens.Couleurs.fond.copy(alpha = Tokens.Opacites.voileCamera))
            .padding(horizontal = Tokens.Espacements.e13.dp, vertical = Tokens.Espacements.e8.dp),
        couleur = if (accent) Tokens.Couleurs.texteSurAccent else Tokens.Couleurs.texte,
        graisse = Graisse.DEMI_GRAS,
    )
}

/**
 * Version de test : l'angle mesuré en direct et le sort de la dernière pompe,
 * pour régler les seuils du comptage. Absent de la version publiée.
 */
@Composable
private fun AngleMesure(camera: CameraSeance) {
    if (!BuildConfig.DEBUG) return
    val angle = camera.angle
    val dernier = camera.diagnostic
    val texte = buildString {
        append(if (angle != null) angle.toInt().toString() + "°" else "–")
        if (dernier != null) {
            append("\n")
            append(
                when (dernier.raison) {
                    Diagnostic.Raison.COMPTEE -> "OK"
                    Diagnostic.Raison.PAS_ASSEZ_BAS -> "pas assez bas"
                    Diagnostic.Raison.CORPS_IMMOBILE -> "corps immobile"
                    Diagnostic.Raison.APPUI_DEPLACE -> "mains bougent"
                },
            )
            append("\nbas " + dernier.angleBas.toInt() + "°")
            append("\ncorps " + (dernier.deplacement * 100).toInt())
            append("\nmains " + (dernier.glissement * 100).toInt())
        }
    }
    Legende(
        texte,
        Modifier
            .clip(RoundedCornerShape(Tokens.Arrondis.petit.dp))
            .background(Tokens.Couleurs.fond.copy(alpha = Tokens.Opacites.voileCamera))
            .padding(horizontal = Tokens.Espacements.e8.dp, vertical = Tokens.Espacements.e5.dp),
        couleur = Tokens.Couleurs.accent,
        graisse = Graisse.DEMI_GRAS,
    )
}

/** Compteur géant "3/10" de l'essai et de la séance, avec le message d'encouragement. */
@Composable
fun CompteurGeant(faites: Int, total: Int, tailleTotal: Float, corpsVisible: Boolean) {
    val description = f(R.string.a11y_progression_valeur, faites, total)
    val message = when {
        faites >= total -> t(R.string.seance_validee)
        !corpsVisible || faites == 0 -> t(R.string.seance_en_position)
        else -> f(R.string.seance_encore, total - faites)
    }
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e3.dp),
    ) {
        Row(Modifier.clearAndSetSemantics { contentDescription = description }) {
            Chiffre(faites.toString(), Tokens.TaillesTexte.chiffreGeant, Modifier.alignByBaseline())
            Chiffre("/$total", tailleTotal, Modifier.alignByBaseline(), couleur = Tokens.Couleurs.texteSecondaire)
        }
        Corps(message, couleur = Tokens.Couleurs.texteSecondaire)
    }
}

/** Nombre de pompes de l'essai de l'onboarding. */
const val REPETITIONS_ESSAI = 3

/**
 * Écran 9 : le moment de preuve. Trois vraies pompes comptées en direct, puis le
 * bouton apparaît. L'essai est facultatif : on peut le passer à tout moment.
 */
@Composable
fun EcranEssai(etat: EtatApp, camera: CameraSeance, retour: () -> Unit, suivant: () -> Unit) {
    val total = REPETITIONS_ESSAI
    val proprietaire = LocalLifecycleOwner.current
    val haptique = rememberHaptique()
    val contexte = LocalView.current.context
    DisposableEffect(Unit) {
        camera.surRepetition = { compte -> if (compte >= total) haptique.serieValidee() else haptique.repetition() }
        camera.demarrer(proprietaire, Exercice.POMPES, total, etat.comptageSimule)
        onDispose { camera.arreter() }
    }
    val refusee = camera.etat == CameraSeance.Etat.REFUSEE || camera.etat == CameraSeance.Etat.INDISPONIBLE
    EcranCamera(
        camera, etat.comptageSimule,
        haut = {
            Box(Modifier.weight(1f)) { com.getkova.app.design.EnTeteEtapes(8, NOMBRE_ETAPES, retour) }
            AngleMesure(camera)
        },
    ) {
        Titre(t(R.string.essai_titre), taille = Tokens.TaillesTexte.titreCarte, centre = true)
        if (camera.repetitions == 0) {
            Legende(t(R.string.essai_conseil), couleur = Tokens.Couleurs.texte, centre = true)
        }
        CompteurGeant(camera.repetitions, total, Tokens.TaillesTexte.titreEcran, camera.corpsVisible)
        when {
            camera.repetitions >= total ->
                BoutonPrincipal(t(R.string.essai_bouton), Modifier.entree(), action = suivant)
            refusee -> {
                BoutonPrincipal(t(R.string.camera_ouvrir_reglages)) { ouvrirFicheDeLApp(contexte) }
                BoutonSecondaire(t(R.string.essai_passer), couleur = Tokens.Couleurs.texte, action = suivant)
            }
            else -> BoutonSecondaire(t(R.string.essai_passer), couleur = Tokens.Couleurs.texte, action = suivant)
        }
    }
}

/**
 * Écran 13 : l'app verrouillée. Sur Android, Kova affiche lui-même cet écran par
 * dessus l'app bloquée. Il part de la maquette et l'enrichit : icône de l'app,
 * temps en stock utilisable tout de suite, série, urgences restantes, et la
 * plage stricte en cours quand il y en a une.
 */
@Composable
fun EcranVerrou(
    nomDeLApp: String?,
    icone: ImageBitmap?,
    jeu: EtatJeu,
    maintenant: Long,
    zone: ZoneId,
    commencer: () -> Unit,
    utiliserLeStock: () -> Unit,
    fermer: () -> Unit,
    urgence: () -> Unit,
) {
    val plage = jeu.plageEnCours(maintenant, zone)
    val stock = jeu.soldeMinutes
    val peutOuvrir = stock > 0 && plage == null
    val urgences = jeu.urgencesRestantes(maintenant, zone)
    val urgencePossible = jeu.urgenceActivee && urgences > 0
    val serie = jeu.serie(maintenant, zone)
    val texte = when {
        plage != null -> f(R.string.verrou_app_plage, plage.nom, heureDePlage(plage.fin))
        peutOuvrir -> f(R.string.verrou_app_stock, stock)
        else -> t(R.string.verrou_app_texte)
    }
    EcranKova(
        bas = {
            if (peutOuvrir) {
                BoutonPrincipal(f(R.string.verrou_app_utiliser, stock), Modifier.entree(6), pulse = true, action = utiliserLeStock)
                BoutonSecondaire(t(R.string.verrou_app_gagner_plus), couleur = Tokens.Couleurs.texte, action = commencer)
            } else {
                BoutonPrincipal(t(R.string.verrou_app_commencer), Modifier.entree(6), pulse = true, action = commencer)
            }
            BoutonSecondaire(
                if (nomDeLApp != null) f(R.string.verrou_app_fermer_nom, nomDeLApp) else t(R.string.verrou_app_plus_tard),
                action = fermer,
            )
            if (urgencePossible) {
                BoutonSecondaire(t(R.string.ecran_urgence), action = urgence)
            }
        },
    ) {
        if (icone != null) IconeDeLAppBloquee(icone) else IconeAnneau(Icone.CADENAS)
        Centre(Modifier.padding(top = Tokens.Espacements.e13.dp).entree(2)) {
            Titre(
                if (nomDeLApp != null) f(R.string.verrou_app_titre_nom, nomDeLApp) else t(R.string.verrou_app_titre),
                centre = true,
            )
        }
        Centre(Modifier.entree(3)) {
            Paragraphe(texte, couleur = if (plage != null) Tokens.Couleurs.accent else Tokens.Couleurs.texteSecondaire, centre = true)
        }
        Row(
            Modifier.padding(top = Tokens.Espacements.e13.dp).entree(4).fillMaxWidth().carte(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e3.dp)) {
                Titre(f(R.string.verrou_app_pompes, jeu.regle.repetitions), taille = Tokens.TaillesTexte.titreCarte, lignesMax = 1)
                Legende(t(R.string.verrou_app_a_faire))
            }
            Pictogramme(Icone.FLECHE, Modifier.padding(horizontal = Tokens.Espacements.e8.dp), couleur = Tokens.Couleurs.texteSecondaire)
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e3.dp),
            ) {
                Titre(
                    f(R.string.commun_minutes_court, jeu.regle.minutes),
                    taille = Tokens.TaillesTexte.titreCarte, couleur = Tokens.Couleurs.accent, lignesMax = 1,
                )
                Legende(t(R.string.verrou_app_debloquees))
            }
        }
        // Trois repères d'un coup d'œil : le temps en stock, la série, les urgences.
        Row(Modifier.entree(5).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Tokens.Espacements.e8.dp)) {
            val marge = Tokens.Espacements.e13
            RepereDeVerrou(f(R.string.commun_minutes_court, stock), t(R.string.verrou_app_stock_legende), Modifier.weight(1f).fillMaxHeight(), marge)
            RepereDeVerrou(
                n(R.string.commun_jours_un, R.string.commun_jours_autre, serie), t(R.string.verrou_app_serie),
                Modifier.weight(1f).fillMaxHeight(), marge,
            )
            RepereDeVerrou(
                f(R.string.urgence_compte, urgences, EtatJeu.URGENCES_PAR_SEMAINE), t(R.string.verrou_app_urgences),
                Modifier.weight(1f).fillMaxHeight(), marge,
            )
        }
    }
}

/** Petit repère de l'écran de blocage : une valeur et sa légende. */
@Composable
private fun RepereDeVerrou(valeur: String, libelle: String, modifier: Modifier, marge: Float) {
    Column(modifier.carte(marge = marge), verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e3.dp)) {
        Titre(valeur, taille = Tokens.TaillesTexte.corpsFort, lignesMax = 1)
        Legende(libelle)
    }
}

/** L'icône de l'app bloquée, avec l'anneau qui s'élargit et un cadenas en pastille. */
@Composable
private fun IconeDeLAppBloquee(icone: ImageBitmap) {
    val taille = Tokens.Composants.Logo.taille
    Box(Modifier.fillMaxWidth().padding(top = Tokens.Espacements.e55.dp), contentAlignment = Alignment.Center) {
        Anneau(Modifier.size(taille.dp))
        Box(Modifier.size(taille.dp)) {
            Image(
                icone,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(Tokens.Arrondis.grand.dp)),
            )
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = Tokens.Espacements.e8.dp, y = Tokens.Espacements.e8.dp)
                    .size(Tokens.Espacements.e34.dp)
                    .clip(CircleShape)
                    .background(Tokens.Couleurs.fond)
                    .padding(Tokens.Espacements.e3.dp)
                    .clip(CircleShape)
                    .background(Tokens.Couleurs.accent),
                contentAlignment = Alignment.Center,
            ) {
                Pictogramme(Icone.CADENAS, taille = Tokens.Espacements.e13 + Tokens.Espacements.e3, couleur = Tokens.Couleurs.texteSurAccent)
            }
        }
    }
}

/**
 * Écran 14 : choix de l'exercice. Seuls les exercices dont le comptage est livré
 * sont proposés. Sans abonnement, seules les pompes sont utilisables.
 * Sert aussi au réglage "Exercice par défaut".
 */
@Composable
fun EcranExercice(etat: EtatApp, parDefaut: Boolean, retour: () -> Unit, valider: () -> Unit, ouvrirPaywall: () -> Unit) {
    fun reserve(exercice: Exercice) = !etat.abonne && exercice !in Exercice.gratuits
    val choisi = if (parDefaut) etat.jeu.exerciceParDefaut else etat.exercice
    LaunchedEffect(Unit) {
        // Un exercice réservé à l'abonnement ne peut pas rester choisi.
        if (!parDefaut && reserve(etat.exercice)) etat.exercice = Exercice.POMPES
    }
    EcranKova(
        espacement = Tokens.Espacements.e13,
        bas = {
            BoutonPrincipal(
                t(if (parDefaut) R.string.commun_enregistrer else R.string.exercice_bouton),
                Modifier.entree(4), action = valider,
            )
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Espacements.e13.dp)) {
            BoutonRetour(retour)
            Legende(f(R.string.seance_pour_minutes, etat.regle.minutes))
        }
        Titre(
            t(if (parDefaut) R.string.reglages_exercice else R.string.exercice_titre),
            Modifier.padding(top = Tokens.Espacements.e13.dp).entree(1),
        )
        Column(
            Modifier.padding(top = Tokens.Espacements.e8.dp).entree(2),
            verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e8.dp),
        ) {
            Exercice.disponibles.forEach { exercice ->
                CarteOption(choisi == exercice, {
                    when {
                        reserve(exercice) -> ouvrirPaywall()
                        parDefaut -> etat.definirExerciceParDefaut(exercice)
                        else -> etat.exercice = exercice
                    }
                }) {
                    Corps(t(exercice.nom), graisse = Graisse.DEMI_GRAS)
                    if (reserve(exercice)) {
                        Pictogramme(
                            Icone.CADENAS, Modifier.padding(start = Tokens.Espacements.e8.dp),
                            taille = Tokens.Espacements.e13, couleur = Tokens.Couleurs.texteSecondaire,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Titre(
                        etat.regle.repetitionsDemandees(exercice).toString(),
                        taille = Tokens.TaillesTexte.corpsFort,
                        couleur = if (choisi == exercice) Tokens.Couleurs.accent else Tokens.Couleurs.texteSecondaire,
                    )
                }
            }
        }
    }
}

/** Écran 15 : mise en place du téléphone. Le bouton apparaît quand le corps est détecté. */
@Composable
fun EcranPlacement(etat: EtatApp, camera: CameraSeance, retour: () -> Unit, suivant: () -> Unit) {
    val proprietaire = LocalLifecycleOwner.current
    val contexte = LocalView.current.context
    LaunchedEffect(Unit) {
        // Objectif à zéro : la caméra observe sans s'arrêter seule.
        camera.surRepetition = null
        camera.demarrer(proprietaire, etat.exercice, 0, etat.comptageSimule)
    }
    val refusee = camera.etat == CameraSeance.Etat.REFUSEE || camera.etat == CameraSeance.Etat.INDISPONIBLE
    EcranKova(
        bas = {
            when {
                camera.corpsVisible -> BoutonPrincipal(t(R.string.placement_bouton), Modifier.entree(), action = suivant)
                refusee -> BoutonPrincipal(t(R.string.camera_ouvrir_reglages)) { ouvrirFicheDeLApp(contexte) }
                else -> Spacer(Modifier.height(Tokens.Hauteurs.boutonPrincipal.dp))
            }
        },
    ) {
        BoutonRetour(retour)
        Titre(t(R.string.placement_titre), Modifier.padding(top = Tokens.Espacements.e13.dp).entree(1))
        Paragraphe(t(R.string.placement_texte), Modifier.entree(2))
        val description = t(R.string.a11y_placement)
        Column(
            Modifier
                .padding(top = Tokens.Espacements.e8.dp)
                .entree(3)
                .fillMaxWidth()
                .height(Tokens.Composants.VueCamera.hauteurEssai.dp)
                .clip(RoundedCornerShape(Tokens.Arrondis.grand.dp))
                .background(Tokens.Couleurs.carte)
                .padding(horizontal = Tokens.Espacements.e13.dp)
                .clearAndSetSemantics { contentDescription = description },
            verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e5.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IllustrationPlacement(Modifier.fillMaxWidth())
            // La légende se place sous la flèche de distance du schéma.
            Box(Modifier.fillMaxWidth()) {
                Legende(t(R.string.placement_distance), Modifier.fillMaxWidth(PLACEMENT_PART_DISTANCE), centre = true)
            }
        }
        when {
            camera.corpsVisible -> Box(Modifier.entree()) { LigneCoche(t(R.string.placement_detecte)) }
            refusee -> Paragraphe(t(R.string.camera_refusee))
        }
    }
}

/** Écran 16 : la séance. Le bouton de validation n'apparaît qu'à la fin. Abandonner ne donne rien. */
@Composable
fun EcranCompteur(etat: EtatApp, camera: CameraSeance, abandonner: () -> Unit, valider: () -> Unit) {
    val total = etat.repetitionsDeLaSeance
    val faites = camera.repetitions.coerceAtMost(total)
    val proprietaire = LocalLifecycleOwner.current
    val haptique = rememberHaptique()
    val voix = rememberVoix()
    val vue = LocalView.current
    DisposableEffect(Unit) {
        // L'écran reste allumé pendant la séance.
        vue.keepScreenOn = true
        val aVoixHaute = etat.compterAVoixHaute
        camera.surRepetition = { compte ->
            if (compte >= total) haptique.serieValidee() else haptique.repetition()
            if (aVoixHaute) voix.dire(compte)
        }
        camera.demarrer(proprietaire, etat.exercice, total, etat.comptageSimule)
        onDispose { vue.keepScreenOn = false }
    }
    val segmentsTotal = Tokens.Composants.BarreProgression.segments
    EcranCamera(
        camera, etat.comptageSimule,
        haut = {
            BoutonFermer(t(R.string.a11y_arreter_seance), action = abandonner)
            Spacer(Modifier.weight(1f))
            AngleMesure(camera)
            Etiquette(f(R.string.seance_pour_minutes, etat.regle.minutes))
        },
    ) {
        CompteurGeant(faites, total, Tokens.TaillesTexte.titreAccueil, camera.corpsVisible)
        BarreSegments(if (total > 0) faites * segmentsTotal / total else 0, segmentsTotal)
        if (faites >= total) {
            BoutonPrincipal(t(R.string.seance_bouton), Modifier.entree(), action = valider)
        } else {
            Spacer(Modifier.height(Tokens.Hauteurs.boutonPrincipal.dp))
        }
    }
}

private fun Exercice.texteValide(): Int = when (this) {
    Exercice.POMPES -> R.string.gagne_valide_pompes
    Exercice.SQUATS -> R.string.gagne_valide_squats
    Exercice.ABDOS -> R.string.gagne_valide_abdos
    Exercice.FENTES -> R.string.gagne_valide_fentes
    Exercice.JUMPING_JACKS -> R.string.gagne_valide_jumping_jacks
}

/**
 * Écran 17 : la récompense, en plein fond accent. "+15 min" qui défile, deux stats,
 * puis le choix : ouvrir maintenant ou garder son temps. Les minutes sont déjà au solde.
 */
@Composable
fun EcranGagne(etat: EtatApp, ouvrir: () -> Unit, garder: () -> Unit) {
    val cible = etat.regle.minutes
    val reduire = reduireLesAnimations()
    var minutes by remember { mutableIntStateOf(if (reduire) cible else 0) }
    LaunchedEffect(cible) {
        while (minutes < cible) {
            delay(Tokens.Animations.CompteurGain.intervalleMs.toLong())
            minutes = (minutes + Tokens.Animations.CompteurGain.pas).coerceAtMost(cible)
        }
    }
    val maintenant = remember { System.currentTimeMillis() }
    val plageEnCours = etat.jeu.plageActive(maintenant, etat.zone)
    val texte = if (plageEnCours) {
        t(R.string.gagne_texte_plage)
    } else {
        // On calcule sur une copie ce que donnerait l'ouverture maintenant, plages à venir comprises.
        val essai = etat.jeu.ouvrirFenetre(maintenant, etat.zone).first
        f(R.string.gagne_texte, heure(essai.finDOuverture(maintenant, etat.zone) ?: maintenant, etat))
    }
    val sombre = Tokens.Couleurs.texteSurAccent
    EcranKova(
        fond = Tokens.Couleurs.accent,
        espacement = Tokens.Espacements.e13,
        bas = {
            if (plageEnCours) {
                BoutonPrincipal(t(R.string.gagne_garder), Modifier.entree(14), style = StyleBouton.SOMBRE, action = garder)
            } else {
                BoutonPrincipal(t(R.string.gagne_ouvrir), Modifier.entree(14), style = StyleBouton.SOMBRE, action = ouvrir)
                BoutonSecondaire(t(R.string.gagne_garder), couleur = sombre, action = garder)
            }
        },
    ) {
        Corps(
            f(etat.exercice.texteValide(), etat.repetitionsDeLaSeance),
            Modifier.padding(top = Tokens.Espacements.e89.dp).entree(1),
            couleur = sombre, graisse = Graisse.DEMI_GRAS,
        )
        Chiffre(f(R.string.gagne_minutes, minutes), Tokens.TaillesTexte.chiffreGeant, Modifier.pop(1), couleur = sombre)
        Paragraphe(texte, Modifier.padding(top = Tokens.Espacements.e8.dp).entree(10), couleur = sombre)
        Row(
            Modifier.padding(top = Tokens.Espacements.e13.dp).entree(12),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Espacements.e13.dp),
        ) {
            CarteStat(
                etat.jeu.repetitionsDuJour(maintenant, etat.zone).toString(), t(R.string.gagne_repetitions_jour),
                Modifier.weight(1f), Tokens.Couleurs.fond,
            )
            val serie = etat.jeu.serie(maintenant, etat.zone)
            CarteStat(
                n(R.string.commun_jours_un, R.string.commun_jours_autre, serie), t(R.string.gagne_serie),
                Modifier.weight(1f), Tokens.Couleurs.fond,
            )
        }
    }
}

/** Petite carte "valeur + libellé" (récompense, stats). */
@Composable
fun CarteStat(
    valeur: String,
    libelle: String,
    modifier: Modifier = Modifier,
    fond: Color = Tokens.Couleurs.carte,
    marge: Float = Tokens.Espacements.e21,
) {
    Column(modifier.carte(marge = marge, fond = fond), verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e3.dp)) {
        Titre(valeur, taille = Tokens.TaillesTexte.titreCarte, lignesMax = 1)
        Legende(libelle)
    }
}

/** Écran 18 : déblocage d'urgence. 5 minutes sans exercice, 2 fois par semaine, retour le lundi. */
@Composable
fun EcranUrgence(etat: EtatApp, retour: () -> Unit, debloquer: () -> Unit, faireSesPompes: () -> Unit) {
    val restantes = etat.jeu.urgencesRestantes(System.currentTimeMillis(), etat.zone)
    val disponible = etat.jeu.urgenceActivee && restantes > 0
    EcranKova(
        bas = {
            if (disponible) {
                BoutonPrincipal(
                    f(R.string.urgence_bouton, EtatJeu.MINUTES_URGENCE), Modifier.entree(6),
                    style = StyleBouton.CONTOUR, action = debloquer,
                )
                BoutonSecondaire(t(R.string.urgence_pompes), couleur = Tokens.Couleurs.texte, action = faireSesPompes)
            } else {
                BoutonPrincipal(t(R.string.urgence_pompes), Modifier.entree(6), action = faireSesPompes)
            }
        },
    ) {
        BoutonRetour(retour)
        IconeAnneau(Icone.ECLAIR, avecAnneau = false)
        Centre(Modifier.padding(top = Tokens.Espacements.e13.dp).entree(2)) { Titre(t(R.string.urgence_titre), centre = true) }
        Centre(Modifier.entree(3)) { Paragraphe(f(R.string.urgence_texte, EtatJeu.MINUTES_URGENCE), centre = true) }
        Row(
            Modifier.padding(top = Tokens.Espacements.e13.dp).entree(4).fillMaxWidth().carte(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Corps(t(R.string.urgence_restant), Modifier.weight(1f))
            Titre(
                f(R.string.urgence_compte, restantes, EtatJeu.URGENCES_PAR_SEMAINE),
                taille = Tokens.TaillesTexte.titreCarte,
                couleur = if (disponible) Tokens.Couleurs.accent else Tokens.Couleurs.texteSecondaire,
            )
        }
        if (!disponible) {
            Centre {
                Paragraphe(
                    t(if (etat.jeu.urgenceActivee) R.string.urgence_epuisee else R.string.urgence_desactivee),
                    centre = true,
                )
            }
        }
    }
}
