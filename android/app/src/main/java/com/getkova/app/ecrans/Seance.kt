package com.getkova.app.ecrans

import androidx.camera.view.PreviewView
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
import androidx.compose.ui.draw.rotate
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

/**
 * Le cadre caméra de l'essai et de la séance : aperçu en direct, ligne de scan,
 * et message quand le corps sort du cadre. Sans caméra (refus, comptage simulé),
 * il montre la silhouette de la maquette.
 */
@Composable
fun VueCamera(camera: CameraSeance, hauteur: Float, simule: Boolean) {
    val forme = RoundedCornerShape(Tokens.Arrondis.grand.dp)
    val enDirect = camera.etat == CameraSeance.Etat.ACTIVE && !simule
    val scan = positionDuScan()
    Box(
        Modifier
            .fillMaxWidth()
            .height(hauteur.dp)
            .clip(forme)
            .background(Tokens.Couleurs.carte)
            .pointilles(),
    ) {
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
            Box(
                Modifier
                    .offset(y = (hauteur * scan).dp)
                    .fillMaxWidth()
                    .height(Tokens.Traits.normal.dp)
                    .alpha(Tokens.Opacites.scan)
                    .background(Tokens.Couleurs.accent),
            )
        }
        if (enDirect && !camera.corpsVisible) {
            Corps(
                t(R.string.camera_hors_cadre),
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = Tokens.Espacements.e13.dp)
                    .clip(RoundedCornerShape(Tokens.Arrondis.moyen.dp))
                    .background(Tokens.Couleurs.accent)
                    .padding(horizontal = Tokens.Espacements.e13.dp, vertical = Tokens.Espacements.e8.dp),
                couleur = Tokens.Couleurs.texteSurAccent,
                graisse = Graisse.DEMI_GRAS,
            )
        }
    }
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

/** Écran 9 : le moment de preuve. Trois vraies pompes comptées en direct, puis le bouton apparaît. */
@Composable
fun EcranEssai(etat: EtatApp, camera: CameraSeance, retour: () -> Unit, suivant: () -> Unit) {
    val total = 3
    val proprietaire = LocalLifecycleOwner.current
    val haptique = rememberHaptique()
    val contexte = LocalView.current.context
    DisposableEffect(Unit) {
        camera.surRepetition = { compte -> if (compte >= total) haptique.serieValidee() else haptique.repetition() }
        camera.demarrer(proprietaire, Exercice.POMPES, total, etat.comptageSimule)
        onDispose { camera.arreter() }
    }
    val refusee = camera.etat == CameraSeance.Etat.REFUSEE || camera.etat == CameraSeance.Etat.INDISPONIBLE
    EcranKova(
        espacement = Tokens.Espacements.e13,
        bas = {
            when {
                camera.repetitions >= total ->
                    BoutonPrincipal(t(R.string.essai_bouton), Modifier.entree(), action = suivant)
                refusee -> {
                    // Sans caméra, l'essai est impossible : on ne bloque pas l'onboarding.
                    BoutonPrincipal(t(R.string.camera_ouvrir_reglages)) { ouvrirFicheDeLApp(contexte) }
                    BoutonSecondaire(t(R.string.essai_passer), action = suivant)
                }
                else -> Spacer(Modifier.height(Tokens.Hauteurs.boutonPrincipal.dp))
            }
        },
    ) {
        com.getkova.app.design.EnTeteEtapes(8, NOMBRE_ETAPES, retour)
        Titre(
            t(R.string.essai_titre),
            Modifier.padding(top = Tokens.Espacements.e13.dp).entree(1),
            taille = Tokens.TaillesTexte.titreCarte,
        )
        VueCamera(camera, Tokens.Composants.VueCamera.hauteurEssai, etat.comptageSimule)
        CompteurGeant(camera.repetitions, total, Tokens.TaillesTexte.titreEcran, camera.corpsVisible)
    }
}

/**
 * Écran 13 : l'app verrouillée. Sur Android, Kova affiche lui-même cet écran par
 * dessus l'app bloquée : il reprend la maquette, nom de l'app compris.
 */
