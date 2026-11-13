package com.getkova.app.ecrans

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getkova.app.BuildConfig
import com.getkova.app.EtatApp
import com.getkova.app.R
import com.getkova.app.design.BoutonFermer
import com.getkova.app.design.BoutonPointille
import com.getkova.app.design.BoutonPrincipal
import com.getkova.app.design.BoutonRetour
import com.getkova.app.design.BoutonSecondaire
import com.getkova.app.design.Centre
import com.getkova.app.design.Chiffre
import com.getkova.app.design.Corps
import com.getkova.app.design.EcranKova
import com.getkova.app.design.Graisse
import com.getkova.app.design.GroupeReglages
import com.getkova.app.design.Icone
import com.getkova.app.design.IconeAnneau
import com.getkova.app.design.Interrupteur
import com.getkova.app.design.Legende
import com.getkova.app.design.LigneReglage
import com.getkova.app.design.LigneReglageLien
import com.getkova.app.design.Paragraphe
import com.getkova.app.design.Pictogramme
import com.getkova.app.design.Polices
import com.getkova.app.design.StyleBouton
import com.getkova.app.design.Titre
import com.getkova.app.design.Tokens
import com.getkova.app.design.carte
import com.getkova.app.design.clic
import com.getkova.app.design.croissanceDepuisLeBas
import com.getkova.app.design.entree
import com.getkova.app.design.f
import com.getkova.app.design.n
import com.getkova.app.design.pop
import com.getkova.app.design.t
import com.getkova.app.jeu.Plage
import com.getkova.app.jeu.Semaine
import com.getkova.app.jeu.bilanDeLaSemaine
import com.getkova.app.jeu.enVigueur
import com.getkova.app.jeu.finDOuverture
import com.getkova.app.jeu.plageActive
import com.getkova.app.jeu.repetitionsDuJour
import com.getkova.app.jeu.serie
import com.getkova.app.nom
import com.getkova.app.verrou.AppInstallee
import com.getkova.app.verrou.AppsInstallees
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle as StyleDeJour
import java.util.Locale
import java.util.UUID
import kotlin.math.ceil

enum class Onglet(val titre: Int, val icone: Icone) {
    ACCUEIL(R.string.onglet_accueil, Icone.MAISON),
    STATS(R.string.onglet_stats, Icone.STATS),
    PLANNING(R.string.onglet_planning, Icone.CALENDRIER),
    REGLAGES(R.string.onglet_reglages, Icone.REGLAGES),
}

/** Barre d'onglets de la maquette : carte arrondie, onglet actif en fond accent. */
@Composable
fun BarreOnglets(selection: Onglet, choisir: (Onglet) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().carte(marge = Tokens.Espacements.e8),
        horizontalArrangement = Arrangement.spacedBy(Tokens.Espacements.e3.dp),
    ) {
        Onglet.entries.forEach { onglet ->
            val actif = onglet == selection
            val couleur = if (actif) Tokens.Couleurs.texteSurAccent else Tokens.Couleurs.texteSecondaire
            Column(
                Modifier
                    .weight(1f)
                    .height(Tokens.Hauteurs.onglet.dp)
                    .clip(RoundedCornerShape(Tokens.Arrondis.moyen.dp))
                    .background(if (actif) Tokens.Couleurs.accent else Color.Transparent)
                    .clic { choisir(onglet) }
                    .semantics { selected = actif },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e3.dp, Alignment.CenterVertically),
            ) {
                Pictogramme(onglet.icone, couleur = couleur)
                Legende(t(onglet.titre), couleur = couleur, graisse = Graisse.DEMI_GRAS, lignesMax = 1)
            }
        }
    }
}

// --- Apps -------------------------------------------------------------------

