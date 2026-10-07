package fr.cooplib.util.reseau

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IdentifiantVisiteurTest {

    @Test
    fun `un identifiant neuf respecte le contrat du serveur`() {
        repeat(100) {
            assertTrue(IdentifiantVisiteur.estValide(IdentifiantVisiteur.nouveau().valeur))
        }
    }

    @Test
    fun `deux identifiants neufs different`() {
        assertNotEquals(IdentifiantVisiteur.nouveau(), IdentifiantVisiteur.nouveau())
    }

    @Test
    fun `les bornes de longueur sont celles du serveur`() {
        assertNull(IdentifiantVisiteur.depuis("a".repeat(7)))
        assertEquals("a".repeat(8), IdentifiantVisiteur.depuis("a".repeat(8))?.valeur)
        assertEquals("a".repeat(64), IdentifiantVisiteur.depuis("a".repeat(64))?.valeur)
        assertNull(IdentifiantVisiteur.depuis("a".repeat(65)))
    }

    @Test
    fun `un identifiant abime est ecarte avant de partir`() {
        assertNull(IdentifiantVisiteur.depuis("abcdefgh "))
        assertNull(IdentifiantVisiteur.depuis("abcd_efgh"))
        assertNull(IdentifiantVisiteur.depuis("abcdéfgh"))
        assertNull(IdentifiantVisiteur.depuis(""))
    }
}
