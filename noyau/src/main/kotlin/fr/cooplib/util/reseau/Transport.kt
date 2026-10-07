package fr.cooplib.util.reseau

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

/*
 * Ce qui part sur le réseau, réduit au strict nécessaire : le client
 * d'API ne connaît que cette interface. Les tests lui en donnent une
 * fausse, qui note ce qu'on lui demande ; l'application, la vraie,
 * plus bas.
 */
fun interface Transport {

    // Lève IOException quand le réseau manque : ni réponse, ni code.
    @Throws(IOException::class)
    fun envoyer(requete: Requete): ReponseHttp
}

data class Requete(
    val methode: String,
    val url: String,
    val entetes: Map<String, String> = emptyMap(),
    val corps: String? = null,
)

data class ReponseHttp(
    val code: Int,
    val corps: String,
    val entetes: Map<String, String> = emptyMap(),
)

/*
 * Le transport réel : HttpURLConnection, qu'Android et la JVM ont tous
 * les deux. Aucune bibliothèque : chacune en plus est une dépendance à
 * faire accepter par F-Droid, et à surveiller.
 *
 * Rien n'est ajouté à ce que la requête porte : ni cookie (aucun
 * CookieHandler n'est installé), ni cache disque, ni en-tête qui
 * nommerait l'application. L'agent de navigation reste celui du
 * système (« Dalvik/… » sur Android), le même que pour n'importe quelle
 * application.
 */
class TransportHttp(
    private val delaiConnexionMs: Int = 15_000,
    private val delaiLectureMs: Int = 30_000,
) : Transport {

    override fun envoyer(requete: Requete): ReponseHttp {

        val connexion = URI(requete.url).toURL().openConnection() as HttpURLConnection

        try {

            connexion.requestMethod = requete.methode
            connexion.connectTimeout = delaiConnexionMs
            connexion.readTimeout = delaiLectureMs
            connexion.useCaches = false
            // Une redirection mènerait ailleurs que là où l'on a choisi
            // d'aller : sous le nom neutre, vers violentometres.fr, et
            // c'est ce nom-là que le réseau lirait. Elle reste un refus.
            connexion.instanceFollowRedirects = false

            for ((nom, valeur) in requete.entetes) {
                connexion.setRequestProperty(nom, valeur)
            }

            if (requete.corps != null) {
                connexion.doOutput = true
                connexion.outputStream.use { it.write(requete.corps.toByteArray(Charsets.UTF_8)) }
            }

            val code = connexion.responseCode

            // Au-delà de 400, HttpURLConnection lève sur inputStream :
            // le corps de l'erreur (le « detail » de l'API, qu'on veut
            // montrer) est dans errorStream.
            val flux = if (code >= 400) connexion.errorStream else connexion.inputStream

            val corps = flux?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""

            val entetes = connexion.headerFields
                .filterKeys { it != null }
                .mapValues { it.value.joinToString(", ") }

            return ReponseHttp(code, corps, entetes)

        } finally {
            connexion.disconnect()
        }
    }
}
