package fr.cooplib.util.reseau

import fr.cooplib.util.modeles.Aide
import fr.cooplib.util.modeles.Cadre
import fr.cooplib.util.modeles.CadreResume
import fr.cooplib.util.modeles.Mecanisme
import fr.cooplib.util.modeles.MecanismeResume
import fr.cooplib.util.modeles.Historique
import fr.cooplib.util.modeles.Parcours
import fr.cooplib.util.modeles.ParcoursResume
import fr.cooplib.util.modeles.PoolDuPoint
import fr.cooplib.util.modeles.Proches
import fr.cooplib.util.modeles.Recit
import fr.cooplib.util.modeles.Violentometre
import fr.cooplib.util.modeles.ViolentometreResume
import fr.cooplib.util.modeles.decodage
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.io.IOException
import java.net.URLEncoder
import java.util.UUID

/*
 * L'API, vue de l'application : tout ce qu'elle lit, et les deux seules
 * choses qu'elle écrit, déposer un récit et signaler.
 *
 * LES LECTURES NE PORTENT AUCUN IDENTIFIANT. Le site joint le sien à
 * certaines (les vues comptées) ; l'application, jamais : sinon le
 * serveur pourrait relier ce qu'on lit à ce qu'on a écrit. Elle ne
 * compte pas non plus les vues ni les points faits (`/done`) : chaque
 * requête est une trace sur le réseau, celles-là ne servent à rien à
 * la personne.
 */
