package fr.cooplib.util.donnees

import fr.cooplib.util.modeles.Version
import fr.cooplib.util.reseau.ClientApi
import fr.cooplib.util.reseau.Resultat
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

/*
 * Garder le catalogue à jour, en faisant aussi peu de réseau que
 * possible.
 *
 * LE RÉSEAU EST LA TRACE QUE LE DÉGUISEMENT NE COUVRE PAS. Une requête,
 * c'est une résolution DNS et une connexion que la box voit ; et la
 * consommation de données par application se lit dans les réglages.
 * D'où, plutôt qu'à chaque ouverture :
 *
 *  - à la demande, quand la personne appuie sur « mettre à jour » ;
 *  - tout seul, au plus tous les trois jours, à l'ouverture ;
 *  - jamais tout seul sur une connexion facturée au volume ;
 *  - un rechargement complet par semaine, en plus.
 *
 * Le rechargement complet n'est pas un luxe : le contenu semé côté
 * serveur ne crée PAS de versions, la sonde ne le voit jamais. Et au 7
 * octobre 2026, l'historique de production est vide : pour l'instant,
 * c'est même le seul mécanisme qui ramène du neuf.
 */

// Ce que l'application garde d'une synchronisation à l'autre. Du
// contenu public, comme le catalogue : il ne dit rien de la personne.
@Serializable
data class EtatDeSynchro(
    // Réussie ou non : un téléphone hors réseau ne doit pas réessayer à
    // chaque ouverture.
    val derniereTentative: String? = null,
    val dernierComplet: String,
)

enum class Decision {
    RIEN,
    // La sonde (quelques centaines d'octets), puis seulement ce qui a changé.
    INCREMENTALE,
    COMPLETE,
    // Une synchronisation serait due, mais la connexion est facturée au
    // volume : l'écran demande, il ne décide pas.
    DEMANDER,
}

object Synchronisation {

    val ESPACEMENT: Duration = Duration.ofDays(3)

    val COMPLET_TOUS_LES: Duration = Duration.ofDays(7)

    // Au départ, l'état vient du catalogue embarqué : il compte comme un
    // rechargement complet fait le jour de sa fabrication.
    fun depart(catalogue: Catalogue) = EtatDeSynchro(dernierComplet = catalogue.fabriqueLe)

    fun decider(etat: EtatDeSynchro, maintenant: Instant, demandee: Boolean, facturee: Boolean): Decision {

        val completDu = !maintenant.isBefore(instant(etat.dernierComplet).plus(COMPLET_TOUS_LES))

        // Demandée : la personne a choisi, connexion facturée comprise.
        if (demandee) {
            return if (completDu) Decision.COMPLETE else Decision.INCREMENTALE
        }

        val derniere = etat.derniereTentative?.let(::instant)

        if (derniere != null && maintenant.isBefore(derniere.plus(ESPACEMENT))) {
            return Decision.RIEN
        }

        if (facturee) {
            return Decision.DEMANDER
        }

        return if (completDu) Decision.COMPLETE else Decision.INCREMENTALE
    }

    /*
     * La synchronisation entière : décider, faire, et dire où l'on en
     * est. Le catalogue rendu est complet et cohérent, à garder tel
     * quel ; sur un échec, l'ancien reste valable.
     */
    fun synchroniser(
        api: ClientApi,
        catalogue: Catalogue,
        etat: EtatDeSynchro,
        maintenant: Instant,
        demandee: Boolean,
        facturee: Boolean,
    ): Bilan {

        val decision = decider(etat, maintenant, demandee, facturee)

        if (decision == Decision.RIEN || decision == Decision.DEMANDER) {
            return Bilan(decision, catalogue, etat, null)
        }

        val tente = etat.copy(derniereTentative = maintenant.toString())

        val issue = if (decision == Decision.COMPLETE) {
            rechargerTout(api, maintenant).map { Issue.Recharge(it) }
        } else {
            incrementale(api, catalogue)
        }

        // L'incrémentale peut ne pas suffire (trop de changements, un
        // type qu'on ne sait pas suivre) : on recharge tout, tout de suite.
        val finale = if (issue is Resultat.Ok && issue.valeur is Issue.CompletNecessaire) {
            rechargerTout(api, maintenant).map { Issue.Recharge(it) }
        } else {
            issue
        }

        return when (finale) {
            is Resultat.Ok -> when (val i = finale.valeur) {
                is Issue.Recharge -> Bilan(decision, i.catalogue, tente.copy(dernierComplet = maintenant.toString()), finale)
                is Issue.MisAJour -> Bilan(decision, i.catalogue, tente, finale)
                Issue.Inchange, Issue.CompletNecessaire -> Bilan(decision, catalogue, tente, finale)
            }
            else -> Bilan(decision, catalogue, tente, finale)
        }
    }

