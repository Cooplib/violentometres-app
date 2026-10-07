package fr.cooplib.util.donnees

import fr.cooplib.util.modeles.Aide
import fr.cooplib.util.modeles.Cadre
import fr.cooplib.util.modeles.Mecanisme
import fr.cooplib.util.modeles.Parcours
import fr.cooplib.util.modeles.PoolDuPoint
import fr.cooplib.util.modeles.Recit
import fr.cooplib.util.modeles.Violentometre
import fr.cooplib.util.modeles.decodage
import kotlinx.serialization.Serializable

/*
 * Tout le contenu public dont l'application a besoin sans réseau.
 *
 * EMBARQUÉ DANS L'APK : le premier lancement, dans le métro ou dans un
 * foyer sans wifi, ne doit pas être un écran vide. Et chaque requête
 * est une trace que le déguisement ne couvre pas : moins on en fait,
 * mieux c'est.
 *
 * Produit par `./gradlew :fabrique:catalogue`, qui interroge l'API
 * (des lectures seulement) et COMMITÉ. Pas fabriqué à chaque
 * construction : F-Droid recompile depuis les sources et compare, et
 * deux constructions du même commit doivent donner le même APK, sans
 * dépendre du réseau du moment. Le prix : penser à le rafraîchir avant
 * chaque version. La synchronisation fait le reste chez les gens.
 *
 * Du contenu public seulement. Le garder ne révèle rien de plus que
 * l'existence de l'application, que l'icône trahit déjà.
 */
@Serializable
data class Catalogue(
    // À changer si la forme change d'une manière qu'un ancien lecteur ne
    // saurait pas lire.
    val format: Int = FORMAT,
    // Quand il a été tiré de l'API, en ISO 8601 : le point de départ de
    // la synchronisation.
    val fabriqueLe: String,
    val source: String,
    val aides: List<Aide>,
    val violentometres: List<Violentometre>,
    val parcours: List<Parcours>,
    val recits: List<Recit>,
    val cadres: List<Cadre>,
    val mecanismes: List<Mecanisme>,
    // « Faire le point » sans réseau : le point général, et celui de
    // chaque violentomètre, tels que l'API les assemble. Les recomposer
    // ici depuis les violentomètres serait porter une logique de plus
    // du serveur, qui divergerait comme toutes les autres.
    val pointGeneral: PoolDuPoint,
    val pointsParViolentometre: Map<String, PoolDuPoint>,
) {

    companion object {

        const val FORMAT = 1

        // Une ressource Java du noyau : elle suit le module dans l'APK,
        // et les tests la lisent sans Android.
        const val RESSOURCE = "/fr/cooplib/util/donnees/catalogue.json"

        fun lire(texte: String): Catalogue = decodage.decodeFromString(serializer(), texte)

        fun embarque(): Catalogue =
            lire(
                Catalogue::class.java.getResourceAsStream(RESSOURCE)
                    ?.use { it.readBytes().toString(Charsets.UTF_8) }
                    ?: error("catalogue embarqué absent : $RESSOURCE")
            )
    }
}
