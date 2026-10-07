package fr.cooplib.util.modeles

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * `GET /history/recent` : les versions de tout le site, la plus récente
 * d'abord. C'est la sonde de la synchronisation.
 *
 * Le contenu semé n'y apparaît PAS : le semis ne crée pas de versions.
 * En production le 7 octobre 2026, la liste était même vide — personne
 * n'avait encore rien modifié à la main. Une sonde vide ne veut donc
 * pas dire « rien de neuf depuis toujours », d'où le rechargement
 * complet de temps en temps.
 */
@Serializable
data class Historique(
    @SerialName("items") val versions: List<Version> = emptyList(),
    @SerialName("has_more") val encore: Boolean = false,
)

@Serializable
data class Version(
    val id: String,
    @SerialName("kind") val sorte: String = "",
    @SerialName("resource_type") val typeElement: String,
    @SerialName("resource_id") val elementId: String,
    @SerialName("title") val titre: String = "",
    @SerialName("updated_at") val modifieLe: String = "",
    // Une situation absorbée par une autre : c'est l'autre qu'il faut
    // recharger.
    @SerialName("merged_into_id") val absorbeeDans: String? = null,
)
