package fr.cooplib.util

import android.content.Context
import fr.cooplib.util.reseau.IdentifiantVisiteur

/*
 * Ce que l'application retient de ses réglages, sur ce téléphone
 * seulement : les règles de sauvegarde du manifeste l'excluent du compte
 * Google et du transfert vers un autre téléphone.
 *
 * Rien là-dedans ne parle de la personne. Les réponses au point, quand
 * elles seront gardées, ne passeront pas par ici.
 *
 * Un nom de fichier muet : il se lit dans les données de l'application.
 */
class Reglages(contexte: Context) {

    private val p = contexte.getSharedPreferences("r", Context.MODE_PRIVATE)

    // Les deux écrans du premier lancement ont été vus.
    var configure: Boolean
        get() = p.getBoolean("configure", false)
        set(v) { p.edit().putBoolean("configure", v).commit() }

    // Calculatrice dans le lanceur, et code pour entrer. Vrai par défaut :
    // discret tant que la personne n'a pas choisi autre chose.
    var deguise: Boolean
        get() = p.getBoolean("deguise", true)
        set(v) { p.edit().putBoolean("deguise", v).commit() }

    // L'empreinte du code (CodeSecret), jamais le code.
    var empreinteDuCode: String?
        get() = p.getString("code", null)
        set(v) { p.edit().putString("code", v).commit() }

    // « auto » (comme le téléphone), « clair » ou « sombre ».
    var theme: String
        get() = p.getString("theme", "auto") ?: "auto"
        set(v) { p.edit().putString("theme", v).commit() }

    /*
     * L'identifiant de contributeur, GARDÉ depuis que les likes partent au
     * site (décision du 7 octobre 2026, notes de conception) : sans lui, on ne
     * pourrait pas retirer un like. Il relie donc, côté serveur, ce qui
     * vient de ce téléphone. Fabriqué au premier besoin, jamais avant.
     */
    val visiteur: IdentifiantVisiteur
        get() = p.getString("visiteur", null)?.let(IdentifiantVisiteur::depuis)
            ?: IdentifiantVisiteur.nouveau().also { p.edit().putString("visiteur", it.valeur).commit() }

    // Ne plus prévenir avant d'ouvrir un site dans le navigateur (case
    // cochée dans l'avertissement, VersUnSite.kt).
    var nePlusPrevenirPourLesSites: Boolean
        get() = p.getBoolean("site", false)
        set(v) { p.edit().putBoolean("site", v).commit() }

    // Pouvoir reprendre ses réponses au point d'une fois à l'autre. Faux
    // par défaut, comme sur le site : c'est une trace sur le téléphone.
    var garderLesReponses: Boolean
        get() = p.getBoolean("garder", false)
        set(v) { p.edit().putBoolean("garder", v).commit() }
}
