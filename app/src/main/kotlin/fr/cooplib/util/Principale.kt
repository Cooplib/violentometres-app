package fr.cooplib.util

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import fr.cooplib.util.ecrans.Application

/*
 * L'unique activité. Un nom muet : il se lit dans l'APK et dans
 * certains outils du système.
 */
class Principale : ComponentActivity() {

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

        setContent { Application() }
    }
}
