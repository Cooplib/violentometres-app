package fr.cooplib.util.ecrans

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import fr.cooplib.util.modeles.Recit
import fr.cooplib.util.reseau.Motif
import fr.cooplib.util.reseau.Resultat
import fr.cooplib.util.stockage.Brouillon
import fr.cooplib.util.stockage.Depot
import kotlinx.coroutines.launch

/*
 * « Des récits » : lire ce que d'autres ont vécu, et déposer le sien.
 *
 * Avec le dépôt, l'application devient un service de contenu
 * d'utilisateurs au sens de Google : SIGNALER doit être atteignable
 * depuis chaque récit. Ce n'est pas une option, et c'est aussi le seul
 * recours de qui se reconnaît dans un récit.
 */
private val TRIS_RECITS = listOf("Les plus récents", "Les plus lus", "De A à Z")

@Composable
fun Recits(recits: List<Recit>, depot: Depot, garderLesBrouillons: Boolean, ouvert: String? = null) {

    val souvenirs by depot.memoire.etat.collectAsState()

    // Ce qui est ouvert : la liste (null), un récit, son signalement, ou
    // le dépôt. `ouvert` : venu d'ailleurs (un récit recommandé à la fin
    // du point).
    var lu by rememberSaveable { mutableStateOf(ouvert) }
    var signale by rememberSaveable { mutableStateOf(false) }
    var depose by rememberSaveable { mutableStateOf(false) }

    var cherche by rememberSaveable { mutableStateOf("") }
    var tri by rememberSaveable { mutableStateOf(TRIS_RECITS[0]) }

    BackHandler(enabled = lu != null || depose) {
        when {
            signale -> signale = false
            depose -> depose = false
            else -> lu = null
        }
    }

    // L'ordre de la liste est aussi celui dans lequel on glisse d'un récit
    // à l'autre.
    val liste = remember(recits, cherche, tri) {
        recits
            .filter { r -> cherche.isBlank() || correspond(cherche, r.titre, if (r.sansFlou) r.texte else "") }
            .let { l ->
                when (tri) {
                    "Les plus lus" -> l.sortedByDescending { it.vues }
                    "De A à Z" -> l.sortedWith(compareBy(ordreFrancais) { it.titre })
                    else -> l.sortedByDescending { it.creeLe }
                }
            }
    }

    val recit = recits.find { it.id == lu }

    when {
        depose -> Deposer(depot, garderLesBrouillons) { depose = false }
        recit != null && signale -> Signaler(recit, depot) { signale = false }
        recit != null -> Lecteur(
            liste = if (liste.any { it.id == recit.id }) liste else listOf(recit),
            depart = recit.id,
            lus = souvenirs.lus,
            caches = souvenirs.caches,
            marquerLu = depot::marquerLu,
            recacher = depot::recacher,
            changer = { lu = it },
            signaler = { signale = true },
        )
        else -> Liste(liste, souvenirs.lus, souvenirs.caches, cherche, { cherche = it }, tri, { tri = it }, ouvrir = { lu = it }, deposer = { depose = true })
    }
}

@Composable
private fun Liste(
    recits: List<Recit>,
    lus: Set<String>,
    caches: Set<String>,
    cherche: String,
    chercher: (String) -> Unit,
    tri: String,
    trier: (String) -> Unit,
    ouvrir: (String) -> Unit,
    deposer: () -> Unit,
) {

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        item { Text("📖 Des récits", style = MaterialTheme.typography.headlineSmall) }

        item { Encadre("Des récits entiers, écrits par celles et ceux à qui c'est arrivé. Certains sont difficiles à lire : ils restent cachés jusqu'à ce que vous choisissiez de les lire.") }

        item { Button(onClick = deposer, Modifier.fillMaxWidth()) { Text("🖊️ Déposer mon récit") } }

        item { BarreDeRecherche(cherche, chercher, emptyList(), null, {}, TRIS_RECITS, tri, trier, indication = "Chercher un récit") }

        if (recits.isEmpty()) {
            item { Note("Aucun récit ne correspond.") }
        }

        items(recits, key = { it.id }) { r ->
            Card(Modifier.fillMaxWidth(), onClick = { ouvrir(r.id) }) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(r.titre, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        when {
                            r.id in caches -> Note("Caché par vous")
                            r.id in lus -> Note("Lu")
                        }
                    }
                    // Un récit dur à lire ne montre rien de son texte dans la
                    // liste : seulement qu'il est là.
                    if (visible(r, lus, caches)) {
                        Text(r.texte, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    } else {
                        Text("Ce récit peut être difficile à lire.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Compteurs(ilYA(r.creeLe), "👁 ${r.vues}")
                }
            }
        }
    }
}

