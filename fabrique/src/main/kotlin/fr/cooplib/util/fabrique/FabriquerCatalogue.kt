package fr.cooplib.util.fabrique

import fr.cooplib.util.donnees.Catalogue
import fr.cooplib.util.reseau.AdresseApi
import fr.cooplib.util.reseau.ClientApi
import fr.cooplib.util.reseau.Resultat
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.system.exitProcess

/*
 * Tire le catalogue embarqué de l'API : des LECTURES seulement, une à
 * une. Rien ne se supprime sur ce site, une écriture de sonde y
 * resterait pour toujours.
 *
 * Tout passe par les modèles du noyau : seuls les champs que
 * l'application lit survivent (ni compteurs de vues, ni scores, ni le
 * violentomètre entier que chaque étape de parcours embarque), et un
 * catalogue qui s'écrit ici est un catalogue que l'application sait
 * relire.
 *
 * Tout ou rien : une seule requête qui échoue, et l'ancien catalogue
 * reste en place. Un catalogue à moitié rempli passerait les tests et
 * partirait dans un APK.
 */

// Un JSON lisible, aux listes triées par identifiant : l'API en range
// certaines par popularité, qui bouge tous les jours, et le diff du
// commit ne dirait plus ce qui a VRAIMENT changé.
@OptIn(ExperimentalSerializationApi::class)
private val ecriture = Json {
    prettyPrint = true
    prettyPrintIndent = " "
}

private class Echec(message: String) : Exception(message)

private fun <T> Resultat<T>.ou(quoi: String): T = when (this) {
    is Resultat.Ok -> valeur
    is Resultat.Limite -> throw Echec("$quoi : limite de cadence, réessayer dans $attenteSecondes s")
    is Resultat.Refuse -> throw Echec("$quoi : refusé ($code) ${detail ?: ""}")
    is Resultat.Injoignable -> throw Echec("$quoi : injoignable ($cause)")
}

fun main(args: Array<String>) {

    val sortie = File(args.getOrNull(0) ?: error("Usage : FabriquerCatalogue <sortie.json> [adresse]"))

    val adresse = AdresseApi.depuis(args.getOrNull(1) ?: AdresseApi.PRODUCTION.base)
        ?: error("Adresse d'API invalide : ${args.getOrNull(1)}")

    val api = ClientApi(adresse)

    val catalogue = try {
        fabriquer(api, adresse)
    } catch (e: Echec) {
        System.err.println("Catalogue NON réécrit, l'ancien reste : ${e.message}")
        exitProcess(1)
    }

    sortie.parentFile.mkdirs()

    // Écrit à côté puis remplacé d'un coup : jamais de fichier tronqué.
    val provisoire = File(sortie.parentFile, "${sortie.name}.provisoire")
    provisoire.writeText(ecriture.encodeToString(Catalogue.serializer(), catalogue) + "\n")
    Files.move(provisoire.toPath(), sortie.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)

    println(
        "Catalogue écrit : ${sortie.length() / 1024} Ko — " +
            "${catalogue.violentometres.size} violentomètres, ${catalogue.parcours.size} parcours, " +
            "${catalogue.recits.size} récits, ${catalogue.aides.size} aides, " +
            "${catalogue.cadres.size} cadres, ${catalogue.mecanismes.size} mécanismes, " +
            "${catalogue.pointsParViolentometre.size} points par violentomètre."
    )
}

private fun fabriquer(api: ClientApi, adresse: AdresseApi): Catalogue {

    // Noté avant la première requête : ce qui change pendant qu'on lit
    // sera revu par la première synchronisation, pas perdu.
    val debut = Instant.now().truncatedTo(ChronoUnit.SECONDS)

    val liste = api.violentometres().ou("la liste des violentomètres")

    val violentometres = liste.sortedBy { it.id }.map { api.violentometre(it.id).ou("le violentomètre ${it.id}") }

    val points = liste.sortedBy { it.id }.associate { it.id to api.point("violentometer", it.id).ou("le point sur ${it.id}") }

    val parcours = api.parcours().ou("la liste des parcours")
        .sortedBy { it.id }
        .map { api.unParcours(it.id).ou("le parcours ${it.id}") }

    val cadres = api.cadres().ou("la liste des cadres")
        .sortedBy { it.id }
        .map { api.cadre(it.id).ou("le cadre ${it.id}") }

    val mecanismes = api.mecanismes().ou("la liste des mécanismes")
        .sortedBy { it.id }
        .map { api.mecanisme(it.id).ou("le mécanisme ${it.id}") }

    return Catalogue(
        fabriqueLe = debut.toString(),
        source = adresse.base,
        aides = api.aides().ou("les aides").sortedBy { it.id },
        violentometres = violentometres,
        parcours = parcours,
        recits = api.recits().ou("les récits").sortedBy { it.id },
        cadres = cadres,
        mecanismes = mecanismes,
        pointGeneral = api.point("orientation", "tout").ou("le point général"),
        pointsParViolentometre = points,
    )
}
