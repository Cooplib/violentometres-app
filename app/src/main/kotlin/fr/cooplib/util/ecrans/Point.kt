package fr.cooplib.util.ecrans

import android.icu.text.Collator
import android.icu.util.ULocale
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.cooplib.util.donnees.Catalogue
import fr.cooplib.util.modeles.Niveau
import fr.cooplib.util.modeles.PoolDuPoint
import fr.cooplib.util.modeles.decodage
import fr.cooplib.util.point.Deroule
import fr.cooplib.util.point.EtatDuPoint
import fr.cooplib.util.point.Phase
import fr.cooplib.util.point.Reponse
import fr.cooplib.util.point.recommander
import fr.cooplib.util.stockage.Reponses

/*
 * « Faire le point » : on coche ce qu'on vit, on voit où on se situe.
 *
 * Le déroulé, les aides et les recommandations sont dans le noyau
 * (point/Deroule.kt, point/Recommandations.kt), PORTÉS du site et
 * comparés à lui action par action. Ce fichier ne fait que les
 * afficher : aucune décision du point ne se prend ici, sinon le même
 * questionnaire donnerait deux résultats selon l'appareil.
 *
 * Les textes sont ceux de pages/Test.jsx, sauf là où un téléphone n'est
 * pas un navigateur (ce qui est gardé, « Quitter vite »).
 *
 * LES RÉPONSES NE SORTENT PAS DE L'APPAREIL, et l'écran le dit, au
 * début et à la fin. Pas même « un point a été fait » : le site le
 * compte (`/done`), l'application non.
 */

// Même liste que le site (pages/Test.jsx) et le serveur (models/forme.py).
private val LIBELLES_FORMES = mapOf(
    "verbale" to "Les violences verbales",
    "psychologique" to "Les violences psychologiques",
    "controle" to "Le contrôle et l'isolement",
    "economique" to "Les violences économiques",
    "numerique" to "Les violences numériques",
    "physique" to "Les violences physiques",
    "sexuelle" to "Les violences sexuelles",
)

private const val GENERAL = "orientation"

// La cible d'un point sur un parcours entier : « parcours:<id> ».
const val PARCOURS = "parcours:"

private val sauvegardeEtat: Saver<EtatDuPoint, String> = Saver(
    save = { decodage.encodeToString(EtatDuPoint.serializer(), it) },
    restore = { decodage.decodeFromString(EtatDuPoint.serializer(), it) },
)

@Composable
fun FaireLePoint(
    catalogue: Catalogue,
    depot: fr.cooplib.util.stockage.Depot,
    garder: Boolean,
    ouvrirRecit: (String) -> Unit,
    cibleInitiale: String? = null,
    ficheInitiale: String? = null,
    // Venu d'ailleurs (une étape de parcours) : où ramène le retour.
    retour: (() -> Unit)? = null,
) {

    // Ce qui est ouvert : la liste (rien), la fiche d'un violentomètre,
    // ou un point en cours. `cibleInitiale` : venu d'ailleurs (« faire le
    // point dessus » depuis une étape de parcours).
    var fiche by rememberSaveable { mutableStateOf(ficheInitiale) }
    var cible by rememberSaveable { mutableStateOf(cibleInitiale) }
    // « Mon violentomètre » ouvert : on y revient en quittant son point.
    var perso by rememberSaveable { mutableStateOf(false) }
    val carnet by depot.carnet.liste.collectAsState()

    BackHandler(enabled = cible != null || fiche != null || perso || retour != null) {
        when {
            // Le point ouvert depuis une étape de parcours : on y retourne.
            retour != null && cible == cibleInitiale -> retour()
            cible != null -> cible = null
            fiche != null -> fiche = null
            perso -> perso = false
            else -> retour?.invoke()
        }
    }

    val c = cible
    // La cible : le point général, un parcours entier (« parcours:… »,
    // comme sur le site), ou un violentomètre.
    val pool = when {
        c == null -> null
        c == GENERAL -> catalogue.pointGeneral
        c.startsWith(PARCOURS) -> catalogue.pointsParParcours[c.removePrefix(PARCOURS)]
        // Construit sur le téléphone, jamais servi par l'API : le noyau en
        // fait un point comme les autres.
        c.startsWith(PERSO) -> carnet.find { it.id == c.removePrefix(PERSO) }
            ?.enPoint(catalogue.pointGeneral.niveaux, catalogue.aides)
        else -> catalogue.pointsParViolentometre[c]
    }
    val vm = fiche?.let { f -> catalogue.violentometres.find { it.id == f } }

    when {
        c != null && pool != null -> androidx.compose.runtime.key(c) {
            // Une clé par cible : changer de point repart de son propre état.
            Deroulement(pool, c, garder, catalogue, depot, ouvrirRecit, autrePoint = { cible = it }, voirViolentometre = { cible = null; fiche = it })
        }
        vm != null -> androidx.compose.runtime.key(vm.id) {
            FicheViolentometre(vm, catalogue, depot, faireLePoint = { cible = it }, ouvrir = { fiche = it })
        }
        perso -> MesViolentometres(catalogue, depot, garder, faireLePoint = { cible = it })
        else -> ListeDesViolentometres(
            catalogue, depot,
            avant = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🧭 Faire le point", style = MaterialTheme.typography.headlineSmall)
                    Card(Modifier.fillMaxWidth(), onClick = { cible = GENERAL }) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Où j'en suis, en général", style = MaterialTheme.typography.titleMedium)
                            Text(catalogue.pointGeneral.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Commencer ›", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Card(Modifier.fillMaxWidth(), onClick = { perso = true }) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("✍️ Mon violentomètre", style = MaterialTheme.typography.titleMedium)
                            Text("Rangez vous-même des situations, puis faites le point dessus. Il reste sur ce téléphone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Construire ›", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text("Ou sur un sujet précis", style = MaterialTheme.typography.titleMedium)
                }
            },
            ouvrir = { fiche = it },
        )
    }
}

