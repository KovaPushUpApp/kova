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
import com.getkova.app.Mesures
import com.getkova.app.R
import com.getkova.app.design.Tokens
import com.getkova.app.jeu.finDOuverture
import com.getkova.app.jeu.mettreAJour
import com.getkova.app.jeu.repetitionsDuJour
import com.getkova.app.jeu.serie
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

    // --- Fin de l'essai gratuit ---

    const val ACTION_FIN_ESSAI = "com.getkova.app.FIN_ESSAI"
    private const val CANAL_ESSAI = "essai"
    private const val IDENTIFIANT_ESSAI = 3
    private const val CLE_ESSAI = "rappelFinEssaiLe"

    /** Durée de l'essai gratuit de la formule annuelle, en jours. */
    private const val JOURS_ESSAI = 3
    private const val JOUR = 24L * 3600 * 1000

    private fun declencheurEssai(contexte: Context): PendingIntent = PendingIntent.getBroadcast(
        contexte, IDENTIFIANT_ESSAI,
        Intent(contexte, RecepteurRappel::class.java).setAction(ACTION_FIN_ESSAI),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** À appeler quand l'essai démarre : le rappel part la veille du premier paiement. */
    fun programmerFinEssai(contexte: Context, debut: Long = System.currentTimeMillis()) {
        Depot.reglages(contexte).edit().putLong(CLE_ESSAI, debut + (JOURS_ESSAI - 1) * JOUR).apply()
        armerFinEssai(contexte)
    }

    /** Pose le réveil du rappel de fin d'essai s'il est encore à venir. Sert aussi après un redémarrage. */
    fun armerFinEssai(contexte: Context) {
        val le = Depot.reglages(contexte).getLong(CLE_ESSAI, 0)
        if (le <= System.currentTimeMillis()) return
        val reveils = contexte.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        reveils.set(AlarmManager.RTC_WAKEUP, le, declencheurEssai(contexte))
    }

    fun afficherFinEssai(contexte: Context) {
        Depot.reglages(contexte).edit().remove(CLE_ESSAI).apply()
        if (!Minuteur.autorise(contexte)) return
        val gestionnaire = contexte.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        gestionnaire.createNotificationChannel(
            NotificationChannel(CANAL_ESSAI, contexte.getString(R.string.essai_rappel_canal), NotificationManager.IMPORTANCE_HIGH),
        )
        val texte = contexte.getString(R.string.essai_rappel_texte, contexte.getString(R.string.prix_annuel))
        val notification = Notification.Builder(contexte, CANAL_ESSAI)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(Tokens.Couleurs.accent.toArgb())
            .setContentTitle(contexte.getString(R.string.essai_rappel_titre))
            .setContentText(texte)
            .setStyle(Notification.BigTextStyle().bigText(texte))
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setContentIntent(ouvrirKova(contexte))
            .build()
        gestionnaire.notify(IDENTIFIANT_ESSAI, notification)
        Mesures.suivre(contexte, "rappel_fin_essai_affiche")
    }

    private fun ouvrirKova(contexte: Context): PendingIntent = PendingIntent.getActivity(
        contexte, 0,
        Intent(contexte, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

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
        // Avec une série en cours, le rappel dit ce qui est en jeu ce soir.
        val serie = etat.serie(maintenant, zone)
        val titre = when {
            serie <= 0 -> contexte.getString(R.string.rappel_titre)
            serie == 1 -> contexte.getString(R.string.rappel_serie_titre_un, serie)
            else -> contexte.getString(R.string.rappel_serie_titre_autre, serie)
        }
        val texte = contexte.getString(if (serie > 0) R.string.rappel_serie_texte else R.string.rappel_texte)
        val notification = Notification.Builder(contexte, CANAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(Tokens.Couleurs.accent.toArgb())
            .setContentTitle(titre)
            .setContentText(texte)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setContentIntent(ouvrirKova(contexte))
            .build()
        gestionnaire.notify(IDENTIFIANT, notification)
        Mesures.suivre(contexte, "rappel_affiche", "serie" to serie)
    }
}

/**
 * Reçoit le réveil du rappel. Au redémarrage du téléphone, Android oublie les
 * réveils et les notifications : on reprogramme le rappel et on remet la
 * notification du temps restant si une fenêtre est encore ouverte.
 */
class RecepteurRappel : BroadcastReceiver() {
    override fun onReceive(contexte: Context, intention: Intent) {
        if (intention.action == Rappel.ACTION_FIN_ESSAI) {
            Rappel.afficherFinEssai(contexte)
        } else if (intention.action == Intent.ACTION_BOOT_COMPLETED) {
            Rappel.programmer(contexte, Rappel.actif(contexte))
            Rappel.armerFinEssai(contexte)
            val maintenant = System.currentTimeMillis()
            Minuteur.mettreAJour(contexte, Depot.lire(contexte).mettreAJour(maintenant), maintenant, ZoneId.systemDefault())
        } else {
            Rappel.afficherSiUtile(contexte)
        }
    }
}
