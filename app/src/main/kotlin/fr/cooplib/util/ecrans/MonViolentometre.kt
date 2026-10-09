package fr.cooplib.util.ecrans

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import fr.cooplib.util.Reglages
import fr.cooplib.util.donnees.Catalogue
import fr.cooplib.util.modeles.Niveau
import fr.cooplib.util.modeles.SituationDeBibliotheque
import fr.cooplib.util.perso.MaSituation
import fr.cooplib.util.perso.MonViolentometre
import fr.cooplib.util.stockage.Depot
import java.util.UUID

/*
 * « Mon violentomètre » : construire sa propre échelle, sur le téléphone
 * seulement (décidé le 9 octobre 2026). Des situations prises dans la
 * bibliothèque, ou écrites soi-même ; chacune se réécrit, et se range au
 * niveau qu'on veut.
 *
 * RIEN N'EST ENVOYÉ, et l'écran le dit, avec une fenêtre la première fois.
 * Les phrases s'écrivent dans le champ où le clavier n'apprend pas.
 *
 * Le lire (en entier ou situation par situation) et faire le point dessus
 * passent par ce qui existe déjà : le noyau le convertit
 * (perso/MonViolentometre.kt).
 */

// La cible d'un point sur mon violentomètre : « perso:<id> ».
const val PERSO = "perso:"

@Composable
fun MesViolentometres(catalogue: Catalogue, depot: Depot, garder: Boolean, faireLePoint: (String) -> Unit) {

    val contexte = LocalContext.current
    val reglages = remember { Reglages(contexte) }
    val liste by depot.carnet.liste.collectAsState()

    var ouvert by rememberSaveable { mutableStateOf<String?>(null) }
    var bibliotheque by rememberSaveable { mutableStateOf(false) }
    var lecture by rememberSaveable { mutableStateOf<String?>(null) }
    var expliquer by rememberSaveable { mutableStateOf(!reglages.nePlusExpliquerMonViolentometre) }

    val niveaux = catalogue.pointGeneral.niveaux.sortedBy { it.position }
    val vm = liste.find { it.id == ouvert }

    BackHandler(enabled = lecture != null || bibliotheque || ouvert != null) {
        when {
            lecture != null -> lecture = null
            bibliotheque -> bibliotheque = false
            else -> ouvert = null
        }
    }

    if (expliquer) {
        Explication(garder) { nePlusAfficher ->
            if (nePlusAfficher) reglages.nePlusExpliquerMonViolentometre = true
            expliquer = false
        }
    }

    when {
        vm != null && lecture == "entiere" -> EchelleEntiere(vm.enViolentometre(niveaux), fermer = { lecture = null }, faireLePoint = { faireLePoint(PERSO + vm.id) })
        vm != null && lecture == "carte" -> LectureEchelle(vm.enViolentometre(niveaux), fermer = { lecture = null }, faireLePoint = { faireLePoint(PERSO + vm.id) })
        vm != null && bibliotheque -> Bibliotheque(catalogue, niveaux, vm, depot) { bibliotheque = false }
        vm != null -> Editeur(
            vm, niveaux, depot, garder,
            prendre = { bibliotheque = true },
            lire = { lecture = it },
            faireLePoint = { faireLePoint(PERSO + vm.id) },
            fermer = { ouvert = null },
        )
        else -> Page {
            Text("✍️ Mon violentomètre", style = MaterialTheme.typography.headlineSmall)
            Text("Rangez vous-même des situations, de ce qui va bien à ce qui met en danger. Prenez-les dans la bibliothèque, réécrivez-les, ou écrivez les vôtres.", style = MaterialTheme.typography.bodyLarge)
            Encadre(titre = "Il reste sur ce téléphone") {
                Text("Rien n'est envoyé au site. " + if (garder) "Il est gardé sur ce téléphone, comme vos réponses ; « Quitter vite » l'efface." else "Il disparaît quand vous quittez l'application, comme vos réponses. Pour le garder : Réglages, « Les garder sur ce téléphone ».")
            }
            for (m in liste) {
                Card(Modifier.fillMaxWidth(), onClick = { ouvert = m.id }) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(m.titre, style = MaterialTheme.typography.titleMedium)
                        Note("${m.situations.size} situation${if (m.situations.size > 1) "s" else ""}")
                    }
                }
            }
            Button(onClick = {
                val nouveau = MonViolentometre(id = UUID.randomUUID().toString())
                depot.carnet.changer { it + nouveau }
                ouvert = nouveau.id
            }, modifier = Modifier.fillMaxWidth()) { Text("+ Construire un violentomètre") }
            TextButton(onClick = { expliquer = true }) { Text("Comment ça marche ?") }
        }
    }
}

