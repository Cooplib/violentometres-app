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
    content: @Composable ColumnScope.() -> Unit,
) {
    val bordure = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
fun Marque(modifier: Modifier = Modifier) {
    Canvas(modifier.size(width = 44.dp, height = 30.dp)) {
        val largeur = 6.dp.toPx()
        val ecart = 4.dp.toPx()
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
fun EnTete(quitterVite: () -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Marque()
            Text(
                "Violentomètres",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            BoutonContourMaterial(
                onClick = quitterVite,
                shape = Charte.ArrondiMoyen,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("✕ Quitter vite", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
