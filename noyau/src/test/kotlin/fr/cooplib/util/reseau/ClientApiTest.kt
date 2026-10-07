package fr.cooplib.util.reseau

import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import fr.cooplib.util.modeles.decodage
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClientApiTest {

    // Note tout ce qu'on lui demande, répond ce qu'on lui dit.
    private class FauxTransport(var repondre: (Requete) -> ReponseHttp = { ReponseHttp(200, "[]") }) : Transport {
        val requetes = mutableListOf<Requete>()
        override fun envoyer(requete: Requete): ReponseHttp {
            requetes += requete
            return repondre(requete)
        }
    }

    private val transport = FauxTransport()
    private val client = ClientApi(AdresseApi.PRODUCTION, transport)
    private val moi = IdentifiantVisiteur.nouveau()

    private fun exemple(nom: String) = javaClass.getResource("/api/$nom")!!.readText()

    // Le contrôle que fait le serveur sur X-Edit-Session : hors de là,
    // il l'ignore sans le dire.
    private val formeServeur = Regex("^[A-Za-z0-9-]{8,64}$")

    @Test
    fun `aucune lecture ne porte d'identifiant`() {

        client.violentometres()
        client.violentometre("a3f2-1")
        client.parcours()
        client.unParcours("p-1")
        client.recits()
        client.aides()
        client.point("orientation", "tout")
        client.historique(1)
        client.motifsDeSignalement()

        assertEquals(9, transport.requetes.size)

        for (r in transport.requetes) {
            assertEquals("GET", r.methode)
            assertTrue(r.entetes.keys.none { it.startsWith("X-", ignoreCase = true) }, "en-tête d'identité sur ${r.url}")
            assertFalse("visitor" in r.url, "identifiant dans ${r.url}")
        }
    }

    @Test
    fun `une lecture decode la vraie reponse de l'API`() {

        transport.repondre = { ReponseHttp(200, exemple("violentometres.json")) }

        val r = client.violentometres()

        assertIs<Resultat.Ok<*>>(r)
        assertEquals(63, (r.valeur as List<*>).size)
        assertEquals("https://violentometres.fr/api/violentometers", transport.requetes.single().url)
    }

    @Test
    fun `deposer un recit porte l'identifiant et une session neuve, que le serveur accepte`() {

        transport.repondre = { ReponseHttp(200, """{"id":"r-1","title":"Un soir","text":"…","safe":false}""") }

        val r = client.deposerRecit("  Un soir  ", " Il a fallu du temps. ", sansFlou = true, visiteur = moi)

        assertIs<Resultat.Ok<*>>(r)

        val requete = transport.requetes.single()
        assertEquals("POST", requete.methode)
        assertEquals("https://violentometres.fr/api/stories", requete.url)
        assertEquals(moi.valeur, requete.entetes["X-Visitor-Id"])
        assertTrue(formeServeur.matches(requete.entetes["X-Edit-Session"]!!))

        val corps = decodage.parseToJsonElement(requete.corps!!).jsonObject
        assertEquals("Un soir", corps["title"]!!.jsonPrimitive.content)
        assertEquals("Il a fallu du temps.", corps["text"]!!.jsonPrimitive.content)
        assertTrue(corps["safe"]!!.jsonPrimitive.boolean)

        client.deposerRecit("Un autre", "texte", sansFlou = false, visiteur = moi)
        assertNotEquals(transport.requetes[0].entetes["X-Edit-Session"], transport.requetes[1].entetes["X-Edit-Session"])
    }

    @Test
    fun `un recit sans titre ou sans texte ne part pas`() {

        assertEquals(Resultat.Refuse(400, "Le titre est obligatoire"), client.deposerRecit("  ", "texte", false, moi))
        assertEquals(Resultat.Refuse(400, "Le récit est obligatoire"), client.deposerRecit("titre", "\n", false, moi))
        assertTrue(transport.requetes.isEmpty())
    }

    @Test
    fun `une coupure pendant un depot n'est jamais renvoyee`() {

        // Le serveur a peut-être enregistré : renvoyer publierait deux fois.
        transport.repondre = { throw IOException("connexion perdue") }

        assertIs<Resultat.Injoignable>(client.deposerRecit("titre", "texte", false, moi))
        assertEquals(1, transport.requetes.size)
    }

    @Test
    fun `un 429 est une attente, pas une panne`() {

        transport.repondre = { ReponseHttp(429, """{"detail":"Trop de modifications"}""", mapOf("retry-after" to "17")) }
        assertEquals(Resultat.Limite(17), client.deposerRecit("titre", "texte", false, moi))

        transport.repondre = { ReponseHttp(429, "") }
        assertEquals(Resultat.Limite(60), client.deposerRecit("titre", "texte", false, moi))
    }

    @Test
    fun `un refus garde le message du serveur quand il en donne un`() {

        transport.repondre = { ReponseHttp(400, """{"detail":"Identifiant de visiteur invalide"}""") }
        assertEquals(Resultat.Refuse(400, "Identifiant de visiteur invalide"), client.recits())

        // Erreur de validation : une liste, pas un texte à montrer.
        transport.repondre = { ReponseHttp(422, """{"detail":[{"loc":["body"],"msg":"field required"}]}""") }
        assertEquals(Resultat.Refuse(422, null), client.recits())

        transport.repondre = { ReponseHttp(502, "<html>Bad Gateway</html>") }
        assertEquals(Resultat.Refuse(502, null), client.recits())

        // Une redirection n'est pas suivie : elle mènerait sous un autre nom.
        transport.repondre = { ReponseHttp(301, "", mapOf("Location" to "https://ailleurs/")) }
        assertEquals(Resultat.Refuse(301, null), client.recits())
    }

    @Test
    fun `une reponse illisible se dit comme une absence de reseau`() {

        transport.repondre = { ReponseHttp(200, "<html>portail captif</html>") }
        assertIs<Resultat.Injoignable>(client.violentometres())

        transport.repondre = { throw IOException("pas de réseau") }
        assertIs<Resultat.Injoignable>(client.violentometres())
    }

    @Test
    fun `signaler un recit, et ne pas croire un 200 qui n'a rien enregistre`() {

        transport.repondre = { ReponseHttp(200, """{"signale":true}""") }

        assertEquals(Resultat.Ok(Unit), client.signalerRecit("r-1", "identifiant", "  On me reconnaît.  ", moi))

        val requete = transport.requetes.single()
        assertEquals("https://violentometres.fr/api/signalements/story/r-1?visitor_id=${moi.valeur}", requete.url)
        val corps = decodage.parseToJsonElement(requete.corps!!).jsonObject
        assertEquals("identifiant", corps["motif"]!!.jsonPrimitive.content)
        assertEquals("On me reconnaît.", corps["texte"]!!.jsonPrimitive.content)

        // Un texte vide ne part pas comme une chaîne vide.
        client.signalerRecit("r-1", "autre", "   ", moi)
        assertNull(decodage.parseToJsonElement(transport.requetes[1].corps!!).jsonObject["texte"])

        transport.repondre = { ReponseHttp(200, """{"signale":false}""") }
        assertIs<Resultat.Refuse>(client.signalerRecit("r-1", "autre", null, moi))
    }

    @Test
    fun `un identifiant abime ne change pas la route appelee`() {

        assertFailsWith<IllegalArgumentException> { client.violentometre("../admin") }
        assertFailsWith<IllegalArgumentException> { client.signalerRecit("r-1?x=1", "autre", null, moi) }
        assertTrue(transport.requetes.isEmpty())
    }
}
