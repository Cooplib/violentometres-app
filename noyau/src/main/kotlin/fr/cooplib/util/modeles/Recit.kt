package fr.cooplib.util.modeles

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Un récit. `sansFlou` est le choix de qui l'a déposé : « ce récit peut
 * s'afficher sans flou ». Faux, il s'affiche flouté jusqu'à ce qu'on
 * décide de le lire.
 */
@Serializable
data class Recit(
    val id: String,
    @SerialName("title") val titre: String,
    @SerialName("text") val texte: String,
    val apropos: String = "",
    @SerialName("safe") val sansFlou: Boolean = false,
    @SerialName("created_at") val creeLe: String = "",
    @SerialName("views_count") val vues: Int = 0,
    @SerialName("popular_score") val popularite: Int = 0,
)
