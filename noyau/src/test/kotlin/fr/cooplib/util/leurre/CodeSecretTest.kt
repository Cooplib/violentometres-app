package fr.cooplib.util.leurre

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CodeSecretTest {

    @Test
    fun `des chiffres seulement, de 4 a 12`() {
        assertTrue(CodeSecret.valide("2580"))
        assertTrue(CodeSecret.valide("012345678901"))
        assertFalse(CodeSecret.valide("123"))
        assertFalse(CodeSecret.valide("1234567890123"))
        assertFalse(CodeSecret.valide("12+34"))
        assertFalse(CodeSecret.valide("12,34"))
    }

    @Test
    fun `le bon code ouvre, un autre non`() {
        val e = CodeSecret.empreinte("2580")
        assertTrue(CodeSecret.verifie("2580", e))
        assertFalse(CodeSecret.verifie("2581", e))
        assertFalse(CodeSecret.verifie("02580", e))
        // Le même nombre, mais calculé : ce n'est pas le code tapé.
        assertFalse(CodeSecret.verifie("2000+580", e))
    }

    @Test
    fun `jamais en clair, et deux poses du meme code different`() {
        val a = CodeSecret.empreinte("2580")
        val b = CodeSecret.empreinte("2580")
        assertFalse("2580" in a)
        assertNotEquals(a, b)
    }

    @Test
    fun `une empreinte abimee n'ouvre rien`() {
        assertFalse(CodeSecret.verifie("2580", ""))
        assertFalse(CodeSecret.verifie("2580", "1\$x\$y\$z"))
        assertFalse(CodeSecret.verifie("2580", "2\$10000\$AAAA\$AAAA"))
    }
}
