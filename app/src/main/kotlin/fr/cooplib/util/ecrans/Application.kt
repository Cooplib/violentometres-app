package fr.cooplib.util.ecrans

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.cooplib.util.Lanceur
import fr.cooplib.util.Reglages
import fr.cooplib.util.donnees.Catalogue
import fr.cooplib.util.stockage.Brouillon
import fr.cooplib.util.stockage.Depot
import fr.cooplib.util.stockage.Reponses

/*
 * Quatre entrées, et l'ordre est le propos : du plus proche de soi au
 * plus distant. J'ai besoin de quelque chose maintenant, où j'en suis,
 * d'autres l'ont vécu, d'où ça vient.
 */
enum class Entree(val icone: String, val titre: String, val sousTitre: String) {
    AIDE("🆘", "Trouver de l'aide", "Un numéro, à appeler tout de suite."),
    POINT("🧭", "Faire le point", "Où j'en suis, en général ou sur un sujet précis."),
    RECITS("📖", "Des récits", "Ce que d'autres ont vécu, et déposer le sien."),
    COMPRENDRE("🗺️", "Comprendre", "Des parcours courts, une carte par étape."),
}

private enum class Ecran { ACCUEIL, AIDE, POINT, RECITS, COMPRENDRE, REGLAGES, PROTECTION, DIDACTICIEL }

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
    var theme by remember { mutableStateOf(reglages.theme) }

    // Le premier lancement : le déguisement, puis ce qu'on garde.
    var etapeConfiguration by rememberSaveable { mutableStateOf(0) }

    fun poserLeCode(e: String) {
        reglages.empreinteDuCode = e; empreinte = e
    }

    fun deguiser() {
        reglages.deguise = true; deguise = true
        Lanceur.afficherLeVraiNom(contexte, false)
    }

    /*
     * Le vrai nom GARDE le code : c'est le code de secours, avec lequel
     * « Quitter vite » remet la calculatrice même quand le vrai nom est
     * affiché (si on a voulu quitter vite, c'est qu'il y a sans doute une
     * galère ; décidé le 7 octobre 2026).
     */
    fun montrerLeVraiNom() {
        reglages.deguise = false; deguise = false
        Lanceur.afficherLeVraiNom(contexte, true)
    }

    ThemeUtil(theme) {

        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

            when {

                !configure -> Box(Modifier.safeDrawingPadding()) {
                    var protection by rememberSaveable { mutableStateOf(false) }
                    BackHandler(enabled = protection) { protection = false }
                    when {
                        protection -> CeQuiEstProtege(deguise = true)
                        etapeConfiguration == 0 -> Deguisement(
                            garderLaCalculatrice = { poserLeCode(it); deguiser(); etapeConfiguration = 1 },
                            afficherLeVraiNom = { poserLeCode(it); montrerLeVraiNom(); etapeConfiguration = 1 },
                            voirCeQuiEstProtege = { protection = true },
                        )
                        etapeConfiguration == 1 -> CeQuOnGarde { g ->
                            reglages.garderLesReponses = g; garder = g
                            etapeConfiguration = 2
                        }
                        etapeConfiguration == 2 -> ProposerLeDidacticiel { suivre ->
                            if (suivre) etapeConfiguration = 3
                            else { reglages.configure = true; configure = true; deverrouiller() }
                        }
                        else -> Didacticiel {
                            reglages.configure = true; configure = true; deverrouiller()
                        }
                    }
                }

                // Déguisée et verrouillée : la calculatrice, et rien d'autre.
                deguise && verrouillee -> Calculatrice(empreinte, deverrouiller)

                else -> Contenu(
                    deguise = deguise,
                    garder = garder,
                    theme = theme,
                    changerTheme = { reglages.theme = it; theme = it },
                    changerLeCode = ::poserLeCode,
                    deguiser = ::deguiser,
                    montrerLeVraiNom = ::montrerLeVraiNom,
                    changerGarder = {
                        reglages.garderLesReponses = it; garder = it
                        val depot = Depot.de(contexte)
                        if (it) {
                            depot.memoire.garderMaintenant()
                        } else {
                            // « Ne plus les garder » : ce qui était gardé part aussi.
                            Reponses.effacer(contexte); Brouillon.effacer(contexte); depot.memoire.oublier()
                        }
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
    theme: String,
    changerTheme: (String) -> Unit,
    changerLeCode: (String) -> Unit,
    deguiser: () -> Unit,
    montrerLeVraiNom: () -> Unit,
    changerGarder: (Boolean) -> Unit,
    quitterVite: () -> Unit,
) {

    val contexte = LocalContext.current
    val depot = remember { Depot.de(contexte) }

    // Le catalogue : celui gardé sur le téléphone, ou celui de l'APK. Lu
    // hors du fil de l'interface (1,5 Mo de JSON) par le dépôt.
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
    var pointCible by rememberSaveable { mutableStateOf<String?>(null) }
    // La fiche d'un violentomètre à ouvrir en arrivant dans « Faire le
    // point » (une suggestion de l'accueil).
    var ficheAOuvrir by rememberSaveable { mutableStateOf<String?>(null) }
    // Le parcours ouvert et son étape, gardés ici pour survivre à un
    // détour par le point ; et d'où vient le point en cours.
    var parcoursOuvert by rememberSaveable { mutableStateOf<String?>(null) }
    var etapeOuverte by rememberSaveable { mutableIntStateOf(-1) }
    var pointDepuisParcours by rememberSaveable { mutableStateOf(false) }

    fun aller(e: Ecran) {
        ecran = e; recitOuvert = null; pointCible = null; ficheAOuvrir = null
        parcoursOuvert = null; etapeOuverte = -1; pointDepuisParcours = false
    }

    BackHandler(enabled = ecran != Ecran.ACCUEIL) {
        aller(if (ecran == Ecran.PROTECTION) Ecran.REGLAGES else Ecran.ACCUEIL)
    }

    // Un site à ouvrir, en attente de l'avertissement (VersUnSite.kt).
    val reglages = remember { Reglages(contexte) }
    var siteDemande by rememberSaveable { mutableStateOf<String?>(null) }
    val ouvrirUnSite: (String) -> Unit = { adresse ->
        if (reglages.nePlusPrevenirPourLesSites) ouvrirDansLeNavigateur(contexte, adresse) else siteDemande = adresse
    }
    siteDemande?.let { adresse ->
        AvertissementSite(adresse, nePlusPrevenir = { if (it) reglages.nePlusPrevenirPourLesSites = true }, fermer = { siteDemande = null })
    }

    CompositionLocalProvider(LocalOuvrirUnSite provides ouvrirUnSite) {

    Column(Modifier.safeDrawingPadding()) {

        /*
         * L'en-tête, sur chaque écran : le menu ☰ (ce qui n'est pas une des
         * quatre entrées), la marque, et « Quitter vite » comme le ✕ du site.
         */
        EnTete(
            quitterVite,
            menu = listOf(
                "Accueil" to { aller(Ecran.ACCUEIL) },
                "Réglages" to { aller(Ecran.REGLAGES) },
                "Mettre à jour le contenu" to { depot.synchroniser(demandee = true); aller(Ecran.REGLAGES) },
                "Ce qui est protégé, ce qui ne l'est pas" to { aller(Ecran.PROTECTION) },
                "Le didacticiel" to { aller(Ecran.DIDACTICIEL) },
                "Contribuer, sur le site" to { ouvrirUnSite("$SITE/contribuer") },
            ),
        )

        val c = catalogue
        when {
            c == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            ecran == Ecran.ACCUEIL -> Accueil(
                catalogue = c,
                depot = depot,
                aDemander = aDemander,
                ouvrir = { e -> aller(Ecran.valueOf(e.name)) },
                ouvrirViolentometre = { aller(Ecran.POINT); ficheAOuvrir = it },
            )
            ecran == Ecran.AIDE -> Aides(c.aides)
            ecran == Ecran.RECITS -> Recits(c.recits, depot, garderLesBrouillons = garder, ouvert = recitOuvert)
            ecran == Ecran.POINT -> FaireLePoint(
                c, depot, garder,
                ouvrirRecit = { recitOuvert = it; ecran = Ecran.RECITS },
                cibleInitiale = pointCible,
                ficheInitiale = ficheAOuvrir,
                // Venu d'une étape de parcours : le retour y ramène, à
                // l'étape où l'on en était.
                retour = if (pointDepuisParcours) ({ pointDepuisParcours = false; pointCible = null; ecran = Ecran.COMPRENDRE }) else null,
            )
            ecran == Ecran.COMPRENDRE -> Comprendre(
                c, depot,
                ouvert = parcoursOuvert, ouvrir = { parcoursOuvert = it; etapeOuverte = -1 },
                etape = etapeOuverte, allerA = { etapeOuverte = it },
                faireLePoint = { pointCible = it; pointDepuisParcours = true; ecran = Ecran.POINT },
            )
            ecran == Ecran.REGLAGES -> ReglagesEcran(
                deguise = deguise,
                garderLesReponses = garder,
                theme = theme,
                changerTheme = changerTheme,
                changerLeCode = changerLeCode,
                deguiser = deguiser,
                afficherLeVraiNom = montrerLeVraiNom,
                changerGarder = changerGarder,
                voirCeQuiEstProtege = { aller(Ecran.PROTECTION) },
                revoirLeDidacticiel = { aller(Ecran.DIDACTICIEL) },
                miseAJour = { MiseAJour(depot) },
            )
            ecran == Ecran.PROTECTION -> CeQuiEstProtege(deguise)
            ecran == Ecran.DIDACTICIEL -> Didacticiel { aller(Ecran.ACCUEIL) }
        }
    }
    }
}

@Composable
private fun Accueil(
    catalogue: Catalogue,
    depot: Depot,
    aDemander: Boolean,
    ouvrir: (Entree) -> Unit,
    ouvrirViolentometre: (String) -> Unit,
) {

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Connexion facturée au volume : on demande, une fois, sans
        // insister. « Plus tard » ne redemande pas avant trois jours.
        if (aDemander) {
            Encadre(titre = "Une mise à jour du contenu est possible") {
                Text("Vous êtes sur une connexion qui peut être facturée au volume.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { depot.synchroniser(demandee = true) }) { Text("Mettre à jour") }
                    OutlinedButton(onClick = depot::plusTard) { Text("Plus tard") }
                }
            }
        }

        Text("Est-ce que ce que je vis est normal ?", style = MaterialTheme.typography.headlineMedium)

        Text(
            "Certaines situations semblent normales parce qu'on les voit tous les jours. Un violentomètre les met bout à bout, du geste qui va de soi à celui qui met en danger, pour qu'on puisse voir où l'on en est.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Espace(4)

        // Les quatre entrées, dans l'ordre : du plus proche de soi au plus
        // distant.
        for (e in Entree.entries) {
            Card(onClick = { ouvrir(e) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(e.icone, fontSize = 28.sp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(e.titre, style = MaterialTheme.typography.titleMedium)
                        Note(e.sousTitre)
                    }
                    Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Espace(8)

        /*
         * Créer, corriger, discuter : cela se fait sur le site, pas ici
         * (l'application ne modifie rien, sauf un récit déposé). Le lien
         * passe par l'avertissement : le navigateur garde l'adresse.
         */
        val ouvrirUnSite = LocalOuvrirUnSite.current
        Card(onClick = { ouvrirUnSite("$SITE/contribuer") }, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("🖊️", fontSize = 24.sp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Aller plus loin, sur le site", style = MaterialTheme.typography.titleSmall)
                    Note("Créer un violentomètre, proposer une situation, corriger, discuter : cela se fait sur violentometres.fr.")
                }
                Text("↗", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
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

        message?.let { Note(it) }
    }
}
