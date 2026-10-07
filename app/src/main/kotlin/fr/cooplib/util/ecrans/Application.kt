package fr.cooplib.util.ecrans

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.cooplib.util.donnees.Catalogue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * Quatre entrées, et l'ordre est le propos : du plus proche de soi au
 * plus distant. J'ai besoin de quelque chose maintenant, où j'en suis,
 * d'autres l'ont vécu, d'où ça vient.
 */
enum class Entree(val titre: String, val sousTitre: String) {
    AIDE("Trouver de l'aide", "Un numéro, à appeler tout de suite."),
    POINT("Faire le point", "Où j'en suis, en général ou sur un sujet précis."),
    RECITS("Des récits", "Ce que d'autres ont vécu, et déposer le sien."),
    COMPRENDRE("Comprendre", "Des parcours, une carte par étape."),
}

@Composable
fun Application() {

    ThemeUtil {

        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

            // Le catalogue embarqué : 1,1 Mo de JSON, lu hors du fil de
            // l'interface pour que le premier écran ne gèle pas.
            val catalogue by produceState<Catalogue?>(null) {
                value = withContext(Dispatchers.Default) { Catalogue.embarque() }
            }

            var ouverte by rememberSaveable { mutableStateOf<Entree?>(null) }

            BackHandler(enabled = ouverte != null) { ouverte = null }

            Box(Modifier.safeDrawingPadding()) {
                val c = catalogue
                when {
                    c == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    ouverte == null -> Accueil { ouverte = it }
                    ouverte == Entree.AIDE -> Aides(c.aides)
                    else -> AVenir(ouverte!!)
                }
            }
        }
    }
}

@Composable
private fun Accueil(ouvrir: (Entree) -> Unit) {

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        for (e in Entree.entries) {
            Card(onClick = { ouvrir(e) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(e.titre, style = MaterialTheme.typography.titleLarge)
                    Text(e.sousTitre, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun AVenir(e: Entree) {
    Column(Modifier.padding(16.dp)) {
        Text(e.titre, style = MaterialTheme.typography.headlineSmall)
        Text("Pas encore dans cette version.", Modifier.padding(top = 8.dp))
    }
}
