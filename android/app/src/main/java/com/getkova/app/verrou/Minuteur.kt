package com.getkova.app.verrou

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.getkova.app.MainActivity
import com.getkova.app.R
import com.getkova.app.design.Tokens
import com.getkova.app.jeu.EtatJeu
import com.getkova.app.jeu.finDOuverture
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * La notification "temps restant" : pendant qu'une fenêtre est ouverte, elle
 * affiche un compte à rebours jusqu'au reverrouillage, visible d'un coup d'œil
 * en tirant le volet. Android fait défiler le compte à rebours lui-même et
 * retire la notification à la fin : rien ne tourne en arrière-plan pour ça.
 * La notification est créée sur le téléphone, elle ne passe par aucun serveur.
 */
object Minuteur {
    private const val CANAL = "fenetre"
    private const val IDENTIFIANT = 1

    /** Vrai si Kova a le droit d'afficher des notifications. */
    fun autorise(contexte: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(contexte, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Affiche, met à jour ou retire la notification selon l'état du jeu. */
    fun mettreAJour(contexte: Context, etat: EtatJeu, maintenant: Long, zone: ZoneId) {
        val gestionnaire = contexte.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val fin = etat.finDOuverture(maintenant, zone)
        if (fin == null || fin <= maintenant) {
            gestionnaire.cancel(IDENTIFIANT)
            return
        }
        if (!autorise(contexte)) return

        // Canal discret : pas de son, pas de vibration.
        gestionnaire.createNotificationChannel(
            NotificationChannel(CANAL, contexte.getString(R.string.notif_canal), NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
            },
        )
        val ouvrirKova = PendingIntent.getActivity(
            contexte, 0,
            Intent(contexte, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val heure = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(Instant.ofEpochMilli(fin).atZone(zone))
        val notification = Notification.Builder(contexte, CANAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(Tokens.Couleurs.accent.toArgb())
            .setContentTitle(contexte.getString(R.string.notif_titre))
            .setContentText(contexte.getString(R.string.notif_texte, heure))
            // Le compte à rebours est affiché et mis à jour par Android.
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(fin)
            .setShowWhen(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setTimeoutAfter(fin - maintenant)
            .setCategory(Notification.CATEGORY_STATUS)
            .setContentIntent(ouvrirKova)
            .build()
        gestionnaire.notify(IDENTIFIANT, notification)
    }
}
