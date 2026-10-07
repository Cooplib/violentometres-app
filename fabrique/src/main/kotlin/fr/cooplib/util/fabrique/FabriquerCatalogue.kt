package fr.cooplib.util.fabrique

import fr.cooplib.util.donnees.Catalogue
import fr.cooplib.util.donnees.rechargerTout
import fr.cooplib.util.reseau.AdresseApi
import fr.cooplib.util.reseau.ClientApi
import fr.cooplib.util.reseau.Resultat
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import kotlin.system.exitProcess

/*
 * Tire le catalogue embarqué de l'API, par le même rechargement complet
 * que la synchronisation des téléphones (noyau, donnees/Rechargement.kt).
 *
 * Tout passe par les modèles du noyau : seuls les champs que
 * l'application lit survivent, et un catalogue qui s'écrit ici est un
 * catalogue que l'application sait relire.
 */

// Un JSON lisible : le diff du commit doit dire ce qui a changé dans le
// contenu. Les listes sont triées par identifiant (Rechargement.kt).
@OptIn(ExperimentalSerializationApi::class)
private val ecriture = Json {
    prettyPrint = true
    prettyPrintIndent = " "
}

fun main(args: Array<String>) {

    val sortie = File(args.getOrNull(0) ?: error("Usage : FabriquerCatalogue <sortie.json> [adresse]"))

    val adresse = AdresseApi.depuis(args.getOrNull(1) ?: AdresseApi.PRODUCTION.base)
        ?: error("Adresse d'API invalide : ${args.getOrNull(1)}")

    val catalogue = when (val r = rechargerTout(ClientApi(adresse), Instant.now())) {
        is Resultat.Ok -> r.valeur
        else -> {
            System.err.println("Catalogue NON réécrit, l'ancien reste : $r")
            exitProcess(1)
        }
    }

    sortie.parentFile.mkdirs()

    // Écrit à côté puis remplacé d'un coup : jamais de fichier tronqué.
    val provisoire = File(sortie.parentFile, "${sortie.name}.provisoire")
    provisoire.writeText(ecriture.encodeToString(Catalogue.serializer(), catalogue) + "\n")
    Files.move(provisoire.toPath(), sortie.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)

    println(
        "Catalogue écrit : ${sortie.length() / 1024} Ko, " +
            "${catalogue.violentometres.size} violentomètres, ${catalogue.parcours.size} parcours, " +
            "${catalogue.recits.size} récits, ${catalogue.aides.size} aides, " +
            "${catalogue.cadres.size} cadres, ${catalogue.mecanismes.size} mécanismes, " +
            "dernière version : ${catalogue.derniereVersion ?: "aucune (historique vide)"}."
    )
}