    // ---------------------------------------------------------
    // L'incrémentale
    // ---------------------------------------------------------

    // Une page d'historique : au-delà, plus simple et plus sûr de tout
    // recharger que de paginer.
    private const val PAGE = 30

    fun incrementale(api: ClientApi, catalogue: Catalogue): Resultat<Issue> = avecArret {

        val sonde = api.historique(1).ou().versions.firstOrNull()

        // Rien dans l'historique, ou rien depuis la marque.
        if (sonde == null || sonde.marque() == catalogue.derniereVersion) {
            return@avecArret Issue.Inchange
        }

        val page = api.historique(PAGE).ou()

        val depuis = catalogue.derniereVersion?.let { instant(it.modifieLe) }

        // `>=` et non `>` : deux versions peuvent porter la même date, et
        // relire un élément déjà à jour ne coûte qu'une requête.
        val nouvelles = page.versions.filter { depuis == null || !instant(it.modifieLe).isBefore(depuis) }

        // La page entière est neuve et il y en a d'autres derrière : on
        // ne verrait pas tout.
        if (page.encore && nouvelles.size == page.versions.size) {
            return@avecArret Issue.CompletNecessaire
        }

        // Sans marque (historique vide quand le catalogue a été tiré),
        // on ne sait pas ce qui a précédé la page : tout recharger.
        if (depuis == null && page.encore) {
            return@avecArret Issue.CompletNecessaire
        }

        val relire = aRelire(catalogue, nouvelles) ?: return@avecArret Issue.CompletNecessaire

        Issue.MisAJour(appliquer(api, catalogue, relire).copy(derniereVersion = sonde.marque()), relire.taille)
    }

    /*
     * Ce qu'une liste de versions oblige à relire. `null` : on ne sait
     * pas le dire sans risque, tout recharger.
     *
     * C'est le seul endroit qui suppose quelque chose de la façon dont
     * le serveur assemble ses réponses (quels points portent quelles
     * aides, par exemple). Là où ce serait deviner, on recharge tout :
     * les aides et les contextes changent rarement, et le rechargement
     * hebdomadaire rattrape de toute façon ce qu'une règle d'ici aurait
     * manqué.
     */
    internal fun aRelire(catalogue: Catalogue, versions: List<Version>): ARelire? {

        val r = ARelire()

        for (v in versions) {

            val id = v.elementId

            when (v.typeElement) {

                "violentometer" -> r.violentometres += id

                // Une situation : les violentomètres qui l'emploient. Absorbée
                // par une autre, elle a disparu d'eux au profit de l'autre :
                // on relit ceux de l'une et de l'autre.
                "behavior" -> {
                    val cherchees = setOfNotNull(id, v.absorbeeDans)
                    r.violentometres += catalogue.violentometres
                        .filter { vm -> vm.situations.any { it.situationId in cherchees } }
                        .map { it.id }
                }

                "parcours" -> r.parcours += id

                "story" -> r.recits = true

                "cadre" -> r.cadres += id

                "mecanisme" -> r.mecanismes += id

                // Les fiches d'atelier ne sont pas dans l'application.
                "fiche" -> Unit

                // Une aide entre dans les points par des liens (situations,
                // formes, contextes) que le serveur résout ; un contexte
                // change les aides réunies de chaque violentomètre qui le
                // porte. Trop de suppositions : tout recharger.
                "aide", "context" -> return null

                // Un type que cette version de l'application ne connaît pas.
                else -> return null
            }
        }

        return r
    }

