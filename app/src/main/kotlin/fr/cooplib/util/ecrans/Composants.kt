package fr.cooplib.util.ecrans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Button as BoutonMaterial
import androidx.compose.material3.OutlinedButton as BoutonContourMaterial
import androidx.compose.material3.TextButton as BoutonTexteMaterial

/*
 * Les boutons et les cartes du site (base.css), sous les noms que les
 * écrans emploient déjà : Button, OutlinedButton, TextButton, Card.
 *
 * Déclarés dans ce paquet, ils l'emportent sur ceux de Material dès
 * qu'un écran n'importe pas ces derniers. C'est voulu : la charte
 * s'applique partout sans qu'aucun écran ait à y penser, et un écran
 * qui importerait le bouton de Material se verrait au premier coup
 * d'œil. La calculatrice, elle, l'importe exprès : un leurre aux
 * couleurs du site n'en serait plus un.
 */

private val RembourrageBouton = PaddingValues(horizontal = 16.dp, vertical = 11.dp)

// `button.primary` : violet plein, arrondi moyen.
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) = BoutonMaterial(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    shape = Charte.ArrondiMoyen,
    contentPadding = RembourrageBouton,
    elevation = null,
    content = content,
)

// `button` : fond blanc, bordure grise, texte sombre.
@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) = BoutonContourMaterial(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    shape = Charte.ArrondiMoyen,
    contentPadding = RembourrageBouton,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    colors = ButtonDefaults.outlinedButtonColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ),
    content = content,
)

// Un lien : violet, sans cadre.
@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) = BoutonTexteMaterial(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    shape = Charte.ArrondiMoyen,
    content = content,
)

// `.card` : blanche, bordée, grand arrondi, ombre à peine marquée.
@Composable
fun Card(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    couleurDeBordure: androidx.compose.ui.graphics.Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val bordure = if (couleurDeBordure != null) BorderStroke(2.dp, couleurDeBordure) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = Charte.ArrondiGrand, border = bordure, shadowElevation = 1.dp) {
            Column(content = content)
        }
    } else {
        Surface(modifier = modifier, shape = Charte.ArrondiGrand, border = bordure, shadowElevation = 1.dp) {
            Column(content = content)
        }
    }
}

/*
 * La marque du site : quatre barres penchées, aux couleurs de l'échelle
 * (SiteHeader.module.css, `.site-logo`). Seulement DERRIÈRE le code :
 * la calculatrice ne la montre jamais.
 */
@Composable
fun Marque(modifier: Modifier = Modifier.size(width = 44.dp, height = 30.dp)) {
    Canvas(modifier) {
        val largeur = size.width * 0.13f
        val ecart = size.width * 0.09f
        // skewX(-18deg) : le haut part vers la droite de tan(18°) fois la hauteur.
        val penche = size.height * 0.325f
        val couleurs = listOf(NiveauVif.VERT, NiveauVif.JAUNE, NiveauVif.ORANGE, NiveauVif.ROUGE)
        val total = couleurs.size * largeur + (couleurs.size - 1) * ecart + penche
        var x = (size.width - total) / 2
        for (c in couleurs) {
            val barre = Path().apply {
                moveTo(x + penche, 0f)
                lineTo(x + penche + largeur, 0f)
                lineTo(x + largeur, size.height)
                lineTo(x, size.height)
                close()
            }
            drawPath(barre, c)
            x += largeur + ecart
        }
    }
}

/*
 * L'en-tête, derrière le code : la marque, le nom, et « Quitter vite »
 * comme sur le site, bordé de rouge (il dit la sortie, c'est son seul
 * droit à cette couleur).
 */