/** Une app de la liste : icône, nom, interrupteur. Conforme à la maquette, sans le temps d'usage. */
@Composable
private fun LigneApp(app: AppInstallee, active: Boolean, basculer: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().carte(marge = Tokens.Espacements.e13, arrondi = Tokens.Arrondis.moyen),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Tokens.Espacements.e13.dp),
    ) {
        val forme = RoundedCornerShape(Tokens.Composants.VignetteApp.arrondi.dp)
        val taille = Modifier.size(Tokens.Composants.VignetteApp.taille.dp).clip(forme)
        if (app.icone != null) {
            Image(app.icone, contentDescription = null, modifier = taille)
        } else {
            Box(taille.background(Tokens.Couleurs.ligne))
        }
        Corps(app.nom, Modifier.weight(1f), graisse = Graisse.DEMI_GRAS)
        if (basculer != null) {
            Interrupteur(active, app.nom, basculer)
        } else {
            Pictogramme(Icone.CADENAS, couleur = Tokens.Couleurs.texteSecondaire)
        }
    }
}

/** La liste des apps du téléphone, avec un interrupteur par app. Les apps choisies viennent en premier. */
@Composable
fun ListeApps(selection: Set<String>, basculer: (String) -> Unit) {
    val contexte = LocalContext.current
    var apps by remember { mutableStateOf<List<AppInstallee>?>(null) }
    val initiale = remember { selection }
    LaunchedEffect(Unit) { apps = AppsInstallees.charger(contexte) }
    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e8.dp)) {
        apps?.sortedBy { it.paquet !in initiale }?.forEach { app ->
            LigneApp(app, app.paquet in selection) { basculer(app.paquet) }
        }
    }
}

/** Les apps verrouillées, en lecture seule (accueil). */
@Composable
fun AppsVerrouillees(selection: Set<String>) {
    val contexte = LocalContext.current
    var apps by remember { mutableStateOf<List<AppInstallee>>(emptyList()) }
    LaunchedEffect(selection) { apps = AppsInstallees.charger(contexte).filter { it.paquet in selection } }
    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e8.dp)) {
        apps.forEachIndexed { rang, app ->
            Box(Modifier.entree(4 + rang)) { LigneApp(app, active = true, basculer = null) }
        }
    }
}

/** Écran 5 : choix des apps à verrouiller. */
@Composable
fun EcranApps(etat: EtatApp, retour: () -> Unit, suivant: () -> Unit) {
    val nombre = etat.jeu.apps.size
    EcranKova(
        espacement = Tokens.Espacements.e13,
        bas = { BoutonPrincipal(t(R.string.apps_bouton), Modifier.entree(5), actif = nombre > 0, action = suivant) },
    ) {
        com.getkova.app.design.EnTeteEtapes(5, NOMBRE_ETAPES, retour)
        Titre(t(R.string.apps_titre), Modifier.padding(top = Tokens.Espacements.e13.dp).entree(1))
        Paragraphe(
            if (nombre == 0) t(R.string.apps_aucune) else n(R.string.apps_compte_un, R.string.apps_compte_autre, nombre),
            Modifier.entree(2),
        )
        Box(Modifier.entree(3)) {
            // Pendant l'onboarding, rien n'est encore verrouillé : pas de délai.
            ListeApps(etat.jeu.apps) { paquet ->
                val apps = etat.jeu.apps
                etat.definirApps(if (paquet in apps) apps - paquet else apps + paquet, immediat = true)
            }
        }
    }
}

/** Sans abonnement et avec plus d'une app choisie : il faut n'en garder qu'une (brief, section 4). */
@Composable
fun EcranLimiteGratuite(etat: EtatApp, voirAbonnement: () -> Unit, terminer: () -> Unit) {
    EcranKova(
        bas = {
            if (etat.jeu.apps.size == 1) {
                BoutonPrincipal(t(R.string.commun_continuer), action = terminer)
            }
            BoutonSecondaire(t(R.string.limite_abonnement), action = voirAbonnement)
        },
    ) {
        IconeAnneau(Icone.CADENAS, avecAnneau = false)
        Centre(Modifier.padding(top = Tokens.Espacements.e13.dp).entree(2)) { Titre(t(R.string.limite_titre), centre = true) }
        Centre(Modifier.entree(3)) { Paragraphe(t(R.string.limite_texte), centre = true) }
        Box(Modifier.entree(4)) {
            // Toucher une app ne garde qu'elle.
            ListeApps(etat.jeu.apps) { paquet -> etat.definirApps(setOf(paquet), immediat = true) }
        }
    }
}

