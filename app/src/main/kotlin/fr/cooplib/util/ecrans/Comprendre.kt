package fr.cooplib.util.ecrans

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import fr.cooplib.util.reseau.Resultat
import fr.cooplib.util.stockage.Depot
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch

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
/*
 * Le parcours ouvert et l'étape où l'on en est vivent AU-DESSUS de cet
 * écran (Application) : partir faire le point depuis une étape fait
 * sortir « Comprendre » de l'écran, et son état avec. Au retour, on
 * retombait sur la liste des parcours au lieu de l'étape (retour du 7
 * octobre).
 */
@Composable
fun Comprendre(
    catalogue: Catalogue,
    depot: Depot,
    ouvert: String?,
    ouvrir: (String?) -> Unit,
    etape: Int,
    allerA: (Int) -> Unit,
    faireLePoint: (String) -> Unit,
) {

    BackHandler(enabled = ouvert != null) { ouvrir(null) }

    val parcours = catalogue.parcours.find { it.id == ouvert }

    if (parcours == null) {
        Liste(catalogue.parcours, depot) { ouvrir(it) }
    } else {
        androidx.compose.runtime.key(parcours.id) {
            Cartes(parcours, catalogue, depot, etape, allerA, faireLePoint)
        }
    }
}

private val TRIS_PARCOURS = listOf("Les plus vus", "Les plus aimés", "Les plus courts", "De A à Z")

@Composable
private fun Liste(parcours: List<Parcours>, depot: Depot, ouvrir: (String) -> Unit) {

    val souvenirs by depot.memoire.etat.collectAsState()

    var cherche by rememberSaveable { mutableStateOf("") }
    var cadre by rememberSaveable { mutableStateOf<String?>(null) }
    var tri by rememberSaveable { mutableStateOf(TRIS_PARCOURS[0]) }

    // Les cadres d'analyse qui portent des parcours : « sous plusieurs
    // angles », c'est là que ça se choisit.
    val cadres = remember(parcours) {
        parcours.flatMap { p -> p.cadres.map { it.nom } }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key }
    }

    val liste = remember(parcours, cherche, cadre, tri, souvenirs) {
        parcours
            .filter { p -> cadre == null || p.cadres.any { it.nom == cadre } }
            .filter { p -> cherche.isBlank() || correspond(cherche, p.titre, p.description, p.etapes.joinToString(" ") { it.cible?.titre ?: "" }) }
            .let { l ->
                when (tri) {
                    "Les plus aimés" -> l.sortedByDescending { souvenirs.comptes[it.id] ?: it.aime }
                    "Les plus courts" -> l.sortedBy { it.etapes.size }
                    "De A à Z" -> l.sortedWith(compareBy(ordreFrancais) { it.titre })
                    else -> l.sortedByDescending { it.vues }
                }
            }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        item { Text("🗺️ Comprendre", style = MaterialTheme.typography.headlineSmall) }
        item { Encadre("Des parcours courts, une carte par étape : d'une situation vécue à ce qui la produit. Certains lisent la même chose sous plusieurs angles.") }

        item {
            BarreDeRecherche(cherche, { cherche = it }, cadres, cadre, { cadre = it }, TRIS_PARCOURS, tri, { tri = it }, indication = "Chercher un parcours")
        }

        items(liste, key = { it.id }) { p ->
            Card(Modifier.fillMaxWidth(), onClick = { ouvrir(p.id) }) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(p.titre, style = MaterialTheme.typography.titleMedium)
                    if (p.description.isNotBlank()) Text(p.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    // Les étapes en un coup d'œil : un point par étape.
                    Text(p.etapes.sortedBy { it.position }.joinToString("  →  ") { it.cible?.titre ?: "" }, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Etiquettes(p.cadres.map { it.nom })
                    Compteurs(
                        "${p.etapes.size} étape${if (p.etapes.size > 1) "s" else ""}",
                        "👁 ${p.vues}",
                        (if (p.id in souvenirs.aimes) "♥ " else "♡ ") + (souvenirs.comptes[p.id] ?: p.aime),
                    )
                }
            }
        }
    }
}

