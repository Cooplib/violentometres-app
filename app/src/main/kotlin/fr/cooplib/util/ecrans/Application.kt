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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fr.cooplib.util.Lanceur
import fr.cooplib.util.Reglages
import fr.cooplib.util.donnees.Catalogue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
fun Application(verrouillee: Boolean, deverrouiller: () -> Unit) {

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
                    changerGarder = { reglages.garderLesReponses = it; garder = it },
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
) {

    // Le catalogue embarqué : 1,1 Mo de JSON, lu hors du fil de
    // l'interface pour que le premier écran ne gèle pas.
    val catalogue by produceState<Catalogue?>(null) {
        value = withContext(Dispatchers.Default) { Catalogue.embarque() }
    }

    var ecran by rememberSaveable { mutableStateOf(Ecran.ACCUEIL) }

    BackHandler(enabled = ecran != Ecran.ACCUEIL) {
        ecran = if (ecran == Ecran.PROTECTION) Ecran.REGLAGES else Ecran.ACCUEIL
    }

    Box(Modifier.safeDrawingPadding()) {
        val c = catalogue
        when {
            c == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            ecran == Ecran.ACCUEIL -> Accueil(
                ouvrir = { e -> ecran = Ecran.valueOf(e.name) },
                reglages = { ecran = Ecran.REGLAGES },
                protection = { ecran = Ecran.PROTECTION },
            )
            ecran == Ecran.AIDE -> Aides(c.aides)
            ecran == Ecran.REGLAGES -> ReglagesEcran(
                deguise = deguise,
                garderLesReponses = garder,
                deguiser = deguiser,
                afficherLeVraiNom = montrerLeVraiNom,
                changerGarder = changerGarder,
                voirCeQuiEstProtege = { ecran = Ecran.PROTECTION },
            )
            ecran == Ecran.PROTECTION -> CeQuiEstProtege(deguise)
            else -> AVenir(Entree.valueOf(ecran.name))
        }
    }
}

@Composable
private fun Accueil(ouvrir: (Entree) -> Unit, reglages: () -> Unit, protection: () -> Unit) {

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

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