/** Écran 22 : modifier les apps verrouillées. Retirer une app prend effet 24 h plus tard. */
@Composable
fun EcranAppsReglage(etat: EtatApp, retour: () -> Unit, ouvrirPaywall: () -> Unit) {
    EcranKova(
        espacement = Tokens.Espacements.e13,
        bas = { BoutonPrincipal(t(R.string.commun_enregistrer), Modifier.entree(4), action = retour) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Espacements.e13.dp)) {
            BoutonRetour(retour)
            Titre(t(R.string.ecran_apps_reglage), taille = Tokens.TaillesTexte.titreCarte)
        }
        Paragraphe(
            n(R.string.apps_reglage_texte_un, R.string.apps_reglage_texte_autre, etat.jeu.apps.size),
            Modifier.entree(1),
        )
        Box(Modifier.entree(3)) {
            ListeApps(etat.jeu.apps) { paquet ->
                val apps = etat.jeu.apps
                val nouvelles = if (paquet in apps) apps - paquet else apps + paquet
                if (!etat.abonne && nouvelles.size > 1) {
                    // Plus d'une app demande l'abonnement : le choix n'est pas enregistré.
                    ouvrirPaywall()
                } else {
                    etat.definirApps(nouvelles, immediat = false)
                }
            }
        }
    }
}

// --- Accueil ----------------------------------------------------------------

/**
 * Écran 12 : l'accueil. Solde en très gros, série, bouton "Gagner du temps", apps
 * verrouillées. Quand une fenêtre est ouverte, le gros chiffre devient le temps restant.
 */
@Composable
fun EcranAccueil(etat: EtatApp, gagnerDuTemps: () -> Unit, ouvrirVerrou: () -> Unit) {
    val maintenant = horloge()
    val jeu = etat.jeu
    val fin = jeu.finDOuverture(maintenant, etat.zone)
    val serie = jeu.serie(maintenant, etat.zone)
    LaunchedEffect(fin == null) { etat.rafraichir() }
    EcranKova {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Titre(Tokens.nom, Modifier.weight(1f), taille = Tokens.TaillesTexte.corpsFort)
            Legende(
                n(R.string.accueil_serie_un, R.string.accueil_serie_autre, serie),
                couleur = Tokens.Couleurs.accent, graisse = Graisse.MOYEN,
            )
        }
        Column(
            Modifier.padding(top = Tokens.Espacements.e13.dp).entree(1),
            verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e5.dp),
        ) {
            if (fin != null) {
                Legende(f(R.string.accueil_ouvert_jusqua, heure(fin, etat)), couleur = Tokens.Couleurs.accent)
                // Arrondi vers le haut : il reste "1 min" jusqu'à la dernière seconde.
                GrosChiffre(ceil((fin - maintenant) / 60_000.0).toInt().coerceAtLeast(0))
            } else {
                Legende(t(R.string.accueil_solde))
                GrosChiffre(jeu.soldeMinutes)
            }
            Legende(f(R.string.accueil_aujourdhui, jeu.repetitionsDuJour(maintenant, etat.zone)))
            if (fin == null && jeu.plageActive(maintenant, etat.zone)) {
                Legende(t(R.string.accueil_plage), couleur = Tokens.Couleurs.accent)
            } else if (fin == null && jeu.soldeMinutes > 0) {
                BoutonSecondaire(t(R.string.accueil_ouvrir), couleur = Tokens.Couleurs.accent) { etat.ouvrirFenetre() }
            }
        }
        BoutonPrincipal(t(R.string.accueil_bouton), pulse = true, action = gagnerDuTemps)
        Column(verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e8.dp)) {
            Legende(
                t(R.string.accueil_apps).uppercase(),
                graisse = Graisse.DEMI_GRAS,
                interlettrage = (Tokens.Typographie.interlettrageSurtitre * Tokens.TaillesTexte.legende).sp,
            )
            Box(Modifier.clic(action = ouvrirVerrou)) { AppsVerrouillees(jeu.apps) }
        }
    }
}

