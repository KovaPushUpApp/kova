package com.getkova.app.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.getkova.app.R

private val formeGrande = RoundedCornerShape(Tokens.Arrondis.grand.dp)
private val formeMoyenne = RoundedCornerShape(Tokens.Arrondis.moyen.dp)

/** Clic sans effet d'onde, comme dans les maquettes. */
@Composable
fun Modifier.clic(actif: Boolean = true, role: Role? = Role.Button, action: () -> Unit): Modifier = clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    enabled = actif,
    role = role,
    onClick = action,
)

/** Gabarit d'écran : contenu défilant, marges des tokens, boutons fixés en bas. */
@Composable
fun EcranKova(
    fond: Color = Tokens.Couleurs.fond,
    espacement: Float = Tokens.Espacements.e21,
    bas: (@Composable ColumnScope.() -> Unit)? = null,
    contenu: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(fond)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = Tokens.Ecran.margeCotes.dp),
    ) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = Tokens.Espacements.e8.dp, bottom = Tokens.Espacements.e21.dp),
            verticalArrangement = Arrangement.spacedBy(espacement.dp),
            content = contenu,
        )
        if (bas != null) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = Tokens.Espacements.e8.dp),
                verticalArrangement = Arrangement.spacedBy(Tokens.Espacements.e5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = bas,
            )
        }
    }
}

enum class StyleBouton { ACCENT, SOMBRE, CONTOUR, BORDURE }

/** Bouton principal, hauteur 55. */
@Composable
fun BoutonPrincipal(
    titre: String,
    modifier: Modifier = Modifier,
    style: StyleBouton = StyleBouton.ACCENT,
    pulse: Boolean = false,
    actif: Boolean = true,
    action: () -> Unit,
) {
    val couleurTexte = when (style) {
        StyleBouton.ACCENT -> Tokens.Couleurs.texteSurAccent
        StyleBouton.SOMBRE, StyleBouton.BORDURE -> Tokens.Couleurs.texte
        StyleBouton.CONTOUR -> Tokens.Couleurs.accent
    }
    val couleurFond = when (style) {
        StyleBouton.ACCENT -> Tokens.Couleurs.accent
        StyleBouton.SOMBRE -> Tokens.Couleurs.fond
        StyleBouton.CONTOUR, StyleBouton.BORDURE -> Color.Transparent
    }
    val bordure = when (style) {
        StyleBouton.CONTOUR -> Modifier.border(Tokens.Traits.normal.dp, Tokens.Couleurs.accent, formeGrande)
        StyleBouton.BORDURE -> Modifier.border(Tokens.Traits.fin.dp, Tokens.Couleurs.ligne, formeGrande)
        else -> Modifier
    }
    Box(
        modifier
            .then(if (pulse) Modifier.pulsation() else Modifier)
            .fillMaxWidth()
            .heightIn(min = Tokens.Hauteurs.boutonPrincipal.dp)
            .alpha(if (actif) 1f else Tokens.Opacites.scan)
            .clip(formeGrande)
            .background(couleurFond)
            .then(bordure)
            .clic(actif = actif, action = action)
            .padding(horizontal = Tokens.Espacements.e13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = titre,
            color = couleurTexte,
            textAlign = TextAlign.Center,
            style = if (style == StyleBouton.BORDURE) {
                TextStyle(fontFamily = Polices.texte, fontWeight = FontWeight.SemiBold, fontSize = Tokens.TaillesTexte.corps.sp)
            } else {
                TextStyle(fontFamily = Polices.titres, fontWeight = FontWeight.Bold, fontSize = Tokens.TaillesTexte.corps.sp)
            },
        )
    }
}

/** Bouton secondaire : texte seul, hauteur 44. */
@Composable
fun BoutonSecondaire(
    titre: String,
    modifier: Modifier = Modifier,
    couleur: Color = Tokens.Couleurs.texteSecondaire,
    action: () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = Tokens.Hauteurs.boutonSecondaire.dp)
            .clic(action = action),
        contentAlignment = Alignment.Center,
    ) {
        Corps(titre, couleur = couleur, graisse = Graisse.MOYEN, centre = true)
    }
}

