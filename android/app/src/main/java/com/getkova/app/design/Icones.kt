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

/** Silhouette en pompe des maquettes. Grille de 150 x 70. */
@Composable
fun FigurePompe(modifier: Modifier = Modifier, animee: Boolean = true) {
    val descente = if (animee && !reduireLesAnimations()) {
        val transition = rememberInfiniteTransition(label = "pompe")
        val valeur by transition.animateFloat(
            initialValue = 0f,
            targetValue = 12f,
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
        val trait = Stroke(Tokens.Traits.illustration, cap = StrokeCap.Round, join = StrokeJoin.Round)
        scale(size.width / 150f, size.height / 70f, pivot = Offset.Zero) {
            translate(top = descente) {
                drawCircle(Tokens.Couleurs.accent, 8f, Offset(126f, 18f), style = trait)
                drawPath(Path().apply { ligne(116f to 24f, 60f to 40f, 14f to 54f) }, Tokens.Couleurs.accent, style = trait)
            }
            drawPath(
                Path().apply {
                    ligne(104f to 28f, 98f to 56f)
                    ligne(60f to 40f, 52f to 58f)
                },
                Tokens.Couleurs.accent,
                style = trait,
            )
            drawPath(Path().apply { ligne(4f to 62f, 146f to 62f) }, Tokens.Couleurs.pointille, style = trait)
        }
    }
}
