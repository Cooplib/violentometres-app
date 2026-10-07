package fr.cooplib.util.ecrans

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fr.cooplib.util.Lanceur
import fr.cooplib.util.Reglages
import fr.cooplib.util.stockage.Brouillon
import fr.cooplib.util.stockage.Depot
import fr.cooplib.util.stockage.Reponses

/*
 * Quatre entrées, et l'ordre est le propos : du plus proche de soi au
 * plus distant. J'ai besoin de quelque chose maintenant, où j'en suis,
 * d'autres l'ont vécu, d'où ça vient.
 */
enum class Entree(val titre: String, val sousTitre: String) {
    AIDE("Trouver de l'aide", "Un numéro, à appeler tout de suite."),
    POINT("Faire le point", "Où j'en suis, en général ou sur un sujet précis."),
    RECITS("Des récits", "Ce que d'autres ont vécu, et déposer le sien."),
    COMPRENDRE("Comprendre", "Des parcours, une carte par étape."),
}

private enum class Ecran { ACCUEIL, AIDE, POINT, RECITS, COMPRENDRE, REGLAGES, PROTECTION }

/*
 * `verrouillee` : déguisée et quittée, l'application rouvre sur la
 * calculatrice (Principale le remet à vrai à chaque sortie). Tout ce qui
 * était ouvert derrière est oublié avec elle : les écrans sortent de la
 * composition, leur état aussi.
 */
@Composable
fun Application(verrouillee: Boolean, deverrouiller: () -> Unit, quitterVite: () -> Unit) {

    val contexte = LocalContext.current
    val reglages = remember { Reglages(contexte) }

    // Copiés en état pour que l'écran suive quand on les change.
    var configure by remember { mutableStateOf(reglages.configure) }
    var deguise by remember { mutableStateOf(reglages.deguise) }
    var empreinte by remember { mutableStateOf(reglages.empreinteDuCode) }
    var garder by remember { mutableStateOf(reglages.garderLesReponses) }

    // Le premier lancement : le déguisement, puis ce qu'on garde.
    var etapeConfiguration by rememberSaveable { mutableStateOf(0) }

    fun deguiser(e: String) {
        reglages.empreinteDuCode = e; empreinte = e
        reglages.deguise = true; deguise = true
        Lanceur.afficherLeVraiNom(contexte, false)
    }

    fun montrerLeVraiNom() {
        reglages.deguise = false; deguise = false
        reglages.empreinteDuCode = null; empreinte = null
        Lanceur.afficherLeVraiNom(contexte, true)
    }

    ThemeUtil {

        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

            when {

                !configure -> Box(Modifier.safeDrawingPadding()) {
                    var protection by rememberSaveable { mutableStateOf(false) }
                    BackHandler(enabled = protection) { protection = false }
                    when {
                        protection -> CeQuiEstProtege(deguise = true)
                        etapeConfiguration == 0 -> Deguisement(
                            garderLaCalculatrice = { deguiser(it); etapeConfiguration = 1 },
                            afficherLeVraiNom = { montrerLeVraiNom(); etapeConfiguration = 1 },
                            voirCeQuiEstProtege = { protection = true },
                        )
                        else -> CeQuOnGarde { g ->
                            reglages.garderLesReponses = g; garder = g
                            reglages.configure = true; configure = true
                            deverrouiller()
                        }
                    }
                }

                // Déguisée et verrouillée : la calculatrice, et rien d'autre.
                deguise && verrouillee -> Calculatrice(empreinte, deverrouiller)

                else -> Contenu(
                    deguise = deguise,
                    garder = garder,
                    deguiser = ::deguiser,
                    montrerLeVraiNom = ::montrerLeVraiNom,
                    changerGarder = {
                        reglages.garderLesReponses = it; garder = it
                        // « Ne plus les garder » : ce qui était gardé part aussi.
                        if (!it) { Reponses.effacer(contexte); Brouillon.effacer(contexte) }
                    },
                    quitterVite = quitterVite,
                )
            }
        }
    }
}

