package fr.cooplib.util.stockage

import android.content.Context
import fr.cooplib.util.modeles.decodage
import fr.cooplib.util.perso.MonViolentometre
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

/*
 * Mes violentomètres, construits sur le téléphone.
 *
 * LA DONNÉE LA PLUS SENSIBLE DE L'APPLICATION : une échelle qui décrit
 * sa propre relation, parfois avec ses propres mots. Même régime que les
 * réponses au point, sans exception : en mémoire tant que l'application
 * tourne, gardée sur le téléphone seulement si la personne a choisi de
 * garder ses réponses, effacée par « Quitter vite » et par « Ne plus les
 * garder ». Rien n'est jamais envoyé.
 *
 * Un fichier au nom muet, comme les autres.
 */
class Carnet(contexte: Context, private val garder: () -> Boolean) {

    private val fichier = File(contexte.applicationContext.filesDir, "e.json")
    private val forme = ListSerializer(MonViolentometre.serializer())

    private val _liste = MutableStateFlow(
        if (garder()) runCatching { decodage.decodeFromString(forme, fichier.readText()) }.getOrDefault(emptyList())
        else emptyList()
    )
    val liste: StateFlow<List<MonViolentometre>> = _liste

    fun changer(f: (List<MonViolentometre>) -> List<MonViolentometre>) {
        _liste.value = f(_liste.value)
        garderMaintenant()
    }

    fun remplacer(vm: MonViolentometre) = changer { l -> l.map { if (it.id == vm.id) vm else it } }

    fun oublier() {
        _liste.value = emptyList()
        fichier.delete()
    }

    fun garderMaintenant() {
        if (garder()) fichier.writeText(decodage.encodeToString(forme, _liste.value))
    }
}
