package fr.cooplib.util.ecrans

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.cooplib.util.modeles.Niveau

/*
 * LES SEULES COULEURS VERT, JAUNE, ORANGE, ROUGE DE L'APPLICATION : ce
 * sont les niveaux de l'échelle, et rien d'autre ne les prend. Tout ce
 * qui navigue ou dit un état d'interface est violet (Theme.kt).
 */
internal fun couleur(n: Niveau): Color = when (n.couleur) {
    "green" -> Color(0xFF2E7D32)
    "yellow" -> Color(0xFFF9A825)
    "orange" -> Color(0xFFEF6C00)
    "red" -> Color(0xFFC62828)
    else -> runCatching { Color(android.graphics.Color.parseColor(n.couleur)) }.getOrDefault(Color.Gray)
}

// Sur le jaune, le blanc ne se lit pas.
internal fun texteSur(n: Niveau) = if (n.couleur == "yellow") Color(0xFF212121) else Color.White

@Composable
internal fun EtiquetteNiveau(n: Niveau) {
    Text(
        n.label,
        color = texteSur(n),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.background(couleur(n), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
