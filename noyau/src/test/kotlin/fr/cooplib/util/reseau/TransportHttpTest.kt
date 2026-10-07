package fr.cooplib.util.reseau

import com.sun.net.httpserver.HttpServer
import java.io.IOException
import java.net.InetSocketAddress
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/*
 * Le vrai HttpURLConnection, contre un petit serveur sur la machine
 * (celui du JDK) : ce qu'un faux transport ne peut pas montrer. Les
 * accents à l'aller et au retour, le corps d'une erreur, qui n'est pas
 * là où l'on croit, et une redirection qui ne doit pas être suivie.
 */
class TransportHttpTest {

    private lateinit var serveur: HttpServer
    private var recu: Pair<Map<String, String>, String>? = null

    private val base get() = "http://127.0.0.1:${serveur.address.port}"

    @BeforeTest
    fun demarrer() {

        serveur = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)

        serveur.createContext("/echo") { e ->
            val corps = e.requestBody.readBytes().toString(Charsets.UTF_8)
            recu = e.requestHeaders.mapValues { it.value.joinToString() } to corps
            val sortie = """{"recu":"$corps"}""".toByteArray(Charsets.UTF_8)
            e.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
            e.sendResponseHeaders(200, sortie.size.toLong())
            e.responseBody.use { it.write(sortie) }
        }

        serveur.createContext("/refus") { e ->
            val sortie = """{"detail":"Le récit est obligatoire"}""".toByteArray(Charsets.UTF_8)
            e.sendResponseHeaders(400, sortie.size.toLong())
            e.responseBody.use { it.write(sortie) }
        }

        serveur.createContext("/stories") { e ->
            e.responseHeaders.add("Retry-After", "12")
            e.sendResponseHeaders(429, -1)
            e.close()
        }

        serveur.createContext("/ailleurs") { e ->
            e.responseHeaders.add("Location", "$base/echo")
            e.sendResponseHeaders(302, -1)
            e.close()
        }

        serveur.start()
    }

    @AfterTest
    fun arreter() = serveur.stop(0)

    @Test
    fun `les accents passent dans les deux sens`() {

        val r = TransportHttp().envoyer(
            Requete("POST", "$base/echo", mapOf("X-Visitor-Id" to "abcdefgh-1234"), "Ça s'est passé à l'été")
        )

        assertEquals(200, r.code)
        assertEquals("""{"recu":"Ça s'est passé à l'été"}""", r.corps)
        assertEquals("abcdefgh-1234", recu!!.first.entries.first { it.key.equals("X-Visitor-Id", true) }.value)
    }

    @Test
    fun `le corps d'une erreur est lu, pas perdu`() {
        val r = TransportHttp().envoyer(Requete("GET", "$base/refus"))
        assertEquals(400, r.code)
        assertEquals("""{"detail":"Le récit est obligatoire"}""", r.corps)
    }

    @Test
    fun `de bout en bout, un 429 donne l'attente du serveur`() {
        // L'en-tête traverse HttpURLConnection, qui en change la casse
        // selon les versions : le client doit le retrouver quand même.
        val client = ClientApi(AdresseApi.depuis(base)!!, TransportHttp())
        assertEquals(Resultat.Limite(12), client.deposerRecit("titre", "texte", false, IdentifiantVisiteur.nouveau()))
    }

    @Test
    fun `une redirection n'est pas suivie`() {
        val r = TransportHttp().envoyer(Requete("GET", "$base/ailleurs"))
        assertEquals(302, r.code)
        assertEquals(null, recu)
    }

    @Test
    fun `personne au bout, c'est une IOException`() {
        val port = serveur.address.port
        serveur.stop(0)
        assertFailsWith<IOException> { TransportHttp(delaiConnexionMs = 2000).envoyer(Requete("GET", "http://127.0.0.1:$port/echo")) }
        // Pour que @AfterTest n'arrête pas deux fois.
        serveur = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    }
}
