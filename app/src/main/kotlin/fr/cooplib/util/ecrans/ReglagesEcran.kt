package fr.cooplib.util.ecrans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/*
 * Revenir sur les choix du premier lancement : on peut avoir de bonnes
 * raisons de changer d'avis, dans un sens comme dans l'autre.
 */
@Composable
fun ReglagesEcran(
    deguise: Boolean,
    garderLesReponses: Boolean,
    capturesPermises: Boolean,
    changerCaptures: (Boolean) -> Unit,
    theme: String,
    changerTheme: (String) -> Unit,
    changerLeCode: (empreinteDuCode: String) -> Unit,
    deguiser: () -> Unit,
    afficherLeVraiNom: () -> Unit,
    changerGarder: (Boolean) -> Unit,
    voirCeQuiEstProtege: () -> Unit,
    revoirLeDidacticiel: () -> Unit,
    miseAJour: @Composable () -> Unit,
) {

    var choixCode by rememberSaveable { mutableStateOf(false) }
    var confirmerCaptures by rememberSaveable { mutableStateOf(false) }

    Page {

        Text("Réglages", style = MaterialTheme.typography.headlineSmall)

        Text("Sur l'écran d'accueil", style = MaterialTheme.typography.titleMedium)

        Encadre(
            if (deguise) "L'application s'appelle Calculatrice, et s'ouvre avec votre code."
            else "L'application s'appelle Violentomètres. Votre code reste celui de secours : « Quitter vite » remet la calculatrice."
        )

        if (choixCode) {
            ChoixDuCode(valider = { changerLeCode(it); choixCode = false }, annuler = { choixCode = false })
        } else {
            if (deguise) {
                OutlinedButton(onClick = afficherLeVraiNom, Modifier.fillMaxWidth()) { Text("Afficher le vrai nom") }
            } else {
                Button(onClick = deguiser, Modifier.fillMaxWidth()) { Text("Redevenir une calculatrice") }
            }
            OutlinedButton(onClick = { choixCode = true }, Modifier.fillMaxWidth()) { Text("Changer le code") }
        }

        Note("Le changement de nom peut prendre quelques secondes, et retirer l'icône de l'écran d'accueil. L'application reste dans la liste de toutes les applications.")

        Text("Vos réponses au point", style = MaterialTheme.typography.titleMedium)

        Text(
            if (garderLesReponses) "Elles sont gardées sur ce téléphone, pour reprendre plus tard, avec vos brouillons, vos « J'aime » et les récits déjà lus."
            else "Elles ne sont pas gardées : en quittant, elles disparaissent, comme vos brouillons, vos « J'aime » et les récits déjà lus."
        )

        OutlinedButton(onClick = { changerGarder(!garderLesReponses) }, Modifier.fillMaxWidth()) {
            Text(if (garderLesReponses) "Ne plus les garder" else "Les garder sur ce téléphone")
        }

        Text("Captures d'écran", style = MaterialTheme.typography.titleMedium)

        if (capturesPermises) {
            Text("Elles sont permises.")
            RisquesDesCaptures()
            Button(onClick = { changerCaptures(false) }, Modifier.fillMaxWidth()) { Text("Les refuser de nouveau") }
        } else {
            Text("Elles sont refusées : l'application ne peut être ni capturée, ni enregistrée, ni partagée en vidéo.")
            OutlinedButton(onClick = { confirmerCaptures = true }, Modifier.fillMaxWidth()) { Text("Permettre les captures") }
        }

        if (confirmerCaptures) {
            Dialog(onDismissRequest = { confirmerCaptures = false }) {
                Surface(shape = Charte.ArrondiGrand, color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Permettre les captures ?", style = MaterialTheme.typography.titleMedium)
                        RisquesDesCaptures()
                        Button(onClick = { changerCaptures(true); confirmerCaptures = false }, Modifier.fillMaxWidth()) { Text("Les permettre") }
                        TextButton(onClick = { confirmerCaptures = false }) { Text("Annuler") }
                    }
                }
            }
        }

        Text("Affichage", style = MaterialTheme.typography.titleMedium)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf("auto" to "Comme le téléphone", "clair" to "Clair", "sombre" to "Sombre")) { (cle, libelle) ->
                Pastille(libelle, theme == cle) { changerTheme(cle) }
            }
        }

        miseAJour()

        TextButton(onClick = revoirLeDidacticiel) { Text("Revoir le didacticiel") }
        TextButton(onClick = voirCeQuiEstProtege) { Text("Ce qui est protégé, ce qui ne l'est pas") }
    }
}
