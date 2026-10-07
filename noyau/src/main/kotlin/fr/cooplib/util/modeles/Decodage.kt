package fr.cooplib.util.modeles

import kotlinx.serialization.json.Json

/*
 * Une seule façon de lire l'API, pour tout le noyau.
 *
 * `ignoreUnknownKeys` : l'API envoie bien plus que ce dont
 * l'application se sert (compteurs de vues, scores, discussions), et
 * elle gagnera des champs. Une application installée ne se met pas à
 * jour le jour où l'API change : un champ nouveau ne doit rien casser.
 *
 * `coerceInputValues` : un `null` reçu là où le modèle a une valeur par
 * défaut prend cette valeur. Les colonnes de l'API sont presque toutes
 * nullables (`description or ""` n'est fait qu'à certains endroits), et
 * un violentomètre sans description ne doit pas rendre tout le
 * catalogue illisible.
 */
val decodage = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}
