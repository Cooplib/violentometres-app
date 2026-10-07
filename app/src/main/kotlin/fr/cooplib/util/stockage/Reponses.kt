package fr.cooplib.util.stockage

import android.content.Context
import fr.cooplib.util.modeles.decodage
import fr.cooplib.util.point.EtatDuPoint
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.io.File

/*
 * Les réponses au point : LA SEULE CHOSE QUI PARLE DE LA PERSONNE.
 *
 * Rien n'en est gardé par défaut, comme sur le site : elles vivent en
 * mémoire, et disparaissent quand l'application se reverrouille. Gardées
 * sur le téléphone seulement si la personne l'a choisi, et effacées par
 * « Quitter vite » comme par « Ne plus les garder ».
 *
 * Tous les points dans un seul fichier, au nom muet : un fichier par
 * violentomètre nommerait, de l'extérieur, celui sur lequel quelqu'un
 * fait le point (le site a eu ce défaut avec ses clés de stockage).
 */
object Reponses {

    private val forme = MapSerializer(String.serializer(), EtatDuPoint.serializer())

    private fun fichier(c: Context) = File(c.applicationContext.filesDir, "p.json")

    private fun tout(c: Context): Map<String, EtatDuPoint> =
        runCatching { decodage.decodeFromString(forme, fichier(c).readText()) }.getOrDefault(emptyMap())

    fun lire(c: Context, cle: String): EtatDuPoint? = tout(c)[cle]

    fun ecrire(c: Context, cle: String, etat: EtatDuPoint) {
        val f = fichier(c)
        val provisoire = File(f.parentFile, "p.json.provisoire")
        provisoire.writeText(decodage.encodeToString(forme, tout(c) + (cle to etat)))
        provisoire.renameTo(f)
    }

    fun oublier(c: Context, cle: String) {
        val reste = tout(c) - cle
        if (reste.isEmpty()) effacer(c) else fichier(c).writeText(decodage.encodeToString(forme, reste))
    }

    fun effacer(c: Context) {
        fichier(c).delete()
    }
}
