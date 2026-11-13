package com.getkova.app.ecrans

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.getkova.app.EtatApp
import com.getkova.app.Objectif
import com.getkova.app.R
import com.getkova.app.TempsParJour
import com.getkova.app.design.BoutonFermer
import com.getkova.app.design.BoutonPrincipal
import com.getkova.app.design.BoutonRetour
import com.getkova.app.design.BoutonSecondaire
import com.getkova.app.design.CarteOption
import com.getkova.app.design.Centre
import com.getkova.app.design.Chiffre
import com.getkova.app.design.Corps
import com.getkova.app.design.EcranKova
import com.getkova.app.design.EnTeteEtapes
import com.getkova.app.design.Graisse
import com.getkova.app.design.Icone
import com.getkova.app.design.IconeAnneau
import com.getkova.app.design.Legende
import com.getkova.app.design.LigneCoche
import com.getkova.app.design.LogoKova
import com.getkova.app.design.Paragraphe
import com.getkova.app.design.StyleBouton
import com.getkova.app.design.Titre
import com.getkova.app.design.Tokens
import com.getkova.app.design.carte
import com.getkova.app.design.clic
import com.getkova.app.design.entree
import com.getkova.app.design.f
import com.getkova.app.design.n
import com.getkova.app.design.pop
import com.getkova.app.design.reduireLesAnimations
import com.getkova.app.design.remplissageDepuisLaGauche
import com.getkova.app.design.t
import com.getkova.app.jeu.Exercice
import com.getkova.app.jeu.Regle
import kotlinx.coroutines.delay

/** Étapes comptées par la barre d'avancement : de Temps à Essai. */
const val NOMBRE_ETAPES = 8

/** Écran 1 : accroche. */
@Composable
fun EcranBienvenue(commencer: () -> Unit) {
    EcranKova(
        espacement = Tokens.Espacements.e34,
        bas = { BoutonPrincipal(t(R.string.bienvenue_commencer), Modifier.entree(9), action = commencer) },
    ) {
        LogoKova(Modifier.padding(top = Tokens.Espacements.e144.dp).pop())
        Titre(t(R.string.bienvenue_titre), Modifier.entree(4), taille = Tokens.TaillesTexte.titreAccueil)
        Paragraphe(t(R.string.bienvenue_texte), Modifier.entree(6))
    }
}

/** Écran 2 : "Combien de temps par jour sur ton téléphone ?" Un seul choix. */
@Composable
fun EcranTemps(etat: EtatApp, retour: () -> Unit, suivant: () -> Unit) {
    EcranKova(bas = { BoutonPrincipal(t(R.string.commun_continuer), Modifier.entree(5), action = suivant) }) {
        EnTeteEtapes(1, NOMBRE_ETAPES, retour)
        Titre(t(R.string.temps_titre), Modifier.padding(top = Tokens.Espacements.e13.dp).entree(1))
        Paragraphe(t(R.string.temps_texte), Modifier.entree(2))
        Column(
            Modifier.padding(top = Tokens.Espacements.e13.dp).entree(3),
            verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e13.dp),
        ) {
            TempsParJour.entries.forEach { choix ->
                CarteOption(etat.tempsParJour == choix, { etat.choisirTemps(choix) }) {
                    Corps(t(choix.libelle), Modifier.weight(1f), graisse = Graisse.DEMI_GRAS)
                    if (choix == TempsParJour.CINQ_A_SIX) {
                        Legende(t(R.string.temps_moyenne), graisse = Graisse.MOYEN)
                    }
                }
            }
        }
    }
}

