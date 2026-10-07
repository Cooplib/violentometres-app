package fr.cooplib.util.donnees

import fr.cooplib.util.modeles.Historique
import fr.cooplib.util.modeles.MecanismeResume
import fr.cooplib.util.modeles.ParcoursResume
import fr.cooplib.util.modeles.Version
import fr.cooplib.util.modeles.ViolentometreResume
import fr.cooplib.util.reseau.AdresseApi
import fr.cooplib.util.reseau.ClientApi
import fr.cooplib.util.reseau.ReponseHttp
import fr.cooplib.util.reseau.Requete
import fr.cooplib.util.reseau.Resultat
import fr.cooplib.util.reseau.Transport
import fr.cooplib.util.modeles.CadreResume
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.URI
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/*
 * Ce qui compte ici, c'est autant CE QUI PART que ce qui revient :
 * chaque requête est une trace sur le réseau. Les tests comptent donc
 * les requêtes, et pas seulement le résultat.
 */
class SynchronisationTest {

    private val embarque = Catalogue.embarque()

    private val maintenant = Instant.parse("2026-11-01T12:00:00Z")

    private fun v(id: String, type: String, element: String, le: String, absorbee: String? = null) =
        Version(id = id, typeElement = type, elementId = element, modifieLe = le, absorbeeDans = absorbee)

    /*
     * Une fausse API, servie depuis un catalogue : nos modèles s'écrivent
     * avec les noms de champs de l'API, ses réponses ont donc la forme
     * de celles de l'API. `modifs` change ce qu'elle sert pour un chemin.
     */
    private class FausseApi(
        val contenu: Catalogue,
        var historique: List<Version> = emptyList(),
        val modifs: MutableMap<String, ReponseHttp> = mutableMapOf(),
    ) : Transport {

        val chemins = mutableListOf<String>()
        private val json = Json { encodeDefaults = true }

        override fun envoyer(requete: Requete): ReponseHttp {

            val uri = URI(requete.url)
            val chemin = uri.path.removePrefix("/api")
            chemins += chemin + (uri.query?.let { "?$it" } ?: "")
            modifs[chemin]?.let { return it }

            fun ok(texte: String) = ReponseHttp(200, texte)
            val c = contenu
            val morceaux = chemin.trim('/').split('/')

            return when {
                chemin == "/history/recent" -> {
                    val limite = uri.query!!.substringAfter("limit=").toInt()
                    ok(json.encodeToString(Historique.serializer(), Historique(historique.take(limite), historique.size > limite)))
                }
                chemin == "/violentometers" -> ok(json.encodeToString(ListSerializer(ViolentometreResume.serializer()),
                    c.violentometres.map { ViolentometreResume(it.id, it.titre) }))
                morceaux[0] == "violentometers" -> c.violentometres.find { it.id == morceaux[1] }
                    ?.let { ok(json.encodeToString(fr.cooplib.util.modeles.Violentometre.serializer(), it)) } ?: ReponseHttp(404, "")
                chemin == "/tests/orientation/tout" -> ok(json.encodeToString(fr.cooplib.util.modeles.PoolDuPoint.serializer(), c.pointGeneral))
                morceaux[0] == "tests" -> c.pointsParViolentometre[morceaux[2]]
                    ?.let { ok(json.encodeToString(fr.cooplib.util.modeles.PoolDuPoint.serializer(), it)) } ?: ReponseHttp(404, "")
                chemin == "/parcours" -> ok(json.encodeToString(ListSerializer(ParcoursResume.serializer()), c.parcours.map { ParcoursResume(it.id, it.titre) }))
                morceaux[0] == "parcours" -> c.parcours.find { it.id == morceaux[1] }
                    ?.let { ok(json.encodeToString(fr.cooplib.util.modeles.Parcours.serializer(), it)) } ?: ReponseHttp(404, "")
                chemin == "/cadres" -> ok(json.encodeToString(ListSerializer(CadreResume.serializer()), c.cadres.map { CadreResume(it.id, it.nom) }))
                morceaux[0] == "cadres" -> c.cadres.find { it.id == morceaux[1] }
                    ?.let { ok(json.encodeToString(fr.cooplib.util.modeles.Cadre.serializer(), it)) } ?: ReponseHttp(404, "")
                chemin == "/mecanismes" -> ok(json.encodeToString(ListSerializer(MecanismeResume.serializer()), c.mecanismes.map { MecanismeResume(it.id, it.nom) }))
                morceaux[0] == "mecanismes" -> c.mecanismes.find { it.id == morceaux[1] }
                    ?.let { ok(json.encodeToString(fr.cooplib.util.modeles.Mecanisme.serializer(), it)) } ?: ReponseHttp(404, "")
                chemin == "/stories" -> ok(json.encodeToString(ListSerializer(fr.cooplib.util.modeles.Recit.serializer()), c.recits))
                chemin == "/aides" -> ok(json.encodeToString(ListSerializer(fr.cooplib.util.modeles.Aide.serializer()), c.aides))
                else -> ReponseHttp(404, "")
            }
        }
    }

