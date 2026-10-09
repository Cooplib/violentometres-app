package fr.cooplib.util.modeles

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Un violentomètre tel que la liste le donne (`GET /violentometers`) :
 * de quoi l'afficher dans une liste et le chercher, sans ses
 * situations.
 */
@Serializable
data class ViolentometreResume(
    val id: String,
    @SerialName("title") val titre: String,
    val description: String = "",
    @SerialName("levels") val niveaux: List<Niveau> = emptyList(),
    @SerialName("contexts") val contextes: List<Contexte> = emptyList(),
    val cadres: List<CadreResume> = emptyList(),
    @SerialName("updated_at") val modifieLe: String = "",
    // De quoi juger d'un coup d'œil, comme sur le site : combien l'ont
    // vu, aimé, et (pour un violentomètre) combien de situations il porte.
    // Figés au moment où le catalogue a été tiré, rafraîchis avec lui.
    @SerialName("views_count") val vues: Int = 0,
    @SerialName("like_count") val aime: Int = 0,
    @SerialName("popular_score") val popularite: Int = 0,
    @SerialName("behavior_count") val nombreDeSituations: Int = 0,
)

/*
 * Un violentomètre entier (`GET /violentometers/{id}`), avec ses
 * situations rangées par gravité.
 */
@Serializable
data class Violentometre(
    val id: String,
    @SerialName("title") val titre: String,
    val description: String = "",
    @SerialName("levels") val niveaux: List<Niveau> = emptyList(),
    @SerialName("contexts") val contextes: List<Contexte> = emptyList(),
    val cadres: List<CadreResume> = emptyList(),
    val apropos: String = "",
    val analyse: String = "",
    @SerialName("behaviors") val situations: List<Situation> = emptyList(),
    @SerialName("updated_at") val modifieLe: String = "",
    // De quoi juger d'un coup d'œil, comme sur le site : combien l'ont
    // vu, aimé, et (pour un violentomètre) combien de situations il porte.
    // Figés au moment où le catalogue a été tiré, rafraîchis avec lui.
    @SerialName("views_count") val vues: Int = 0,
    @SerialName("like_count") val aime: Int = 0,
    @SerialName("popular_score") val popularite: Int = 0,
)

/*
 * Une situation telle qu'un violentomètre l'emploie. `id` est celui du
 * lien, `situationId` celui de la situation, partagée entre
 * violentomètres : c'est le second qui sert partout ailleurs (le point,
 * les aides, les récits).
 *
 * `display_text` est le texte à montrer : la reformulation propre à ce
 * violentomètre s'il en a une, le texte commun sinon.
 */
@Serializable
data class Situation(
    val id: String,
    @SerialName("behavior_id") val situationId: String,
    @SerialName("display_text") val texte: String,
    val position: Int = 0,
    @SerialName("severity") val gravite: Int,
)

@Serializable
data class Contexte(
    val id: String,
    @SerialName("name") val nom: String,
)

@Serializable
data class CadreResume(
    val id: String,
    val nom: String,
)

/*
 * Une situation de la bibliothèque (`GET /situations/bibliotheque`) :
 * de quoi construire « Mon violentomètre » hors ligne. `niveauSuggere`
 * est le niveau où les violentomètres la rangent, calculé comme pour le
 * point d'orientation ; nul si aucun ne l'emploie.
 */
@Serializable
data class SituationDeBibliotheque(
    val id: String,
    @SerialName("text") val texte: String,
    val usages: Int = 0,
    @SerialName("severity") val niveauSuggere: Int? = null,
)
