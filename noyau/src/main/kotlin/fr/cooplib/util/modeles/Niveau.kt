package fr.cooplib.util.modeles

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Un niveau d'un violentomètre. Sa couleur est celle de l'échelle (vert,
 * jaune, orange, rouge) et ne sert qu'à elle : tout ce qui navigue ou
 * dit un état d'interface prend le violet.
 *
 * `id` n'existe que dans un violentomètre ; les niveaux du point en
 * sont dépourvus (ce sont les niveaux par défaut, recopiés).
 */
@Serializable
data class Niveau(
    val position: Int,
    val label: String,
    @SerialName("color") val couleur: String = "",
    val id: String? = null,
)
