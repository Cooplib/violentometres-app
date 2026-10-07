package fr.cooplib.util.reseau

import java.net.URI

/*
 * Où est l'API. Configurable dès le départ, parce qu'un second nom
 * d'hôte, neutre, est prévu (APPLICATION-ANDROID.md, chapitre 5) : le
 * même serveur sous un autre nom, pour qu'un journal de box ne lise
 * pas « violentometres » à chaque synchronisation.
 *
 * Ce second nom cache le NOM, pas la destination : l'adresse IP reste
 * la même, et une recherche inverse la retrouve. Ne pas lui prêter
 * plus.
 *
 * HTTPS seulement : en clair, le contenu de chaque requête se lirait
 * sur le réseau, récits déposés compris. Seule exception, la machine
 * elle-même, pour développer contre l'API du banc.
 */
@JvmInline
value class AdresseApi private constructor(val base: String) {

    override fun toString() = base

    companion object {

        val PRODUCTION = AdresseApi("https://violentometres.fr/api")

        private val LOCALES = setOf("localhost", "127.0.0.1", "10.0.2.2")

        fun depuis(texte: String): AdresseApi? {

            val propre = texte.trim().trimEnd('/')

            val uri = runCatching { URI(propre) }.getOrNull() ?: return null

            val hote = uri.host ?: return null

            val permise = uri.scheme == "https" || (uri.scheme == "http" && hote in LOCALES)

            // Ni requête ni fragment : les chemins s'ajoutent derrière.
            if (!permise || uri.rawQuery != null || uri.rawFragment != null) {
                return null
            }

            return AdresseApi(propre)
        }
    }
}