/** Petit bouton carré avec pictogramme, 44 x 44. */
@Composable
fun BoutonIcone(icone: Icone, libelle: String, surAccent: Boolean = false, action: () -> Unit) {
    Box(
        Modifier
            .size(Tokens.Hauteurs.boutonIcone.dp)
            .clip(formeMoyenne)
            .background(
                if (surAccent) Tokens.Couleurs.texteSurAccent.copy(alpha = Tokens.Opacites.voileSurAccent)
                else Tokens.Couleurs.carte,
            )
            .clic(action = action)
            .semantics { contentDescription = libelle },
        contentAlignment = Alignment.Center,
    ) {
        Pictogramme(icone, couleur = if (surAccent) Tokens.Couleurs.texteSurAccent else Tokens.Couleurs.texte)
    }
}

@Composable
fun BoutonRetour(action: () -> Unit) = BoutonIcone(Icone.CHEVRON_GAUCHE, t(R.string.commun_retour), action = action)

@Composable
fun BoutonFermer(libelle: String = t(R.string.commun_fermer), surAccent: Boolean = false, action: () -> Unit) =
    BoutonIcone(Icone.CROIX, libelle, surAccent, action)

/** Fond de carte arrondi. */
fun Modifier.carte(
    marge: Float = Tokens.Espacements.e21,
    arrondi: Float = Tokens.Arrondis.grand,
    fond: Color = Tokens.Couleurs.carte,
): Modifier = clip(RoundedCornerShape(arrondi.dp)).background(fond).padding(marge.dp)

/** Contour en pointillés arrondi. */
fun Modifier.pointilles(arrondi: Float = Tokens.Arrondis.grand): Modifier = drawBehind {
    val tiret = Tokens.Espacements.e5.dp.toPx()
    drawRoundRect(
        color = Tokens.Couleurs.pointille,
        cornerRadius = CornerRadius(arrondi.dp.toPx()),
        style = Stroke(Tokens.Traits.fin.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(tiret, tiret))),
    )
}

/** En-tête d'onboarding : retour et barre d'avancement. */
@Composable
fun EnTeteEtapes(etape: Int, total: Int, retour: () -> Unit) {
    val description = f(R.string.a11y_etape, etape, total)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Espacements.e13.dp)) {
        BoutonRetour(retour)
        Box(
            Modifier
                .weight(1f)
                .height(Tokens.Espacements.e5.dp)
                .clip(CircleShape)
                .background(Tokens.Couleurs.ligne)
                .semantics { contentDescription = description },
        ) {
            Box(
                Modifier
                    .fillMaxWidth(etape.toFloat() / total.coerceAtLeast(1))
                    .height(Tokens.Espacements.e5.dp)
                    .remplissageDepuisLaGauche()
                    .clip(CircleShape)
                    .background(Tokens.Couleurs.accent),
            )
        }
    }
}

/** Ligne à choisir (question, exercice, formule) : bordure accent quand elle est choisie. */
@Composable
fun CarteOption(
    choisie: Boolean,
    action: () -> Unit,
    margeVerticale: Dp = 0.dp,
    contenu: @Composable RowScope.() -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Tokens.Hauteurs.option.dp)
            .clip(formeGrande)
            .background(if (choisie) Tokens.Couleurs.carteSelectionnee else Tokens.Couleurs.carte)
            .border(
                Tokens.Traits.normal.dp,
                if (choisie) Tokens.Couleurs.accent else Tokens.Couleurs.ligne,
                formeGrande,
            )
            .clic(action = action)
            .semantics { selected = choisie }
            .padding(horizontal = Tokens.Espacements.e21.dp, vertical = margeVerticale),
        verticalAlignment = Alignment.CenterVertically,
        content = contenu,
    )
}

/** Interrupteur de la maquette : 55 x 34, pastille de 28. */
@Composable
fun Interrupteur(actif: Boolean, libelle: String, action: () -> Unit) {
    val etat = t(if (actif) R.string.a11y_active else R.string.a11y_desactive)
    val duree = Mouvement.ms(Tokens.Animations.dureeStandard) / 2
    val decalage by animateDpAsState(
        if (actif) Tokens.Composants.Interrupteur.decalageActif.dp else 0.dp, tween(duree), label = "pastille",
    )
    val fond by animateColorAsState(
        if (actif) Tokens.Couleurs.accent else Tokens.Couleurs.ligne, tween(duree), label = "fond",
    )
    Box(
        Modifier
            .heightIn(min = Tokens.Hauteurs.boutonIcone.dp)
            .clic(role = Role.Switch, action = action)
            .semantics {
                contentDescription = libelle
                stateDescription = etat
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(Tokens.Composants.Interrupteur.largeur.dp, Tokens.Composants.Interrupteur.hauteur.dp)
                .clip(formeMoyenne)
                .background(fond)
                .padding(Tokens.Espacements.e3.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .offset(x = decalage)
                    .size(Tokens.Composants.Interrupteur.pastille.dp)
                    .clip(CircleShape)
                    .background(if (actif) Tokens.Couleurs.texteSurAccent else Tokens.Couleurs.texteSecondaire),
            )
        }
    }
}

