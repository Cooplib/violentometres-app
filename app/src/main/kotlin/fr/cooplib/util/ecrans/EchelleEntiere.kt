package fr.cooplib.util.ecrans

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.cooplib.util.modeles.Violentometre

/*
 * L'échelle EN ENTIER, de haut en bas, qu'on fait défiler : l'autre façon
 * de la lire, à côté de « situation par situation » (retour du 8 octobre
 * 2026). On y voit d'un coup toute la progression.
 *
 * Chaque niveau s'ouvre sur un bandeau de sa couleur, et ses situations
 * sont des cases teintées, avec un filet de la même couleur. C'est le
 * seul endroit, avec le point, où ces couleurs s'étalent : c'est leur
 * sens, dire à quel niveau on est.
 */
@Composable
fun EchelleEntiere(vm: Violentometre, fermer: () -> Unit, faireLePoint: (() -> Unit)? = null) {

    BackHandler(onBack = fermer)

    val groupes = remember(vm) {
        vm.niveaux.sortedBy { it.position }
            .map { n -> n to vm.situations.filter { it.gravite == n.position }.sortedBy { it.position } }
            .filter { it.second.isNotEmpty() }
    }
    val fond = MaterialTheme.colorScheme.surface

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(vm.titre, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = fermer) { Text("Fermer") }
            }
        }

        item { Note("${vm.situations.size} situations, du plus léger au plus grave.") }

        for ((niveau, situations) in groupes) {

            // Le bandeau du niveau : sa couleur pleine, son nom, combien.
            item(key = "niveau-${niveau.position}") {
                Surface(Modifier.fillMaxWidth().padding(top = 10.dp), shape = Charte.ArrondiMoyen, color = couleur(niveau)) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(niveau.label, color = texteSurNiveau(niveau), fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                        Text("${situations.size}", color = texteSurNiveau(niveau), fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(situations, key = { it.id }) { s ->
                Surface(Modifier.fillMaxWidth(), shape = Charte.ArrondiMoyen, color = teinteSur(couleur(niveau), fond, 0.10f)) {
                    Row(Modifier.height(IntrinsicSize.Min)) {
                        Box(Modifier.width(5.dp).fillMaxHeight().background(couleur(niveau)))
                        Text(s.texte, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        if (faireLePoint != null) {
            item { Espace(8) }
            item { Button(onClick = faireLePoint, Modifier.fillMaxWidth()) { Text("🧭 Faire le point dessus") } }
        }
    }
}
