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

private val PUBLICS = listOf(
    "personne" to "Pour moi : je réponds sur ce que je vis.",
    "proche" to "Pour quelqu'un que je connais : je réponds sur ce que j'observe.",
)

private const val GENERAL = "orientation"

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

    BackHandler(enabled = cible != null || fiche != null || retour != null) {
        when {
            // Le point ouvert depuis une étape de parcours : on y retourne.
            retour != null && cible == cibleInitiale -> retour()
            cible != null -> cible = null
            fiche != null -> fiche = null
            else -> retour?.invoke()
        }
    }

    val c = cible
    val pool = when (c) {
        null -> null
        GENERAL -> catalogue.pointGeneral
        else -> catalogue.pointsParViolentometre[c]
    }
    val vm = fiche?.let { f -> catalogue.violentometres.find { it.id == f } }

    when {
        c != null && pool != null -> androidx.compose.runtime.key(c) {
            // Une clé par cible : changer de point repart de son propre état.
            Deroulement(pool, c, garder, ouvrirRecit, autrePoint = { cible = it })
        }
        vm != null -> androidx.compose.runtime.key(vm.id) {
            FicheViolentometre(vm, catalogue, depot, faireLePoint = { cible = it }, ouvrir = { fiche = it })
        }
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
    ouvrirRecit: (String) -> Unit,
    autrePoint: (String) -> Unit,
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
    val seuil = largeur / 3

    Box(
        Modifier.fillMaxSize().then(
            if (question == null) Modifier
            else Modifier.pointerInput(cleQuestion) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        portee.launch {
                            val v = decalage.value
                            when {
                                v <= -seuil -> { decalage.animateTo(-largeur * 1.4f); avancer(d.repondre(etat, question, Reponse.OUI, maintenant())) }
                                v >= seuil -> { decalage.animateTo(largeur * 1.4f); avancer(d.repondre(etat, question, Reponse.NON, maintenant())) }
                                else -> decalage.animateTo(0f)
                            }
                        }
                    },
                    onDragCancel = { portee.launch { decalage.animateTo(0f) } },
                    onHorizontalDrag = { change, delta ->
                        change.consume()
                        portee.launch { decalage.snapTo(decalage.value + delta) }
                    },
                )
            }
        )
    ) { Page {

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

                Text("Vous répondez…", style = MaterialTheme.typography.titleSmall)
                for ((cleP, titre) in PUBLICS) {
                    Row(Modifier.fillMaxWidth().clickable { avancer(d.choisirPublic(etat, cleP)) }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = etat.public == cleP, onClick = { avancer(d.choisirPublic(etat, cleP)) })
                        Text(titre)
                    }
                }
                Button(onClick = { avancer(d.commencer(etat)) }, enabled = etat.public != null, modifier = Modifier.fillMaxWidth()) { Text("Commencer") }

                if (etat.finiLe != null) {
                    Text("Vous aviez déjà fait ce point sur ce téléphone.")
                    OutlinedButton(onClick = { avancer(etat.copy(phase = Phase.RESULTAT)) }) { Text("Revoir le résultat") }
                }
            }

            Phase.QUESTIONS -> {
                val niveau = d.niveauCourant(etat)
                val situation = d.questionCourante(etat)
                if (niveau == null || situation == null) {
                    // Le contenu a changé depuis la pause.
                    Text("Ce point a changé depuis votre dernière visite.")
                    Button(onClick = ::recommencer) { Text("Recommencer") }
                    return@Page
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
                // La situation, en grand, dans sa carte (`.test__situation`),
                // qu'on peut aussi glisser : à gauche oui, à droite non.
                CarteQuiSuit(texte = situation.texte, bordure = couleur(niveau), decalage = decalage.value, seuil = seuil)
                Note("Glissez n'importe où : à gauche pour oui, à droite pour non. Ou touchez les boutons.")
                if (situation.sources.size > 1) Text("Dans : ${situation.sources.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                etat.reponses[situation.id]?.let { r ->
                    Text("Vous aviez répondu : ${mapOf(Reponse.OUI to "oui", Reponse.NON to "non", Reponse.PASSER to "passé")[r]}. Vous pouvez changer.", style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    // Comme sur le site : « Oui » plein, « Non » bordé, « Passer »
                    // en simple lien.
                    Button(onClick = { avancer(d.repondre(etat, situation, Reponse.OUI, maintenant())) }, Modifier.weight(1f)) { Text("Oui", fontSize = 17.sp) }
                    OutlinedButton(onClick = { avancer(d.repondre(etat, situation, Reponse.NON, maintenant())) }, Modifier.weight(1f)) { Text("Non", fontSize = 17.sp) }
                }
                TextButton(onClick = { avancer(d.repondre(etat, situation, Reponse.PASSER, maintenant())) }, Modifier.fillMaxWidth()) {
                    Text("Passer", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { d.precedent(etat)?.let(::avancer) }, enabled = etat.niveau > 0 || etat.index > 0) { Text("← Question précédente") }
                Actions(pause = { avancer(d.pause(etat)) }, arreter = { avancer(d.terminer(etat, maintenant())) })
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
                Text("Ce que vous venez de reconnaître est grave.", style = MaterialTheme.typography.headlineSmall)
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

            Phase.RESULTAT -> Resultat(pool, d, etat, garder, recommencer = ::recommencer, ouvrirRecit = ouvrirRecit, autrePoint = autrePoint)
        }
    } }
    }
}

