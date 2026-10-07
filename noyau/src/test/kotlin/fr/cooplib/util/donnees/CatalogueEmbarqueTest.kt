package fr.cooplib.util.donnees

import fr.cooplib.util.point.Deroule
import kotlinx.serialization.json.Json
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/*
 * Le catalogue qui part dans l'APK. Ces tests ne disent pas que le
 * contenu est bon (c'est le travail du site), ils disent que
 * l'application saura s'en servir sans réseau : chaque entrée a de quoi
 * s'afficher, chaque renvoi mène quelque part.
 *
 * Un échec ici après `:fabrique:catalogue`, c'est presque toujours le
 * contenu qui a bougé côté site. Le regarder avant de corriger le test.
 */
class CatalogueEmbarqueTest {

    private val catalogue = Catalogue.embarque()

    @Test
    fun `il se relit sans rien perdre`() {
        // Strict : la fabrique n'écrit que ce que les modèles connaissent.
        val texte = javaClass.getResource(Catalogue.RESSOURCE)!!.readText()
        Json { ignoreUnknownKeys = false }.decodeFromString(Catalogue.serializer(), texte)
        assertEquals(Catalogue.FORMAT, catalogue.format)
        Instant.parse(catalogue.fabriqueLe)
    }

    @Test
    fun `les listes sont triees, pour que les diffs disent ce qui a change`() {
        for (ids in listOf(
            catalogue.aides.map { it.id },
            catalogue.violentometres.map { it.id },
            catalogue.parcours.map { it.id },
            catalogue.recits.map { it.id },
            catalogue.cadres.map { it.id },
            catalogue.mecanismes.map { it.id },
            catalogue.pointsParViolentometre.keys.toList(),
        )) {
            assertEquals(ids.sorted(), ids)
        }
    }

    @Test
    fun `chaque entree a de quoi s'afficher hors ligne`() {
        assertTrue(catalogue.aides.isNotEmpty(), "Trouver de l'aide")
        assertTrue(catalogue.violentometres.isNotEmpty() && catalogue.pointGeneral.situations.isNotEmpty(), "Faire le point")
        assertTrue(catalogue.recits.isNotEmpty(), "Des récits")
        assertTrue(catalogue.parcours.all { it.etapes.isNotEmpty() }, "Comprendre")
    }

    @Test
    fun `chaque violentometre a son point, et chaque point se deroule`() {

        assertEquals(catalogue.violentometres.map { it.id }.toSet(), catalogue.pointsParViolentometre.keys)

        for ((id, pool) in catalogue.pointsParViolentometre + ("orientation" to catalogue.pointGeneral)) {
            assertTrue(Deroule(pool).niveauxProposes.isNotEmpty(), "le point sur $id n'a aucun niveau à proposer")
        }
    }

    @Test
    fun `chaque etape de parcours mene a quelque chose du catalogue`() {

        val vms = catalogue.violentometres.map { it.id }.toSet()
        val cadres = catalogue.cadres.map { it.id }.toSet()
        val mecanismes = catalogue.mecanismes.map { it.id }.toSet()

        for (p in catalogue.parcours) {
            for (e in p.etapes) {
                val ou = "« ${p.titre} », étape ${e.position} (${e.type})"
                e.violentometreId?.let { assertTrue(it in vms, "$ou : violentomètre absent") }
                e.cadreId?.let { assertTrue(it in cadres, "$ou : cadre absent") }
                e.mecanismeId?.let { assertTrue(it in mecanismes, "$ou : mécanisme absent") }
                assertTrue(e.cible != null, "$ou : pas de quoi faire la carte")
            }
        }
    }

    @Test
    fun `une aide dit comment la joindre`() {
        // Un numéro, un site, ou à défaut le chemin en clair. Certaines
        // n'ont ni l'un ni l'autre par nature : le référent harcèlement
        // d'un établissement, la médecine du travail. Celles-là se
        // trouvent sur place, et c'est leur `contact` qui le dit.
        val muettes = catalogue.aides.filter {
            it.telephone.isNullOrBlank() && it.url.isNullOrBlank() && it.contact.isNullOrBlank()
        }
        assertTrue(muettes.isEmpty(), "aides sans aucun moyen de les joindre : ${muettes.map { it.titre }}")
    }

    @Test
    fun `le plus recent des deux, et jamais un catalogue d'un autre format`() {
        val ancien = catalogue.copy(fabriqueLe = "2026-01-01T00:00:00Z")
        val recent = catalogue.copy(fabriqueLe = "2027-01-01T00:00:00Z")
        assertEquals(recent, Catalogue.plusRecent(embarque = ancien, garde = recent))
        assertEquals(recent, Catalogue.plusRecent(embarque = recent, garde = ancien))
        assertEquals(ancien, Catalogue.plusRecent(embarque = ancien, garde = null))
        assertEquals(ancien, Catalogue.plusRecent(embarque = ancien, garde = recent.copy(format = 99)))
    }
}
