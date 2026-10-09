package fr.cooplib.util.point

import fr.cooplib.util.modeles.PoolDuPoint
import fr.cooplib.util.modeles.decodage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/*
 * La règle de reprise, posée à part : DerouleContreLeSiteTest dit que
 * l'application fait COMME le site, pas que le site a raison. Le 9
 * octobre 2026, les deux proposaient de « reprendre » un point mené
 * jusqu'au bout : l'index restait sur la dernière question.
 */
class RepriseTest {

    private val pool = decodage.decodeFromString(PoolDuPoint.serializer(), javaClass.getResource("/api/point-violentometre.json")!!.readText())
    private val d = Deroule(pool)

    // « Non » partout, et « continuer » à chaque sas.
    private fun jusquAuBout(): EtatDuPoint {
        var etat = d.commencer(EtatDuPoint())
        var n = 0
        while (etat.phase != Phase.RESULTAT && n++ < 1000) {
            etat = when (etat.phase) {
                Phase.SAS -> d.reprendreApresSas(etat)
                else -> d.repondre(etat, d.questionCourante(etat)!!, Reponse.NON, 0L)
            }
        }
        return etat
    }

    @Test
    fun `un point mene jusqu'au bout ne se reprend pas`() {
        val fin = jusquAuBout()
        assertEquals(Phase.RESULTAT, fin.phase)
        assertEquals(pool.situations.size, fin.reponses.size)
        assertFalse(d.peutReprendre(fin))
    }

    @Test
    fun `un point arrete en route se reprend`() {
        var etat = d.commencer(EtatDuPoint())
        etat = d.repondre(etat, d.questionCourante(etat)!!, Reponse.NON, 0L)
        val arrete = d.terminer(etat, 0L)
        assertTrue(d.peutReprendre(arrete))
        assertEquals(Phase.QUESTIONS, d.reprendreLePoint(arrete).phase)
    }
}
