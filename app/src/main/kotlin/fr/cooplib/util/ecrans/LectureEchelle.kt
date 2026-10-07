package fr.cooplib.util.ecrans

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.cooplib.util.modeles.Violentometre

/*
 * Le mode lecture d'une échelle : UNE SITUATION PAR CARTE, et on glisse
 * de l'une à l'autre, du plus léger au plus grave.
 *
 * La liste d'avant, tout d'un bloc, ne donnait pas envie de lire
 * (retour du 7 octobre 2026). Une carte à la fois laisse à chaque phrase
 * le temps d'être lue, et la progression, qui est tout le propos d'un
 * violentomètre, se SENT : la page prend la couleur du niveau en cours,
 * et elle change en passant de l'un à l'autre. C'est le seul usage de
 * ces couleurs ici, et c'est le leur.
 */
@Composable
fun LectureEchelle(vm: Violentometre, fermer: () -> Unit, faireLePoint: (() -> Unit)? = null) {

    BackHandler(onBack = fermer)

    val niveaux = remember(vm) { vm.niveaux.sortedBy { it.position }.associateBy { it.position } }
    val situations = remember(vm) { vm.situations.sortedWith(compareBy({ it.gravite }, { it.position })) }

    // Une page de plus à la fin : où l'on en est, et la suite.
    val etat = rememberPagerState { situations.size + 1 }

    val courante = situations.getOrNull(etat.currentPage)
    val niveau = courante?.let { niveaux[it.gravite] }
    val teinte by animateColorAsState(niveau?.let { couleur(it).copy(alpha = 0.12f) } ?: MaterialTheme.colorScheme.background, label = "teinte")

    Column(Modifier.fillMaxSize().background(teinte)) {

        // En haut : le titre, la position, et une barre de l'échelle entière
        // où l'on voit où l'on est.
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(vm.titre, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 1)
                TextButton(onClick = fermer) { Text("Fermer") }
            }
            Row(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp))) {
                situations.forEachIndexed { i, s ->
                    val n = niveaux[s.gravite]
                    Box(
                        Modifier.weight(1f).height(6.dp).background(
                            if (n == null) MaterialTheme.colorScheme.outline
                            else couleur(n).copy(alpha = if (i <= etat.currentPage) 1f else 0.25f)
                        )
                    )
                }
            }
            Note(if (courante != null) "${etat.currentPage + 1} sur ${situations.size} · glissez pour la suivante" else "Fin de l'échelle")
        }

        HorizontalPager(state = etat, modifier = Modifier.weight(1f)) { page ->

            val s = situations.getOrNull(page)

            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {

                if (s == null) {
                    // La fin : ce qu'on peut en faire.
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Vous avez lu toute l'échelle.", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                        Note("${situations.size} situations, du plus léger au plus grave.")
                        faireLePoint?.let { Button(onClick = it) { Text("🧭 Faire le point dessus") } }
                        OutlinedButton(onClick = fermer) { Text("Revenir") }
                    }
                } else {
                    val n = niveaux[s.gravite]
                    Card(Modifier.fillMaxWidth(), couleurDeBordure = n?.let { couleur(it) }) {
                        Column(
                            Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            n?.let { EtiquetteNiveau(it) }
                            Text(s.texte, fontSize = 23.sp, lineHeight = 31.sp, fontWeight = FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}
