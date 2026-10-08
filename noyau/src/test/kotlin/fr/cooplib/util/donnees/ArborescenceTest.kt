package fr.cooplib.util.donnees

import kotlin.test.Test
import kotlin.test.assertEquals

class ArborescenceTest {

    // Travail et École, et un stage qui relève des deux.
    private val categories = listOf(
        Categorie("travail", "Travail", emptyList()),
        Categorie("ecole", "École", emptyList()),
        Categorie("salarie", "Emploi salarié", listOf("travail")),
        Categorie("stage", "Stage, alternance", listOf("travail", "ecole")),
        Categorie("vide", "Sans rien", emptyList()),
    )

    @Suppress("UNCHECKED_CAST")
    private val ordre = java.text.Collator.getInstance(java.util.Locale.FRENCH) as Comparator<String>

    @Test
    fun `choisir une famille garde ce qui se range dessous`() {
        assertEquals(setOf("travail", "salarie", "stage"), Arborescence.avecSous(categories, "travail"))
        assertEquals(setOf("stage"), Arborescence.avecSous(categories, "stage"))
    }

    @Test
    fun `une categorie a deux parents apparait sous chacun, et rien d'inutile`() {
        val lignes = Arborescence.lignes(categories, utiles = setOf("stage", "salarie"), ordre = ordre)
        assertEquals(
            listOf("ecole" to 0, "stage" to 1, "travail" to 0, "salarie" to 1, "stage" to 1),
            lignes.map { it.categorie.id to it.profondeur },
        )
    }

    @Test
    fun `un cycle ne fait pas boucler`() {
        val cycle = listOf(Categorie("a", "A", listOf("b")), Categorie("b", "B", listOf("a")), Categorie("r", "R", emptyList()))
        assertEquals(setOf("a", "b"), Arborescence.avecSous(cycle, "a"))
        Arborescence.lignes(cycle, setOf("a"), ordre)
    }
}
