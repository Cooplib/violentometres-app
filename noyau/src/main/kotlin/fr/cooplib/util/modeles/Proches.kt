package fr.cooplib.util.modeles

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Ce qui ressemble à un violentomètre, tel que LE SERVEUR le calcule
 * (`GET /violentometers/{id}/related`) : les parcours dont il est une
 * étape, les violentomètres de ces parcours, ceux qui partagent ses
 * situations, ceux du même cadre d'analyse ou de cadres voisins.
 *
 * Embarqué dans le catalogue tel quel, pas recalculé ici : refaire ce
 * rapprochement sur le téléphone, ce serait une logique de plus du
 * serveur portée à la main, qui divergerait comme les autres.
 *
 * Chaque groupe ne garde que les identifiants : les éléments eux-mêmes
 * sont déjà dans le catalogue. Un violentomètre n'apparaît que dans un
 * groupe (le serveur y veille), dans l'ordre du plus fort signal au
 * plus faible.
 */
@Serializable
data class Proches(
    // Les parcours dont il est une étape : le signal le plus fort.
    val parcours: List<Reference> = emptyList(),
    @SerialName("same_parcours") val memesParcours: List<Proche> = emptyList(),
    @SerialName("shared_behaviors") val situationsCommunes: List<Proche> = emptyList(),
    @SerialName("meme_cadre") val memeCadre: List<Proche> = emptyList(),
    @SerialName("cadres_voisins") val cadresVoisins: List<Proche> = emptyList(),
) {
    // Les violentomètres, groupes confondus, dans l'ordre du serveur.
    val violentometres: List<Proche>
        get() = (memesParcours + situationsCommunes + memeCadre + cadresVoisins).distinctBy { it.violentometre.id }
}

/*
 * Un violentomètre proche, et POURQUOI : de quoi dire à l'écran « 9
 * situations en commun » ou « dans le parcours La famille », comme le
 * site, plutôt qu'une liste sans raison.
 */
@Serializable
data class Proche(
    @SerialName("violentometer") val violentometre: Reference,
    @SerialName("shared_count") val enCommun: Int = 0,
    val parcours: List<Reference> = emptyList(),
)

@Serializable
data class Reference(val id: String, @SerialName("title") val titre: String = "")
