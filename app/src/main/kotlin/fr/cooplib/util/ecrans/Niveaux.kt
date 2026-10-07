package fr.cooplib.util.ecrans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.cooplib.util.modeles.Niveau

/*
 * LES SEULES COULEURS VERT, JAUNE, ORANGE, ROUGE DE L'APPLICATION : ce
 * sont les niveaux de l'échelle, et rien d'autre ne les prend.
 *
 * Deux teintes par niveau, comme sur le site (base.css) : les vives pour
 * les aplats et les barres, les foncées pour le TEXTE. Le vert, l'orange
 * et le jaune vifs tombent à 2,7:1 sur blanc, il en faut 4,5 pour lire.
 */
object NiveauVif {
    val VERT = Color(0xFF4CAF50)   // --green
    val JAUNE = Color(0xFFFBC02D)  // --yellow
    val ORANGE = Color(0xFFF57C00) // --orange
    val ROUGE = Color(0xFFD32F2F)  // --red
}

private object NiveauTexte {
    val VERT = Color(0xFF2E7D32)   // --green-texte
    val JAUNE = Color(0xFF7A5C00)  // --yellow-texte
    val ORANGE = Color(0xFFA35200) // --orange-texte
    val ROUGE = Color(0xFFD32F2F)
}

internal fun couleur(n: Niveau): Color = when (n.couleur) {
    "green" -> NiveauVif.VERT
    "yellow" -> NiveauVif.JAUNE
    "orange" -> NiveauVif.ORANGE
    "red" -> NiveauVif.ROUGE
    else -> runCatching { Color(android.graphics.Color.parseColor(n.couleur)) }.getOrDefault(Color.Gray)
}

internal fun couleurDeTexte(n: Niveau): Color = when (n.couleur) {
    "green" -> NiveauTexte.VERT
    "yellow" -> NiveauTexte.JAUNE
    "orange" -> NiveauTexte.ORANGE
    "red" -> NiveauTexte.ROUGE
    else -> couleur(n)
}

/*
 * L'étiquette d'un niveau, comme `.test__level` sur le site : une
 * pastille blanche et bordée, un point de la couleur du niveau, le nom
 * en gras et en sombre. Pas un aplat : un aplat jaune ne se lit pas.
 */
@Composable
internal fun EtiquetteNiveau(n: Niveau) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(10.dp).background(couleur(n), CircleShape))
            Text(n.label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

/*
 * La jauge du résultat, comme `.test__gauge` : une case par niveau, une
 * barre de sa couleur allumée jusqu'au niveau atteint, le nom dessous ;
 * la case atteinte cerclée de sombre.
 */
@Composable
internal fun Jauge(niveaux: List<Niveau>, atteint: Int?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (n in niveaux) {
            val allume = atteint != null && n.position <= atteint
            val cercle = if (n.position == atteint) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, Charte.ArrondiMoyen) else Modifier
            Surface(
                modifier = Modifier.weight(1f).then(cercle),
                shape = Charte.ArrondiMoyen,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.fillMaxWidth().height(14.dp)
                            .background(if (allume) couleur(n) else MaterialTheme.colorScheme.outline, RoundedCornerShape(999.dp))
                    )
                    Text(
                        n.label,
                        Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        color = if (allume) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