/*
 * Les récits, un par page : on glisse pour passer au suivant, dans
 * l'ordre de la liste. Un récit qui n'est pas « sans flou » ne s'affiche
 * qu'après « Lire le récit », comme sur le site ; une fois dévoilé, il
 * reste visible (Souvenirs.kt : en mémoire, gardé seulement si la
 * personne garde ses réponses).
 *
 * Pas de flou dessiné : Android ne sait flouter qu'à partir de la
 * version 12, et un texte caché dit la même chose partout.
 */
@Composable
private fun Lecteur(
    liste: List<Recit>,
    depart: String,
    lus: Set<String>,
    caches: Set<String>,
    marquerLu: (String) -> Unit,
    recacher: (String) -> Unit,
    changer: (String) -> Unit,
    signaler: () -> Unit,
) {

    val etat = rememberPagerState(initialPage = liste.indexOfFirst { it.id == depart }.coerceAtLeast(0)) { liste.size }

    // Le récit affiché devient « l'ouvert » : le signalement et le retour
    // portent sur lui.
    LaunchedEffect(etat.currentPage) { changer(liste[etat.currentPage].id) }

    Column {
        if (liste.size > 1) {
            Note("${etat.currentPage + 1} sur ${liste.size} · glissez pour passer au suivant", Modifier.padding(start = 16.dp, top = 8.dp))
        }
        // Toute la hauteur de l'écran, pas seulement celle du texte : on
        // doit pouvoir glisser n'importe où (retour du 7 octobre).
        HorizontalPager(state = etat, beyondViewportPageCount = 0, modifier = Modifier.fillMaxSize()) { page ->
            val recit = liste[page]
            Box(Modifier.fillMaxSize()) { Page {
                Text(recit.titre, style = MaterialTheme.typography.headlineSmall)
                Compteurs(ilYA(recit.creeLe), "👁 ${recit.vues}")

                if (visible(recit, lus, caches)) {
                    Text(recit.texte, style = MaterialTheme.typography.bodyLarge)
                    if (recit.apropos.isNotBlank()) Note(recit.apropos)
                    // Le recacher pour soi, même gardé, même « sans flou ».
                    TextButton(onClick = { recacher(recit.id) }) { Text("🙈 Cacher ce récit pour moi") }
                } else {
                    Encadre(
                        if (recit.id in caches) "Caché par vous. Vous pouvez le lire à nouveau quand vous voulez."
                        else "Ce récit peut être difficile à lire. Vous pouvez le lire maintenant, ou revenir plus tard."
                    )
                    Button(onClick = { marquerLu(recit.id) }) { Text("Lire le récit") }
                }

                // Atteignable depuis chaque récit, lu ou non : on peut avoir à
                // signaler un récit qu'on ne veut pas lire en entier.
                TextButton(onClick = signaler) { Text("Signaler un problème dans ce récit") }
            } }
        }
    }
}

// Un récit se montre s'il est « sans flou » et pas recaché, ou s'il a été
// dévoilé.
private fun visible(r: Recit, lus: Set<String>, caches: Set<String>) =
    r.id in lus || (r.sansFlou && r.id !in caches)

/*
 * « aujourd'hui », « hier », « il y a 3 jours », « il y a un mois » :
 * comme ilYA() dans le points.js du site. Approximatif par construction.
 */
internal fun ilYA(date: String): String? {
    val quand = runCatching { java.time.OffsetDateTime.parse(date).toInstant() }.getOrNull() ?: return null
    val jours = java.time.Duration.between(quand, java.time.Instant.now()).toDays()
    return when {
        jours <= 0 -> "aujourd'hui"
        jours == 1L -> "hier"
        jours < 30 -> "il y a $jours jours"
        jours < 365 -> (jours / 30.0).let { Math.round(it).toInt() }.let { m -> if (m == 1) "il y a un mois" else "il y a $m mois" }
        else -> (jours / 365.0).let { Math.round(it).toInt() }.let { a -> if (a == 1) "il y a un an" else "il y a $a ans" }
    }
}