class ClientApi(
    val adresse: AdresseApi,
    private val transport: Transport = TransportHttp(),
) {

    // ---------------------------------------------------------
    // Lire
    // ---------------------------------------------------------

    fun violentometres() = lire("/violentometers", ListSerializer(ViolentometreResume.serializer()))

    fun violentometre(id: String) = lire("/violentometers/${segment(id)}", Violentometre.serializer())

    // Ce qui lui ressemble, calculé par le serveur (voir modeles/Proches.kt).
    fun proches(id: String) = lire("/violentometers/${segment(id)}/related", Proches.serializer())

    fun parcours() = lire("/parcours", ListSerializer(ParcoursResume.serializer()))

    fun unParcours(id: String) = lire("/parcours/${segment(id)}", Parcours.serializer())

    fun recits() = lire("/stories", ListSerializer(Recit.serializer()))

    fun aides() = lire("/aides", ListSerializer(Aide.serializer()))

    fun cadres() = lire("/cadres", ListSerializer(CadreResume.serializer()))

    // Toutes les situations, pour « Mon violentomètre » hors ligne.
    fun bibliotheque() = lire("/situations/bibliotheque", ListSerializer(fr.cooplib.util.modeles.SituationDeBibliotheque.serializer()))

    fun contextes() = lire("/contexts", ListSerializer(fr.cooplib.util.modeles.ContexteDuSite.serializer()))

    fun cadre(id: String) = lire("/cadres/${segment(id)}", Cadre.serializer())

    fun mecanismes() = lire("/mecanismes", ListSerializer(MecanismeResume.serializer()))

    fun mecanisme(id: String) = lire("/mecanismes/${segment(id)}", Mecanisme.serializer())

    // `type` : « orientation » (avec l'identifiant « tout »), ou
    // « violentometer », « parcours », « context ».
    fun point(type: String, id: String) = lire("/tests/${segment(type)}/${segment(id)}", PoolDuPoint.serializer())

    // La sonde de la synchronisation : `limite` = 1 suffit à savoir si
    // quelque chose a bougé (quelques centaines d'octets).
    fun historique(limite: Int) = lire("/history/recent?limit=${limite.coerceIn(1, 100)}", Historique.serializer())

    fun motifsDeSignalement(): Resultat<List<Motif>> =
        when (val r = lire("/signalements/motifs", Motifs.serializer())) {
            is Resultat.Ok -> Resultat.Ok(r.valeur.items)
            is Resultat.Limite -> r
            is Resultat.Refuse -> r
            is Resultat.Injoignable -> r
        }

    // ---------------------------------------------------------
    // Écrire
    // ---------------------------------------------------------

    /*
     * Déposer un récit. Il entre dans un contenu PUBLIC et VERSIONNÉ :
     * l'écran qui y mène porte les avertissements du site (ne jamais
     * nommer une personne réelle, le choix du flou), et ce qui part ici
     * ne se reprend pas.
     *
     * JAMAIS RENVOYÉ AUTOMATIQUEMENT. Une coupure après que le serveur
     * l'a enregistré, suivie d'un nouvel essai, le publierait deux
     * fois — et rien ne se supprime sur ce site. Sur Injoignable,
     * l'écran dit « peut-être pas envoyé » et laisse la personne
     * vérifier dans les récits avant de renvoyer.
     */
    fun deposerRecit(
        titre: String,
        texte: String,
        sansFlou: Boolean,
        visiteur: IdentifiantVisiteur,
    ): Resultat<Recit> {

        // Le serveur refuserait ; autant le dire sans rien envoyer.
        if (titre.isBlank()) return Resultat.Refuse(400, "Le titre est obligatoire")
        if (texte.isBlank()) return Resultat.Refuse(400, "Le récit est obligatoire")

        val corps = buildJsonObject {
            put("title", titre.trim())
            put("text", texte.trim())
            put("safe", sansFlou)
        }

        // Une session par dépôt : elle regroupe les modifications en une
        // version d'historique. Un UUID passe le contrôle du serveur,
        // qui ignore SANS LE DIRE une session mal formée.
        val entetes = mapOf(
            "X-Visitor-Id" to visiteur.valeur,
            "X-Edit-Session" to UUID.randomUUID().toString(),
        )

        return envoyer(Requete("POST", url("/stories"), JSON + entetes, corps.toString()), Recit.serializer())
    }

    /*
     * Signaler un récit. Obligatoire dès qu'on accepte du contenu
     * d'utilisateurs (règle de Google), et utile pour de vrai : c'est le
     * seul recours de qui se reconnaît dans un récit.
     *
     * Un signalement ne cache rien et n'est pas public (voir
     * app/routes/signalements.py, côté API). L'identifiant sert au
     * serveur à ne compter qu'une fois deux signalements de la même
     * personne.
     */
    fun signalerRecit(recitId: String, motif: String, texte: String?, visiteur: IdentifiantVisiteur): Resultat<Unit> {

        val corps = buildJsonObject {
            put("motif", motif)
            texte?.trim()?.takeIf { it.isNotEmpty() }?.let { put("texte", it) }
        }

        val chemin = "/signalements/story/${segment(recitId)}?visitor_id=${requeteEncodee(visiteur.valeur)}"

        val requete = Requete("POST", url(chemin), JSON + ("X-Visitor-Id" to visiteur.valeur), corps.toString())

        return when (val r = envoyerBrut(requete)) {

            // Le serveur répond 200 même quand il n'a rien enregistré
            // (`{"signale": false}` pour un type qu'il ne connaît pas) :
            // on lit la réponse, on ne se fie pas au code.
            is Resultat.Ok -> {
                val signale = runCatching {
                    decodage.parseToJsonElement(r.valeur).jsonObject["signale"]?.let { (it as JsonPrimitive).boolean }
                }.getOrNull()
                if (signale == true) Resultat.Ok(Unit) else Resultat.Refuse(200, "Le signalement n'a pas été enregistré")
            }

            is Resultat.Limite -> r
            is Resultat.Refuse -> r
            is Resultat.Injoignable -> r
        }
    }

    /*
     * Aimer un violentomètre ou un parcours, ou ne plus l'aimer : la
     * TROISIÈME écriture de l'application, décidée par Cooplib le 7 octobre
     * 2026 (notes de conception), « comme sur l'original ».
     *
     * Elle porte l'identifiant, comme les deux autres, et le serveur n'en
     * garde qu'une empreinte par élément. Idempotente des deux côtés :
     * aimer deux fois ne compte qu'une fois, retirer un like absent ne
     * fait rien. On peut donc réessayer sans risque, à la différence d'un
     * récit.
     *
     * L'état « aimé » n'est PAS demandé au serveur (`GET …/like`) : ce
     * serait une lecture portant l'identifiant, et les lectures n'en
     * portent jamais. L'application se souvient elle-même de ce qu'elle a
     * aimé.
     */
    fun aimerViolentometre(id: String, aime: Boolean, visiteur: IdentifiantVisiteur) =
        aimer("/violentometers/${segment(id)}/like", aime, visiteur)

    fun aimerParcours(id: String, aime: Boolean, visiteur: IdentifiantVisiteur) =
        aimer("/parcours/${segment(id)}/like", aime, visiteur)

    private fun aimer(chemin: String, aime: Boolean, visiteur: IdentifiantVisiteur): Resultat<EtatDuLike> {
        val entetes = JSON + ("X-Visitor-Id" to visiteur.valeur)
        val requete = if (aime) {
            Requete("POST", url(chemin), entetes, buildJsonObject { put("visitor_id", visiteur.valeur) }.toString())
        } else {
            Requete("DELETE", url("$chemin?visitor_id=${requeteEncodee(visiteur.valeur)}"), entetes)
        }
        return envoyer(requete, EtatDuLike.serializer())
    }

    // ---------------------------------------------------------
    // La mécanique
    // ---------------------------------------------------------

    private fun url(chemin: String) = "${adresse.base}$chemin"

    private fun <T> lire(chemin: String, lecteur: KSerializer<T>): Resultat<T> =
        envoyer(Requete("GET", url(chemin), mapOf("Accept" to "application/json")), lecteur)

    private fun <T> envoyer(requete: Requete, lecteur: KSerializer<T>): Resultat<T> =
        when (val r = envoyerBrut(requete)) {
            is Resultat.Ok -> try {
                Resultat.Ok(decodage.decodeFromString(lecteur, r.valeur))
            } catch (e: IllegalArgumentException) {
                // Une réponse qu'on ne sait pas lire : pour l'écran, c'est
                // comme pas de réponse. SerializationException en hérite.
                Resultat.Injoignable("réponse illisible : ${e.message?.take(200)}")
            }
            is Resultat.Limite -> r
            is Resultat.Refuse -> r
            is Resultat.Injoignable -> r
        }

    // Le corps tel quel, une fois le code de réponse lu.
    private fun envoyerBrut(requete: Requete): Resultat<String> {

        val reponse = try {
            transport.envoyer(requete)
        } catch (e: IOException) {
            return Resultat.Injoignable(e.message ?: e.javaClass.simpleName)
        }

        return when (reponse.code) {
            429 -> Resultat.Limite(attente(reponse))
            in 200..299 -> Resultat.Ok(reponse.corps)
            else -> Resultat.Refuse(reponse.code, detail(reponse.corps))
        }
    }

    // Retry-After, en secondes ; une minute si le serveur ne le dit pas
    // (c'est la fenêtre de sa cadence).
    private fun attente(reponse: ReponseHttp): Int {
        val valeur = reponse.entetes.entries.firstOrNull { it.key.equals("Retry-After", ignoreCase = true) }?.value
        return valeur?.trim()?.toIntOrNull()?.coerceAtLeast(1) ?: 60
    }

    // FastAPI répond `{"detail": "…"}`, ou une liste pour une erreur de
    // validation : on ne garde que le texte, quand il y en a un.
    private fun detail(corps: String): String? = runCatching {
        (decodage.parseToJsonElement(corps).jsonObject["detail"] as? JsonPrimitive)?.takeIf { it.isString }?.content
    }.getOrNull()

    companion object {

        private val JSON = mapOf(
            "Accept" to "application/json",
            "Content-Type" to "application/json; charset=utf-8",
        )

        private val SEGMENT = Regex("^[A-Za-z0-9_-]{1,100}$")

        /*
         * Un identifiant dans un chemin : des UUID et des types connus,
         * rien d'autre. Un identifiant venu d'un stockage abîmé, avec un
         * « / » ou un « ? », changerait la route appelée sans que rien
         * ne le dise.
         */
        private fun segment(id: String): String {
            require(SEGMENT.matches(id)) { "identifiant invalide dans un chemin : $id" }
            return id
        }

        private fun requeteEncodee(texte: String) = URLEncoder.encode(texte, Charsets.UTF_8)
    }
}

@Serializable
data class Motif(val cle: String, val titre: String)

@Serializable
data class EtatDuLike(
    @SerialName("liked") val aime: Boolean = false,
    @SerialName("like_count") val combien: Int = 0,
)

@Serializable
private data class Motifs(val items: List<Motif> = emptyList())