@Composable
private fun Resultat(
    pool: PoolDuPoint,
    d: Deroule,
    etat: EtatDuPoint,
    garder: Boolean,
    recommencer: () -> Unit,
    ouvrirRecit: (String) -> Unit,
    autrePoint: (String) -> Unit,
) {

    val reconnues = d.situationsReconnues(etat.reponses)
    val atteint = Deroule.niveauAtteint(reconnues)?.let { a -> pool.niveaux.find { it.position == a } }
    val sansPositif = reconnues.none { it.gravite == 0 } && d.situationsParNiveau[0].orEmpty().isNotEmpty()
    val formes = Deroule.formesReconnues(reconnues)
    val aides = d.aidesDuResultat(etat)

    Text("Où vous en êtes", style = MaterialTheme.typography.headlineSmall)
    Text("${pluriel(etat.reponses.size, "réponse")} sur ${pool.situations.size}", style = MaterialTheme.typography.bodySmall)

    // La jauge du site : une case par niveau, allumée jusqu'au niveau
    // atteint, la case atteinte cerclée.
    Jauge(pool.niveaux, atteint?.position)

    Text(
        if (atteint != null) "Le niveau le plus élevé que vous avez reconnu : ${atteint.label}."
        else "Vous n'avez reconnu aucune situation.",
        style = MaterialTheme.typography.titleMedium,
    )
    if (atteint != null) Note("Une seule situation à ce niveau suffit : ce n'est pas une moyenne.")

    if (sansPositif) Encadre("Vous n'avez reconnu aucune situation positive. C'est en soi un signal.")

    if (formes.isNotEmpty()) {
        val liste = formes.map { (cle, n) -> "${(LIBELLES_FORMES[cle] ?: cle).lowercase()} ($n)" }
        val phrase = if (liste.size == 1) liste[0] else liste.dropLast(1).joinToString(", ") + " et " + liste.last()
        Text("Ce que vous avez reconnu porte surtout sur $phrase.")
    }

    for ((niveau, situations) in d.reconnuesParNiveau(reconnues)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EtiquetteNiveau(niveau)
            Text("${situations.size}")
        }
        situations.forEach { Text("· ${it.texte}") }
    }

    if (aides.isNotEmpty()) {
        Text("🆘 Quoi faire, qui contacter", style = MaterialTheme.typography.titleMedium)
        if (d.aidesReconnues(reconnues).isNotEmpty()) Text("Les premières sont liées à ce que vous avez reconnu.", style = MaterialTheme.typography.bodySmall)
        aides.forEach { CarteAide(it) }
    }

    EtMaintenant(pool, reconnues, ouvrirRecit, autrePoint)

    OutlinedButton(onClick = recommencer, Modifier.fillMaxWidth()) { Text("Effacer mes réponses") }

    Encadre(
        "Rien de ce point n'a été envoyé : ni vos réponses, ni ce résultat. " +
            if (garder) "Ils restent sur ce téléphone jusqu'à ce que vous les effaciez."
            else "Ils disparaissent quand vous quittez l'application."
    )
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
