package fr.cooplib.util.leurre

import fr.cooplib.util.leurre.Calculette.Touche
import kotlin.test.Test
import kotlin.test.assertEquals

/*
 * Le leurre doit calculer comme une calculatrice de téléphone. Chaque
 * écart ici est un écart que quelqu'un pourrait remarquer.
 */
class CalculetteTest {

    // « 12+3×4= » : une touche par caractère, comme on les tape.
    private fun taper(touches: String, depart: Calculette = Calculette()): Calculette =
        touches.fold(depart) { c, t ->
            c.appuyer(
                when (t) {
                    in '0'..'9' -> Touche.Chiffre(t)
                    ',' -> Touche.Virgule
                    '+' -> Touche.Operateur(Calculette.PLUS)
                    '-' -> Touche.Operateur(Calculette.MOINS)
                    '*' -> Touche.Operateur(Calculette.FOIS)
                    '/' -> Touche.Operateur(Calculette.DIVISE)
                    '%' -> Touche.Pourcent
                    '<' -> Touche.Retour
                    'C' -> Touche.Effacer
                    '=' -> Touche.Egal
                    else -> error("touche : $t")
                }
            )
        }

    private fun resultat(touches: String) = taper(touches).affichage

    @Test
    fun `les priorites`() {
        assertEquals("14", resultat("2+3*4="))
        assertEquals("10", resultat("2*3+4="))
        assertEquals("1", resultat("10-3*3="))
        assertEquals("2,5", resultat("10/4="))
    }

    @Test
    fun `le decimal est exact, comme sur une calculatrice`() {
        assertEquals("0,3", resultat("0,1+0,2="))
        assertEquals("0,333333333333", resultat("1/3="))
        assertEquals("0,666666666667", resultat("2/3="))
        assertEquals("1", resultat("1/3*3="))
    }

    @Test
    fun `les negatifs`() {
        assertEquals("−3", resultat("-5+2="))
        assertEquals("−6", resultat("2*-3="))
        assertEquals("−2", resultat("6/-3="))
        // « × − » puis « + » : le plus remplace les deux.
        assertEquals("5", resultat("2*-+3="))
    }

    @Test
    fun `le pourcent divise par cent`() {
        assertEquals("0,5", resultat("50%="))
        assertEquals("20", resultat("200*10%="))
    }

    @Test
    fun `diviser par zero se dit, sans planter`() {
        assertEquals(Calculette.DIVISION_PAR_ZERO, resultat("1/0="))
        assertEquals("7", resultat("1/0=7="))
    }

    @Test
    fun `deux operateurs de suite, le second remplace le premier`() {
        assertEquals("2×3", taper("2+*3").saisie)
        assertEquals("6", resultat("2+*3="))
    }

    @Test
    fun `apres egal, un chiffre repart de zero, un operateur continue`() {
        assertEquals("9", resultat("2+3=9="))
        assertEquals("10", resultat("2+3=*2="))
    }

    @Test
    fun `pas de zeros en tete, pas deux virgules`() {
        assertEquals("7", taper("007").saisie)
        assertEquals("0,5", taper(",5").saisie)
        assertEquals("1,25", taper("1,2,5").saisie)
    }

    @Test
    fun `effacer et revenir`() {
        assertEquals("12", taper("123<").saisie)
        assertEquals("0", taper("12+3C").affichage)
        assertEquals("0", taper("2+3=<").affichage)
    }

    @Test
    fun `les grands et les petits nombres`() {
        assertEquals("1E12", resultat("1000000*1000000="))
        assertEquals("999999999999", resultat("999999999999="))
        assertEquals("1E−10", resultat("1/10000000000="))
    }

    @Test
    fun `un egal sans calcul ne fait rien`() {
        assertEquals("0", resultat("="))
        assertEquals("5", resultat("5+="))
    }

    @Test
    fun `le code se tape comme un calcul et s'affiche comme un nombre`() {
        // L'écran le reconnaît avant « = » ; tapé sans code posé, c'est un
        // nombre comme un autre.
        assertEquals("2580", taper("2580").saisie)
        assertEquals("2580", resultat("2580="))
    }
}
