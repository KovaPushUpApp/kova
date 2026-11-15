package com.getkova.app.design

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.getkova.app.R

// Tracés repris des maquettes (SVG). Les coordonnées sont celles des fichiers
// .dc.html : elles décrivent la forme, pas une valeur de design.

enum class Icone {
    CHEVRON_GAUCHE, CHEVRON_DROIT, CROIX, COCHE, CADENAS, CAMERA, ECLAIR, FLECHE,
    MAISON, STATS, CALENDRIER, REGLAGES,
}

private fun Path.ligne(vararg points: Pair<Float, Float>) {
    points.forEachIndexed { rang, (x, y) -> if (rang == 0) moveTo(x, y) else lineTo(x, y) }
}

private fun Path.rectangleArrondi(x: Float, y: Float, largeur: Float, hauteur: Float, rayon: Float) {
    addRoundRect(RoundRect(Rect(Offset(x, y), Size(largeur, hauteur)), CornerRadius(rayon)))
}

/** Tracé d'un pictogramme sur une grille de 24. */
private fun trace(icone: Icone): Path = Path().apply {
    when (icone) {
        Icone.CHEVRON_GAUCHE -> ligne(15f to 6f, 9f to 12f, 15f to 18f)
        Icone.CHEVRON_DROIT -> ligne(9f to 6f, 15f to 12f, 9f to 18f)
        Icone.CROIX -> {
            ligne(6f to 6f, 18f to 18f)
            ligne(18f to 6f, 6f to 18f)
        }
        Icone.COCHE -> ligne(5f to 12f, 10f to 17f, 19f to 7f)
        Icone.CADENAS -> {
            rectangleArrondi(5f, 11f, 14f, 9f, 2f)
            moveTo(8f, 11f)
            lineTo(8f, 8f)
            arcTo(Rect(8f, 4f, 16f, 12f), 180f, 180f, false)
            lineTo(16f, 11f)
        }
        Icone.CAMERA -> {
            rectangleArrondi(3f, 7f, 18f, 13f, 3f)
            addOval(Rect(8.5f, 10f, 15.5f, 17f))
            ligne(9f to 7f, 10.5f to 4f, 13.5f to 4f, 15f to 7f)
        }
        Icone.ECLAIR -> {
            ligne(13f to 3f, 5f to 14f, 11f to 14f, 10f to 21f, 18f to 10f, 12f to 10f)
            close()
        }
        Icone.FLECHE -> {
            ligne(5f to 12f, 19f to 12f)
            ligne(13f to 6f, 19f to 12f, 13f to 18f)
        }
        Icone.MAISON -> {
            ligne(4f to 11f, 12f to 4f, 20f to 11f)
            ligne(6f to 10f, 6f to 19f, 18f to 19f, 18f to 10f)
        }
        Icone.STATS -> {
            ligne(5f to 20f, 5f to 10f)
            ligne(12f to 20f, 12f to 4f)
            ligne(19f to 20f, 19f to 13f)
        }
        Icone.CALENDRIER -> {
            rectangleArrondi(4f, 5f, 16f, 15f, 2f)
            ligne(4f to 10f, 20f to 10f)
            ligne(9f to 3f, 9f to 7f)
            ligne(15f to 3f, 15f to 7f)
        }
        Icone.REGLAGES -> {
            addOval(Rect(9f, 9f, 15f, 15f))
            ligne(12f to 3f, 12f to 6f)
            ligne(12f to 18f, 12f to 21f)
            ligne(3f to 12f, 6f to 12f)
            ligne(18f to 12f, 21f to 12f)
        }
    }
}

