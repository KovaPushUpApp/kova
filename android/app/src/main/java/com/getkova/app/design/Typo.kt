package com.getkova.app.design

import androidx.annotation.StringRes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getkova.app.R

/** Space Grotesk 700 pour les titres et les chiffres, Inter pour le reste. */
object Polices {
    val titres = FontFamily(Font(R.font.space_grotesk_bold, FontWeight.Bold))
    val texte = FontFamily(
        Font(R.font.inter_regular, FontWeight.Normal),
        Font(R.font.inter_medium, FontWeight.Medium),
        Font(R.font.inter_semibold, FontWeight.SemiBold),
    )
}

enum class Graisse(val poids: FontWeight) {
    NORMAL(FontWeight.Normal), MOYEN(FontWeight.Medium), DEMI_GRAS(FontWeight.SemiBold)
}

// --- Textes localisés (ressources générées depuis les fichiers communs) -------

@Composable
fun t(@StringRes id: Int): String = stringResource(id)

@Composable
fun f(@StringRes id: Int, vararg valeurs: Any): String = stringResource(id, *valeurs)

/** Texte accordé en nombre : une ressource pour 1, une autre pour le reste. */
@Composable
fun n(@StringRes un: Int, @StringRes autre: Int, nombre: Int): String =
    stringResource(if (nombre == 1) un else autre, nombre)

// --- Styles de texte ---------------------------------------------------------

/** Titre en Space Grotesk. Par défaut, le titre d'écran de 33. */
@Composable
fun Titre(
    texte: String,
    modifier: Modifier = Modifier,
    taille: Float = Tokens.TaillesTexte.titreEcran,
    couleur: Color = Tokens.Couleurs.texte,
    centre: Boolean = false,
    lignesMax: Int = Int.MAX_VALUE,
) {
    Text(
        text = texte,
        modifier = modifier.semantics { heading() },
        color = couleur,
        maxLines = lignesMax,
        textAlign = if (centre) TextAlign.Center else TextAlign.Start,
        style = TextStyle(
            fontFamily = Polices.titres,
            fontWeight = FontWeight.Bold,
            fontSize = taille.sp,
            lineHeight = (taille * Tokens.Interligne.titre).sp,
        ),
    )
}

/** Très gros chiffres : taille fixe, ils ne suivent pas la taille de police du système. */
@Composable
fun Chiffre(
    texte: String,
    taille: Float,
    modifier: Modifier = Modifier,
    couleur: Color = Tokens.Couleurs.texte,
) {
    Text(
        text = texte,
        modifier = modifier,
        color = couleur,
        style = TextStyle(
            fontFamily = Polices.titres,
            fontWeight = FontWeight.Bold,
            fontSize = taille.fixe(),
            lineHeight = (taille * Tokens.Interligne.chiffre).fixe(),
        ),
    )
}

/** Paragraphe en Inter, interligne du nombre d'or. */
@Composable
fun Paragraphe(
    texte: String,
    modifier: Modifier = Modifier,
    couleur: Color = Tokens.Couleurs.texteSecondaire,
    taille: Float = Tokens.TaillesTexte.corps,
    centre: Boolean = false,
) {
    Text(
        text = texte,
        modifier = modifier,
        color = couleur,
        textAlign = if (centre) TextAlign.Center else TextAlign.Start,
        style = TextStyle(
            fontFamily = Polices.texte,
            fontSize = taille.sp,
            lineHeight = (taille * Tokens.Interligne.paragraphe).sp,
        ),
    )
}

/** Texte courant, 16. */
@Composable
fun Corps(
    texte: String,
    modifier: Modifier = Modifier,
    couleur: Color = Tokens.Couleurs.texte,
    graisse: Graisse = Graisse.NORMAL,
    centre: Boolean = false,
) {
    Text(
        text = texte,
        modifier = modifier,
        color = couleur,
        textAlign = if (centre) TextAlign.Center else TextAlign.Start,
        style = TextStyle(fontFamily = Polices.texte, fontWeight = graisse.poids, fontSize = Tokens.TaillesTexte.corps.sp),
    )
}

/** Légende, 13. */
@Composable
fun Legende(
    texte: String,
    modifier: Modifier = Modifier,
    couleur: Color = Tokens.Couleurs.texteSecondaire,
    graisse: Graisse = Graisse.NORMAL,
    centre: Boolean = false,
    barre: Boolean = false,
    souligne: Boolean = false,
    interlettrage: TextUnit = TextUnit.Unspecified,
    lignesMax: Int = Int.MAX_VALUE,
) {
    Text(
        text = texte,
        modifier = modifier,
        color = couleur,
        maxLines = lignesMax,
        textAlign = if (centre) TextAlign.Center else TextAlign.Start,
        style = TextStyle(
            fontFamily = Polices.texte,
            fontWeight = graisse.poids,
            fontSize = Tokens.TaillesTexte.legende.sp,
            letterSpacing = interlettrage,
            textDecoration = when {
                barre -> TextDecoration.LineThrough
                souligne -> TextDecoration.Underline
                else -> null
            },
        ),
    )
}

/** Taille en sp qui ignore le réglage de taille de police du système. */
@Composable
private fun Float.fixe(): TextUnit = with(LocalDensity.current) { this@fixe.dp.toSp() }