/** Ligne "coche + texte" des listes d'arguments. */
@Composable
fun LigneCoche(texte: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Espacements.e13.dp)) {
        Pictogramme(Icone.COCHE, couleur = Tokens.Couleurs.accent)
        Corps(texte, Modifier.weight(1f))
    }
}

/** Icône dans un carré arrondi, avec l'anneau qui s'élargit (autorisations, verrou). */
@Composable
fun IconeAnneau(icone: Icone, avecAnneau: Boolean = true) {
    Box(
        Modifier.fillMaxWidth().padding(top = Tokens.Espacements.e55.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (avecAnneau) {
            Anneau(Modifier.size(Tokens.Composants.Logo.taille.dp))
        }
        Box(
            Modifier
                .pop()
                .size(Tokens.Composants.Logo.taille.dp)
                .clip(RoundedCornerShape(Tokens.Arrondis.tresGrand.dp))
                .background(Tokens.Couleurs.carte),
            contentAlignment = Alignment.Center,
        ) {
            Pictogramme(icone, taille = Tokens.Espacements.e34, couleur = Tokens.Couleurs.accent)
        }
    }
}

/** Barre de progression à segments de la séance. */
@Composable
fun BarreSegments(faits: Int, total: Int) {
    val description = f(R.string.a11y_progression_valeur, faits, total)
    Row(
        Modifier.fillMaxWidth().semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(Tokens.Composants.BarreProgression.ecart.dp),
    ) {
        repeat(total.coerceAtLeast(1)) { rang ->
            val couleur by animateColorAsState(
                if (rang < faits) Tokens.Couleurs.accent else Tokens.Couleurs.ligne, label = "segment",
            )
            Box(
                Modifier
                    .weight(1f)
                    .height(Tokens.Composants.BarreProgression.hauteur.dp)
                    .clip(CircleShape)
                    .background(couleur),
            )
        }
    }
}

/** Groupe de lignes de réglages : carte arrondie, lignes séparées par un filet. */
@Composable
fun GroupeReglages(contenu: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(formeGrande).background(Tokens.Couleurs.carte), content = contenu)
}

/** Ligne de réglage : libellé à gauche, valeur ou interrupteur à droite. */
@Composable
fun LigneReglage(libelle: String, action: (() -> Unit)? = null, droite: @Composable RowScope.() -> Unit) {
    Column(if (action != null) Modifier.clic(action = action) else Modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = Tokens.Hauteurs.ligneReglage.dp)
                .padding(horizontal = Tokens.Espacements.e13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Espacements.e13.dp),
        ) {
            Corps(libelle, Modifier.weight(1f))
            droite()
        }
        Box(Modifier.fillMaxWidth().height(Tokens.Traits.fin.dp).background(Tokens.Couleurs.ligneFine))
    }
}

/** Ligne de réglage qui ouvre un autre écran : valeur puis chevron. */
@Composable
fun LigneReglageLien(libelle: String, valeur: String = "", action: () -> Unit) {
    LigneReglage(libelle, action) {
        Legende(valeur)
        Spacer(Modifier.width(Tokens.Espacements.e5.dp))
        Pictogramme(Icone.CHEVRON_DROIT, taille = Tokens.Espacements.e13, couleur = Tokens.Couleurs.texteSecondaire)
    }
}

/** Bouton en pointillés ("Ajouter une plage"). */
@Composable
fun BoutonPointille(titre: String, action: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Tokens.Hauteurs.boutonPrincipal.dp)
            .pointilles()
            .clic(action = action),
        contentAlignment = Alignment.Center,
    ) {
        Corps(titre, graisse = Graisse.DEMI_GRAS)
    }
}

/** Boîte centrée sur toute la largeur, pour les titres et textes centrés. */
@Composable
fun Centre(modifier: Modifier = Modifier, contenu: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center, content = contenu)
}
