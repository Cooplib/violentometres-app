package fr.cooplib.util.modeles

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Une aide : qui appeler, où aller. C'est la première entrée de
 * l'application, et le numéro doit s'appeler d'un seul appui.
 *
 * `what`, `contact`, `phone` et `url` sont nullables en base : une aide
 * peut n'être qu'un site, ou qu'un numéro. Rien n'est inventé ici pour
 * combler un manque.
 */
@Serializable
data class Aide(
    val id: String,
    @SerialName("title") val titre: String,
    @SerialName("what") val quoi: String? = null,
    val contact: String? = null,
    @SerialName("phone") val telephone: String? = null,
    val url: String? = null,
    @SerialName("kind") val sorte: String = "",
    @SerialName("audience") val public: String = "",
    // À partir de quel niveau atteint elle est proposée à la fin du point.
    @SerialName("min_level") val niveauMinimum: Int = 0,
    @SerialName("country") val pays: String = "",
    val national: Boolean = false,
    // Seulement dans le point : les situations qui l'appellent.
    @SerialName("behavior_ids") val situationIds: List<String> = emptyList(),
)
