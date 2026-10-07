package fr.cooplib.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/*
 * Le nom et l'icône dans le lanceur, changés pendant que l'application
 * tourne : deux `activity-alias` déclarés d'avance dans le manifeste,
 * dont un seul est actif à la fois.
 *
 * À l'installation, c'est la calculatrice (décision du 7 octobre 2026) :
 * discret par défaut, et le premier écran propose d'afficher le vrai
 * nom, jamais l'inverse. S'installer sous le vrai nom puis proposer de
 * le cacher laisserait « Violentomètres » dans le lanceur juste au
 * moment où l'on regarde le téléphone, celui où l'on vient de
 * télécharger quelque chose.
 *
 * Ce que ça ne change pas : le nom dans Réglages → Applications reste
 * celui de l'application (« Calculatrice », voir le manifeste), et le
 * nom du paquet, fr.cooplib.util, ne bouge jamais.
 */
object Lanceur {

    private const val CALCULATRICE = "fr.cooplib.util.EntreeCalculatrice"
    private const val VRAI_NOM = "fr.cooplib.util.EntreeNommee"

    fun afficherLeVraiNom(contexte: Context, vraiNom: Boolean) {

        val pm = contexte.packageManager

        fun regler(alias: String, actif: Boolean) = pm.setComponentEnabledSetting(
            ComponentName(contexte, alias),
            if (actif) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )

        // Le nouveau d'abord, l'ancien ensuite : jamais un instant sans
        // aucune entrée dans le lanceur.
        if (vraiNom) {
            regler(VRAI_NOM, true)
            regler(CALCULATRICE, false)
        } else {
            regler(CALCULATRICE, true)
            regler(VRAI_NOM, false)
        }
    }
}
