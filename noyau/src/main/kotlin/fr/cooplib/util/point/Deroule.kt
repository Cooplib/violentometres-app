package fr.cooplib.util.point

import fr.cooplib.util.modeles.Aide
import fr.cooplib.util.modeles.Niveau
import fr.cooplib.util.modeles.PoolDuPoint
import fr.cooplib.util.modeles.SituationDuPoint

/*
 * Le déroulé d'un point : quelles questions, dans quel ordre, où l'on
 * s'arrête, ce qu'on a atteint, quelles aides.
 *
 * PORTAGE de ../violentometres-frontend/src/utils/deroule.js, qui fait
 * référence. Fonction pour fonction, dans le même ordre, avec les mêmes
 * noms quand le français le permet : celui qui corrige l'un doit
 * retrouver l'autre sans chercher. DerouleContreLeSiteTest joue les
 * mêmes réponses des deux côtés et compare après chaque action.
 *
 * Une différence de forme, pas de fond : les transitions du site
 * rendent les modifications à fusionner dans l'état, celles-ci rendent
 * l'état suivant entier. C'est la même fusion, faite ici.
 */
class Deroule(private val pool: PoolDuPoint) {

    // ---------------------------------------------------------
    // Ce que le point parcourt
    // ---------------------------------------------------------

    // Les niveaux qui ont au moins une situation : un niveau vide n'a
    // pas de sas, on ne s'y arrête pas.
    val niveauxProposes: List<Niveau> =
        pool.niveaux.filter { n -> pool.situations.any { it.gravite == n.position } }

    val situationsParNiveau: Map<Int, List<SituationDuPoint>> =
        pool.situations.groupBy { it.gravite }

    val plusGrave: Int? = niveauxProposes.lastOrNull()?.position

    fun situationsReconnues(reponses: Map<String, Reponse>): List<SituationDuPoint> =
        pool.situations.filter { reponses[it.id] == Reponse.OUI }

    fun niveauCourant(etat: EtatDuPoint): Niveau? = niveauxProposes.getOrNull(etat.niveau)

    fun situationsCourantes(etat: EtatDuPoint): List<SituationDuPoint> =
        niveauCourant(etat)?.let { situationsParNiveau[it.position] } ?: emptyList()

    // La question à l'écran ; `null` si le pool a changé depuis la
    // pause, et le site propose alors de recommencer.
    fun questionCourante(etat: EtatDuPoint): SituationDuPoint? =
        situationsCourantes(etat).getOrNull(etat.index)

    // ---------------------------------------------------------
    // Les aides
    // ---------------------------------------------------------

    fun aidesLiees(situationId: String?): List<Aide> =
        pool.aides.filter { situationId in it.situationIds }

    fun aidesReconnues(reconnues: List<SituationDuPoint>): List<Aide> {
        val ids = reconnues.map { it.id }.toSet()
        return pool.aides.filter { a -> a.situationIds.any { it in ids } }
    }

    /*
     * Les aides rattachées directement à une situation, pas encore
     * montrées : elles viennent dès qu'on la reconnaît. Les aides plus
     * larges attendent la fin.
     */
    fun aidesNouvelles(etat: EtatDuPoint, situation: SituationDuPoint): List<Aide> {
        val vues = etat.aidesVues.toSet()
        return aidesLiees(situation.id).filter { it.id !in vues }
    }

    /*
     * Les aides rattachées aux situations reconnues, puis celles du
     * niveau atteint ; celles pour le public choisi d'abord. Le tri est
     * stable des deux côtés (Array.sort l'est depuis ES2019) : à rang
     * égal, l'ordre d'arrivée.
     */
    fun aidesDuResultat(etat: EtatDuPoint): List<Aide> {

        val reconnues = situationsReconnues(etat.reponses)

        val directes = aidesReconnues(reconnues)

        val deja = directes.map { it.id }.toSet()

        val niveau = niveauAtteint(reconnues) ?: 0

        val autres = pool.aides.filter { it.id !in deja && it.niveauMinimum <= niveau }

        fun rang(a: Aide) = when (a.public) {
            "tous" -> 1
            etat.public -> 0
            else -> 2
        }

        return (directes + autres).sortedBy(::rang)
    }

    // Une situation du niveau le plus grave vient d'être reconnue : ses
    // aides à elle, puis jusqu'à quatre aides d'urgence ou d'écoute.
    fun aidesDAlerte(etat: EtatDuPoint): List<Aide> {

        val liees = aidesLiees(etat.situationVue)

        val dejaLa = liees.map { it.id }.toSet()

        return liees + aidesDuResultat(etat)
            .filter { it.id !in dejaLa && (it.sorte == "urgence" || it.sorte == "ecoute") }
            .take(4)
    }

    // ---------------------------------------------------------
    // Le résultat
    // ---------------------------------------------------------

    fun reconnuesParNiveau(reconnues: List<SituationDuPoint>): List<Pair<Niveau, List<SituationDuPoint>>> =
        pool.niveaux
            .map { n -> n to reconnues.filter { it.gravite == n.position } }
            .filter { it.second.isNotEmpty() }