@Composable
private fun Signaler(recit: Recit, depot: Depot, fini: () -> Unit) {

    var motifs by remember { mutableStateOf<List<Motif>?>(null) }
    var erreur by remember { mutableStateOf<String?>(null) }
    var choisi by rememberSaveable { mutableStateOf<String?>(null) }
    var texte by rememberSaveable { mutableStateOf("") }
    var envoi by remember { mutableStateOf(false) }
    var envoye by remember { mutableStateOf(false) }
    val portee = rememberCoroutineScope()

    // Les motifs viennent du serveur, pour ne pas en inventer un qu'il
    // ne connaîtrait pas (il le changerait en « autre » sans le dire).
    LaunchedEffect(Unit) {
        when (val r = depot.motifs()) {
            is Resultat.Ok -> motifs = r.valeur
            else -> erreur = "Pas de réseau : le signalement ne peut pas partir maintenant. Réessayez plus tard."
        }
    }

    Page {

        Text("Signaler un problème", style = MaterialTheme.typography.headlineSmall)
        Text("« ${recit.titre} »", style = MaterialTheme.typography.titleMedium)

        if (envoye) {
            Text("Merci. Le signalement est arrivé : quelqu'un va regarder ce récit. Il n'est ni caché, ni marqué en attendant, et personne ne saura qu'il a été signalé.")
            Button(onClick = fini) { Text("Revenir au récit") }
            return@Page
        }

        Encadre("Un signalement ne cache rien et n'est pas public. Il est lu par les personnes qui s'occupent du site.")

        erreur?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        motifs?.let { liste ->
            for (m in liste) {
                Row(
                    Modifier.fillMaxWidth().clickable { choisi = m.cle },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = choisi == m.cle, onClick = { choisi = m.cle })
                    Text(m.titre)
                }
            }

            Text("Ce qui ne va pas, si vous voulez le préciser :")
            ChampPrive(texte, { texte = it }, lignes = 4)

            Button(
                onClick = {
                    envoi = true
                    portee.launch {
                        when (val r = depot.signaler(recit.id, choisi!!, texte)) {
                            is Resultat.Ok -> envoye = true
                            is Resultat.Limite -> erreur = "Le serveur demande d'attendre. Réessayez dans ${r.attenteSecondes} secondes."
                            is Resultat.Refuse -> erreur = r.detail ?: "Le signalement n'a pas été enregistré. Réessayez plus tard."
                            is Resultat.Injoignable -> erreur = "Pas de réseau : le signalement n'est pas parti. Réessayez plus tard."
                        }
                        envoi = false
                    }
                },
                enabled = choisi != null && !envoi,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (envoi) "Envoi…" else "Envoyer le signalement") }
        }

        TextButton(onClick = fini) { Text("Annuler") }
    }
}

/*
 * Déposer son récit. Il entre dans un contenu PUBLIC et VERSIONNÉ : les
 * avertissements du site, et une confirmation avant d'envoyer, parce
 * que ce qui part ne se reprend pas.
 *
 * Une coupure pendant l'envoi ne veut pas dire « pas envoyé » : le
 * serveur a pu l'enregistrer. On ne renvoie jamais tout seul (une
 * seconde copie resterait pour toujours), on dit de vérifier.
 */
