package fr.cooplib.util.donnees

import fr.cooplib.util.modeles.Aide
import fr.cooplib.util.modeles.Cadre
import fr.cooplib.util.modeles.Mecanisme
import fr.cooplib.util.modeles.Parcours
import fr.cooplib.util.modeles.PoolDuPoint
import fr.cooplib.util.modeles.Proches
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
    // La version la plus récente de l'historique du site au moment où le
    // catalogue a été tiré, à l'heure DU SERVEUR : c'est d'elle que part
    // la synchronisation, sans dépendre de l'horloge du téléphone. Nulle
    // tant que l'historique est vide (le contenu semé n'en crée pas).
    val derniereVersion: Marque? = null,
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
    // Ce qui ressemble à chaque violentomètre, calculé par le serveur.
    // Vide dans un catalogue tiré avant qu'on l'embarque.
    val proches: Map<String, Proches> = emptyMap(),
    // Les contextes et leurs familles, pour le filtre des listes.
    val contextes: List<fr.cooplib.util.modeles.ContexteDuSite> = emptyList(),
    // « Faire le point » sur un parcours entier, comme sur le site : ses
    // violentomètres réunis, tels que l'API les assemble.
    val pointsParParcours: Map<String, PoolDuPoint> = emptyMap(),
    // La bibliothèque des situations, pour construire « Mon
    // violentomètre » hors ligne ; dans l'ordre de l'API, les plus
    // employées d'abord.
    val bibliotheque: List<fr.cooplib.util.modeles.SituationDeBibliotheque> = emptyList(),
) {

    companion object {

        const val FORMAT = 1

        // Une ressource Java du noyau : elle suit le module dans l'APK,
        // et les tests la lisent sans Android.
        const val RESSOURCE = "/fr/cooplib/util/donnees/catalogue.json"

        fun lire(texte: String): Catalogue = decodage.decodeFromString(serializer(), texte)

        /*
         * Lequel servir : celui gardé sur le téléphone (mis à jour par la
         * synchronisation), ou celui de l'APK. Après une mise à jour de
         * l'application, l'APK peut porter un catalogue tiré plus tard que
         * le dernier rechargement complet du téléphone : il l'emporte, et
         * la synchronisation repart de sa marque à lui.
         *
         * Un catalogue gardé d'un autre format (écrit par une version
         * plus ancienne, ou plus récente puis désinstallée) est ignoré
         * plutôt que mal lu.
         */
        fun plusRecent(embarque: Catalogue, garde: Catalogue?): Catalogue = when {
            garde == null || garde.format != FORMAT -> embarque
            java.time.Instant.parse(embarque.fabriqueLe).isAfter(java.time.Instant.parse(garde.fabriqueLe)) -> embarque
            else -> garde
        }

        fun embarque(): Catalogue =
            lire(
                Catalogue::class.java.getResourceAsStream(RESSOURCE)
                    ?.use { it.readBytes().toString(Charsets.UTF_8) }
                    ?: error("catalogue embarqué absent : $RESSOURCE")
            )
    }
}

// Une version de l'historique, reconnue à son identifiant ET à sa date :
// une version se prolonge (même session d'écriture, moins de trente
// minutes), elle garde alors son identifiant et sa date avance.
@Serializable
data class Marque(val id: String, val modifieLe: String)
