package fr.cooplib.util.ecrans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import fr.cooplib.util.leurre.CodeSecret

/*
 * Les deux écrans du premier lancement (APPLICATION-ANDROID.md,
 * chapitre 3).
 *
 * Le premier : le déguisement, et SES LIMITES DITES AU MÊME ENDROIT.
 * Promettre une protection qu'on ne tient pas mettrait quelqu'un en
 * danger en lui donnant une confiance qu'il n'a pas lieu d'avoir.
 *
 * Le second : une seule question, mais sur la bonne chose. Le contenu
 * du site est gardé, il ne dit rien de la personne ; ses réponses,
 * elles, parlent d'elle.
 *
 * Le ton est celui de la page « Ne pas laisser de traces » du site :
 * des phrases courtes, des consignes, pas d'alarme. Quelqu'un qui lit
 * ceci a déjà peur.
 */

@Composable
fun Deguisement(
    garderLaCalculatrice: (empreinteDuCode: String) -> Unit,
    afficherLeVraiNom: (empreinteDuCode: String) -> Unit,
    voirCeQuiEstProtege: () -> Unit,
) {

    // Le choix fait, avant le code : « calculatrice » ou « vrai ».
    var choix by rememberSaveable { mutableStateOf<String?>(null) }

    Page {

        Text("Avant de commencer", style = MaterialTheme.typography.headlineSmall)

        Text("Sur votre écran d'accueil, cette application s'appelle Calculatrice. Elle en a l'icône, et elle calcule pour de vrai.", style = MaterialTheme.typography.bodyLarge)

        Encadre("Pour ouvrir l'application : tapez votre code dans la calculatrice, puis =. L'écran se vide aussitôt.")

        when (choix) {

            null -> {
                Button(onClick = { choix = "calculatrice" }, Modifier.fillMaxWidth()) { Text("Garder la calculatrice") }
                OutlinedButton(onClick = { choix = "vrai" }, Modifier.fillMaxWidth()) { Text("Afficher le vrai nom : Violentomètres") }
            }

            else -> {
                /*
                 * Un code pour tout le monde, décidé le 7 octobre 2026 : avec
                 * le vrai nom, c'est le code DE SECOURS. « Quitter vite » remet
                 * alors la calculatrice, parce que si l'on a voulu quitter
                 * vite, c'est qu'il y a sans doute une galère.
                 */
                if (choix == "vrai") {
                    Encadre(titre = "Un code de secours") {
                        Text("Même avec le vrai nom, choisissez un code. Si vous touchez « Quitter vite », l'application redevient une calculatrice, et il faudra ce code pour la rouvrir.")
                    }
                }
                ChoixDuCode(
                    valider = { e -> if (choix == "vrai") afficherLeVraiNom(e) else garderLaCalculatrice(e) },
                    annuler = { choix = null },
                )
            }
        }

        Encadre(titre = "Ce que ça protège") {
            Text("Quelqu'un qui prend votre téléphone et regarde l'écran d'accueil.")
        }

        Encadre(alerte = true, titre = "Ce que ça ne protège pas") {
            Text("Quelqu'un qui cherche : dans les réglages du téléphone, dans votre compte Google. Quelqu'un qui vous oblige à ouvrir l'application. Un logiciel espion.")
        }

        TextButton(onClick = voirCeQuiEstProtege) { Text("Ce qui est protégé, ce qui ne l'est pas") }
    }
}

@Composable
fun ChoixDuCode(valider: (empreinteDuCode: String) -> Unit, annuler: () -> Unit) {

    var code by rememberSaveable { mutableStateOf("") }
    var encore by rememberSaveable { mutableStateOf("") }

    val probleme = when {
        code.isNotEmpty() && !CodeSecret.valide(code) -> "De 4 à 12 chiffres."
        encore.isNotEmpty() && encore != code -> "Les deux codes ne sont pas les mêmes."
        else -> null
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // Un clavier de code : les claviers n'apprennent pas ce qu'on tape
        // dans un champ de mot de passe.
        OutlinedTextField(
            value = code,
            onValueChange = { code = it.filter(Char::isDigit).take(12) },
            label = { Text("Votre code, de 4 à 12 chiffres") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, autoCorrectEnabled = false),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = encore,
            onValueChange = { encore = it.filter(Char::isDigit).take(12) },
            label = { Text("Le même, encore une fois") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, autoCorrectEnabled = false),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        probleme?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Text(
            "Évitez une date de naissance ou un code que l'on connaît de vous. Si vous l'oubliez, désinstallez puis réinstallez : vous ne perdez rien, l'application ne garde rien de vous par défaut.",
            style = MaterialTheme.typography.bodySmall,
        )

        Button(
            onClick = { valider(CodeSecret.empreinte(code)) },
            enabled = CodeSecret.valide(code) && code == encore,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Valider ce code") }

        TextButton(onClick = annuler) { Text("Revenir") }
    }
}

@Composable
fun CeQuOnGarde(choisir: (garderLesReponses: Boolean) -> Unit) {

    Page {

        Text("Ce que l'application garde", style = MaterialTheme.typography.headlineSmall)

        Encadre("Le contenu du site est gardé sur ce téléphone : les violentomètres, les aides, les parcours. C'est ce qui permet de s'en servir sans réseau. Il ne dit rien de vous.")

        Text("Vos réponses quand vous faites le point, elles, parlent de vous, comme vos brouillons, vos « J'aime » et les récits que vous avez lus. Par défaut, rien n'en est gardé : en quittant l'application, ils disparaissent.", style = MaterialTheme.typography.bodyLarge)

        Text("Voulez-vous pouvoir reprendre vos réponses d'une fois à l'autre ?", style = MaterialTheme.typography.titleMedium)

        Button(onClick = { choisir(false) }, Modifier.fillMaxWidth()) { Text("Non, ne rien garder") }

        OutlinedButton(onClick = { choisir(true) }, Modifier.fillMaxWidth()) { Text("Oui, garder mes réponses sur ce téléphone") }

        Text("Gardées, elles sont une trace sur ce téléphone. Vous pourrez changer d'avis dans les réglages.", style = MaterialTheme.typography.bodySmall)
    }
}

/*
 * Le troisième écran du premier lancement : proposer le didacticiel,
 * sans l'imposer. On peut le refaire depuis le menu ou les réglages.
 */
@Composable
fun ProposerLeDidacticiel(choisir: (suivre: Boolean) -> Unit) {
    Page {
        Text("Un petit tour ?", style = MaterialTheme.typography.headlineSmall)
        Text("Quelques écrans pour montrer ce que fait l'application, et les gestes qui servent partout. Deux minutes.", style = MaterialTheme.typography.bodyLarge)
        Button(onClick = { choisir(true) }, Modifier.fillMaxWidth()) { Text("Suivre le didacticiel") }
        OutlinedButton(onClick = { choisir(false) }, Modifier.fillMaxWidth()) { Text("Plus tard") }
        Note("Vous le retrouverez dans le menu ☰ et dans les réglages.")
    }
}

// Une colonne qui défile, aux marges de téléphone.
@Composable
fun Page(contenu: @Composable () -> Unit) {
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { contenu() }
}
