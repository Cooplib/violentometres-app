package fr.cooplib.util.perso

import fr.cooplib.util.donnees.Catalogue
import fr.cooplib.util.point.Deroule
import fr.cooplib.util.point.EtatDuPoint
import fr.cooplib.util.point.Phase
import fr.cooplib.util.point.Reponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MonViolentometreTest {

    private val catalogue = Catalogue.embarque()
    private val niveaux = catalogue.pointGeneral.niveaux
    private val prise = catalogue.violentometres.first().situations.first()

    private val mien = MonViolentometre(
        id = "m-1",
        titre = "Chez nous",
        situations = listOf(
            MaSituation("s-1", "Il regarde mon téléphone quand je dors.", gravite = 2),
            MaSituation("s-2", prise.texte, gravite = 1, origine = prise.situationId),
            MaSituation("s-3", "On décide ensemble des vacances.", gravite = 0),
        ),
    )

    @Test
    fun `il se lit comme les autres, avec ses phrases`() {
        val vm = mien.enViolentometre(niveaux)
        assertEquals(3, vm.situations.size)
        assertEquals(prise.situationId, vm.situations[1].situationId)
        assertEquals("s-1", vm.situations[0].situationId)
    }

    @Test
    fun `on fait le point dessus avec le meme deroule`() {

        val pool = mien.enPoint(niveaux, catalogue.aides)
        val d = Deroule(pool)

        // Les niveaux proposés sont ceux qui ont des situations, dans l'ordre.
        assertEquals(listOf(0, 1, 2), d.niveauxProposes.map { it.position })

        var etat = d.commencer(EtatDuPoint())
        while (etat.phase != Phase.RESULTAT) {
            etat = when (etat.phase) {
                Phase.QUESTIONS -> d.repondre(etat, d.questionCourante(etat)!!, Reponse.OUI, 0L)
                Phase.SAS -> d.reprendreApresSas(etat)
                else -> d.apresAlerte(etat, 0L)
            }
        }

        assertEquals(2, Deroule.niveauAtteint(d.situationsReconnues(etat.reponses)))
    }

    @Test
    fun `une situation prise garde son identifiant, et donc ses aides`() {
        val pool = mien.enPoint(niveaux, catalogue.aides)
        assertTrue(pool.situations.any { it.id == prise.situationId })
    }
}
