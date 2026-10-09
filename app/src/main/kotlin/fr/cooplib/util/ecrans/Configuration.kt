package fr.cooplib.util.ecrans

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import fr.cooplib.util.leurre.CodeSecret

/*
 * Le premier lancement (APPLICATION-ANDROID.md, chapitre 3) : un réglage
 * par écran, les points en bas pour voir où l'on en est.
 *
 * C'était d'abord trois pages, dont une très longue (le nom, le code, ce
 * qui est protégé, tout ensemble) : au premier essai, Cooplib l'a trouvée
 * lourde (9 octobre 2026). Un écran, une question.
 *
 * Les limites du déguisement ont LEUR écran, juste après le choix du nom
 * et avant le code. Promettre une protection qu'on ne tient pas mettrait
 * quelqu'un en danger en lui donnant une confiance qu'il n'a pas lieu
 * d'avoir.
 *
 * Rien n'est appliqué avant le dernier écran : revenir en arrière ne
 * bascule pas l'icône du lanceur d'un nom à l'autre, et une configuration
 * abandonnée en route laisse l'application telle qu'à l'installation
 * (la calculatrice).
 *
 * Le ton est celui de la page « Ne pas laisser de traces » du site :
 * des phrases courtes, des consignes, pas d'alarme. Quelqu'un qui lit
 * ceci a déjà peur.
 */

private enum class Etape { BIENVENUE, NOM, PROTECTION, CODE, GARDER, CAPTURES, APPARENCE, PRET }

@Composable
fun Configuration(
    theme: String,
    changerTheme: (String) -> Unit,
    voirCeQuiEstProtege: () -> Unit,
    terminer: (vraiNom: Boolean, empreinteDuCode: String, garderLesReponses: Boolean, capturesPermises: Boolean, didacticiel: Boolean) -> Unit,
) {

    var etape by rememberSaveable { mutableStateOf(Etape.BIENVENUE) }
    var vraiNom by rememberSaveable { mutableStateOf(false) }
    var empreinte by rememberSaveable { mutableStateOf<String?>(null) }
    var garder by rememberSaveable { mutableStateOf(false) }
    var captures by rememberSaveable { mutableStateOf(false) }

    val etapes = Etape.entries
    fun suivante() { etape = etapes[etape.ordinal + 1] }
    fun precedente() { etape = etapes[etape.ordinal - 1] }

    BackHandler(enabled = etape != Etape.BIENVENUE) { precedente() }

    Column(Modifier.fillMaxSize()) {

        Box(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp)) {
            if (etape != Etape.BIENVENUE) TextButton(onClick = ::precedente) { Text("‹ Retour") }
        }

        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            when (etape) {

                Etape.BIENVENUE -> {
                    Text("Bonjour", style = MaterialTheme.typography.headlineMedium)
                    Text("Comprendre les violences, faire le point sur ce qu'on vit, trouver de l'aide.", style = MaterialTheme.typography.bodyLarge)
                    Text("D'abord, quelques réglages pour protéger votre usage. Une minute.", style = MaterialTheme.typography.bodyLarge)
                }

                Etape.NOM -> {
                    Text("Sur l'écran d'accueil", style = MaterialTheme.typography.headlineSmall)
                    Text("Comment l'application apparaît sur votre téléphone ?", style = MaterialTheme.typography.bodyLarge)
                    Option("🧮 Calculatrice", "Elle en a l'icône, et elle calcule pour de vrai. Pour l'ouvrir : votre code, puis =.", choisie = !vraiNom) { vraiNom = false }
                    Option("Violentomètres", "Son vrai nom, visible de qui regarde votre téléphone.", choisie = vraiNom) { vraiNom = true }
                }

                Etape.PROTECTION -> {
                    Text("Ce que ça protège, et ce que ça ne protège pas", style = MaterialTheme.typography.headlineSmall)
                    Encadre(titre = "Protégé") {
                        Text("Quelqu'un qui prend votre téléphone et regarde l'écran d'accueil.")
                    }
                    Encadre(alerte = true, titre = "Pas protégé") {
                        Text("Quelqu'un qui cherche : dans les réglages du téléphone, dans votre compte Google. Quelqu'un qui vous oblige à ouvrir l'application. Un logiciel espion.")
                    }
                    TextButton(onClick = voirCeQuiEstProtege) { Text("En savoir plus") }
                }

                /*
                 * Un code pour tout le monde, décidé le 7 octobre 2026 : avec
                 * le vrai nom, c'est le code DE SECOURS. « Quitter vite » remet
                 * alors la calculatrice, parce que si l'on a voulu quitter
                 * vite, c'est qu'il y a sans doute une galère.
                 */
                Etape.CODE -> {
                    Text(if (vraiNom) "Un code de secours" else "Votre code", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        if (vraiNom) "Si vous touchez « Quitter vite », l'application redevient une calculatrice. Il faudra ce code pour la rouvrir."
                        else "Tapé dans la calculatrice, puis =, il ouvre l'application. L'écran se vide aussitôt.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    ChoixDuCode(valider = { empreinte = it; suivante() })
                }

                Etape.GARDER -> {
                    Text("Ce que l'application garde", style = MaterialTheme.typography.headlineSmall)
                    Text("Vos réponses quand vous faites le point parlent de vous, comme vos brouillons et vos « J'aime ». Les garder, c'est pouvoir les reprendre, mais c'est une trace sur ce téléphone.", style = MaterialTheme.typography.bodyLarge)
                    Option("Ne rien garder", "Tout disparaît quand vous quittez l'application.", choisie = !garder) { garder = false }
                    Option("Garder sur ce téléphone", "« Quitter vite » les efface quand même.", choisie = garder) { garder = true }
                    Note("Le contenu du site, lui, est toujours gardé : c'est ce qui permet de s'en servir sans réseau. Il ne dit rien de vous. Tout se change dans les réglages.")
                }

                Etape.CAPTURES -> {
                    Text("Les captures d'écran", style = MaterialTheme.typography.headlineSmall)
                    Option("Les refuser", "Conseillé. Ce qu'affiche l'application ne peut être ni capturé, ni enregistré, ni partagé en vidéo. Ça gêne aussi certains logiciels espions, sans les arrêter tous.", choisie = !captures) { captures = false }
                    Option("Les permettre", "Pour garder une image d'un résultat, ou la montrer à quelqu'un.", choisie = captures) { captures = true }
                    if (captures) RisquesDesCaptures()
                }

                Etape.APPARENCE -> {
                    Text("L'apparence", style = MaterialTheme.typography.headlineSmall)
                    for ((cle, libelle, detail) in listOf(
                        Triple("auto", "Comme le téléphone", "Clair ou sombre, selon votre réglage."),
                        Triple("clair", "Clair", "Fond blanc."),
                        Triple("sombre", "Sombre", "Fond noir, plus discret le soir."),
                    )) Option(libelle, detail, choisie = theme == cle) { changerTheme(cle) }
                }

                Etape.PRET -> {
                    Text("C'est prêt", style = MaterialTheme.typography.headlineMedium)
                    if (!vraiNom) {
                        Encadre("Désormais, l'application s'appelle Calculatrice. Pour l'ouvrir : votre code, puis =.")
                    }
                    Text("Un petit tour montre ce que fait l'application, et les gestes qui servent partout. Deux minutes.", style = MaterialTheme.typography.bodyLarge)
                    Note("Vous le retrouverez dans le menu ☰ et dans les réglages.")
                }
            }
        }

        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (etape) {
                // Le code a son propre bouton : il faut d'abord qu'il soit bon.
                Etape.CODE -> {}
                Etape.PRET -> {
                    val e = empreinte!!
                    Button(onClick = { terminer(vraiNom, e, garder, captures, true) }, Modifier.fillMaxWidth()) { Text("Faire le petit tour") }
                    OutlinedButton(onClick = { terminer(vraiNom, e, garder, captures, false) }, Modifier.fillMaxWidth()) { Text("Commencer") }
                }
                else -> Button(onClick = ::suivante, Modifier.fillMaxWidth()) { Text(if (etape == Etape.BIENVENUE) "Commencer" else "Suivant") }
            }
            // Les points mènent en arrière seulement : en avant, il y a
            // peut-être un code à poser.
            Points(etapes.size, etape.ordinal, aller = { i -> if (i < etape.ordinal) etape = etapes[i] })
        }
    }
}