@Composable
private fun GrosChiffre(minutes: Int) {
    val description = f(R.string.commun_minutes_court, minutes)
    Row(
        Modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(Tokens.Espacements.e8.dp),
    ) {
        Chiffre(minutes.toString(), Tokens.TaillesTexte.chiffreGeant, Modifier.alignByBaseline())
        Chiffre(
            t(R.string.commun_min), Tokens.TaillesTexte.titreEcran, Modifier.alignByBaseline(),
            couleur = Tokens.Couleurs.texteSecondaire,
        )
    }
}

// --- Stats ------------------------------------------------------------------

/** Initiales des jours, du lundi au dimanche, dans la langue du téléphone. */
private fun initialesDesJours(): List<String> =
    DayOfWeek.entries.map { it.getDisplayName(StyleDeJour.NARROW_STANDALONE, Locale.getDefault()) }

/** Écran 19 : la semaine. Total de répétitions, barres par jour, quatre cartes. */
@Composable
fun EcranStats(etat: EtatApp) {
    val maintenant = remember { System.currentTimeMillis() }
    val bilan = etat.jeu.bilanDeLaSemaine(maintenant, etat.zone)
    val lundi = Semaine.date(Semaine.debut(maintenant, etat.zone), etat.zone).toLocalDate()
    val format = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val progression = when (val pourcent = bilan.progressionPourcent) {
        null -> t(R.string.stats_progression_aucune)
        else -> f(if (pourcent >= 0) R.string.stats_progression_hausse else R.string.stats_progression_baisse, kotlin.math.abs(pourcent))
    }
    val gagne = if (bilan.minutesGagnees >= 60) {
        f(R.string.stats_gagne_heures, bilan.minutesGagnees / 60, bilan.minutesGagnees % 60)
    } else {
        f(R.string.commun_minutes_court, bilan.minutesGagnees)
    }
    val serie = etat.jeu.serie(maintenant, etat.zone)
    EcranKova(espacement = Tokens.Espacements.e13) {
        Row(verticalAlignment = Alignment.Bottom) {
            Titre(t(R.string.stats_titre), Modifier.weight(1f))
            Legende(format.format(lundi) + " – " + format.format(lundi.plusDays(6)), Modifier.padding(bottom = Tokens.Espacements.e5.dp))
        }
        val description = f(R.string.a11y_stats, bilan.total)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Tokens.Arrondis.grand.dp))
                .background(Tokens.Couleurs.carte)
                .padding(
                    start = Tokens.Espacements.e13.dp, end = Tokens.Espacements.e13.dp,
                    top = Tokens.Espacements.e21.dp, bottom = Tokens.Espacements.e13.dp,
                )
                .clearAndSetSemantics { contentDescription = description },
            verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e8.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Titre(bilan.total.toString(), Modifier.weight(1f), taille = Tokens.TaillesTexte.titreAccueil)
                Legende(t(R.string.stats_total), Modifier.padding(bottom = Tokens.Espacements.e8.dp))
            }
            val maximum = (bilan.repetitionsParJour.maxOrNull() ?: 1).coerceAtLeast(1)
            val initiales = initialesDesJours()
            Row(
                Modifier.height(Tokens.Composants.Graphique.hauteur.dp),
                horizontalArrangement = Arrangement.spacedBy(Tokens.Espacements.e8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                bilan.repetitionsParJour.forEachIndexed { rang, valeur ->
                    val aujourdHui = rang == bilan.rangAujourdHui
                    val hauteur = if (valeur > 0) {
                        Tokens.Composants.Graphique.barreMax * valeur / maximum
                    } else {
                        Tokens.Composants.Graphique.barreVide
                    }
                    val couleur = when {
                        valeur > 0 && aujourdHui -> Tokens.Couleurs.accent
                        valeur > 0 -> Tokens.Couleurs.barreInactive
                        else -> Tokens.Couleurs.ligne
                    }
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e8.dp),
                    ) {
                        Legende(if (valeur > 0) valeur.toString() else " ")
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(hauteur.dp)
                                .croissanceDepuisLeBas(1 + rang)
                                .clip(RoundedCornerShape(Tokens.Arrondis.petit.dp))
                                .background(couleur),
                        )
                        Legende(
                            initiales.getOrElse(rang) { "" },
                            couleur = if (aujourdHui) Tokens.Couleurs.texte else Tokens.Couleurs.texteSecondaire,
                            graisse = Graisse.DEMI_GRAS,
                        )
                    }
                }
            }
        }
        val marge = Tokens.Espacements.e13
        Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Espacements.e13.dp)) {
            CarteStat(bilan.seances.toString(), t(R.string.stats_seances), Modifier.weight(1f).entree(7), marge = marge)
            CarteStat(progression, t(R.string.stats_progression), Modifier.weight(1f).entree(8), marge = marge)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Espacements.e13.dp)) {
            CarteStat(
                n(R.string.commun_jours_un, R.string.commun_jours_autre, serie), t(R.string.stats_serie),
                Modifier.weight(1f).entree(9), marge = marge,
            )
            CarteStat(gagne, t(R.string.stats_gagne), Modifier.weight(1f).entree(10), marge = marge)
        }
    }
}