/** Écran 3 : le compteur monte jusqu'au nombre de jours par an sur le téléphone. */
@Composable
fun EcranChoc(etat: EtatApp, retour: () -> Unit, suivant: () -> Unit) {
    val temps = etat.tempsParJour
    val cible = temps.joursParAn
    val reduire = reduireLesAnimations()
    var jours by remember { mutableIntStateOf(if (reduire) cible else 0) }
    LaunchedEffect(cible) {
        while (jours < cible) {
            delay(Tokens.Animations.CompteurChoc.intervalleMs.toLong())
            jours = (jours + Tokens.Animations.CompteurChoc.pas).coerceAtMost(cible)
        }
    }
    val mois = temps.moisSurDixAns
    val duree = if (mois >= 12) {
        n(R.string.choc_plus_de_ans_un, R.string.choc_plus_de_ans_autre, mois / 12)
    } else {
        n(R.string.choc_mois_un, R.string.choc_mois_autre, mois)
    }
    val description = f(R.string.a11y_choc, cible)

    EcranKova(bas = { BoutonPrincipal(t(R.string.choc_bouton), Modifier.entree(21), action = suivant) }) {
        EnTeteEtapes(2, NOMBRE_ETAPES, retour)
        Corps(
            f(R.string.choc_intro, temps.heures),
            Modifier.padding(top = Tokens.Espacements.e34.dp).entree(1),
            couleur = Tokens.Couleurs.texteSecondaire,
        )
        Row(
            Modifier.clearAndSetSemantics { contentDescription = description },
            horizontalArrangement = Arrangement.spacedBy(Tokens.Espacements.e8.dp),
        ) {
            // Trois chiffres (7 h ou plus) ne tiennent pas à la taille de la maquette.
            val taille = if (cible >= 100) Tokens.TaillesTexte.chiffreGeant else Tokens.TaillesTexte.chiffreChoc
            Chiffre(jours.toString(), taille, Modifier.alignByBaseline(), couleur = Tokens.Couleurs.accent)
            Titre(t(R.string.choc_jours), Modifier.alignByBaseline())
        }
        Paragraphe(
            f(R.string.choc_texte, duree),
            Modifier.entree(13),
            couleur = Tokens.Couleurs.texte,
            taille = Tokens.TaillesTexte.corpsFort,
        )
        Column(
            Modifier.padding(top = Tokens.Espacements.e13.dp).entree(17).fillMaxWidth().carte(),
            verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e13.dp),
        ) {
            Row {
                Legende(t(R.string.choc_annee), Modifier.weight(1f), couleur = Tokens.Couleurs.texte)
                Legende(f(R.string.choc_part, temps.pourcentDeLAnnee))
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(Tokens.Composants.BarreAnnee.hauteur.dp)
                    .clip(RoundedCornerShape(Tokens.Composants.BarreAnnee.arrondi.dp))
                    .background(Tokens.Couleurs.ligne),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(temps.pourcentDeLAnnee / 100f)
                        .fillMaxHeight()
                        .remplissageDepuisLaGauche(18)
                        .background(Tokens.Couleurs.accent),
                )
            }
        }
    }
}

/** Écran 4 : "Tu veux quoi, au fond ?" Plusieurs choix possibles. */
@Composable
fun EcranObjectif(etat: EtatApp, retour: () -> Unit, suivant: () -> Unit) {
    EcranKova(bas = { BoutonPrincipal(t(R.string.commun_continuer), Modifier.entree(5), action = suivant) }) {
        EnTeteEtapes(3, NOMBRE_ETAPES, retour)
        Titre(t(R.string.objectif_titre), Modifier.padding(top = Tokens.Espacements.e13.dp).entree(1))
        Paragraphe(t(R.string.objectif_texte), Modifier.entree(2))
        Column(
            Modifier.padding(top = Tokens.Espacements.e13.dp).entree(3),
            verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e13.dp),
        ) {
            Objectif.entries.forEach { objectif ->
                val choisi = objectif in etat.objectifs
                CarteOption(choisi, { etat.basculerObjectif(objectif) }) {
                    Corps(t(objectif.libelle), Modifier.weight(1f), graisse = Graisse.DEMI_GRAS)
                    if (choisi) {
                        Legende(t(R.string.objectif_choisi), graisse = Graisse.MOYEN)
                    }
                }
            }
        }
    }
}

/**
 * Écrans d'autorisation : une explication, puis la demande. En cas de refus,
 * l'écran explique pourquoi l'accès est nécessaire (brief, section 7).
 */
@Composable
fun EcranAutorisation(
    etape: Int,
    icone: Icone,
    titre: String,
    texte: String,
    arguments: List<String>,
    bouton: String,
    retour: () -> Unit,
    autoriser: () -> Unit,
    refus: String? = null,
    aide: String? = null,
    boutonSecondaire: String? = null,
    actionSecondaire: () -> Unit = {},
) {
    EcranKova(
        bas = {
            BoutonPrincipal(bouton, Modifier.entree(6), action = autoriser)
            if (boutonSecondaire != null) {
                BoutonSecondaire(boutonSecondaire, action = actionSecondaire)
            }
        },
    ) {
        EnTeteEtapes(etape, NOMBRE_ETAPES, retour)
        IconeAnneau(icone)
        Centre(Modifier.padding(top = Tokens.Espacements.e13.dp).entree(2)) { Titre(titre, centre = true) }
        Centre(Modifier.entree(3)) { Paragraphe(texte, centre = true) }
        Column(
            Modifier.padding(top = Tokens.Espacements.e13.dp).entree(4).fillMaxWidth().carte(),
            verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e13.dp),
        ) {
            arguments.forEach { LigneCoche(it) }
        }
        if (refus != null) {
            Centre { Paragraphe(refus, couleur = Tokens.Couleurs.accent, centre = true) }
        }
        if (aide != null) {
            Paragraphe(aide, taille = Tokens.TaillesTexte.legende)
        }
    }
}

