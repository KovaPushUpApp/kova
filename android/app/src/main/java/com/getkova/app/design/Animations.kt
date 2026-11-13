package com.getkova.app.design

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Courbes et durées communes. Toutes les valeurs viennent des tokens. */
object Mouvement {
    /** Courbe "ease" du web : cubic-bezier(0.25, 0.1, 0.25, 1). */
    val courbe = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

    fun ms(secondes: Float): Int = (secondes * 1000).toInt()

    /** Décalage d'entrée d'un bloc selon son rang dans la cascade. */
    fun delai(rang: Int): Long = (rang * Tokens.Animations.Entree.cascade[0] * 1000).toLong()
}

/** Vrai si l'utilisateur a coupé les animations dans les réglages du téléphone. */
@Composable
fun reduireLesAnimations(): Boolean {
    val contexte = LocalContext.current
    return remember {
        Settings.Global.getFloat(contexte.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** Entrée d'un bloc : fondu et montée de 18. `rang` place le bloc dans la cascade. */
@Composable
fun Modifier.entree(rang: Int = 0): Modifier {
    val reduire = reduireLesAnimations()
    val avancement = remember { Animatable(if (reduire) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduire) {
            delay(Mouvement.delai(rang))
            avancement.animateTo(1f, tween(Mouvement.ms(Tokens.Animations.dureeStandard), easing = Mouvement.courbe))
        }
    }
    return graphicsLayer {
        alpha = avancement.value
        translationY = (1f - avancement.value) * Tokens.Animations.Entree.translationY[0].dp.toPx()
    }
}

/** Apparition "pop" : échelle 0,6 vers 1,06 puis 1. */
@Composable
fun Modifier.pop(rang: Int = 0): Modifier {
    val reduire = reduireLesAnimations()
    val echelles = Tokens.Animations.Pop.echelle
    val echelle = remember { Animatable(if (reduire) echelles[2] else echelles[0]) }
    val opacite = remember { Animatable(if (reduire) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduire) {
            delay(Mouvement.delai(rang))
            val duree = Mouvement.ms(Tokens.Animations.Pop.duree)
            val montee = (duree * Tokens.Animations.Pop.pic).toInt()
            opacite.animateTo(1f, tween(montee / 2))
            echelle.animateTo(echelles[1], tween(montee, easing = Mouvement.courbe))
            echelle.animateTo(echelles[2], tween(duree - montee, easing = Mouvement.courbe))
        }
    }
    return graphicsLayer {
        scaleX = echelle.value
        scaleY = echelle.value
        alpha = opacite.value
    }
}

/** Pulsation lente et continue des boutons principaux. */
@Composable
fun Modifier.pulsation(): Modifier {
    if (reduireLesAnimations()) return this
    val transition = rememberInfiniteTransition(label = "pulsation")
    val echelle by transition.animateFloat(
        initialValue = Tokens.Animations.Pulsation.echelle[0],
        targetValue = Tokens.Animations.Pulsation.echelle[1],
        animationSpec = infiniteRepeatable(
            tween(Mouvement.ms(Tokens.Animations.Pulsation.duree) / 2, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "echelle",
    )
    return graphicsLayer {
        scaleX = echelle
        scaleY = echelle
    }
}

/** Croissance d'une barre : depuis le bas (stats) ou depuis la gauche (jauges). */
@Composable
private fun Modifier.croissance(depuisLeBas: Boolean, duree: Float, rang: Int): Modifier {
    val reduire = reduireLesAnimations()
    val avancement = remember { Animatable(if (reduire) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduire) {
            delay(Mouvement.delai(rang))
            avancement.animateTo(1f, tween(Mouvement.ms(duree), easing = Mouvement.courbe))
        }
    }
    return graphicsLayer {
        if (depuisLeBas) {
            scaleY = avancement.value
            transformOrigin = TransformOrigin(0.5f, 1f)
        } else {
            scaleX = avancement.value
            transformOrigin = TransformOrigin(0f, 0.5f)
        }
    }
}

@Composable
fun Modifier.croissanceDepuisLeBas(rang: Int = 0): Modifier =
    croissance(true, Tokens.Animations.CroissanceBarre.duree, rang)

@Composable
fun Modifier.remplissageDepuisLaGauche(rang: Int = 0): Modifier =
    croissance(false, Tokens.Animations.Remplissage.duree, rang)

/** Anneau qui s'élargit en boucle autour d'une icône. */
@Composable
fun Anneau(modifier: Modifier = Modifier) {
    if (reduireLesAnimations()) return
    val transition = rememberInfiniteTransition(label = "anneau")
    val avancement by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(Mouvement.ms(Tokens.Animations.Anneau.duree), easing = LinearOutSlowInEasing),
            RepeatMode.Restart,
        ),
        label = "avancement",
    )
    val echelles = Tokens.Animations.Anneau.echelle
    val opacites = Tokens.Animations.Anneau.opacite
    Canvas(modifier) {
        val echelle = echelles[0] + (echelles[1] - echelles[0]) * avancement
        drawCircle(
            color = Tokens.Couleurs.accent,
            radius = size.minDimension / 2 * echelle,
            alpha = opacites[0] + (opacites[1] - opacites[0]) * avancement,
            style = Stroke(Tokens.Traits.normal.dp.toPx()),
        )
    }
}

/** Position de la ligne de scan de la vue caméra, entre 0 (haut) et 1 (bas). */
@Composable
fun positionDuScan(): Float? {
    if (reduireLesAnimations()) return null
    val transition = rememberInfiniteTransition(label = "scan")
    val position by transition.animateFloat(
        initialValue = Tokens.Animations.ScanCamera.debut,
        targetValue = Tokens.Animations.ScanCamera.fin,
        animationSpec = infiniteRepeatable(
            tween(Mouvement.ms(Tokens.Animations.ScanCamera.duree) / 2, easing = LinearEasing),
            RepeatMode.Reverse,
        ),
        label = "position",
    )
    return position
}
