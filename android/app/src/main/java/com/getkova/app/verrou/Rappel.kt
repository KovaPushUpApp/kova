package com.getkova.app.verrou

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.toArgb
import com.getkova.app.Depot
import com.getkova.app.MainActivity
import com.getkova.app.R
import com.getkova.app.design.Tokens
import com.getkova.app.jeu.finDOuverture
import com.getkova.app.jeu.mettreAJour
import com.getkova.app.jeu.repetitionsDuJour
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Le rappel quotidien : une notification en fin de journée si aucune répétition
 * n'a été faite ce jour-là. Elle est créée sur le téléphone, sans serveur.
 * L'heure n'a pas besoin d'être exacte : Android la regroupe avec d'autres
 * réveils pour ménager la batterie.
 */
object Rappel {
    const val CLE = "rappelQuotidien"
    private const val CANAL = "rappel"
    private const val IDENTIFIANT = 2

    /** Heure du rappel, la même que sur iOS. */
    private const val HEURE = 18

    private fun declencheur(contexte: Context): PendingIntent = PendingIntent.getBroadcast(
        contexte, IDENTIFIANT,
        Intent(contexte, RecepteurRappel::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun actif(contexte: Context): Boolean = Depot.reglages(contexte).getBoolean(CLE, false)

    /** Programme le rappel de chaque jour, ou le retire. */
    fun programmer(contexte: Context, actif: Boolean) {
        val reveils = contexte.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val declencheur = declencheur(contexte)
        reveils.cancel(declencheur)
        if (!actif) {
            (contexte.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(IDENTIFIANT)
            return
        }
        val maintenant = ZonedDateTime.now()
        var prochain = maintenant.with(LocalTime.of(HEURE, 0))
        if (!prochain.isAfter(maintenant)) prochain = prochain.plusDays(1)
        reveils.setInexactRepeating(
            AlarmManager.RTC_WAKEUP, prochain.toInstant().toEpochMilli(), AlarmManager.INTERVAL_DAY, declencheur,
        )
    }

    /** Affiche le rappel, sauf si la journée est déjà gagnée ou si les apps sont ouvertes. */
    fun afficherSiUtile(contexte: Context) {
        if (!actif(contexte) || !Minuteur.autorise(contexte)) return
        val maintenant = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val etat = Depot.lire(contexte).mettreAJour(maintenant)
        if (etat.repetitionsDuJour(maintenant, zone) > 0) return
        if ((etat.finDOuverture(maintenant, zone) ?: 0L) > maintenant) return

        val gestionnaire = contexte.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        gestionnaire.createNotificationChannel(
            NotificationChannel(CANAL, contexte.getString(R.string.reglages_rappel), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val ouvrirKova = PendingIntent.getActivity(
            contexte, 0,
            Intent(contexte, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = Notification.Builder(contexte, CANAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(Tokens.Couleurs.accent.toArgb())
            .setContentTitle(contexte.getString(R.string.rappel_titre))
            .setContentText(contexte.getString(R.string.rappel_texte))
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setContentIntent(ouvrirKova)
            .build()
        gestionnaire.notify(IDENTIFIANT, notification)
    }
}

/**
 * Reçoit le réveil du rappel. Au redémarrage du téléphone, Android oublie les
 * réveils et les notifications : on reprogramme le rappel et on remet la
 * notification du temps restant si une fenêtre est encore ouverte.
 */
class RecepteurRappel : BroadcastReceiver() {
    override fun onReceive(contexte: Context, intention: Intent) {
        if (intention.action == Intent.ACTION_BOOT_COMPLETED) {
            Rappel.programmer(contexte, Rappel.actif(contexte))
            val maintenant = System.currentTimeMillis()
            Minuteur.mettreAJour(contexte, Depot.lire(contexte).mettreAJour(maintenant), maintenant, ZoneId.systemDefault())
        } else {
            Rappel.afficherSiUtile(contexte)
        }
    }
}
