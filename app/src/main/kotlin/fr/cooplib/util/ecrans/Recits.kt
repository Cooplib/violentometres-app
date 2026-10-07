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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
@Composable
fun Recits(recits: List<Recit>, depot: Depot, garderLesBrouillons: Boolean) {

    // Ce qui est ouvert : la liste (null), un récit, son signalement, ou
    // le dépôt.
    var lu by rememberSaveable { mutableStateOf<String?>(null) }
    var signale by rememberSaveable { mutableStateOf(false) }
    var depose by rememberSaveable { mutableStateOf(false) }

    // Les récits déjà dévoilés, pour ne pas reflouter en revenant.
    val ouverts = remember { mutableSetOf<String>() }

    BackHandler(enabled = lu != null || depose) {
        when {
            signale -> signale = false
            depose -> depose = false
            else -> lu = null
        }
    }

    val recit = recits.find { it.id == lu }

    when {
        depose -> Deposer(depot, garderLesBrouillons) { depose = false }
        recit != null && signale -> Signaler(recit, depot) { signale = false }
        recit != null -> Lecture(recit, recit.id in ouverts, { ouverts += recit.id }) { signale = true }
        else -> Liste(recits, ouvrir = { lu = it }, deposer = { depose = true })
    }
}

@Composable
private fun Liste(recits: List<Recit>, ouvrir: (String) -> Unit, deposer: () -> Unit) {

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        item { Text("Des récits", style = MaterialTheme.typography.headlineSmall) }

        item {
            Button(onClick = deposer, Modifier.fillMaxWidth()) { Text("Déposer mon récit") }
        }

        if (recits.isEmpty()) {
            item { Text("Aucun récit pour l'instant.") }
        }

        items(recits.sortedByDescending { it.creeLe }, key = { it.id }) { r ->
            Card(onClick = { ouvrir(r.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(r.titre, style = MaterialTheme.typography.titleMedium)
                    // Un récit dur à lire ne montre rien de son texte dans la
                    // liste : seulement qu'il est là.
                    Text(
                        if (r.sansFlou) r.texte.take(140) + if (r.texte.length > 140) "…" else ""
                        else "Ce récit peut être difficile à lire.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

/*
 * Un récit qui n'est pas déclaré « sans flou » ne s'affiche qu'après
 * « Lire le récit », comme sur le site. Pas de flou dessiné : Android ne
 * sait flouter qu'à partir de la version 12, et un texte caché dit la
 * même chose partout.
 */
@Composable
private fun Lecture(recit: Recit, dejaOuvert: Boolean, ouvrir: () -> Unit, signaler: () -> Unit) {

    var visible by rememberSaveable(recit.id) { mutableStateOf(recit.sansFlou || dejaOuvert) }

    Page {

        Text(recit.titre, style = MaterialTheme.typography.headlineSmall)

        if (visible) {
            Text(recit.texte)
        } else {
            Text("Ce récit peut être difficile à lire. Vous pouvez le lire maintenant, ou revenir plus tard.")
            Button(onClick = { visible = true; ouvrir() }) { Text("Lire le récit") }
        }

        if (recit.apropos.isNotBlank() && visible) {
            Text(recit.apropos, style = MaterialTheme.typography.bodySmall)
        }

        // Atteignable depuis chaque récit, lu ou non : on peut avoir à
        // signaler un récit qu'on ne veut pas lire en entier.
        TextButton(onClick = signaler) { Text("Signaler un problème dans ce récit") }
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

        Text("Un signalement ne cache rien et n'est pas public. Il est lu par les personnes qui s'occupent du site.")

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

        Text("Avant d'écrire", style = MaterialTheme.typography.titleMedium)
        Text("Ce récit sera public : tout le monde pourra le lire, sur le site et dans l'application. Le site garde toutes ses versions : ce qui est envoyé ne se reprend pas.")
        Text("Ne mettez jamais le nom d'une personne réelle. Changez ce qui permettrait de vous reconnaître, ou de reconnaître quelqu'un : les noms, les lieux, les dates.")
        Text(
            if (garder) "Votre brouillon est gardé sur ce téléphone, comme vous l'avez choisi, jusqu'à l'envoi."
            else "Votre brouillon n'est pas gardé : si vous quittez l'application, il disparaît.",
            style = MaterialTheme.typography.bodySmall,
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
            text = { Text("Il sera public, et ne se reprend pas. Vérifiez qu'il ne nomme personne.") },
            confirmButton = {
                Button(onClick = {
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