@Composable
fun EnTete(quitterVite: () -> Unit, menu: List<Pair<String, () -> Unit>> = emptyList()) {
    var ouvert by remember { mutableStateOf(false) }
    Column {
        /*
         * Le nom EN ENTIER. Le site, sur mobile, le cache pour laisser la
         * place à « Quitter vite » ; ici on resserre plutôt ce qui
         * l'entoure (le menu, la marque, le bouton), et sur un très petit
         * écran le nom passe un peu plus petit, sans jamais être coupé.
         */
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val etroit = maxWidth < 380.dp
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Le menu, comme le ☰ du site : ce qui n'est pas une des
                // quatre entrées (réglages, protection, mise à jour).
                if (menu.isNotEmpty()) {
                    Box {
                        BoutonTexteMaterial(onClick = { ouvert = true }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Text("☰", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        DropdownMenu(expanded = ouvert, onDismissRequest = { ouvert = false }) {
                            for ((libelle, action) in menu) {
                                DropdownMenuItem(text = { Text(libelle) }, onClick = { ouvert = false; action() })
                            }
                        }
                    }
                }
                Marque(Modifier.size(width = 30.dp, height = 24.dp))
                Text(
                    "Violentomètres",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
                    fontSize = if (etroit) 15.sp else 17.sp,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f),
                )
                BoutonContourMaterial(
                    onClick = quitterVite,
                    shape = Charte.ArrondiMoyen,
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("✕ Quitter vite", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

// Un texte d'appoint, gris et plus petit : dates, compteurs, consignes.
@Composable
fun Note(texte: String, modifier: Modifier = Modifier) {
    Text(texte, modifier, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun Espace(hauteur: Int = 8) = Spacer(Modifier.height(hauteur.dp))

/*
 * Un encadré : ce qui explique, rassure ou prévient, sorti du flot du
 * texte. Fond violet très pâle et filet violet à gauche ; `alerte` le
 * passe au rouge, réservé à ce qui dit un danger ou ce qui ne se
 * reprend pas.
 */
@Composable
fun Encadre(modifier: Modifier = Modifier, alerte: Boolean = false, titre: String? = null, contenu: @Composable ColumnScope.() -> Unit) {
    val trait = if (alerte) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Surface(modifier.fillMaxWidth(), shape = Charte.ArrondiMoyen, color = trait.copy(alpha = 0.07f)) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(trait))
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                titre?.let { Text(it, style = MaterialTheme.typography.titleSmall, color = trait) }
                contenu()
            }
        }
    }
}

// Le cas le plus courant : un encadré d'un seul paragraphe.
@Composable
fun Encadre(texte: String, alerte: Boolean = false, titre: String? = null) =
    Encadre(alerte = alerte, titre = titre) { Text(texte, style = MaterialTheme.typography.bodyMedium) }

// Vues, likes, et ce qu'on voudra : « 👁 12 · ♥ 3 · 20 situations ».
@Composable
fun Compteurs(vararg morceaux: String?) {
    val liste = morceaux.filterNotNull().filter { it.isNotBlank() }
    if (liste.isNotEmpty()) Note(liste.joinToString("  ·  "))
}

/*
 * « J'aime », comme sur le site. Le like part au serveur (décision du 7
 * octobre 2026) ; le cœur change tout de suite, et revient en arrière si
 * le serveur ne suit pas.
 */
@Composable
fun BoutonJAime(aime: Boolean, combien: Int, basculer: () -> Unit) {
    BoutonContourMaterial(
        onClick = basculer,
        shape = Charte.ArrondiMoyen,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        border = BorderStroke(1.dp, if (aime) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (aime) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        Text((if (aime) "♥ J'aime" else "♡ J'aime") + if (combien > 0) "  $combien" else "", fontWeight = FontWeight.SemiBold)
    }
}

/*
 * Chercher, filtrer, trier, comme les listes du site. La recherche passe
 * par le champ où le clavier n'apprend pas : ce qu'on cherche en dit
 * autant que ce qu'on écrit.
 */
private const val PREMIERS_FILTRES = 8

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BarreDeRecherche(
    cherche: String,
    chercher: (String) -> Unit,
    filtres: List<String>,
    filtre: String?,
    filtrer: (String?) -> Unit,
    tris: List<String>,
    tri: String,
    trier: (String) -> Unit,
    indication: String = "Chercher",
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Note(indication)
        ChampPrive(cherche, chercher, lignes = 1)
        if (filtres.isNotEmpty()) {
            /*
             * À la ligne, pas en défilement de côté : chercher un contexte
             * parmi des dizaines en faisant glisser une rangée, c'était
             * pénible (retour du 7 octobre). Les plus fréquents d'abord,
             * le reste à déplier.
             */
            var tout by remember { mutableStateOf(false) }
            val visibles = if (tout) filtres else (filtres.take(PREMIERS_FILTRES) + listOfNotNull(filtre)).distinct()
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Pastille("Tous", filtre == null) { filtrer(null) }
                for (f in visibles) Pastille(f, filtre == f) { filtrer(if (filtre == f) null else f) }
            }
            if (filtres.size > PREMIERS_FILTRES) {
                BoutonTexteMaterial(onClick = { tout = !tout }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text(if (tout) "Moins de choix ▴" else "Voir les ${filtres.size} ▾", fontSize = 14.sp)
                }
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            item { Note("Trier :") }
            items(tris) { tr -> Pastille(tr, tri == tr) { trier(tr) } }
        }
    }
}

@Composable
fun Pastille(texte: String, choisie: Boolean, choisir: () -> Unit) {
    FilterChip(
        selected = choisie,
        onClick = choisir,
        label = { Text(texte) },
        shape = RoundedCornerShape(999.dp),
    )
}

// Les contextes d'un violentomètre, en petites étiquettes grises.
@Composable
fun Etiquettes(textes: List<String>) {
    if (textes.isEmpty()) return
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(textes) { t ->
            Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.background, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Text(t, Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/*
 * Les petits points d'une suite d'écrans (le bilan du point, le
 * didacticiel) : combien il y en a, où l'on est, et un appui sur l'un
 * d'eux y mène directement.
 */
@Composable
fun Points(combien: Int, courant: Int, aller: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until combien) {
            Box(
                Modifier
                    .padding(horizontal = 2.dp)
                    .size(28.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { aller(i) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(if (i == courant) 12.dp else 8.dp)
                        .background(
                            if (i == courant) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(999.dp),
                        )
                )
            }
        }
    }
}
