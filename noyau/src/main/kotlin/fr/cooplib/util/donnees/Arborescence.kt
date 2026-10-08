package fr.cooplib.util.donnees

/*
 * Les contextes et les cadres d'analyse forment un GRAPHE, pas un arbre :
 * une catégorie peut se ranger sous plusieurs familles (« Stage,
 * alternance » est à la fois du travail et de l'école ; l'écoféminisme,
 * un féminisme ET une écologie politique).
 *
 * PORTAGE de ../violentometres-frontend/src/utils/cadres.js et de
 * FiltreCadre.jsx, qui font référence, pour que les filtres de liste se
 * comportent comme ceux du site :
 *  - choisir une famille garde aussi tout ce qui se range sous elle ;
 *  - une catégorie à plusieurs parents apparaît sous chacun ;
 *  - on ne propose que ce qui mène à au moins un élément de la liste.
 */
data class Categorie(val id: String, val nom: String, val parents: List<String>)

// Une ligne du sélecteur : la catégorie, et sa profondeur dans l'arbre.
data class Ligne(val categorie: Categorie, val profondeur: Int)

object Arborescence {

    private fun enfantsParParent(categories: List<Categorie>): Map<String, List<String>> {
        val enfants = mutableMapOf<String, MutableList<String>>()
        for (c in categories) for (p in c.parents) enfants.getOrPut(p) { mutableListOf() } += c.id
        return enfants
    }

    // Une catégorie et tout ce qui se range sous elle, à toute profondeur ;
    // chacune une seule fois, même atteinte par deux chemins.
    fun avecSous(categories: List<Categorie>, id: String): Set<String> {
        val enfants = enfantsParParent(categories)
        val vus = mutableSetOf<String>()
        val aVoir = ArrayDeque(listOf(id))
        while (aVoir.isNotEmpty()) {
            val courant = aVoir.removeLast()
            if (!vus.add(courant)) continue
            aVoir.addAll(enfants[courant].orEmpty())
        }
        return vus
    }

    /*
     * Les lignes du sélecteur, dans l'ordre de l'arbre : les familles
     * (sans parent connu) par ordre alphabétique, chacune suivie de ses
     * enfants, indentés. `utiles` : les catégories dont un élément de la
     * liste se réclame directement ; une ligne n'est proposée que si elle
     * ou l'une de ses descendantes en fait partie.
     */
    fun lignes(categories: List<Categorie>, utiles: Set<String>, ordre: Comparator<String>): List<Ligne> {

        val parId = categories.associateBy { it.id }
        val enfants = enfantsParParent(categories)
        val tri = compareBy(ordre) { id: String -> parId[id]?.nom ?: "" }

        fun utile(id: String) = avecSous(categories, id).any { it in utiles }

        val lignes = mutableListOf<Ligne>()

        fun parcourir(id: String, profondeur: Int, chemin: Set<String>) {
            // Un cycle ne devrait pas exister (l'API le refuse) ; on ne
            // boucle pas pour autant.
            if (id in chemin || !utile(id)) return
            lignes += Ligne(parId.getValue(id), profondeur)
            for (e in enfants[id].orEmpty().filter { it in parId }.sortedWith(tri)) parcourir(e, profondeur + 1, chemin + id)
        }

        categories
            .filter { c -> c.parents.none { it in parId } }
            .map { it.id }
            .sortedWith(tri)
            .forEach { parcourir(it, 0, emptySet()) }

        return lignes
    }
}