    // ---------------------------------------------------------
    // Les transitions
    // ---------------------------------------------------------

    // Au site, un bouton radio ; ici comme là, rien d'autre ne change.
    fun choisirPublic(etat: EtatDuPoint, public: String) = etat.copy(public = public)

    fun commencer(etat: EtatDuPoint) = etat.copy(
        phase = Phase.QUESTIONS, niveau = 0, index = 0,
        reponses = emptyMap(), aidesVues = emptyList(), finiLe = null,
    )

    fun terminer(etat: EtatDuPoint, maintenant: Long) =
        etat.copy(phase = Phase.RESULTAT, finiLe = maintenant)

    private fun passerAuNiveauSuivant(etat: EtatDuPoint, maintenant: Long): EtatDuPoint {

        if (etat.niveau + 1 >= niveauxProposes.size) {
            return terminer(etat, maintenant)
        }

        return etat.copy(niveau = etat.niveau + 1, index = 0, phase = Phase.SAS)
    }

    // La question suivante, ou le sas du niveau suivant. `etat` porte
    // déjà les réponses à jour ; `suivant` est l'index visé.
    private fun avancerApres(etat: EtatDuPoint, suivant: Int, maintenant: Long): EtatDuPoint {

        if (suivant < situationsCourantes(etat).size) {
            return etat.copy(index = suivant, phase = Phase.QUESTIONS)
        }

        return passerAuNiveauSuivant(etat, maintenant)
    }

    fun repondre(etat: EtatDuPoint, situation: SituationDuPoint, reponse: Reponse, maintenant: Long): EtatDuPoint {

        val repondu = etat.copy(reponses = etat.reponses + (situation.id to reponse))
        val suivant = etat.index + 1

        if (reponse == Reponse.OUI) {

            val nouvelles = aidesNouvelles(etat, situation)
            val aidesVues = etat.aidesVues + nouvelles.map { it.id }

            // Une situation du niveau le plus grave reconnue : les aides
            // d'urgence, tout de suite, et le choix de s'arrêter là.
            if (situation.gravite == plusGrave) {
                return repondu.copy(aidesVues = aidesVues, index = suivant, phase = Phase.ALERTE, situationVue = situation.id)
            }

            if (nouvelles.isNotEmpty()) {
                return repondu.copy(aidesVues = aidesVues, index = suivant, phase = Phase.AIDE, situationVue = situation.id)
            }
        }

        return avancerApres(repondu, suivant, maintenant)
    }

    // Après l'écran d'une aide ou d'une alerte : là où l'on en était.
    fun apresAlerte(etat: EtatDuPoint, maintenant: Long) = avancerApres(etat, etat.index, maintenant)

    // La question d'avant, celle du niveau précédent s'il le faut ;
    // `null` avant la première. La réponse déjà donnée reste.
    fun precedent(etat: EtatDuPoint): EtatDuPoint? {

        if (etat.index > 0) {
            return etat.copy(index = etat.index - 1)
        }

        if (etat.niveau > 0) {
            val niveauPrecedent = niveauxProposes[etat.niveau - 1]
            return etat.copy(
                niveau = etat.niveau - 1,
                index = (situationsParNiveau[niveauPrecedent.position]?.size ?: 1) - 1,
            )
        }

        return null
    }

    fun reprendreApresSas(etat: EtatDuPoint) = etat.copy(phase = Phase.QUESTIONS)

    fun revoirLesPositifs(etat: EtatDuPoint) = etat.copy(phase = Phase.POSITIFS)

    fun revenirAuSas(etat: EtatDuPoint) = etat.copy(phase = Phase.SAS)

    // On reprend là où la pause a été prise : une question, ou un sas.
    fun pause(etat: EtatDuPoint) = etat.copy(phase = Phase.PAUSE, reprise = etat.phase)

    fun reprendre(etat: EtatDuPoint) = etat.copy(phase = etat.reprise ?: Phase.QUESTIONS)

    companion object {

        // Le résultat n'est pas un score : le niveau le plus élevé
        // parmi les situations reconnues. Une seule suffit.
        fun niveauAtteint(reconnues: List<SituationDuPoint>): Int? = reconnues.maxOfOrNull { it.gravite }

        /*
         * Sur quoi porte ce qui a été reconnu : les trois formes qui
         * reviennent le plus ; à égalité, l'ordre où on les a
         * rencontrées (l'ordre d'insertion d'un objet JS, celui d'un
         * LinkedHashMap ici, et deux tris stables).
         */
        fun formesReconnues(reconnues: List<SituationDuPoint>): List<Pair<String, Int>> {

            val formes = LinkedHashMap<String, Int>()

            for (situation in reconnues) {
                for (forme in situation.formes) {
                    formes[forme] = (formes[forme] ?: 0) + 1
                }
            }

            return formes.entries
                .sortedByDescending { it.value }
                .take(3)
                .map { it.key to it.value }
        }
    }
}
