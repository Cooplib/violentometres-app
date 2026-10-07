package fr.cooplib.util.ecrans

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * LA CHARTE DU SITE, reprise telle quelle : les variables de
 * ../violentometres-frontend/src/styles/base.css. Une seule source pour
 * l'application : les écrans ne décident d'aucune couleur, d'aucune
 * taille, ils prennent celles-ci.
 *
 * LES COULEURS ONT UN SENS. Vert, jaune, orange, rouge sont les niveaux
 * d'un violentomètre, et rien d'autre (Niveaux.kt) ; le rouge dit aussi
 * le danger (urgence, sortie, erreur). Tout ce qui navigue ou dit un
 * état d'interface prend le violet.
 *
 * La calculatrice n'en prend RIEN : un leurre aux couleurs du site
 * n'en serait plus un.
 */
object Charte {
    val Violet = Color(0xFF5B3FA0)          // --primary
    val VioletFonce = Color(0xFF472F80)     // --primary-hover
    val VioletPale = Color(0xFFEFEAF8)
    val Texte = Color(0xFF222222)           // --text
    val TexteSecondaire = Color(0xFF666666) // --text-secondary
    val Fond = Color(0xFFFAFAFA)            // --background
    val Surface = Color.White               // --surface
    val Bordure = Color(0xFFE2E2E2)         // --border
    val Rouge = Color(0xFFD32F2F)           // --red : niveau Danger, et le danger

    val ArrondiPetit = RoundedCornerShape(6.dp)   // --radius-sm
    val ArrondiMoyen = RoundedCornerShape(10.dp)  // --radius-md : boutons
    val ArrondiGrand = RoundedCornerShape(15.dp)  // --radius-lg : cartes
}

private val Clair = lightColorScheme(
    primary = Charte.Violet,
    onPrimary = Color.White,
    primaryContainer = Charte.VioletPale,
    onPrimaryContainer = Charte.VioletFonce,
    secondary = Charte.Violet,
    tertiary = Charte.Violet,
    background = Charte.Fond,
    onBackground = Charte.Texte,
    surface = Charte.Surface,
    onSurface = Charte.Texte,
    surfaceVariant = Charte.Fond,
    onSurfaceVariant = Charte.TexteSecondaire,
    outline = Charte.Bordure,
    outlineVariant = Charte.Bordure,
    error = Charte.Rouge,
)

/*
 * Le site n'a pas de mode sombre ; l'application, si, parce qu'on y
 * écrit son récit le soir, dans le noir (APPLICATION-ANDROID.md,
 * chapitre 2). Les mêmes rôles, transposés : le violet s'éclaircit pour
 * rester lisible, les neutres s'inversent.
 */
private val Sombre = darkColorScheme(
    primary = Color(0xFFB9A3E6),
    onPrimary = Color(0xFF2A1A52),
    primaryContainer = Color(0xFF3A2A66),
    onPrimaryContainer = Color(0xFFEFEAF8),
    secondary = Color(0xFFB9A3E6),
    tertiary = Color(0xFFB9A3E6),
    background = Color(0xFF151515),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF1F1F1F),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF151515),
    onSurfaceVariant = Color(0xFFB0B0B0),
    outline = Color(0xFF3A3A3A),
    outlineVariant = Color(0xFF3A3A3A),
    error = Color(0xFFEF6B6B),
)

// Les titres du site (h1 2,2rem, h2 1,6rem, h3 1,2rem, graisses 700 et
// 650), ramenés à un écran de téléphone ; le corps à 16, interligne 1,5.
private val Textes = Typography(
    headlineMedium = TextStyle(fontSize = 30.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontSize = 25.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold),
)

private val Formes = Shapes(
    small = Charte.ArrondiPetit,
    medium = Charte.ArrondiMoyen,
    large = Charte.ArrondiGrand,
)

@Composable
fun ThemeUtil(contenu: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Sombre else Clair,
        typography = Textes,
        shapes = Formes,
        content = contenu,
    )
}