    internal class ARelire(
        val violentometres: MutableSet<String> = sortedSetOf(),
        val parcours: MutableSet<String> = sortedSetOf(),
        val cadres: MutableSet<String> = sortedSetOf(),
        val mecanismes: MutableSet<String> = sortedSetOf(),
        var recits: Boolean = false,
    ) {
        val taille get() = violentometres.size + parcours.size + cadres.size + mecanismes.size + (if (recits) 1 else 0)
    }

    private fun appliquer(api: ClientApi, catalogue: Catalogue, r: ARelire): Catalogue {

        var c = catalogue

        for (id in r.violentometres) {
            val vm = relireOuRetire(api.violentometre(id))
            val point = vm?.let { api.point("violentometer", id).ou() }
            c = c.copy(
                violentometres = remplacer(c.violentometres, id, vm) { it.id },
                pointsParViolentometre = (c.pointsParViolentometre - id + listOfNotNull(point?.let { id to it })).toSortedMap(),
            )
        }

        for (id in r.parcours) {
            c = c.copy(parcours = remplacer(c.parcours, id, relireOuRetire(api.unParcours(id))) { it.id })
        }

        for (id in r.cadres) {
            c = c.copy(cadres = remplacer(c.cadres, id, relireOuRetire(api.cadre(id))) { it.id })
        }

        // Un mécanisme est aussi nommé dans les points qui en parlent.
        for (id in r.mecanismes) {
            c = c.copy(mecanismes = remplacer(c.mecanismes, id, relireOuRetire(api.mecanisme(id))) { it.id })
            for ((vmId, point) in c.pointsParViolentometre) {
                if (point.mecanismes.any { it.id == id }) {
                    c = c.copy(pointsParViolentometre = c.pointsParViolentometre + (vmId to api.point("violentometer", vmId).ou()))
                }
            }
        }

        if (r.recits) {
            c = c.copy(recits = api.recits().ou().sortedBy { it.id })
        }

        // Le point général reprend tout le site (situations, violentomètres,
        // parcours, récits, mécanismes) : relu dès que quoi que ce soit a
        // changé. 41 Ko, une requête.
        if (r.taille > 0) {
            c = c.copy(pointGeneral = api.point("orientation", "tout").ou())
        }

        return c
    }

    // Un élément retiré répond 404 : il quitte le catalogue. Tout autre
    // échec arrête la synchronisation, qui ne rend rien.
    private fun <T> relireOuRetire(r: Resultat<T>): T? =
        if (r is Resultat.Refuse && r.code == 404) null else r.ou()

    // Remplacé, ajouté s'il est nouveau, retiré si `nouveau` est nul ; la
    // liste reste triée par identifiant, comme la fabrique l'écrit.
    private fun <T> remplacer(liste: List<T>, id: String, nouveau: T?, cle: (T) -> String): List<T> =
        (liste.filter { cle(it) != id } + listOfNotNull(nouveau)).sortedBy(cle)

    /*
     * Les dates de l'API sont en ISO 8601 avec leur décalage
     * (« 2026-09-30T07:30:19.253551+00:00 »). Une date sans décalage
     * (SQLite, dans les tests de l'API) est prise pour de l'UTC.
     */
    internal fun instant(texte: String): Instant = try {
        OffsetDateTime.parse(texte).toInstant()
    } catch (_: DateTimeParseException) {
        try {
            Instant.parse(texte)
        } catch (_: DateTimeParseException) {
            java.time.LocalDateTime.parse(texte).toInstant(ZoneOffset.UTC)
        }
    }
}

sealed interface Issue {
    data object Inchange : Issue
    data class MisAJour(val catalogue: Catalogue, val elements: Int) : Issue
    data class Recharge(val catalogue: Catalogue) : Issue
    data object CompletNecessaire : Issue
}

/*
 * Ce que l'écran a besoin de savoir : quoi garder (le catalogue et
 * l'état, à écrire tels quels), et quoi dire. `issue` est nulle quand
 * rien n'a été tenté.
 */
data class Bilan(
    val decision: Decision,
    val catalogue: Catalogue,
    val etat: EtatDeSynchro,
    val issue: Resultat<Issue>?,
)

internal fun <T, U> Resultat<T>.map(f: (T) -> U): Resultat<U> = when (this) {
    is Resultat.Ok -> Resultat.Ok(f(valeur))
    is Resultat.Limite -> this
    is Resultat.Refuse -> this
    is Resultat.Injoignable -> this
}
