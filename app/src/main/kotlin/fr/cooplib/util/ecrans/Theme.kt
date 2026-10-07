package fr.cooplib.util.ecrans

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/*
 * LES COULEURS ONT UN SENS. Vert, jaune, orange, rouge sont les niveaux
 * d'un violentomètre, et rien d'autre ; le rouge dit aussi le danger
 * (urgence, sortie, erreur). Tout ce qui navigue ou dit un état
 * d'interface prend le violet. C'est la règle du site, imposée là-bas
 * par un test ; ici, par ce fichier, seul endroit où des couleurs
 * d'interface se décident.
 */
private val Violet = Color(0xFF6A3FA0)
private val VioletClair = Color(0xFFCDB8EB)
private val VioletFonce = Color(0xFF3E2163)

private val Clair = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE3F8),
    onPrimaryContainer = VioletFonce,
    secondary = Violet,
    tertiary = Violet,
)

private val Sombre = darkColorScheme(
    primary = VioletClair,
    onPrimary = VioletFonce,
    primaryContainer = Color(0xFF4B2C78),
    onPrimaryContainer = Color(0xFFEDE3F8),
    secondary = VioletClair,
    tertiary = VioletClair,
)

@Composable
fun ThemeUtil(contenu: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Sombre else Clair, content = contenu)
}