@Composable
private fun Deroulement(
    pool: PoolDuPoint,
    cle: String,
    garder: Boolean,
    catalogue: Catalogue,
    depot: fr.cooplib.util.stockage.Depot,
    ouvrirRecit: (String) -> Unit,
    autrePoint: (String) -> Unit,
    voirViolentometre: (String) -> Unit,
) {

    val contexte = LocalContext.current
    val d = remember(pool) { Deroule(pool) }

    // Gardé sur le téléphone seulement si la personne l'a choisi ; sinon,
    // en mémoire, et oublié au reverrouillage.
    val garde = remember(cle) { if (garder) Reponses.lire(contexte, cle) else null }

    var etat by rememberSaveable(stateSaver = sauvegardeEtat) {
        // Un point fini se rouvre sur l'intro, avec le résultat à portée.
        mutableStateOf(garde?.let { if (it.finiLe != null) it.copy(phase = Phase.INTRO) else it } ?: EtatDuPoint())
    }

    // Un point entamé et gardé : reprendre ou recommencer.
    var reprise by rememberSaveable { mutableStateOf(garde != null && garde.phase != Phase.INTRO && garde.finiLe == null) }

    fun avancer(suivant: EtatDuPoint) {
        etat = suivant
        if (garder) Reponses.ecrire(contexte, cle, suivant)
    }

    fun recommencer() {
        Reponses.oublier(contexte, cle)
        reprise = false
        etat = EtatDuPoint()
    }

    val maintenant = { System.currentTimeMillis() }
    val reconnues = d.situationsReconnues(etat.reponses)
    val total = pool.situations.size

    /*
     * La couleur du niveau en cours, plus présente (retour du 7 octobre
     * 2026) : le fond de l'écran en prend une teinte, la carte sa
     * bordure, la progression sa couleur. C'est l'usage même de ces
     * couleurs : dire à quel niveau on est.
     */
    val niveauDeLEcran = when (etat.phase) {
        Phase.QUESTIONS, Phase.SAS -> d.niveauCourant(etat)
        Phase.AIDE -> pool.situations.find { it.id == etat.situationVue }?.let { s -> pool.niveaux.find { it.position == s.gravite } }
        Phase.ALERTE -> pool.niveaux.find { it.position == d.plusGrave }
        else -> null
    }
    val fond = MaterialTheme.colorScheme.background
    val teinte by animateColorAsState(niveauDeLEcran?.let { teinteSur(couleur(it), fond, 0.10f) } ?: fond, label = "teinte")

    /*
     * Le glissement d'une question se fait SUR TOUT L'ÉCRAN, pas seulement
     * sur la carte (retour du 7 octobre) : c'est le fond qui écoute le
     * geste, la carte ne fait que le suivre. À gauche oui, à droite non.
     */
    val question = if (etat.phase == Phase.QUESTIONS && !reprise) d.questionCourante(etat) else null
    val cleQuestion = "${etat.niveau}-${etat.index}-${etat.phase}"
    val decalage = remember(cleQuestion) { Animatable(0f) }
    val portee = rememberCoroutineScope()

    BoxWithConstraints(Modifier.fillMaxSize().background(teinte)) {

    val largeur = constraints.maxWidth.toFloat()
    /*
     * Un geste court suffit (retour du 8 octobre 2026) : il fallait
     * pousser la carte d'un tiers de l'écran. C'est maintenant un
     * sixième, ou un coup de doigt rapide, même bref : ce qu'on fait
     * naturellement.
     */
    val seuil = largeur / 6
    val vitesse = remember(cleQuestion) { VelocityTracker() }
    val coupDeDoigt = 900f

    Box(
        Modifier.fillMaxSize().then(
            if (question == null) Modifier
            else Modifier.pointerInput(cleQuestion) {
                detectHorizontalDragGestures(
                    onDragStart = { vitesse.resetTracking() },
                    onDragEnd = {
                        val vx = vitesse.calculateVelocity().x
                        portee.launch {
                            val v = decalage.value
                            when {
                                v <= -seuil || (vx <= -coupDeDoigt && v < 0) -> { decalage.animateTo(-largeur * 1.4f); avancer(d.repondre(etat, question, Reponse.OUI, maintenant())) }
                                v >= seuil || (vx >= coupDeDoigt && v > 0) -> { decalage.animateTo(largeur * 1.4f); avancer(d.repondre(etat, question, Reponse.NON, maintenant())) }
                                else -> decalage.animateTo(0f)
                            }
                        }
                    },
                    onDragCancel = { portee.launch { decalage.animateTo(0f) } },
                    onHorizontalDrag = { change, delta ->
                        change.consume()
                        vitesse.addPosition(change.uptimeMillis, change.position)
                        portee.launch { decalage.snapTo(decalage.value + delta) }
                    },
                )
            }
        )
    ) {

    val niveauQuestion = d.niveauCourant(etat)

    when {

        question != null && niveauQuestion != null -> EcranQuestion(
            pool = pool, d = d, etat = etat, total = total,
            niveau = niveauQuestion, situation = question,
            decalage = decalage.value, seuil = seuil,
            avancer = ::avancer, maintenant = maintenant,
        )

        etat.phase == Phase.RESULTAT && !reprise -> Bilan(
            pool, d, etat, garder, catalogue, depot,
            recommencer = ::recommencer, ouvrirRecit = ouvrirRecit, autrePoint = autrePoint,
            voirViolentometre = voirViolentometre.takeIf { cle != GENERAL }?.let { v -> { v(cle) } },
            // Arrêté en route : reprendre là où l'on en était, s'il reste de quoi.
            reprendre = if (d.peutReprendre(etat)) ({ avancer(d.reprendreLePoint(etat)) }) else null,
        )

        else -> Page {

        if (reprise) {
            Text("Je fais le point", style = MaterialTheme.typography.headlineSmall)
            Text("Vous aviez commencé ce point sur ce téléphone : ${pluriel(etat.reponses.size, "réponse")} sur $total.")
            Button(onClick = { reprise = false; if (etat.phase == Phase.PAUSE) avancer(d.reprendre(etat)) }, Modifier.fillMaxWidth()) {
                Text("Reprendre où j'en étais")
            }
            OutlinedButton(onClick = ::recommencer, Modifier.fillMaxWidth()) { Text("Recommencer") }
            return@Page
        }

        // Ce sur quoi porte le point, rappelé à chaque étape, comme le
        // cadre du site (`.test__frame`) : un filet à gauche, le titre.
        if (etat.phase != Phase.INTRO) {
            Row(Modifier.height(IntrinsicSize.Min)) {
                Box(Modifier.width(4.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp)))
                Column(Modifier.padding(start = 10.dp)) {
                    Note("Je fais le point")
                    Text(pool.titre, style = MaterialTheme.typography.titleSmall)
                }
            }
        }

        when (etat.phase) {

            Phase.INTRO -> {
                Text("Je fais le point", style = MaterialTheme.typography.headlineSmall)
                Text(pool.titre, style = MaterialTheme.typography.titleMedium)
                Text("Une situation à la fois, $total en tout, du plus léger au plus grave. Vous répondez oui, non, ou vous passez.", style = MaterialTheme.typography.bodyLarge)
                Encadre(titre = "Rien n'est envoyé") {
                    Text(
                        "Vos réponses ne sortent pas de ce téléphone. " +
                            if (garder) "Elles y sont gardées pour reprendre plus tard ; vous pouvez les effacer à tout moment."
                            else "Elles ne sont pas gardées : en quittant l'application, elles disparaissent."
                    )
                }
                Encadre("Ce n'est pas un diagnostic. C'est un repère, pour mettre des mots.")
                Note("Vous pouvez faire une pause ou vous arrêter quand vous voulez ; les aides sont là à chaque étape. « Quitter vite », en haut, efface vos réponses et ferme l'application.")

                Button(onClick = { avancer(d.commencer(etat)) }, modifier = Modifier.fillMaxWidth()) { Text("Commencer") }

                if (etat.finiLe != null) {
                    Text("Vous aviez déjà fait ce point sur ce téléphone.")
                    OutlinedButton(onClick = { avancer(etat.copy(phase = Phase.RESULTAT)) }) { Text("Revoir le résultat") }
                }
            }

            // Les questions et le bilan ont leur propre écran, plus bas ;
            // ici, seulement le cas où le contenu a changé depuis la pause.
            Phase.QUESTIONS -> {
                Text("Ce point a changé depuis votre dernière visite.")
                Button(onClick = ::recommencer) { Text("Recommencer") }
            }

            Phase.SAS -> {
                val niveau = d.niveauCourant(etat)!!
                val dernier = niveau.position == d.plusGrave
                val nb = d.situationsCourantes(etat).size
                val aides = d.aidesReconnues(reconnues)
                Text("${pluriel(reconnues.size, "situation")} ${if (reconnues.size > 1) "reconnues" else "reconnue"} jusqu'ici", style = MaterialTheme.typography.bodySmall)
                EtiquetteNiveau(niveau)
                Text(if (dernier) "Le niveau le plus grave" else "Niveau suivant : ${niveau.label}", style = MaterialTheme.typography.headlineSmall)
                Text(
                    if (dernier) "« ${niveau.label} » rassemble ${if (nb > 1) "les $nb situations les plus dures. Vous pouvez les lire" else "la situation la plus dure. Vous pouvez la lire"}, ou vous arrêter ici : ce que vous avez déjà reconnu suffit à voir où vous en êtes."
                    else "« ${niveau.label} » : ${pluriel(nb, "situation")}. Continuer ?"
                )
                if (aides.isNotEmpty()) {
                    Text("🆘 ${pluriel(aides.size, "aide")} pour ce que vous avez reconnu", style = MaterialTheme.typography.titleSmall)
                    aides.forEach { CarteAide(it) }
                }
                Button(onClick = { avancer(d.reprendreApresSas(etat)) }, Modifier.fillMaxWidth()) { Text("Continuer") }
                if (reconnues.any { it.gravite == 0 }) {
                    OutlinedButton(onClick = { avancer(d.revoirLesPositifs(etat)) }, Modifier.fillMaxWidth()) { Text("Revoir ce qui va bien") }
                }
                Actions(pause = { avancer(d.pause(etat)) }, arreter = { avancer(d.terminer(etat, maintenant())) })
            }

            Phase.POSITIFS -> {
                Text("Ce qui va bien", style = MaterialTheme.typography.headlineSmall)
                Text("Vous avez reconnu ces situations positives :")
                reconnues.filter { it.gravite == 0 }.forEach { Text("· ${it.texte}") }
                Button(onClick = { avancer(d.revenirAuSas(etat)) }, Modifier.fillMaxWidth()) { Text("Revenir au point") }
                TextButton(onClick = { avancer(d.terminer(etat, maintenant())) }) { Text("Arrêter et voir où j'en suis") }
            }

            Phase.AIDE -> {
                Text("Une aide pour ce que vous venez de reconnaître", style = MaterialTheme.typography.headlineSmall)
                pool.situations.find { it.id == etat.situationVue }?.let { Text(it.texte, style = MaterialTheme.typography.bodyLarge) }
                d.aidesLiees(etat.situationVue).forEach { CarteAide(it) }
                Text("Vous la retrouverez dans le résultat, avec les autres.", style = MaterialTheme.typography.bodySmall)
                Button(onClick = { avancer(d.apresAlerte(etat, maintenant())) }, Modifier.fillMaxWidth()) { Text("Continuer") }
                Actions(pause = { avancer(d.pause(etat)) }, arreter = { avancer(d.terminer(etat, maintenant())) })
            }

            Phase.ALERTE -> {
                // Pas « c'est grave » : ce que la situation peut faire, et
                // maintenant (retour du 8 octobre 2026).
                Text(
                    if (etat.public == "proche") "Cette situation peut mettre cette personne en danger."
                    else "Cette situation peut vous mettre en danger.",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Encadre("Vous n'êtes pas obligé·e d'aller plus loin. Voici qui peut vous aider, maintenant.", alerte = true)
                d.aidesDAlerte(etat).forEach { CarteAide(it) }
                Button(onClick = { avancer(d.terminer(etat, maintenant())) }, Modifier.fillMaxWidth()) { Text("Arrêter ici et voir où j'en suis") }
                OutlinedButton(onClick = { avancer(d.apresAlerte(etat, maintenant())) }, Modifier.fillMaxWidth()) { Text("Continuer le point") }
            }

            Phase.PAUSE -> {
                Text("Pause", style = MaterialTheme.typography.headlineSmall)
                Encadre(
                    if (garder) "Vos réponses sont gardées sur ce téléphone. Revenez quand vous voulez : ce point vous proposera de reprendre."
                    else "Vos réponses restent tant que l'application est ouverte. Si vous la quittez, elles disparaissent."
                )
                Button(onClick = { avancer(d.reprendre(etat)) }, Modifier.fillMaxWidth()) { Text("Reprendre") }
                TextButton(onClick = { avancer(d.terminer(etat, maintenant())) }) { Text("Arrêter et voir où j'en suis") }
            }

            Phase.RESULTAT -> Unit
        }
    }
    }
    }
    }
}