/** Onglet réservé à l'abonnement : un titre, une phrase et l'accès au paywall. */
@Composable
fun EcranReserve(titre: String, texte: String, voir: () -> Unit) {
    EcranKova(espacement = Tokens.Espacements.e13) {
        Titre(titre)
        IconeAnneau(Icone.CADENAS, avecAnneau = false)
        Centre(Modifier.entree(2)) { Paragraphe(texte, centre = true) }
        BoutonPrincipal(t(R.string.abonnement_voir), Modifier.padding(top = Tokens.Espacements.e13.dp).entree(3), action = voir)
    }
}

// --- Planning ---------------------------------------------------------------

private fun heureDePlage(minutes: Int): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(LocalTime.of(minutes / 60 % 24, minutes % 60))

@Composable
private fun libelleDesJours(jours: Set<Int>): String = when (jours) {
    Plage.tousLesJours -> t(R.string.planning_jours_tous)
    setOf(1, 2, 3, 4, 5) -> t(R.string.planning_jours_semaine)
    setOf(6, 7) -> t(R.string.planning_jours_weekend)
    else -> jours.sorted().joinToString(", ") {
        DayOfWeek.of(it).getDisplayName(StyleDeJour.SHORT_STANDALONE, Locale.getDefault())
    }
}

/**
 * Écran 20 : les plages strictes. Pendant une plage active, les apps restent
 * verrouillées même avec du solde. Activer est immédiat, désactiver prend 24 h.
 */