    private fun api(f: FausseApi) = ClientApi(AdresseApi.PRODUCTION, f)

    private val marque = Marque("v-0", "2026-10-20T10:00:00.000000+00:00")

    private val catalogue = embarque.copy(derniereVersion = marque)

    // ---------------------------------------------------------
    // Quand
    // ---------------------------------------------------------

    @Test
    fun `tout seul, au plus tous les trois jours`() {
        val etat = EtatDeSynchro(derniereTentative = "2026-10-30T12:00:00Z", dernierComplet = "2026-10-30T12:00:00Z")
        assertEquals(Decision.RIEN, Synchronisation.decider(etat, maintenant, demandee = false, facturee = false))
        assertEquals(Decision.INCREMENTALE, Synchronisation.decider(etat, Instant.parse("2026-11-02T12:00:00Z"), false, false))
    }

    @Test
    fun `jamais tout seul sur une connexion facturee, mais a la demande oui`() {
        val etat = EtatDeSynchro(dernierComplet = "2026-10-30T12:00:00Z")
        assertEquals(Decision.DEMANDER, Synchronisation.decider(etat, maintenant, demandee = false, facturee = true))
        assertEquals(Decision.INCREMENTALE, Synchronisation.decider(etat, maintenant, demandee = true, facturee = true))
    }

    @Test
    fun `une fois par semaine, tout recharger`() {
        val etat = EtatDeSynchro(dernierComplet = "2026-10-25T12:00:00Z")
        assertEquals(Decision.COMPLETE, Synchronisation.decider(etat, maintenant, false, false))
        assertEquals(Decision.COMPLETE, Synchronisation.decider(etat, maintenant, true, false))
    }

    @Test
    fun `au depart, le catalogue embarque compte comme un rechargement`() {
        val etat = Synchronisation.depart(embarque)
        assertNull(etat.derniereTentative)
        assertEquals(embarque.fabriqueLe, etat.dernierComplet)
    }

    // ---------------------------------------------------------
    // L'incrémentale
    // ---------------------------------------------------------

    @Test
    fun `historique vide, une seule requete et rien d'autre`() {
        // C'est l'état de la production au 7 octobre 2026.
        val f = FausseApi(catalogue)
        assertEquals(Resultat.Ok(Issue.Inchange), Synchronisation.incrementale(api(f), catalogue))
        assertEquals(listOf("/history/recent?limit=1"), f.chemins)
    }

    @Test
    fun `rien depuis la marque, une seule requete`() {
        val f = FausseApi(catalogue, listOf(v("v-0", "violentometer", "x", marque.modifieLe)))
        assertEquals(Resultat.Ok(Issue.Inchange), Synchronisation.incrementale(api(f), catalogue))
        assertEquals(1, f.chemins.size)
    }

