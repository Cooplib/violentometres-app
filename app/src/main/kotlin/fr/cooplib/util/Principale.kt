package fr.cooplib.util

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import fr.cooplib.util.ecrans.Application
import fr.cooplib.util.stockage.Brouillon
import fr.cooplib.util.stockage.Depot
import fr.cooplib.util.stockage.Reponses

/*
 * L'unique activité, ouverte par l'un ou l'autre alias du lanceur
 * (Lanceur.kt). Un nom muet : il se lit dans l'APK et dans certains
 * outils du système.
 */
class Principale : ComponentActivity() {

    // Verrouillée au départ : déguisée, l'application s'ouvre toujours
    // sur la calculatrice.
    private val verrouillee = mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        // Avant tout affichage, pour qu'aucune image ne passe entre les deux.
        appliquerLesCaptures(Reglages(this).capturesPermises)

        // Une rotation recrée l'activité : elle ne doit pas reverrouiller.
        verrouillee.value = savedInstanceState?.getBoolean("verrouillee") ?: true

        setContent {
            Application(
                verrouillee = verrouillee.value,
                deverrouiller = { verrouillee.value = false },
                quitterVite = ::quitterVite,
                permettreLesCaptures = { Reglages(this).capturesPermises = it; appliquerLesCaptures(it) },
            )
        }
    }

    /*
     * FLAG_SECURE : la vignette des applications récentes ne montre pas
     * le dernier écran, les captures d'écran sont refusées, et aucune
     * application ne peut enregistrer ni partager l'écran. Sans lui,
     * renommer l'icône ne sert à rien : il suffirait d'ouvrir les
     * applications récentes pour voir un résultat du point.
     *
     * Quand la personne permet les captures, il faut le retirer : c'est
     * lui qui les refuse. Depuis Android 13, la vignette se cache à part
     * (setRecentsScreenshotEnabled) : elle reste vide quoi qu'on choisisse.
     * Avant, rien ne la cache sans refuser aussi les captures ; l'écran du
     * choix le dit.
     */
    private fun appliquerLesCaptures(permises: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setRecentsScreenshotEnabled(false)
        }
        if (permises) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("verrouillee", verrouillee.value)
    }

    /*
     * « Quitter vite » : effacer ce qui parle de la personne (les réponses
     * au point, le brouillon d'un récit, ce qu'elle a aimé et lu), puis
     * fermer, en retirant l'application des applications récentes.
     *
     * Et REMETTRE LA CALCULATRICE, même si la personne avait choisi
     * d'afficher le vrai nom (décidé le 7 octobre 2026) : si l'on a voulu
     * quitter vite, c'est qu'il y a sans doute une galère. Le code de
     * secours, choisi au premier lancement, rouvrira l'application.
     * Sans code (une installation d'avant cette règle), on ne redéguise
     * pas : la calculatrice ne pourrait plus s'ouvrir.
     */
    private fun quitterVite() {
        Reponses.effacer(this)
        Brouillon.effacer(this)
        Depot.de(this).memoire.oublier()
        Depot.de(this).carnet.oublier()
        val reglages = Reglages(this)
        // Comme la calculatrice : si l'on a voulu quitter vite, on
        // referme ce qui avait été ouvert.
        reglages.capturesPermises = false
        if (!reglages.deguise && reglages.empreinteDuCode != null) {
            reglages.deguise = true
            Lanceur.afficherLeVraiNom(this, false)
        }
        finishAndRemoveTask()
    }

    /*
     * Quittée (écran d'accueil, autre application, écran éteint), elle
     * se reverrouille : en revenant, on retombe sur la calculatrice, et
     * ce qui était ouvert derrière est oublié.
     *
     * Y compris quand on part appeler un numéro depuis « Trouver de
     * l'aide » : il faudra retaper le code au retour. C'est voulu. Un
     * délai de grâce laisserait l'application ouverte sur un téléphone
     * posé, et c'est exactement le moment où quelqu'un d'autre le prend.
     */
    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            verrouillee.value = true
        }
    }
}
