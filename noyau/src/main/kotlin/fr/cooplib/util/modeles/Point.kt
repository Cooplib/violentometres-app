package fr.cooplib.util.modeles

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Ce que l'API donne pour mener un point : `GET /tests/{type}/{id}`.
 *
 * Deux formes sous la même adresse, assemblées par deux fonctions
 * différentes côté serveur (app/routes/tests.py) :
 *
 *  - le point général (`orientation/tout`) : des situations prises dans
 *    tout le site, avec leurs violentomètres, leurs récits, et les
 *    parcours, de quoi recommander à la fin ;
 *  - le point sur un élément (`violentometer/{id}`…) : ses situations
 *    seulement. Ni `story_ids`, ni `violentometer_ids` dans les
 *    situations, ni parcours, ni récits.
 *
 * D'où les valeurs par défaut partout où l'une des deux formes se tait.
 */
@Serializable
data class PoolDuPoint(
    val type: String,
    val id: String,
    @SerialName("title") val titre: String,
    val description: String = "",
    @SerialName("contexts") val contextes: List<String> = emptyList(),
    @SerialName("levels") val niveaux: List<Niveau>,
    val situations: List<SituationDuPoint>,
    val aides: List<Aide> = emptyList(),
    @SerialName("violentometers") val violentometres: List<ViolentometreDuPoint> = emptyList(),
    val parcours: List<ParcoursDuPoint> = emptyList(),
    val mecanismes: List<MecanismeDuPoint> = emptyList(),
    @SerialName("stories") val recits: List<RecitDuPoint> = emptyList(),
)

@Serializable
data class SituationDuPoint(
    val id: String,
    @SerialName("text") val texte: String,
    // La position du niveau où elle se range : 0 pour ce qui va bien,
    // le plus haut pour le plus grave.
    @SerialName("severity") val gravite: Int,
    val formes: List<String> = emptyList(),
    val mecanismes: List<String> = emptyList(),
    val sources: List<String> = emptyList(),
    @SerialName("violentometer_ids") val violentometreIds: List<String> = emptyList(),
    @SerialName("story_ids") val recitIds: List<String> = emptyList(),
)

@Serializable
data class ViolentometreDuPoint(
    val id: String,
    @SerialName("title") val titre: String,
    val description: String = "",
    @SerialName("contexts") val contextes: List<String> = emptyList(),
)

@Serializable
data class ParcoursDuPoint(
    val id: String,
    @SerialName("title") val titre: String,
    val description: String = "",
    @SerialName("violentometer_ids") val violentometreIds: List<String> = emptyList(),
    @SerialName("mecanisme_ids") val mecanismeIds: List<String> = emptyList(),
)

@Serializable
data class MecanismeDuPoint(
    val id: String,
    val nom: String,
    val resume: String = "",
)

@Serializable
data class RecitDuPoint(
    val id: String,
    @SerialName("title") val titre: String,
)
