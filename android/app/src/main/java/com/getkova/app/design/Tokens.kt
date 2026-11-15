// Fichier généré par android/outils/generer.py depuis design-tokens.json.
// Ne pas modifier à la main : changer le JSON puis relancer le script.
// Les dimensions sont des nombres : ajouter .dp ou .sp à l'usage.

package com.getkova.app.design

import androidx.compose.ui.graphics.Color

object Tokens {
    const val nom = "Kova"
    const val principe = "Nombre d'or (1,618) pour les tailles de texte et les proportions, suite de Fibonacci pour les espacements"
    object Ecran {
        const val largeur = 390.0f
        const val hauteur = 844.0f
        const val margeHaut = 55.0f
        const val margeCotes = 21.0f
        const val margeBas = 34.0f
    }
    object Couleurs {
        val fond = Color(0xFF0E0E10)
        val carte = Color(0xFF1A1A1E)
        val carteSelectionnee = Color(0xFF23261A)
        val ligne = Color(0xFF2C2C33)
        val ligneFine = Color(0xFF26262B)
        val pointille = Color(0xFF3A3A42)
        val texte = Color(0xFFF4F4F1)
        val texteSecondaire = Color(0xFFA3A3AB)
        val accent = Color(0xFFC8F53C)
        val texteSurAccent = Color(0xFF0E0E10)
        val barreInactive = Color(0xFF4A4D3A)
    }
    object Polices {
        object Titres {
            const val famille = "Space Grotesk"
            const val graisse = 700
        }
        object Texte {
            const val famille = "Inter"
            val graisses = listOf(400, 500, 600)
        }
    }
    object TaillesTexte {
        const val legende = 13.0f
        const val corps = 16.0f
        const val corpsFort = 20.0f
        const val titreCarte = 26.0f
        const val titreEcran = 33.0f
        const val titreAccueil = 42.0f
        const val chiffre = 68.0f
        const val chiffreGeant = 110.0f
        const val chiffreChoc = 178.0f
    }
    object Interligne {
        const val paragraphe = 1.618f
        const val titre = 1.12f
        const val chiffre = 1.0f
    }
    /** Suite de Fibonacci. `e21` vaut 21. */
    object Espacements {
        const val e3 = 3.0f
        const val e5 = 5.0f
        const val e8 = 8.0f
        const val e13 = 13.0f
        const val e21 = 21.0f
        const val e34 = 34.0f
        const val e55 = 55.0f
        const val e89 = 89.0f
        const val e144 = 144.0f
    }
    object Arrondis {
        const val petit = 8.0f
        const val moyen = 13.0f
        const val grand = 21.0f
        const val tresGrand = 34.0f
        const val rond = "50%"
    }
    object Hauteurs {
        const val boutonPrincipal = 55.0f
        const val option = 55.0f
        const val onglet = 55.0f
        const val boutonSecondaire = 44.0f
        const val boutonIcone = 44.0f
        const val ligneReglage = 55.0f
    }
    object Composants {
        object Interrupteur {
            const val largeur = 55.0f
            const val hauteur = 34.0f
            const val pastille = 28.0f
            const val decalageActif = 21.0f
        }
        object VignetteApp {
            const val taille = 34.0f
            const val arrondi = 8.0f
        }
        object Logo {
            const val taille = 89.0f
            const val arrondi = 21.0f
            const val proportionSymbole = 0.618f
        }
        object VueCamera {
            const val hauteurSeance = 322.0f
            const val hauteurEssai = 233.0f
            const val partDeLecran = 0.382f
        }
        object BarreProgression {
            const val segments = 10
            const val hauteur = 8.0f
            const val ecart = 5.0f
        }
        object Illustration {
            const val largeur = 170.0f
            const val hauteur = 80.0f
        }
        object Graphique {
            const val hauteur = 233.0f
            const val barreMax = 144.0f
            const val barreVide = 4.0f
        }
        object BarreAnnee {
            const val hauteur = 13.0f
            const val arrondi = 8.0f
        }
    }
    object Animations {
        const val dureeStandard = 0.5f
        const val courbe = "ease"
        object Entree {
            const val nom = "up"
            val opacite = listOf(0.0f, 1.0f)
            val translationY = listOf(18.0f, 0.0f)
            val cascade = listOf(0.07f, 0.15f)
        }
        object Pop {
            const val duree = 0.6f
            val echelle = listOf(0.6f, 1.06f, 1.0f)
            const val pic = 0.7f
        }
        object Pulsation {
            const val duree = 2.4f
            val echelle = listOf(1.0f, 1.035f, 1.0f)
            const val boucle = true
        }
        object CroissanceBarre {
            const val duree = 0.6f
            const val origine = "bas"
        }
        object Remplissage {
            const val duree = 0.6f
            const val origine = "gauche"
        }
        object Anneau {
            const val duree = 2.0f
            val echelle = listOf(0.7f, 1.9f)
            val opacite = listOf(0.8f, 0.0f)
            const val boucle = true
        }
        object ScanCamera {
            const val duree = 3.0f
            const val boucle = true
            const val debut = 0.08f
            const val fin = 0.88f
        }
        object CompteurChoc {
            const val pas = 2
            const val intervalleMs = 28
        }
        object CompteurGain {
            const val pas = 1
            const val intervalleMs = 55
        }
        const val reduireLesAnimations = "respecter le réglage système"
        object Pompe {
            const val duree = 1.6f
            const val boucle = true
        }
    }
    object Haptique {
        const val repetition = "léger"
        const val serieValidee = "fort"
    }
    object Traits {
        const val fin = 1.0f
        const val normal = 2.0f
        const val illustration = 3.0f
    }
    object Opacites {
        const val voileSurAccent = 0.12f
        const val scan = 0.5f
        const val voileCamera = 0.85f
    }
    object Typographie {
        const val echelleMinimale = 0.5f
        const val interlettrageSurtitre = 0.08f
    }
}