@Composable
fun EcranPlanning(etat: EtatApp, ouvrirPaywall: () -> Unit, ajouter: () -> Unit) {
    val maintenant = remember { System.currentTimeMillis() }
    EcranKova(espacement = Tokens.Espacements.e13) {
        Titre(t(R.string.planning_titre))
        Paragraphe(t(R.string.planning_texte), Modifier.entree(1))
        etat.jeu.plages.forEachIndexed { rang, plage ->
            val enSursis = !plage.active && plage.enVigueur(maintenant)
            Row(
                Modifier.entree(2 + rang).fillMaxWidth().carte(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Tokens.Espacements.e13.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e3.dp)) {
                    Corps(plage.nom, graisse = Graisse.DEMI_GRAS)
                    Titre(
                        f(R.string.planning_horaires, heureDePlage(plage.debut), heureDePlage(plage.fin)),
                        taille = Tokens.TaillesTexte.titreCarte,
                    )
                    Legende(
                        if (enSursis) t(R.string.planning_sursis) else libelleDesJours(plage.jours),
                        couleur = if (enSursis) Tokens.Couleurs.accent else Tokens.Couleurs.texteSecondaire,
                    )
                }
                Interrupteur(plage.active, f(R.string.a11y_plage, plage.nom)) { etat.basculerPlage(plage.id) }
            }
        }
        if (etat.jeu.plages.size < Plage.NOMBRE_MAXIMUM) {
            Box(Modifier.entree(6)) {
                // Le planning fait partie de l'abonnement (brief, section 4).
                BoutonPointille(t(R.string.planning_ajouter)) { if (etat.abonne) ajouter() else ouvrirPaywall() }
            }
        }
    }
}