/** Écran 6 : la règle. Deux compteurs, répétitions et minutes. Sert aussi depuis les réglages. */
@Composable
fun EcranRegle(etat: EtatApp, onboarding: Boolean, retour: () -> Unit, valider: () -> Unit) {
    var brouillon by remember { mutableStateOf(etat.jeu.regleEnAttente ?: etat.regle) }
    EcranKova(
        bas = {
            BoutonPrincipal(t(R.string.regle_bouton), Modifier.entree(5)) {
                // Dans les réglages, assouplir la règle prend effet 24 h plus tard.
                etat.proposerRegle(brouillon, immediat = onboarding)
                valider()
            }
        },
    ) {
        if (onboarding) EnTeteEtapes(6, NOMBRE_ETAPES, retour) else BoutonRetour(retour)
        Titre(t(R.string.regle_titre), Modifier.padding(top = Tokens.Espacements.e13.dp).entree(1))
        Paragraphe(t(if (onboarding) R.string.regle_texte else R.string.regle_texte_reglages), Modifier.entree(2))
        Column(
            Modifier.padding(top = Tokens.Espacements.e13.dp).entree(3),
            verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e13.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CompteurDeRegle(
                brouillon.repetitions, t(R.string.regle_unite_pompes),
                t(R.string.a11y_moins_de_pompes), t(R.string.a11y_plus_de_pompes),
            ) { brouillon = brouillon.changerRepetitions(it) }
            Titre("=", taille = Tokens.TaillesTexte.corpsFort, couleur = Tokens.Couleurs.texteSecondaire)
            CompteurDeRegle(
                brouillon.minutes, t(R.string.regle_unite_minutes),
                t(R.string.a11y_moins_de_minutes), t(R.string.a11y_plus_de_minutes),
            ) { brouillon = brouillon.changerMinutes(it) }
        }
        Centre { Legende(f(R.string.regle_par_heure, brouillon.pompesParHeure)) }
        val enAttenteLe = etat.jeu.regleEnAttenteLe
        if (!onboarding && etat.jeu.regleEnAttente != null && enAttenteLe != null) {
            Centre { Legende(f(R.string.regle_en_attente, dateEtHeure(enAttenteLe, etat)), couleur = Tokens.Couleurs.accent, centre = true) }
        }
    }
}

@Composable
private fun CompteurDeRegle(valeur: Int, unite: String, moins: String, plus: String, changer: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().carte(), verticalAlignment = Alignment.CenterVertically) {
        BoutonPas("−", moins) { changer(-Regle.PAS) }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Chiffre(valeur.toString(), Tokens.TaillesTexte.chiffre)
            Legende(unite)
        }
        BoutonPas("+", plus) { changer(Regle.PAS) }
    }
}

@Composable
private fun BoutonPas(signe: String, libelle: String, action: () -> Unit) {
    Box(
        Modifier
            .size(Tokens.Hauteurs.option.dp)
            .clip(RoundedCornerShape(Tokens.Arrondis.grand.dp))
            .background(Tokens.Couleurs.ligne)
            .clic(action = action)
            .clearAndSetSemantics { contentDescription = libelle },
        contentAlignment = Alignment.Center,
    ) {
        Titre(signe, taille = Tokens.TaillesTexte.titreCarte)
    }
}

/** Écran 10 : le paywall (brief, section 4). Bouton fermer et "Restaurer" visibles tout de suite. */
@Composable
fun EcranPaywall(etat: EtatApp, fermer: () -> Unit, acheter: (annuel: Boolean) -> Unit, restaurer: () -> Unit) {
    var annuel by remember { mutableStateOf(true) }
    val prixAnnuel = t(R.string.prix_annuel)
    val prixHebdo = t(R.string.prix_hebdomadaire)
    EcranKova(
        espacement = Tokens.Espacements.e13,
        bas = {
            BoutonPrincipal(
                t(if (annuel) R.string.paywall_bouton_essai else R.string.commun_continuer),
                pulse = true,
            ) { acheter(annuel) }
            Legende(
                if (annuel) f(R.string.paywall_mentions_annuel_essai, prixAnnuel) else f(R.string.paywall_mentions_hebdo, prixHebdo),
                Modifier.padding(top = Tokens.Espacements.e3.dp),
                centre = true,
            )
            LiensLegaux()
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BoutonFermer(action = fermer)
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.heightIn(min = Tokens.Hauteurs.boutonSecondaire.dp).clic(action = restaurer)
                    .padding(horizontal = Tokens.Espacements.e8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Legende(t(R.string.paywall_restaurer), graisse = Graisse.MOYEN)
            }
        }
        Titre(t(R.string.paywall_titre), Modifier.padding(top = Tokens.Espacements.e8.dp).entree(1))
        Paragraphe(t(R.string.paywall_texte), Modifier.entree(2))
        Column(Modifier.entree(3), verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e13.dp)) {
            LigneCoche(t(R.string.paywall_argument_apps))
            LigneCoche(
                n(R.string.paywall_argument_exercices_un, R.string.paywall_argument_exercices_autre, Exercice.disponibles.size),
            )
            LigneCoche(t(R.string.paywall_argument_suivi))
        }
        Column(
            Modifier.padding(top = Tokens.Espacements.e3.dp).entree(4),
            verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e8.dp),
        ) {
            CarteFormule(
                annuel, { annuel = true }, t(R.string.formule_annuel), t(R.string.paywall_annuel_detail),
                prixAnnuel, t(R.string.paywall_annuel_etiquette), accent = true,
            )
            CarteFormule(
                !annuel, { annuel = false }, t(R.string.formule_hebdomadaire), t(R.string.paywall_hebdo_detail),
                prixHebdo, t(R.string.paywall_hebdo_etiquette), accent = false,
            )
        }
    }
}

