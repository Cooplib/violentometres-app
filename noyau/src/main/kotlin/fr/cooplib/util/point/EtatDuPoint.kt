package fr.cooplib.util.point

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Où l'on en est d'un point. Le même état que celui du site
 * (../violentometres-frontend/src/utils/deroule.js), champ pour champ,
 * pour que les deux se comparent après chaque action.
 *
 * C'est la seule donnée de l'application qui parle de la personne :
 * rien n'en est gardé par défaut, et rien n'en sort de l'appareil. Le
 * site envoie « un point a été fait » à la fin (`/done`) ;
 * l'application non, c'est décidé (voir les notes de conception).
 */
@Serializable
data class EtatDuPoint(
    val phase: Phase = Phase.INTRO,
    // « personne » : je réponds sur ce que je vis ; « proche » : sur ce
    // que j'observe. Change l'ordre des aides, rien d'autre.
    @SerialName("audience") val public: String? = null,
    // L'indice dans les niveaux PROPOSÉS, pas la position du niveau :
    // un niveau sans situation est sauté.
    @SerialName("level") val niveau: Int = 0,
    val index: Int = 0,
    @SerialName("answers") val reponses: Map<String, Reponse> = emptyMap(),
    val aidesVues: List<String> = emptyList(),
    @SerialName("finishedAt") val finiLe: Long? = null,
    val situationVue: String? = null,
    val reprise: Phase? = null,
)

@Serializable
enum class Phase {
    @SerialName("intro") INTRO,
    @SerialName("questions") QUESTIONS,
    // Avant chaque niveau plus dur : continuer, souffler, s'arrêter.
    @SerialName("sas") SAS,
    @SerialName("positifs") POSITIFS,
    @SerialName("aide") AIDE,
    @SerialName("alerte") ALERTE,
    @SerialName("pause") PAUSE,
    @SerialName("resultat") RESULTAT,
}

@Serializable
enum class Reponse {
    // Seul « oui » reconnaît une situation.
    @SerialName("oui") OUI,
    @SerialName("non") NON,
    @SerialName("passer") PASSER,
}