@Composable
private fun Contenu(
    deguise: Boolean,
    garder: Boolean,
    deguiser: (String) -> Unit,
    montrerLeVraiNom: () -> Unit,
    changerGarder: (Boolean) -> Unit,
    quitterVite: () -> Unit,
) {

    val contexte = LocalContext.current
    val depot = remember { Depot.de(contexte) }

    // Le catalogue : celui gardé sur le téléphone, ou celui de l'APK. Lu
    // hors du fil de l'interface (1,1 Mo de JSON) par le dépôt.
    val catalogue by depot.catalogue.collectAsState()
    val aDemander by depot.aDemander.collectAsState()

    /*
     * À chaque déverrouillage, une synchronisation SI elle est due : au
     * plus tous les trois jours, jamais seule sur une connexion facturée
     * (Synchronisation.decider). Jamais depuis la calculatrice : une
     * calculatrice qui consomme des données attire l'œil, et le réseau
     * reste lié à un vrai usage.
     */
    LaunchedEffect(catalogue != null) {
        if (catalogue != null) depot.synchroniser(demandee = false)
    }

    var ecran by rememberSaveable { mutableStateOf(Ecran.ACCUEIL) }
    var recitOuvert by rememberSaveable { mutableStateOf<String?>(null) }

    BackHandler(enabled = ecran != Ecran.ACCUEIL) {
        ecran = if (ecran == Ecran.PROTECTION) Ecran.REGLAGES else Ecran.ACCUEIL
        recitOuvert = null
    }

    Column(Modifier.safeDrawingPadding()) {

        /*
         * « Quitter vite », sur chaque écran, comme le ✕ du site : efface
         * les réponses et le brouillon, ferme l'application et la retire
         * des applications récentes. Déguisée, elle rouvrira sur la
         * calculatrice. Rouge : il dit la sortie, c'est son seul droit.
         */
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = quitterVite) {
                Text("✕ Quitter vite", color = MaterialTheme.colorScheme.error)
            }
        }

        val c = catalogue
        when {
            c == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            ecran == Ecran.ACCUEIL -> Accueil(
                aDemander = aDemander,
                mettreAJour = { depot.synchroniser(demandee = true) },
                plusTard = depot::plusTard,
                ouvrir = { e -> ecran = Ecran.valueOf(e.name) },
                reglages = { ecran = Ecran.REGLAGES },
                protection = { ecran = Ecran.PROTECTION },
            )
            ecran == Ecran.AIDE -> Aides(c.aides)
            ecran == Ecran.RECITS -> Recits(c.recits, depot, garderLesBrouillons = garder, ouvert = recitOuvert)
            ecran == Ecran.POINT -> FaireLePoint(c, garder, ouvrirRecit = { recitOuvert = it; ecran = Ecran.RECITS })
            ecran == Ecran.REGLAGES -> ReglagesEcran(
                deguise = deguise,
                garderLesReponses = garder,
                deguiser = deguiser,
                afficherLeVraiNom = montrerLeVraiNom,
                changerGarder = changerGarder,
                voirCeQuiEstProtege = { ecran = Ecran.PROTECTION },
                miseAJour = { MiseAJour(depot) },
            )
            ecran == Ecran.PROTECTION -> CeQuiEstProtege(deguise)
            else -> AVenir(Entree.valueOf(ecran.name))
        }
    }
}

@Composable
private fun Accueil(
    aDemander: Boolean,
    mettreAJour: () -> Unit,
    plusTard: () -> Unit,
    ouvrir: (Entree) -> Unit,
    reglages: () -> Unit,
    protection: () -> Unit,
) {

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Connexion facturée au volume : on demande, une fois, sans
        // insister. « Plus tard » ne redemande pas avant trois jours.
        if (aDemander) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Une mise à jour du contenu est possible. Vous êtes sur une connexion qui peut être facturée au volume.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = mettreAJour) { Text("Mettre à jour") }
                        OutlinedButton(onClick = plusTard) { Text("Plus tard") }
                    }
                }
            }
        }

        for (e in Entree.entries) {
            Card(onClick = { ouvrir(e) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(e.titre, style = MaterialTheme.typography.titleLarge)
                    Text(e.sousTitre, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        TextButton(onClick = protection) { Text("Ce qui est protégé, ce qui ne l'est pas") }
        TextButton(onClick = reglages) { Text("Réglages") }
    }
}

@Composable
private fun AVenir(e: Entree) {
    Column(Modifier.padding(16.dp)) {
        Text(e.titre, style = MaterialTheme.typography.headlineSmall)
        Text("Pas encore dans cette version.", Modifier.padding(top = 8.dp))
    }
}

// Dans les réglages : la mise à jour à la demande, promise par la page
// « ce qui est protégé ».
@Composable
fun MiseAJour(depot: Depot) {

    val enCours by depot.enCours.collectAsState()
    val message by depot.message.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        Text("Le contenu", style = MaterialTheme.typography.titleMedium)

        Text("Le contenu du site est sur ce téléphone, et sert sans réseau. Il se met à jour tout seul tous les trois jours au plus, en wifi. Ou maintenant :")

        Button(onClick = { depot.synchroniser(demandee = true) }, enabled = !enCours, modifier = Modifier.fillMaxWidth()) {
            Text(if (enCours) "Mise à jour en cours…" else "Mettre à jour le contenu")
        }

        message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}
