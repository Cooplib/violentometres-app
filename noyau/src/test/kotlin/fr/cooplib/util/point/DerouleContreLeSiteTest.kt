package fr.cooplib.util.point

import fr.cooplib.util.modeles.PoolDuPoint
import fr.cooplib.util.modeles.decodage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import com.ibm.icu.text.Collator
import com.ibm.icu.util.ULocale
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/*
 * Le même questionnaire doit donner le même résultat sur le site et
 * dans l'application. Sans ce test, ils divergeraient au premier
 * correctif appliqué d'un seul côté, et personne ne le saurait.
 *
 * outils/reference-du-point.mjs joue des centaines de parties avec le
 * code DU SITE (lancé par Gradle avant ce test, sur le site cloné à
 * côté). Ici, on rejoue les mêmes actions avec le portage, et on
 * compare après CHAQUE action : l'état, et tout ce que les écrans en
 * tirent. Puis les recommandations, à la fin.
 *
 * Un écart s'arrête au premier pas qui diffère, avec la graine : la
 * partie se rejoue à l'identique des deux côtés.
 */
class DerouleContreLeSiteTest {

    // Strict : un champ que le site ajouterait à son état sans que le
    // portage le connaisse doit faire échouer, pas passer inaperçu.
    private val strict = Json { ignoreUnknownKeys = false }

    // Le tri de `localeCompare(…, "fr")` : l'ICU, comme le navigateur
    // et comme Android.
    @Suppress("UNCHECKED_CAST")
    private val ordreDuNavigateur = Collator.getInstance(ULocale.FRENCH) as Comparator<String>

    private val pools = mutableMapOf<String, PoolDuPoint>()

    private fun pool(nom: String) = pools.getOrPut(nom) {
        decodage.decodeFromString(PoolDuPoint.serializer(), javaClass.getResource("/api/$nom")!!.readText())
    }

    @Test
    fun `le portage retrouve le site, action apres action`() {

        val chemin = System.getProperty("reference.du.point")
            ?: fail("Propriété reference.du.point absente : lancer par Gradle, qui produit la référence.")

        val parties = Json.parseToJsonElement(File(chemin).readText()).jsonObject["parties"]!!.jsonArray

        assertTrue(parties.size > 100, "trop peu de parties : ${parties.size}")

        var pas = 0

        for (partie in parties.map { it.jsonObject }) {
            pas += rejouer(partie)
        }

        // Que la comparaison ait vraiment porté sur quelque chose.
        assertTrue(pas > 5000, "trop peu d'actions jouées : $pas")
    }

    private fun rejouer(partie: JsonObject): Int {

        val nomPool = partie["pool"]!!.jsonPrimitive.content
        val graine = partie["graine"]!!.jsonPrimitive.int
        val pool = pool(nomPool)
        val d = Deroule(pool)

        var etat = d.choisirPublic(EtatDuPoint(), partie["audience"]!!.jsonPrimitive.content)

        val tousLesPas = partie["pas"]!!.jsonArray

        for ((n, pasJson) in tousLesPas.withIndex()) {

            val p = pasJson.jsonObject
            val action = p["action"]!!.jsonPrimitive.content
            val maintenant = p["maintenant"]!!.jsonPrimitive.long

            etat = jouer(d, etat, action, maintenant)

            val ou = "$nomPool, graine $graine, pas $n ($action)"

            val attendu = strict.decodeFromJsonElement<EtatDuPoint>(p["etat"]!!)
            assertEquals(attendu, etat, "état différent du site : $ou")

            assertEquals(p["derive"]!!, derive(d, etat), "écran différent du site : $ou")
        }

        val reconnues = d.situationsReconnues(etat.reponses)
        assertEquals(partie["recommandations"]!!, recommandations(pool, reconnues), "recommandations différentes : $nomPool, graine $graine")

        return tousLesPas.size
    }

    private fun jouer(d: Deroule, etat: EtatDuPoint, action: String, maintenant: Long): EtatDuPoint = when (action) {
        "commencer" -> d.commencer(etat)
        "oui", "non", "passer" -> d.repondre(
            etat,
            d.questionCourante(etat) ?: fail("pas de question à l'écran"),
            Reponse.valueOf(action.uppercase()),
            maintenant,
        )
        "precedent" -> d.precedent(etat) ?: etat
        "pause" -> d.pause(etat)
        "reprendre" -> d.reprendre(etat)
        "terminer" -> d.terminer(etat, maintenant)
        "reprendreApresSas" -> d.reprendreApresSas(etat)
        "revoirLesPositifs" -> d.revoirLesPositifs(etat)
        "revenirAuSas" -> d.revenirAuSas(etat)
        "apresAlerte" -> d.apresAlerte(etat, maintenant)
        else -> fail("action inconnue : $action")
    }

    // La même forme que `derive` dans outils/reference-du-point.mjs.
    private fun derive(d: Deroule, etat: EtatDuPoint): JsonElement {

        val reconnues = d.situationsReconnues(etat.reponses)

        return JsonObject(
            mapOf(
                "question" to (d.questionCourante(etat)?.id?.let(::JsonPrimitive) ?: JsonNull),
                "niveauAtteint" to (Deroule.niveauAtteint(reconnues)?.let(::JsonPrimitive) ?: JsonNull),
                "aidesDuResultat" to ids(d.aidesDuResultat(etat).map { it.id }),
                "aidesDAlerte" to ids(if (etat.phase == Phase.ALERTE) d.aidesDAlerte(etat).map { it.id } else emptyList()),
                "aidesReconnues" to ids(d.aidesReconnues(reconnues).map { it.id }),
                "reconnuesParNiveau" to JsonArray(
                    d.reconnuesParNiveau(reconnues).map { (n, s) -> JsonArray(listOf(JsonPrimitive(n.position), ids(s.map { it.id }))) }
                ),
                "formes" to JsonArray(
                    Deroule.formesReconnues(reconnues).map { (cle, combien) -> JsonArray(listOf(JsonPrimitive(cle), JsonPrimitive(combien))) }
                ),
            )
        )
    }

    private fun recommandations(pool: PoolDuPoint, reconnues: List<fr.cooplib.util.modeles.SituationDuPoint>): JsonElement {

        val r = recommander(pool, reconnues, ordreDuNavigateur)

        fun paires(liste: List<Pair<String, Int>>) =
            JsonArray(liste.map { (id, n) -> JsonArray(listOf(JsonPrimitive(id), JsonPrimitive(n))) })

        return JsonObject(
            mapOf(
                "violentometres" to paires(r.violentometres.map { it.element.id to it.recoupements }),
                "parcours" to paires(r.parcours.map { it.element.id to it.recoupements }),
                "recits" to paires(r.recits.map { it.element.id to it.recoupements }),
                "mecanismes" to paires(r.mecanismes.map { it.element.id to it.recoupements }),
            )
        )
    }

    private fun ids(liste: List<String>) = JsonArray(liste.map(::JsonPrimitive))
}