/*
 * Une question, sur son propre écran. Les boutons sont CALÉS EN BAS, au
 * même endroit quelle que soit la longueur de la situation : avant, ils
 * montaient ou descendaient avec le texte (retour du 8 octobre 2026). Ce
 * qui précède défile si la situation est longue.
 */
@Composable
private fun EcranQuestion(
    pool: PoolDuPoint,
    d: Deroule,
    etat: EtatDuPoint,
    total: Int,
    niveau: Niveau,
    situation: fr.cooplib.util.modeles.SituationDuPoint,
    decalage: Float,
    seuil: Float,
    avancer: (EtatDuPoint) -> Unit,
    maintenant: () -> Long,
) {
    Column(Modifier.fillMaxSize()) {

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.height(IntrinsicSize.Min)) {
                Box(Modifier.width(4.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp)))
                Column(Modifier.padding(start = 10.dp)) {
                    Note("Je fais le point")
                    Text(pool.titre, style = MaterialTheme.typography.titleSmall)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EtiquetteNiveau(niveau)
                Text("${etat.index + 1} / ${d.situationsCourantes(etat).size} · ${etat.reponses.size} sur $total au total", style = MaterialTheme.typography.bodySmall)
            }
            // La progression du point entier, à la couleur du niveau.
            LinearProgressIndicator(
                progress = { etat.reponses.size.toFloat() / total.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth(),
                color = couleur(niveau),
                trackColor = couleur(niveau).copy(alpha = 0.2f),
            )
            Text(if (etat.public == "proche") "Vous observez ceci ?" else "Vous vivez ceci ?", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            // La situation, en grand, dans sa carte (`.test__situation`) ; elle
            // suit le geste fait n'importe où sur l'écran.
            CarteQuiSuit(texte = situation.texte, bordure = couleur(niveau), decalage = decalage, seuil = seuil)
            if (situation.sources.size > 1) Note("Dans : ${situation.sources.joinToString(", ")}")
            etat.reponses[situation.id]?.let { r ->
                Note("Vous aviez répondu : ${mapOf(Reponse.OUI to "oui", Reponse.NON to "non", Reponse.PASSER to "passé")[r]}. Vous pouvez changer.")
            }
        }

        // Le bas de l'écran : toujours au même endroit.
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Note("Glissez n'importe où : à gauche pour oui, à droite pour non.", Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                // Comme sur le site : « Oui » plein, « Non » bordé.
                Button(onClick = { avancer(d.repondre(etat, situation, Reponse.OUI, maintenant())) }, Modifier.weight(1f)) { Text("Oui", fontSize = 18.sp) }
                OutlinedButton(onClick = { avancer(d.repondre(etat, situation, Reponse.NON, maintenant())) }, Modifier.weight(1f)) { Text("Non", fontSize = 18.sp) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { d.precedent(etat)?.let(avancer) }, enabled = etat.niveau > 0 || etat.index > 0) { Text("← Précédente") }
                TextButton(onClick = { avancer(d.repondre(etat, situation, Reponse.PASSER, maintenant())) }) {
                    Text("Passer", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { avancer(d.pause(etat)) }) { Text("Faire une pause") }
                TextButton(onClick = { avancer(d.terminer(etat, maintenant())) }) { Text("Arrêter et voir où j'en suis") }
            }
        }
    }
}