@Composable
private fun CarteFormule(
    choisie: Boolean, action: () -> Unit, nom: String, detail: String, prix: String, etiquette: String, accent: Boolean,
) {
    CarteOption(choisie, action, margeVerticale = Tokens.Espacements.e13.dp) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e3.dp)) {
            Corps(nom, graisse = Graisse.DEMI_GRAS)
            Legende(detail)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e3.dp)) {
            Titre(prix, taille = Tokens.TaillesTexte.corpsFort)
            Legende(
                etiquette,
                couleur = if (accent) Tokens.Couleurs.accent else Tokens.Couleurs.texteSecondaire,
                graisse = Graisse.DEMI_GRAS,
            )
        }
    }
}

/** Écran 11 : l'offre de rattrapage, en plein fond accent. Affichée une seule fois. */
@Composable
fun EcranOffre(fermer: () -> Unit, prendre: () -> Unit) {
    val prixReduit = t(R.string.prix_offre)
    val prixNormal = t(R.string.prix_annuel)
    val sombre = Tokens.Couleurs.texteSurAccent
    EcranKova(
        fond = Tokens.Couleurs.accent,
        espacement = Tokens.Espacements.e13,
        bas = {
            BoutonPrincipal(t(R.string.offre_bouton), Modifier.entree(10), style = StyleBouton.SOMBRE, action = prendre)
            BoutonSecondaire(t(R.string.offre_refuser), couleur = sombre, action = fermer)
        },
    ) {
        Row {
            Spacer(Modifier.weight(1f))
            BoutonFermer(surAccent = true, action = fermer)
        }
        Corps(
            t(R.string.offre_surtitre),
            Modifier.padding(top = Tokens.Espacements.e55.dp).entree(1),
            couleur = sombre,
            graisse = Graisse.DEMI_GRAS,
        )
        Chiffre(f(R.string.offre_remise, 33), Tokens.TaillesTexte.chiffreGeant, Modifier.pop(2), couleur = sombre)
        Paragraphe(
            f(R.string.offre_texte, prixReduit, prixNormal),
            Modifier.entree(6),
            couleur = sombre,
            taille = Tokens.TaillesTexte.corpsFort,
        )
        Row(
            Modifier.padding(top = Tokens.Espacements.e13.dp).entree(8).fillMaxWidth().carte(fond = Tokens.Couleurs.fond),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e3.dp)) {
                Corps(t(R.string.formule_annuel), graisse = Graisse.DEMI_GRAS)
                Legende(t(R.string.offre_detail))
            }
            Column(horizontalAlignment = Alignment.End) {
                Titre(prixReduit, taille = Tokens.TaillesTexte.corpsFort)
                Legende(prixNormal, barre = true)
            }
        }
        Legende(f(R.string.offre_mentions, prixReduit, prixNormal), couleur = sombre)
    }
}

/** Liens vers les conditions d'utilisation et la politique de confidentialité. */
@Composable
fun LiensLegaux() {
    val ouvrir = ouvreurDeLiens()
    Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Espacements.e21.dp)) {
        listOf(R.string.legal_conditions to Liens.CONDITIONS, R.string.legal_confidentialite to Liens.CONFIDENTIALITE)
            .filter { it.second.isNotEmpty() }
            .forEach { (titre, adresse) ->
                Box(
                    Modifier.heightIn(min = Tokens.Hauteurs.boutonSecondaire.dp).clic { ouvrir(adresse) },
                    contentAlignment = Alignment.Center,
                ) {
                    Legende(t(titre), souligne = true)
                }
            }
    }
}