@Composable
fun EcranVerrou(
    nomDeLApp: String?,
    repetitions: Int,
    minutes: Int,
    commencer: () -> Unit,
    fermer: () -> Unit,
    urgence: () -> Unit,
) {
    EcranKova(
        bas = {
            BoutonPrincipal(t(R.string.verrou_app_commencer), Modifier.entree(6), action = commencer)
            BoutonSecondaire(
                if (nomDeLApp != null) f(R.string.verrou_app_fermer_nom, nomDeLApp) else t(R.string.verrou_app_plus_tard),
                action = fermer,
            )
            BoutonSecondaire(t(R.string.ecran_urgence), action = urgence)
        },
    ) {
        IconeAnneau(Icone.CADENAS)
        Centre(Modifier.padding(top = Tokens.Espacements.e13.dp).entree(2)) {
            Titre(
                if (nomDeLApp != null) f(R.string.verrou_app_titre_nom, nomDeLApp) else t(R.string.verrou_app_titre),
                centre = true,
            )
        }
        Centre(Modifier.entree(3)) { Paragraphe(t(R.string.verrou_app_texte), centre = true) }
        Row(
            Modifier.padding(top = Tokens.Espacements.e13.dp).entree(4).fillMaxWidth().carte(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e3.dp)) {
                Titre(f(R.string.verrou_app_pompes, repetitions), taille = Tokens.TaillesTexte.titreCarte, lignesMax = 1)
                Legende(t(R.string.verrou_app_a_faire))
            }
            Pictogramme(Icone.FLECHE, Modifier.padding(horizontal = Tokens.Espacements.e8.dp), couleur = Tokens.Couleurs.texteSecondaire)
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e3.dp),
            ) {
                Titre(
                    f(R.string.commun_minutes_court, minutes),
                    taille = Tokens.TaillesTexte.titreCarte, couleur = Tokens.Couleurs.accent, lignesMax = 1,
                )
                Legende(t(R.string.verrou_app_debloquees))
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
                .padding(bottom = Tokens.Espacements.e21.dp)
                .clearAndSetSemantics { contentDescription = description },
            verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e13.dp, Alignment.Bottom),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Tokens.Espacements.e13.dp)) {
                Box(Modifier.size(Tokens.Espacements.e55.dp), contentAlignment = Alignment.Center) {
                    Anneau(Modifier.size(Tokens.Espacements.e55.dp))
                    // Inclinaison du téléphone dessiné, reprise de la maquette.
                    Box(
                        Modifier
                            .rotate(-12f)
                            .size(Tokens.Espacements.e34.dp, Tokens.Espacements.e55.dp)
                            .border(Tokens.Traits.illustration.dp, Tokens.Couleurs.accent, RoundedCornerShape(Tokens.Arrondis.petit.dp)),
                    )
                }
                Box(
                    Modifier
                        .padding(bottom = Tokens.Espacements.e8.dp)
                        .width(Tokens.Espacements.e34.dp)
                        .height(Tokens.Traits.normal.dp)
                        .background(Tokens.Couleurs.pointille),
                )
                FigurePompe(animee = false)
            }
            Legende(t(R.string.placement_distance))
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
    val segments = Tokens.Composants.BarreProgression.segments
    EcranKova(
        espacement = Tokens.Espacements.e13,
        bas = {
            if (faites >= total) {
                BoutonPrincipal(t(R.string.seance_bouton), Modifier.entree(), action = valider)
            } else {
                Spacer(Modifier.height(Tokens.Hauteurs.boutonPrincipal.dp))
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BoutonFermer(t(R.string.a11y_arreter_seance), action = abandonner)
            Spacer(Modifier.weight(1f))
            Legende(f(R.string.seance_pour_minutes, etat.regle.minutes), graisse = Graisse.MOYEN)
        }
        VueCamera(camera, Tokens.Composants.VueCamera.hauteurSeance, etat.comptageSimule)
        CompteurGeant(faites, total, Tokens.TaillesTexte.titreAccueil, camera.corpsVisible)
        BarreSegments(if (total > 0) faites * segments / total else 0, segments)
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
