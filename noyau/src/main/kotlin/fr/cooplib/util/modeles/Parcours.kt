package fr.cooplib.util.modeles

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Un parcours dans la liste (`GET /parcours`) : sans ses étapes, qu'il
 * faut demander une à une. 20 parcours font 14 Ko ; avec leurs étapes,
 * environ 9 Ko chacun, parce que chaque étape embarque son
 * violentomètre entier.
 */
@Serializable
data class ParcoursResume(
    val id: String,
    @SerialName("title") val titre: String,
    val description: String = "",
    @SerialName("step_count") val nombreDEtapes: Int = 0,
    val cadres: List<CadreResume> = emptyList(),
    @SerialName("updated_at") val modifieLe: String = "",
)

@Serializable
data class Parcours(
    val id: String,
    @SerialName("title") val titre: String,
    val description: String = "",
    val apropos: String = "",
    val conclusion: String = "",
    @SerialName("steps") val etapes: List<Etape> = emptyList(),
    @SerialName("updated_at") val modifieLe: String = "",
)

/*
 * Une étape : une carte à l'écran. Elle désigne un violentomètre, un
 * mécanisme ou un cadre d'analyse ; `cible` en donne le titre et un
 * résumé court, de quoi faire la carte sans aller chercher l'élément.
 * Le texte entier d'un cadre ne vient qu'avec « en savoir plus ».
 */
@Serializable
data class Etape(
    val id: String,
    val type: String,
    @SerialName("violentometer_id") val violentometreId: String? = null,
    @SerialName("mecanisme_id") val mecanismeId: String? = null,
    @SerialName("cadre_id") val cadreId: String? = null,
    val cible: Cible? = null,
    val position: Int = 0,
    val note: String = "",
)

@Serializable
data class Cible(
    val id: String,
    val type: String,
    val titre: String,
    val resume: String = "",
)