/** Pictogramme au trait. */
@Composable
fun Pictogramme(
    icone: Icone,
    modifier: Modifier = Modifier,
    taille: Float = Tokens.Espacements.e21,
    couleur: Color = Tokens.Couleurs.texte,
) {
    Canvas(modifier.size(taille.dp)) {
        val echelle = size.width / 24f
        scale(echelle, echelle, pivot = Offset.Zero) {
            drawPath(
                trace(icone),
                couleur,
                style = Stroke(Tokens.Traits.normal, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

/** Symbole de Kova : cadenas ouvert avec un chevron vers le haut. Grille de 104. */
@Composable
fun SymboleKova(taille: Float, modifier: Modifier = Modifier, couleur: Color = Tokens.Couleurs.texteSurAccent) {
    Canvas(modifier.size(taille.dp)) {
        val echelle = size.width / 104f
        scale(echelle, echelle, pivot = Offset.Zero) {
            val corps = Path().apply {
                rectangleArrondi(24f, 48f, 56f, 40f, 10f)
                moveTo(36f, 48f)
                lineTo(36f, 34f)
                arcTo(Rect(36f, 18f, 68f, 50f), 180f, 180f, false)
            }
            drawPath(corps, couleur, style = Stroke(10f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            val chevron = Path().apply { ligne(42f to 72f, 52f to 62f, 62f to 72f) }
            drawPath(chevron, couleur, style = Stroke(7f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Logo : le symbole sur un carré arrondi vert citron. */
@Composable
fun LogoKova(modifier: Modifier = Modifier) {
    val description = t(R.string.a11y_logo)
    Box(
        modifier
            .size(Tokens.Composants.Logo.taille.dp)
            .clip(RoundedCornerShape(Tokens.Composants.Logo.arrondi.dp))
            .background(Tokens.Couleurs.accent)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        SymboleKova(Tokens.Composants.Logo.taille * Tokens.Composants.Logo.proportionSymbole)
    }
}

/**
 * Silhouette qui fait une pompe, vue de profil. Elle remplace le bonhomme en
 * bâtons des maquettes : le corps reste droit et pivote sur les pieds, la main
 * reste au sol et le coude plie vraiment, comme dans une vraie pompe.
 * Dessinée sur une grille de 200 x 100.
 */
@Composable
fun FigurePompe(modifier: Modifier = Modifier, animee: Boolean = true) {
    val descente = if (animee && !reduireLesAnimations()) {
        val transition = rememberInfiniteTransition(label = "pompe")
        val valeur by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(Mouvement.ms(Tokens.Animations.Pompe.duree) / 2, easing = FastOutSlowInEasing),
                RepeatMode.Reverse,
            ),
            label = "descente",
        )
        valeur
    } else {
        0f
    }
    Canvas(modifier.size(Tokens.Composants.Illustration.largeur.dp, Tokens.Composants.Illustration.hauteur.dp)) {
        val echelle = minOf(size.width / 200f, size.height / 100f)
        translate((size.width - 200f * echelle) / 2, (size.height - 100f * echelle) / 2) {
            scale(echelle, echelle, pivot = Offset.Zero) { silhouette(descente, avecSol = true) }
        }
    }
}

/** La silhouette sur sa grille de 200 x 100, pieds à gauche, tête à droite, sol à 88. */
private fun DrawScope.silhouette(descente: Float, avecSol: Boolean) {
    val sol = 88f
    val main = Offset(152f, sol - 3f)
    val cheville = Offset(26f, sol - 7f)
    // L'épaule descend vers la main : haute bras tendus, basse en bas de la pompe.
    val epaule = Offset(150f, 43f + 27f * descente)
    val hanche = Offset(
        cheville.x + (epaule.x - cheville.x) * 0.52f,
        cheville.y + (epaule.y - cheville.y) * 0.52f,
    )
    // Le coude : deux segments de même longueur entre l'épaule et la main, pliés vers l'arrière.
    val segment = 23f
    val ecart = (main - epaule).getDistance().coerceIn(1f, segment * 2)
    val milieu = Offset((epaule.x + main.x) / 2, (epaule.y + main.y) / 2)
    val saillie = kotlin.math.sqrt((segment * segment - ecart * ecart / 4).coerceAtLeast(0f))
    val direction = (main - epaule) / ecart
    // Perpendiculaire au bras, du côté des pieds.
    val coude = Offset(milieu.x - direction.y * saillie, milieu.y + direction.x * saillie)
    // La tête prolonge le corps, un peu relevée.
    val axe = (epaule - cheville) / (epaule - cheville).getDistance()
    val tete = Offset(epaule.x + axe.x * 17f, epaule.y + axe.y * 17f - 5f)

    val corps = Stroke(11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val bras = Stroke(8f, cap = StrokeCap.Round, join = StrokeJoin.Round)

    if (avecSol) {
        // Le sol, puis une ombre qui s'élargit quand le corps descend.
        drawLine(Tokens.Couleurs.pointille, Offset(8f, sol), Offset(192f, sol), 3f, StrokeCap.Round)
        drawOval(
            Tokens.Couleurs.fond.copy(alpha = 0.35f + 0.25f * descente),
            topLeft = Offset(40f - 6f * descente, sol - 3f),
            size = Size(110f + 12f * descente, 6f),
        )
    }
    // Jambes et tronc d'un seul trait, pied posé sur la pointe.
    drawPath(Path().apply { ligne(cheville.x to cheville.y, hanche.x to hanche.y, epaule.x to epaule.y) }, Tokens.Couleurs.accent, style = corps)
    drawLine(Tokens.Couleurs.accent, cheville, Offset(cheville.x - 8f, sol - 2f), 8f, StrokeCap.Round)
    // Le bras passe devant le corps, dans une teinte plus claire pour le détacher.
    drawPath(Path().apply { ligne(epaule.x to epaule.y, coude.x to coude.y, main.x to main.y) }, Tokens.Couleurs.texte, style = bras)
    drawLine(Tokens.Couleurs.texte, main, Offset(main.x + 9f, sol - 2f), 7f, StrokeCap.Round)
    drawCircle(Tokens.Couleurs.accent, 11f, tete)
}

/** Part de la largeur de [IllustrationPlacement] sous laquelle centrer la légende de distance. */
const val PLACEMENT_PART_DISTANCE = 2f * 87f / 300f

/**
 * Schéma de mise en place, vu de côté : le téléphone debout au sol, calé contre
 * un objet, et la personne en position de pompe, la tête vers le téléphone.
 * Le voile montre ce que voit la caméra, la flèche la distance entre les deux.
 * Dessiné sur une grille de 300 x 120.
 */
@Composable
fun IllustrationPlacement(modifier: Modifier = Modifier) {
    Canvas(modifier.aspectRatio(300f / 120f)) {
        val echelle = size.width / 300f
        scale(echelle, echelle, pivot = Offset.Zero) {
            val sol = 98f
            val accent = Tokens.Couleurs.accent
            val discret = Tokens.Couleurs.pointille
            // Ce que voit la caméra : un cône qui part du haut du téléphone et couvre tout le corps.
            drawPath(
                Path().apply {
                    ligne(41f to 52f, 296f to 4f, 296f to sol, 62f to sol)
                    close()
                },
                accent.copy(alpha = Tokens.Opacites.voileSurAccent),
            )
            drawLine(discret, Offset(6f, sol), Offset(296f, sol), 3f, StrokeCap.Round)
            // L'objet qui cale le téléphone.
            drawRoundRect(discret, Offset(8f, 58f), Size(20f, 40f - 1.5f), CornerRadius(4f))
            // Le téléphone, bien droit : debout, à 90°.
            drawRoundRect(Tokens.Couleurs.carte, Offset(30f, 44f), Size(12f, sol - 44f - 1.5f), CornerRadius(4f))
            drawRoundRect(accent, Offset(30f, 44f), Size(12f, sol - 44f - 1.5f), CornerRadius(4f), style = Stroke(3f))
            drawCircle(accent, 2.5f, Offset(41f, 52f))
            // La personne, retournée pour faire face au téléphone.
            translate(116f, sol - 88f * 0.9f) {
                scale(-0.9f, 0.9f, pivot = Offset.Zero) {
                    translate(-200f, 0f) { silhouette(0f, avecSol = false) }
                }
            }
            // La distance entre le téléphone et la tête.
            val cote = sol + 13f
            val trait = Stroke(2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawLine(discret, Offset(46f, cote), Offset(128f, cote), 2f, StrokeCap.Round)
            drawPath(Path().apply { ligne(52f to cote - 5f, 46f to cote, 52f to cote + 5f) }, discret, style = trait)
            drawPath(Path().apply { ligne(122f to cote - 5f, 128f to cote, 122f to cote + 5f) }, discret, style = trait)
        }
    }
}
