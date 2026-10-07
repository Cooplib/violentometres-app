package fr.cooplib.util.stockage

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import fr.cooplib.util.donnees.Bilan
import fr.cooplib.util.donnees.Catalogue
import fr.cooplib.util.donnees.Decision
import fr.cooplib.util.donnees.EtatDeSynchro
import fr.cooplib.util.donnees.Issue
import fr.cooplib.util.donnees.Synchronisation
import fr.cooplib.util.modeles.decodage
import fr.cooplib.util.reseau.AdresseApi
import fr.cooplib.util.reseau.ClientApi
import fr.cooplib.util.reseau.Resultat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant

/*
 * Le catalogue que voient les écrans, et sa mise à jour.
 *
 * Gardé sur le téléphone dans un fichier de l'application (exclu de la
 * sauvegarde du compte et du transfert, voir le manifeste), sous un nom
 * muet. Écrit à côté puis renommé : une coupure au milieu laisse
 * l'ancien, jamais un fichier tronqué.
 *
 * Les synchronisations vivent ici et pas dans un écran : quitter
 * l'application en pleine mise à jour ne doit pas laisser une moitié de
 * catalogue. Elle se termine, et le résultat est gardé.
 *
 * Un seul dépôt par processus (`de`), partagé par les écrans.
 */
class Depot private constructor(contexte: Context) {

    private val app = contexte.applicationContext
    private val fichier = File(app.filesDir, "c.json")
    private val etats = app.getSharedPreferences("s", Context.MODE_PRIVATE)
    private val portee = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // L'adresse de l'API. Un second nom d'hôte, neutre, est prévu
    // (APPLICATION-ANDROID.md, chapitre 5) : il se changera ici.
    private val api = ClientApi(AdresseApi.PRODUCTION)

    private val _catalogue = MutableStateFlow<Catalogue?>(null)
    val catalogue: StateFlow<Catalogue?> = _catalogue

    private val _enCours = MutableStateFlow(false)
    val enCours: StateFlow<Boolean> = _enCours

    // Ce que la dernière mise à jour a donné, à dire en clair.
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    // Une mise à jour est due, mais la connexion est facturée au volume :
    // l'écran demande, il ne décide pas.
    private val _aDemander = MutableStateFlow(false)
    val aDemander: StateFlow<Boolean> = _aDemander

    init {
        portee.launch { _catalogue.value = charger() }
    }

    private fun charger(): Catalogue {
        val embarque = Catalogue.embarque()
        val garde = runCatching { Catalogue.lire(fichier.readText()) }.getOrNull()
        val choisi = Catalogue.plusRecent(embarque, garde)
        /*
         * L'APK a gagné : son catalogue vaut un rechargement complet fait
         * le jour de sa fabrication. On n'efface PAS la date de la dernière
         * tentative : remise à zéro à chaque lancement (la première
         * version le faisait, faute de fichier gardé), elle aurait fait
         * partir une synchronisation à chaque ouverture.
         */
        if (choisi === embarque) {
            if (garde != null) fichier.delete()
            val e = lireEtat(embarque)
            if (Instant.parse(e.dernierComplet).isBefore(Instant.parse(embarque.fabriqueLe))) {
                ecrireEtat(e.copy(dernierComplet = embarque.fabriqueLe))
            }
        }
        return choisi
    }

    private fun lireEtat(c: Catalogue): EtatDeSynchro =
        etats.getString("etat", null)
            ?.let { runCatching { decodage.decodeFromString(EtatDeSynchro.serializer(), it) }.getOrNull() }
            ?: Synchronisation.depart(c)

    private fun ecrireEtat(e: EtatDeSynchro) {
        etats.edit().putString("etat", decodage.encodeToString(EtatDeSynchro.serializer(), e)).commit()
    }

    private fun garder(c: Catalogue) {
        val provisoire = File(fichier.parentFile, "c.json.provisoire")
        provisoire.writeText(decodage.encodeToString(Catalogue.serializer(), c))
        provisoire.renameTo(fichier)
    }

