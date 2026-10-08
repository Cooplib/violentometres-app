package fr.cooplib.util.ecrans

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.cooplib.util.modeles.Niveau
import kotlinx.coroutines.launch

/*
 * Le didacticiel : ce que fait l'application, MONTRÉ plutôt qu'expliqué.
 * Proposé à la fin de la configuration, et à refaire depuis le menu ou
 * les réglages (retour du 8 octobre 2026).
 *
 * Une suite d'écrans qu'on fait glisser ; les points en bas disent
 * combien il y en a et y mènent directement ; « Quitter le didacticiel »
 * est toujours en haut. Les gestes (glisser partout, à gauche oui) sont
 * montrés par une carte qui bouge toute seule : c'est ce qu'on retient.
 *
 * Rien de réel n'y est affiché : des maquettes, pas le contenu du site.
 */
@Composable
fun Didacticiel(finir: () -> Unit) {

    BackHandler(onBack = finir)

    val ecrans: List<@Composable () -> Unit> = listOf(
        { Bienvenue() },
        { LaCalculatrice() },
        { GlisserPartout() },
        { LireUneEchelle() },
        { LesRecits() },
        { LesParcours() },
        { CeQuiResteIci() },
    )

    val etat = rememberPagerState { ecrans.size }
    val portee = rememberCoroutineScope()
    val dernier = etat.currentPage == ecrans.size - 1

    Column(Modifier.fillMaxSize()) {

        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = finir) { Text("Quitter le didacticiel") }
        }

        HorizontalPager(state = etat, modifier = Modifier.weight(1f)) { page ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) { ecrans[page]() }
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Points(ecrans.size, etat.currentPage, { portee.launch { etat.animateScrollToPage(it) } })
            Button(
                onClick = { if (dernier) finir() else portee.launch { etat.animateScrollToPage(etat.currentPage + 1) } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (dernier) "C'est parti" else "Suivant →") }
        }
    }
}

@Composable
private fun Titre(texte: String) =
    Text(texte, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)

@Composable
private fun Explication(texte: String) =
    Text(texte, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)

private val NIVEAUX = listOf(Niveau(0, "Positif", "green"), Niveau(1, "Vigilance", "yellow"), Niveau(2, "Attention", "orange"), Niveau(3, "Danger", "red"))

@Composable
private fun Bienvenue() {
    Marque(Modifier.size(width = 88.dp, height = 60.dp))
    Titre("Quatre entrées")
    Explication("De ce dont on a besoin maintenant à ce qui aide à comprendre.")
    for (e in Entree.entries) {
        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(e.icone, fontSize = 24.sp)
                Column {
                    Text(e.titre, style = MaterialTheme.typography.titleSmall)
                    Note(e.sousTitre)
                }
            }
        }
    }
}

