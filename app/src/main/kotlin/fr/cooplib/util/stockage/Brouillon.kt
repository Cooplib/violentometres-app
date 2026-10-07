package fr.cooplib.util.stockage

import android.content.Context
import kotlinx.serialization.Serializable
import fr.cooplib.util.modeles.decodage
import java.io.File

/*
 * Un récit en cours d'écriture. MÊME RÉGIME QUE LES RÉPONSES AU POINT :
 * gardé sur le téléphone seulement si la personne a choisi de garder
 * ses réponses (second écran du premier lancement, ou les réglages).
 * Sinon il vit en mémoire, et disparaît quand l'application se
 * reverrouille.
 */
@Serializable
data class Brouillon(val titre: String = "", val texte: String = "", val sansFlou: Boolean = false) {

    val vide get() = titre.isBlank() && texte.isBlank()

    companion object {

        private fun fichier(c: Context) = File(c.applicationContext.filesDir, "b.json")

        fun lire(c: Context): Brouillon =
            runCatching { decodage.decodeFromString(serializer(), fichier(c).readText()) }.getOrDefault(Brouillon())

        fun ecrire(c: Context, b: Brouillon) {
            if (b.vide) effacer(c) else fichier(c).writeText(decodage.encodeToString(serializer(), b))
        }

        fun effacer(c: Context) {
            fichier(c).delete()
        }
    }
}
