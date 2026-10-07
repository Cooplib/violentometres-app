package fr.cooplib.util.ecrans

import android.icu.text.Collator
import android.icu.util.ULocale
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.cooplib.util.donnees.Catalogue
import fr.cooplib.util.modeles.Proche
import fr.cooplib.util.modeles.Violentometre
import fr.cooplib.util.reseau.Resultat
import fr.cooplib.util.stockage.Depot
import kotlinx.coroutines.launch

/*
 * Les violentomètres : la liste, avec recherche, filtre par contexte et
 * tri comme sur le site ; puis la fiche d'un violentomètre, qui propose
 * comme un parcours de FAIRE LE POINT dessus ou de DÉROULER L'ÉCHELLE,
 * et ce qui lui ressemble.
 */

@Suppress("UNCHECKED_CAST")
internal val ordreFrancais = Collator.getInstance(ULocale.FRENCH) as Comparator<String>

private val TRIS = listOf("Les plus vus", "Les plus aimés", "Les plus complets", "De A à Z")

// Comparer sans les accents ni la casse : « violence » trouve « Violences ».
internal fun normal(s: String) =
    java.text.Normalizer.normalize(s.lowercase(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")

internal fun correspond(cherche: String, vararg champs: String): Boolean {
    val mots = normal(cherche).split(Regex("\\s+")).filter { it.isNotEmpty() }
    val texte = normal(champs.joinToString(" "))
    return mots.all { it in texte }
}

@Composable
internal fun ListeDesViolentometres(
    catalogue: Catalogue,
    depot: Depot,
    avant: @Composable () -> Unit = {},
    ouvrir: (String) -> Unit,
) {

    val souvenirs by depot.memoire.etat.collectAsState()

    var cherche by rememberSaveable { mutableStateOf("") }
    var contexte by rememberSaveable { mutableStateOf<String?>(null) }
    var tri by rememberSaveable { mutableStateOf(TRIS[0]) }

    val contextes = remember(catalogue) {
        catalogue.violentometres.flatMap { vm -> vm.contextes.map { it.nom } }
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key }
    }

    val liste = remember(catalogue, cherche, contexte, tri, souvenirs) {
        catalogue.violentometres
            .filter { vm -> contexte == null || vm.contextes.any { it.nom == contexte } }
            .filter { vm -> cherche.isBlank() || correspond(cherche, vm.titre, vm.description, vm.contextes.joinToString(" ") { it.nom }) }
            .let { l ->
                when (tri) {
                    "Les plus aimés" -> l.sortedByDescending { souvenirs.comptes[it.id] ?: it.aime }
                    "Les plus complets" -> l.sortedByDescending { it.situations.size }
                    "De A à Z" -> l.sortedWith(compareBy(ordreFrancais) { it.titre })
                    else -> l.sortedByDescending { it.vues }
                }
            }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        item { avant() }

        item {
            BarreDeRecherche(
                cherche, { cherche = it },
                filtres = contextes, filtre = contexte, filtrer = { contexte = it },
                tris = TRIS, tri = tri, trier = { tri = it },
                indication = "Chercher un violentomètre : un mot, un lieu de vie",
            )
        }

        item { Note("${liste.size} violentomètre${if (liste.size > 1) "s" else ""}") }

        items(liste, key = { it.id }) { vm ->
            CarteViolentometre(vm, aime = vm.id in souvenirs.aimes, likes = souvenirs.comptes[vm.id] ?: vm.aime) { ouvrir(vm.id) }
        }
    }
}

@Composable
internal fun CarteViolentometre(vm: Violentometre, aime: Boolean, likes: Int, raison: String? = null, ouvrir: () -> Unit) {
    Card(Modifier.fillMaxWidth(), onClick = ouvrir) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(vm.titre, style = MaterialTheme.typography.titleMedium)
            if (vm.description.isNotBlank()) {
                Text(vm.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Repartition(vm)
            Etiquettes(vm.contextes.map { it.nom })
            raison?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            Compteurs(
                "${vm.situations.size} situations",
                "👁 ${vm.vues}",
                (if (aime) "♥ " else "♡ ") + likes,
            )
        }
    }
}

/*
 * La forme d'un violentomètre d'un coup d'œil : une barre partagée entre
 * ses niveaux, chacun à proportion de ses situations. Les couleurs de
 * l'échelle, à leur place.
 */
@Composable
private fun Repartition(vm: Violentometre) {
    val parNiveau = vm.niveaux.sortedBy { it.position }.map { n -> n to vm.situations.count { it.gravite == n.position } }.filter { it.second > 0 }
    if (parNiveau.isEmpty()) return
    Row(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp))) {
        for ((n, combien) in parNiveau) {
            androidx.compose.foundation.layout.Box(Modifier.weight(combien.toFloat()).height(6.dp).background(couleur(n)))
        }
    }
}