/*
 * Ce que coûte une capture permise, dit au même endroit que le choix :
 * au premier lancement et dans les réglages. Le plus sérieux d'abord.
 */
@Composable
fun RisquesDesCaptures() {
    Encadre(alerte = true, titre = "Ce que ça risque") {
        Text("Une capture va dans la galerie du téléphone. Souvent, la galerie est copiée dans un compte en ligne (Google Photos, par exemple) : la capture y reste, même effacée du téléphone.")
        Text("Quelqu'un qui regarde vos photos la verra, avec ce qu'elle montre.")
        Text("Un logiciel espion qui filme l'écran n'en sera plus empêché.")
        if (android.os.Build.VERSION.SDK_INT < 33) {
            Text("Sur ce téléphone, la vignette des applications récentes montrera aussi le dernier écran ouvert.")
        }
        Text("« Quitter vite » les refuse de nouveau.")
    }
}

// Un choix parmi plusieurs : bordé de violet quand il est pris.
@Composable
private fun Option(titre: String, detail: String, choisie: Boolean, choisir: () -> Unit) {
    Card(Modifier.fillMaxWidth(), onClick = choisir, couleurDeBordure = if (choisie) MaterialTheme.colorScheme.primary else null) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RadioButton(selected = choisie, onClick = choisir)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(titre, style = MaterialTheme.typography.titleMedium)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ChoixDuCode(valider: (empreinteDuCode: String) -> Unit, annuler: (() -> Unit)? = null) {

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

        annuler?.let { TextButton(onClick = it) { Text("Revenir") } }
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
