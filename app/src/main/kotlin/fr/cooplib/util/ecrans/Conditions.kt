package fr.cooplib.util.ecrans

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/*
 * Les conditions d'utilisation, dans l'application : la même chose que
 * la page /conditions du site (src/pages/Conditions.jsx, qui fait foi),
 * raccourcie pour un téléphone. Modifier l'une, c'est modifier l'autre.
 *
 * CC BY-NC-SA 4.0 pour tout le monde ; une autorisation en plus pour les
 * formatrices et formateurs (CC+) ; et ce qu'on accepte en publiant, dont
 * le droit réservé à Cooplib d'en faire des supports, même vendus.
 */
@Composable
fun ConditionsDUtilisation() {

    val ouvrirUnSite = LocalOuvrirUnSite.current

    Page {

        Text("Conditions d'utilisation", style = MaterialTheme.typography.headlineSmall)
        Text("Ce que vous pouvez faire du contenu, et ce que vous acceptez en publiant.", style = MaterialTheme.typography.bodyLarge)

        Encadre(titre = "La licence du contenu") {
            Text("Sauf mention contraire, le contenu (textes, images, supports pédagogiques) est sous licence Creative Commons Attribution – Pas d'Utilisation Commerciale – Partage dans les Mêmes Conditions 4.0 International (CC BY-NC-SA 4.0).")
            Text("Vous pouvez le copier, le partager, l'adapter (le traduire, le transformer, créer à partir de lui), à condition :")
            Text("· de citer l'auteur et la source, avec un lien vers la licence, et de dire si vous l'avez modifié ;")
            Text("· de ne pas l'utiliser à des fins commerciales ;")
            Text("· de diffuser vos propres versions sous la même licence.")
            Note("Pour citer : « Violentomètres, violentometres.fr ».")
        }

        Encadre(titre = "Pour les formatrices et formateurs (CC+)") {
            Text("En plus de la licence, vous pouvez utiliser ce contenu sans le modifier comme support de formation, y compris dans des formations rémunérées, en citant clairement la source.")
            Text("Cela ne permet pas de le modifier pour l'intégrer à un support sous une licence plus restrictive, ni de le revendre tel quel (dans un manuel vendu, par exemple) sans accord écrit.")
            Note("Cette autorisation complète la licence Creative Commons : elle ne réduit aucun des droits qu'elle accorde.")
        }

        Encadre(alerte = true, titre = "Ce que vous publiez") {
            Text("En publiant un récit, vous acceptez qu'il soit diffusé sous cette même licence, avec l'autorisation pour les formatrices et formateurs.")
            Text("Vous accordez aussi à Cooplib, éditrice du site, gratuitement et sans exclusivité, partout et pour toute la durée des droits d'auteur, le droit de le reproduire, de le représenter et de l'adapter, y compris dans des supports qu'elle édite et vend : carnets, livrets, supports de formation. Ce droit n'est qu'à Cooplib.")
            Text("Ce que vous publiez est signé d'un pseudonyme, jamais de votre nom. Ne publiez que vos propres mots, et ne nommez jamais une personne réelle.")
        }

        Note("Pour un accord particulier, ou une question : equipe@cooplib.fr.")
        TextButton(onClick = { ouvrirUnSite("$SITE/conditions") }) { Text("La page complète, sur le site ↗") }
    }
}