/*
 * La fiche d'un violentomètre. Deux façons de s'en servir, comme une
 * étape de parcours : faire le point dessus, ou dérouler l'échelle pour
 * la lire en entier.
 */
@Composable
internal fun FicheViolentometre(
    vm: Violentometre,
    catalogue: Catalogue,
    depot: Depot,
    faireLePoint: (String) -> Unit,
    ouvrir: (String) -> Unit,
) {

    val souvenirs by depot.memoire.etat.collectAsState()
    var deroulee by rememberSaveable(vm.id) { mutableStateOf(false) }
    var plus by rememberSaveable(vm.id) { mutableStateOf(false) }
    var erreur by remember(vm.id) { mutableStateOf<String?>(null) }
    val portee = rememberCoroutineScope()
    val aime = vm.id in souvenirs.aimes

    // L'échelle se lit en mode lecture, une situation par carte.
    if (deroulee) {
        LectureEchelle(vm, fermer = { deroulee = false }, faireLePoint = if (vm.id in catalogue.pointsParViolentometre) ({ faireLePoint(vm.id) }) else null)
        return
    }

    Page {

        Text(vm.titre, style = MaterialTheme.typography.headlineSmall)
        if (vm.description.isNotBlank()) Text(vm.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Etiquettes(vm.contextes.map { it.nom })

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BoutonJAime(aime, souvenirs.comptes[vm.id] ?: vm.aime) {
                portee.launch {
                    erreur = when (depot.aimer("violentometer", vm.id, !aime)) {
                        is Resultat.Ok -> null
                        else -> "Pas de réseau : le « J'aime » n'est pas parti."
                    }
                }
            }
            Compteurs("${vm.situations.size} situations", "👁 ${vm.vues}")
        }
        erreur?.let { Note(it) }

        Button(onClick = { faireLePoint(vm.id) }, Modifier.fillMaxWidth(), enabled = vm.id in catalogue.pointsParViolentometre) {
            Text("🧭 Faire le point dessus")
        }
        OutlinedButton(onClick = { deroulee = true }, Modifier.fillMaxWidth()) {
            Text("📊 Lire l'échelle, situation par situation")
        }

        val textes = listOf(vm.apropos, vm.analyse).filter { it.isNotBlank() }
        if (textes.isNotEmpty()) {
            TextButton(onClick = { plus = !plus }) { Text(if (plus) "Moins" else "À propos de ce violentomètre") }
            if (plus) textes.forEach { Encadre(it) }
        }

        // Ce qui lui ressemble, calculé par le serveur, avec la raison.
        val proches = catalogue.proches[vm.id]?.violentometres.orEmpty()
            .mapNotNull { p -> catalogue.violentometres.find { it.id == p.violentometre.id }?.let { it to p } }
        if (proches.isNotEmpty()) {
            Espace(4)
            Text("Dans le même genre", style = MaterialTheme.typography.titleMedium)
            for ((autre, p) in proches.take(6)) {
                CarteViolentometre(autre, autre.id in souvenirs.aimes, souvenirs.comptes[autre.id] ?: autre.aime, raison(p)) { ouvrir(autre.id) }
            }
        }
    }
}

internal fun raison(p: Proche): String? = when {
    p.parcours.isNotEmpty() -> "Dans le même parcours : ${p.parcours.first().titre}"
    p.enCommun > 0 -> "En commun : ${p.enCommun} situation${if (p.enCommun > 1) "s" else ""}"
    else -> null
}
