package fr.cooplib.util.ecrans

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text

/*
 * « Ce qui est protégé, ce qui ne l'est pas » : l'équivalent de /traces
 * sur le site, et ce qui distingue une vraie précaution d'une fausse
 * assurance.
 *
 * Seulement ce qui concerne l'application INSTALLÉE. La page du site
 * fait foi pour le reste : deux textes proches divergent au premier
 * correctif qu'on n'applique qu'à l'un (APPLICATION-ANDROID.md,
 * chapitre 7).
 *
 * Elle dit surtout ce qu'on ne peut PAS faire, avec ce qu'on peut y
 * faire soi-même quand il y a quelque chose à faire.
 */
@Composable
fun CeQuiEstProtege(deguise: Boolean) {

    Page {

        Text("Ce qui est protégé, ce qui ne l'est pas", style = MaterialTheme.typography.headlineSmall)

        Encadre(titre = "Ce que fait l'application") {
            if (deguise) {
                Text("Sur l'écran d'accueil, elle s'appelle Calculatrice, et elle calcule pour de vrai. Quand vous la quittez, elle redevient une calculatrice : il faut retaper le code.")
            } else {
                Text("Vous avez choisi d'afficher son vrai nom. « Quitter vite » la remet en calculatrice, avec votre code de secours.")
            }
            Text("La vignette des applications récentes reste vide, et les captures d'écran sont refusées.")
            Text("Elle n'envoie aucune notification.")
            Text("Rien n'est copié dans la sauvegarde de votre compte Google, ni vers un nouveau téléphone.")
            Text("Par défaut, vos réponses au point ne sont pas gardées.")
        }

        Encadre(alerte = true, titre = "Ce qu'elle ne peut pas protéger") {
            Text("Quelqu'un qui cherche. Dans les réglages du téléphone, la liste des applications montre une Calculatrice de plus, avec la place qu'elle prend et les données qu'elle consomme. Une deuxième calculatrice peut étonner.")
            Text("Votre compte Google. Installée depuis le Play Store, l'application apparaît sous son vrai nom dans la bibliothèque du compte. Installée depuis le site ou depuis F-Droid, elle n'y apparaît pas.")
            Text("Le réseau. Quand elle se met à jour, ou quand vous aimez un violentomètre, une box ou un réseau surveillé peut voir à quel site elle parle. Elle se met à jour rarement : tous les trois jours au plus, ou quand vous le demandez.")
            Text("Vos « J'aime ». Ils partent au site, comme sur l'original : le site sait qu'un même téléphone a aimé ceci et cela, sans savoir à qui il est.")
            Text("Les appels. Un numéro appelé depuis l'application reste dans le journal d'appels du téléphone, comme tout appel. Effacez-le si besoin.")
            Text("Le code. Quatre chiffres se retrouvent en essayant. Il arrête un regard, pas quelqu'un qui s'acharne.")
            Text("La contrainte. Si quelqu'un vous oblige à ouvrir l'application, le code n'y peut rien.")
            Text("Un logiciel espion. S'il y en a un sur le téléphone, il voit ce qui s'affiche à l'écran. Si vous pensez être surveillé·e, utilisez un autre téléphone, celui de quelqu'un de confiance.")
        }

        Text("Si vous oubliez votre code", style = MaterialTheme.typography.titleMedium)
        Text("Désinstallez puis réinstallez l'application. Vous ne perdez rien : par défaut, elle ne garde rien de vous.")

        Text("En cas de danger immédiat : 17, ou 114 par SMS si vous ne pouvez pas parler. Violences Femmes Info : 3919.", style = MaterialTheme.typography.titleSmall)
    }
}