/*
 * Le bilan, EN PLUSIEURS ÉCRANS qu'on fait glisser (retour du 8 octobre
 * 2026) : tout sur une seule page faisait un long défilement où rien ne
 * ressortait. Les petits points en bas disent combien il y en a, et y
 * mènent directement. Un écran sans rien à dire n'apparaît pas.
 */
@Composable
private fun Bilan(
    pool: PoolDuPoint,
    d: Deroule,
    etat: EtatDuPoint,
    garder: Boolean,
    catalogue: Catalogue,
    depot: fr.cooplib.util.stockage.Depot,
    recommencer: () -> Unit,
    ouvrirRecit: (String) -> Unit,
    autrePoint: (String) -> Unit,
    voirViolentometre: (() -> Unit)?,
    reprendre: (() -> Unit)?,
) {

    val reconnues = d.situationsReconnues(etat.reponses)
    val aides = d.aidesDuResultat(etat)

    // Sur le premier écran comme sur le dernier : le point n'est pas fini,
    // on peut y retourner (retour du 8 octobre 2026).
    val boutonReprendre: @Composable () -> Unit = {
        if (reprendre != null) {
            Encadre(titre = "Vous vous étiez arrêté·e en route") {
                Text("Ce bilan porte sur ce que vous avez déjà répondu. Vous pouvez reprendre le point là où vous l'aviez laissé.")
                Button(onClick = reprendre, Modifier.fillMaxWidth()) { Text("Reprendre où j'en étais") }
            }
        }
    }
    val recos = remember(pool, reconnues) { recommander(pool, reconnues, ordreFrancais) }
    val aRecommander = recos.violentometres.isNotEmpty() || recos.parcours.isNotEmpty() || recos.recits.isNotEmpty() || recos.mecanismes.isNotEmpty()
    // Le point portait sur un violentomètre : y revenir, et ce qui lui ressemble.
    val vm = catalogue.violentometres.find { it.id == pool.id && pool.type == "violentometer" }
    val proches = vm?.let { v ->
        catalogue.proches[v.id]?.violentometres.orEmpty()
            .mapNotNull { p -> catalogue.violentometres.find { it.id == p.violentometre.id }?.let { it to p } }
    }.orEmpty()

    val ecrans = buildList<Pair<String, @Composable () -> Unit>> {
        add("Où vous en êtes" to { OuVousEnEtes(pool, d, etat, reconnues); boutonReprendre() })
        if (reconnues.isNotEmpty()) add("Ce que vous avez reconnu" to { CeQueVousAvezReconnu(d, reconnues) })
        if (aides.isNotEmpty()) add("Qui contacter" to {
            Text("🆘 Quoi faire, qui contacter", style = MaterialTheme.typography.headlineSmall)
            if (d.aidesReconnues(reconnues).isNotEmpty()) Note("Les premières sont liées à ce que vous avez reconnu.")
            aides.forEach { CarteAide(it) }
        })
        if (aRecommander || vm != null) add("Et maintenant" to {
            if (vm != null && voirViolentometre != null) {
                Text("Et maintenant ?", style = MaterialTheme.typography.headlineSmall)
                OutlinedButton(onClick = voirViolentometre, Modifier.fillMaxWidth()) { Text("↩ Revoir « ${vm.titre} »") }
            }
            EtMaintenant(pool, reconnues, ouvrirRecit, autrePoint)
            if (proches.isNotEmpty()) {
                Text("📊 Dans le même genre", style = MaterialTheme.typography.titleMedium)
                val souvenirs by depot.memoire.etat.collectAsState()
                for ((autre, p) in proches.take(5)) {
                    CarteViolentometre(autre, autre.id in souvenirs.aimes, souvenirs.comptes[autre.id] ?: autre.aime, raison(p)) { autrePoint(autre.id) }
                }
            }
        })
        add("Vos réponses" to {
            Text("Vos réponses", style = MaterialTheme.typography.headlineSmall)
            Encadre(
                titre = "Rien n'a été envoyé",
                texte = "Ni vos réponses, ni ce résultat. " +
                    if (garder) "Ils restent sur ce téléphone jusqu'à ce que vous les effaciez."
                    else "Ils disparaissent quand vous quittez l'application.",
            )
            boutonReprendre()
            OutlinedButton(onClick = recommencer, Modifier.fillMaxWidth()) { Text("Effacer mes réponses et recommencer") }
        })
    }

    val etatPages = rememberPagerState { ecrans.size }
    val portee = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        HorizontalPager(state = etatPages, modifier = Modifier.weight(1f)) { page ->
            Box(Modifier.fillMaxSize()) { Page { ecrans[page].second() } }
        }
        Note("${ecrans[etatPages.currentPage].first} · glissez pour la suite", Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        Points(ecrans.size, etatPages.currentPage, { portee.launch { etatPages.animateScrollToPage(it) } }, Modifier.padding(bottom = 6.dp))
    }
}