@Composable
private fun Explication(garder: Boolean, fermer: (nePlusAfficher: Boolean) -> Unit) {
    var nePlus by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = { fermer(nePlus) }) {
        Surface(shape = Charte.ArrondiGrand, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("✍️ Mon violentomètre", style = MaterialTheme.typography.titleLarge)
                Text("Construire sa propre échelle aide à voir ce qu'on vit : ranger soi-même une situation entre Vigilance et Danger fait réfléchir autrement que de la cocher.")
                Text("Prenez des situations dans la bibliothèque, plus de mille, ou écrivez les vôtres. Chacune se réécrit, et se range au niveau que vous voulez. Ensuite, lisez-le, ou faites le point dessus.")
                Encadre(alerte = true, titre = "Rien n'est envoyé") {
                    Text("Votre violentomètre reste sur ce téléphone, il ne part jamais au site. " +
                        if (garder) "Il est gardé, comme vos réponses ; « Quitter vite » l'efface."
                        else "Par défaut, il disparaît quand vous quittez l'application. Pour le garder : Réglages, « Les garder sur ce téléphone ».")
                    Text("Il peut dire beaucoup de vous : n'y écrivez pas de noms.")
                }
                Row(Modifier.fillMaxWidth().clickable { nePlus = !nePlus }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = nePlus, onCheckedChange = { nePlus = it })
                    Text("Ne plus afficher")
                }
                Button(onClick = { fermer(nePlus) }, Modifier.fillMaxWidth()) { Text("C'est compris") }
            }
        }
    }
}

