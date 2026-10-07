package fr.cooplib.util

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import fr.cooplib.util.ecrans.Application

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

        /*
         * FLAG_SECURE : la vignette des applications récentes ne montre
         * pas le dernier écran, et les captures d'écran sont refusées.
         * Sans lui, renommer l'icône ne sert à rien : il suffirait
         * d'ouvrir les applications récentes pour voir un résultat du
         * point. Posé avant tout affichage, pour qu'aucune image ne
         * passe entre les deux.
         */
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        // Une rotation recrée l'activité : elle ne doit pas reverrouiller.
        verrouillee.value = savedInstanceState?.getBoolean("verrouillee") ?: true

        setContent { Application(verrouillee.value) { verrouillee.value = false } }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("verrouillee", verrouillee.value)
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
