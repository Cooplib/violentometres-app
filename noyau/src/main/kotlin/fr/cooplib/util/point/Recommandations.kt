package fr.cooplib.util.point

import fr.cooplib.util.modeles.MecanismeDuPoint
import fr.cooplib.util.modeles.ParcoursDuPoint
import fr.cooplib.util.modeles.PoolDuPoint
import fr.cooplib.util.modeles.RecitDuPoint
import fr.cooplib.util.modeles.SituationDuPoint
import fr.cooplib.util.modeles.ViolentometreDuPoint

/*
 * Ce qui pourrait servir à quelqu'un, d'après ce qu'il vient de
 * reconnaître.
 *
 * PORTAGE de ../violentometres-frontend/src/utils/recommandations.js,
 * qui fait référence. Comme là-bas, tout se passe sur l'appareil : ce
 * qu'on coche, c'est ce qu'on vit.
 *
 * Ce n'est pas un classement de qualité : un compte de recoupements.
 */

data class Recommande<T>(val element: T, val recoupements: Int)

data class Recommandations(
    val violentometres: List<Recommande<ViolentometreDuPoint>> = emptyList(),
    val parcours: List<Recommande<ParcoursDuPoint>> = emptyList(),
    val recits: List<Recommande<RecitDuPoint>> = emptyList(),
    val mecanismes: List<Recommande<MecanismeDuPoint>> = emptyList(),
)

// Assez pour avoir le choix, pas au point de redonner une liste.
private const val COMBIEN = 3

/*
 * À égalité de recoupements, le site départage par le titre, avec
 * `localeCompare(…, "fr")` : le tri de l'ICU du navigateur.
 *
 * Le noyau ne choisit PAS ce tri, il le reçoit. Essayé d'abord : le
 * java.text.Collator de la JVM. La comparaison avec le site l'a pris
 * en défaut dès la deuxième partie, sur « Le travail, sous plusieurs
 * angles » et « Le travail : du général au particulier » : la JVM et
 * l'ICU ne rangent pas la virgule et l'espace devant les deux-points
 * dans le même ordre. Seule l'ICU retrouve l'ICU.
 *
 * Donc : android.icu.text.Collator dans l'application, ICU4J dans les
 * tests. Pas d'ICU4J dans le noyau lui-même, qui finirait dans l'APK
 * pour une douzaine de Mo, alors qu'Android a déjà la sienne.
 */
// Le compte, dans l'ordre où chaque clé est rencontrée la première fois :
// c'est l'ordre d'une Map JS, et il départage avant le titre.
private fun compter(cles: List<String>): LinkedHashMap<String, Int> {
    val comptes = LinkedHashMap<String, Int>()
    for (cle in cles) {
        comptes[cle] = (comptes[cle] ?: 0) + 1
    }
    return comptes
}

/*
 * À égalité de recoupements, l'ordre doit rester le même d'une fois
 * sur l'autre : un résultat qui change tout seul se lit comme un
 * hasard.
 */
private fun <T> meilleurs(
    ordre: Comparator<String>,
    comptes: Map<String, Int>,
    parId: Map<String, T>,
    titre: (T) -> String,
    combien: Int = COMBIEN,
): List<Recommande<T>> =
    comptes.entries
        .filter { it.key in parId }
        .sortedWith(
            compareByDescending<Map.Entry<String, Int>> { it.value }
                .thenComparator { a, b -> ordre.compare(titre(parId.getValue(a.key)), titre(parId.getValue(b.key))) }
        )
        .take(combien)
        .map { Recommande(parId.getValue(it.key), it.value) }

/*
 * `ordre` : le tri français des titres, celui de l'ICU (voir plus haut).
 */
fun recommander(pool: PoolDuPoint, reconnues: List<SituationDuPoint>, ordre: Comparator<String>): Recommandations {

    if (reconnues.isEmpty()) {
        return Recommandations()
    }

    /*
     * Les violentomètres, d'après TOUTES les situations reconnues,
     * positives comprises : elles disent dans quel cadre de vie on se
     * trouve.
     */
    val violentometres = meilleurs(
        ordre,
        compter(reconnues.flatMap { it.violentometreIds }),
        pool.violentometres.associateBy { it.id },
        { it.titre },
    )

    /*
     * Les parcours, d'après ce qu'ils traversent : les violentomètres
     * reconnus, et les mécanismes de ce qui n'est pas positif. Un
     * violentomètre que le parcours traverse deux fois compte deux
     * fois, comme sur le site.
     */
    val vmsReconnus = reconnues.flatMap { it.violentometreIds }.toSet()

    val mecanismesReconnus = reconnues.filter { it.gravite > 0 }.flatMap { it.mecanismes }.toSet()

    val parcours = meilleurs(
        ordre,
        compter(
            pool.parcours.flatMap { p ->
                (p.violentometreIds.filter { it in vmsReconnus } + p.mecanismeIds.filter { it in mecanismesReconnus })
                    .map { p.id }
            }
        ),
        pool.parcours.associateBy { it.id },
        { it.titre },
        2,
    )

    // Les récits, d'après ce qui n'est PAS positif : un récit raconte ce
    // qui a mal tourné.
    val recits = meilleurs(
        ordre,
        compter(reconnues.filter { it.gravite > 0 }.flatMap { it.recitIds }),
        pool.recits.associateBy { it.id },
        { it.titre },
    )

    // Les mécanismes : les noms seuls, jamais un diagnostic.
    val mecanismes = meilleurs(
        ordre,
        compter(reconnues.filter { it.gravite > 0 }.flatMap { it.mecanismes }),
        pool.mecanismes.associateBy { it.id },
        { it.nom },
    )

    return Recommandations(violentometres, parcours, recits, mecanismes)
}
