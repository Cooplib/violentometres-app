package fr.cooplib.util.ecrans

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/*
 * Revenir sur les choix du premier lancement : on peut avoir de bonnes
 * raisons de changer d'avis, dans un sens comme dans l'autre.
 */
@Composable
fun ReglagesEcran(
    deguise: Boolean,
    garderLesReponses: Boolean,
    deguiser: (empreinteDuCode: String) -> Unit,
    afficherLeVraiNom: () -> Unit,
    changerGarder: (Boolean) -> Unit,
    voirCeQuiEstProtege: () -> Unit,
    miseAJour: @Composable () -> Unit,
) {

    var choixCode by rememberSaveable { mutableStateOf(false) }

    Page {

        Text("Réglages", style = MaterialTheme.typography.headlineSmall)

        Text("Sur l'écran d'accueil", style = MaterialTheme.typography.titleMedium)

        Text(if (deguise) "L'application s'appelle Calculatrice, et s'ouvre avec votre code." else "L'application s'appelle Violentomètres.")

        when {
            choixCode -> ChoixDuCode(valider = { deguiser(it); choixCode = false }, annuler = { choixCode = false })
            deguise -> {
                Button(onClick = { choixCode = true }, Modifier.fillMaxWidth()) { Text("Changer le code") }
                OutlinedButton(onClick = afficherLeVraiNom, Modifier.fillMaxWidth()) { Text("Afficher le vrai nom") }
            }
            else -> Button(onClick = { choixCode = true }, Modifier.fillMaxWidth()) { Text("Redevenir une calculatrice") }
        }

        Text(
            "Le changement de nom peut prendre quelques secondes, et retirer l'icône de l'écran d'accueil. L'application reste dans la liste de toutes les applications.",
            style = MaterialTheme.typography.bodySmall,
        )

        Text("Vos réponses au point", style = MaterialTheme.typography.titleMedium)

        Text(if (garderLesReponses) "Elles sont gardées sur ce téléphone, pour reprendre plus tard." else "Elles ne sont pas gardées : en quittant, elles disparaissent.")

        OutlinedButton(onClick = { changerGarder(!garderLesReponses) }, Modifier.fillMaxWidth()) {
            Text(if (garderLesReponses) "Ne plus les garder" else "Les garder sur ce téléphone")
        }

        miseAJour()

        TextButton(onClick = voirCeQuiEstProtege) { Text("Ce qui est protégé, ce qui ne l'est pas") }
    }
}