@Composable
private fun Cartes(parcours: Parcours, catalogue: Catalogue, depot: Depot, i: Int, allerA: (Int) -> Unit, faireLePoint: (String) -> Unit) {

    val souvenirs by depot.memoire.etat.collectAsState()
    val portee = rememberCoroutineScope()
    var erreur by remember { mutableStateOf<String?>(null) }

    val etapes = parcours.etapes.sortedBy { it.position }

    // `i` : -1 pour l'ouverture du parcours, etapes.size pour sa fin.
    // L'échelle d'une étape, par-dessus le parcours : « carte » (une
    // situation à la fois) ou « entiere » (de haut en bas).
    var echelle by rememberSaveable { mutableStateOf<String?>(null) }
    var lecture by rememberSaveable { mutableStateOf("carte") }
    catalogue.violentometres.find { it.id == echelle }?.let { vm ->
        val versLePoint: (() -> Unit)? = if (vm.id in catalogue.pointsParViolentometre) ({ faireLePoint(vm.id) }) else null
        if (lecture == "entiere") EchelleEntiere(vm, fermer = { echelle = null }, faireLePoint = versLePoint)
        else LectureEchelle(vm, fermer = { echelle = null }, faireLePoint = versLePoint)
        return
    }

    /*
     * Une page par étape, qu'on fait glisser (retour du 8 octobre 2026) :
     * l'ouverture du parcours, ses étapes, sa fin. La page et l'étape `i`
     * se suivent dans les deux sens : glisser change l'étape, les boutons
     * font glisser.
     */
    val pages = etapes.size + 2
    val etat = rememberPagerState(initialPage = (i + 1).coerceIn(0, pages - 1)) { pages }
    LaunchedEffect(etat.currentPage) { if (etat.currentPage - 1 != i) allerA(etat.currentPage - 1) }
    LaunchedEffect(i) { if (etat.currentPage != i + 1) etat.animateScrollToPage((i + 1).coerceIn(0, pages - 1)) }

    Column(Modifier.fillMaxSize()) {

        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(parcours.titre, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            if (etapes.isNotEmpty()) {
                LinearProgressIndicator(
                    progress = { (etat.currentPage.coerceIn(0, etapes.size)).toFloat() / etapes.size },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        HorizontalPager(state = etat, modifier = Modifier.weight(1f)) { page ->

            val j = page - 1

            Box(Modifier.fillMaxSize()) { Page {

                when {
                    j < 0 -> {
                        Text(parcours.titre, style = MaterialTheme.typography.headlineSmall)
                        if (parcours.description.isNotBlank()) Text(parcours.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Etiquettes(parcours.cadres.map { it.nom })
                        val aime = parcours.id in souvenirs.aimes
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            BoutonJAime(aime, souvenirs.comptes[parcours.id] ?: parcours.aime) {
                                portee.launch {
                                    erreur = if (depot.aimer("parcours", parcours.id, !aime) is Resultat.Ok) null else "Pas de réseau : le « J'aime » n'est pas parti."
                                }
                            }
                            Compteurs("👁 ${parcours.vues}")
                        }
                        erreur?.let { Note(it) }
                        if (parcours.apropos.isNotBlank()) Encadre(parcours.apropos)
                        Note("${etapes.size} étape${if (etapes.size > 1) "s" else ""}, à faire glisser.")
                        Button(onClick = { allerA(0) }, Modifier.fillMaxWidth(), enabled = etapes.isNotEmpty()) { Text("Commencer") }
                    }

                    j >= etapes.size -> {
                        Text("Fin du parcours", style = MaterialTheme.typography.headlineSmall)
                        if (parcours.conclusion.isNotBlank()) Encadre(parcours.conclusion)
                        OutlinedButton(onClick = { allerA(0) }, Modifier.fillMaxWidth()) { Text("Revoir depuis le début") }
                    }

                    else -> Carte(etapes[j], j, etapes.size, catalogue, faireLePoint, lireEchelle = { id, comment -> lecture = comment; echelle = id })
                }
            } }
        }

        // En bas, toujours au même endroit.
        if (etat.currentPage > 0) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { allerA(i - 1) }) { Text("← Précédente") }
                if (i < etapes.size) {
                    Button(onClick = { allerA(i + 1) }) { Text(if (i == etapes.size - 1) "Terminer" else "Suivante →") }
                }
            }
        }
    }
}

@Composable
private fun Carte(etape: Etape, i: Int, n: Int, catalogue: Catalogue, faireLePoint: (String) -> Unit, lireEchelle: (String, String) -> Unit) {

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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { lireEchelle(vm.id, "carte") }, Modifier.weight(1f)) { Text("📊 Situation par situation") }
                    OutlinedButton(onClick = { lireEchelle(vm.id, "entiere") }, Modifier.weight(1f)) { Text("📜 L'échelle en entier") }
                }
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