    /*
     * `demandee` : la personne a appuyé sur « mettre à jour ». Sinon,
     * c'est l'ouverture de l'application, et la synchronisation ne part
     * que si elle est due (au plus tous les trois jours), jamais seule sur
     * une connexion facturée, et jamais sans réseau : sans réseau, la
     * tentative ne compte pas, on réessaiera à la prochaine ouverture.
     */
    fun synchroniser(demandee: Boolean) {

        if (_enCours.value) return

        val c = _catalogue.value ?: return

        val reseau = reseau()

        if (reseau == null) {
            if (demandee) _message.value = "Pas de réseau : rien n'a changé. Le contenu sur ce téléphone reste utilisable."
            return
        }

        _enCours.value = true
        _aDemander.value = false

        portee.launch {
            try {
                val bilan = Synchronisation.synchroniser(api, c, lireEtat(c), Instant.now(), demandee, facturee = reseau.facturee)
                appliquer(bilan, demandee)
            } finally {
                _enCours.value = false
            }
        }
    }

    /*
     * Un récit que la personne vient de déposer : ajouté tout de suite au
     * catalogue gardé, sans requête de plus. La prochaine synchronisation
     * le relira de toute façon, avec le reste.
     */
    fun ajouterRecit(recit: fr.cooplib.util.modeles.Recit) {
        val c = _catalogue.value ?: return
        val suivant = c.copy(recits = (c.recits.filter { it.id != recit.id } + recit).sortedBy { it.id })
        _catalogue.value = suivant
        portee.launch { garder(suivant) }
    }

    /*
     * L'identité d'un contributeur, pour l'API : un identifiant neuf à
     * chaque lancement, jamais gardé. L'application n'a pas de « mes
     * récits » à retrouver ; le garder ne servirait à rien d'autre qu'à
     * relier entre eux, côté serveur, les récits et signalements venus
     * de ce téléphone.
     */
    private val visiteur = fr.cooplib.util.reseau.IdentifiantVisiteur.nouveau()

    /*
     * Les deux seules écritures. Lancées dans la portée du dépôt, pas de
     * l'écran : quitter l'application juste après « publier » (elle se
     * reverrouille) ne doit pas laisser un récit déposé sans que le
     * catalogue le sache.
     */
    suspend fun deposer(titre: String, texte: String, sansFlou: Boolean) =
        portee.async {
            api.deposerRecit(titre, texte, sansFlou, visiteur).also { r ->
                if (r is Resultat.Ok) ajouterRecit(r.valeur)
            }
        }.await()

    suspend fun motifs() = portee.async { api.motifsDeSignalement() }.await()

    suspend fun signaler(recitId: String, motif: String, texte: String?) =
        portee.async { api.signalerRecit(recitId, motif, texte, visiteur) }.await()

        // « Plus tard », sur une connexion facturée : ne plus demander avant
    // la prochaine échéance.
    fun plusTard() {
        val c = _catalogue.value ?: return
        ecrireEtat(lireEtat(c).copy(derniereTentative = Instant.now().toString()))
        _aDemander.value = false
    }

    private fun appliquer(b: Bilan, demandee: Boolean) {

        if (b.decision == Decision.DEMANDER) {
            _aDemander.value = true
            return
        }

        if (b.decision == Decision.RIEN) return

        ecrireEtat(b.etat)

        val issue = b.issue

        if (issue is Resultat.Ok && issue.valeur !is Issue.Inchange) {
            garder(b.catalogue)
            _catalogue.value = b.catalogue
        }

        // Partie toute seule, elle ne dit rien : elle ne doit pas se faire
        // remarquer.
        if (demandee) _message.value = dire(issue)
    }

    private fun dire(issue: Resultat<Issue>?): String = when (issue) {
        null -> "Rien à faire."
        is Resultat.Ok -> when (issue.valeur) {
            Issue.Inchange -> "Le contenu est à jour."
            is Issue.MisAJour -> "Contenu mis à jour."
            is Issue.Recharge -> "Tout le contenu a été rechargé."
            Issue.CompletNecessaire -> "Le contenu est à jour."
        }
        is Resultat.Limite -> "Le serveur demande d'attendre un peu. Réessayez dans ${issue.attenteSecondes} secondes."
        is Resultat.Refuse, is Resultat.Injoignable -> "La mise à jour n'a pas abouti. Le contenu sur ce téléphone reste utilisable."
    }

    private class Reseau(val facturee: Boolean)

    private fun reseau(): Reseau? {
        val cm = app.getSystemService(ConnectivityManager::class.java) ?: return null
        val capacites = cm.getNetworkCapabilities(cm.activeNetwork ?: return null) ?: return null
        if (!capacites.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return null
        return Reseau(facturee = cm.isActiveNetworkMetered)
    }

    companion object {

        @Volatile private var unique: Depot? = null

        fun de(contexte: Context): Depot =
            unique ?: synchronized(this) { unique ?: Depot(contexte).also { unique = it } }
    }
}