/** Ajout d'une plage : nom, heures (par pas de 30 minutes) et jours. */
@Composable
fun EcranEditeurPlage(annuler: () -> Unit, ajouter: (Plage) -> Unit) {
    var nom by remember { mutableStateOf("") }
    var debut by remember { mutableIntStateOf(9 * 60) }
    var fin by remember { mutableIntStateOf(18 * 60) }
    var jours by remember { mutableStateOf(setOf(1, 2, 3, 4, 5)) }
    val valide = jours.isNotEmpty() && debut != fin
    val sansNom = t(R.string.planning_editeur_sans_nom)
    val journee = 24 * 60
    val pas = 30
    EcranKova(
        bas = {
            BoutonPrincipal(t(R.string.planning_editeur_bouton), actif = valide) {
                ajouter(Plage(UUID.randomUUID().toString(), nom.trim().ifEmpty { sansNom }, debut, fin, jours))
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Titre(t(R.string.planning_ajouter), Modifier.weight(1f), taille = Tokens.TaillesTexte.titreCarte)
            BoutonFermer(action = annuler)
        }
        Box(
            Modifier.fillMaxWidth().heightIn(min = Tokens.Hauteurs.option.dp).carte(marge = Tokens.Espacements.e13),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (nom.isEmpty()) {
                Corps(t(R.string.planning_editeur_nom), couleur = Tokens.Couleurs.texteSecondaire)
            }
            BasicTextField(
                value = nom,
                onValueChange = { nom = it.take(30) },
                singleLine = true,
                textStyle = TextStyle(color = Tokens.Couleurs.texte, fontFamily = Polices.texte, fontSize = Tokens.TaillesTexte.corps.sp),
                cursorBrush = SolidColor(Tokens.Couleurs.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        GroupeReglages {
            LigneHeure(t(R.string.planning_editeur_debut), debut) { debut = (debut + it * pas + journee) % journee }
            LigneHeure(t(R.string.planning_editeur_fin), fin) { fin = (fin + it * pas + journee) % journee }
        }
        val initiales = initialesDesJours()
        Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Espacements.e5.dp)) {
            (1..7).forEach { jour ->
                val choisi = jour in jours
                val nomDuJour = DayOfWeek.of(jour).getDisplayName(StyleDeJour.FULL_STANDALONE, Locale.getDefault())
                Box(
                    Modifier
                        .weight(1f)
                        .heightIn(min = Tokens.Hauteurs.boutonIcone.dp)
                        .clip(RoundedCornerShape(Tokens.Arrondis.moyen.dp))
                        .background(if (choisi) Tokens.Couleurs.accent else Tokens.Couleurs.carte)
                        .clic { jours = if (choisi) jours - jour else jours + jour }
                        .clearAndSetSemantics {
                            contentDescription = nomDuJour
                            selected = choisi
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Legende(
                        initiales[jour - 1],
                        couleur = if (choisi) Tokens.Couleurs.texteSurAccent else Tokens.Couleurs.texteSecondaire,
                        graisse = Graisse.DEMI_GRAS,
                    )
                }
            }
        }
        Paragraphe(t(R.string.planning_editeur_note))
    }
}

@Composable
private fun LigneHeure(libelle: String, minutes: Int, changer: (Int) -> Unit) {
    LigneReglage(libelle) {
        Box(Modifier.size(Tokens.Hauteurs.boutonIcone.dp).clic { changer(-1) }, contentAlignment = Alignment.Center) {
            Pictogramme(Icone.CHEVRON_GAUCHE, couleur = Tokens.Couleurs.texteSecondaire)
        }
        Titre(heureDePlage(minutes), taille = Tokens.TaillesTexte.corpsFort)
        Box(Modifier.size(Tokens.Hauteurs.boutonIcone.dp).clic { changer(1) }, contentAlignment = Alignment.Center) {
            Pictogramme(Icone.CHEVRON_DROIT, couleur = Tokens.Couleurs.texteSecondaire)
        }
    }
}

// --- Réglages ---------------------------------------------------------------

enum class Destination {
    VERROU, EXERCICE, PLACEMENT, COMPTEUR, GAGNE, URGENCE,
    APPS_REGLAGE, REGLE, EXERCICE_PAR_DEFAUT, ABONNEMENT, EDITEUR_PLAGE, CHANTIER,
}

/** Écran 21 : les réglages, en trois groupes (règles, options, compte). */
@Composable
fun EcranReglages(etat: EtatApp, ouvrir: (Destination) -> Unit) {
    val ouvrirLien = ouvreurDeLiens()
    val texteRegle = f(R.string.reglages_regle_valeur, etat.regle.repetitions, etat.regle.minutes)
    EcranKova(espacement = Tokens.Espacements.e13) {
        Titre(t(R.string.reglages_titre))
        Box(Modifier.entree(1)) {
            GroupeReglages {
                LigneReglageLien(
                    t(R.string.ecran_apps_reglage),
                    n(R.string.reglages_apps_un, R.string.reglages_apps_autre, etat.jeu.apps.size),
                ) { ouvrir(Destination.APPS_REGLAGE) }
                LigneReglageLien(
                    t(R.string.reglages_regle),
                    if (etat.jeu.regleEnAttente == null) texteRegle else f(R.string.reglages_regle_en_attente, texteRegle),
                ) { ouvrir(Destination.REGLE) }
                LigneReglageLien(t(R.string.reglages_exercice), t(etat.jeu.exerciceParDefaut.nom)) {
                    ouvrir(Destination.EXERCICE_PAR_DEFAUT)
                }
            }
        }
        Box(Modifier.entree(2)) {
            GroupeReglages {
                LigneReglage(t(R.string.ecran_urgence)) {
                    Interrupteur(etat.jeu.urgenceActivee, t(R.string.ecran_urgence)) {
                        etat.definirUrgenceActivee(!etat.jeu.urgenceActivee)
                    }
                }
                LigneReglage(t(R.string.reglages_voix)) {
                    Interrupteur(etat.compterAVoixHaute, t(R.string.reglages_voix)) {
                        etat.definirOption("compterAVoixHaute", !etat.compterAVoixHaute)
                    }
                }
            }
        }
        Box(Modifier.entree(3)) {
            GroupeReglages {
                LigneReglageLien(
                    t(R.string.reglages_abonnement),
                    t(if (etat.abonne) R.string.formule_annuel else R.string.formule_gratuite),
                ) { ouvrir(Destination.ABONNEMENT) }
                if (Liens.CONTACT.isNotEmpty()) {
                    LigneReglageLien(t(R.string.reglages_aide)) { ouvrirLien("mailto:" + Liens.CONTACT) }
                }
                if (Liens.CONFIDENTIALITE.isNotEmpty()) {
                    LigneReglageLien(t(R.string.legal_confidentialite)) { ouvrirLien(Liens.CONFIDENTIALITE) }
                }
                if (Liens.CONDITIONS.isNotEmpty()) {
                    LigneReglageLien(t(R.string.legal_conditions_longues)) { ouvrirLien(Liens.CONDITIONS) }
                }
            }
        }
        if (BuildConfig.DEBUG) {
            // Accès de chantier, absent de la version publiée.
            BoutonSecondaire(t(R.string.socle_chantier)) { ouvrir(Destination.CHANTIER) }
        }
    }
}

/** Écran 23 : la formule actuelle et la gestion de l'abonnement. */
@Composable
fun EcranAbonnement(etat: EtatApp, retour: () -> Unit, ouvrirPaywall: () -> Unit, restaurer: () -> Unit) {
    val ouvrirLien = ouvreurDeLiens()
    val sombre = Tokens.Couleurs.texteSurAccent
    EcranKova(
        espacement = Tokens.Espacements.e13,
        bas = {
            if (etat.abonne) {
                BoutonPrincipal(t(R.string.abonnement_gerer), style = StyleBouton.BORDURE) { ouvrirLien(Liens.ABONNEMENTS) }
            } else {
                BoutonPrincipal(t(R.string.abonnement_voir), action = ouvrirPaywall)
            }
            BoutonSecondaire(t(R.string.commun_restaurer_achats), action = restaurer)
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Espacements.e13.dp)) {
            BoutonRetour(retour)
            Titre(t(R.string.ecran_paywall), taille = Tokens.TaillesTexte.titreCarte)
        }
        Column(
            Modifier.padding(top = Tokens.Espacements.e8.dp).pop(1).fillMaxWidth().carte(fond = Tokens.Couleurs.accent),
            verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e5.dp),
        ) {
            Legende(t(R.string.abonnement_actuelle), couleur = sombre, graisse = Graisse.DEMI_GRAS)
            Titre(
                t(if (etat.abonne) R.string.formule_annuel else R.string.formule_gratuite),
                taille = Tokens.TaillesTexte.titreAccueil, couleur = sombre,
            )
            Corps(
                if (etat.abonne) f(R.string.abonnement_prix_annuel, t(R.string.prix_annuel)) else t(R.string.abonnement_gratuit),
                couleur = sombre,
            )
        }
        if (etat.abonne) {
            Paragraphe(t(R.string.abonnement_texte), taille = Tokens.TaillesTexte.legende)
        }
    }
}

/** Écran de chantier (version de test seulement) : réglages qui aident à essayer l'app. */
@Composable
fun EcranChantier(etat: EtatApp, retour: () -> Unit) {
    EcranKova(espacement = Tokens.Espacements.e13) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Espacements.e13.dp)) {
            BoutonRetour(retour)
            Titre(t(R.string.socle_chantier), taille = Tokens.TaillesTexte.titreCarte)
        }
        GroupeReglages {
            LigneReglage(t(R.string.socle_simulation)) {
                Interrupteur(etat.abonne, t(R.string.socle_simulation)) { etat.definirOption("abonnementSimule", !etat.abonne) }
            }
            LigneReglage(t(R.string.chantier_comptage)) {
                Interrupteur(etat.comptageSimule, t(R.string.chantier_comptage)) {
                    etat.definirOption("comptageSimule", !etat.comptageSimule)
                }
            }
            LigneReglage(t(R.string.socle_version)) { Legende(BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")") }
        }
        BoutonSecondaire(t(R.string.socle_revoir_onboarding)) { etat.revoirOnboarding() }
    }
}
