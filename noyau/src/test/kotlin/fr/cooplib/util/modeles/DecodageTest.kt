package fr.cooplib.util.modeles

import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/*
 * Sur de vraies réponses de l'API, enregistrées par
 * outils/capturer-exemples.sh. Les nombres attendus sont ceux du
 * contenu de départ : s'ils changent après une nouvelle capture, c'est
 * le contenu qui a bougé, pas forcément le code.
 */
class DecodageTest {

    private inline fun <reified T> lire(nom: String): T {
        val texte = javaClass.getResource("/api/$nom")!!.readText()
        return decodage.decodeFromString(texte)
    }

    @Test
    fun `la liste des violentometres se lit en entier`() {
        val liste = lire<List<ViolentometreResume>>("violentometres.json")
        assertEquals(63, liste.size)
        assertTrue(liste.all { it.titre.isNotBlank() && it.niveaux.isNotEmpty() })
    }

    @Test
    fun `un violentometre entier porte ses situations`() {
        val vm = lire<Violentometre>("violentometre.json")
        assertTrue(vm.situations.isNotEmpty())
        val positions = vm.niveaux.map { it.position }.toSet()
        assertTrue(vm.situations.all { it.gravite in positions })
    }

    @Test
    fun `le point general a tout ce qu'il faut pour recommander`() {
        val pool = lire<PoolDuPoint>("point-orientation.json")
        assertEquals("orientation", pool.type)
        assertTrue(pool.situations.isNotEmpty())
        assertTrue(pool.parcours.isNotEmpty())
        assertTrue(pool.situations.any { it.violentometreIds.isNotEmpty() })
    }

    @Test
    fun `le point sur un violentometre se lit malgre ses champs absents`() {
        // Ni story_ids dans les situations, ni parcours, ni récits.
        val pool = lire<PoolDuPoint>("point-violentometre.json")
        assertEquals("violentometer", pool.type)
        assertTrue(pool.situations.isNotEmpty())
        assertTrue(pool.situations.all { it.recitIds.isEmpty() })
        assertTrue(pool.parcours.isEmpty())
    }

    @Test
    fun `un parcours se lit avec ses etapes`() {
        val parcours = lire<Parcours>("un-parcours.json")
        assertTrue(parcours.etapes.isNotEmpty())
        assertTrue(parcours.etapes.all { it.cible != null })
        assertEquals(parcours.etapes.sortedBy { it.position }, parcours.etapes)
    }

    @Test
    fun `les parcours, les recits, les aides et l'historique se lisent`() {
        assertEquals(20, lire<List<ParcoursResume>>("parcours.json").size)
        assertTrue(lire<List<Recit>>("recits.json").isNotEmpty())
        assertEquals(13, lire<List<Aide>>("aides.json").size)
        lire<Historique>("historique.json")
    }

    @Test
    fun `un champ inconnu ou un null inattendu ne casse rien`() {
        // L'application installée ne suit pas l'API le jour où elle change.
        val aide = decodage.decodeFromString<Aide>(
            """{"id":"a","title":"3919","phone":null,"kind":null,"min_level":null,"nouveau":{"x":1}}"""
        )
        assertNull(aide.telephone)
        assertEquals("", aide.sorte)
        assertEquals(0, aide.niveauMinimum)
    }
}