    @Test
    fun `un violentometre change, on ne relit que lui, son point, et le point general`() {

        val vm = catalogue.violentometres[5]
        val change = vm.copy(titre = "Un titre neuf")
        val f = FausseApi(catalogue.copy(violentometres = catalogue.violentometres.map { if (it.id == vm.id) change else it }))
        f.historique = listOf(v("v-1", "violentometer", vm.id, "2026-10-21T08:00:00+00:00"), v("v-0", "fiche", "x", marque.modifieLe))

        val issue = Synchronisation.incrementale(api(f), catalogue)

        assertIs<Resultat.Ok<Issue>>(issue)
        val maj = assertIs<Issue.MisAJour>(issue.valeur).catalogue

        assertEquals("Un titre neuf", maj.violentometres.first { it.id == vm.id }.titre)
        assertEquals(Marque("v-1", "2026-10-21T08:00:00+00:00"), maj.derniereVersion)
        assertEquals(
            listOf("/history/recent?limit=1", "/history/recent?limit=30", "/violentometers/${vm.id}",
                "/tests/violentometer/${vm.id}", "/tests/orientation/tout"),
            f.chemins,
        )
    }

    @Test
    fun `une situation change, on relit les violentometres qui l'emploient`() {

        val situation = catalogue.violentometres[0].situations[0].situationId
        val employeurs = catalogue.violentometres.filter { vm -> vm.situations.any { it.situationId == situation } }.map { it.id }

        val f = FausseApi(catalogue, listOf(v("v-1", "behavior", situation, "2026-10-21T08:00:00+00:00")))

        Synchronisation.incrementale(api(f), catalogue)

        assertEquals(employeurs.toSet(), f.chemins.filter { it.startsWith("/violentometers/") }.map { it.substringAfterLast('/') }.toSet())
    }

    @Test
    fun `un violentometre retire quitte le catalogue, avec son point`() {

        val vm = catalogue.violentometres[3]
        val f = FausseApi(catalogue.copy(violentometres = catalogue.violentometres - vm))
        f.historique = listOf(v("v-1", "violentometer", vm.id, "2026-10-21T08:00:00+00:00"))

        val maj = (Synchronisation.incrementale(api(f), catalogue) as Resultat.Ok).valeur as Issue.MisAJour

        assertTrue(maj.catalogue.violentometres.none { it.id == vm.id })
        assertTrue(vm.id !in maj.catalogue.pointsParViolentometre)
    }

    @Test
    fun `un violentometre nouveau entre, a sa place dans l'ordre`() {

        val nouveau = catalogue.violentometres[0].copy(id = "0000-nouveau", titre = "Nouveau")
        val avecLui = catalogue.copy(
            violentometres = listOf(nouveau) + catalogue.violentometres,
            pointsParViolentometre = catalogue.pointsParViolentometre + ("0000-nouveau" to catalogue.pointGeneral),
        )
        val f = FausseApi(avecLui, listOf(v("v-1", "violentometer", "0000-nouveau", "2026-10-21T08:00:00+00:00")))

        val maj = ((Synchronisation.incrementale(api(f), catalogue) as Resultat.Ok).valeur as Issue.MisAJour).catalogue

        assertEquals("0000-nouveau", maj.violentometres.first().id)
        assertEquals(maj.violentometres.map { it.id }.sorted(), maj.violentometres.map { it.id })
    }

    @Test
    fun `une aide ou un type inconnu, on ne devine pas, on recharge tout`() {
        for (type in listOf("aide", "context", "nouveau-type")) {
            val f = FausseApi(catalogue, listOf(v("v-1", type, "x", "2026-10-21T08:00:00+00:00")))
            assertEquals(Resultat.Ok(Issue.CompletNecessaire), Synchronisation.incrementale(api(f), catalogue), type)
        }
    }

