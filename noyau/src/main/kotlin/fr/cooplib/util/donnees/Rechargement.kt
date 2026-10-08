package fr.cooplib.util.donnees

import fr.cooplib.util.modeles.Version
import fr.cooplib.util.reseau.ClientApi
import fr.cooplib.util.reseau.Resultat
import java.time.Instant
import java.time.temporal.ChronoUnit

/*
 * Tout le catalogue, relu de l'API. Servi à deux endroits, avec le même
 * code : la fabrique du catalogue embarqué, et le rechargement complet
 * de la synchronisation chez les gens.
 *
 * Des LECTURES seulement, une à une : environ 290 requêtes et 2,5 Mo,
 * d'où une fois par semaine au plus, et jamais sur une connexion
 * facturée au volume sans demander.
 *
 * Tout ou rien : à la première requête qui échoue, rien n'est rendu, et
 * l'appelant garde ce qu'il avait. Un catalogue à moitié rempli serait
 * pire qu'un catalogue ancien.
 */
fun rechargerTout(api: ClientApi, maintenant: Instant): Resultat<Catalogue> = avecArret {

    // La marque AVANT tout le reste : ce qui change pendant qu'on lit
    // sera plus récent qu'elle, donc revu par la synchronisation
    // suivante, et non perdu.
    val marque = api.historique(1).ou().versions.firstOrNull()?.marque()

    val liste = api.violentometres().ou().sortedBy { it.id }
    val listeDesParcours = api.parcours().ou().sortedBy { it.id }

    Catalogue(
        fabriqueLe = maintenant.truncatedTo(ChronoUnit.SECONDS).toString(),
        source = api.adresse.base,
        derniereVersion = marque,
        aides = api.aides().ou().sortedBy { it.id },
        violentometres = liste.map { api.violentometre(it.id).ou() },
        parcours = listeDesParcours.map { api.unParcours(it.id).ou() },
        recits = api.recits().ou().sortedBy { it.id },
        cadres = api.cadres().ou().sortedBy { it.id }.map { api.cadre(it.id).ou() },
        mecanismes = api.mecanismes().ou().sortedBy { it.id }.map { api.mecanisme(it.id).ou() },
        pointGeneral = api.point("orientation", "tout").ou(),
        pointsParViolentometre = liste.associate { it.id to api.point("violentometer", it.id).ou() },
        proches = liste.associate { it.id to api.proches(it.id).ou() },
        contextes = api.contextes().ou().sortedBy { it.id },
        pointsParParcours = listeDesParcours.associate { it.id to api.point("parcours", it.id).ou() },
    )
}

internal fun Version.marque() = Marque(id, modifieLe)

// ---------------------------------------------------------
// Arrêter à la première requête qui échoue
// ---------------------------------------------------------

/*
 * Écrire « .ou() » derrière chaque requête plutôt qu'un `when` de
 * quatre cas deux cents fois. L'exception ne sort jamais d'ici :
 * `avecArret` la rend en Resultat.
 */
internal class Arret(val resultat: Resultat<Nothing>) : Exception(null, null, false, false)

internal fun <T> Resultat<T>.ou(): T = when (this) {
    is Resultat.Ok -> valeur
    is Resultat.Limite -> throw Arret(this)
    is Resultat.Refuse -> throw Arret(this)
    is Resultat.Injoignable -> throw Arret(this)
}

internal inline fun <T> avecArret(bloc: () -> T): Resultat<T> = try {
    Resultat.Ok(bloc())
} catch (a: Arret) {
    a.resultat
}