@Composable
private fun Editeur(
    vm: MonViolentometre,
    niveaux: List<Niveau>,
    depot: Depot,
    garder: Boolean,
    prendre: () -> Unit,
    lire: (String) -> Unit,
    faireLePoint: () -> Unit,
    fermer: () -> Unit,
) {

    // La situation en cours d'écriture : une existante, ou une nouvelle à
    // un niveau donné.
    var edition by remember { mutableStateOf<MaSituation?>(null) }
    var supprimer by remember { mutableStateOf(false) }
    val fond = MaterialTheme.colorScheme.surface

    fun enregistrer(s: MaSituation) {
        depot.carnet.remplacer(vm.copy(situations = if (vm.situations.any { it.id == s.id }) vm.situations.map { if (it.id == s.id) s else it } else vm.situations + s))
    }

    Page {

        Note("Mon violentomètre · rien n'est envoyé")
        ChampPrive(vm.titre, { depot.carnet.remplacer(vm.copy(titre = it)) }, lignes = 1)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = prendre, Modifier.weight(1f)) { Text("📚 Bibliothèque") }
            OutlinedButton(onClick = { edition = MaSituation(UUID.randomUUID().toString(), "", gravite = 1) }, Modifier.weight(1f)) { Text("✍️ Écrire") }
        }

        if (vm.situations.isEmpty()) {
            Encadre("Prenez des situations dans la bibliothèque, ou écrivez les vôtres. Touchez-en une ensuite pour la réécrire ou changer son niveau.")
        }

        for (n in niveaux) {
            val ici = vm.situations.filter { it.gravite == n.position }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EtiquetteNiveau(n)
                Note("${ici.size}")
                Box(Modifier.weight(1f))
                TextButton(onClick = { edition = MaSituation(UUID.randomUUID().toString(), "", gravite = n.position) }) { Text("+ Écrire ici") }
            }
            for (s in ici) {
                Surface(Modifier.fillMaxWidth().clickable { edition = s }, shape = Charte.ArrondiMoyen, color = teinteSur(couleur(n), fond, 0.12f)) {
                    Row(Modifier.height(IntrinsicSize.Min)) {
                        Box(Modifier.width(5.dp).fillMaxHeight().background(couleur(n)))
                        Text(s.texte, Modifier.padding(12.dp).weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text("✎", Modifier.padding(12.dp), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        if (vm.situations.isNotEmpty()) {
            Espace(4)
            Button(onClick = faireLePoint, Modifier.fillMaxWidth()) { Text("🧭 Faire le point dessus") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { lire("carte") }, Modifier.weight(1f)) { Text("📊 Situation par situation") }
                OutlinedButton(onClick = { lire("entiere") }, Modifier.weight(1f)) { Text("📜 En entier") }
            }
        }

        Note(if (garder) "Gardé sur ce téléphone ; « Quitter vite » l'efface." else "Pas gardé : il disparaît quand vous quittez l'application.")
        TextButton(onClick = { supprimer = true }) { Text("Supprimer ce violentomètre", color = MaterialTheme.colorScheme.error) }
    }

    edition?.let { s ->
        EditionSituation(
            s, niveaux,
            existe = vm.situations.any { it.id == s.id },
            enregistrer = { enregistrer(it); edition = null },
            supprimer = { depot.carnet.remplacer(vm.copy(situations = vm.situations.filter { it.id != s.id })); edition = null },
            fermer = { edition = null },
        )
    }

    if (supprimer) {
        Dialog(onDismissRequest = { supprimer = false }) {
            Surface(shape = Charte.ArrondiGrand, color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Supprimer « ${vm.titre} » ?", style = MaterialTheme.typography.titleMedium)
                    Text("Il disparaît de ce téléphone. Il n'existe nulle part ailleurs.")
                    Button(onClick = { depot.carnet.changer { l -> l.filter { it.id != vm.id } }; supprimer = false; fermer() }, Modifier.fillMaxWidth()) { Text("Supprimer") }
                    TextButton(onClick = { supprimer = false }) { Text("Annuler") }
                }
            }
        }
    }
}

// Écrire ou réécrire une situation, et choisir son niveau.
@Composable
private fun EditionSituation(
    s: MaSituation,
    niveaux: List<Niveau>,
    existe: Boolean,
    enregistrer: (MaSituation) -> Unit,
    supprimer: () -> Unit,
    fermer: () -> Unit,
) {
    var texte by remember(s.id) { mutableStateOf(s.texte) }
    var gravite by remember(s.id) { mutableStateOf(s.gravite) }

    Dialog(onDismissRequest = fermer) {
        Surface(shape = Charte.ArrondiGrand, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (existe) "Réécrire la situation" else "Écrire une situation", style = MaterialTheme.typography.titleMedium)
                Note("Avec vos mots, à la première personne, comme sur le site : « Il regarde mon téléphone quand je dors. » Pas de noms.")
                ChampPrive(texte, { texte = it }, lignes = 4)
                Selecteur("Niveau", niveaux.map { Choix(it.position.toString(), it.label) }, gravite.toString()) { gravite = it?.toInt() ?: gravite }
                Button(onClick = { enregistrer(s.copy(texte = texte.trim(), gravite = gravite)) }, enabled = texte.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Enregistrer") }
                if (existe) TextButton(onClick = supprimer) { Text("Retirer de mon violentomètre", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = fermer) { Text("Annuler") }
            }
        }
    }
}

/*
 * La bibliothèque : toutes les situations du site, plus de mille, les plus
 * employées d'abord. Une situation prise arrive au niveau où les
 * violentomètres la rangent le plus souvent (Vigilance si aucun ne
 * l'emploie) ; on la change ensuite d'un toucher.
 */
@Composable
private fun Bibliotheque(catalogue: Catalogue, niveaux: List<Niveau>, vm: MonViolentometre, depot: Depot, fermer: () -> Unit) {

    var cherche by rememberSaveable { mutableStateOf("") }
    var niveau by rememberSaveable { mutableStateOf<String?>(null) }

    // Un catalogue d'avant la bibliothèque : les situations de ses
    // violentomètres, à défaut.
    val toutes = remember(catalogue) {
        catalogue.bibliotheque.ifEmpty {
            catalogue.violentometres.flatMap { v -> v.situations.map { SituationDeBibliotheque(it.situationId, it.texte, 1, it.gravite) } }.distinctBy { it.id }
        }
    }
    val prises = vm.situations.mapNotNull { it.origine }.toSet()

    val liste = remember(toutes, cherche, niveau) {
        toutes
            .filter { s -> niveau == null || (if (niveau == "aucun") s.niveauSuggere == null else s.niveauSuggere?.toString() == niveau) }
            .filter { s -> cherche.isBlank() || correspond(cherche, s.texte) }
    }

    val choixNiveaux = listOf(Choix(null, "Tous les niveaux")) + niveaux.map { Choix(it.position.toString(), it.label) } + Choix("aucun", "Sans niveau suggéré")

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📚 La bibliothèque", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = fermer) { Text("Terminer") }
            }
        }

        item {
            BarreDeRecherche(cherche, { cherche = it }, indication = "Chercher une situation") {
                Selecteur("Niveau suggéré", choixNiveaux, niveau) { niveau = it }
            }
        }

        item { Note("${liste.size} situations · ${prises.size} dans « ${vm.titre} »") }

        items(liste, key = { it.id }) { s ->
            val prise = s.id in prises
            val suggere = niveaux.find { it.position == s.niveauSuggere }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(s.texte, style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (suggere != null) EtiquetteNiveau(suggere) else Note("Aucun violentomètre ne l'emploie")
                        Box(Modifier.weight(1f))
                        if (prise) {
                            Text("✓ Prise", color = MaterialTheme.colorScheme.primary)
                        } else {
                            TextButton(onClick = {
                                val nouvelle = MaSituation(UUID.randomUUID().toString(), s.texte, s.niveauSuggere ?: 1, origine = s.id)
                                depot.carnet.remplacer(vm.copy(situations = vm.situations + nouvelle))
                            }) { Text("+ Prendre") }
                        }
                    }
                }
            }
        }
    }
}
