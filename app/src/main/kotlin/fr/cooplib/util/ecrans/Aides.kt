package fr.cooplib.util.ecrans

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fr.cooplib.util.modeles.Aide

/*
 * « Trouver de l'aide » : la première entrée, parce que c'est celle dont
 * on a besoin quand ça va mal. Les aides nationales d'abord : elles
 * répondent partout, tout de suite.
 *
 * Appeler passe par le composeur (ACTION_DIAL), numéro déjà tapé : un
 * appui de plus que ACTION_CALL, mais sans la permission « Téléphone »,
 * qui se lit dans Réglages → Applications et ferait une trace de plus.
 * L'appel passé, lui, reste dans le journal du téléphone, comme tout
 * appel : la page « ce qui est protégé » devra le dire.
 */
@Composable
fun Aides(aides: List<Aide>) {

    val rangees = aides.sortedWith(compareByDescending<Aide> { it.national }.thenBy { it.titre })

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        item { Text("🆘 Trouver de l'aide", style = MaterialTheme.typography.headlineSmall) }

        item { Note("Les aides nationales d'abord : elles répondent partout, tout de suite. En cas de danger immédiat : 17, ou 114 par SMS si vous ne pouvez pas parler.") }

        items(rangees, key = { it.id }) { aide -> CarteAide(aide) }
    }
}

// Une aide : ce qu'elle fait, comment la joindre. Reprise par le point,
// à chaque étape où une aide se propose.
@Composable
fun CarteAide(aide: Aide) {

    val contexte = LocalContext.current

    Card(Modifier.fillMaxWidth()) {

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {

            Text(aide.titre, style = MaterialTheme.typography.titleMedium)

            aide.quoi?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

            aide.contact?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

            aide.telephone?.takeIf { it.isNotBlank() }?.let { numero ->
                Button(onClick = { ouvrir(contexte, Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + numero.filter { it.isDigit() || it == '+' }))) }) {
                    Text("📞 Appeler le $numero")
                }
            }

            aide.url?.takeIf { it.isNotBlank() }?.let { url ->
                OutlinedButton(onClick = { ouvrir(contexte, Intent(Intent.ACTION_VIEW, Uri.parse(url))) }) {
                    Text("🔗 Ouvrir le site")
                }
            }
        }
    }
}

// Un téléphone sans composeur (une tablette) : ne pas planter.
private fun ouvrir(contexte: android.content.Context, intention: Intent) {
    try {
        contexte.startActivity(intention)
    } catch (_: ActivityNotFoundException) {
    }
}