@Composable
private fun OuVousEnEtes(pool: PoolDuPoint, d: Deroule, etat: EtatDuPoint, reconnues: List<fr.cooplib.util.modeles.SituationDuPoint>) {

    val atteint = Deroule.niveauAtteint(reconnues)?.let { a -> pool.niveaux.find { it.position == a } }
    val sansPositif = reconnues.none { it.gravite == 0 } && d.situationsParNiveau[0].orEmpty().isNotEmpty()
    val formes = Deroule.formesReconnues(reconnues)

    Text("Où vous en êtes", style = MaterialTheme.typography.headlineSmall)
    Note("${pluriel(etat.reponses.size, "réponse")} sur ${pool.situations.size} · ${pool.titre}")

    // La jauge du site : une case par niveau, allumée jusqu'au niveau
    // atteint, la case atteinte cerclée.
    Jauge(pool.niveaux, atteint?.position)

    Text(
        if (atteint != null) "Le niveau le plus élevé que vous avez reconnu : ${atteint.label}."
        else "Vous n'avez reconnu aucune situation.",
        style = MaterialTheme.typography.titleLarge,
    )
    if (atteint != null) Note("Une seule situation à ce niveau suffit : ce n'est pas une moyenne.")

    if (sansPositif) Encadre("Vous n'avez reconnu aucune situation positive. C'est en soi un signal.")

    if (formes.isNotEmpty()) {
        val liste = formes.map { (cle, n) -> "${(LIBELLES_FORMES[cle] ?: cle).lowercase()} ($n)" }
        val phrase = if (liste.size == 1) liste[0] else liste.dropLast(1).joinToString(", ") + " et " + liste.last()
        Encadre("Ce que vous avez reconnu porte surtout sur $phrase.")
    }
}

