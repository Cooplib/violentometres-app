package fr.cooplib.util.ecrans

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PersistableBundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/*
 * Ouvrir un site dans le navigateur : le site violentometres.fr pour
 * contribuer, ou celui d'une aide.
 *
 * L'APPLICATION SE CACHE, LE NAVIGATEUR NON. L'adresse ouverte reste
 * dans son historique, revient dans les suggestions quand on commence à
 * taper, et, si la synchronisation est active, dans le compte Google.
 * D'où un avertissement la première fois (retour du 8 octobre 2026),
 * qu'on peut ne plus voir, et un second écran qui dit comment effacer.
 *
 * Tous les liens vers l'extérieur passent par ici, y compris ceux des
 * aides, qui ouvraient le navigateur sans rien dire : une règle qui ne
 * vaut que pour certains liens ne protège de rien.
 */

// Ouvrir une adresse, en prévenant si besoin : fourni par l'écran qui
// porte l'avertissement (Application), lu par les cartes qui ont un lien.
val LocalOuvrirUnSite = compositionLocalOf<(String) -> Unit> { { } }

const val SITE = "https://violentometres.fr"

fun ouvrirDansLeNavigateur(contexte: Context, adresse: String) {
    try {
        contexte.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(adresse)))
    } catch (_: ActivityNotFoundException) {
    }
}

/*
 * La copie est marquée « sensible » (Android 13 et après) : l'aperçu du
 * presse-papiers ne l'affiche pas. Mais certains claviers gardent un
 * historique de ce qu'on copie : l'écran le dit.
 */
private fun copier(contexte: Context, adresse: String) {
    val pp = contexte.getSystemService(ClipboardManager::class.java) ?: return
    val clip = ClipData.newPlainText("", adresse)
    if (Build.VERSION.SDK_INT >= 33) {
        clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
    }
    pp.setPrimaryClip(clip)
}

@Composable
fun AvertissementSite(adresse: String, nePlusPrevenir: (Boolean) -> Unit, fermer: () -> Unit) {

    val contexte = LocalContext.current
    var page by remember { mutableStateOf(0) }
    var plusJamais by remember { mutableStateOf(false) }
    var copie by remember { mutableStateOf(false) }

    fun ouvrir() {
        nePlusPrevenir(plusJamais)
        fermer()
        ouvrirDansLeNavigateur(contexte, adresse)
    }

    Dialog(onDismissRequest = fermer) {
        Surface(shape = Charte.ArrondiGrand, color = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (page == 0) {

                    Text("Vous allez ouvrir un site", style = MaterialTheme.typography.titleLarge)
                    Note(adresse)
                    Text("Ce lien s'ouvre dans votre navigateur, en dehors de l'application. Le navigateur, lui, ne se cache pas : l'adresse restera dans son historique, et reviendra dans les suggestions quand on commence à taper.")

                    Encadre(titre = "Pour ne rien laisser") {
                        Text("Une fenêtre de navigation privée. Copiez l'adresse, puis collez-la dans une fenêtre privée de votre navigateur.")
                        OutlinedButton(onClick = { copier(contexte, adresse); copie = true }) { Text(if (copie) "Adresse copiée" else "Copier l'adresse") }
                        if (copie) Note("Certains claviers gardent ce qu'on a copié : effacez-le de leur presse-papiers ensuite.")
                    }

                    Row(Modifier.fillMaxWidth().clickable { plusJamais = !plusJamais }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = plusJamais, onCheckedChange = { plusJamais = it })
                        Text("Ne plus me prévenir", style = MaterialTheme.typography.bodyMedium)
                    }

                    Button(onClick = ::ouvrir, Modifier.fillMaxWidth()) { Text("Ouvrir le site") }
                    TextButton(onClick = { page = 1 }) { Text("Comment l'effacer ensuite ?") }
                    TextButton(onClick = fermer) { Text("Annuler") }

                } else {

                    Text("Effacer l'adresse ensuite", style = MaterialTheme.typography.titleLarge)
                    Text("Dans l'historique du navigateur, après votre visite :")
                    Encadre(titre = "Chrome") { Text("Menu ⋮, puis Historique : touchez la croix à côté de la page. Ou « Effacer les données de navigation » pour tout effacer.") }
                    Encadre(titre = "Firefox") { Text("Menu ☰, puis Historique : appui long sur la page, puis Supprimer.") }
                    Encadre(titre = "Samsung Internet") { Text("Menu ☰, puis Historique, puis Modifier : choisissez la page et supprimez-la.") }
                    Note("Dans les autres navigateurs : le menu, Historique, puis supprimer la page.")
                    Encadre(alerte = true, titre = "Le compte Google") {
                        Text("Si Chrome est synchronisé avec votre compte, l'historique y est aussi gardé : myactivity.google.com, à vérifier depuis un endroit sûr.")
                    }
                    Button(onClick = ::ouvrir, Modifier.fillMaxWidth()) { Text("Ouvrir le site") }
                    TextButton(onClick = { page = 0 }) { Text("Retour") }
                }
            }
        }
    }
}