@Composable
private fun LaCalculatrice() {
    Titre("Une calculatrice, devant")
    // La calculatrice garde ses propres couleurs : grises, neutres.
    Surface(shape = Charte.ArrondiGrand, color = Color(0xFF202124)) {
        Column(Modifier.padding(16.dp).width(220.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("2580", color = Color(0xFFF1F3F4), fontSize = 34.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (t in listOf("7", "8", "9", "=")) {
                    Surface(shape = RoundedCornerShape(20.dp), color = if (t == "=") Color(0xFF5F6368) else Color(0xFF3C4043)) {
                        Text(t, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = Color(0xFFF1F3F4), fontSize = 20.sp)
                    }
                }
            }
        }
    }
    Explication("Sur l'écran d'accueil, l'application est une calculatrice qui calcule pour de vrai. Votre code, puis « = », l'ouvre ; l'écran se vide aussitôt.")
    Encadre(alerte = true, titre = "✕ Quitter vite") {
        Text("En haut de chaque écran. Il efface vos réponses, remet la calculatrice et ferme, même si vous affichez le vrai nom.")
    }
}

// Une carte qui se balance d'un côté à l'autre : le geste, montré.
@Composable
private fun CarteQuiSeBalance(texte: String, couleurDeBordure: Color? = null) {
    val transition = rememberInfiniteTransition(label = "balance")
    val x by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "x")
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Card(
            Modifier.width(240.dp).offset(x = (x * 36).dp).graphicsLayer { rotationZ = x * 6f },
            couleurDeBordure = couleurDeBordure,
        ) {
            Box(Modifier.fillMaxWidth()) {
                Text(texte, Modifier.padding(horizontal = 18.dp, vertical = 26.dp), style = MaterialTheme.typography.titleMedium)
                Text(
                    if (x < 0) "Oui" else "Non",
                    Modifier.align(if (x < 0) Alignment.TopEnd else Alignment.TopStart).padding(8.dp).graphicsLayer { alpha = kotlin.math.abs(x) },
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun GlisserPartout() {
    Titre("Glisser, n'importe où")
    CarteQuiSeBalance("Une situation, ici.", couleur(NIVEAUX[1]))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("← Oui", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Non →", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
    Explication("Quand vous faites le point, glissez n'importe où sur l'écran : à gauche pour oui, à droite pour non. Les boutons restent en bas, toujours au même endroit.")
    Note("On glisse aussi d'un récit à l'autre, d'une étape de parcours à l'autre, d'une situation à l'autre dans une échelle, et entre les écrans du bilan.")
}

@Composable
private fun LireUneEchelle() {
    Titre("Lire une échelle")
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (n in NIVEAUX) {
            Surface(Modifier.fillMaxWidth(), shape = Charte.ArrondiMoyen, color = couleur(n)) {
                Text(n.label, Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = texteSurNiveau(n), fontWeight = FontWeight.Bold)
            }
        }
    }
    Explication("Un violentomètre met des situations bout à bout, du geste qui va de soi à celui qui met en danger. Chaque couleur est un niveau, et ne sert qu'à ça.")
    Note("Lisez-le situation par situation, en glissant, ou en entier, de haut en bas. Ou faites le point dessus.")
}

@Composable
private fun LesRecits() {
    Titre("Des récits")
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Un titre de récit", style = MaterialTheme.typography.titleMedium)
            Encadre("Ce récit peut être difficile à lire.")
            Button(onClick = {}) { Text("Lire le récit") }
        }
    }
    Explication("Certains récits sont cachés jusqu'à ce que vous choisissiez de les lire. Une fois lus, ils restent visibles ; vous pouvez les recacher pour vous.")
    Note("Vous pouvez déposer le vôtre. Il sera public : ne nommez jamais une personne réelle. Et chaque récit peut être signalé.")
}

@Composable
private fun LesParcours() {
    Titre("Comprendre")
    val transition = rememberInfiniteTransition(label = "etapes")
    val courant by transition.animateFloat(0f, 3.99f, infiniteRepeatable(tween(4000), RepeatMode.Restart), label = "courant")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until 4) {
            Box(
                Modifier.size(if (i == courant.toInt()) 18.dp else 12.dp)
                    .background(if (i <= courant.toInt()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(999.dp))
            )
        }
    }
    Explication("Des parcours courts : une carte par étape, qu'on fait glisser. Certains lisent la même chose sous plusieurs angles.")
    Note("D'une étape, on peut lire l'échelle ou faire le point dessus ; le retour ramène à l'étape.")
}

@Composable
private fun CeQuiResteIci() {
    Titre("Ce qui reste ici")
    Encadre(titre = "Vos réponses ne sortent pas du téléphone") {
        Text("Rien de ce que vous cochez n'est envoyé. Par défaut, rien n'est gardé non plus : en quittant, tout disparaît.")
    }
    Encadre(titre = "♥ J'aime") {
        Text("Aimer un violentomètre ou un parcours, comme sur le site : ça, c'est envoyé au site.")
    }
    Explication("Le menu ☰, en haut, mène aux réglages, à la mise à jour du contenu, et à ce qui est protégé ou ne l'est pas.")
}
