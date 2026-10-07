package fr.cooplib.util.reseau

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AdresseApiTest {

    @Test
    fun `une adresse https se prend, sans sa barre finale`() {
        assertEquals("https://api.exemple.fr/api", AdresseApi.depuis(" https://api.exemple.fr/api/ ")?.base)
    }

    @Test
    fun `en clair, seulement vers la machine elle-meme`() {
        assertNull(AdresseApi.depuis("http://violentometres.fr/api"))
        assertEquals("http://localhost:8000", AdresseApi.depuis("http://localhost:8000")?.base)
        // L'hôte vu depuis l'émulateur Android.
        assertEquals("http://10.0.2.2:8000", AdresseApi.depuis("http://10.0.2.2:8000")?.base)
    }

    @Test
    fun `ni requete, ni fragment, ni n'importe quoi`() {
        assertNull(AdresseApi.depuis("https://exemple.fr/api?x=1"))
        assertNull(AdresseApi.depuis("https://exemple.fr/api#haut"))
        assertNull(AdresseApi.depuis("ftp://exemple.fr"))
        assertNull(AdresseApi.depuis("exemple.fr"))
        assertNull(AdresseApi.depuis(""))
    }
}
