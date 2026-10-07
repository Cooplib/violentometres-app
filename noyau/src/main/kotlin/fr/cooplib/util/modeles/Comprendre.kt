package fr.cooplib.util.modeles

import kotlinx.serialization.Serializable

/*
 * Ce qui fait « comprendre » : les cadres d'analyse et les mécanismes,
 * vers lesquels mènent les étapes des parcours et la fin du point.
 *
 * Une étape s'affiche par son résumé (court : 143 et 116 signes de
 * médiane) ; le reste vient avec « en savoir plus ». Au 7 octobre 2026,
 * aucun cadre n'a encore de texte long en production : « en savoir
 * plus » montre alors les références, qui, elles, existent.
 */
@Serializable
data class Cadre(
    val id: String,
    val nom: String,
    val resume: String = "",
    val description: String = "",
    val apropos: String = "",
    val references: List<String> = emptyList(),
)

@Serializable
data class Mecanisme(
    val id: String,
    val nom: String,
    val resume: String = "",
    val description: String = "",
    val apropos: String = "",
)

// La liste des mécanismes (`GET /mecanismes`), de quoi aller chercher
// chaque fiche ; celle des cadres prend CadreResume.
@Serializable
data class MecanismeResume(
    val id: String,
    val nom: String,
)
