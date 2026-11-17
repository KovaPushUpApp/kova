package com.getkova.app.ecrans

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import com.getkova.app.EtatApp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Liens externes de l'app. À renseigner avant la mise en ligne
 * (voir appstore/A_FAIRE.md). Un lien vide n'est pas affiché.
 */
object Liens {
    const val CONDITIONS = "https://kova-site-theta.vercel.app/conditions.html"
    const val CONFIDENTIALITE = "https://kova-site-theta.vercel.app/confidentialite.html"
    const val CONTACT = "supportkovaapp@gmail.com"
    const val ABONNEMENTS = "https://play.google.com/store/account/subscriptions"
}

@Composable
fun ouvreurDeLiens(): (String) -> Unit {
    val ouvreur = LocalUriHandler.current
    return { adresse -> runCatching { ouvreur.openUri(adresse) } }
}

/** "18:42", dans le format du téléphone. */
fun heure(instant: Long, etat: EtatApp): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(Instant.ofEpochMilli(instant).atZone(etat.zone))

/** "14 nov. 2026, 18:42". */
fun dateEtHeure(instant: Long, etat: EtatApp): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .format(Instant.ofEpochMilli(instant).atZone(etat.zone))

/** "18:00" pour une heure de plage donnée en minutes depuis minuit. */
fun heureDePlage(minutes: Int): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(java.time.LocalTime.of(minutes / 60 % 24, minutes % 60))

/** L'heure qu'il est, rafraîchie chaque seconde, pour les comptes à rebours. */
@Composable
fun horloge(): Long {
    var maintenant by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            maintenant = System.currentTimeMillis()
        }
    }
    return maintenant
}

/** Ouvre la fiche de Kova dans les réglages du téléphone. */
fun ouvrirFicheDeLApp(contexte: Context) {
    val intention = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", contexte.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { contexte.startActivity(intention) }
}

/** Retours haptiques : léger à chaque répétition, fort à la validation de la série. */
class Haptique(contexte: Context) {
    private val vibreur: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (contexte.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        contexte.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun repetition() = vibrer(DUREE_LEGERE)

    fun serieValidee() = vibrer(DUREE_FORTE)

    private fun vibrer(duree: Long) {
        runCatching { vibreur?.vibrate(VibrationEffect.createOneShot(duree, VibrationEffect.DEFAULT_AMPLITUDE)) }
    }

    private companion object {
        const val DUREE_LEGERE = 25L
        const val DUREE_FORTE = 180L
    }
}

@Composable
fun rememberHaptique(): Haptique {
    val contexte = LocalContext.current
    return remember { Haptique(contexte) }
}

/** Option "Compter à voix haute" : annonce le numéro de chaque répétition. */
class Voix(contexte: Context) {
    private var prete = false
    private val synthese: TextToSpeech = TextToSpeech(contexte.applicationContext) { statut ->
        prete = statut == TextToSpeech.SUCCESS
    }

    fun dire(nombre: Int) {
        if (!prete) return
        synthese.language = Locale.getDefault()
        synthese.speak(nombre.toString(), TextToSpeech.QUEUE_FLUSH, null, "repetition")
    }

    fun fermer() = synthese.shutdown()
}

@Composable
fun rememberVoix(): Voix {
    val contexte = LocalContext.current
    val voix = remember { Voix(contexte) }
    DisposableEffect(Unit) { onDispose { voix.fermer() } }
    return voix
}