/*
 * Ce qui a été reconnu, plus lisible qu'une liste à puces : une case par
 * situation, teintée de la couleur de son niveau, groupées sous
 * l'étiquette du niveau.
 */
@Composable
private fun CeQueVousAvezReconnu(d: Deroule, reconnues: List<fr.cooplib.util.modeles.SituationDuPoint>) {
    val fond = MaterialTheme.colorScheme.surface
    Text("Ce que vous avez reconnu", style = MaterialTheme.typography.headlineSmall)
    for ((niveau, situations) in d.reconnuesParNiveau(reconnues).reversed()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EtiquetteNiveau(niveau)
            Note(pluriel(situations.size, "situation"))
        }
        for (s in situations) {
            Surface(Modifier.fillMaxWidth(), shape = Charte.ArrondiMoyen, color = teinteSur(couleur(niveau), fond, 0.14f)) {
                Row(Modifier.height(IntrinsicSize.Min)) {
                    Box(Modifier.width(5.dp).fillMaxHeight().background(couleur(niveau)))
                    Text(s.texte, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Espace(4)
    }
}

/*
 * Ce qu'on peut lire ensuite, calculé sur le téléphone (Recommandations).
 * Silencieux tant que le point ne porte pas la matière : un point sur un
 * seul violentomètre n'a rien à rapprocher.
 */
@Composable
private fun EtMaintenant(
    pool: PoolDuPoint,
    reconnues: List<fr.cooplib.util.modeles.SituationDuPoint>,
    ouvrirRecit: (String) -> Unit,
    autrePoint: (String) -> Unit,
) {

    val r = remember(pool, reconnues) { recommander(pool, reconnues, ordreFrancais) }

    if (r.violentometres.isEmpty() && r.parcours.isEmpty() && r.recits.isEmpty() && r.mecanismes.isEmpty()) return

    fun enCommun(n: Int) = "En commun : ${pluriel(n, "situation")}"

    Text("Et maintenant ?", style = MaterialTheme.typography.titleLarge)
    Text(
        "Ce qui suit vient de ce que vous venez de cocher, rapproché du reste du contenu sur ce téléphone. Ce n'est pas un classement : c'est ce qui a le plus de choses en commun avec vos réponses.",
        style = MaterialTheme.typography.bodySmall,
    )

    if (r.mecanismes.isNotEmpty()) {
        Text("🔗 Comprendre d'où ça vient", style = MaterialTheme.typography.titleMedium)
        Text("Des situations très différentes font parfois la même chose. Voici ce que font le plus souvent celles que vous avez reconnues.", style = MaterialTheme.typography.bodySmall)
        for (m in r.mecanismes) {
            Text(m.element.nom, style = MaterialTheme.typography.titleSmall)
            if (m.element.resume.isNotBlank()) Text(m.element.resume, style = MaterialTheme.typography.bodyMedium)
            Text(enCommun(m.recoupements), style = MaterialTheme.typography.bodySmall)
        }
    }

    if (r.parcours.isNotEmpty()) {
        Text("🧭 Un tour d'horizon", style = MaterialTheme.typography.titleMedium)
        for (p in r.parcours) {
            Text(p.element.titre, style = MaterialTheme.typography.titleSmall)
            if (p.element.description.isNotBlank()) Text(p.element.description, style = MaterialTheme.typography.bodyMedium)
        }
        Text("Dans « Comprendre », depuis l'accueil.", style = MaterialTheme.typography.bodySmall)
    }

    if (r.violentometres.isNotEmpty()) {
        Text("📊 Les échelles qui vous ressemblent le plus", style = MaterialTheme.typography.titleMedium)
        for (v in r.violentometres) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(v.element.titre, style = MaterialTheme.typography.titleSmall)
                    Text(enCommun(v.recoupements) + v.element.contextes.joinToString("") { " · $it" }, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { autrePoint(v.element.id) }) { Text("🧭 Faire le point dessus") }
                }
            }
        }
    }

    if (r.recits.isNotEmpty()) {
        Text("📖 Quelqu'un l'a vécu, et l'a raconté", style = MaterialTheme.typography.titleMedium)
        for (rc in r.recits) {
            TextButton(onClick = { ouvrirRecit(rc.element.id) }) { Text("${rc.element.titre} · ${enCommun(rc.recoupements)}") }
        }
    }
}

