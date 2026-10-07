package fr.cooplib.util.ecrans

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.cooplib.util.donnees.Catalogue
import fr.cooplib.util.modeles.Etape
import fr.cooplib.util.modeles.Parcours
import fr.cooplib.util.modeles.Violentometre

/*
 * « Comprendre » : les parcours, UNE CARTE PAR ÉTAPE.
 *
 * Écartés d'abord comme « trente minutes à travers plusieurs pages ».
 * C'était faux, les données le disent : médiane de 4 étapes, notes de
 * 33 signes. Un parcours est déjà écrit à la taille d'un téléphone. Et
 * c'est ce qui fait prendre du recul : « sous plusieurs angles », le
 * même couple lu par le contrôle coercitif, puis le féminisme
 * matérialiste, puis les critiques des normes de genre. Les retirer,
 * c'était garder le constat et jeter la politisation
 * (APPLICATION-ANDROID.md, chapitre 2).
 *
 * Une étape de cadre ou de mécanisme s'affiche par son résumé, avec
 * « en savoir plus » pour le reste : inutile de poser la page entière
 * sur un écran de téléphone. Une étape de violentomètre montre, à la
 * demande, son échelle, et mène au point.
 */
@Composable
fun Comprendre(catalogue: Catalogue, faireLePoint: (String) -> Unit) {

    var ouvert by rememberSaveable { mutableStateOf<String?>(null) }

    BackHandler(enabled = ouvert != null) { ouvert = null }

    val parcours = catalogue.parcours.find { it.id == ouvert }

    if (parcours == null) {
        Liste(catalogue.parcours) { ouvert = it }
    } else {
        androidx.compose.runtime.key(parcours.id) {
            Cartes(parcours, catalogue, faireLePoint)
        }
    }
}

@Composable
private fun Liste(parcours: List<Parcours>, ouvrir: (String) -> Unit) {

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        item { Text("Comprendre", style = MaterialTheme.typography.headlineSmall) }
        item { Text("Des parcours courts, une carte par étape : d'une situation vécue à ce qui la produit.") }

        items(parcours.sortedBy { it.titre }, key = { it.id }) { p ->
            Card(onClick = { ouvrir(p.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(p.titre, style = MaterialTheme.typography.titleMedium)
                    if (p.description.isNotBlank()) Text(p.description, style = MaterialTheme.typography.bodyMedium)
                    Text("${p.etapes.size} étape${if (p.etapes.size > 1) "s" else ""}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun Cartes(parcours: Parcours, catalogue: Catalogue, faireLePoint: (String) -> Unit) {

    val etapes = parcours.etapes.sortedBy { it.position }

    // -1 : l'ouverture du parcours ; etapes.size : sa fin.
    var i by rememberSaveable { mutableIntStateOf(-1) }

    Page {

        Text(parcours.titre, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

        if (etapes.isNotEmpty()) {
            LinearProgressIndicator(
                progress = { ((i + 1).coerceIn(0, etapes.size)).toFloat() / etapes.size },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        when {
            i < 0 -> {
                Text(parcours.titre, style = MaterialTheme.typography.headlineSmall)
                if (parcours.description.isNotBlank()) Text(parcours.description)
                if (parcours.apropos.isNotBlank()) Text(parcours.apropos, style = MaterialTheme.typography.bodyMedium)
                Text("${etapes.size} étape${if (etapes.size > 1) "s" else ""}.", style = MaterialTheme.typography.bodySmall)
                Button(onClick = { i = 0 }, Modifier.fillMaxWidth(), enabled = etapes.isNotEmpty()) { Text("Commencer") }
            }

            i >= etapes.size -> {
                Text("Fin du parcours", style = MaterialTheme.typography.headlineSmall)
                if (parcours.conclusion.isNotBlank()) Text(parcours.conclusion)
                OutlinedButton(onClick = { i = 0 }, Modifier.fillMaxWidth()) { Text("Revoir depuis le début") }
            }

            else -> Carte(etapes[i], i, etapes.size, catalogue, faireLePoint)
        }

        if (i >= 0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { i -= 1 }) { Text("← Précédente") }
                if (i < etapes.size) {
                    Button(onClick = { i += 1 }) { Text(if (i == etapes.size - 1) "Terminer" else "Suivante →") }
                }
            }
        }
    }
}

@Composable
private fun Carte(etape: Etape, i: Int, n: Int, catalogue: Catalogue, faireLePoint: (String) -> Unit) {

    // Remis à zéro d'une carte à l'autre.
    var plus by rememberSaveable(etape.id) { mutableStateOf(false) }

    Text("Étape ${i + 1} sur $n", style = MaterialTheme.typography.bodySmall)

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            if (etape.note.isNotBlank()) Text(etape.note, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

            Text(etape.cible?.titre ?: "", style = MaterialTheme.typography.titleLarge)

            etape.cible?.resume?.takeIf { it.isNotBlank() }?.let { Text(it) }
        }
    }

    when (etape.type) {

        "violentometer" -> {
            val vm = catalogue.violentometres.find { it.id == etape.violentometreId }
            if (vm != null) {
                OutlinedButton(onClick = { plus = !plus }, Modifier.fillMaxWidth()) { Text(if (plus) "Masquer l'échelle" else "Voir l'échelle") }
                if (plus) Echelle(vm)
                if (vm.id in catalogue.pointsParViolentometre) {
                    Button(onClick = { faireLePoint(vm.id) }, Modifier.fillMaxWidth()) { Text("Faire le point dessus") }
                }
            }
        }

        "cadre" -> catalogue.cadres.find { it.id == etape.cadreId }?.let { cadre ->
            val texte = listOf(cadre.description, cadre.apropos).filter { it.isNotBlank() }
            if (texte.isNotEmpty() || cadre.references.isNotEmpty()) {
                TextButton(onClick = { plus = !plus }) { Text(if (plus) "Moins" else "En savoir plus") }
                if (plus) {
                    texte.forEach { Text(it) }
                    if (cadre.references.isNotEmpty()) {
                        Text("Pour aller plus loin", style = MaterialTheme.typography.titleSmall)
                        cadre.references.forEach { Text("· $it", style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
        }

        "mecanisme" -> catalogue.mecanismes.find { it.id == etape.mecanismeId }?.let { m ->
            val texte = listOf(m.description, m.apropos).filter { it.isNotBlank() }
            if (texte.isNotEmpty()) {
                TextButton(onClick = { plus = !plus }) { Text(if (plus) "Moins" else "En savoir plus") }
                if (plus) texte.forEach { Text(it) }
            }
        }
    }
}

// L'échelle d'un violentomètre : ses situations, du plus léger au plus
// grave, chaque niveau à sa couleur.
@Composable
private fun Echelle(vm: Violentometre) {
    for (niveau in vm.niveaux.sortedBy { it.position }) {
        val situations = vm.situations.filter { it.gravite == niveau.position }.sortedBy { it.position }
        if (situations.isEmpty()) continue
        EtiquetteNiveau(niveau)
        situations.forEach { Text("· ${it.texte}", style = MaterialTheme.typography.bodyMedium) }
    }
}