@Composable
private fun Deposer(depot: Depot, garder: Boolean, fini: () -> Unit) {

    val contexte = LocalContext.current

    // Gardé sur le téléphone seulement si la personne l'a choisi ; sinon
    // en mémoire, oublié au reverrouillage.
    var titre by rememberSaveable { mutableStateOf(if (garder) Brouillon.lire(contexte).titre else "") }
    var texte by rememberSaveable { mutableStateOf(if (garder) Brouillon.lire(contexte).texte else "") }
    var sansFlou by rememberSaveable { mutableStateOf(if (garder) Brouillon.lire(contexte).sansFlou else false) }

    var confirmer by remember { mutableStateOf(false) }

    /*
     * La licence, dite au moment de publier, la première fois : c'est ce
     * qui la rend valable pour ce récit. Ensuite, seulement si la personne
     * n'a pas coché « ne plus me le rappeler ».
     */
    val reglages = remember { fr.cooplib.util.Reglages(contexte) }
    var rappelerLaLicence by remember { mutableStateOf(!reglages.nePlusRappelerLaLicence) }
    var nePlusRappeler by remember { mutableStateOf(false) }
    var envoi by remember { mutableStateOf(false) }
    var issue by remember { mutableStateOf<String?>(null) }
    var publie by remember { mutableStateOf(false) }
    val portee = rememberCoroutineScope()

    fun retenir() {
        if (garder) Brouillon.ecrire(contexte, Brouillon(titre, texte, sansFlou))
    }

    Page {

        Text("Déposer mon récit", style = MaterialTheme.typography.headlineSmall)

        if (publie) {
            Text("Votre récit est publié. Il est maintenant lisible par tout le monde, sur le site et dans l'application.")
            Button(onClick = fini) { Text("Revenir aux récits") }
            return@Page
        }

        Text("Racontez ce qui s'est passé, avec vos mots.")

        Encadre(alerte = true, titre = "Avant d'écrire") {
            Text("Ce récit sera public : tout le monde pourra le lire, sur le site et dans l'application. Le site garde toutes ses versions : ce qui est envoyé ne se reprend pas.")
            Text("Ne mettez jamais le nom d'une personne réelle. Changez ce qui permettrait de vous reconnaître, ou de reconnaître quelqu'un : les noms, les lieux, les dates.", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        }
        Note(
            if (garder) "Votre brouillon est gardé sur ce téléphone, comme vous l'avez choisi, jusqu'à l'envoi."
            else "Votre brouillon n'est pas gardé : si vous quittez l'application, il disparaît."
        )

        Text("Titre")
        ChampPrive(titre, { titre = it; retenir() }, lignes = 1)

        Text("Le récit")
        ChampPrive(texte, { texte = it; retenir() }, lignes = 10)

        Row(Modifier.fillMaxWidth().clickable { sansFlou = !sansFlou; retenir() }, verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = sansFlou, onCheckedChange = { sansFlou = it; retenir() })
            Text("Ce récit peut s'afficher sans flou : il ne contient rien de difficile à lire.")
        }

        issue?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = { confirmer = true },
            enabled = titre.isNotBlank() && texte.isNotBlank() && !envoi,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (envoi) "Envoi…" else "Publier le récit") }

        OutlinedButton(onClick = fini, Modifier.fillMaxWidth()) { Text("Revenir") }
    }

    if (confirmer) {
        AlertDialog(
            onDismissRequest = { confirmer = false },
            title = { Text("Publier ce récit ?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Il sera public, et ne se reprend pas. Vérifiez qu'il ne nomme personne.")
                    if (rappelerLaLicence) {
                        Encadre(titre = "La licence") {
                            Text("Votre récit sera diffusé sous licence CC BY-NC-SA 4.0, et Cooplib pourra le reprendre dans ses propres supports, même vendus. Le détail est dans « Conditions d'utilisation », dans le menu.")
                        }
                        Row(Modifier.fillMaxWidth().clickable { nePlusRappeler = !nePlusRappeler }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = nePlusRappeler, onCheckedChange = { nePlusRappeler = it })
                            Text("Ne plus me le rappeler")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (nePlusRappeler) { reglages.nePlusRappelerLaLicence = true; rappelerLaLicence = false }
                    confirmer = false
                    envoi = true
                    issue = null
                    portee.launch {
                        when (val r = depot.deposer(titre, texte, sansFlou)) {
                            is Resultat.Ok -> {
                                Brouillon.effacer(contexte)
                                publie = true
                            }
                            is Resultat.Limite -> issue = "Le serveur demande d'attendre. Réessayez dans ${r.attenteSecondes} secondes."
                            is Resultat.Refuse -> issue = r.detail ?: "Le récit n'a pas été accepté."
                            is Resultat.Injoignable -> issue =
                                "La connexion a coupé. Le récit est peut-être parti quand même : mettez à jour le contenu (Réglages) et regardez la liste des récits avant de le renvoyer, pour ne pas le publier deux fois."
                        }
                        envoi = false
                    }
                }) { Text("Publier") }
            },
            dismissButton = { TextButton(onClick = { confirmer = false }) { Text("Revenir") } },
        )
    }
}

/*
 * Un champ où le clavier N'APPREND PAS ce qu'on tape
 * (IME_FLAG_NO_PERSONALIZED_LEARNING) : sans ça, il proposerait plus
 * tard, ailleurs, les mots d'un récit à qui emprunte le téléphone.
 * Compose ne sait pas poser ce drapeau : c'est un EditText classique,
 * posé dans l'écran. Les claviers sont libres de l'ignorer ; Gboard et
 * SwiftKey le respectent (mode navigation privée).
 */
@Composable
fun ChampPrive(valeur: String, changer: (String) -> Unit, lignes: Int) {

    val couleur = MaterialTheme.colorScheme.onSurface.toArgb()

    // L'écouteur est posé une fois, à la création du champ : il doit
    // appeler la fonction d'aujourd'hui, pas celle de ce moment-là.
    val changerActuel by rememberUpdatedState(changer)

    AndroidView(
        modifier = Modifier.fillMaxWidth().heightIn(min = (lignes * 24 + 24).dp),
        factory = { c ->
            EditText(c).apply {
                imeOptions = imeOptions or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                    (if (lignes > 1) InputType.TYPE_TEXT_FLAG_MULTI_LINE else 0)
                if (lignes > 1) {
                    minLines = lignes
                    gravity = Gravity.TOP or Gravity.START
                } else {
                    isSingleLine = true
                }
                setTextColor(couleur)
                setText(valeur)
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, d: Int) {}
                    override fun onTextChanged(s: CharSequence?, a: Int, b: Int, d: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        changerActuel(s?.toString() ?: "")
                    }
                })
            }
        },
        // Ne réécrire le champ que si la valeur vient d'ailleurs : réécrit
        // à chaque frappe, il perdrait la position du curseur.
        update = { e -> if (e.text.toString() != valeur) e.setText(valeur) },
    )
}