@Composable
private fun Actions(pause: () -> Unit, arreter: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = pause) { Text("Faire une pause") }
        TextButton(onClick = arreter) { Text("Arrêter et voir où j'en suis") }
    }
}

private fun pluriel(n: Int, mot: String) = "$n $mot${if (n > 1) "s" else ""}"

/*
 * La carte d'une situation, qui SUIT le geste fait sur tout l'écran
 * (Deroulement écoute, elle se déplace) : elle penche un peu, et dit
 * « Oui » ou « Non » de plus en plus nettement ; passé un tiers de la
 * largeur, elle part et la réponse est donnée, sinon elle revient. À
 * gauche oui, à droite non, le sens demandé par Cooplib. Les boutons
 * restent : un geste qu'on ne connaît pas ne doit jamais être le seul
 * chemin.
 */
@Composable
private fun CarteQuiSuit(texte: String, bordure: Color, decalage: Float, seuil: Float) {

    val part = (decalage / seuil).coerceIn(-1f, 1f)

    Card(
        Modifier
            .fillMaxWidth()
            .offset { IntOffset(decalage.roundToInt(), 0) }
            .graphicsLayer { rotationZ = part * 6f },
        couleurDeBordure = bordure,
    ) {
        Box {
            Text(
                texte,
                Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal, lineHeight = 29.sp),
            )
            // Ce que le geste va répondre, de plus en plus net.
            if (part != 0f) {
                Text(
                    if (part < 0) "Oui" else "Non",
                    Modifier
                        .align(if (part < 0) Alignment.TopEnd else Alignment.TopStart)
                        .padding(10.dp)
                        .graphicsLayer { alpha = kotlin.math.abs(part) },
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
            }
        }
    }
}