    @Test
    fun `plus de changements qu'une page, on recharge tout`() {
        val versions = (1..31).map { v("v-$it", "violentometer", "x", "2026-10-21T08:00:00+00:00") }
        val f = FausseApi(catalogue, versions)
        assertEquals(Resultat.Ok(Issue.CompletNecessaire), Synchronisation.incrementale(api(f), catalogue))
    }

    // ---------------------------------------------------------
    // Le rechargement, et l'ensemble
    // ---------------------------------------------------------

    @Test
    fun `le rechargement complet relit tout le catalogue a l'identique`() {

        val f = FausseApi(catalogue, listOf(v("v-9", "fiche", "x", "2026-10-22T08:00:00+00:00")))

        val relu = (rechargerTout(api(f), maintenant) as Resultat.Ok).valeur

        assertEquals(catalogue.copy(fabriqueLe = relu.fabriqueLe, derniereVersion = relu.derniereVersion), relu)
        assertEquals(Marque("v-9", "2026-10-22T08:00:00+00:00"), relu.derniereVersion)
        // La marque est prise AVANT le reste.
        assertEquals("/history/recent?limit=1", f.chemins.first())
    }

    @Test
    fun `une coupure en route, et rien n'est garde de la moitie faite`() {

        val f = FausseApi(catalogue)
        f.modifs["/parcours"] = ReponseHttp(200, "")
        val etat = EtatDeSynchro(dernierComplet = "2026-10-01T00:00:00Z")

        val bilan = Synchronisation.synchroniser(api(f), catalogue, etat, maintenant, demandee = false, facturee = false)

        assertEquals(Decision.COMPLETE, bilan.decision)
        assertIs<Resultat.Injoignable>(bilan.issue)
        assertEquals(catalogue, bilan.catalogue)
        // Tentée : on ne réessaiera pas à chaque ouverture…
        assertEquals(maintenant.toString(), bilan.etat.derniereTentative)
        // …mais le rechargement reste dû.
        assertEquals(etat.dernierComplet, bilan.etat.dernierComplet)
    }

    @Test
    fun `une incrementale qui ne suffit pas enchaine sur le rechargement`() {

        val f = FausseApi(catalogue, listOf(v("v-1", "aide", "x", "2026-10-21T08:00:00+00:00")))
        val etat = EtatDeSynchro(dernierComplet = "2026-10-30T00:00:00Z")

        val bilan = Synchronisation.synchroniser(api(f), catalogue, etat, maintenant, demandee = true, facturee = false)

        assertIs<Issue.Recharge>((bilan.issue as Resultat.Ok).valeur)
        assertEquals(maintenant.toString(), bilan.etat.dernierComplet)
        assertEquals(Marque("v-1", "2026-10-21T08:00:00+00:00"), bilan.catalogue.derniereVersion)
    }

    @Test
    fun `hors reseau, un echec et l'ancien catalogue reste`() {

        val horsReseau = Transport { throw IOException("pas de réseau") }
        val etat = EtatDeSynchro(dernierComplet = "2026-10-30T00:00:00Z")

        val bilan = Synchronisation.synchroniser(ClientApi(AdresseApi.PRODUCTION, horsReseau), catalogue, etat, maintenant, true, false)

        assertIs<Resultat.Injoignable>(bilan.issue)
        assertEquals(catalogue, bilan.catalogue)
    }

    @Test
    fun `les dates de l'API se lisent, avec ou sans decalage`() {
        val attendu = Instant.parse("2026-09-30T07:30:19.253551Z")
        assertEquals(attendu, Synchronisation.instant("2026-09-30T07:30:19.253551+00:00"))
        assertEquals(attendu, Synchronisation.instant("2026-09-30T07:30:19.253551Z"))
        assertEquals(attendu, Synchronisation.instant("2026-09-30T07:30:19.253551"))
        assertEquals(attendu, Synchronisation.instant("2026-09-30T09:30:19.253551+02:00"))
    }
}
