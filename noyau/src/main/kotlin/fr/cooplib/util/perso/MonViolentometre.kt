package fr.cooplib.util.perso

import fr.cooplib.util.modeles.Aide
import fr.cooplib.util.modeles.Niveau
import fr.cooplib.util.modeles.PoolDuPoint
import fr.cooplib.util.modeles.Situation
import fr.cooplib.util.modeles.SituationDuPoint
import fr.cooplib.util.modeles.Violentometre
import kotlinx.serialization.Serializable

/*
 * « Mon violentomètre » : une échelle qu'on construit soi-même, sur le
 * téléphone, avec des situations de la bibliothèque ou ses propres
 * phrases (décidé avec Cooplib le 9 octobre 2026). Construire sa propre
 * échelle est un geste de prise de conscience : ranger soi-même « il
 * regarde mon téléphone » entre Vigilance et Danger fait réfléchir
 * autrement que cocher.
 *
 * RIEN N'EST ENVOYÉ. C'est la donnée la plus sensible de l'application,
 * plus que des réponses cochées : elle décrit une relation, parfois avec
 * ses propres mots. Même régime que les réponses au point, sans
 * exception (stockage, côté application).
 */
@Serializable
data class MonViolentometre(
    val id: String,
    val titre: String = "Mon violentomètre",
    val situations: List<MaSituation> = emptyList(),
) {

    /*
     * Pour le lire comme les autres : en entier, ou situation par
     * situation. Les niveaux sont ceux de l'échelle par défaut du site.
     */
    fun enViolentometre(niveaux: List<Niveau>) = Violentometre(
        id = id,
        titre = titre,
        niveaux = niveaux,
        situations = situations.mapIndexed { i, s -> Situation(s.id, s.origine ?: s.id, s.texte, i, s.gravite) },
    )

    /*
     * Pour faire le point dessus, avec le même déroulé que le reste. Une
     * situation prise dans la bibliothèque garde son identifiant : les
     * aides qui lui sont rattachées se proposent comme ailleurs. Une
     * phrase écrite soi-même n'en a pas.
     */
    fun enPoint(niveaux: List<Niveau>, aides: List<Aide>) = PoolDuPoint(
        type = "perso",
        id = id,
        titre = titre,
        niveaux = niveaux,
        situations = situations
            .sortedBy { it.gravite }
            .map { SituationDuPoint(id = it.origine ?: it.id, texte = it.texte, gravite = it.gravite, sources = listOf(titre)) },
        aides = aides,
    )
}

/*
 * Une situation de mon échelle. `origine` : la situation de la
 * bibliothèque dont elle vient, si elle en vient, même si on l'a
 * réécrite ; nulle pour une phrase écrite soi-même.
 */
@Serializable
data class MaSituation(
    val id: String,
    val texte: String,
    val gravite: Int,
    val origine: String? = null,
)
