package fr.cooplib.util.stockage

import android.content.Context
import fr.cooplib.util.modeles.decodage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import java.io.File

/*
 * Ce dont l'application se souvient de la personne, en dehors du point :
 * ce qu'elle a aimé, et les récits qu'elle a déjà dévoilés (pour ne plus
 * les lui cacher).
 *
 * C'est une trace sur soi : ce qu'on a lu, ce qu'on a aimé. Même régime
 * que les réponses au point, décidé le 7 octobre 2026 : en mémoire tant
 * que l'application tourne, gardé sur le téléphone SEULEMENT si la
 * personne a choisi de garder ses réponses, effacé par « Quitter vite »
 * et par « Ne plus les garder ».
 */
@Serializable
data class Souvenirs(
    val aimes: Set<String> = emptySet(),
    val lus: Set<String> = emptySet(),
    // Les récits que la personne a choisi de recacher, pour elle-même,
    // même ceux déclarés « sans flou » (retour du 8 octobre 2026).
    val caches: Set<String> = emptySet(),
    // Le nombre de likes que le serveur a rendu après un like : plus juste
    // que celui du catalogue, tiré avant.
    val comptes: Map<String, Int> = emptyMap(),
)

class Memoire(contexte: Context, private val garder: () -> Boolean) {

    private val fichier = File(contexte.applicationContext.filesDir, "m.json")

    private val _etat = MutableStateFlow(
        if (garder()) runCatching { decodage.decodeFromString(Souvenirs.serializer(), fichier.readText()) }.getOrDefault(Souvenirs())
        else Souvenirs()
    )
    val etat: StateFlow<Souvenirs> = _etat

    fun changer(f: (Souvenirs) -> Souvenirs) {
        _etat.value = f(_etat.value)
        if (garder()) fichier.writeText(decodage.encodeToString(Souvenirs.serializer(), _etat.value))
    }

    // « Quitter vite », « Ne plus les garder » : tout part, la mémoire et
    // le fichier.
    fun oublier() {
        _etat.value = Souvenirs()
        fichier.delete()
    }

    // « Les garder » : ce qui est en mémoire est écrit tout de suite.
    fun garderMaintenant() {
        if (garder()) fichier.writeText(decodage.encodeToString(Souvenirs.serializer(), _etat.value))
    }
}
